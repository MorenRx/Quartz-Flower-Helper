package top.morenrx.quartz_flower_helper.gui;

import auviotre.enigmatic.addon.contents.items.ArtificialFlower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;
import top.morenrx.quartz_flower_helper.controller.AutoRollManager;

import java.util.*;

public class FilterableAttributeList extends AbstractFilterableList {
    private final List<ResourceLocation> allAttributes = new ArrayList<>();

    public FilterableAttributeList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);

        // Load attributes
        if (ArtificialFlower.Helper.attributePool != null && !ArtificialFlower.Helper.attributePool.isEmpty()) {
            for (Attribute attr : ArtificialFlower.Helper.attributePool) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (id != null) allAttributes.add(id);
            }
        } else {
            for (Attribute attr : ForgeRegistries.ATTRIBUTES.getValues()) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (id != null) allAttributes.add(id);
            }
        }
        allAttributes.sort(Comparator.comparing(ResourceLocation::toString));
        rebuildEntries();
    }

    public List<ResourceLocation> getAllAttributes() {
        return allAttributes;
    }

    @Override
    public void clearSelection() {
        AutoRollManager.INSTANCE.getCriteria().getSelectedAttributes().clear();
        AutoRollConfig.save();
        rebuildEntries();
    }

    @Override
    public void rebuildEntries() {
        this.clearEntries();
        List<ResourceLocation> filtered = new ArrayList<>();
        for (ResourceLocation id : allAttributes) {
            if (!modFilter.isEmpty() && !modFilter.contains(id.getNamespace())) {
                continue;
            }

            Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
            String localized = attr != null ? Component.translatable(attr.getDescriptionId()).getString() : "";
            String idStr = id.toString();

            boolean matches = currentFilter.isEmpty()
                    || idStr.toLowerCase(Locale.ROOT).contains(currentFilter.toLowerCase(Locale.ROOT))
                    || containsMatch(localized, currentFilter);

            if (matches) {
                filtered.add(id);
            }
        }

        Set<String> favs = AutoRollConfig.getInstance().favoriteAttributes;
        Set<ResourceLocation> selected = AutoRollManager.INSTANCE.getCriteria().getSelectedAttributes();

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

            Attribute attrA = ForgeRegistries.ATTRIBUTES.getValue(a);
            Attribute attrB = ForgeRegistries.ATTRIBUTES.getValue(b);
            String nameA = attrA != null ? Component.translatable(attrA.getDescriptionId()).getString() : a.toString();
            String nameB = attrB != null ? Component.translatable(attrB.getDescriptionId()).getString() : b.toString();
            return nameA.compareToIgnoreCase(nameB);
        });

        if (!pinned.isEmpty()) {
            this.addEntry(new SeparatorEntry(Component.translatable("quartz_flower_helper.list.pinned")));
            for (ResourceLocation id : pinned) {
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
                this.addEntry(new ItemEntry(id, attr));
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
                Attribute attrA = ForgeRegistries.ATTRIBUTES.getValue(a);
                Attribute attrB = ForgeRegistries.ATTRIBUTES.getValue(b);
                String nameA = attrA != null ? Component.translatable(attrA.getDescriptionId()).getString() : a.toString();
                String nameB = attrB != null ? Component.translatable(attrB.getDescriptionId()).getString() : b.toString();
                return nameA.compareToIgnoreCase(nameB);
            });

            for (ResourceLocation id : list) {
                Attribute attr = ForgeRegistries.ATTRIBUTES.getValue(id);
                this.addEntry(new ItemEntry(id, attr));
            }
        }

        this.setScrollAmount(0);
    }

    @Override
    protected void renderItemTooltip(GuiGraphics guiGraphics, AbstractItemEntry item, int mouseX, int mouseY) {
        if (item instanceof ItemEntry attrItem) {
            List<Component> tooltip = new ArrayList<>();

            String modName = ModList.get().getModContainerById(attrItem.attributeId.getNamespace())
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(attrItem.attributeId.getNamespace());

            // 第一行: 属性名
            tooltip.add(attrItem.displayName.copy().withStyle(ChatFormatting.WHITE));

            // 第二行: ID (DARK_GRAY)
            tooltip.add(Component.literal(attrItem.attributeId.toString()).withStyle(ChatFormatting.DARK_GRAY));

            // 第三行: 模组名 (BLUE, ITALIC)
            tooltip.add(Component.literal(modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

            guiGraphics.renderComponentTooltip(minecraft.font, tooltip, mouseX, mouseY);
        }
    }

    public class ItemEntry extends AbstractItemEntry {
        public final ResourceLocation attributeId;
        public final Attribute attribute;
        public final Component displayName;

        public ItemEntry(ResourceLocation attributeId, Attribute attribute) {
            this.attributeId = attributeId;
            this.attribute = attribute;
            if (attribute != null) {
                this.displayName = Component.translatable(attribute.getDescriptionId());
            } else {
                this.displayName = Component.literal(attributeId.getPath());
            }
        }

        @Override
        public Component getDisplayName() {
            return displayName;
        }

        @Override
        public boolean isSelected() {
            return AutoRollManager.INSTANCE.getCriteria().getSelectedAttributes().contains(attributeId);
        }

        @Override
        public boolean isFavorite() {
            return AutoRollConfig.getInstance().favoriteAttributes.contains(attributeId.toString());
        }

        @Override
        public int getTextColor() {
            return isSelected() ? 0xFFFFD700 : 0xFFAAAAAA;
        }

        @Override
        public void onToggleFavorite() {
            String idStr = attributeId.toString();
            if (AutoRollConfig.getInstance().favoriteAttributes.contains(idStr)) {
                AutoRollConfig.getInstance().favoriteAttributes.remove(idStr);
            } else {
                AutoRollConfig.getInstance().favoriteAttributes.add(idStr);
            }
            AutoRollConfig.save();
        }

        @Override
        public void onToggleSelection() {
            Set<ResourceLocation> selected = AutoRollManager.INSTANCE.getCriteria().getSelectedAttributes();
            if (selected.contains(attributeId)) {
                selected.remove(attributeId);
            } else {
                selected.add(attributeId);
            }
            AutoRollConfig.save();
        }
    }
}
