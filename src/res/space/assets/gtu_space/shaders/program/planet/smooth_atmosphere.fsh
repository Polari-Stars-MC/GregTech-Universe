#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D MainScreenSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 atmosphere = texture(DiffuseSampler, texCoord);
    vec4 mainScreen = texture(MainScreenSampler, texCoord);

    vec2 texelSize = 1.0 / textureSize(DiffuseSampler, 0);
    vec4 smoothed = vec4(0.0);
    float total = 0.0;
    for (int x = -2; x <= 2; x++) {
        for (int y = -2; y <= 2; y++) {
            float w = 1.0 / (1.0 + float(x*x + y*y));
            smoothed += texture(DiffuseSampler, texCoord + vec2(x, y) * texelSize) * w;
            total += w;
        }
    }
    smoothed /= total;

    fragColor = mix(mainScreen, smoothed, smoothed.a);
}
