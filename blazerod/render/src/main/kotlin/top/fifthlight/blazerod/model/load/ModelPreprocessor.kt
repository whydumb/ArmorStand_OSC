package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import kotlinx.coroutines.*
import net.minecraft.client.render.VertexFormats
import org.lwjgl.system.MemoryUtil
import top.fifthlight.blazerod.extension.NativeImageExt
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelPreprocessor private constructor(
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val model: Model,
) {
    private val textures = mutableListOf<Deferred<TextureLoadData?>>()
    private val textureIndexMap = mutableMapOf<Texture, Int>()
    private fun loadTextureIndex(texture: Texture) = textureIndexMap.getOrPut(texture) {
        val texture = coroutineScope.async(dispatcher) {
            val bufferView = texture.bufferView ?: return@async null
            val byteBuffer = bufferView.buffer.buffer
                .slice(bufferView.byteOffset, bufferView.byteLength)
                .order(ByteOrder.nativeOrder())
            val nativeImage = try {
                NativeImageExt.read(null, texture.type, byteBuffer)
            } catch (ex: Exception) {
                throw Exception("Failed to load texture ${texture.name ?: "unnamed"}", ex)
            }
            TextureLoadData(
                name = texture.name,
                nativeImage = nativeImage,
                sampler = texture.sampler,
            )
        }
        val index = textures.size
        textures.add(texture)
        index
    }

    private fun loadTextureInfo(textureInfo: Material.TextureInfo?) = textureInfo?.let {
        MaterialLoadInfo.TextureInfo(
            textureIndex = loadTextureIndex(textureInfo.texture),
            textureCoordinate = textureInfo.textureCoordinate,
        )
    }

    private fun loadMaterial(
        material: Material,
        skinned: Boolean,
        morphed: Boolean,
    ) = when (material) {
        // TODO: Pbr is not really supported for now
        is Material.Pbr -> MaterialLoadInfo.Unlit(
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = loadTextureInfo(material.baseColorTexture),
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = false,
            morphed = false,
        )

        is Material.Unlit -> MaterialLoadInfo.Unlit(
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = loadTextureInfo(material.baseColorTexture),
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = false,
            morphed = false,
        )
    }

    private data class IndexBufferLoadInfo(
        val type: VertexFormat.IndexType,
        val bufferIndex: Int,
    )

    private val indexBuffers = mutableListOf<Deferred<IndexBufferLoadData>>()
    private fun loadIndexBuffer(accessor: Accessor): IndexBufferLoadInfo {
        check(accessor.type == Accessor.AccessorType.SCALAR) { "Index buffer must be scalar" }
        return when (accessor.componentType) {
            Accessor.ComponentType.UNSIGNED_SHORT, Accessor.ComponentType.UNSIGNED_INT -> {
                val byteBuffer = accessor.readByteBuffer()
                val index = indexBuffers.size
                indexBuffers.add(
                    CompletableDeferred(
                        IndexBufferLoadData(
                            type = when (accessor.componentType) {
                                Accessor.ComponentType.UNSIGNED_SHORT -> VertexFormat.IndexType.SHORT
                                Accessor.ComponentType.UNSIGNED_INT -> VertexFormat.IndexType.INT
                                else -> error("Bad index type: ${accessor.componentType}")
                            },
                            length = accessor.count,
                            buffer = byteBuffer,
                        )
                    )
                )
                IndexBufferLoadInfo(
                    type = when (accessor.componentType) {
                        Accessor.ComponentType.UNSIGNED_SHORT -> VertexFormat.IndexType.SHORT
                        Accessor.ComponentType.UNSIGNED_INT -> VertexFormat.IndexType.INT
                        else -> throw AssertionError()
                    },
                    bufferIndex = index,
                )
            }

            Accessor.ComponentType.UNSIGNED_BYTE -> {
                val loadData = coroutineScope.async(dispatcher) {
                    val byteBuffer =
                        ByteBuffer.allocateDirect(2 * accessor.count).order(ByteOrder.nativeOrder()).apply {
                            accessor.read { input ->
                                val index = input.get().toUByte()
                                putShort(index.toShort())
                            }
                            flip()
                        }
                    IndexBufferLoadData(
                        type = VertexFormat.IndexType.SHORT,
                        length = accessor.count,
                        buffer = byteBuffer,
                    )
                }
                val index = indexBuffers.size
                indexBuffers.add(loadData)
                IndexBufferLoadInfo(
                    type = VertexFormat.IndexType.SHORT,
                    bufferIndex = index,
                )
            }

            else -> throw IllegalArgumentException("Unsupported component type for index: ${accessor.componentType}")
        }
    }

    private val vertexBuffers = mutableListOf<Deferred<ByteBuffer>>()
    private fun loadVertexBuffer(
        material: MaterialLoadInfo?,
        attributes: Primitive.Attributes.Primitive,
    ): Int {
        val vertexFormat = when (material) {
            is MaterialLoadInfo.Pbr -> VertexFormats.POSITION_TEXTURE_COLOR_NORMAL
            is MaterialLoadInfo.Unlit, null -> VertexFormats.POSITION_TEXTURE_COLOR
        }
        val vertexBuffer = coroutineScope.async(dispatcher) {
            val vertices = attributes.position.count
            val stride = vertexFormat.vertexSize
            val buffer = ByteBuffer.allocateDirect(stride * vertices).order(ByteOrder.nativeOrder())
            val pointer = MemoryUtil.memAddress(buffer)

            for (element in vertexFormat.elements) {
                val dstOffset = vertexFormat.getOffset(element)
                val currentDstStartAddress = pointer + dstOffset

                fun copyRawData(
                    srcAttribute: Accessor,
                    dstAddress: Long,
                    dstLength: Long,
                ) {
                    require(srcAttribute.elementLength.toLong() == dstLength) { "Raw copy failed: Source element length (${srcAttribute.elementLength}) does not match destination element length ($dstLength)" }
                    val srcByteBufferView = srcAttribute.bufferView ?: return
                    val srcByteOffset = srcAttribute.byteOffset + srcByteBufferView.byteOffset
                    var currentSrcAddress = MemoryUtil.memAddress(srcByteBufferView.buffer.buffer) + srcByteOffset
                    var currentDstAddress = dstAddress
                    val srcStride =
                        srcByteBufferView.byteStride.toLong().takeIf { it != 0L } ?: srcAttribute.elementLength.toLong()

                    repeat(vertices) {
                        MemoryUtil.memCopy(currentSrcAddress, currentDstAddress, dstLength)
                        currentSrcAddress += srcStride
                        currentDstAddress += stride
                    }
                }

                fun copyNormalizedData(
                    srcAttribute: Accessor,
                    dstAddress: Long,
                    targetElementType: VertexFormatElement.Type,
                    componentsToWrite: Int,
                    fillAlphaValue: Float? = null,
                ) {
                    if (fillAlphaValue != null) {
                        require(srcAttribute.type == Accessor.AccessorType.VEC3 && componentsToWrite == 4) {
                            "Fill alpha value is only supported for vec3 input attributes with 4 output components"
                        }
                    }
                    if (srcAttribute.bufferView == null) {
                        return
                    }

                    var currentDstAddress = dstAddress
                    var dstComponentIndex = 0
                    val byteSizePerComponent = targetElementType.size()

                    srcAttribute.readNormalized { value ->
                        when (targetElementType) {
                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value
                            )

                            VertexFormatElement.Type.UBYTE -> MemoryUtil.memPutByte(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedUByte()
                            )

                            VertexFormatElement.Type.BYTE -> MemoryUtil.memPutByte(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedSByte()
                            )

                            VertexFormatElement.Type.USHORT -> MemoryUtil.memPutShort(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedUShort()
                            )

                            VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedSShort()
                            )

                            VertexFormatElement.Type.UINT -> MemoryUtil.memPutInt(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedUInt()
                            )

                            VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                currentDstAddress + dstComponentIndex * byteSizePerComponent,
                                value.toNormalizedUInt()
                            )
                        }
                        dstComponentIndex++

                        if (dstComponentIndex == 3 && fillAlphaValue != null) {
                            when (targetElementType) {
                                VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue
                                )

                                VertexFormatElement.Type.UBYTE -> MemoryUtil.memPutByte(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedUByte()
                                )

                                VertexFormatElement.Type.BYTE -> MemoryUtil.memPutByte(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedSByte()
                                )

                                VertexFormatElement.Type.USHORT -> MemoryUtil.memPutShort(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedUShort()
                                )

                                VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedSShort()
                                )

                                VertexFormatElement.Type.UINT -> MemoryUtil.memPutInt(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedUInt()
                                )

                                VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                    currentDstAddress + 3 * byteSizePerComponent,
                                    fillAlphaValue.toNormalizedUInt()
                                )
                            }
                            dstComponentIndex++
                        }

                        if (dstComponentIndex == componentsToWrite) {
                            dstComponentIndex = 0
                            currentDstAddress += stride
                        }
                    }
                }

                fun copyAttributeData(
                    element: VertexFormatElement,
                    srcAttribute: Accessor,
                    dstAddress: Long,
                ) {
                    val dstLength = element.byteSize().toLong()
                    if (srcAttribute.bufferView == null) {
                        return
                    }

                    val canCopyRaw = srcAttribute.componentType == Accessor.ComponentType.FLOAT &&
                            !srcAttribute.normalized &&
                            srcAttribute.elementLength.toLong() == dstLength &&
                            element.type == VertexFormatElement.Type.FLOAT

                    if (canCopyRaw) {
                        copyRawData(
                            srcAttribute = srcAttribute,
                            dstAddress = dstAddress,
                            dstLength = dstLength,
                        )
                    } else {
                        val fillAlpha =
                            if (element == VertexFormatElement.COLOR && srcAttribute.type.components == 3 && element.count() == 4) {
                                1f
                            } else {
                                null
                            }

                        copyNormalizedData(
                            srcAttribute = srcAttribute,
                            dstAddress = dstAddress,
                            targetElementType = element.type,
                            componentsToWrite = element.count(),
                            fillAlphaValue = fillAlpha,
                        )
                    }
                }

                when (element) {
                    VertexFormatElement.POSITION -> {
                        val srcAttribute = attributes.position
                        copyAttributeData(
                            element = element,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
                    }

                    VertexFormatElement.UV0 -> {
                        val srcAttribute = attributes.texcoords.firstOrNull() ?: continue
                        copyAttributeData(
                            element = element,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
                    }

                    VertexFormatElement.COLOR -> {
                        val srcAttribute = attributes.colors.firstOrNull()
                        if (srcAttribute != null) {
                            copyAttributeData(
                                element = element,
                                srcAttribute = srcAttribute,
                                dstAddress = currentDstStartAddress,
                            )
                        } else {
                            var currentVertexDstAddress = currentDstStartAddress
                            repeat(vertices) {
                                MemoryUtil.memPutByte(currentVertexDstAddress, 0xFF.toByte())
                                MemoryUtil.memPutByte(currentVertexDstAddress + 1, 0xFF.toByte())
                                MemoryUtil.memPutByte(currentVertexDstAddress + 2, 0xFF.toByte())
                                MemoryUtil.memPutByte(currentVertexDstAddress + 3, 0xFF.toByte())
                                currentVertexDstAddress += stride
                            }
                        }
                    }

                    else -> {}
                }
            }

            buffer
        }
        val index = vertexBuffers.size
        vertexBuffers.add(vertexBuffer)
        return index
    }

    private fun loadPrimitive(
        primitive: Primitive,
    ): PrimitiveLoadInfo? {
        val vertexFormatMode = when (primitive.mode) {
            Primitive.Mode.POINTS -> return null
            Primitive.Mode.LINE_STRIP -> VertexFormat.DrawMode.LINE_STRIP
            Primitive.Mode.LINE_LOOP -> return null
            Primitive.Mode.LINES -> VertexFormat.DrawMode.LINES
            Primitive.Mode.TRIANGLES -> VertexFormat.DrawMode.TRIANGLES
            Primitive.Mode.TRIANGLE_STRIP -> VertexFormat.DrawMode.TRIANGLE_STRIP
            Primitive.Mode.TRIANGLE_FAN -> VertexFormat.DrawMode.TRIANGLE_FAN
        }
        val material = primitive.material?.let {
            loadMaterial(
                material = it,
                skinned = false,
                morphed = false,
            )
        }
        return PrimitiveLoadInfo(
            vertices = primitive.attributes.position.count,
            vertexFormatMode = vertexFormatMode,
            materialInfo = material,
            indexBufferIndex = primitive.indices?.let { loadIndexBuffer(it).bufferIndex },
            vertexBufferIndex = loadVertexBuffer(material, primitive.attributes),
        )
    }

    private val primitiveInfos = mutableListOf<PrimitiveLoadInfo>()
    private fun loadMesh(mesh: Mesh) = mesh.primitives.mapNotNull { primitive ->
        val primitiveInfo = loadPrimitive(primitive) ?: return@mapNotNull null
        val index = primitiveInfos.size
        primitiveInfos.add(primitiveInfo)
        NodeLoadInfo.Component.Primitive(index)
    }

    private val nodes = mutableListOf<NodeLoadInfo>()
    private fun loadNode(node: Node): Int {
        val node = NodeLoadInfo(
            transform = node.transform,
            components = node.components.flatMap { component ->
                when (component) {
                    is NodeComponent.MeshComponent -> loadMesh(component.mesh)
                    else -> listOf()
                }
            },
            childrenIndices = node.children.map { loadNode(it) },
        )
        val nodeIndex = nodes.size
        nodes.add(node)
        return nodeIndex
    }

    private fun loadScene(scene: Scene): PreProcessModelLoadInfo? {
        val rootNode = NodeLoadInfo(
            transform = scene.initialTransform,
            childrenIndices = scene.nodes.map { loadNode(it) },
        )
        val rootNodeIndex = nodes.size
        nodes.add(rootNode)
        return PreProcessModelLoadInfo(
            textures = textures,
            indexBuffers = indexBuffers,
            vertexBuffers = vertexBuffers,
            primitiveInfos = primitiveInfos,
            nodes = nodes,
            rootNodeIndex = rootNodeIndex,
        )
    }

    private fun loadModel(): PreProcessModelLoadInfo? {
        val scene = model.defaultScene ?: model.scenes.firstOrNull() ?: return null
        return loadScene(scene)
    }

    companion object {
        fun preprocess(
            scope: CoroutineScope,
            dispatcher: CoroutineDispatcher,
            model: Model,
        ) = ModelPreprocessor(scope, dispatcher, model).loadModel()
    }
}