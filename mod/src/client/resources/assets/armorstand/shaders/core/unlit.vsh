#version 150

#define INSTANCE_STRUCT_ITEMS \
    ITEM_CONV(ivec3, LightMapUv, ivec2, light_map_uv, xy)

#moj_import <armorstand:instance.glsl>
#moj_import <armorstand:skin.glsl>
#moj_import <armorstand:morph.glsl>
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

void main() {
    instance_t instance = get_instance();

    vec3 position = GET_MORPHED_VERTEX_POSITION(Position);
    vec4 color = GET_MORPHED_VERTEX_COLOR(Color);
    vec2 texCoord = GET_MORPHED_VERTEX_TEX_COORD(UV0);

    vec4 vertex_position = GET_SKINNED_VERTEX_POSITION(instance.model_view_proj_mat, position);

    gl_Position = vertex_position;
    vertexDistance = fog_distance(vertex_position.xyz, FogShape);
    vertexColor = color;
    lightMapColor = texelFetch(SamplerLightMap, instance.light_map_uv / 16, 0);
    texCoord0 = texCoord;
}
