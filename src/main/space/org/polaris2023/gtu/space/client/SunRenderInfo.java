package org.polaris2023.gtu.space.client;

import org.joml.Vector3d;

public record SunRenderInfo(
        String id,
        Vector3d worldPosition,
        float intensity,
        float red,
        float green,
        float blue
) {
}
