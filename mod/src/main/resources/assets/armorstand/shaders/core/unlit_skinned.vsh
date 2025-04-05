#version 150

#moj_import <minecraft:fog.glsl>
#moj_import <armorstand:joint.glsl>

in vec3 Position;
in vec4 Color;
// Texture UV
in vec2 UV0;
in ivec4 Joint;
in vec4 Weight;

// Lightmap texture
uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

// Lightmap UV
uniform ivec3 LightMapUv;

out float vertexDistance;
out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;

void main() {
    mat4 skinMatrix = getSkinMatrix(Weight, Joint);
    gl_Position = ProjMat * ModelViewMat * skinMatrix * vec4(Position, 1.0);

    vertexDistance = fog_distance(Position, FogShape);
    vertexColor = Color;
    lightMapColor = texelFetch(Sampler2, LightMapUv.xy / 16, 0);

    texCoord0 = UV0;
}
