package top.fifthlight.renderer.model.gltf

import kotlinx.serialization.json.Json
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.renderer.model.Accessor
import top.fifthlight.renderer.model.Buffer
import top.fifthlight.renderer.model.BufferView
import top.fifthlight.renderer.model.HumanoidTag
import top.fifthlight.renderer.model.Material
import top.fifthlight.renderer.model.Mesh
import top.fifthlight.renderer.model.Node
import top.fifthlight.renderer.model.NodeId
import top.fifthlight.renderer.model.NodeTransform
import top.fifthlight.renderer.model.Primitive
import top.fifthlight.renderer.model.Scene
import top.fifthlight.renderer.model.Skin
import top.fifthlight.renderer.model.Texture
import top.fifthlight.renderer.model.gltf.format.Gltf
import top.fifthlight.renderer.model.gltf.format.GltfAttributeKey
import top.fifthlight.renderer.model.gltf.format.GltfPrimitive
import top.fifthlight.renderer.model.gltf.format.GltfTextureInfo
import top.fifthlight.renderer.model.util.readAll
import java.io.IOException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.UUID

class GltfLoadException(message: String) : Exception(message)

object GltfLoader {
    private val defaultSampler = Texture.Sampler(
        magFilter = Texture.Sampler.MagFilter.LINEAR,
        minFilter = Texture.Sampler.MinFilter.LINEAR,
        wrapS = Texture.Sampler.WrapMode.REPEAT,
        wrapT = Texture.Sampler.WrapMode.REPEAT,
    )
    private val format = Json {
        ignoreUnknownKeys = true
    }

