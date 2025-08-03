package top.fifthlight.blazerod.model.load

import com.mojang.blaze3d.vertex.VertexFormatElement
import org.lwjgl.system.MemoryUtil
import top.fifthlight.blazerod.model.Accessor
import top.fifthlight.blazerod.model.elementLength
import top.fifthlight.blazerod.model.read
import top.fifthlight.blazerod.model.readNormalized
import top.fifthlight.blazerod.model.util.*

object VertexLoadUtil {
    fun copyRawData(
        vertices: Int,
        stride: Int,
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

    fun copyUnnormalizedData(
        stride: Int,
        srcAttribute: Accessor,
        dstAddress: Long,
        targetElement: VertexFormatElement,
    ) {
        require(srcAttribute.type.components == targetElement.count()) { "Copy unnormalized data failed: Source element count (${srcAttribute.type.components}) does not match destination element count (${targetElement.count()})" }
        if (srcAttribute.bufferView == null) {
            return
        }
        var currentDstAddress = dstAddress
        srcAttribute.read { elementBuffer ->
            repeat(srcAttribute.type.components) { component ->
                val dstComponentLength = targetElement.type.size()
                val dstAddress = currentDstAddress + component * dstComponentLength
                when (srcAttribute.componentType) {
                    Accessor.ComponentType.BYTE -> {
                        val value = elementBuffer.get()
                        when (targetElement.type) {
                            VertexFormatElement.Type.UBYTE, VertexFormatElement.Type.BYTE -> MemoryUtil.memPutByte(
                                dstAddress,
                                value
                            )

                            VertexFormatElement.Type.USHORT, VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                dstAddress,
                                value.toShort()
                            )

                            VertexFormatElement.Type.UINT, VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                dstAddress,
                                value.toInt()
                            )

                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(dstAddress, value.toFloat())
                        }
                    }

                    Accessor.ComponentType.UNSIGNED_BYTE -> {
                        val value = elementBuffer.get().toUByte()
                        when (targetElement.type) {
                            VertexFormatElement.Type.UBYTE, VertexFormatElement.Type.BYTE -> MemoryUtil.memPutByte(
                                dstAddress,
                                value.toByte()
                            )

                            VertexFormatElement.Type.USHORT, VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                dstAddress,
                                value.toShort()
                            )

                            VertexFormatElement.Type.UINT, VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                dstAddress,
                                value.toInt()
                            )

                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(dstAddress, value.toFloat())
                        }
                    }

                    Accessor.ComponentType.SHORT -> {
                        val value = elementBuffer.getShort()
                        when (targetElement.type) {
                            VertexFormatElement.Type.UBYTE, VertexFormatElement.Type.BYTE -> {
                                require(value in Byte.MIN_VALUE..Byte.MAX_VALUE) { "Short input cannot be copied to byte output" }
                                MemoryUtil.memPutByte(dstAddress, value.toByte())
                            }

                            VertexFormatElement.Type.USHORT, VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                dstAddress,
                                value
                            )

                            VertexFormatElement.Type.UINT, VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                dstAddress,
                                value.toInt()
                            )

                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(dstAddress, value.toFloat())
                        }
                    }

                    Accessor.ComponentType.UNSIGNED_SHORT -> {
                        val value = elementBuffer.getShort().toUShort()
                        when (targetElement.type) {
                            VertexFormatElement.Type.UBYTE, VertexFormatElement.Type.BYTE -> {
                                require(value in UByte.MIN_VALUE..UByte.MAX_VALUE) { "Short input cannot be copied to byte output" }
                                MemoryUtil.memPutByte(dstAddress, value.toByte())
                            }

                            VertexFormatElement.Type.USHORT, VertexFormatElement.Type.SHORT -> MemoryUtil.memPutShort(
                                dstAddress,
                                value.toShort()
                            )

                            VertexFormatElement.Type.UINT, VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                dstAddress,
                                value.toInt()
                            )

                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(dstAddress, value.toFloat())
                        }
                    }

                    Accessor.ComponentType.UNSIGNED_INT -> {
                        val value = elementBuffer.getInt().toUInt()
                        when (targetElement.type) {
                            VertexFormatElement.Type.UBYTE, VertexFormatElement.Type.BYTE -> {
                                require(value in UByte.MIN_VALUE..UByte.MAX_VALUE) { "Int input cannot be copied to byte output" }
                                MemoryUtil.memPutByte(dstAddress, value.toByte())
                            }

                            VertexFormatElement.Type.USHORT, VertexFormatElement.Type.SHORT -> {
                                require(value in UShort.MIN_VALUE..UShort.MAX_VALUE) { "Int input cannot be copied to Short output" }
                                MemoryUtil.memPutShort(dstAddress, value.toShort())
                            }

                            VertexFormatElement.Type.UINT, VertexFormatElement.Type.INT -> MemoryUtil.memPutInt(
                                dstAddress,
                                value.toInt()
                            )

                            VertexFormatElement.Type.FLOAT -> MemoryUtil.memPutFloat(dstAddress, value.toFloat())
                        }
                    }

                    Accessor.ComponentType.FLOAT -> {
                        require(targetElement.type == VertexFormatElement.Type.FLOAT) { "Float input can only be copied to float output" }
                        MemoryUtil.memPutFloat(dstAddress, elementBuffer.getFloat())
                    }
                }
            }
            currentDstAddress += stride
        }
    }

    fun copyNormalizedData(
        stride: Int,
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
        vertices: Int,
        stride: Int,
        element: VertexFormatElement,
        normalized: Boolean,
        srcAttribute: Accessor,
        dstAddress: Long,
    ) {
        val isFloat =
            srcAttribute.componentType == Accessor.ComponentType.FLOAT && element.type == VertexFormatElement.Type.FLOAT
        require(normalized == srcAttribute.normalized || isFloat) { "Source attribute's normalized ${srcAttribute.normalized} don't match target element $normalized" }
        require(srcAttribute.count == vertices) { "Source attribute's vertex count ${srcAttribute.count} don't match target vertex count $vertices" }
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
                vertices = vertices,
                stride = stride,
                srcAttribute = srcAttribute,
                dstAddress = dstAddress,
                dstLength = dstLength,
            )
        } else if (normalized) {
            val fillAlpha =
                if (element == VertexFormatElement.COLOR && srcAttribute.type.components == 3 && element.count() == 4) {
                    1f
                } else {
                    null
                }

            copyNormalizedData(
                stride = stride,
                srcAttribute = srcAttribute,
                dstAddress = dstAddress,
                targetElementType = element.type,
                componentsToWrite = element.count(),
                fillAlphaValue = fillAlpha,
            )
        } else {
            copyUnnormalizedData(
                stride = stride,
                srcAttribute = srcAttribute,
                dstAddress = dstAddress,
                targetElement = element,
            )
        }
    }
}