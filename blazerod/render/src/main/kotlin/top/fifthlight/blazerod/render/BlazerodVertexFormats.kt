package top.fifthlight.blazerod.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import net.irisshaders.iris.vertices.IrisVertexFormats
import net.minecraft.client.render.VertexFormats

object BlazerodVertexFormats {
    val POSITION: VertexFormat = VertexFormats.POSITION

    val POSITION_COLOR_TEXTURE: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .padding(8)
        .build()

    val POSITION_COLOR_TEXTURE_JOINT_WEIGHT: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("Joint", BlazerodVertexFormatElements.JOINT)
        .add("Weight", BlazerodVertexFormatElements.WEIGHT)
        .build()

    val ENTITY_PADDED: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("Color", VertexFormatElement.COLOR)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV1", VertexFormatElement.UV1)
        .add("UV2", VertexFormatElement.UV2)
        .add("Normal", VertexFormatElement.NORMAL)
        .padding(13)
        .build()

    val IRIS_ENTITY_PADDED: VertexFormat by lazy {
        VertexFormat.builder()
            .add("Position", VertexFormatElement.POSITION)
            .add("Color", VertexFormatElement.COLOR)
            .add("UV0", VertexFormatElement.UV0)
            .add("UV1", VertexFormatElement.UV1)
            .add("UV2", VertexFormatElement.UV2)
            .add("Normal", VertexFormatElement.NORMAL)
            .padding(1) // 36
            .add("iris_Entity", IrisVertexFormats.ENTITY_ID_ELEMENT) // 6
            .padding(2)
            .add("mc_midTexCoord", IrisVertexFormats.MID_TEXTURE_ELEMENT) // 8
            .add("at_tangent", IrisVertexFormats.TANGENT_ELEMENT) // 4
            .padding(8)
            .build()
    }
}