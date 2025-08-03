package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import kotlinx.coroutines.*
import org.lwjgl.system.MemoryUtil
import top.fifthlight.blazerod.extension.NativeImageExt
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.resource.RenderSkin
import top.fifthlight.blazerod.render.BlazerodVertexFormatElements
import top.fifthlight.blazerod.render.BlazerodVertexFormats
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ModelPreprocessor private constructor(
    private val coroutineScope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val model: Model,
) {
    data class SkinJointData(
        val skinIndex: Int,
        val jointIndex: Int,
        val humanoidTag: HumanoidTag?,
    )

    private lateinit var skinsList: List<RenderSkin>
    private lateinit var skinJointsData: Map<NodeId, List<SkinJointData>>

    private fun loadSkins() {
        val skinJointMap = mutableMapOf<NodeId, MutableList<SkinJointData>>()
        val skinsList = mutableListOf<RenderSkin>()
        for ((index, skin) in model.skins.withIndex()) {
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
        }
        this.skinsList = skinsList
        this.skinJointsData = skinJointMap
    }

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
            skinned = skinned,
            morphed = false,
        )

        is Material.Unlit -> MaterialLoadInfo.Unlit(
            name = material.name,
            baseColor = material.baseColor,
            baseColorTexture = loadTextureInfo(material.baseColorTexture),
            alphaMode = material.alphaMode,
            alphaCutoff = material.alphaCutoff,
            doubleSided = material.doubleSided,
            skinned = skinned,
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
        skinned: Boolean,
        attributes: Primitive.Attributes.Primitive,
    ): Int {
        val vertexFormat = material?.getVertexFormat(skinned) ?: BlazerodVertexFormats.POSITION_TEXTURE_COLOR
        val vertexBuffer = coroutineScope.async(dispatcher) {
            val vertices = attributes.position.count
            val stride = vertexFormat.vertexSize
            val buffer = ByteBuffer.allocateDirect(stride * vertices).order(ByteOrder.nativeOrder())
            val pointer = MemoryUtil.memAddress(buffer)

            for (element in vertexFormat.elements) {
                val dstOffset = vertexFormat.getOffset(element)
                val currentDstStartAddress = pointer + dstOffset

                when (element) {
                    VertexFormatElement.POSITION -> {
                        val srcAttribute = attributes.position
                        VertexLoadUtil.copyAttributeData(
                            vertices = vertices,
                            stride = stride,
                            element = element,
                            normalized = false,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
                    }

                    VertexFormatElement.UV0 -> {
                        val srcAttribute = attributes.texcoords.firstOrNull() ?: continue
                        VertexLoadUtil.copyAttributeData(
                            vertices = vertices,
                            stride = stride,
                            element = element,
                            normalized = false,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
                    }

                    VertexFormatElement.COLOR -> {
                        val srcAttribute = attributes.colors.firstOrNull()
                        if (srcAttribute != null) {
                            VertexLoadUtil.copyAttributeData(
                                vertices = vertices,
                                stride = stride,
                                element = element,
                                normalized = true,
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

                    BlazerodVertexFormatElements.JOINT -> {
                        val srcAttribute = attributes.joints.firstOrNull() ?: continue
                        VertexLoadUtil.copyAttributeData(
                            vertices = vertices,
                            stride = stride,
                            element = element,
                            normalized = false,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
                    }

                    BlazerodVertexFormatElements.WEIGHT -> {
                        val srcAttribute = attributes.weights.firstOrNull() ?: continue
                        VertexLoadUtil.copyAttributeData(
                            vertices = vertices,
                            stride = stride,
                            element = element,
                            normalized = true,
                            srcAttribute = srcAttribute,
                            dstAddress = currentDstStartAddress,
                        )
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
        skinIndex: Int?,
        primitive: Primitive,
    ): PrimitiveLoadInfo? {
        val skinned =
            skinIndex != null && primitive.attributes.joints.isNotEmpty() && primitive.attributes.weights.isNotEmpty()
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
                skinned = skinned,
                morphed = false,
            )
        }
        return PrimitiveLoadInfo(
            vertices = primitive.attributes.position.count,
            vertexFormatMode = vertexFormatMode,
            materialInfo = material,
            indexBufferIndex = primitive.indices?.let { loadIndexBuffer(it).bufferIndex },
            vertexBufferIndex = loadVertexBuffer(
                material = material,
                skinned = skinned,
                attributes = primitive.attributes,
            ),
            skinIndex = skinIndex.takeIf { skinned },
        )
    }

    private val primitiveInfos = mutableListOf<PrimitiveLoadInfo>()
    private fun loadMesh(
        mesh: Mesh,
        skinIndex: Int?,
    ) = mesh.primitives.mapNotNull { primitive ->
        val primitiveInfo = loadPrimitive(
            skinIndex = skinIndex,
            primitive = primitive,
        ) ?: return@mapNotNull null
        val index = primitiveInfos.size
        primitiveInfos.add(primitiveInfo)
        NodeLoadInfo.Component.Primitive(index)
    }

    private val nodes = mutableListOf<NodeLoadInfo>()
    private fun loadNode(node: Node): Int {
        val skinJointData = skinJointsData[node.id]
        val node = NodeLoadInfo(
            nodeId = node.id,
            nodeName = node.name,
            humanoidTags = skinJointData?.mapNotNull { it.humanoidTag } ?: listOf(),
            transform = node.transform,
            components = buildList {
                skinJointData?.forEach { (skinIndex, jointIndex) ->
                    add(
                        NodeLoadInfo.Component.Joint(
                            skinIndex = skinIndex,
                            jointIndex = jointIndex,
                        )
                    )
                }
                node.components.forEach { component ->
                    when (component) {
                        is NodeComponent.MeshComponent -> {
                            val skinIndex = model.skins.indexOf(node.skinComponent?.skin).takeIf { it >= 0 }
                            addAll(
                                loadMesh(
                                    mesh = component.mesh,
                                    skinIndex = skinIndex,
                                )
                            )
                        }

                        else -> {}
                    }
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
            nodeId = null,
            nodeName = "Root node",
            humanoidTags = listOf(),
            transform = scene.initialTransform,
            components = listOf(),
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
            skins = skinsList,
        )
    }

    private fun loadModel(): PreProcessModelLoadInfo? {
        loadSkins()
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