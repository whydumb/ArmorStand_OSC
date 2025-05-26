package top.fifthlight.renderer.model.util

import top.fifthlight.renderer.model.ByteBufferUtil
import java.nio.ByteBuffer

// Workaround of Bazel rules-kotlin using JDK11 as build JDK
fun ByteBuffer.sliceWorkaround(index: Int, length: Int) = ByteBufferUtil.slice(this, index, length)
fun ByteBuffer.putWorkaround(index: Int, src: ByteBuffer, offset: Int, length: Int) =
    ByteBufferUtil.put(this, index, src, offset, length)
