package org.polaris2023.gtu.modpacks;

import com.gregtechceu.gtceu.api.addon.GTAddon;
import com.gregtechceu.gtceu.api.addon.IGTAddon;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import org.polaris2023.gtu.modpacks.init.MachineRegistries;

@GTAddon(GregtechUniverseModPacks.MOD_ID)
public class GregtechUniverseModPacksAddon implements IGTAddon {
    @Override
    public GTRegistrate getRegistrate() {
        return MachineRegistries.REGISTRATE;
    }

    @Override
    public void gtInitComplete() {
    }
}
