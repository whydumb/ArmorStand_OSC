#ifdef MORPHED

#ifndef MAX_ENABLED_MORPH_TARGETS
#error "MAX_ENABLED_MORPH_TARGETS not defined"
#endif

// Same for every instances
uniform int TotalVertices;
uniform samplerBuffer MorphPositionData;
uniform samplerBuffer MorphColorData;
uniform samplerBuffer MorphTexCoordData;
// pos, color, total, for all targets (include not-enabled, so every instance is same)
uniform ivec3 MorphTargetSizes;

#ifdef INSTANCED
#define MORPH_INSTANCE_ID gl_InstanceID
#else // INSTANCED
#define MORPH_INSTANCE_ID 0
#endif

// Instance #0             Instance #1
// (pos, color, texCoord)  (pos, color, texCoord)
// index for all targets (include not-enabled)
uniform samplerBuffer MorphWeights;

float fetchPositionWeight(int index) {
    return texelFetch(MorphWeights, MORPH_INSTANCE_ID * MorphTargetSizes.z + index).x;
}

float fetchColorWeight(int index) {
    return texelFetch(MorphWeights, MORPH_INSTANCE_ID * MorphTargetSizes.z + MorphTargetSizes.x + index).x;
}

float fetchTexCoordWeight(int index) {
    return texelFetch(MorphWeights, MORPH_INSTANCE_ID * MorphTargetSizes.z + MorphTargetSizes.x + MorphTargetSizes.y + index).x;
}

// ivec3 has padding of 4 bytes, should be handled in user program
// indices are for Morph*Data and MorphWeights
struct morph_indices_t {
    ivec3 count;
    ivec3 indices[MAX_ENABLED_MORPH_TARGETS];
};

layout(std140) uniform MorphIndices {
#ifdef INSTANCED
#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE
    morph_indices_t indices[INSTANCE_SIZE];
#else // INSTANCED
    morph_indices_t indices[1];
#endif // INSTANCED
} morphIndices;

vec3 applyPositionMorph(vec3 basePos) {
    vec3 delta = vec3(0.0);
    for(int i = 0; i < morphIndices.indices[MORPH_INSTANCE_ID].count.x; i++) {
        int index = morphIndices.indices[MORPH_INSTANCE_ID].indices[i].x;
        float weight = fetchPositionWeight(index);
        delta += texelFetch(MorphPositionData, gl_VertexID + index * TotalVertices).xyz * weight;
    }
    return basePos + delta;
}

vec4 applyColorMorph(vec4 baseColor) {
    vec4 delta = vec4(0.0);
    for(int i = 0; i < morphIndices.indices[MORPH_INSTANCE_ID].count.y; i++) {
        int index = morphIndices.indices[MORPH_INSTANCE_ID].indices[i].y;
        float weight = fetchColorWeight(index);
        delta += texelFetch(MorphColorData, gl_VertexID + index * TotalVertices) * weight;
    }
    return baseColor + delta;
}

vec2 applyTexCoordMorph(vec2 baseUV) {
    vec2 delta = vec2(0.0);
    for(int i = 0; i < morphIndices.indices[MORPH_INSTANCE_ID].count.z; i++) {
        int index = morphIndices.indices[MORPH_INSTANCE_ID].indices[i].z;
        float weight = fetchTexCoordWeight(index);
        delta += texelFetch(MorphTexCoordData, gl_VertexID + index * TotalVertices).xy * weight;
    }
    return baseUV + delta;
}

#define GET_MORPHED_VERTEX_POSITION(position) applyPositionMorph(position)
#define GET_MORPHED_VERTEX_COLOR(color) applyColorMorph(color)
#define GET_MORPHED_VERTEX_TEX_COORD(texCoord) applyTexCoordMorph(texCoord)

#else // MORPHED

#define GET_MORPHED_VERTEX_POSITION(position) (position)
#define GET_MORPHED_VERTEX_COLOR(color) (color)
#define GET_MORPHED_VERTEX_TEX_COORD(texCoord) (texCoord)

#endif // MORPHED
