package top.morenrx.quartz_flower_helper.gui;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;

import java.util.Set;

public abstract class AbstractConfigPanel {
    protected final ArtificialFlowerScreen parent;
    protected final Minecraft minecraft;
    protected final Font font;

    protected int x;
    protected int y;
    protected final int width = 220;
    protected final int height = 230;
    protected boolean visible = false;

    // Common UI Widgets
    protected FlatButton closeBtn;
    protected FlatButton modFilterBtn;
    protected ModFilterList modFilterList;
    protected boolean isModDropdownOpen = false;

    protected EditBox searchBox;
    protected FlatButton clearBtn;

    public AbstractConfigPanel(ArtificialFlowerScreen parent) {
        this.parent = parent;
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
    }

    public void init(int screenWidth, int screenHeight, Set<String> detectedMods) {
        this.x = (screenWidth - width) / 2;
        this.y = (screenHeight - height) / 2;

        closeBtn = new FlatButton(x + width - 17, y + 3, 14, 13, Component.literal("✕"), b -> close());

        int clearTextW = font.width(Component.translatable("quartz_flower_helper.btn.clear_selected"));
        int clearBtnW = Math.max(57, clearTextW + 8);
        int clearBtnX = x + width - 6 - clearBtnW;
        clearBtn = new FlatButton(clearBtnX, y + 40, clearBtnW, 15, Component.translatable("quartz_flower_helper.btn.clear_selected"), b -> onClearSelected());

        int searchBoxW = (clearBtnX - 4) - (x + 7);
        searchBox = new EditBox(font, x + 7, y + 41, searchBoxW, 13, Component.translatable("quartz_flower_helper.search.hint"));
        searchBox.setHint(Component.translatable("quartz_flower_helper.search.hint"));
        searchBox.setResponder(this::onSearchChanged);

        modFilterList = new ModFilterList(
                minecraft,
                width - 12,
                135,
                y + 38,
                getDropdownBottom(),
                13,
                detectedMods,
                AutoRollConfig.getInstance().selectedMods,
                () -> {
                    AutoRollConfig.save();
                    onModFilterChanged(AutoRollConfig.getInstance().selectedMods);
                    updateModButtonLabel();
                }
        );
        modFilterList.setLeftPos(x + 6);

        initCustomWidgets();
        updateModButtonLabel();
    }

