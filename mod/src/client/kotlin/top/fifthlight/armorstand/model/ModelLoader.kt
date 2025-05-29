package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.textures.GpuTexture
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import top.fifthlight.armorstand.extension.*
import top.fifthlight.armorstand.render.*
import top.fifthlight.armorstand.util.blaze3d
import top.fifthlight.renderer.model.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelLoader {
    private lateinit var skinsMap: Map<Skin, Pair<Int, RenderSkin>>
    private lateinit var skinsList: List<RenderSkin>

    data class JointSkinData(
        val skinIndex: Int,
        val jointIndex: Int,
        val humanoidTag: HumanoidTag?,
    )

    private lateinit var jointSkins: Map<NodeId, List<JointSkinData>>
    private fun loadSkins(skins: List<Skin>) {
        val jointSkinMap = mutableMapOf<NodeId, MutableList<JointSkinData>>()
        val skinsMap = mutableMapOf<Skin, Pair<Int, RenderSkin>>()
        val skinsList = mutableListOf<RenderSkin>()
        for ((index, skin) in skins.withIndex()) {
            val renderSkin = RenderSkin(
                name = skin.name,
                inverseBindMatrices = skin.inverseBindMatrices,
                jointSize = skin.joints.size,
            )
            for ((jointIndex, joint) in skin.joints.withIndex()) {
                jointSkinMap.getOrPut(joint) { mutableListOf() }.add(
                    JointSkinData(
                        skinIndex = index,
                        jointIndex = jointIndex,
                        humanoidTag = skin.jointHumanoidTags[jointIndex],
                    )
                )
            }
            skinsMap[skin] = Pair(index, renderSkin)
            skinsList.add(renderSkin)
        }
        this.skinsList = skinsList
        this.skinsMap = skinsMap
        jointSkins = jointSkinMap
    }

    private val nodes = mutableListOf<RenderNode>()
    private val transformNodeIndices = IntArrayList()
    private val primitiveNodes = mutableListOf<RenderNode.Primitive>()
    private val morphedPrimitiveNodeIndices = IntArrayList()
    private val nodeToMorphedPrimitiveMap = mutableMapOf<NodeId, MutableList<Int>>()
    private val meshToMorphedPrimitiveMap = mutableMapOf<MeshId, MutableList<Int>>()
    private val nodeIdTransformMap = Object2IntOpenHashMap<NodeId>()
    private val nodeNameTransformMap = Object2IntOpenHashMap<String>()
    private val humanoidJointTransformIndices = Reference2IntOpenHashMap<HumanoidTag>()
    private val textureCache = mutableMapOf<Texture, RefCountedGpuTexture>()
    private val vertexBufferCache = mutableMapOf<Buffer, RefCountedGpuBuffer>()

    private fun loadTexture(texture: Material.TextureInfo): RenderTexture? {
        val gpuTexture = texture.texture.let { texture ->
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
                        device.createCommandEncoder().writeToTexture(gpuTexture, nativeImage)
                        val textureView = device.createTextureView(gpuTexture)
                        RefCountedGpuTexture(gpuTexture, textureView)
                    }
                }
            }
        } ?: return null
        return RenderTexture(
            texture = gpuTexture,
            sampler = texture.texture.sampler,
            coordinate = texture.textureCoordinate,
        )
    }

    private fun loadMaterial(
        material: Material,
        hasSkinElements: Boolean,
        hasMorphTarget: Boolean,
    ): RenderMaterial<*>? = when (material) {
        Material.Default -> RenderMaterial.Fallback
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
            val byteBuffer = ByteBuffer.allocateDirect(2 * accessor.count)
            byteBuffer.order(ByteOrder.nativeOrder())
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
                ByteBuffer.allocateDirect(textureFormat.pixelSize())
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
        primitive: Primitive,
        skin: Pair<Int, RenderSkin>?,
        weights: List<Float>?,
        nodeId: NodeId,
        meshId: MeshId,
    ): RenderNode.Primitive? {
        val hasSkinElements =
            skin != null && primitive.attributes.joints.isNotEmpty() && primitive.attributes.weights.isNotEmpty()
        val material = loadMaterial(
            material = primitive.material,
            hasSkinElements = hasSkinElements,
            hasMorphTarget = primitive.targets.isNotEmpty()
        ) ?: RenderMaterial.Fallback
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
            loadMorphTargets(verticesCount, primitive.targets, weights)
        } else {
            Pair(null, listOf())
        }
        val indexBuffer = primitive.indices?.let { loadIndexBuffer(it) }
        val morphedPrimitiveIndex = if (material.morphed) {
            val morphedPrimitiveIndex = morphedPrimitiveNodeIndices.size
            meshToMorphedPrimitiveMap.getOrPut(meshId) { mutableListOf() }.add(morphedPrimitiveIndex)
            nodeToMorphedPrimitiveMap.getOrPut(nodeId) { mutableListOf() }.add(morphedPrimitiveIndex)
            morphedPrimitiveIndex
        } else {
            null
        }
        return RenderNode.Primitive(
            primitiveIndex = primitiveNodes.size,
            primitive = RenderPrimitive(
                vertexBuffer = vertexBuffer,
                indexBuffer = indexBuffer,
                material = material,
                targets = targets,
                targetGroups = targetGroups,
            ),
            skinIndex = skin?.first,
            morphedPrimitiveIndex = morphedPrimitiveIndex,
        )
    }

    private fun appendRenderNode(renderNode: RenderNode) {
        nodes.add(renderNode)
    }

    private fun loadNode(node: Node): RenderNode? {
        val skinItem = skinsMap[node.skin]
        val jointSkin = jointSkins[node.id]

        val children = buildList {
            jointSkin?.forEach { (skinIndex, jointIndex) ->
                val jointNode = RenderNode.Joint(
                    name = node.name,
                    skinIndex = skinIndex,
                    jointIndex = jointIndex,
                )
                add(jointNode)
                appendRenderNode(jointNode)
            }
            node.mesh?.let { mesh ->
                for (primitive in mesh.primitives) {
                    val primitiveNode = loadPrimitive(
                        primitive = primitive,
                        skin = skinItem,
                        weights = mesh.weights,
                        nodeId = node.id,
                        meshId = mesh.id,
                    ) ?: continue
                    primitiveNodes.add(primitiveNode)
                    val renderNodeIndex = nodes.size
                    if (primitiveNode.primitive.targets != null) {
                        morphedPrimitiveNodeIndices.add(renderNodeIndex)
                    }
                    appendRenderNode(primitiveNode)
                }

            }
            node.children.forEach { loadNode(it)?.let { child -> add(child) } }
        }
        if (children.isEmpty()) {
            return null
        }

        var currentNode: RenderNode = if (children.size == 1) {
            children.first()
        } else {
            val groupNode = RenderNode.Group(children)
            children.forEach { it.parent = groupNode }
            appendRenderNode(groupNode)
            groupNode
        }

        val transformIndex = transformNodeIndices.size
        transformNodeIndices.add(nodes.size)
        jointSkin?.let {
            for (skinItem in jointSkin) {
                skinItem.humanoidTag?.let { tag -> humanoidJointTransformIndices.put(tag, transformIndex) }
            }
        }
        nodeIdTransformMap.put(node.id, transformIndex)
        node.name?.let { nodeNameTransformMap.put(it, transformIndex) }
        currentNode = RenderNode.Transform(
            transformIndex = transformIndex,
            defaultTransform = node.transform,
            child = currentNode,
        ).also {
            currentNode.parent = it
            appendRenderNode(it)
        }

        return currentNode
    }

    private fun loadScene(scene: Scene, expressions: List<Expression>): RenderScene {
        val rootNode = RenderNode.Transform(
            child = RenderNode.Group(scene.nodes.mapNotNull { loadNode(it) }),
            defaultTransform = scene.initialTransform,
            transformIndex = transformNodeIndices.size,
        )
        val rootTransformId = nodes.size
        transformNodeIndices.add(rootTransformId)
        appendRenderNode(rootNode)
        return RenderScene(
            rootNode = rootNode,
            nodes = nodes,
            transformNodeIndices = transformNodeIndices,
            primitiveNodes = primitiveNodes,
            morphedPrimitiveNodeIndices = morphedPrimitiveNodeIndices,
            skins = skinsList,
            rootTransformNodeIndex = rootTransformId,
            humanoidTagToTransformMap = humanoidJointTransformIndices,
            nodeNameToTransformMap = nodeNameTransformMap,
            nodeIdToTransformMap = nodeIdTransformMap,
            expressions = expressions.mapNotNull { expression ->
                RenderExpression(
                    name = expression.name,
                    tag = expression.tag,
                    isBinary = expression.isBinary,
                    bindings = expression.bindings.flatMap {
                        when (it) {
                            is Expression.Binding.MeshMorphTarget -> {
                                meshToMorphedPrimitiveMap[it.meshId]?.map { index ->
                                    RenderExpression.Binding.MorphTarget(
                                        morphedPrimitiveIndex = index,
                                        groupIndex = it.index,
                                        weight = it.weight,
                                    )
                                } ?: listOf()
                            }

                            is Expression.Binding.NodeMorphTarget -> {
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
                ).takeIf { it.bindings.isNotEmpty() }
            }
        )
    }

    fun loadModel(model: Model): RenderScene {
        loadSkins(model.skins)
        return loadScene(model.defaultScene ?: model.scenes.first(), model.expressions)
    }
}
