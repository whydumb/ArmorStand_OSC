package top.fifthlight.armorstand.util

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement
import top.fifthlight.renderer.model.Accessor.ComponentType
import top.fifthlight.renderer.model.Primitive

val Primitive.Mode.blaze3d
    get() = when(this) {
        Primitive.Mode.POINTS -> null
        Primitive.Mode.LINE_STRIP -> VertexFormat.DrawMode.LINE_STRIP
        Primitive.Mode.LINE_LOOP -> null
        Primitive.Mode.LINES -> VertexFormat.DrawMode.LINES
        Primitive.Mode.TRIANGLES -> VertexFormat.DrawMode.TRIANGLES
        Primitive.Mode.TRIANGLE_STRIP -> VertexFormat.DrawMode.TRIANGLE_STRIP
        Primitive.Mode.TRIANGLE_FAN -> VertexFormat.DrawMode.TRIANGLE_FAN
    }

val Primitive.Attributes.Key.usageName
    get() = when(this) {
        Primitive.Attributes.Key.POSITION -> "Position"
        Primitive.Attributes.Key.NORMAL -> "Normal"
        Primitive.Attributes.Key.TANGENT -> "Tangent"
        Primitive.Attributes.Key.TEXCOORD -> "UV0"
        Primitive.Attributes.Key.COLORS -> "Color"
        Primitive.Attributes.Key.JOINTS -> "Joint"
        Primitive.Attributes.Key.WEIGHTS -> "Weight"
    }

val ComponentType.blaze3d: VertexFormatElement.Type
    get() = when (this) {
        ComponentType.BYTE -> VertexFormatElement.Type.BYTE
        ComponentType.UNSIGNED_BYTE -> VertexFormatElement.Type.UBYTE
        ComponentType.SHORT -> VertexFormatElement.Type.SHORT
        ComponentType.UNSIGNED_SHORT -> VertexFormatElement.Type.USHORT
        ComponentType.UNSIGNED_INT -> VertexFormatElement.Type.UINT
        ComponentType.FLOAT -> VertexFormatElement.Type.FLOAT
    }