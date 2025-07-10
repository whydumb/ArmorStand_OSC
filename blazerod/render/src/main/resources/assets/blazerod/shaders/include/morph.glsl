#version 150

#ifdef MORPHED

#ifndef MAX_ENABLED_MORPH_TARGETS
#error "MAX_ENABLED_MORPH_TARGETS not defined"
#endif

// Same for every instances
uniform samplerBuffer MorphPositionData;
uniform samplerBuffer MorphColorData;
uniform samplerBuffer MorphTexCoordData;

// for all targets (include not-enabled, so every instance is same)
layout(std140) uniform MorphData {
    int TotalVertices;
    int PosTargets;
    int ColorTargets;
    int TexCoordTargets;
    int TotalTargets;
};

// index for all targets (include not-enabled)
// struct model_weight_t {
//     float position[PosTargets];
//     float color[ColorTargets];
//     float texCoord[TexCoordTargets];
// }
// MorphWeights = model_weight_t[MODEL_SIZE]
uniform samplerBuffer MorphWeights;

float fetchPositionWeight(int modelIndex, int targetIndex) {
    return texelFetch(MorphWeights, modelIndex * TotalTargets + targetIndex).x;
}

float fetchColorWeight(int modelIndex, int targetIndex) {
    return texelFetch(MorphWeights, modelIndex * TotalTargets + PosTargets + targetIndex).x;
}

float fetchTexCoordWeight(int modelIndex, int targetIndex) {
    return texelFetch(MorphWeights, modelIndex * TotalTargets + PosTargets + ColorTargets + targetIndex).x;
}

// struct index_t {
//     int pos;
//     int color;
//     int texCoord;
// }
// struct model_item_t {
//     int posCount;
//     int colorCount;
//     int texCoordCount;
//     index_t indices[MAX_ENABLED_MORPH_TARGETS];
// }
// MorphTargetIndices = model_item_t[MODEL_SIZE]
uniform isamplerBuffer MorphTargetIndices;

#define MODEL_ITEM_SIZE (3 + 3 * (MAX_ENABLED_MORPH_TARGETS))

int fetchModelTargetSize(int modelIndex, int offset) {
    return texelFetch(MorphTargetIndices, modelIndex * MODEL_ITEM_SIZE + offset).x;
}

int fetchModelTargetIndex(int modelIndex, int index, int offset) {
    return texelFetch(MorphTargetIndices, modelIndex * MODEL_ITEM_SIZE + 3 + 3 * index + offset).x;
}

#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE
#ifdef INSTANCED
#define MORPH_INSTANCE_ID gl_InstanceID
#else // INSTANCED
#define MORPH_INSTANCE_ID 0
#endif // INSTANCED

vec3 applyPositionMorph(vec3 basePos) {
    vec3 delta = vec3(0.0);
    int targetSize = fetchModelTargetSize(MORPH_INSTANCE_ID, 0);
    for(int i = 0; i < MAX_ENABLED_MORPH_TARGETS; i++) {
        if (i >= targetSize) {
            break;
        }
        int targetIndex = fetchModelTargetIndex(MORPH_INSTANCE_ID, i, 0);
        float weight = fetchPositionWeight(MORPH_INSTANCE_ID, targetIndex);
        delta += texelFetch(MorphPositionData, gl_VertexID + targetIndex * TotalVertices).xyz * weight;
    }
    return basePos + delta;
}

vec4 applyColorMorph(vec4 baseColor) {
    vec4 delta = vec4(0.0);
    int targetSize = fetchModelTargetSize(MORPH_INSTANCE_ID, 1);
    for(int i = 0; i < MAX_ENABLED_MORPH_TARGETS; i++) {
        if (i >= targetSize) {
            break;
        }
        int targetIndex = fetchModelTargetIndex(MORPH_INSTANCE_ID, i, 1);
        float weight = fetchColorWeight(MORPH_INSTANCE_ID, targetIndex);
        delta += texelFetch(MorphColorData, gl_VertexID + targetIndex * TotalVertices) * weight;
    }
    return baseColor + delta;
}

vec2 applyTexCoordMorph(vec2 baseUV) {
    vec2 delta = vec2(0.0);
    int targetSize = fetchModelTargetSize(MORPH_INSTANCE_ID, 2);
    for(int i = 0; i < MAX_ENABLED_MORPH_TARGETS; i++) {
        if (i >= targetSize) {
            break;
        }
        int targetIndex = fetchModelTargetIndex(MORPH_INSTANCE_ID, i, 2);
        float weight = fetchTexCoordWeight(MORPH_INSTANCE_ID, targetIndex);
        delta += texelFetch(MorphTexCoordData, gl_VertexID + targetIndex * TotalVertices).xy * weight;
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
