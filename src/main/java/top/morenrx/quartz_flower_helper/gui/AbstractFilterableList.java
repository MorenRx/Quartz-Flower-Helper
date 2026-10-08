package top.morenrx.quartz_flower_helper.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.morenrx.quartz_flower_helper.compat.JecHelper;
import top.morenrx.quartz_flower_helper.compat.ModCompat;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public abstract class AbstractFilterableList extends AbstractSelectionList<AbstractFilterableList.Entry> {
    protected String currentFilter = "";
    protected final Set<String> modFilter = new HashSet<>();
    protected boolean isDraggingScrollbar = false;
    protected AbstractItemEntry hoveredItem = null;

    protected double mouseDownX = 0;
    protected double mouseDownY = 0;
    protected double initialScrollAmount = 0;
    protected boolean isDraggingContent = false;
    protected boolean hasDragged = false;
    protected Entry pressedEntry = null;
    protected static final double DRAG_THRESHOLD = 4.0;

    protected double scrollVelocity = 0.0;
    protected long lastFrameTime = 0L;
    protected double lastDragY = 0;
    protected long lastDragTime = 0L;
    protected double dragVelocity = 0.0;
    protected static final double FRICTION = 8.5;

    public AbstractFilterableList(Minecraft mc, int width, int height, int top, int bottom, int itemHeight) {
        super(mc, width, height, top, bottom, itemHeight);
    }

    public void resetInertia() {
        this.scrollVelocity = 0.0;
        this.isDraggingContent = false;
        this.isDraggingScrollbar = false;
        this.hasDragged = false;
        this.pressedEntry = null;
        this.lastFrameTime = 0L;
    }

    public static boolean containsMatch(String target, String query) {
        if (target == null || query == null) return false;
        if (query.isEmpty()) return true;

        if (target.toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))) {
            return true;
        }

        if (ModCompat.isJecharactersLoaded()) {
            return JecHelper.contains(target, query);
        }

        return false;
    }

    public void setFilter(String query) {
        this.scrollVelocity = 0.0;
        this.currentFilter = query.trim();
        rebuildEntries();
    }

    public void setModFilter(Set<String> selectedMods) {
        this.scrollVelocity = 0.0;
        this.modFilter.clear();
        this.modFilter.addAll(selectedMods);
        rebuildEntries();
    }

    public abstract void rebuildEntries();

    public abstract void clearSelection();

    @Override
    public int getMaxPosition() {
        int total = 2;
        for (Entry e : this.children()) {
            total += e.getHeight();
        }
        return total;
    }

    @Override
    public int getRowTop(int index) {
        int top = this.y0 + 1 - (int) this.getScrollAmount();
        for (int i = 0; i < index && i < this.children().size(); i++) {
            top += this.children().get(i).getHeight();
        }
        return top;
    }

    @Override
    public int getRowBottom(int index) {
        if (index >= 0 && index < this.children().size()) {
            return this.getRowTop(index) + this.children().get(index).getHeight();
        }
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
            int entryHeight = entry.getHeight();
            int bottom = top + entryHeight;
            if (bottom >= this.y0 && top <= this.y1) {
                entry.render(graphics, i, top, rowLeft, rowWidth, entryHeight, mouseX, mouseY,
                        this.isMouseOver(mouseX, mouseY) && this.findEntryAtPosition(mouseX, mouseY) == entry, partialTick);
            }
            top += entryHeight;
        }
    }

    @Nullable
    public SeparatorEntry getCurrentStickySeparator() {
        if (this.getMaxScroll() <= 0) return null;
        SeparatorEntry currentStickySep = null;
        int currentY = this.y0 + 1 - (int) this.getScrollAmount();

        for (int i = 0; i < this.children().size(); i++) {
            Entry e = this.children().get(i);
            if (e instanceof SeparatorEntry sep) {
                if (currentY <= this.y0 + 1) {
                    currentStickySep = sep;
                } else {
                    break;
                }
            }
            currentY += e.getHeight();
        }
        return currentStickySep;
    }

    @Nullable
    public Entry findEntryAtPosition(double mouseX, double mouseY) {
        if (mouseX < this.x0 || mouseX > this.x1 || mouseY < this.y0 || mouseY > this.y1) {
            return null;
        }

        SeparatorEntry stickySep = getCurrentStickySeparator();
        if (stickySep != null && mouseY >= this.y0 + 1 && mouseY < this.y0 + 1 + 16) {
            return stickySep;
        }

        int top = this.y0 + 1 - (int) this.getScrollAmount();
        for (Entry entry : this.children()) {
            int bottom = top + entry.getHeight();
            if (mouseY >= top && mouseY < bottom) {
                return entry;
            }
            top = bottom;
        }
        return null;
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        hoveredItem = null;

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

        // Draw clean list background inside bounds
        guiGraphics.fill(this.x0, this.y0, this.x1, this.y1, 0x90111111);
        guiGraphics.renderOutline(this.x0, this.y0, this.x1 - this.x0, this.y1 - this.y0, 0xFF444444);

        // Scissor clip items strictly inside the list frame
        guiGraphics.enableScissor(this.x0 + 1, this.y0 + 1, this.x1 - 1, this.y1 - 1);
        this.renderList(guiGraphics, mouseX, mouseY, partialTick);

        // Sticky Header Label at the top (full row width, opaque background without border, same layout as separator)
        SeparatorEntry stickySep = getCurrentStickySeparator();
        if (stickySep != null) {
            int stickyH = 16;
            guiGraphics.fill(this.x0 + 1, this.y0 + 1, this.x1 - 1, this.y0 + 1 + stickyH, 0xFF24252A);
            guiGraphics.drawString(minecraft.font, stickySep.title, this.x0 + 6, this.y0 + 5, 0xFFFFCC00, false);
        }

        guiGraphics.disableScissor();

        // Detect hovered item (suppressed while dragging or fast gliding)
        if (!hasDragged && Math.abs(this.scrollVelocity) < 60.0 && mouseX >= this.x0 && mouseX <= this.x1 && mouseY >= this.y0 && mouseY <= this.y1) {
            Entry e = this.findEntryAtPosition(mouseX, mouseY);
            if (e instanceof AbstractItemEntry item) {
                hoveredItem = item;
            }
        }

        // Draggable custom slim scrollbar
        int maxScroll = this.getMaxScroll();
        if (maxScroll > 0) {
            int scrollbarPosition = this.getScrollbarPosition();
            int listH = this.y1 - this.y0;
            int scrollbarHeight = (int) ((float) (listH * listH) / (float) this.getMaxPosition());
            scrollbarHeight = Math.max(16, Math.min(scrollbarHeight, listH - 4));
            int scrollbarY = (int) this.getScrollAmount() * (listH - scrollbarHeight) / maxScroll + this.y0;

            guiGraphics.fill(scrollbarPosition, this.y0, scrollbarPosition + 4, this.y1, 0xFF1A1A1A);
            int barColor = isDraggingScrollbar ? 0xFFAAAAAA : 0xFF777777;
            guiGraphics.fill(scrollbarPosition, scrollbarY, scrollbarPosition + 4, scrollbarY + scrollbarHeight, barColor);
        }
    }

    public void renderHoverTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (hoveredItem != null) {
            renderItemTooltip(guiGraphics, hoveredItem, mouseX, mouseY);
        }
    }

    protected abstract void renderItemTooltip(GuiGraphics guiGraphics, AbstractItemEntry item, int mouseX, int mouseY);

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

        if (button == 1) { // Right click: favorite toggle
            this.scrollVelocity = 0.0;
            Entry entry = this.findEntryAtPosition(mouseX, mouseY);
            if (entry != null) {
                return entry.mouseClicked(mouseX, mouseY, 1);
            }
            return false;
        }

        if (button == 0) { // Left click: prepare drag-scroll or short click
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
    public void updateNarration(@NotNull NarrationElementOutput output) {}

    public abstract static class Entry extends AbstractSelectionList.Entry<Entry> {
        public abstract int getHeight();
    }

    public class SeparatorEntry extends Entry {
        public final Component title;

        public SeparatorEntry(Component title) {
            this.title = title;
        }

        @Override
        public int getHeight() {
            return 16;
        }

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (top + height < y0 || top > y1) return;
            guiGraphics.fill(x0 + 1, top, x1 - 1, top + 16, 0xFF24252A);
            guiGraphics.drawString(minecraft.font, title, x0 + 6, top + 4, 0xFFFFCC00, false);
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 0) {
                int targetScroll = 0;
                for (Entry e : AbstractFilterableList.this.children()) {
                    if (e == this) break;
                    targetScroll += e.getHeight();
                }
                AbstractFilterableList.this.setScrollAmount(targetScroll);
                return true;
            }
            return false;
        }
    }

    public abstract class AbstractItemEntry extends Entry {
        @Override
        public int getHeight() {
            return 14;
        }

        public abstract Component getDisplayName();
        public abstract boolean isSelected();
        public abstract boolean isFavorite();
        public abstract int getTextColor();
        public abstract void onToggleFavorite();
        public abstract void onToggleSelection();

        @Override
        public void render(@NotNull GuiGraphics guiGraphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (top + height < y0 || top > y1) return;

            if (hovered && !AbstractFilterableList.this.hasDragged) {
                guiGraphics.fill(x0 + 1, top, x1 - 5, top + height, 0x22FFFFFF);
            }

            boolean selected = isSelected();
            boolean favorite = isFavorite();
            int color = getTextColor();

            String checkMark = selected ? "§a■ " : "§8□ ";
            String favMark = favorite ? "§6★ " : "";

            Component text = Component.literal(favMark + checkMark).append(getDisplayName());
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(left + 3, top + (height - 9 * 0.85f) / 2.0f, 0);
            guiGraphics.pose().scale(0.85f, 0.85f, 1.0f);
            guiGraphics.drawString(minecraft.font, text, 0, 0, color, false);
            guiGraphics.pose().popPose();
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == 1) { // Right-click: Toggle Favorite (immediately re-sorts)
                onToggleFavorite();
                rebuildEntries();
                return true;
            }

            if (button == 0) { // Left-click: Toggle Selection (does NOT re-sort immediately)
                onToggleSelection();
                return true;
            }

            return false;
        }
    }
}
