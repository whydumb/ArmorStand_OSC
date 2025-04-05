#version 150

// Joint matrix buffer
uniform samplerBuffer Joints;

mat4 getJointMatrix(int index) {
    int base = index * 4;
    return mat4(
        texelFetch(Joints, base),
        texelFetch(Joints, base + 1),
        texelFetch(Joints, base + 2),
        texelFetch(Joints, base + 3)
    );
}

mat4 getSkinMatrix(vec4 weight, ivec4 jointIndices) {
    return weight.x * getJointMatrix(jointIndices.x) +
        weight.y * getJointMatrix(jointIndices.y) +
        weight.z * getJointMatrix(jointIndices.z) +
        weight.w * getJointMatrix(jointIndices.w);
}
