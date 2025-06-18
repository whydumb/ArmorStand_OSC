package top.fifthlight.blazerod.model.pmd

import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3f
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.Material.TextureInfo
import top.fifthlight.blazerod.model.pmd.format.PmdBone
import top.fifthlight.blazerod.model.pmd.format.PmdHeader
import top.fifthlight.blazerod.model.pmd.format.PmdMaterial

import top.fifthlight.blazerod.model.util.readAll

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.math.PI

class PmdLoadException(message: String) : Exception(message)

// Pmd loader
// Format from: https://mikumikudance.fandom.com/wiki/MMD:Polygon_Model_Data
object PmdLoader: ModelFileLoader {
    override val extensions = mapOf(
        "pmd" to setOf(ModelFileLoader.Ability.MODEL),
    )

    private val PMD_SIGNATURE = byteArrayOf(0x50, 0x6D, 0x64, 0x00, 0x00, 0x80u.toByte(), 0x3F)
    override val probeLength = PMD_SIGNATURE.size
    override fun probe(buffer: ByteBuffer): Boolean {
        if (buffer.remaining() < PMD_SIGNATURE.size) return false
        val signatureBytes = ByteArray(PMD_SIGNATURE.size)
        buffer.get(signatureBytes, 0, PMD_SIGNATURE.size)
        return signatureBytes.contentEquals(PMD_SIGNATURE)
    }

