package org.polaris2023.gtu.core.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.polaris2023.gtu.core.GregtechUniverseCore;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberRef;
import org.polaris2023.gtu.core.api.multiblock.network.StructureMemberType;
import org.polaris2023.gtu.core.api.multiblock.network.StructureNetwork;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.CompiledStructureTemplate;
import org.polaris2023.gtu.core.api.multiblock.runtime.access.StructureAccess;
import org.polaris2023.gtu.core.api.multiblock.runtime.access.StructureMemberStorage;
import org.polaris2023.gtu.core.api.multiblock.runtime.cache.StructureTemplateServices;
import org.polaris2023.gtu.core.api.multiblock.runtime.check.StructureValidationResult;
import org.polaris2023.gtu.core.init.BlockEntityRegistries;
import org.polaris2023.gtu.core.menu.TestMultiblockMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestMultiblockControllerBlockEntity extends BlockEntity implements MenuProvider {
    private static final String TAG_NETWORK_ID = "network_id";
    private static final String TAG_FORMED = "formed";
    private static final String TAG_COMPLETION = "completion";
    private static final String TAG_MISMATCH_COUNT = "mismatch_count";

    private final Container container = new SimpleContainer(9);
    private UUID networkId;
    private boolean formed;
    private float completion;
    private int mismatchCount;

    public TestMultiblockControllerBlockEntity(BlockPos pos, BlockState blockState) {
        super(BlockEntityRegistries.TEST_MULTIBLOCK_CONTROLLER.get(), pos, blockState);
    }

    public UUID ensureNetworkId() {
        if (networkId == null) {
            networkId = UUID.randomUUID();
            setChanged();
        }
        return networkId;
    }

    public StructureValidationResult validateStructure(ServerLevel level) {
        UUID id = ensureNetworkId();
        StructureValidationResult result = StructureTemplateServices.validation().validate(
                level,
                worldPosition,
                GregtechUniverseCore.id("test_multiblock"),
                level.getGameTime()
        );

        this.formed = result.formed();
        this.completion = result.completion();
        this.mismatchCount = result.mismatches().size();

        syncNetwork(level, id, result);
        setChanged();
        return result;
    }

    private void syncNetwork(ServerLevel level, UUID id, StructureValidationResult result) {
        StructureNetwork network = StructureAccess.manager(level).networks().get(id);
        if (network == null) {
            network = StructureAccess.manager(level).createNetwork(id, GregtechUniverseCore.id("test_multiblock"), worldPosition.asLong());
        }

        clearMembers(level, network);
        StructureMemberStorage.set(level, worldPosition, new StructureMemberRef(id, StructureMemberType.CONTROLLER));

        if (!result.formed()) {
            network.setFormed(false);
            network.setDirty(true);
            return;
        }

        CompiledStructureTemplate template = StructureTemplateServices.cache().getOrCompile(
                GregtechUniverseCore.id("test_multiblock"),
                level.getGameTime()
        );

        Map<Long, ?> mismatches = result.mismatches();
        for (var entry : template.nodesByRelativePos().entrySet()) {
            long relativePos = entry.getKey();
            if (mismatches.containsKey(relativePos)) {
                continue;
            }

            BlockPos absolutePos = worldPosition.offset(BlockPos.of(relativePos));
            if (absolutePos.equals(worldPosition)) {
                continue;
            }

            StructureAccess.manager(level).registerMember(id, absolutePos.asLong(), StructureMemberType.GENERAL);
            StructureMemberStorage.set(level, absolutePos, new StructureMemberRef(id, StructureMemberType.GENERAL));
        }

        network.setFormed(true);
        network.setDirty(false);
    }

    private void clearMembers(ServerLevel level, StructureNetwork network) {
        List<Long> members = new ArrayList<>(network.members());
        for (Long memberPos : members) {
            BlockPos absolutePos = BlockPos.of(memberPos);
            StructureMemberStorage.clear(level, absolutePos);
            StructureAccess.manager(level).removeMember(memberPos);
        }
    }

    public boolean formed() {
        return formed;
    }

    public float completion() {
        return completion;
    }

    public int mismatchCount() {
        return mismatchCount;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (networkId != null) {
            tag.putUUID(TAG_NETWORK_ID, networkId);
        }
        tag.putBoolean(TAG_FORMED, formed);
        tag.putFloat(TAG_COMPLETION, completion);
        tag.putInt(TAG_MISMATCH_COUNT, mismatchCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        networkId = tag.hasUUID(TAG_NETWORK_ID) ? tag.getUUID(TAG_NETWORK_ID) : null;
        formed = tag.getBoolean(TAG_FORMED);
        completion = tag.getFloat(TAG_COMPLETION);
        mismatchCount = tag.getInt(TAG_MISMATCH_COUNT);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Test Multiblock");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new TestMultiblockMenu(containerId, inventory, container);
    }
}
