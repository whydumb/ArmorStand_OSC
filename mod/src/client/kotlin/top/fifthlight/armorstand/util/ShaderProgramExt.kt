package top.fifthlight.armorstand.util

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlStateManager
import net.minecraft.client.gl.CompiledShader
import net.minecraft.client.gl.ShaderLoader.LoadException
import net.minecraft.client.gl.ShaderProgram
import top.fifthlight.armorstand.model.VertexType
import kotlin.jvm.Throws

object ShaderProgramExt {
    @JvmStatic
    @Throws(LoadException::class)
    fun create(vertexShader: CompiledShader, fragmentShader: CompiledShader, type: VertexType, name: String): ShaderProgram {
        val programId = GlStateManager.glCreateProgram()
        if (programId <= 0) {
            throw LoadException("Could not create shader program (returned program ID $programId)")
        } else {
            type.elements.forEachIndexed { index, element ->
                GlStateManager._glBindAttribLocation(programId, index, element.usageName)
            }

            GlStateManager.glAttachShader(programId, vertexShader.handle)
            GlStateManager.glAttachShader(programId, fragmentShader.handle)
            GlStateManager.glLinkProgram(programId)
            val linkResult = GlStateManager.glGetProgrami(programId, GlConst.GL_LINK_STATUS)
            if (linkResult == 0) {
                val string = GlStateManager.glGetProgramInfoLog(programId, 32768)
                throw LoadException(
                    "Error encountered when linking program containing VS " + vertexShader.id + " and FS " + fragmentShader.id + ". Log output: " + string
                )
            } else {
                return ShaderProgram(programId, name)
            }
        }
    }
}