#version 150

in vec4 vertexColor;
in vec2 portalUV;

uniform float GameTime;

out vec4 fragColor;

// Simple hash-based noise
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {
    float time = GameTime * 1200.0;
    vec2 uv = portalUV;

    // Animated noise distortion
    float n = noise(uv * 8.0 + time * 0.3) * 0.5
            + noise(uv * 16.0 - time * 0.2) * 0.25
            + noise(uv * 32.0 + time * 0.5) * 0.125;

    // Edge glow: stronger near portal border
    vec2 centered = fract(uv) * 2.0 - 1.0;
    float edgeDist = max(abs(centered.x), abs(centered.y));
    float edgeGlow = smoothstep(0.6, 1.0, edgeDist) * 0.5;

    vec4 color = vertexColor;
    color.rgb += vec3(0.2, 0.4, 0.9) * n * 0.4;
    color.rgb += vec3(0.3, 0.5, 1.0) * edgeGlow;
    color.a *= 0.7 + n * 0.3;

    fragColor = color;
}
