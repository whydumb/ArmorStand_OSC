#ifdef SKINNED
#moj_import <armorstand:joint.glsl>
in ivec4 Joint;
in vec4 Weight;

// Joint matrix buffer
uniform samplerBuffer Joints;
#endif

#ifdef SKINNED
#define GET_SKINNED_VERTEX_POSITION(model_view_proj_mat, position) ((model_view_proj_mat) * getSkinMatrix(Joints, Weight, JOINT_INDEX(Joint)) * vec4(position, 1.0))
#else
#define GET_SKINNED_VERTEX_POSITION(model_view_proj_mat, position) ((model_view_proj_mat) * vec4(position, 1.0))
#endif
