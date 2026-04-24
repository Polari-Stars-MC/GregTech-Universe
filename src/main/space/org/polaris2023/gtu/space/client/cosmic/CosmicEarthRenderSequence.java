package org.polaris2023.gtu.space.client.cosmic;

import org.joml.Vector3f;
import org.polaris2023.gtu.space.client.cosmic.ch.InitialiseRenderSequenceProcedure;

/**
 * CH-style render sequence initializer for the Earth cube.
 * This prepares light_data / alpha_data / i_alpha_data / cloud_data each frame.
 */
public final class CosmicEarthRenderSequence {
    private CosmicEarthRenderSequence() {
    }

    public static CosmicEarthRenderData build(Vector3f lightLocal, Vector3f viewLocal) {
        return InitialiseRenderSequenceProcedure.execute(lightLocal, viewLocal);
    }
}
