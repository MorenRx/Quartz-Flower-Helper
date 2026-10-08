package top.morenrx.quartz_flower_helper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import top.morenrx.quartz_flower_helper.controller.AutoRollManager;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class AutoRollConfig {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("quartz_flower_helper.json");

    public Set<String> selectedAttributes = new HashSet<>();
    public Set<String> selectedProvideEffects = new HashSet<>();
    public Set<String> selectedImmuneEffects = new HashSet<>();
    public double minAttrPercent = 0;
    public double maxAttrPercent = 0;
    public Set<String> selectedMods = new HashSet<>();
    public int provideEffectFilterType = 0;
    public int immuneEffectFilterType = 0;
    public Set<String> favoriteAttributes = new HashSet<>();
    public Set<String> favoriteProvideEffects = new HashSet<>();
    public Set<String> favoriteImmuneEffects = new HashSet<>();
    public boolean showHelp = true;

    private static AutoRollConfig INSTANCE = null;

    public static AutoRollConfig getInstance() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static synchronized void load() {
        if (INSTANCE == null) INSTANCE = new AutoRollConfig();

        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                AutoRollConfig loaded = GSON.fromJson(reader, AutoRollConfig.class);
                if (loaded != null) {
                    if (loaded.selectedAttributes == null) loaded.selectedAttributes = new HashSet<>();
                    if (loaded.selectedProvideEffects == null) loaded.selectedProvideEffects = new HashSet<>();
                    if (loaded.selectedImmuneEffects == null) loaded.selectedImmuneEffects = new HashSet<>();
                    if (loaded.selectedMods == null) loaded.selectedMods = new HashSet<>();
                    if (loaded.favoriteAttributes == null) loaded.favoriteAttributes = new HashSet<>();
                    if (loaded.favoriteProvideEffects == null) loaded.favoriteProvideEffects = new HashSet<>();
                    if (loaded.favoriteImmuneEffects == null) loaded.favoriteImmuneEffects = new HashSet<>();
                    INSTANCE = loaded;
                    applyToCriteria();
                    return;
                }
            } catch (Exception e) {
                LOGGER.error("魔法石英花助手配置加载失败!", e);
            }
        }
        applyToCriteria();
    }

    public static synchronized void save() {
        try {
            updateFromCriteria();
            if (CONFIG_PATH.getParent() != null && !Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (Exception e) {
            LOGGER.error("魔法石英花助手配置保存失败!", e);
        }
    }

    private static void applyToCriteria() {
        TargetCriteria criteria = AutoRollManager.INSTANCE.getCriteria();

        criteria.getSelectedAttributes().clear();
        for (String s : INSTANCE.selectedAttributes) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) criteria.getSelectedAttributes().add(id);
        }

        criteria.getSelectedProvideEffects().clear();
        for (String s : INSTANCE.selectedProvideEffects) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) criteria.getSelectedProvideEffects().add(id);
        }

        criteria.getSelectedImmuneEffects().clear();
        for (String s : INSTANCE.selectedImmuneEffects) {
            ResourceLocation id = ResourceLocation.tryParse(s);
            if (id != null) criteria.getSelectedImmuneEffects().add(id);
        }

        criteria.setMinAttrPercent(INSTANCE.minAttrPercent);
        criteria.setMaxAttrPercent(INSTANCE.maxAttrPercent);
    }

    private static void updateFromCriteria() {
        TargetCriteria criteria = AutoRollManager.INSTANCE.getCriteria();

        INSTANCE.selectedAttributes.clear();
        for (ResourceLocation id : criteria.getSelectedAttributes()) {
            INSTANCE.selectedAttributes.add(id.toString());
        }

        INSTANCE.selectedProvideEffects.clear();
        for (ResourceLocation id : criteria.getSelectedProvideEffects()) {
            INSTANCE.selectedProvideEffects.add(id.toString());
        }

        INSTANCE.selectedImmuneEffects.clear();
        for (ResourceLocation id : criteria.getSelectedImmuneEffects()) {
            INSTANCE.selectedImmuneEffects.add(id.toString());
        }

        INSTANCE.minAttrPercent = criteria.getMinAttrPercent();
        INSTANCE.maxAttrPercent = criteria.getMaxAttrPercent();
    }
}
