package top.morenrx.quartz_flower_helper.gui;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import auviotre.enigmatic.addon.contents.items.ArtificialFlower;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;
import top.morenrx.quartz_flower_helper.config.TargetCriteria;
import top.morenrx.quartz_flower_helper.controller.AutoRollManager;

import java.util.List;
import java.util.Set;

public class AttributeConfigPanel extends AbstractConfigPanel {
    private FilterableAttributeList attributeList;
    private EditBox minPercentBox;
    private EditBox maxPercentBox;
    private int maxAttrLimit = 16;

    public AttributeConfigPanel(ArtificialFlowerScreen parent) {
        super(parent);
    }

    @Override
    protected void initCustomWidgets() {
        TargetCriteria criteria = AutoRollManager.INSTANCE.getCriteria();

        // Mod filter button spans full width (208) in attribute mode
        modFilterBtn = new FlatButton(x + 6, y + 22, 208, 15, Component.literal(""), b -> toggleModDropdown());

        int attrListTop = y + 58;
        int attrListBottom = y + 196;
        attributeList = new FilterableAttributeList(minecraft, width - 12, attrListBottom - attrListTop, attrListTop, attrListBottom, 14);
        attributeList.setLeftPos(x + 6);
        attributeList.setModFilter(AutoRollConfig.getInstance().selectedMods);

        maxAttrLimit = ArtificialFlower.randomAttributeMaxModifier.getValue().asPercentage();

        // Widened input boxes (width 26) to comfortably accommodate negative signs (e.g. -16%)
        minPercentBox = new EditBox(font, x + 32, y + 202, 26, 11, Component.literal("Min%"));
        minPercentBox.setValue(String.valueOf((int) criteria.getMinAttrPercent()));
        minPercentBox.setFilter(s -> s.matches("-?\\d*"));
        minPercentBox.setResponder(s -> {
            try {
                if (!s.isEmpty() && !s.equals("-")) {
                    criteria.setMinAttrPercent(Double.parseDouble(s));
                    AutoRollConfig.save();
                }
            } catch (Exception ignored) {}
        });

        maxPercentBox = new EditBox(font, x + 69, y + 202, 26, 11, Component.literal("Max%"));
        maxPercentBox.setValue(String.valueOf((int) criteria.getMaxAttrPercent()));
        maxPercentBox.setFilter(s -> s.matches("-?\\d*"));
        maxPercentBox.setResponder(s -> {
            try {
                if (!s.isEmpty() && !s.equals("-")) {
                    criteria.setMaxAttrPercent(Double.parseDouble(s));
                    AutoRollConfig.save();
                }
            } catch (Exception ignored) {}
        });
    }

    public List<ResourceLocation> getAllAttributes() {
        return attributeList != null ? attributeList.getAllAttributes() : List.of();
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("quartz_flower_helper.title.attribute");
    }

    @Override
    protected int getDropdownBottom() {
        return y + 196;
    }

    @Override
    public void open() {
        super.open();
        if (attributeList != null) {
            attributeList.rebuildEntries();
        }
    }

    @Override
    protected void renderBody(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Attribute list
        attributeList.render(graphics, mouseX, mouseY, partialTicks);

        // Range row
        graphics.drawString(font, Component.translatable("quartz_flower_helper.gui.range_label"), x + 6, y + 203, 0xFFAAAAAA, false);
        minPercentBox.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, "-", x + 61, y + 203, 0xFFAAAAAA, false);
        maxPercentBox.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawString(font, "%", x + 98, y + 203, 0xFFAAAAAA, false);
        graphics.drawString(font, Component.translatable("quartz_flower_helper.gui.effective_range", maxAttrLimit).getString(), x + 108, y + 203, 0xFF888888, false);
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        attributeList.renderHoverTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void onSearchChanged(String query) {
        attributeList.setFilter(query);
    }

    @Override
    protected void onModFilterChanged(Set<String> selectedMods) {
        attributeList.setModFilter(selectedMods);
    }

    @Override
    protected void onClearSelected() {
        attributeList.clearSelection();
    }

    @Override
    protected boolean isCustomBoxFocused() {
        return minPercentBox.isFocused() || maxPercentBox.isFocused();
    }

    @Override
    protected void clearCustomFocus() {
        minPercentBox.setFocused(false);
        maxPercentBox.setFocused(false);
    }

    @Override
    protected void resetListInertia() {
        super.resetListInertia();
        if (attributeList != null) {
            attributeList.resetInertia();
        }
    }

    @Override
    protected boolean bodyMouseClicked(double mouseX, double mouseY, int button) {
        boolean clickedMin = minPercentBox.isMouseOver(mouseX, mouseY);
        minPercentBox.setFocused(clickedMin);
        if (clickedMin) {
            minPercentBox.mouseClicked(mouseX, mouseY, button);
            searchBox.setFocused(false);
            maxPercentBox.setFocused(false);
            return true;
        }

        boolean clickedMax = maxPercentBox.isMouseOver(mouseX, mouseY);
        maxPercentBox.setFocused(clickedMax);
        if (clickedMax) {
            maxPercentBox.mouseClicked(mouseX, mouseY, button);
            searchBox.setFocused(false);
            minPercentBox.setFocused(false);
            return true;
        }

        if (attributeList.mouseClicked(mouseX, mouseY, button)) {
            clearFocus();
            return true;
        }

        return false;
    }

    @Override
    protected boolean customMouseReleased(double mouseX, double mouseY, int button) {
        attributeList.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    protected boolean customMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return attributeList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    protected boolean customMouseScrolled(double mouseX, double mouseY, double delta) {
        return attributeList.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    protected boolean customKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (minPercentBox.isFocused() && minPercentBox.keyPressed(keyCode, scanCode, modifiers)) return true;
        return maxPercentBox.isFocused() && maxPercentBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean customCharTyped(char codePoint, int modifiers) {
        if (minPercentBox.isFocused() && minPercentBox.charTyped(codePoint, modifiers)) return true;
        return maxPercentBox.isFocused() && maxPercentBox.charTyped(codePoint, modifiers);
    }
}
