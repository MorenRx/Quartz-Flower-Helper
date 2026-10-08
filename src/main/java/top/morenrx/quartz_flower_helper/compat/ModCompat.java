package top.morenrx.quartz_flower_helper.compat;

import net.minecraftforge.fml.loading.FMLLoader;

public class ModCompat {
    private static Boolean jecharacters = null;

    public static boolean isJecharactersLoaded() {
        if (jecharacters == null) jecharacters = isModLoaded("jecharacters");
        return jecharacters;
    }


    public static boolean isModLoaded(String modId) {
        return FMLLoader.getLoadingModList().getModFileById(modId) != null;
    }
}
