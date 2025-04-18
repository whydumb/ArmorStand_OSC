#version 150

mat4 getJointMatrix(in samplerBuffer joints, int index) {
    int base = index * 4;
    return mat4(
        texelFetch(joints, base),
        texelFetch(joints, base + 1),
        texelFetch(joints, base + 2),
        texelFetch(joints, base + 3)
    );
}

mat4 getSkinMatrix(in samplerBuffer joints, vec4 weight, ivec4 jointIndices) {
    if (weight == vec4(0.0)) {
        return mat4(1.0);
    }
    return weight.x * getJointMatrix(joints, jointIndices.x) +
        weight.y * getJointMatrix(joints, jointIndices.y) +
        weight.z * getJointMatrix(joints, jointIndices.z) +
        weight.w * getJointMatrix(joints, jointIndices.w);
}