    private data class Context(
        private val buffer: ByteBuffer?,
        private val filePath: Path,
        private val basePath: Path,
    ) {
        private val uuid = UUID.randomUUID()
        private var loaded = false
        private lateinit var gltf: Gltf
        private val externalBuffers = mutableMapOf<URI, ByteBuffer>()
        private lateinit var buffers: List<Buffer>
        private lateinit var bufferViews: List<BufferView>
        private lateinit var accessors: List<Accessor>
        private lateinit var samplers: List<Texture.Sampler>
        private lateinit var textures: List<Texture>
        private lateinit var materials: List<Material>
        private lateinit var meshes: List<Mesh>
        private lateinit var skins: List<Skin>
        private val nodes = mutableMapOf<Int, Node>()
        private lateinit var scenes: List<Scene>

        private fun loadExternalUri(uri: URI): ByteBuffer = externalBuffers.getOrPut(uri) {
            TODO("URI resources is not supported for now")
        }

        private fun loadBuffers() {
            buffers = gltf.buffers?.mapIndexed { index, buffer ->
                if (buffer.uri == null) {
                    val contextBuffer = this@Context.buffer
                    if (index != 0 || contextBuffer == null) {
                        throw GltfLoadException("Buffer at $index missing URI")
                    }
                    Buffer(
                        name = "GLB built-in buffer",
                        buffer = contextBuffer,
                    )
                } else {
                    Buffer(
                        name = buffer.uri,
                        buffer = loadExternalUri(URI.create(buffer.uri)),
                    )
                }
            } ?: listOf()
        }

        private fun loadBufferViews() {
            bufferViews = gltf.bufferViews?.map {
                BufferView(
                    buffer = buffers.getOrNull(it.buffer)
                        ?: throw GltfLoadException("Invalid buffer view: buffer ${it.buffer} not found"),
                    byteLength = it.byteLength,
                    byteOffset = it.byteOffset ?: 0,
                    byteStride = it.byteStride ?: 0,
                )
            } ?: listOf()
        }

        private fun loadAccessors() {
            accessors = gltf.accessors?.map {
                Accessor(
                    bufferView = it.bufferView?.let { index ->
                        bufferViews.getOrNull(index)
                            ?: throw GltfLoadException("Invalid accessor: buffer view ${it.bufferView} not found")
                    },
                    byteOffset = it.byteOffset,
                    componentType = it.componentType,
                    normalized = it.normalized,
                    count = it.count,
                    type = it.type,
                    max = it.max,
                    min = it.min,
                    name = it.name,
                )
            } ?: listOf()
        }

        private fun loadSamplers() {
            samplers = gltf.samplers?.map {
                Texture.Sampler(
                    magFilter = it.magFilter,
                    minFilter = it.minFilter,
                    wrapS = it.wrapS,
                    wrapT = it.wrapT,
                )
            } ?: listOf()
        }

        private fun loadTextures() {
            val images = gltf.images ?: listOf()
            textures = gltf.textures?.map {
                val image = it.source?.let { index ->
                    images.getOrNull(index)
                        ?: throw GltfLoadException("Bad texture: image $index not found")
                }
                val bufferView = image?.bufferView?.let { index ->
                    bufferViews.getOrNull(index) ?: throw GltfLoadException("")
                } ?: image?.uri?.let { uri ->
                    val buffer = loadExternalUri(URI.create(uri))
                    BufferView(
                        buffer = Buffer(buffer = buffer),
                        byteLength = buffer.remaining(),
                        byteOffset = 0,
                        byteStride = 0,
                    )
                }
                Texture(
                    name = it.name,
                    sampler = it.sampler?.let { index ->
                        samplers.getOrNull(index)
                            ?: throw GltfLoadException("Bad texture: sampler $index not found")
                    } ?: defaultSampler,
                    bufferView = bufferView,
                    type = image?.mimeType?.let { mime ->
                        Texture.TextureType.entries.firstOrNull { mime == it.mimeType }
                    },
                )
            } ?: listOf()
        }

        private fun loadMaterials() {
            fun loadTextureInfo(info: GltfTextureInfo?) = info?.let {
                Material.TextureInfo(
                    texture = textures.getOrNull(info.index)
                        ?: throw GltfLoadException("Bad texture info: texture ${info.index} not found"),
                    textureCoordinate = info.texCoord,
                )
            }
            materials = gltf.materials?.map {
                val unlit = it.extensions.unlit
                when {
                    unlit != null -> Material.Unlit(
                        name = it.name,
                        baseColor = it.pbrMetallicRoughness.baseColorFactor,
                        baseColorTexture = loadTextureInfo(it.pbrMetallicRoughness.baseColorTexture),
                        alphaMode = it.alphaMode,
                        alphaCutoff = it.alphaCutoff,
                        doubleSided = it.doubleSided,
                    )

                    else -> Material.Pbr(
                        name = it.name,
                        baseColor = it.pbrMetallicRoughness.baseColorFactor,
                        baseColorTexture = loadTextureInfo(it.pbrMetallicRoughness.baseColorTexture),
                        metallicFactor = it.pbrMetallicRoughness.metallicFactor,
                        metallicRoughnessTexture = loadTextureInfo(it.pbrMetallicRoughness.metallicRoughnessTexture),
                        // TODO more attributes
                        alphaMode = it.alphaMode,
                        alphaCutoff = it.alphaCutoff,
                        doubleSided = it.doubleSided,
                    )
                }
            } ?: listOf()
        }

        private fun loadMeshes() {
            fun loadAttributes(attributes: Map<GltfAttributeKey, Int>): Primitive.Attributes {
                var position: Accessor? = null
                var normal: Accessor? = null
                var tangent: Accessor? = null
                var texcoords = mutableMapOf<Int, Accessor>()
                var colors = mutableMapOf<Int, Accessor>()
                var joints = mutableMapOf<Int, Accessor>()
                var weights = mutableMapOf<Int, Accessor>()
                for ((key, value) in attributes) {
                    val accessor = accessors.getOrNull(value)
                        ?: throw GltfLoadException("Bad attributes: unknown accessor index: $value")
                    when (key) {
                        GltfAttributeKey.Normal -> normal = accessor
                        GltfAttributeKey.Position -> position = accessor
                        GltfAttributeKey.Tangent -> tangent = accessor
                        is GltfAttributeKey.Color -> colors[key.index] = accessor
                        is GltfAttributeKey.TexCoord -> texcoords[key.index] = accessor
                        is GltfAttributeKey.Joints -> joints[key.index] = accessor
                        is GltfAttributeKey.Weights -> weights[key.index] = accessor
                        else -> continue
                    }
                }
                fun <T> loadAttributeMap(map: Map<Int, T>): List<T> = if (map.isEmpty()) {
                    listOf()
                } else {
                    (0..map.keys.max()).map {
                        map[it] ?: throw GltfLoadException("Bad attribute map: missing index $it")
                    }
                }
                check(position != null) { "No position attribute in primitive" }
                return Primitive.Attributes(
                    position = position,
                    normal = normal,
                    tangent = tangent,
                    texcoords = loadAttributeMap(texcoords),
                    colors = loadAttributeMap(colors),
                    joints = loadAttributeMap(joints),
                    weights = loadAttributeMap(weights)
                )
            }

            fun loadPrimitive(primitive: GltfPrimitive) = Primitive(
                mode = primitive.mode,
                material = primitive.material?.let {
                    materials.getOrNull(it) ?: throw GltfLoadException("Bad primitive: unknown material $it")
                } ?: Material.Default,
                attributes = loadAttributes(primitive.attributes),
                indices = primitive.indices?.let { indices ->
                    accessors.getOrNull(indices)
                        ?: throw GltfLoadException("Bad primitive: unknown accessor index: $indices")
                },
            )

            meshes = gltf.meshes?.map { mesh ->
                Mesh(primitives = mesh.primitives.map { primitive -> loadPrimitive(primitive) })
            } ?: listOf()
        }

        private fun loadSkins() {
            val boneMapping = run {
                val vrmV0 = gltf.extensions?.vrmV0?.humanoid?.humanBones
                if (vrmV0 != null) {
                    return@run vrmV0
                        .asSequence()
                        .mapNotNull { HumanoidTag.fromVrmName(it.bone)?.let { tag -> Pair(it.node, tag) } }
                        .associate { it }
                }
                val vrmV1 = gltf.extensions?.vrmV1?.humanoid?.humanBones
                if (vrmV1 != null) {
                    return@run vrmV1.entries.asSequence()
                        .mapNotNull { (boneName, boneItem) -> HumanoidTag.fromVrmName(boneName)?.let { tag -> Pair(tag, boneItem.node) } }
                        .associate { (tag, index) -> Pair(index, tag) }
                }
                null
            }
            skins = gltf.skins?.map { skin ->
                if (skin.joints.isEmpty()) {
                    throw GltfLoadException("Bad skin: no joints")
                }
                val inverseBindMatrices = run {
                    val accessor = skin.inverseBindMatrices?.let {
                        accessors.getOrNull(it) ?: throw GltfLoadException("Bad skin: no accessor at index $it")
                    } ?: return@run null
                    if (accessor.componentType != Accessor.ComponentType.FLOAT) {
                        throw GltfLoadException("Bad component type in skin's inverseBindMatrices: ${accessor.componentType}, should be FLOAT")
                    }
                    if (accessor.type != Accessor.AccessorType.MAT4) {
                        throw GltfLoadException("Bad type in skin's inverseBindMatrices: ${accessor.type}, should be MAT4")
                    }
                    if (accessor.count != skin.joints.size) {
                        throw GltfLoadException("Bad size of inverseBindMatrices: get ${accessor.count}, should be ${skin.joints.size}")
                    }
                    buildList<Matrix4f>(accessor.count) {
                        accessor.read { buffer ->
                            add(Matrix4f().set(buffer))
                        }
                    }
                }
                Skin(
                    name = skin.name,
                    joints = skin.joints.map { NodeId(uuid, it) },
                    skeleton = skin.skeleton?.let { NodeId(uuid, it) },
                    inverseBindMatrices = inverseBindMatrices,
                    ignoreGlobalTransform = true,
                    jointHumanoidTags = skin.joints.map { boneMapping?.get(it) },
                )
            } ?: listOf()
        }

        private fun loadNode(index: Int): Node = nodes.getOrPut(index) {
            // TODO avoid stack overflow on bad models
            val node = gltf.nodes?.getOrNull(index) ?: throw GltfLoadException("No node at index $index")
            val transform = when {
                node.matrix != null -> NodeTransform.Matrix(node.matrix)
                node.translation != null || node.rotation != null || node.scale != null -> NodeTransform.Decomposed(
                    translation = node.translation ?: Vector3f(),
                    rotation = node.rotation ?: Quaternionf(),
                    scale = node.scale ?: Vector3f(1f),
                )

                else -> null
            }
            Node(
                name = node.name,
                id = NodeId(
                    modelId = uuid,
                    index = index,
                ),
                children = (node.children ?: listOf()).map(::loadNode),
                mesh = node.mesh?.let { meshes.getOrNull(it) ?: throw GltfLoadException("Bad node: unknown mesh $it") },
                transform = transform,
                skin = node.skin?.let { skins.getOrNull(it) ?: throw GltfLoadException("Bad node: unknown skin $it") },
            )
        }

        private fun loadScenes() {
            scenes = gltf.scenes?.map {
                Scene(
                    name = it.name,
                    nodes = it.nodes?.map(::loadNode) ?: listOf(),
                    skins = skins,
                )
            } ?: listOf()
        }

        fun load(json: String): Scene? {
            if (loaded) {
                throw GltfLoadException("Already loaded. Please don't load again.")
            }
            loaded = true

            gltf = format.decodeFromString(json)

            loadBuffers()
            loadBufferViews()
            loadAccessors()
            loadSamplers()
            loadTextures()
            loadMaterials()
            loadMeshes()
            loadSkins()
            loadScenes()

            val sceneIndex = gltf.scene ?: 0
            return scenes.getOrNull(sceneIndex)
        }
    }

