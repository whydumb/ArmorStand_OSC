package top.fifthlight.blazerod.extension;

import org.jetbrains.annotations.NotNull;
import top.fifthlight.blazerod.model.resource.VertexType;

public interface RenderPipelineBuilderExt {
    void blazerod$withVertexType(@NotNull VertexType type);

    void blazerod$withStorageBuffer(@NotNull String name);
}
