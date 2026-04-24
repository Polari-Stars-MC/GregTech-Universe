package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.apache.commons.lang3.StringUtils;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

public class EnUs extends Lang {
    public EnUs(PackOutput output) {
        super(output, "en_us");
    }

    @Override
    protected void addTranslations() {
        // 自动注册非BlockItem物品
        for (DeferredHolder<Item, ? extends Item> entry : ItemRegistries.REGISTER.getEntries()) {
            ResourceLocation id = entry.getId();
            Item item = entry.get();
            switch (item) {
                case BlockItem ignored -> {}
                default -> {
                    String[] s = id.getPath().split("_");
                    StringBuilder t = new StringBuilder();
                    for (String string : s) {
                        t.append(StringUtils.capitalize(string)).append(" ");
                    }
                    addItem(entry, t.toString().trim());
                }
            }
        }

        // 水坝控制器
        addBlock(BlockRegistries.WATER_DAM_CONTROLLER, "Water Dam Controller");

        // 传动杆
        addBlock(BlockRegistries.DAM_SHAFT, "Dam Shaft");

        // 应力输出仓 (各等级)
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_PRIMITIVE, "Stress Output Hatch (Primitive)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_ULV, "Stress Output Hatch (ULV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_LV, "Stress Output Hatch (LV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_MV, "Stress Output Hatch (MV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_HV, "Stress Output Hatch (HV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_EV, "Stress Output Hatch (EV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_IV, "Stress Output Hatch (IV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_LUV, "Stress Output Hatch (LuV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_ZPM, "Stress Output Hatch (ZPM)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UV, "Stress Output Hatch (UV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UHV, "Stress Output Hatch (UHV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UEV, "Stress Output Hatch (UEV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UIV, "Stress Output Hatch (UIV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UXV, "Stress Output Hatch (UXV)");

        // GUI 翻译键
        add("gui.gtu_modpacks.water_dam.title", "Water Dam Controller");
        add("gui.gtu_modpacks.water_dam.formed", "Formed");
        add("gui.gtu_modpacks.water_dam.not_formed", "Not Formed");
        add("gui.gtu_modpacks.water_dam.tier", "Tier");
        add("gui.gtu_modpacks.water_dam.stress_output", "Stress Output");
        add("gui.gtu_modpacks.water_dam.rotation_speed", "Rotation Speed");
        add("gui.gtu_modpacks.water_dam.flow_speed", "River Flow Speed");
        add("gui.gtu_modpacks.water_dam.connected_dams", "Connected Dams");

        // 护目镜信息
        add("goggles.gtu_modpacks.dam_controller", "Dam Controller");
        add("goggles.gtu_modpacks.stress_hatch", "Stress Output Hatch");
        add("goggles.gtu_modpacks.linked", "Linked");
        add("goggles.gtu_modpacks.unlinked", "Not Linked");
    }
}
