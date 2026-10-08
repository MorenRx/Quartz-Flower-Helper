package top.morenrx.quartz_flower_helper.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ModFilterList extends AbstractSelectionList<ModFilterList.Entry> {
    private final Set<String> selectedMods;
    private final Runnable onFilterChanged;
    private boolean isDraggingScrollbar = false;
    private Entry hoveredEntry = null;

    private double mouseDownX = 0;
    private double mouseDownY = 0;
    private double initialScrollAmount = 0;
    private boolean isDraggingContent = false;
    private boolean hasDragged = false;
    private Entry pressedEntry = null;
    private static final double DRAG_THRESHOLD = 4.0;

    private double scrollVelocity = 0.0;
    private long lastFrameTime = 0L;
    private double lastDragY = 0;
    private long lastDragTime = 0L;
    private double dragVelocity = 0.0;
    private static final double FRICTION = 8.5;

    public void resetInertia() {
        this.scrollVelocity = 0.0;
        this.isDraggingContent = false;
        this.isDraggingScrollbar = false;
        this.hasDragged = false;
        this.pressedEntry = null;
        this.lastFrameTime = 0L;
    }

    public ModFilterList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight,
                          Set<String> allDetectedMods, Set<String> selectedMods, Runnable onFilterChanged) {
        super(mc, width, height, top, bottom, itemHeight);
        this.selectedMods = selectedMods;
        this.onFilterChanged = onFilterChanged;

        // Entry 0: "全部" (All)
        this.addEntry(new AllEntry());

        // Sort mods: "minecraft" first, then others alphabetically by display name
        List<String> sortedMods = new ArrayList<>(allDetectedMods);
        sortedMods.sort((a, b) -> {
            if ("minecraft".equals(a)) return -1;
            if ("minecraft".equals(b)) return 1;
            String nameA = ModList.get().getModContainerById(a).map(c -> c.getModInfo().getDisplayName()).orElse(a);
            String nameB = ModList.get().getModContainerById(b).map(c -> c.getModInfo().getDisplayName()).orElse(b);
            return nameA.compareToIgnoreCase(nameB);
        });

        for (String modId : sortedMods) {
            this.addEntry(new ModItemEntry(modId));
        }
    }

    public void updatePosition(int top, int bottom) {
        this.y0 = top;
        this.y1 = bottom;
    }

    @Override
    public int getMaxPosition() {
        return this.getItemCount() * this.itemHeight + 2;
    }

    @Override
    public int getRowTop(int index) {
        return this.y0 + 1 - (int) this.getScrollAmount() + index * this.itemHeight;
    }

    @Override
    public int getRowBottom(int index) {
        return this.getRowTop(index) + this.itemHeight;
    }

    @Override
    protected void renderList(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int count = this.getItemCount();
        int top = this.y0 + 1 - (int) this.getScrollAmount();
        int rowLeft = this.getRowLeft();
        int rowWidth = this.getRowWidth();

        for (int i = 0; i < count; ++i) {
            Entry entry = this.children().get(i);
            int entryHeight = this.itemHeight;
            int bottom = top + entryHeight;
            if (bottom >= this.y0 && top <= this.y1) {
                entry.render(graphics, i, top, rowLeft, rowWidth, entryHeight, mouseX, mouseY, this.isMouseOver(mouseX, mouseY) && this.findEntryAtPosition(mouseX, mouseY) == entry, partialTick);
            }
            top += entryHeight;
        }
    }

    @Nullable
    public Entry findEntryAtPosition(double mouseX, double mouseY) {
        if (mouseX < this.x0 || mouseX > this.x1 || mouseY < this.y0 || mouseY > this.y1) {
            return null;
        }
        int top = this.y0 + 1 - (int) this.getScrollAmount();
        for (Entry entry : this.children()) {
            int bottom = top + this.itemHeight;
            if (mouseY >= top && mouseY < bottom) {
                return entry;
            }
            top = bottom;
        }
        return null;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hoveredEntry = null;

        // Apply smooth momentum / inertia scrolling
        long now = System.currentTimeMillis();
        if (this.lastFrameTime == 0L) {
            this.lastFrameTime = now;
        }
        double dt = (now - this.lastFrameTime) / 1000.0;
        this.lastFrameTime = now;
        if (dt > 0.1) dt = 0.1;

        if (!this.isDraggingContent && Math.abs(this.scrollVelocity) > 1.0) {
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                double decay = Math.exp(-FRICTION * dt);
                double displacement = this.scrollVelocity * (1.0 - decay) / FRICTION;
                double newScroll = this.getScrollAmount() - displacement;

                if (newScroll <= 0) {
                    this.setScrollAmount(0);
                    this.scrollVelocity = 0.0;
                } else if (newScroll >= maxScroll) {
                    this.setScrollAmount(maxScroll);
                    this.scrollVelocity = 0.0;
                } else {
                    this.setScrollAmount(newScroll);
                    this.scrollVelocity *= decay;
                }
            } else {
                this.setScrollAmount(0);
                this.scrollVelocity = 0.0;
            }
        } else if (!this.isDraggingContent) {
            this.scrollVelocity = 0.0;
        }

        // Draw popup background and border
        guiGraphics.fill(this.x0, this.y0, this.x1, this.y1, 0xF8181818);
        guiGraphics.renderOutline(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0, 0xFF666666);

        // Scissor clip items strictly inside the list frame
        guiGraphics.enableScissor(this.x0 + 1, this.y0 + 1, this.x1 - 1, this.y1 - 1);
        this.renderList(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.disableScissor();

        // Detect hovered entry within list bounds (suppressed while dragging or fast gliding)
        if (!hasDragged && Math.abs(this.scrollVelocity) < 60.0 && mouseX >= this.x0 && mouseX <= this.x1 && mouseY >= this.y0 && mouseY <= this.y1) {
            hoveredEntry = this.findEntryAtPosition(mouseX, mouseY);
        }

        // Draggable custom slim scrollbar
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            int scrollbarPosition = this.getScrollbarPosition();
            int listH = this.y1 - this.y0;
            int scrollbarHeight = (int) ((float) (listH * listH) / (float) this.getMaxPosition());
            scrollbarHeight = Math.max(16, Math.min(scrollbarHeight, listH - 4));
            int scrollbarY = (int) this.getScrollAmount() * (listH - scrollbarHeight) / maxScroll + this.y0;

            guiGraphics.fill(scrollbarPosition, this.y0, scrollbarPosition + 4, this.y1, 0xFF111111);
            int barColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF777777;
            guiGraphics.fill(scrollbarPosition, scrollbarY, scrollbarPosition + 4, scrollbarY + scrollbarHeight, barColor);
        }
    }

    public void renderHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredEntry instanceof ModItemEntry modEntry) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal(modEntry.displayName).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("quartz_flower_helper.tooltip.mod_id", modEntry.modId).withStyle(ChatFormatting.GRAY));
            guiGraphics.renderComponentTooltip(minecraft.font, tooltip, mouseX, mouseY);
        } else if (hoveredEntry instanceof AllEntry) {
            guiGraphics.renderTooltip(minecraft.font, Component.translatable("quartz_flower_helper.tooltip.mod_all"), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= getScrollbarPosition() && mouseX <= getScrollbarPosition() + 6 && mouseY >= this.y0 && mouseY <= this.y1) {
            this.scrollVelocity = 0.0;
            this.isDraggingScrollbar = true;
            return true;
        }
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        if (button == 0) {
            this.scrollVelocity = 0.0;
            this.mouseDownX = mouseX;
            this.mouseDownY = mouseY;
            this.lastDragY = mouseY;
            this.lastDragTime = System.currentTimeMillis();
            this.dragVelocity = 0.0;
            this.initialScrollAmount = this.getScrollAmount();
            this.isDraggingContent = true;
            this.hasDragged = false;
            this.pressedEntry = this.findEntryAtPosition(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
            if (this.isDraggingContent) {
                this.isDraggingContent = false;
                boolean dragged = this.hasDragged;
                this.hasDragged = false;
                Entry entry = this.pressedEntry;
                this.pressedEntry = null;

                if (!dragged && entry != null) {
                    entry.mouseClicked(mouseX, mouseY, 0);
                } else if (dragged) {
                    long now = System.currentTimeMillis();
                    if (now - this.lastDragTime <= 100) {
                        this.scrollVelocity = Math.max(-3000.0, Math.min(3000.0, this.dragVelocity));
                    } else {
                        this.scrollVelocity = 0.0;
                    }
                    this.lastFrameTime = now;
                }
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.isDraggingScrollbar) {
            this.scrollVelocity = 0.0;
            int maxScroll = this.getMaxScroll();
            if (maxScroll > 0) {
                int listH = this.y1 - this.y0;
                int scrollbarHeight = (int) ((float) (listH * listH) / (float) this.getMaxPosition());
                scrollbarHeight = Math.max(16, Math.min(scrollbarHeight, listH - 4));
                double scrollFactor = (double) maxScroll / (double) Math.max(1, listH - scrollbarHeight);
                this.setScrollAmount(this.getScrollAmount() + dragY * scrollFactor);
            }
            return true;
        }

        if (button == 0 && this.isDraggingContent) {
            double deltaY = mouseY - this.mouseDownY;
            double distance = Math.hypot(mouseX - this.mouseDownX, deltaY);
            if (distance >= DRAG_THRESHOLD) {
                this.hasDragged = true;
            }
            if (this.hasDragged) {
                this.setScrollAmount(this.initialScrollAmount - deltaY);
                long now = System.currentTimeMillis();
                long dtMs = now - this.lastDragTime;
                if (dtMs > 5) {
                    double instV = (mouseY - this.lastDragY) / (dtMs / 1000.0);
                    this.dragVelocity = this.dragVelocity * 0.4 + instV * 0.6;
                    this.lastDragY = mouseY;
                    this.lastDragTime = now;
                }
            }
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.isMouseOver(mouseX, mouseY)) {
            return false;
        }
        this.scrollVelocity += delta * 350.0;
        this.scrollVelocity = Math.max(-3000.0, Math.min(3000.0, this.scrollVelocity));
        return true;
    }

    @Override
    protected int getScrollbarPosition() {
        return this.getRight() - 5;
    }

    @Override
    public int getRowWidth() {
        return this.width - 10;
    }

    @Override
    protected void renderSelection(@NotNull GuiGraphics guiGraphics, int top, int width, int height, int outerColor, int innerColor) {
        // Suppress default selection box outline when clicked
    }

    @Override
    protected boolean isSelectedItem(int index) {
        return false;
    }

    @Override
    public void updateNarration(@NotNull NarrationElementOutput output) {}

    public abstract static class Entry extends AbstractSelectionList.Entry<Entry> {}

    public class AllEntry extends Entry {
        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (top + height < y0 || top > y1) return;

            if (hovered && !ModFilterList.this.hasDragged) {
                guiGraphics.fill(x0 + 1, top, x1 - 5, top + height, 0x22FFFFFF);
            }

            boolean isChecked = selectedMods.isEmpty();
            int color = isChecked ? 0xFFFFD700 : 0xFFAAAAAA;
            String mark = isChecked ? "§a■ " : "§8□ ";
            Component text = Component.literal(mark).append(Component.translatable("quartz_flower_helper.mod.all"));

            guiGraphics.drawString(minecraft.font, text, left, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            selectedMods.clear();
            if (onFilterChanged != null) onFilterChanged.run();
            return true;
        }
    }

    public class ModItemEntry extends Entry {
        public final String modId;
        public final String displayName;

        public ModItemEntry(String modId) {
            this.modId = modId;
            this.displayName = ModList.get().getModContainerById(modId)
                    .map(c -> c.getModInfo().getDisplayName())
                    .orElse(modId);
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (top + height < y0 || top > y1) return;

            if (hovered && !ModFilterList.this.hasDragged) {
                guiGraphics.fill(x0 + 1, top, x1 - 5, top + height, 0x22FFFFFF);
            }

            boolean isChecked = selectedMods.contains(modId);
            int color = isChecked ? 0xFFFFD700 : 0xFFAAAAAA;
            String mark = isChecked ? "§a■ " : "§8□ ";

            String full = mark + displayName;
            String trimmed = minecraft.font.plainSubstrByWidth(full, width - 8);
            guiGraphics.drawString(minecraft.font, trimmed, left, top + (height - 9) / 2, color, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (selectedMods.contains(modId)) {
                selectedMods.remove(modId);
            } else {
                selectedMods.add(modId);
            }
            if (onFilterChanged != null) onFilterChanged.run();
            return true;
        }
    }
}
