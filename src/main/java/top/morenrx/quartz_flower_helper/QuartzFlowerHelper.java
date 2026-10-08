package top.morenrx.quartz_flower_helper;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;

@Mod(QuartzFlowerHelper.MODID)
public class QuartzFlowerHelper {
    public static final String MODID = "quartz_flower_helper";

    public QuartzFlowerHelper(FMLJavaModLoadingContext context) {
        AutoRollConfig.load();
    }
}
