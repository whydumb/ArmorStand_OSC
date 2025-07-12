package top.fifthlight.blazerod.model

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import top.fifthlight.blazerod.extension.*
import top.fifthlight.blazerod.model.node.RenderNode
import top.fifthlight.blazerod.model.node.RenderNodeComponent
import top.fifthlight.blazerod.model.resource.*
import top.fifthlight.blazerod.render.IndexBuffer
import top.fifthlight.blazerod.render.RefCountedGpuBuffer
import top.fifthlight.blazerod.render.VertexBuffer
import top.fifthlight.blazerod.util.blaze3d
import top.fifthlight.blazerod.util.useMipmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelLoader {
    private lateinit var skinIndexMap: Reference2IntMap<Skin>
    private lateinit var skinsList: List<RenderSkin>

    data class SkinJointData(
        val skinIndex: Int,
        val jointIndex: Int,
        val humanoidTag: HumanoidTag?,
    )

    private lateinit var skinJoints: Map<NodeId, List<SkinJointData>>
    private fun loadSkins(skins: List<Skin>) {
        val skinIndexMap = Reference2IntOpenHashMap<Skin>()
        val skinJointMap = mutableMapOf<NodeId, MutableList<SkinJointData>>()
        val skinsList = mutableListOf<RenderSkin>()
        for ((index, skin) in skins.withIndex()) {
            val renderSkin = RenderSkin(
                name = skin.name,
                inverseBindMatrices = skin.inverseBindMatrices,
                jointSize = skin.joints.size,
            )
            for ((jointIndex, joint) in skin.joints.withIndex()) {
                skinJointMap.getOrPut(joint) { mutableListOf() }.add(
                    SkinJointData(
                        skinIndex = index,
                        jointIndex = jointIndex,
                        humanoidTag = skin.jointHumanoidTags[jointIndex],
                    )
                )
            }
            skinsList.add(renderSkin)
            skinIndexMap.put(skin, index)
        }
        this.skinsList = skinsList
        this.skinIndexMap = skinIndexMap
        this.skinJoints = skinJointMap
    }

    private val nodeIdToIndexMap = Object2IntOpenHashMap<NodeId>()
    private val renderNodes = Int2ObjectOpenHashMap<RenderNode>()
    private val primitiveComponents = mutableListOf<RenderNodeComponent.Primitive>()
    private val morphedPrimitiveComponents = mutableListOf<RenderNodeComponent.Primitive>()
    private val nodeToMorphedPrimitiveMap = mutableMapOf<NodeId, MutableList<Int>>()
    private val meshToMorphedPrimitiveMap = mutableMapOf<MeshId, MutableList<Int>>()
    private val textureCache = mutableMapOf<Texture, RenderTexture>()
    private val vertexBufferCache = mutableMapOf<Buffer, RefCountedGpuBuffer>()
    private val cameras = mutableListOf<RenderCamera>()

    private fun loadTexture(texture: Material.TextureInfo): RenderTexture? = texture.texture.let { texture ->
        texture.bufferView?.let { bufferView ->
            textureCache.getOrPut(texture) {
                val byteBuffer = bufferView.buffer.buffer.slice(bufferView.byteOffset, bufferView.byteLength)
                val nativeImage = NativeImageExt.read(null, texture.type, byteBuffer)
                RenderSystem.getDevice().let { device ->
                    val gpuTexture = device.createTexture(
                        texture.name,
                        GpuTexture.USAGE_TEXTURE_BINDING or GpuTexture.USAGE_COPY_DST,
                        TextureFormat.RGBA8,
                        nativeImage.width,
                        nativeImage.height,
                        1,
                        1
                    )
                    gpuTexture.setAddressMode(texture.sampler.wrapS.blaze3d, texture.sampler.wrapT.blaze3d)
                    gpuTexture.setTextureFilter(
                        texture.sampler.minFilter.blaze3d,
                        texture.sampler.magFilter.blaze3d,
                        texture.sampler.minFilter.useMipmap
                    )
                    device.createCommandEncoder().writeToTexture(gpuTexture, nativeImage)
                    val textureView = device.createTextureView(gpuTexture)
                    RenderTexture(gpuTexture, textureView)
                }
            }
        }
    }

    private fun loadMaterial(
        material: Material,
        hasSkinElements: Boolean,
        hasMorphTarget: Boolean,
    ): RenderMaterial<*>? = when (material) {
        is Material.Pbr -> RenderMaterial.Unlit(
            // TODO load PBR material
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = material.baseColorTexture?.let { loadTexture(it) } ?: RenderTexture.WHITE_RGBA_TEXTURE,
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = hasSkinElements,
            morphed = false,
        )

        is Material.Unlit -> RenderMaterial.Unlit(
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = material.baseColorTexture?.let { loadTexture(it) } ?: RenderTexture.WHITE_RGBA_TEXTURE,
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = hasSkinElements,
            morphed = hasMorphTarget,
        )
    }

    private fun loadVertexBuffer(buffer: Buffer): RefCountedGpuBuffer = vertexBufferCache.getOrPut(buffer) {
        RefCountedGpuBuffer(
            RenderSystem.getDevice().let { device ->
                device.createBuffer({ buffer.name }, GpuBuffer.USAGE_VERTEX, buffer.buffer)
            }
        )
    }

    private fun loadGenericBuffer(accessor: Accessor, usage: Int): RefCountedGpuBuffer =
        accessor.bufferView?.let { bufferView ->
            require(bufferView.byteStride == 0) { "Non-vertex buffer view's byteStride is not zero: ${bufferView.byteStride}" }
            require(accessor.totalByteLength <= bufferView.byteLength) { "Accessor's length larger than underlying buffer view" }
            val buffer =
                bufferView.buffer.buffer.slice(bufferView.byteOffset + accessor.byteOffset, accessor.totalByteLength)
            RefCountedGpuBuffer(
                RenderSystem.getDevice()
                    .createBuffer({ bufferView.buffer.name }, usage, buffer)
            )
        } ?: TODO("Create zero filled buffer")

    private fun fillVertexElement(
        componentType: Accessor.ComponentType,
        accessorType: Accessor.AccessorType,
        normalized: Boolean,
        usage: Primitive.Attributes.Key,
        elementCount: Int,
        one: Boolean,
    ) = RenderSystem.getDevice().let { device ->
        VertexBuffer.VertexElement(
            buffer = RefCountedGpuBuffer(
                device.createBuffer(
                    { "Filled buffer for usage $usage and type $componentType $accessorType" },
                    GpuBuffer.USAGE_VERTEX or GpuBuffer.USAGE_COPY_DST,
                    componentType.byteLength * accessorType.components * elementCount,
                    if (!one) {
                        CommandEncoderExt.ClearType.ZERO_FILLED
                    } else if (componentType == Accessor.ComponentType.FLOAT) {
                        CommandEncoderExt.ClearType.FLOAT_ONE_FILLED
                    } else {
                        CommandEncoderExt.ClearType.BYTE_ONE_FILLED
                    }
                )
            ),
            offset = 0,
            stride = 0,
            usage = usage,
            type = componentType.blaze3d,
            componentType = accessorType,
            normalized = normalized,
        )
    }

    private fun loadVertexElements(
        attributes: Primitive.Attributes.Primitive,
        material: RenderMaterial<*>,
    ): List<VertexBuffer.VertexElement> = buildList {
        fun createElement(
            accessor: Accessor,
            usage: Primitive.Attributes.Key,
            verticesCount: Int,
        ): VertexBuffer.VertexElement {
            require(accessor.count == verticesCount) { "Vertex attribute for usage $usage's length not equals to verticesCount $verticesCount" }
            return accessor.bufferView?.let { bufferView ->
                VertexBuffer.VertexElement(
                    buffer = loadVertexBuffer(bufferView.buffer),
                    offset = accessor.byteOffset + bufferView.byteOffset,
                    stride = bufferView.byteStride,
                    usage = usage,
                    type = accessor.componentType.blaze3d,
                    componentType = accessor.type,
                    normalized = accessor.normalized,
                )
            } ?: run {
                fillVertexElement(
                    componentType = accessor.componentType,
                    accessorType = accessor.type,
                    normalized = accessor.normalized,
                    usage = usage,
                    elementCount = verticesCount,
                    one = false,
                )
            }
        }
        for (elementKey in material.vertexType.elements) {
            add(
                when (elementKey) {
                    Primitive.Attributes.Key.POSITION -> attributes.position
                    Primitive.Attributes.Key.NORMAL -> attributes.normal
                    Primitive.Attributes.Key.TANGENT -> attributes.tangent
                    Primitive.Attributes.Key.TEXCOORD -> attributes.texcoords.firstOrNull()
                    Primitive.Attributes.Key.COLORS -> attributes.colors.firstOrNull()
                    Primitive.Attributes.Key.JOINTS -> attributes.joints.firstOrNull()
                    Primitive.Attributes.Key.WEIGHTS -> attributes.weights.firstOrNull()
                }?.let { accessor ->
                    createElement(
                        accessor = accessor,
                        usage = elementKey,
                        verticesCount = attributes.position.count,
                    )
                } ?: run {
                    val componentTypeKey = elementKey.allowedComponentTypes.first()
                    val accessorType = elementKey.allowedAccessorTypes.first()
                    fillVertexElement(
                        componentType = componentTypeKey.type,
                        accessorType = accessorType,
                        normalized = componentTypeKey.normalized,
                        usage = elementKey,
                        elementCount = attributes.position.count,
                        one = elementKey.defaultOne,
                    )
                }
            )
        }
    }

    private fun loadIndexBuffer(accessor: Accessor): IndexBuffer {
        check(accessor.type == Accessor.AccessorType.SCALAR)
        if (accessor.componentType == Accessor.ComponentType.UNSIGNED_BYTE) {
            // We need some workaround, because there is just no index type of BYTE
            val byteBuffer = ByteBuffer.allocateDirect(2 * accessor.count).order(ByteOrder.nativeOrder())
            accessor.read { input ->
                val index = input.get().toUByte()
                byteBuffer.putShort(index.toShort())
            }
            byteBuffer.flip()
            val gpuBuffer = RefCountedGpuBuffer(
                RenderSystem.getDevice().createBuffer(
                    { "Transformed index buffer ${accessor.name}" },
                    GpuBuffer.USAGE_INDEX,
                    byteBuffer
                )
            )
            return IndexBuffer(
                buffer = gpuBuffer,
                length = accessor.count,
                type = VertexFormat.IndexType.SHORT,
            )
        }
        val buffer = loadGenericBuffer(accessor, GpuBuffer.USAGE_INDEX)
        return IndexBuffer(
            buffer = buffer,
            length = accessor.count,
            type = when (accessor.componentType) {
                Accessor.ComponentType.UNSIGNED_SHORT -> VertexFormat.IndexType.SHORT
                Accessor.ComponentType.UNSIGNED_INT -> VertexFormat.IndexType.INT
                else -> error("Bad index type: ${accessor.componentType}")
            }
        )
    }

    @ConsistentCopyVisibility
    private data class BuildingTarget private constructor(
        val buffer: ByteBuffer,
        val textureFormat: TextureFormat,
        val targetsCount: Int,
    ) {
        constructor(
            textureFormat: TextureFormat,
            itemCount: Int,
            targetsCount: Int,
        ) : this(
            buffer = ByteBuffer.allocateDirect(textureFormat.pixelSize() * itemCount * targetsCount)
                .order(ByteOrder.nativeOrder()),
            textureFormat = textureFormat,
            targetsCount = targetsCount,
        )

        fun toTarget(): RenderPrimitive.Target {
            require(!buffer.hasRemaining()) { "Has remaining size for morph targets" }
            buffer.flip()
            val targetBuffer = if (!buffer.hasRemaining()) {
                // No targets, but we can't create an empty buffer, so let's create a dummy one
                ByteBuffer.allocateDirect(textureFormat.pixelSize()).order(ByteOrder.nativeOrder())
            } else {
                buffer
            }
            val device = RenderSystem.getDevice()
            val gpuBuffer = device.createBuffer(
                { "Morph target buffer" },
                GpuBuffer.USAGE_UNIFORM_TEXEL_BUFFER,
                targetBuffer
            )
            return RenderPrimitive.Target(
                data = gpuBuffer,
                targetsCount = targetsCount,
            )
        }
    }

    private fun loadMorphTargets(
        verticesCount: Int,
        targets: List<Primitive.Attributes.MorphTarget>,
        weights: List<Float>?,
    ): Pair<RenderPrimitive.Targets, List<MorphTargetGroup>> {
        val positionTarget = BuildingTarget(
            textureFormat = TextureFormatExt.RGB32F,
            itemCount = verticesCount,
            targetsCount = targets.count { it.position != null },
        )
        val colorTarget = BuildingTarget(
            textureFormat = TextureFormatExt.RGBA32F,
            itemCount = verticesCount,
            targetsCount = targets.count { it.colors.isNotEmpty() },
        )
        val texCoordTarget = BuildingTarget(
            textureFormat = TextureFormatExt.RG32F,
            itemCount = verticesCount,
            targetsCount = targets.count { it.texcoords.isNotEmpty() },
        )
        var posIndex = 0
        var colorIndex = 0
        var texCoordIndex = 0
        val groups = mutableListOf<MorphTargetGroup>()
        for ((index, target) in targets.withIndex()) {
            val position = target.position?.let { position ->
                position.read { input ->
                    positionTarget.buffer.put(input)
                }
                posIndex++
            }
            val color = target.colors.firstOrNull()?.let { color ->
                when (color.type) {
                    Accessor.AccessorType.VEC3 -> {
                        var index = 0
                        color.readNormalized { input ->
                            colorTarget.buffer.putFloat(input)
                            index++
                            if (index == 3) {
                                // For padding
                                colorTarget.buffer.putFloat(0f)
                            }
                        }
                    }

                    Accessor.AccessorType.VEC4 -> color.readNormalized {
                        colorTarget.buffer.putFloat(it)
                    }

                    else -> throw AssertionError("Bad morph target: accessor type of color is ${color.type}")
                }
                colorIndex++
            }
            val texCoord = target.texcoords.firstOrNull()?.let { texCoord ->
                texCoord.readNormalized {
                    texCoordTarget.buffer.putFloat(it)
                }
                texCoordIndex++
            }
            groups.add(
                MorphTargetGroup(
                    position = position,
                    color = color,
                    texCoord = texCoord,
                    weight = weights?.getOrNull(index) ?: 0f,
                )
            )
        }
        val targets = RenderPrimitive.Targets(
            position = positionTarget.toTarget(),
            color = colorTarget.toTarget(),
            texCoord = texCoordTarget.toTarget(),
        )
        return Pair(targets, groups)
    }

    private fun loadPrimitive(
        mesh: Mesh,
        primitive: Primitive,
        skinIndex: Int?,
        nodeId: NodeId,
        meshId: MeshId,
    ): RenderNodeComponent.Primitive? {
        val hasSkinElements =
            skinIndex != null && primitive.attributes.joints.isNotEmpty() && primitive.attributes.weights.isNotEmpty()
        val material = primitive.material?.let {
            loadMaterial(
                material = it,
                hasSkinElements = hasSkinElements,
                hasMorphTarget = primitive.targets.isNotEmpty()
            )
        } ?: RenderMaterial.defaultMaterial
        val vertexElements = loadVertexElements(primitive.attributes, material)
        val verticesCount = primitive.attributes.position.count
        val vertexBuffer = RenderSystem.getDevice().createVertexBuffer(
            mode = when (primitive.mode) {
                Primitive.Mode.POINTS -> return null
                Primitive.Mode.LINE_STRIP -> VertexFormat.DrawMode.LINE_STRIP
                Primitive.Mode.LINE_LOOP -> return null
                Primitive.Mode.LINES -> VertexFormat.DrawMode.LINES
                Primitive.Mode.TRIANGLES -> VertexFormat.DrawMode.TRIANGLES
                Primitive.Mode.TRIANGLE_STRIP -> VertexFormat.DrawMode.TRIANGLE_STRIP
                Primitive.Mode.TRIANGLE_FAN -> VertexFormat.DrawMode.TRIANGLE_FAN
            },
            elements = vertexElements,
            verticesCount = verticesCount,
        )
        val (targets, targetGroups) = if (material.morphed) {
            loadMorphTargets(verticesCount, primitive.targets, mesh.weights)
        } else {
            Pair(null, listOf())
        }
        val indexBuffer = primitive.indices?.let { loadIndexBuffer(it) }
        val morphedPrimitiveIndex = if (material.morphed) {
            val morphedPrimitiveIndex = morphedPrimitiveComponents.size
            meshToMorphedPrimitiveMap.getOrPut(meshId) { mutableListOf() }.add(morphedPrimitiveIndex)
            nodeToMorphedPrimitiveMap.getOrPut(nodeId) { mutableListOf() }.add(morphedPrimitiveIndex)
            morphedPrimitiveIndex
        } else {
            null
        }
        return RenderNodeComponent.Primitive(
            primitiveIndex = primitiveComponents.size,
            primitive = RenderPrimitive(
                vertexBuffer = vertexBuffer,
                indexBuffer = indexBuffer,
                material = material,
                targets = targets,
                targetGroups = targetGroups,
            ),
            skinIndex = skinIndex,
            morphedPrimitiveIndex = morphedPrimitiveIndex,
            firstPersonFlag = mesh.firstPersonFlag,
        ).also {
            primitiveComponents.add(it)
            if (it.morphedPrimitiveIndex != null) {
                morphedPrimitiveComponents.add(it)
            }
        }
    }

    private fun loadNode(node: Node): RenderNode? {
        val jointSkin = skinJoints[node.id]

        val components = buildList {
            jointSkin?.forEach { (skinIndex, jointIndex) ->
                add(
                    RenderNodeComponent.Joint(
                        skinIndex = skinIndex,
                        jointIndex = jointIndex,
                    )
                )
            }
            node.meshComponent?.let { meshComponent ->
                val mesh = meshComponent.mesh
                val skin = node.skinComponent?.skin
                for (primitive in mesh.primitives) {
                    loadPrimitive(
                        mesh = mesh,
                        primitive = primitive,
                        skinIndex = skin?.let { skinIndexMap.getInt(it) },
                        nodeId = node.id,
                        meshId = mesh.id,
                    )?.let { add(it) }
                }
            }
            for (component in node.components) {
                when (component) {
                    is NodeComponent.CameraComponent -> {
                        val cameraIndex = cameras.size
                        cameras.add(
                            RenderCamera(
                                cameraIndex = cameraIndex,
                                camera = component.camera,
                            )
                        )
                        add(RenderNodeComponent.Camera(cameraIndex))
                    }

                    is NodeComponent.InfluenceTargetComponent -> {
                        val influence = component.influence
                        add(
                            RenderNodeComponent.InfluenceTarget(
                                sourceNodeIndex = nodeIdToIndexMap.getInt(influence.source),
                                influence = influence.influence,
                                influenceRotation = influence.influenceRotation,
                                influenceTranslation = influence.influenceTranslation,
                                target = TransformId.INFLUENCE,
                            )
                        )
                    }

                    else -> {}
                }
            }
        }

        return RenderNode(
            nodeIndex = nodeIdToIndexMap.getInt(node.id),
            children = node.children.mapNotNull { loadNode(it) },
            absoluteTransform = node.transform,
            components = components,
            nodeId = node.id,
            nodeName = node.name,
            humanoidTags = jointSkin?.mapNotNull { it.humanoidTag } ?: listOf(),
        ).also { renderNodes[it.nodeIndex] = it }
    }

    private fun loadScene(scene: Scene, expressions: List<Expression>): RenderScene {
        // Pre-allocate node id
        var nodeIndex = 0
        for (node in scene.nodes) {
            node.forEach { node ->
                nodeIdToIndexMap[node.id] = nodeIndex++
            }
        }

        val rootNode = RenderNode(
            nodeIndex = nodeIndex,
            children = scene.nodes.mapNotNull { loadNode(it) },
            absoluteTransform = scene.initialTransform,
            components = listOf(),
        )
        renderNodes[nodeIdToIndexMap.size] = rootNode
        val expressionTargetMap = mutableMapOf<Int, Int>()
        return RenderScene(
            rootNode = rootNode,
            nodes = (0 until renderNodes.size).map { index ->
                renderNodes[index] ?: error("Missing node index $index")
            },
            skins = skinsList,
            expressions = buildList {
                for ((expressionIndex, expression) in expressions.withIndex()) {
                    if (expression !is Expression.Target) {
                        continue
                    }
                    val expression = RenderExpression(
                        name = expression.name,
                        tag = expression.tag,
                        isBinary = expression.isBinary,
                        bindings = expression.bindings.flatMap {
                            when (it) {
                                is Expression.Target.Binding.MeshMorphTarget -> {
                                    meshToMorphedPrimitiveMap[it.meshId]?.map { index ->
                                        RenderExpression.Binding.MorphTarget(
                                            morphedPrimitiveIndex = index,
                                            groupIndex = it.index,
                                            weight = it.weight,
                                        )
                                    } ?: listOf()
                                }

                                is Expression.Target.Binding.NodeMorphTarget -> {
                                    nodeToMorphedPrimitiveMap[it.nodeId]?.map { index ->
                                        RenderExpression.Binding.MorphTarget(
                                            morphedPrimitiveIndex = index,
                                            groupIndex = it.index,
                                            weight = it.weight,
                                        )
                                    } ?: listOf()
                                }
                            }
                        }
                    )
                    if (expression.bindings.isEmpty()) {
                        continue
                    }
                    expressionTargetMap[expressionIndex] = this.size
                    add(expression)
                }
            },
            expressionGroups = expressions.mapNotNull { expression ->
                if (expression !is Expression.Group) {
                    return@mapNotNull null
                }
                RenderExpressionGroup(
                    name = expression.name,
                    tag = expression.tag,
                    items = expression.targets.mapNotNull {
                        val targetIndex =
                            expressions.indexOf(it.target).takeIf { index -> index != -1 } ?: return@mapNotNull null
                        RenderExpressionGroup.Item(
                            expressionIndex = expressionTargetMap[targetIndex] ?: return@mapNotNull null,
                            influence = it.influence,
                        )
                    }
                )
            },
            cameras = cameras,
        )
    }

    fun loadModel(model: Model): RenderScene {
        loadSkins(model.skins)
        return loadScene(model.defaultScene ?: model.scenes.first(), model.expressions)
    }
}
