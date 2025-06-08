package top.fifthlight.blazerod.helper;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.gl.CompiledShader;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gl.ShaderProgram;
import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.model.VertexType;
import top.fifthlight.blazerod.util.Blaze3DConstsKt;

public class ShaderProgramHelper {
    public static ShaderProgram create(@NotNull CompiledShader vertexShader, @NotNull CompiledShader fragmentShader, @NotNull VertexType type, @NotNull String name) throws ShaderLoader.LoadException {
        var programId = GlStateManager.glCreateProgram();
        if (programId <= 0) {
            throw new ShaderLoader.LoadException("Could not create shader program (returned program ID " + programId + ')');
        } else {
            var elements = type.getElements();
            var elementSize = elements.size();
            for (var i = 0; i < elementSize; i++) {
                GlStateManager._glBindAttribLocation(programId, i, Blaze3DConstsKt.getUsageName(elements.get(i)));
            }

            GlStateManager.glAttachShader(programId, vertexShader.getHandle());
            GlStateManager.glAttachShader(programId, fragmentShader.getHandle());
            GlStateManager.glLinkProgram(programId);
            var linkResult = GlStateManager.glGetProgrami(programId, GlConst.GL_LINK_STATUS);
            if (linkResult == 0) {
                var string = GlStateManager.glGetProgramInfoLog(programId, 32768);
                throw new ShaderLoader.LoadException("Error encountered when linking program containing VS " + vertexShader.getId() + " and FS " + fragmentShader.getId() + ". Log output: " + string);
            } else {
                return new ShaderProgram(programId, name);
            }
        }
    }
}
