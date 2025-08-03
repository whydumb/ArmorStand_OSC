package top.fifthlight.blazerod.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.minecraft.client.render.VertexFormats

object BlazerodVertexFormats {
    val POSITION: VertexFormat = VertexFormats.POSITION

    val POSITION_TEXTURE_COLOR: VertexFormat = VertexFormats.POSITION_TEXTURE_COLOR

    val POSITION_TEXTURE_COLOR_JOINT_WEIGHT: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("Joint", BlazerodVertexFormatElements.JOINT)
        .add("Weight", BlazerodVertexFormatElements.WEIGHT)
        .build();
}