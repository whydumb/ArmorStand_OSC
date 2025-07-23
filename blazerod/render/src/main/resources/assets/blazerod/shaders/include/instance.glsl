#blazerod_version version(<4.3) ? 150 : 430
#blazerod_extension version(<4.3) && defined(SUPPORT_SSBO); ARB_shader_storage_buffer_object : require

#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE
#ifdef INSTANCED
#define INSTANCE_ID gl_InstanceID
#else // INSTANCED
#define INSTANCE_ID 0
#endif // INSTANCED

// mat4 LocalMatrices[PrimitiveSize][INSTANCE_SIZE]
#ifdef SUPPORT_SSBO
layout(std430) buffer LocalMatricesData {
    mat4 LocalMatrices[];
};
#else// SUPPORT_SSBO
uniform samplerBuffer LocalMatrices;// TBO declaration
#endif// SUPPORT_SSBO

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
    mat4 local_matrix =
    #ifdef SUPPORT_SSBO
    LocalMatrices[matricesOffset];
    #else// SUPPORT_SSBO
    mat4(
    texelFetch(LocalMatrices, matricesOffset * 4 + 0),
    texelFetch(LocalMatrices, matricesOffset * 4 + 1),
    texelFetch(LocalMatrices, matricesOffset * 4 + 2),
    texelFetch(LocalMatrices, matricesOffset * 4 + 3)
    );
    #endif// SUPPORT_SSBO
    instance.model_view_mat = ModelViewMatrices[INSTANCE_ID] * local_matrix;
    instance.light_map_uv = LightMapUvs[INSTANCE_ID];
    return instance;
}