    protected abstract void initCustomWidgets();
    protected abstract Component getTitle();
    protected abstract int getDropdownBottom();
    protected abstract void renderBody(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);
    protected abstract void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY);
    protected abstract void onSearchChanged(String query);
    protected abstract void onModFilterChanged(Set<String> selectedMods);
    protected abstract void onClearSelected();
    protected abstract boolean isCustomBoxFocused();
    protected abstract void clearCustomFocus();

    protected boolean customMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean bodyMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean customMouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    protected boolean customMouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return false;
    }

    protected boolean customMouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    protected boolean customKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    protected boolean customCharTyped(char codePoint, int modifiers) {
        return false;
    }

    public void open() {
        this.visible = true;
        this.isModDropdownOpen = false;
        searchBox.setValue("");
        clearFocus();
        updateModButtonLabel();
        resetListInertia();
    }

    public void close() {
        this.visible = false;
        this.isModDropdownOpen = false;
        clearFocus();
        AutoRollConfig.save();
        resetListInertia();
    }

    protected void resetListInertia() {
        if (modFilterList != null) {
            modFilterList.resetInertia();
        }
    }

    public boolean isVisible() {
        return visible;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void clearFocus() {
        searchBox.setFocused(false);
        clearCustomFocus();
    }

    public boolean isAnyBoxFocused() {
        return searchBox.isFocused() || isCustomBoxFocused();
    }

    protected void toggleModDropdown() {
        this.isModDropdownOpen = !this.isModDropdownOpen;
        updateModButtonLabel();
    }

    protected void updateModButtonLabel() {
        if (modFilterBtn == null) return;
        Set<String> selected = AutoRollConfig.getInstance().selectedMods;
        if (isModDropdownOpen) {
            modFilterBtn.setMessage(Component.translatable("quartz_flower_helper.btn.mod_filter_open"));
        } else if (selected.isEmpty()) {
            modFilterBtn.setMessage(Component.translatable("quartz_flower_helper.btn.mod_filter_all"));
        } else {
            modFilterBtn.setMessage(Component.translatable("quartz_flower_helper.btn.mod_count", selected.size()));
        }
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!visible) return;

        graphics.flush();

        // Dim flower screen area only
        int guiLeft = parent.getGuiLeft();
        int guiTop = parent.getGuiTop();
        int guiWidth = parent.getXSize();
        int guiHeight = parent.getYSize();
        graphics.fill(guiLeft, guiTop, guiLeft + guiWidth, guiTop + guiHeight, 0x75000000);

        // Dialog background
        graphics.fill(x, y, x + width, y + height, 0xF8181818);
        graphics.fill(x, y, x + width, y + 19, 0xFF242424);
        graphics.renderOutline(x, y, width, height, 0xFF666666);

        // Title
        graphics.drawString(font, getTitle(), x + 7, y + 7, 0xFFFFCC00, false);

        // Close button
        closeBtn.render(graphics, mouseX, mouseY, partialTicks);

        // Mod filter button
        if (modFilterBtn != null) {
            modFilterBtn.render(graphics, mouseX, mouseY, partialTicks);
        }

        // Subclass specific widgets (e.g. lists, mode toggles, type buttons, range boxes)
        renderBody(graphics, mouseX, mouseY, partialTicks);

        // Search bar & Clear button
        searchBox.render(graphics, mouseX, mouseY, partialTicks);
        if (!searchBox.getValue().isEmpty()) {
            int iconBoxR = searchBox.getX() + searchBox.getWidth() - 1;
            int iconBoxL = iconBoxR - 14;
            int iconBoxT = searchBox.getY();
            int iconBoxB = searchBox.getY() + searchBox.getHeight();
            boolean isHoverClear = mouseX >= iconBoxL && mouseX <= iconBoxR && mouseY >= iconBoxT && mouseY <= iconBoxB;
            graphics.fill(iconBoxL, searchBox.getY() + 1, iconBoxR, searchBox.getY() + searchBox.getHeight() - 1, 0xFF000000);
            int clearColor = isHoverClear ? 0xFFFFFFFF : 0xFF777777;
            graphics.drawString(font, "✕", iconBoxL + 4, searchBox.getY() + 2, clearColor, false);
        }
        clearBtn.render(graphics, mouseX, mouseY, partialTicks);

        // Footer hint
        graphics.drawString(font, Component.translatable("quartz_flower_helper.gui.hint_footer").getString(), x + 6, y + 218, 0xFF888888, false);

        // Dropdown on top
        if (isModDropdownOpen) {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 100.0F);

            modFilterList.render(graphics, mouseX, mouseY, partialTicks);
            modFilterList.renderHoverTooltip(graphics, mouseX, mouseY);

            graphics.flush();
            graphics.pose().popPose();
        } else {
            renderTooltips(graphics, mouseX, mouseY);
            if (!searchBox.getValue().isEmpty()) {
                int iconBoxR = searchBox.getX() + searchBox.getWidth() - 1;
                int iconBoxL = iconBoxR - 14;
                if (mouseX >= iconBoxL && mouseX <= iconBoxR && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
                    graphics.renderTooltip(font, Component.translatable("quartz_flower_helper.search.clear"), mouseX, mouseY);
                }
            }
        }
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return;

        // Click outside dialog
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) {
            if (isModDropdownOpen) {
                isModDropdownOpen = false;
                modFilterList.resetInertia();
                updateModButtonLabel();
                return;
            }
            close();
            return;
        }

        if (closeBtn.mouseClicked(mouseX, mouseY, button)) return;

        if (isModDropdownOpen) {
            if (modFilterBtn != null && modFilterBtn.isMouseOver(mouseX, mouseY)) {
                isModDropdownOpen = false;
                modFilterList.resetInertia();
                updateModButtonLabel();
                return;
            }
            if (modFilterList.isMouseOver(mouseX, mouseY)) {
                modFilterList.mouseClicked(mouseX, mouseY, button);
                return;
            }
            isModDropdownOpen = false;
            modFilterList.resetInertia();
            updateModButtonLabel();
            return;
        }

        if (customMouseClicked(mouseX, mouseY, button)) return;
        if (modFilterBtn != null && modFilterBtn.mouseClicked(mouseX, mouseY, button)) return;
        if (clearBtn.mouseClicked(mouseX, mouseY, button)) return;

        // Quick clear search icon
        if (!searchBox.getValue().isEmpty()) {
            int iconBoxR = searchBox.getX() + searchBox.getWidth() - 1;
            int iconBoxL = iconBoxR - 14;
            if (mouseX >= iconBoxL && mouseX <= iconBoxR && mouseY >= searchBox.getY() && mouseY <= searchBox.getY() + searchBox.getHeight()) {
                searchBox.setValue("");
                searchBox.setFocused(true);
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                clearCustomFocus();
                return;
            }
        }

        boolean clickedSearch = searchBox.isMouseOver(mouseX, mouseY);
        searchBox.setFocused(clickedSearch);
        if (clickedSearch) {
            searchBox.mouseClicked(mouseX, mouseY, button);
            clearCustomFocus();
            return;
        }

        if (bodyMouseClicked(mouseX, mouseY, button)) {
            return;
        }

        clearFocus();
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        if (isModDropdownOpen && modFilterList.mouseReleased(mouseX, mouseY, button)) return true;
        return customMouseReleased(mouseX, mouseY, button);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!visible) return false;
        if (isModDropdownOpen) return modFilterList.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return customMouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!visible) return false;
        if (isModDropdownOpen) {
            if (modFilterList.isMouseOver(mouseX, mouseY)) {
                return modFilterList.mouseScrolled(mouseX, mouseY, delta);
            }
            return true;
        }
        return customMouseScrolled(mouseX, mouseY, delta);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!visible) return false;

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isModDropdownOpen) {
                isModDropdownOpen = false;
                updateModButtonLabel();
                return true;
            }
            if (isAnyBoxFocused()) {
                clearFocus();
                return true;
            }
            close();
            return true;
        }

        if (searchBox.isFocused() && searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        return customKeyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char codePoint, int modifiers) {
        if (!visible) return false;
        if (searchBox.isFocused() && searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return customCharTyped(codePoint, modifiers);
    }

    public static class FlatButton extends Button {
        private int customBg = 0;
        private int customBorder = 0;
        private int customText = 0;
        private String iconPrefix = null;
        private int iconOffsetY = 0;

        public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        }

        public void setCustomColors(int bg, int border, int text) {
            this.customBg = bg;
            this.customBorder = border;
            this.customText = text;
        }

        public void setIconPrefix(String icon, int offsetY) {
            this.iconPrefix = icon;
            this.iconOffsetY = offsetY;
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            boolean hovered = this.isHoveredOrFocused();

            int bg;
            int border;
            int text;

            if (customBg != 0) {
                bg = hovered ? brighten(customBg) : customBg;
                border = hovered ? 0xFFCCCCCC : customBorder;
                text = customText != 0 ? customText : (hovered ? 0xFFFFFFFF : 0xFFDDDDDD);
            } else if (!this.active) {
                bg = 0xD01E1E22;
                border = 0xFF35353C;
                text = 0xFF666666;
            } else if (hovered) {
                bg = 0xF0353742;
                border = 0xFF7E8092;
                text = 0xFFFFFFFF;
            } else {
                bg = 0xE0222328;
                border = 0xFF44454E;
                text = 0xFFCCCCCC;
            }

            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);
            graphics.renderOutline(this.getX(), this.getY(), this.width, this.height, border);

            Font font = Minecraft.getInstance().font;
            if (iconPrefix != null && !iconPrefix.isEmpty()) {
                int iconW = font.width(iconPrefix);
                int gap = 2;
                int textW = font.width(this.getMessage());
                int totalW = iconW + gap + textW;
                int startX = this.getX() + (this.width - totalW) / 2;
                int textY = this.getY() + (this.height - 8) / 2;

                graphics.drawString(font, iconPrefix, startX, textY + iconOffsetY, text, false);
                graphics.drawString(font, this.getMessage(), startX + iconW + gap, textY, text, false);
            } else {
                int textW = font.width(this.getMessage());
                int textX = this.getX() + (this.width - textW) / 2;
                int textY = this.getY() + (this.height - 8) / 2;
                graphics.drawString(font, this.getMessage(), textX, textY, text, false);
            }
        }

        private static int brighten(int argb) {
            int a = (argb >> 24) & 0xFF;
            int r = Math.min(255, ((argb >> 16) & 0xFF) + 25);
            int g = Math.min(255, ((argb >> 8) & 0xFF) + 25);
            int b = Math.min(255, (argb & 0xFF) + 25);
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
    }
}
