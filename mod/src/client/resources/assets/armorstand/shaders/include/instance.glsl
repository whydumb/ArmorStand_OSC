struct instance_t {
    mat4 model_view_proj_mat;
#ifdef INSTANCE_STRUCT_ITEMS
#define ITEM_CONV(ut, un, st, sn, conv) st sn;
#define ITEM(st, sn, un) st sn;
    INSTANCE_STRUCT_ITEMS
#undef ITEM_CONV
#undef ITEM
#endif // INSTANCE_STRUCT_ITEMS
};

#ifdef INSTANCED
#ifndef INSTANCE_SIZE
#error "INSTANCE_SIZE not defined"
#endif // INSTANCE_SIZE

layout (std140) uniform Instances {
    instance_t[INSTANCE_SIZE] instances;
} instances;

instance_t get_instance() {
    return instances.instances[gl_InstanceID];
}

#else // INSTANCED

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

#ifdef INSTANCE_STRUCT_ITEMS
#define ITEM_CONV(ut, un, st, sn, conv) uniform ut un;
#define ITEM(st, sn, un) uniform st un;
INSTANCE_STRUCT_ITEMS
#undef ITEM_CONV
#undef ITEM
#endif // INSTANCE_STRUCT_ITEMS

instance_t get_instance() {
    instance_t instance;
    instance.model_view_proj_mat = ProjMat * ModelViewMat;

#ifdef INSTANCE_STRUCT_ITEMS
#define ITEM_CONV(ut, un, st, sn, conv) instance.sn = un.conv;
#define ITEM(st, sn, un) instance.sn = un;
    INSTANCE_STRUCT_ITEMS
#undef ITEM_CONV
#undef ITEM
#endif // INSTANCE_STRUCT_ITEMS

    return instance;
}

#endif // INSTANCED

#ifdef SKINNED
#ifdef INSTANCED
uniform int TotalJoints;
#define JOINT_INDEX(offset) ((offset) + ivec4(gl_InstanceID * TotalJoints))
#else // INSTANCED
#define JOINT_INDEX(offset) (offset)
#endif // INSTANCED
#endif // SKINNED
