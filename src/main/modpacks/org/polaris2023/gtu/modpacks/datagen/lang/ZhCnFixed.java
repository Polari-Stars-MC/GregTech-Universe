package org.polaris2023.gtu.modpacks.datagen.lang;

import net.minecraft.data.PackOutput;
import org.polaris2023.gtu.modpacks.init.BlockRegistries;
import org.polaris2023.gtu.modpacks.init.ItemRegistries;

public class ZhCnFixed extends Lang {
    public ZhCnFixed(PackOutput output) {
        super(output, "zh_cn");
    }

    @Override
    protected void addTranslations() {
        // 燧石工具
        addItem(ItemRegistries.FLINT_ROPE_AXE, "燧石绳斧");
        addItem(ItemRegistries.FLINT_ROPE_PICKAXE, "燧石绳镐");
        addItem(ItemRegistries.FLINT_ROPE_HOE, "燧石绳锄");
        addItem(ItemRegistries.FLINT_ROPE_SWORD, "燧石绳剑");
        addItem(ItemRegistries.FLINT_ROPE_SHOVEL, "燧石绳铲");

        // 水坝控制器
        addBlock(BlockRegistries.WATER_DAM_CONTROLLER, "水坝控制器");

        // 传动杆
        addBlock(BlockRegistries.DAM_SHAFT, "水坝传动杆");

        // 应力输出仓 (各等级)
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_PRIMITIVE, "应力输出仓 (原始人)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_ULV, "应力输出仓 (ULV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_LV, "应力输出仓 (LV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_MV, "应力输出仓 (MV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_HV, "应力输出仓 (HV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_EV, "应力输出仓 (EV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_IV, "应力输出仓 (IV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_LUV, "应力输出仓 (LuV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_ZPM, "应力输出仓 (ZPM)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UV, "应力输出仓 (UV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UHV, "应力输出仓 (UHV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UEV, "应力输出仓 (UEV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UIV, "应力输出仓 (UIV)");
        addBlock(BlockRegistries.STRESS_OUTPUT_HATCH_UXV, "应力输出仓 (UXV)");

        // GUI翻译键
        add("gui.gtu_modpacks.water_dam.title", "水坝控制器");
        add("gui.gtu_modpacks.water_dam.formed", "已成型");
        add("gui.gtu_modpacks.water_dam.not_formed", "未成型");
        add("gui.gtu_modpacks.water_dam.tier", "等级");
        add("gui.gtu_modpacks.water_dam.stress_output", "应力输出");
        add("gui.gtu_modpacks.water_dam.rotation_speed", "转速");
        add("gui.gtu_modpacks.water_dam.flow_speed", "河流流速");
        add("gui.gtu_modpacks.water_dam.connected_dams", "连接水坝数");

        // 护目镜信息翻译键
        add("goggles.gtu_modpacks.dam_controller", "水坝控制器");
        add("goggles.gtu_modpacks.stress_hatch", "应力输出仓");
        add("goggles.gtu_modpacks.linked", "已连接");
        add("goggles.gtu_modpacks.unlinked", "未连接");
    }
}
