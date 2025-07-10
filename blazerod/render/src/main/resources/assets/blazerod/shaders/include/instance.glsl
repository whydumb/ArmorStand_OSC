#version 150

#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE
#ifdef INSTANCED
#define INSTANCE_ID gl_InstanceID
#else // INSTANCED
#define INSTANCE_ID 0
#endif // INSTANCED

// mat4 LocalMatrices[PrimitiveSize][INSTANCE_SIZE]
uniform samplerBuffer LocalMatrices;

layout (std140) uniform InstanceData {
    int PrimitiveSize;
    int PrimitiveIndex;
    mat4 ModelViewMatrices[INSTANCE_SIZE];
    ivec2 LightMapUvs[INSTANCE_SIZE];
};

struct instance_t {
    mat4 model_view_mat;
    ivec2 light_map_uv;
};

instance_t get_instance() {
    instance_t instance;
    int matricesOffset = INSTANCE_ID * PrimitiveSize + PrimitiveIndex;
    mat4 local_matrix = mat4(
        texelFetch(LocalMatrices, matricesOffset * 4 + 0),
        texelFetch(LocalMatrices, matricesOffset * 4 + 1),
        texelFetch(LocalMatrices, matricesOffset * 4 + 2),
        texelFetch(LocalMatrices, matricesOffset * 4 + 3)
    );
    instance.model_view_mat = ModelViewMatrices[INSTANCE_ID] * local_matrix;
    instance.light_map_uv = LightMapUvs[INSTANCE_ID];
    return instance;
}
