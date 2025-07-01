package top.fifthlight.blazerod.helper.gl;

import com.mojang.blaze3d.systems.RenderSystem;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL31;

public class GlStateManagerHelper {
    public static void _glDeleteVertexArrays(int array) {
        RenderSystem.assertOnRenderThread();
        GL30.glDeleteVertexArrays(array);
    }
}
