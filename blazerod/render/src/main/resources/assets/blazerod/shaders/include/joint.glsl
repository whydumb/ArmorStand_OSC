#blazerod_version version(<4.3) ? 150 : 430
#blazerod_extension version(<4.3) && defined(SUPPORT_SSBO); GL_ARB_shader_storage_buffer_object : require

#ifdef SUPPORT_SSBO
layout(std430) buffer JointsData {
    mat4 Joints[];
};
#else// SUPPORT_SSBO
uniform samplerBuffer Joints;
#endif// SUPPORT_SSBO

mat4 getJointMatrix(int index) {
    #ifdef SUPPORT_SSBO
    return Joints[index];
    #else// SUPPORT_SSBO
    int base = index * 4;
    return mat4(
    texelFetch(Joints, base),
    texelFetch(Joints, base + 1),
    texelFetch(Joints, base + 2),
    texelFetch(Joints, base + 3)
    );
    #endif// SUPPORT_SSBO
}

vec4 skinTransform(vec4 position, vec4 weight, ivec4 joint_indices) {
    if (weight == vec4(0.0)) {
        return position;
    }
    vec4 posX = getJointMatrix(joint_indices.x) * position;
    vec4 posY = getJointMatrix(joint_indices.y) * position;
    vec4 posZ = getJointMatrix(joint_indices.z) * position;
    vec4 posW = getJointMatrix(joint_indices.w) * position;
    return posX * weight.x + posY * weight.y + posZ * weight.z + posW * weight.w;
}