package org.polaris2023.gtu.space.simulation.earth;

public record EarthProjectedChunkWindow(
        int sourceChunkX,
        int sourceChunkZ,
        int sourceMinBlockX,
        int sourceMaxBlockX,
        int projectedMinBlockX,
        int projectedMaxBlockX
) {
    public int projectedWidthBlocks() {
        return projectedMaxBlockX - projectedMinBlockX + 1;
    }
}
