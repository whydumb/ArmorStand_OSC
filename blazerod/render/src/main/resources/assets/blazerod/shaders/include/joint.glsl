#blazerod_version version(<4.3) ? 150 : 430
#blazerod_extension version(<4.3) && defined(SUPPORT_SSBO); ARB_shader_storage_buffer_object : require

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

mat4 getSkinMatrix(vec4 weight, ivec4 joint_indices) {
    if (weight == vec4(0.0)) {
        return mat4(1.0);
    }
    return weight.x * getJointMatrix(joint_indices.x) +
    weight.y * getJointMatrix(joint_indices.y) +
    weight.z * getJointMatrix(joint_indices.z) +
    weight.w * getJointMatrix(joint_indices.w);
}