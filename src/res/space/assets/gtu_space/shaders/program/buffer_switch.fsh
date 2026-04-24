#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D DepthSampler;
uniform sampler2D NearSpaceSampler;
uniform sampler2D FarSpaceSampler;
uniform sampler2D DeepSpaceSampler;
uniform sampler2D NearSpaceDepthSampler;
uniform sampler2D FarSpaceDepthSampler;
uniform sampler2D DeepSpaceDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 DiffuseColor  = texture(DiffuseSampler, texCoord);
    vec4 nearColor  = texture(NearSpaceSampler, texCoord);
    vec4 farColor   = texture(FarSpaceSampler, texCoord);
    vec4 deepColor  = texture(DeepSpaceSampler, texCoord);

    vec4 combinedColor = nearColor * nearColor.a + (farColor * farColor.a + deepColor * (1.0 - farColor.a)) * (1.0 - nearColor.a);

    fragColor = mix(DiffuseColor, combinedColor, combinedColor.a);

    float Depth = texture(DepthSampler, texCoord).r;
    float nearDepth = texture(NearSpaceDepthSampler, texCoord).r;
    float farDepth = texture(FarSpaceDepthSampler, texCoord).r;
    float deepDepth = texture(DeepSpaceDepthSampler, texCoord).r;

    farDepth  = (farDepth  != 1.0) ? 0.999999998 : 1;
    deepDepth = (deepDepth != 1.0) ? 0.999999999 : 1;

    float mergedDepth = min(min(nearDepth, min(farDepth, deepDepth)), Depth);

    gl_FragDepth = mergedDepth;
}
