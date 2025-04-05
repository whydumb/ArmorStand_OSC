package top.fifthlight.armorstand.model

import com.mojang.blaze3d.buffers.BufferType
import com.mojang.blaze3d.buffers.BufferUsage
import com.mojang.blaze3d.textures.TextureFormat
import com.mojang.blaze3d.vertex.VertexFormat
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.minecraft.client.texture.NativeImage
import org.joml.Vector3f
import top.fifthlight.armorstand.helper.GpuDeviceExt
import top.fifthlight.armorstand.render.IndexBuffer
import top.fifthlight.armorstand.render.RefCountedGpuBuffer
import top.fifthlight.armorstand.render.RefCountedGpuTexture
import top.fifthlight.armorstand.render.VertexBuffer
import top.fifthlight.armorstand.util.CacheMap
import top.fifthlight.armorstand.util.blaze3d
import top.fifthlight.armorstand.util.createBuffer
import top.fifthlight.armorstand.util.createVertexBuffer
import top.fifthlight.armorstand.util.withRenderDevice
import top.fifthlight.renderer.model.*

class ModelLoader {
    private val textureCache = CacheMap<Texture, RefCountedGpuTexture>()
    private val vertexBufferCache = CacheMap<Buffer, RefCountedGpuBuffer>()

    private suspend fun loadTexture(texture: Material.TextureInfo): RenderTexture? {
        val gpuTexture = texture.texture.let { texture ->
            texture.bufferView?.let { bufferView ->
                textureCache.compute(texture) {
                    val byteBuffer = bufferView.buffer.buffer.slice(bufferView.byteOffset, bufferView.byteLength)
                    val nativeImage = withContext(Dispatchers.Default) {
                        val image = NativeImage.read(null, byteBuffer)
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

    private suspend fun loadPrimitive(primitive: Primitive): RenderPrimitive? {
        val hasSkinElements =
            false // primitive.attributes.joints.isNotEmpty() && primitive.attributes.weights.isNotEmpty()
        val material = loadMaterial(primitive.material, hasSkinElements) ?: RenderMaterial.Default
        val vertexElements = loadVertexElements(primitive.attributes, material)
        val positionMin = requireNotNull(primitive.attributes.position.min) { "Position attribute without min" }.let {
            require(it.size == 3) { "Bad min of position attribute" }
            Vector3f(it[0], it[1], it[2])
        }
        val positionMax = requireNotNull(primitive.attributes.position.max) { "Position attribute without max" }.let {
            require(it.size == 3) { "Bad max of position attribute" }
            Vector3f(it[0], it[1], it[2])
        }
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
        val indexBuffer = primitive.indices?.let { indices ->
            check(indices.type == Accessor.AccessorType.SCALAR)
            val buffer = loadGenericBuffer(indices, BufferType.INDICES)
            IndexBuffer(
                buffer = buffer,
                length = indices.count,
                type = when (indices.componentType) {
                    Accessor.ComponentType.UNSIGNED_SHORT -> VertexFormat.IndexType.SHORT
                    Accessor.ComponentType.UNSIGNED_INT -> VertexFormat.IndexType.INT
                    else -> error("Bad index type: ${indices.componentType}")
                }
            )
        }
        return RenderPrimitive(
            vertexBuffer = vertexBuffer,
            indexBuffer = indexBuffer,
            material = material,
            positionMin = positionMin,
            positionMax = positionMax,
        )
    }

    private suspend fun loadMesh(mesh: Mesh) = coroutineScope {
        RenderMesh(primitives = mesh.primitives.map { async { loadPrimitive(it) } }.awaitAll().filterNotNull())
    }

    private suspend fun loadNode(node: Node): RenderNode = coroutineScope {
        RenderNode(
            children = node.children.map { async { loadNode(it) } }.awaitAll(),
            transform = node.transform,
            mesh = node.mesh?.let { loadMesh(it) },
        )
    }

    suspend fun load(scene: Scene): RenderNode = coroutineScope {
        RenderNode(children = scene.nodes.map { async { loadNode(it) } }.awaitAll())
    }
}
