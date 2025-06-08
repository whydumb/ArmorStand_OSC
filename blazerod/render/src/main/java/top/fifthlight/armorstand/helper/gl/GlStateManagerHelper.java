package top.fifthlight.armorstand.helper.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class GlStateManagerHelper {
    public static void _glDeleteVertexArrays(int array) {
        RenderSystem.assertOnRenderThread();
        GL30.glDeleteVertexArrays(array);
    }

    public static void _glCopyBufferSubData(int readTarget, int writeTarget, int readOffset, int writeOffset, int size) {
        RenderSystem.assertOnRenderThread();
        GL31.glCopyBufferSubData(readTarget, writeTarget, readOffset, writeOffset, size);
    }
}
