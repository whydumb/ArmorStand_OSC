package top.fifthlight.armorstand.helper.gl;

import com.mojang.blaze3d.opengl.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class GlStateManagerHelper {
    public static void _bindTexture(int target, int texture) {
        RenderSystem.assertOnRenderThread();
        if (texture != GlStateManager.TEXTURES[GlStateManager.activeTexture].boundTexture) {
            GlStateManager.TEXTURES[GlStateManager.activeTexture].boundTexture = texture;
            GL11.glBindTexture(target, texture);
        }
    }

    public static void _glDeleteVertexArrays(int array) {
        RenderSystem.assertOnRenderThread();
        GL30.glDeleteVertexArrays(array);
    }

    public static void _glTexBuffer(int target, int internalFormat, int buffer) {
        RenderSystem.assertOnRenderThread();
        GL31.glTexBuffer(target, internalFormat, buffer);
    }

    public static void _drawElementsInstanced(int mode, int type, int count, long indices, int instanceCount) {
        RenderSystem.assertOnRenderThread();
        GL31.glDrawElementsInstanced(mode, type, count, indices, instanceCount);
    }

    public static void _drawArraysInstanced(int mode, int first, int count, int instanceCount) {
        RenderSystem.assertOnRenderThread();
        GL31.glDrawArraysInstanced(mode, first, count, instanceCount);
    }
}
