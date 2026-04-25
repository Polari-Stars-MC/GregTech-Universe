package org.polaris2023.gtu.core.api.multiblock.runtime.check;

public final class CheckNodeFlags {
    public static final byte CRITICAL = 1;
    public static final byte SAME_CHUNK = 1 << 1;
    public static final byte FRAME = 1 << 2;
    public static final byte IO = 1 << 3;

    private CheckNodeFlags() {
    }

    public static boolean hasFlag(byte flags, byte flag) {
        return (flags & flag) == flag;
    }
}