    private val SHIFT_JIS = Charset.forName("Shift-JIS")
    private val decoder = SHIFT_JIS.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)

    //                                             POS NORM UV
    private const val BASE_VERTEX_ATTRIBUTE_SIZE = (3 + 3 + 2) * 4

    //                                           JOINT WEIGHT
    private const val SKIN_VERTEX_ATTRIBUTE_SIZE = (4 + 4) * 4
    private const val VERTEX_ATTRIBUTE_SIZE = BASE_VERTEX_ATTRIBUTE_SIZE + SKIN_VERTEX_ATTRIBUTE_SIZE

    private class Context(
        val basePath: Path
    ) {
        private lateinit var vertexBuffer: ByteBuffer
        private var vertices: Int = -1

        private lateinit var indexBuffer: ByteBuffer
        private var indices: Int = -1

        private lateinit var materials: List<PmdMaterial>
        private lateinit var bones: List<PmdBone>
        private val childBoneMap = mutableMapOf<Int, MutableList<Int>>()
        private val rootBones = mutableListOf<Int>()

        private fun loadString(buffer: ByteBuffer, maxLength: Int): String {
            val bytes = ByteBuffer.allocate(maxLength)
            bytes.put(buffer.slice(buffer.position(), maxLength))
            buffer.position(buffer.position() + maxLength)
            val nullIndex = (0 until maxLength).indexOfFirst { bytes.get(it) == 0.toByte() }
            val stringBytes = bytes.slice(0, nullIndex).order(ByteOrder.LITTLE_ENDIAN)
            return decoder.decode(stringBytes).toString()
        }

        private fun loadRgbColor(buffer: ByteBuffer): RgbColor {
            if (buffer.remaining() < 3 * 4) {
                throw PmdLoadException("Bad file: want to read Vec3 (12 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return RgbColor(
                r = buffer.getFloat(),
                g = buffer.getFloat(),
                b = buffer.getFloat(),
            )
        }

        private fun loadRgbaColor(buffer: ByteBuffer): RgbaColor {
            if (buffer.remaining() < 4 * 4) {
                throw PmdLoadException("Bad file: want to read Vec4 (16 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return RgbaColor(
                r = buffer.getFloat(),
                g = buffer.getFloat(),
                b = buffer.getFloat(),
                a = buffer.getFloat(),
            )
        }

        private fun loadVector3f(buffer: ByteBuffer): Vector3f {
            if (buffer.remaining() < 3 * 4) {
                throw PmdLoadException("Bad file: want to read Vec3 (12 bytes), but only have ${buffer.remaining()} bytes available")
            }
            return Vector3f(buffer.getFloat(), buffer.getFloat(), buffer.getFloat())
        }

        private fun loadSignature(buffer: ByteBuffer) {
            if (buffer.remaining() < PMD_SIGNATURE.size) {
                throw PmdLoadException("Bad file: signature is ${PMD_SIGNATURE.size} bytes, but only ${buffer.remaining()} bytes in buffer")
            }
            if (PMD_SIGNATURE.any { buffer.get() != it }) {
                throw PmdLoadException("Bad PMX signature")
            }
        }

        private fun loadHeader(buffer: ByteBuffer): PmdHeader {
            loadSignature(buffer)
            return PmdHeader(
                name = loadString(buffer, 20),
                comment = loadString(buffer, 256),
            )
        }

        private fun loadVertices(buffer: ByteBuffer) {
            val vertexCount = buffer.getInt()
            if (vertexCount <= 0) {
                throw PmdLoadException("Bad vertices: count $vertexCount should be greater than zero")
            }

            val outputBuffer =
                ByteBuffer.allocateDirect(VERTEX_ATTRIBUTE_SIZE * vertexCount).order(ByteOrder.nativeOrder())
            var outputPosition = 0
            var inputPosition = buffer.position()

            fun readFloat(): Float = buffer.getFloat(inputPosition).also {
                inputPosition += 4
            }

            val copyBaseVertexSize = BASE_VERTEX_ATTRIBUTE_SIZE - 12
            // FORMAT: POSITION_NORMAL_UV_JOINT_WEIGHT
            for (i in 0 until vertexCount) {
                // Read vertex data
                // invert z axis
                outputBuffer.put(outputPosition, buffer, inputPosition, 8)
                outputPosition += 8
                inputPosition += 8
                outputBuffer.putFloat(outputPosition, -readFloat())
                outputPosition += 4
                // POSITION_NORMAL_UV_JOINT_WEIGHT
                outputBuffer.put(outputPosition, buffer, inputPosition, copyBaseVertexSize)
                outputPosition += copyBaseVertexSize
                inputPosition += copyBaseVertexSize

                outputBuffer.putInt(outputPosition, buffer.getShort(inputPosition).toUShort().toInt())
                outputBuffer.putInt(outputPosition + 4, buffer.getShort(inputPosition + 2).toUShort().toInt())
                val weight = buffer.get().toUShort().toFloat() / 100f
                outputBuffer.putFloat(outputPosition + 16, weight)
                outputBuffer.putFloat(outputPosition + 20, 1f - weight)
                outputPosition += SKIN_VERTEX_ATTRIBUTE_SIZE
                inputPosition += 6
            }
            require(outputPosition == outputBuffer.capacity()) { "Bug: Not filled the entire output buffer" }
            vertexBuffer = outputBuffer
            vertices = vertexCount
            buffer.position(inputPosition)
        }

        private fun loadIndices(buffer: ByteBuffer) {
            val indexCount = buffer.getInt()
            if (indexCount <= 0) {
                throw PmdLoadException("Bad indeices: count $indexCount should be greater than zero")
            }
            if (indexCount % 3 != 0) {
                throw PmdLoadException("Bad index count: $indexCount % 3 != 0")
            }

            val triangleCount = indexCount / 3
            val indexBufferSize = 2 * indexCount
            if (buffer.remaining() < indexBufferSize) {
                throw PmdLoadException("Bad index data: should have $indexBufferSize bytes, but only ${buffer.remaining()} bytes available")
            }

            val outputBuffer = ByteBuffer.allocateDirect(indexBufferSize).order(ByteOrder.nativeOrder())
            // PMD use clockwise indices, but OpenGL use counterclockwise indices, so let's invert the order here.
            for (i in 0 until triangleCount) {
                outputBuffer.putShort(buffer.getShort())
                val a = buffer.getShort()
                val b = buffer.getShort()
                outputBuffer.putShort(b)
                outputBuffer.putShort(a)
            }
            indexBuffer = outputBuffer
            indices = indexCount
        }

        private fun parseTextureInfo(info: String) = info.split('*').let {
            when (it.size) {
                1 -> Pair(it[0], null)
                2 -> Pair(it[0], it[1])
                else -> throw PmdLoadException("Bad texture info: $info")
            }
        }

        private fun loadMaterials(buffer: ByteBuffer) {
            val materialCount = buffer.getInt()
            materials = (0 until materialCount).map {
                val diffuseColor = loadRgbaColor(buffer)
                val specularStrength = buffer.getFloat()
                val specularColor = loadRgbColor(buffer)
                val ambientColor = loadRgbColor(buffer)
                val toonIndex = buffer.get().toInt()
                val edgeFlag = buffer.get() != 0.toByte()
                val verticesCount = buffer.getInt()
                val (texture, sphere) = parseTextureInfo(loadString(buffer, 20))
                PmdMaterial(
                    diffuseColor = diffuseColor,
                    specularStrength = specularStrength,
                    specularColor = specularColor,
                    ambientColor = ambientColor,
                    toonIndex = toonIndex,
                    edgeFlag = edgeFlag,
                    verticesCount = verticesCount,
                    textureFilename = texture.takeIf { it.isNotEmpty() },
                    sphereFilename = sphere,
                )
            }
        }

        private fun loadTexture(name: String): Texture {
            val path = basePath.resolve(name)
            val buffer = FileChannel.open(path, StandardOpenOption.READ).use { channel ->
                val size = channel.size()
                runCatching {
                    channel.map(FileChannel.MapMode.READ_ONLY, 0, size)
                }.getOrNull() ?: run {
                    if (size > 256 * 1024 * 1024) {
                        throw PmdLoadException("Texture too large! Maximum supported is 256M.")
                    }
                    val size = size.toInt()
                    val buffer = ByteBuffer.allocateDirect(size)
                    channel.readAll(buffer)
                    buffer.flip()
                    buffer
                }
            }
            return Texture(
                name = name,
                bufferView = BufferView(
                    buffer = Buffer(
                        name = "Texture $name",
                        buffer = buffer,
                    ),
                    byteLength = buffer.remaining(),
                    byteOffset = 0,
                    byteStride = 0,
                ),
                sampler = Texture.Sampler(),
            )
        }

        private fun Vector3f.invertZ() = also { z = -z }

        private fun loadBones(buffer: ByteBuffer) {
            val boneCount = buffer.getShort()
            if (boneCount < 0) {
                throw PmdLoadException("Bad PMD model: bones count less than zero")
            }

            fun loadBone(buffer: ByteBuffer): PmdBone = PmdBone(
                name = loadString(buffer, 20),
                parentBoneIndex = buffer.getShort().toInt().takeIf { it != -1 },
                tailBoneIndex = buffer.getShort().toInt().takeIf { it != -1 },
                type = buffer.get().toUByte().toInt(),
                targetBoneIndex = buffer.getShort().toInt().takeIf { it != -1 },
                position = loadVector3f(buffer).invertZ(),
            )

            bones = (0 until boneCount).map { index ->
                loadBone(buffer).also { bone ->
                    bone.parentBoneIndex?.let { parentBoneIndex ->
                        childBoneMap.getOrPut(parentBoneIndex) { mutableListOf() }.add(index)
                    } ?: run {
                        rootBones.add(index)
                    }
                }
            }
        }

        fun load(buffer: ByteBuffer): ModelFileLoader.LoadResult {
            val header = loadHeader(buffer)
            loadVertices(buffer)
            loadIndices(buffer)
            loadMaterials(buffer)
            loadBones(buffer)

            val modelId = UUID.randomUUID()
            val rootNodes = mutableListOf<Node>()
            var nextNodeId = 0

            val jointIds = mutableMapOf<Int, NodeId>()
            fun addBone(index: Int, parentPosition: Vector3f? = null): Node {
                val bone = bones[index]
                val nodeIndex = nextNodeId++
                val nodeId = NodeId(modelId, nodeIndex)
                jointIds[index] = nodeId
                val children = childBoneMap[index]?.map { addBone(it, bone.position) } ?: listOf()
                return Node(
                    name = bone.name,
                    id = nodeId,
                    children = children,
                    transform = NodeTransform.Decomposed(
                        translation = Vector3f().set(bone.position).also {
                            if (parentPosition != null) {
                                it.sub(parentPosition)
                            }
                        },
                    )
                )
            }
            rootBones.forEach { index ->
                rootNodes.add(addBone(index))
            }

            val skin = Skin(
                name = "PMX skin",
                joints = (0 until bones.size).map { jointIds[it]!! },
                inverseBindMatrices = bones.map { Matrix4f().translation(it.position).invertAffine() },
                jointHumanoidTags = bones.map { HumanoidTag.fromPmxJapanese(it.name) },
            )

            val vertexBuffer = Buffer(
                name = "Vertex Buffer",
                buffer = vertexBuffer
            )
            val vertexBufferView = BufferView(
                buffer = vertexBuffer,
                byteLength = vertices * VERTEX_ATTRIBUTE_SIZE,
                byteOffset = 0,
                byteStride = VERTEX_ATTRIBUTE_SIZE,
            )
            val indexBuffer = Buffer(
                name = "Index Buffer",
                buffer = indexBuffer,
            )
            val indexBufferView = BufferView(
                buffer = indexBuffer,
                byteLength = indices * 2,
                byteOffset = 0,
                byteStride = 0,
            )

            var indexOffset = 0
            materials.map { pmdMaterial ->
                val nodeId = nextNodeId++
                val material = Material.Unlit(
                    name = null,
                    baseColor = pmdMaterial.diffuseColor,
                    baseColorTexture = pmdMaterial.textureFilename?.let { TextureInfo(loadTexture(it)) },
                    doubleSided = true,
                )
                Node(
                    id = NodeId(modelId, nodeId),
                    skin = skin,
                    mesh = Mesh(
                        id = MeshId(modelId, nodeId),
                        primitives = listOf(
                            Primitive(
                                mode = Primitive.Mode.TRIANGLES,
                                material = material,
                                attributes = Primitive.Attributes.Primitive(
                                    position = Accessor(
                                        bufferView = vertexBufferView,
                                        byteOffset = 0,
                                        componentType = Accessor.ComponentType.FLOAT,
                                        normalized = false,
                                        count = vertices,
                                        type = Accessor.AccessorType.VEC3,
                                    ),
                                    normal = Accessor(
                                        bufferView = vertexBufferView,
                                        byteOffset = 3 * 4,
                                        componentType = Accessor.ComponentType.FLOAT,
                                        normalized = false,
                                        count = vertices,
                                        type = Accessor.AccessorType.VEC3,
                                    ),
                                    texcoords = listOf(
                                        Accessor(
                                            bufferView = vertexBufferView,
                                            byteOffset = (3 + 3) * 4,
                                            componentType = Accessor.ComponentType.FLOAT,
                                            normalized = false,
                                            count = vertices,
                                            type = Accessor.AccessorType.VEC2,
                                        )
                                    ),
                                    joints = listOf(
                                        Accessor(
                                            bufferView = vertexBufferView,
                                            byteOffset = (3 + 3 + 2) * 4,
                                            componentType = Accessor.ComponentType.UNSIGNED_INT,
                                            normalized = false,
                                            count = vertices,
                                            type = Accessor.AccessorType.VEC4,
                                        )
                                    ),
                                    weights = listOf(
                                        Accessor(
                                            bufferView = vertexBufferView,
                                            byteOffset = (3 + 3 + 2 + 4) * 4,
                                            componentType = Accessor.ComponentType.FLOAT,
                                            normalized = false,
                                            count = vertices,
                                            type = Accessor.AccessorType.VEC4,
                                        )
                                    )
                                ),
                                indices = Accessor(
                                    bufferView = indexBufferView,
                                    byteOffset = indexOffset * 2,
                                    componentType = Accessor.ComponentType.UNSIGNED_SHORT,
                                    normalized = false,
                                    count = pmdMaterial.verticesCount,
                                    type = Accessor.AccessorType.SCALAR,
                                ),
                                targets = listOf(),
                            )
                        ),
                        weights = null,
                    )
                ).also {
                    rootNodes.add(it)
                    indexOffset += pmdMaterial.verticesCount
                }
            }

            val scene = Scene(
                nodes = rootNodes,
                initialTransform = NodeTransform.Decomposed(
                    scale = Vector3f(0.1f),
                    rotation = Quaternionf().rotateY(PI.toFloat()),
                ),
            )

            return ModelFileLoader.LoadResult(
                metadata = Metadata(
                    title = header.name,
                    comment = header.comment,
                ),
                model = Model(
                    scenes = listOf(scene),
                    skins = listOf(skin),
                    defaultScene = scene,
                ),
                animations = listOf(),
            )
        }
    }

    override fun load(path: Path, basePath: Path) =
        FileChannel.open(path, StandardOpenOption.READ).use { channel ->
            val fileSize = channel.size()
            val buffer = runCatching {
                channel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize)
            }.getOrNull() ?: run {
                if (fileSize > 32 * 1024 * 1024) {
                    throw PmdLoadException("PMD model size too large: maximum allowed is 32M, current is $fileSize")
                }
                val fileSize = fileSize.toInt()
                val buffer = ByteBuffer.allocate(fileSize)
                channel.readAll(buffer)
                buffer.flip()
                buffer
            }
            val context = Context(basePath)
            buffer.order(ByteOrder.LITTLE_ENDIAN)
            context.load(buffer)
        }
}
