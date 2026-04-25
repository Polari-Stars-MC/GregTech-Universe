package org.polaris2023.gtu.core.api.multiblock.runtime.access;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberRef;
import org.polaris2023.gtu.core.api.multiblock.storage.StructureMemberRefStorage;
import org.polaris2023.gtu.core.init.AttachmentRegistries;

public final class StructureMemberStorage {
    private StructureMemberStorage() {
    }

    public static void set(ServerLevel level, BlockPos pos, StructureMemberRef ref) {
        StructureMemberRefStorage.get(level).put(pos.asLong(), ref);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null) {
            return;
        }
        blockEntity.setData(AttachmentRegistries.STRUCTURE_MEMBER.get(), ref);
        blockEntity.setChanged();
    }

    @Nullable
    public static StructureMemberRef get(ServerLevel level, BlockPos pos) {
        StructureMemberRef ref = StructureMemberRefStorage.get(level).get(pos.asLong());
        if (ref != null) {
            return ref;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !blockEntity.hasData(AttachmentRegistries.STRUCTURE_MEMBER.get())) {
            return null;
        }
        return blockEntity.getData(AttachmentRegistries.STRUCTURE_MEMBER.get());
    }

    public static void clear(ServerLevel level, BlockPos pos) {
        StructureMemberRefStorage.get(level).remove(pos.asLong());
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !blockEntity.hasData(AttachmentRegistries.STRUCTURE_MEMBER.get())) {
            return;
        }
        blockEntity.removeData(AttachmentRegistries.STRUCTURE_MEMBER.get());
        blockEntity.setChanged();
    }
}
