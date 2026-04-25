package org.polaris2023.gtu.core.api.multiblock.network;

import com.mojang.serialization.Codec;

public enum StructureMemberType {
    CONTROLLER,
    INPUT,
    OUTPUT,
    HATCH,
    CASING,
    GENERAL;

    public static final Codec<StructureMemberType> CODEC = Codec.STRING.xmap(StructureMemberType::valueOf, StructureMemberType::name);
}
