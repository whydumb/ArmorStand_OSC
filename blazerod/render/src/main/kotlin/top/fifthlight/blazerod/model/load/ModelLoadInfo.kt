package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.vertex.VertexFormat
import kotlinx.coroutines.Deferred
import net.minecraft.client.texture.NativeImage
import top.fifthlight.blazerod.model.*
import top.fifthlight.blazerod.model.resource.RenderTexture
import top.fifthlight.blazerod.render.GpuIndexBuffer
import top.fifthlight.blazerod.render.RefCountedGpuBuffer
import java.nio.ByteBuffer

data class TextureLoadData(
    val name: String?,
    val nativeImage: NativeImage,
    val sampler: Texture.Sampler,
) : AutoCloseable by nativeImage

data class IndexBufferLoadData(
    val type: VertexFormat.IndexType,
    val length: Int,
    val buffer: ByteBuffer,
)

sealed class MaterialLoadInfo {
    abstract val name: String?
    abstract val baseColor: RgbaColor
    abstract val baseColorTexture: TextureInfo?
    abstract val alphaMode: Material.AlphaMode
    abstract val alphaCutoff: Float
    abstract val doubleSided: Boolean
    abstract val skinned: Boolean
    abstract val morphed: Boolean

    data class TextureInfo(
        val textureIndex: Int,
        val textureCoordinate: Int,
    )

    data class Pbr(
        override val name: String?,
        override val baseColor: RgbaColor,
        override val baseColorTexture: TextureInfo?,
        val metallicFactor: Float,
        val roughnessFactor: Float,
        val metallicRoughnessTexture: TextureInfo?,
        val normalTexture: TextureInfo?,
        val occlusionTexture: TextureInfo?,
        val emissiveTexture: TextureInfo?,
        val emissiveFactor: RgbColor,
        override val alphaMode: Material.AlphaMode,
        override val alphaCutoff: Float = .5f,
        override val doubleSided: Boolean = false,
        override val skinned: Boolean,
        override val morphed: Boolean,
    ) : MaterialLoadInfo()

    data class Unlit(
        override val name: String?,
        override val baseColor: RgbaColor,
        override val baseColorTexture: TextureInfo?,
        override val alphaMode: Material.AlphaMode,
        override val alphaCutoff: Float,
        override val doubleSided: Boolean,
        override val skinned: Boolean,
        override val morphed: Boolean,
    ) : MaterialLoadInfo()
}

data class PrimitiveLoadInfo(
    val vertices: Int,
    val vertexFormatMode: VertexFormat.DrawMode,
    val materialInfo: MaterialLoadInfo?,
    val indexBufferIndex: Int?,
    val vertexBufferIndex: Int,
)

data class NodeLoadInfo(
    val transform: NodeTransform? = null,
    val components: List<Component> = listOf(),
    val childrenIndices: List<Int> = listOf(),
) {
    sealed class Component {
        data class Primitive(
            val infoIndex: Int,
        ) : Component()
    }
}

data class ModelLoadInfo<Texture : Any?, Index : Any, Vertex : Any>(
    val textures: List<Deferred<Texture>>,
    val indexBuffers: List<Deferred<Index>>,
    val vertexBuffers: List<Deferred<Vertex>>,
    val primitiveInfos: List<PrimitiveLoadInfo>,
    val nodes: List<NodeLoadInfo>,
    val rootNodeIndex: Int,
)

typealias PreProcessModelLoadInfo = ModelLoadInfo<TextureLoadData?, IndexBufferLoadData, ByteBuffer>
typealias GpuLoadModelLoadInfo = ModelLoadInfo<RenderTexture?, GpuIndexBuffer, RefCountedGpuBuffer>
