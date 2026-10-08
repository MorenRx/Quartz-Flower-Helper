package top.morenrx.quartz_flower_helper.gui;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;

import java.util.Set;

public class EffectConfigPanel extends AbstractConfigPanel {
    // 0: Provide Buff, 1: Immune Buff
    private int activeEffectTab = 0;

    // Title toggle button for Effect Mode: [⇄ 提供] / [⇄ 免疫]
    private FlatButton effectModeToggleBtn;

    // Effect type filter buttons (Positive / Negative, mutually exclusive)
    private FlatButton btnTypePos;
    private FlatButton btnTypeNeg;

    // Effect lists (156px tall!)
    private FilterableEffectList provideEffectList;
    private FilterableEffectList immuneEffectList;

    public EffectConfigPanel(ArtificialFlowerScreen parent) {
        super(parent);
    }

    @Override
    protected void initCustomWidgets() {
        // Effect Mode Title toggle button in title bar (minWidth 40)
        int iconW = font.width("⇄");
        int maxModeTextW = Math.max(
                font.width(Component.translatable("quartz_flower_helper.btn.mode_provide_name")),
                font.width(Component.translatable("quartz_flower_helper.btn.mode_immune_name"))
        );
        int toggleBtnW = Math.max(40, iconW + 2 + maxModeTextW + 8);
        int toggleBtnX = closeBtn.getX() - 3 - toggleBtnW;
        effectModeToggleBtn = new FlatButton(toggleBtnX, y + 3, toggleBtnW, 13, Component.translatable("quartz_flower_helper.btn.mode_provide_name"), b -> {
            setEffectTab(activeEffectTab == 0 ? 1 : 0);
        });
        effectModeToggleBtn.setIconPrefix("⇄", 1);

        // Effect Type Filter Buttons: [正面] [负面] (minWidth 38 each)
        int negTextW = font.width(Component.translatable("quartz_flower_helper.btn.type_harmful"));
        int negBtnW = Math.max(38, negTextW + 8);
        int negX = x + width - 6 - negBtnW;
        btnTypeNeg = new FlatButton(negX, y + 22, negBtnW, 15, Component.translatable("quartz_flower_helper.btn.type_harmful"), b -> toggleEffectTypeFilter(2));

        int posTextW = font.width(Component.translatable("quartz_flower_helper.btn.type_beneficial"));
        int posBtnW = Math.max(38, posTextW + 8);
        int posX = negX - 3 - posBtnW;
        btnTypePos = new FlatButton(posX, y + 22, posBtnW, 15, Component.translatable("quartz_flower_helper.btn.type_beneficial"), b -> toggleEffectTypeFilter(1));

        // Mod filter button dynamically fills remaining row width
        int modFilterBtnW = (posX - 3) - (x + 6);
        modFilterBtn = new FlatButton(x + 6, y + 22, modFilterBtnW, 15, Component.literal(""), b -> toggleModDropdown());

        int effectListTop = y + 58;
        int effectListBottom = y + 214;
        int effectListHeight = effectListBottom - effectListTop;

        provideEffectList = new FilterableEffectList(minecraft, width - 12, effectListHeight, effectListTop, effectListBottom, 14, false);
        provideEffectList.setLeftPos(x + 6);

        immuneEffectList = new FilterableEffectList(minecraft, width - 12, effectListHeight, effectListTop, effectListBottom, 14, true);
        immuneEffectList.setLeftPos(x + 6);

        Set<String> savedMods = AutoRollConfig.getInstance().selectedMods;
        provideEffectList.setModFilter(savedMods);
        immuneEffectList.setModFilter(savedMods);

        provideEffectList.setTypeFilter(AutoRollConfig.getInstance().provideEffectFilterType);
        immuneEffectList.setTypeFilter(AutoRollConfig.getInstance().immuneEffectFilterType);

        updateEffectTabStyles();
        updateTypeButtonStyles();
    }

    @Override
    protected Component getTitle() {
        return Component.translatable("quartz_flower_helper.title.effect");
    }

    @Override
    protected int getDropdownBottom() {
        return y + 214;
    }

    @Override
    public void open() {
        super.open();
        if (provideEffectList != null) provideEffectList.rebuildEntries();
        if (immuneEffectList != null) immuneEffectList.rebuildEntries();
    }

    private void setEffectTab(int tab) {
        this.activeEffectTab = tab;
        this.isModDropdownOpen = false;
        updateEffectTabStyles();
        updateTypeButtonStyles();
        searchBox.setValue("");
        if (provideEffectList != null) provideEffectList.resetInertia();
        if (immuneEffectList != null) immuneEffectList.resetInertia();
        if (activeEffectTab == 0) {
            provideEffectList.rebuildEntries();
        } else {
            immuneEffectList.rebuildEntries();
        }
        clearFocus();
    }

    private void toggleEffectTypeFilter(int targetType) {
        int currentType = (activeEffectTab == 0)
                ? provideEffectList.getTypeFilter()
                : immuneEffectList.getTypeFilter();

        int newType = (currentType == targetType) ? 0 : targetType;

        if (activeEffectTab == 0) {
            provideEffectList.setTypeFilter(newType);
            AutoRollConfig.getInstance().provideEffectFilterType = newType;
        } else {
            immuneEffectList.setTypeFilter(newType);
            AutoRollConfig.getInstance().immuneEffectFilterType = newType;
        }
        AutoRollConfig.save();
        updateTypeButtonStyles();
    }

