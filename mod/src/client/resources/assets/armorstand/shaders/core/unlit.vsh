#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
// Texture UV
in vec2 UV0;

// Lightmap texture
uniform sampler2D SamplerLightMap;

uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;

#ifdef SKINNED
#moj_import <armorstand:joint.glsl>
in ivec4 Joint;
in vec4 Weight;

// Joint matrix buffer
uniform samplerBuffer Joints;
#endif

struct instance_t {
    mat4 model_view_proj_mat;
    ivec2 light_map_uv;
};

#ifdef INSTANCED

#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif

#ifdef SKINNED
uniform int ModelJoints;
#endif

layout (std140) uniform Instances {
    instance_t[INSTANCE_SIZE] instances;
};

instance_t get_instance() {
    return instances[gl_InstanceID];
}

#else

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;
uniform ivec3 LightMapUv;

instance_t get_instance() {
    instance_t instance;
    instance.model_view_proj_mat = ProjMat * ModelViewMat;
    instance.light_map_uv = LightMapUv.xy;
    return instance;
}

#endif

void main() {
    instance_t instance = get_instance();

    #ifdef SKINNED
    ivec4 joint_index =
    #ifdef INSTANCED
        Joint + ivec4(gl_InstanceID * ModelJoints)
    #else
        Joint
    #endif
    #endif
    ;

    gl_Position = instance.model_view_proj_mat
    #ifdef SKINNED
        * getSkinMatrix(Joints, Weight, joint_index)
    #endif
        * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = Color;
    lightMapColor = texelFetch(SamplerLightMap, instance.light_map_uv / 16, 0);

    texCoord0 = UV0;
}