    private enum class ChunkType(
        val id: UInt?,
    ) {
        JSON(0x4E4F534Au),
        BINARY(0x004E4942u),
        UNKNOWN(null),
    }

    fun loadBinary(path: Path) = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
        val readBuffer = ByteBuffer.allocate(16)
        readBuffer.order(ByteOrder.LITTLE_ENDIAN)

        fun ByteBuffer.readBytes(
            len: Int,
            fail: (Int) -> String,
        ) {
            readBuffer.clear()
            readBuffer.limit(len)
            channel.readAll(readBuffer)
            if (readBuffer.limit() < len) {
                throw GltfLoadException(fail(readBuffer.limit()))
            }
            readBuffer.flip()
        }

        readBuffer.readBytes(4) { "Want to read 4 bytes of magic, but only got $it bytes" }
        val magic = readBuffer.getInt()
        if (magic != 0x46546c67) {
            throw GltfLoadException("Bad magic: want 0x46546c67, but got 0x${magic.toString(16).padStart(8, '0')}")
        }

        readBuffer.readBytes(8) { "Bad GLTF binary header" }
        val version = readBuffer.getInt()
        if (version != 2) {
            throw GltfLoadException("Bad GLTF version: want 2, but get $version")
        }
        val totalLength = readBuffer.getInt().toUInt()
        var readLength = 0u

