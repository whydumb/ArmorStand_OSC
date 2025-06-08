package top.fifthlight.blazerod.example.ballblock;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactories;
import top.fifthlight.blazerod.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class BallBlockMod implements ClientModInitializer {
    public static final String MOD_ID = "ball_block";
    private static RenderScene BALL_SCENE = null;
    // Share same instance
    private static ModelInstance BALL_INSTANCE = null;

    private void loadModel() {
        var resource = getClass().getClassLoader().getResource("ball.glb");
        if (resource == null) {
            throw new IllegalStateException("No ball resource");
        }
        URI uri;
        try {
            uri = resource.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        // in case it not loaded
        if ("jar".equals(uri.getScheme())) {
            try {
                //noinspection resource
                FileSystems.newFileSystem(uri, Map.of("create", "true"));
            } catch (FileSystemAlreadyExistsException ignored) {} catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        var file = Path.of(uri);
        var result = ModelFileLoaders.INSTANCE.probeAndLoad(file);
        if (result == null) {
            throw new IllegalStateException("No model loader");
        }
        var model = result.getModel();
        if (model == null) {
            throw new IllegalStateException("Ball model don't contain model");
        }
        var loader = new ModelLoader();
        BALL_SCENE = loader.loadModel(model);
        BALL_SCENE.increaseReferenceCount();
        BALL_INSTANCE = new ModelInstance(BALL_SCENE, ModelBufferManager.INSTANCE.getEntry(BALL_SCENE));
        BALL_INSTANCE.increaseReferenceCount();
        var rootTransformNodeIndex = BALL_SCENE.getRootTransformNodeIndex();
        var rootTransformNode = (RenderNode.Transform) BALL_SCENE.getNodes().get(rootTransformNodeIndex);
        var rootTransformIndex = rootTransformNode.getTransformIndex();
        // Move and scale
        BALL_INSTANCE.setTransformDecomposed(rootTransformIndex, matrix -> {
            matrix.getScale().mul(0.5f);
            matrix.getTranslation().add(0.5f, 0.5f, 0.5f);
        });
        BALL_INSTANCE.update();
    }

    public static ModelInstance getBallInstance() {
        return BALL_INSTANCE;
    }

    @Override
    public void onInitializeClient() {
        ModBlocks.initialize();
        BlockEntityRendererFactories.register(ModBlockEntities.BALL_BLOCK_ENTITY, BallBlockEntityRenderer::new);
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> loadModel());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            BALL_INSTANCE.decreaseReferenceCount();
            BALL_SCENE.decreaseReferenceCount();
        });
    }
}
