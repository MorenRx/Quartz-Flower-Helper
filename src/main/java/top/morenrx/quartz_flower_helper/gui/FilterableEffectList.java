package top.morenrx.quartz_flower_helper.gui;

import auviotre.enigmatic.addon.contents.items.ArtificialFlower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;
import top.morenrx.quartz_flower_helper.controller.AutoRollManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

public class FilterableEffectList extends AbstractFilterableList {
    private static final DecimalFormat MODIFIER_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final boolean isImmuneList;
    private final List<ResourceLocation> allEffects = new ArrayList<>();
    private int typeFilter = 0; // 0: All, 1: Beneficial, 2: Harmful

    public FilterableEffectList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight, boolean isImmuneList) {
        super(mc, width, height, top, bottom, itemHeight);
        this.isImmuneList = isImmuneList;

        // Load mob effects
        if (ArtificialFlower.Helper.allEffectPool != null && !ArtificialFlower.Helper.allEffectPool.isEmpty()) {
            for (MobEffect effect : ArtificialFlower.Helper.allEffectPool) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (id != null) allEffects.add(id);
            }
        } else {
            for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (id != null) allEffects.add(id);
            }
        }
        allEffects.sort(Comparator.comparing(ResourceLocation::toString));
        rebuildEntries();
    }

    public List<ResourceLocation> getAllEffects() {
        return allEffects;
    }

    public Set<ResourceLocation> getTargetSet() {
        return isImmuneList
                ? AutoRollManager.INSTANCE.getCriteria().getSelectedImmuneEffects()
                : AutoRollManager.INSTANCE.getCriteria().getSelectedProvideEffects();
    }

    public Set<String> getFavoriteSet() {
        return isImmuneList
                ? AutoRollConfig.getInstance().favoriteImmuneEffects
                : AutoRollConfig.getInstance().favoriteProvideEffects;
    }

    public void setTypeFilter(int typeFilter) {
        this.typeFilter = typeFilter;
        rebuildEntries();
    }

    public int getTypeFilter() {
        return typeFilter;
    }

    @Override
    public void clearSelection() {
        getTargetSet().clear();
        AutoRollConfig.save();
        rebuildEntries();
    }

    @Override
    public void rebuildEntries() {
        this.clearEntries();
        List<ResourceLocation> filtered = new ArrayList<>();
        for (ResourceLocation id : allEffects) {
            if (!modFilter.isEmpty() && !modFilter.contains(id.getNamespace())) {
                continue;
            }

            MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
            if (effect == null) continue;

            if (typeFilter == 1 && !effect.isBeneficial()) continue;
            if (typeFilter == 2 && effect.isBeneficial()) continue;

            String localized = Component.translatable(effect.getDescriptionId()).getString();
            String idStr = id.toString();
            String descKey = getEffectDescription(effect);
            String desc = !descKey.isEmpty() ? Component.translatable(descKey).getString() : "";

            boolean matches = currentFilter.isEmpty()
                    || idStr.toLowerCase(Locale.ROOT).contains(currentFilter.toLowerCase(Locale.ROOT))
                    || containsMatch(localized, currentFilter)
                    || (!desc.isEmpty() && containsMatch(desc, currentFilter));

            if (!matches && !currentFilter.isEmpty()) {
                for (Attribute attr : effect.getAttributeModifiers().keySet()) {
                    if (attr != null && containsMatch(Component.translatable(attr.getDescriptionId()).getString(), currentFilter)) {
                        matches = true;
                        break;
                    }
                }
            }

            if (matches) {
                filtered.add(id);
            }
        }

        Set<String> favs = getFavoriteSet();
        Set<ResourceLocation> selected = getTargetSet();

        // 4-Tier Sorting & Grouping:
        // Rank 0: 收藏 + 勾选
        // Rank 1: 仅收藏
        // Rank 2: 仅勾选
        // Rank 3: 未收藏未勾选 (按模组分组)
        List<ResourceLocation> pinned = new ArrayList<>();
        Map<String, List<ResourceLocation>> byMod = new TreeMap<>();

        for (ResourceLocation id : filtered) {
            boolean isFav = favs.contains(id.toString());
            boolean isSel = selected.contains(id);
            if (isFav || isSel) {
                pinned.add(id);
            } else {
                byMod.computeIfAbsent(id.getNamespace(), k -> new ArrayList<>()).add(id);
            }
        }

        // Sort pinned items
        pinned.sort((a, b) -> {
            boolean favA = favs.contains(a.toString());
            boolean favB = favs.contains(b.toString());
            boolean selA = selected.contains(a);
            boolean selB = selected.contains(b);

            int rankA = (favA && selA) ? 0 : (favA ? 1 : (selA ? 2 : 3));
            int rankB = (favB && selB) ? 0 : (favB ? 1 : (selB ? 2 : 3));
            if (rankA != rankB) return Integer.compare(rankA, rankB);

            MobEffect effA = ForgeRegistries.MOB_EFFECTS.getValue(a);
            MobEffect effB = ForgeRegistries.MOB_EFFECTS.getValue(b);
            String nameA = effA != null ? Component.translatable(effA.getDescriptionId()).getString() : a.toString();
            String nameB = effB != null ? Component.translatable(effB.getDescriptionId()).getString() : b.toString();
            return nameA.compareToIgnoreCase(nameB);
        });

        if (!pinned.isEmpty()) {
            this.addEntry(new SeparatorEntry(Component.translatable("quartz_flower_helper.list.pinned")));
            for (ResourceLocation id : pinned) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
                if (effect == null) continue;
                this.addEntry(new ItemEntry(id, effect, getEffectDescription(effect)));
            }
        }

        for (Map.Entry<String, List<ResourceLocation>> entry : byMod.entrySet()) {
            String modId = entry.getKey();
            String modName = ModList.get().getModContainerById(modId)
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(modId);

            this.addEntry(new SeparatorEntry(Component.literal("◆ " + modName)));
            List<ResourceLocation> list = entry.getValue();
            list.sort((a, b) -> {
                MobEffect effA = ForgeRegistries.MOB_EFFECTS.getValue(a);
                MobEffect effB = ForgeRegistries.MOB_EFFECTS.getValue(b);
                String nameA = effA != null ? Component.translatable(effA.getDescriptionId()).getString() : a.toString();
                String nameB = effB != null ? Component.translatable(effB.getDescriptionId()).getString() : b.toString();
                return nameA.compareToIgnoreCase(nameB);
            });

            for (ResourceLocation id : list) {
                MobEffect effect = ForgeRegistries.MOB_EFFECTS.getValue(id);
                if (effect == null) continue;
                this.addEntry(new ItemEntry(id, effect, getEffectDescription(effect)));
            }
        }

        this.setScrollAmount(0);
    }

    public static String getEffectDescription(MobEffect effect) {
        String descId = effect.getDescriptionId();
        String key1 = descId + ".description";
        if (I18n.exists(key1)) return key1;
        String key2 = descId + ".desc";
        if (I18n.exists(key2)) return key2;
        return "";
    }

    @Override
    protected void renderItemTooltip(GuiGraphics guiGraphics, AbstractItemEntry item, int mouseX, int mouseY) {
        if (item instanceof ItemEntry effItem) {
            List<Component> tooltip = new ArrayList<>();

            String modName = ModList.get().getModContainerById(effItem.effectId.getNamespace())
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(effItem.effectId.getNamespace());

            // Line 1: [正面/负面] 效果名
            ChatFormatting tagColor = effItem.effect.isBeneficial() ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
            ChatFormatting nameColor = effItem.effect.isBeneficial() ? ChatFormatting.GREEN : ChatFormatting.RED;
            tooltip.add(Component.translatable(effItem.effect.isBeneficial() ? "quartz_flower_helper.tooltip.effect_beneficial_tag" : "quartz_flower_helper.tooltip.effect_harmful_tag")
                    .withStyle(tagColor)
                    .append(effItem.displayName.copy().withStyle(nameColor)));

            // Line 2: 描述 (颜色为药水id的文字颜色即 GRAY)
            boolean hasDesc = !effItem.descriptionKey.isEmpty();
            if (hasDesc) {
                String descText = Component.translatable(effItem.descriptionKey).getString();
                for (String line : descText.split("\\r?\\n")) {
                    if (!line.trim().isEmpty()) {
                        tooltip.add(Component.literal(line.trim()).withStyle(ChatFormatting.GRAY));
                    }
                }
            }

            Map<Attribute, AttributeModifier> modifiers = effItem.effect.getAttributeModifiers();
            if (!modifiers.isEmpty()) {
                tooltip.add(Component.empty());
                tooltip.add(Component.translatable("quartz_flower_helper.tooltip.effect_attr_header").withStyle(ChatFormatting.GRAY));
                for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
                    Attribute attr = entry.getKey();
                    AttributeModifier mod = entry.getValue();
                    if (attr == null || mod == null) continue;
                    AttributeModifier.Operation op = mod.getOperation();
                    double val = effItem.effect.getAttributeModifierValue(0, mod);
                    ChatFormatting valColor = val >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED;

                    if (op == AttributeModifier.Operation.ADDITION) {
                        String valStr = (val >= 0 ? "+" : "-") + MODIFIER_FORMAT.format(Math.abs(val));
                        tooltip.add(Component.literal(" " + valStr + " ").withStyle(valColor)
                                .append(Component.translatable(attr.getDescriptionId()).withStyle(valColor)));
                    } else if (op == AttributeModifier.Operation.MULTIPLY_TOTAL) {
                        double totalVal = val * 100.0;
                        String valStr = (totalVal >= 0 ? "+" : "-") + MODIFIER_FORMAT.format(Math.abs(totalVal)) + "%";
                        tooltip.add(Component.literal(" " + valStr + " ").withStyle(valColor)
                                .append(Component.translatable(attr.getDescriptionId()).withStyle(valColor))
                                .append(Component.translatable("quartz_flower_helper.tooltip.effect_attr_op_total").withStyle(valColor)));
                    } else if (op == AttributeModifier.Operation.MULTIPLY_BASE) {
                        double baseVal = val * 100.0;
                        String valStr = (baseVal >= 0 ? "+" : "-") + MODIFIER_FORMAT.format(Math.abs(baseVal)) + "%";
                        tooltip.add(Component.literal(" " + valStr + " ").withStyle(valColor)
                                .append(Component.translatable(attr.getDescriptionId()).withStyle(valColor))
                                .append(Component.translatable("quartz_flower_helper.tooltip.effect_attr_op_base").withStyle(valColor)));
                    }
                }
            }

            // 不空行, 下一项是id前面的药水id:去掉, 文字颜色改为物品id那种颜色 (DARK_GRAY)
            tooltip.add(Component.literal(effItem.effectId.toString()).withStyle(ChatFormatting.DARK_GRAY));

            // 最后一行显示模组名模仿原版的蓝色 (BLUE, ITALIC)
            tooltip.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

            guiGraphics.renderComponentTooltip(minecraft.font, tooltip, mouseX, mouseY);
        }
    }

    public class ItemEntry extends AbstractItemEntry {
        public final ResourceLocation effectId;
        public final MobEffect effect;
        public final String descriptionKey;
        public final Component displayName;

        public ItemEntry(ResourceLocation effectId, MobEffect effect, String descriptionKey) {
            this.effectId = effectId;
            this.effect = effect;
            this.descriptionKey = descriptionKey;
            this.displayName = Component.translatable(effect.getDescriptionId());
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }

        @Override
        public boolean isSelected() {
            return getTargetSet().contains(effectId);
        }

        @Override
        public boolean isFavorite() {
            return getFavoriteSet().contains(effectId.toString());
        }

        @Override
        public int getTextColor() {
            return isSelected() ? 0xFFFFD700 : (effect.isBeneficial() ? 0xFF88FF88 : 0xFFFF8888);
        }

        @Override
        public void onToggleFavorite() {
            String idStr = effectId.toString();
            Set<String> favs = getFavoriteSet();
            if (favs.contains(idStr)) {
                favs.remove(idStr);
            } else {
                favs.add(idStr);
            }
            AutoRollConfig.save();
        }

        @Override
        public void onToggleSelection() {
            Set<ResourceLocation> selected = getTargetSet();
            if (selected.contains(effectId)) {
                selected.remove(effectId);
            } else {
                selected.add(effectId);
            }
            AutoRollConfig.save();
        }
    }
}