    private void updateEffectTabStyles() {
        if (activeEffectTab == 0) {
            effectModeToggleBtn.setMessage(Component.translatable("quartz_flower_helper.btn.mode_provide_name"));
            effectModeToggleBtn.setIconPrefix("⇄", 1);
            effectModeToggleBtn.setCustomColors(0xFF1E2A20, 0xFF388E3C, 0xFFA5D6A7);
        } else {
            effectModeToggleBtn.setMessage(Component.translatable("quartz_flower_helper.btn.mode_immune_name"));
            effectModeToggleBtn.setIconPrefix("⇄", 1);
            effectModeToggleBtn.setCustomColors(0xFF281E2E, 0xFF8E24AA, 0xFFCE93D8);
        }
    }

    private void updateTypeButtonStyles() {
        int currentType = (activeEffectTab == 0)
                ? provideEffectList.getTypeFilter()
                : immuneEffectList.getTypeFilter();

        if (currentType == 1) {
            btnTypePos.setMessage(Component.literal("§a").append(Component.translatable("quartz_flower_helper.btn.type_beneficial")));
            btnTypePos.setCustomColors(0xFF1B3820, 0xFF4CAF50, 0xFF81C784);
            btnTypeNeg.setMessage(Component.translatable("quartz_flower_helper.btn.type_harmful"));
            btnTypeNeg.setCustomColors(0, 0, 0);
        } else if (currentType == 2) {
            btnTypePos.setMessage(Component.translatable("quartz_flower_helper.btn.type_beneficial"));
            btnTypePos.setCustomColors(0, 0, 0);
            btnTypeNeg.setMessage(Component.literal("§c").append(Component.translatable("quartz_flower_helper.btn.type_harmful")));
            btnTypeNeg.setCustomColors(0xFF381B1B, 0xFFE53935, 0xFFE57373);
        } else {
            btnTypePos.setMessage(Component.translatable("quartz_flower_helper.btn.type_beneficial"));
            btnTypePos.setCustomColors(0, 0, 0);
            btnTypeNeg.setMessage(Component.translatable("quartz_flower_helper.btn.type_harmful"));
            btnTypeNeg.setCustomColors(0, 0, 0);
        }
    }

    @Override
    protected void renderBody(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Title toggle button in title bar
        effectModeToggleBtn.render(graphics, mouseX, mouseY, partialTicks);

        // Type filter buttons
        btnTypePos.render(graphics, mouseX, mouseY, partialTicks);
        btnTypeNeg.render(graphics, mouseX, mouseY, partialTicks);

        // Active Effect List (156px tall)
        if (activeEffectTab == 0) {
            provideEffectList.render(graphics, mouseX, mouseY, partialTicks);
        } else {
            immuneEffectList.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    protected void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        if (effectModeToggleBtn.isMouseOver(mouseX, mouseY)) {
            Component modeTip = Component.translatable(activeEffectTab == 0 ? "quartz_flower_helper.tooltip.mode_toggle_to_immune" : "quartz_flower_helper.tooltip.mode_toggle_to_provide");
            graphics.renderTooltip(font, modeTip, mouseX, mouseY);
        } else if (activeEffectTab == 0) {
            provideEffectList.renderHoverTooltip(graphics, mouseX, mouseY);
        } else {
            immuneEffectList.renderHoverTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void onSearchChanged(String query) {
        if (activeEffectTab == 0) {
            provideEffectList.setFilter(query);
        } else {
            immuneEffectList.setFilter(query);
        }
    }

    @Override
    protected void onModFilterChanged(Set<String> selectedMods) {
        provideEffectList.setModFilter(selectedMods);
        immuneEffectList.setModFilter(selectedMods);
    }

    @Override
    protected void onClearSelected() {
        if (activeEffectTab == 0) {
            provideEffectList.clearSelection();
        } else {
            immuneEffectList.clearSelection();
        }
    }

    @Override
    protected boolean isCustomBoxFocused() {
        return false;
    }

    @Override
    protected void clearCustomFocus() {}

    @Override
    protected void resetListInertia() {
        super.resetListInertia();
        if (provideEffectList != null) {
            provideEffectList.resetInertia();
        }
        if (immuneEffectList != null) {
            immuneEffectList.resetInertia();
        }
    }

    @Override
    protected boolean customMouseClicked(double mouseX, double mouseY, int button) {
        if (effectModeToggleBtn.mouseClicked(mouseX, mouseY, button)) return true;
        if (btnTypePos.mouseClicked(mouseX, mouseY, button)) return true;
        return btnTypeNeg.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected boolean bodyMouseClicked(double mouseX, double mouseY, int button) {
        if (activeEffectTab == 0) {
            if (provideEffectList.mouseClicked(mouseX, mouseY, button)) {
                clearFocus();
                return true;
            }
        } else {
            if (immuneEffectList.mouseClicked(mouseX, mouseY, button)) {
                clearFocus();
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean customMouseReleased(double mouseX, double mouseY, int button) {
        provideEffectList.mouseReleased(mouseX, mouseY, button);
        immuneEffectList.mouseReleased(mouseX, mouseY, button);
        return true;
    }

    @Override
    protected boolean customMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (activeEffectTab == 0) {
            return provideEffectList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        } else {
            return immuneEffectList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
    }

    @Override
    protected boolean customMouseScrolled(double mouseX, double mouseY, double delta) {
        if (activeEffectTab == 0) {
            return provideEffectList.mouseScrolled(mouseX, mouseY, delta);
        } else {
            return immuneEffectList.mouseScrolled(mouseX, mouseY, delta);
        }
    }
}