        fun readChunk(wantedType: ChunkType, sizeLimit: Int): ByteBuffer? {
            if (readLength + 8u > totalLength) {
                throw GltfLoadException("No available space for chunk header: current read $readLength, total length $totalLength")
            }
            readBuffer.clear()
            readBuffer.limit(8)
            channel.readAll(readBuffer)
            when (readBuffer.limit()) {
                0 -> return null
                8 -> Unit
                else -> throw GltfLoadException("Bad chunk header: want 8 bytes, but only read ${readBuffer.limit()} bytes")
            }
            readBuffer.flip()
            readLength += 8u
            val length = readBuffer.getInt().toUInt()
            val type = readBuffer.getInt().toUInt()
            if (readLength + length > totalLength) {
                throw GltfLoadException("Bad chunk length: total length is $totalLength, current location is at $readLength, but want to read $length")
            }
            readLength += length

            val chunkType = ChunkType.entries.firstOrNull { it.id == type } ?: ChunkType.UNKNOWN
            if (chunkType != wantedType) {
                throw GltfLoadException("Bad chunk type: $chunkType, want $wantedType")
            }

            // First, let's try mapping into memory
            try {
                val length = length.toLong()
                val mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, channel.position(), length)
                channel.position(channel.position() + length)
                return mappedBuffer
            } catch (_: IOException) {
            } catch (_: UnsupportedOperationException) { // For ZipFileSystem
            }

            // If mapping failed, just read it to memory
            if (length > sizeLimit.toUInt()) {
                throw GltfLoadException("Chunk is too large: maximum is $sizeLimit, but got $length")
            }
            val binaryBuffer = ByteBuffer.allocateDirect(length.toInt())
            binaryBuffer.limit(length.toInt())
            channel.readAll(binaryBuffer)
            binaryBuffer.flip()
            if (binaryBuffer.limit() < length.toInt()) {
                throw GltfLoadException("Chunk's size not correct: want to read $length, but only got ${binaryBuffer.limit()}")
            }
            return binaryBuffer
        }

        val jsonChunk =
            readChunk(ChunkType.JSON, 4 * 1024 * 1024) ?: throw GltfLoadException("Missing JSON chunk in binary GLTF")
        val binaryChunk = readChunk(ChunkType.BINARY, 256 * 1024 * 1024)

        val context = Context(
            buffer = binaryChunk,
            filePath = path,
            basePath = path.parent,
        )
        context.load(StandardCharsets.UTF_8.decode(jsonChunk).toString())
    }
}