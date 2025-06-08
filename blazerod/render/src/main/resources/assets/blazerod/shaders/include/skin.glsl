#version 150

#ifdef SKINNED
#moj_import <blazerod:joint.glsl>
in ivec4 Joint;
in vec4 Weight;

// Joint matrix buffer
uniform samplerBuffer Joints;

#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE
#ifdef INSTANCED
#define SKIN_INSTANCE_ID gl_InstanceID
#else // INSTANCED
#define SKIN_INSTANCE_ID 0
#endif // INSTANCED

layout(std140) uniform SkinModelIndices {
    int skinJoints;
    int skinModelIndices[INSTANCE_SIZE];
};

#define GET_SKINNED_VERTEX_POSITION(model_view_proj_mat, position) (                                    \
    (model_view_proj_mat)                                                                               \
        * getSkinMatrix(Joints, Weight, Joint + ivec4(skinJoints * skinModelIndices[gl_InstanceID]))    \
        * vec4(position, 1.0)                                                                           \
)

#else // SKINNED

#define GET_SKINNED_VERTEX_POSITION(model_view_proj_mat, position) ((model_view_proj_mat) * vec4(position, 1.0))

#endif // SKINNED
