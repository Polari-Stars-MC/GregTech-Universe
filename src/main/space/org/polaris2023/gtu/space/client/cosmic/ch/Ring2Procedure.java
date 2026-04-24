package org.polaris2023.gtu.space.client.cosmic.ch;

import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Direct geometry/UV port of Cosmic Horizons' Ring2Procedure.
 */
public final class Ring2Procedure {
    private Ring2Procedure() {
    }

    public static RingFace execute() {
        return new RingFace(
                new Vector3f[]{
                        new Vector3f(0.25F, 0.0F, -0.25F),
                        new Vector3f(0.5F, 0.0F, -0.5F),
                        new Vector3f(0.5F, 0.0F, 0.5F),
                        new Vector3f(0.25F, 0.0F, 0.25F)
                },
                new Vector2f[]{
                        new Vector2f(0.25F, 1.0F),
                        new Vector2f(0.0F, 0.0F),
                        new Vector2f(1.0F, 0.0F),
                        new Vector2f(0.75F, 1.0F)
                }
        );
    }

    public record RingFace(Vector3f[] vertices, Vector2f[] uvs) {
    }
}
