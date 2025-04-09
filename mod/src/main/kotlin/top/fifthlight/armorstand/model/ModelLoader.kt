package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import kotlinx.coroutines.*
import top.fifthlight.armorstand.helper.GpuDeviceExt
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.RefCountedGpuBuffer
import top.fifthlight.armorstand.render.RefCountedGpuTexture
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.util.*
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
    private fun loadSkins(scene: Scene) {
        val jointSkinMap = mutableMapOf<NodeId, MutableList<JointSkinData>>()
        val skinsMap = mutableMapOf<Skin, Pair<Int, RenderSkin>>()
        val skinsList = mutableListOf<RenderSkin>()
        for ((index, skin) in scene.skins.withIndex()) {
            val renderSkin = RenderSkin(
                name = skin.name,
                inverseBindMatrices = skin.inverseBindMatrices,
                jointSize = skin.joints.size,
                ignoreGlobalTransform = skin.ignoreGlobalTransform,
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

    private val defaultTransforms = mutableListOf<NodeTransform?>()
    private val humanoidJointTransformIndices = Reference2IntOpenHashMap<HumanoidTag>()
    private val nodeIdTransformMap = Object2IntOpenHashMap<NodeId>()
    private val nodeNameTransformMap = Object2IntOpenHashMap<String>()
    private val textureCache = CacheMap<Texture, RefCountedGpuTexture>()
    private val vertexBufferCache = CacheMap<Buffer, RefCountedGpuBuffer>()

    private suspend fun loadTexture(texture: Material.TextureInfo): RenderTexture? {
        val gpuTexture = texture.texture.let { texture ->
            texture.bufferView?.let { bufferView ->
                textureCache.compute(texture) {
                    val byteBuffer = bufferView.buffer.buffer.slice(bufferView.byteOffset, bufferView.byteLength)
                    val nativeImage = withContext(Dispatchers.Default) {
                        val image = NativeImageExt.read(null, texture.type, byteBuffer)
                        image
                    }
                    withRenderDevice { device ->
                        val gpuTexture = device.createTexture(
                            null as String?,
                            TextureFormat.RGBA8,
                            nativeImage.width,
                            nativeImage.height,
                            1
                        )
                        device.createCommandEncoder().writeToTexture(gpuTexture, nativeImage)
                        RefCountedGpuTexture(gpuTexture)
                    }
                }
            }
        } ?: return null
        gpuTexture.increaseReferenceCount()
        return RenderTexture(
            texture = gpuTexture,
            sampler = texture.texture.sampler,
            coordinate = texture.textureCoordinate,
        )
    }

    private suspend fun loadMaterial(material: Material, hasSkinElements: Boolean): RenderMaterial? = when (material) {
        Material.Default -> RenderMaterial.Default
        is Material.Pbr -> RenderMaterial.Unlit(
            // TODO load PBR material
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = material.baseColorTexture?.let { loadTexture(it) } ?: RenderTexture.WHITE_RGBA_TEXTURE,
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = hasSkinElements,
        )

        is Material.Unlit -> RenderMaterial.Unlit(
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = material.baseColorTexture?.let { loadTexture(it) } ?: RenderTexture.WHITE_RGBA_TEXTURE,
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = hasSkinElements,
        )
    }

    private suspend fun loadVertexBuffer(buffer: Buffer): RefCountedGpuBuffer = vertexBufferCache.compute(buffer) {
        RefCountedGpuBuffer(
            withRenderDevice { device ->
                device.createBuffer({ buffer.name }, BufferType.VERTICES, BufferUsage.STATIC_WRITE, buffer.buffer)
            }
        )
    }

    private suspend fun loadGenericBuffer(accessor: Accessor, type: BufferType): RefCountedGpuBuffer =
        accessor.bufferView?.let { bufferView ->
            require(bufferView.byteStride == 0) { "Non-vertex buffer view's byteStride is not zero: ${bufferView.byteStride}" }
            require(accessor.length <= bufferView.byteLength) { "Accessor's length larger than underlying buffer view" }
            val buffer =
                bufferView.buffer.buffer.slice(bufferView.byteOffset + accessor.byteOffset, accessor.length)
            withRenderDevice { device ->
                RefCountedGpuBuffer(
                    device.createBuffer({ bufferView.buffer.name }, type, BufferUsage.STATIC_WRITE, buffer)
                )
            }
        } ?: TODO("Create zero filled buffer")

    private suspend fun fillVertexElement(
        componentType: Accessor.ComponentType,
        accessorType: Accessor.AccessorType,
        normalized: Boolean,
        usage: Primitive.Attributes.Key,
        elementCount: Int,
        one: Boolean,
    ) = withRenderDevice { device ->
        VertexBuffer.VertexElement(
            buffer = RefCountedGpuBuffer(
                device.createBuffer(
                    { "Filled buffer for usage $usage and type $componentType $accessorType" },
                    BufferType.VERTICES,
                    BufferUsage.STATIC_WRITE,
                    componentType.byteLength * accessorType.components * elementCount,
                    if (!one) {
                        GpuDeviceExt.FillType.ZERO_FILLED
                    } else if (componentType == Accessor.ComponentType.FLOAT) {
                        GpuDeviceExt.FillType.FLOAT_ONE_FILLED
                    } else {
                        GpuDeviceExt.FillType.BYTE_ONE_FILLED
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

    private suspend fun loadVertexElements(
        attributes: Primitive.Attributes,
        material: RenderMaterial
    ): List<VertexBuffer.VertexElement> = buildList {
        suspend fun createElement(
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

    private suspend fun loadIndexBuffer(accessor: Accessor): IndexBuffer {
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
            val gpuBuffer = withRenderDevice { device ->
                RefCountedGpuBuffer(
                    device.createBuffer(
                        { "Transformed index buffer ${accessor.name}" },
                        BufferType.INDICES,
                        BufferUsage.STATIC_WRITE,
                        byteBuffer
                    )
                )
            }
            return IndexBuffer(
                buffer = gpuBuffer,
                length = accessor.count,
                type = VertexFormat.IndexType.SHORT,
            )
        }
        val buffer = loadGenericBuffer(accessor, BufferType.INDICES)
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

    private suspend fun loadPrimitive(primitive: Primitive, skin: RenderSkin?): RenderPrimitive? {
        val hasSkinElements =
            skin != null && primitive.attributes.joints.isNotEmpty() && primitive.attributes.weights.isNotEmpty()
        val material = loadMaterial(primitive.material, hasSkinElements) ?: RenderMaterial.Default
        val vertexElements = loadVertexElements(primitive.attributes, material)
        val vertexBuffer = withRenderDevice { device ->
            device.createVertexBuffer(
                mode = when (primitive.mode) {
                    Primitive.Mode.POINTS -> return@withRenderDevice null
                    Primitive.Mode.LINE_STRIP -> VertexFormat.DrawMode.LINE_STRIP
                    Primitive.Mode.LINE_LOOP -> return@withRenderDevice null
                    Primitive.Mode.LINES -> VertexFormat.DrawMode.LINES
                    Primitive.Mode.TRIANGLES -> VertexFormat.DrawMode.TRIANGLES
                    Primitive.Mode.TRIANGLE_STRIP -> VertexFormat.DrawMode.TRIANGLE_STRIP
                    Primitive.Mode.TRIANGLE_FAN -> VertexFormat.DrawMode.TRIANGLE_FAN
                },
                elements = vertexElements,
                verticesCount = primitive.attributes.position.count,
            )
        } ?: return null
        val indexBuffer = primitive.indices?.let { loadIndexBuffer(it) }
        return RenderPrimitive(
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            material = material,
        )
    }

    private suspend fun loadMesh(mesh: Mesh, skin: RenderSkin?) = coroutineScope {
        RenderMesh(primitives = mesh.primitives.map { async { loadPrimitive(it, skin) } }.awaitAll().filterNotNull())
    }

    private suspend fun loadNode(node: Node): RenderNode? = coroutineScope {
        val skinItem = skinsMap[node.skin]
        val jointSkin = jointSkins[node.id]

        val children = buildList {
            jointSkin?.forEach { (skinIndex, jointIndex) -> add(RenderNode.Joint(skinIndex, jointIndex)) }
            node.mesh?.let {
                add(
                    RenderNode.Mesh(
                        mesh = loadMesh(it, skinItem?.second),
                        skinIndex = skinItem?.first,
                        ignoreGlobalTransform = skinItem?.second?.ignoreGlobalTransform == true,
                    )
                )
            }
            node.children.forEach { loadNode(it)?.let { add(it) } }
        }
        if (children.isEmpty()) {
            return@coroutineScope null
        }

        var currentNode: RenderNode = if (children.size == 1) {
            children.first()
        } else {
            RenderNode.Group(children).also { group ->
                children.forEach { it.parent = group }
            }
        }

        val transformIndex = defaultTransforms.size
        defaultTransforms += node.transform
        jointSkin?.let {
            for (skinItem in jointSkin) {
                skinItem.humanoidTag?.let { tag -> humanoidJointTransformIndices.put(tag, transformIndex) }
            }
        }
        nodeIdTransformMap.put(node.id, transformIndex)
        node.name?.let { nodeNameTransformMap.put(it, transformIndex) }
        RenderNode.Transform(
            transformIndex = transformIndex,
            child = currentNode,
        ).also {
            currentNode.parent = it
        }
    }

    suspend fun loadScene(scene: Scene): RenderScene {
        loadSkins(scene)
        val rootNode = RenderNode.Group(scene.nodes.mapNotNull { loadNode(it) })
        return RenderScene(
            rootNode = rootNode,
            defaultTransforms = defaultTransforms.toTypedArray(),
            skins = skinsList,
            humanoidTagTransformMap = humanoidJointTransformIndices,
            nodeNameTransformMap = nodeNameTransformMap,
            nodeIdTransformMap = nodeIdTransformMap,
        )
    }
}
