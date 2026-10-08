package top.morenrx.quartz_flower_helper.mixin;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import auviotre.enigmatic.addon.contents.gui.ArtificialFlowerMenu;
import auviotre.enigmatic.addon.contents.items.ArtificialFlower;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;
import top.morenrx.quartz_flower_helper.controller.AutoRollManager;
import top.morenrx.quartz_flower_helper.gui.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Mixin(ArtificialFlowerScreen.class)
public abstract class ArtificialFlowerScreenMixin extends AbstractContainerScreen<ArtificialFlowerMenu> implements ArtificialFlowerScreenExtension {

    @Unique
    private AttributeConfigPanel qfh$attrPanel;
    @Unique
    private EffectConfigPanel qfh$effectPanel;

    public ArtificialFlowerScreenMixin(ArtificialFlowerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        ArtificialFlowerScreen self = (ArtificialFlowerScreen) (Object) this;
        qfh$attrPanel = new AttributeConfigPanel(self);
        qfh$effectPanel = new EffectConfigPanel(self);

        qfh$attrPanel.init(this.width, this.height, qfh$collectAttributeMods());
        qfh$effectPanel.init(this.width, this.height, qfh$collectEffectMods());
    }

    @Unique
    private Set<String> qfh$collectAttributeMods() {
        Set<String> mods = new TreeSet<>();
        if (ArtificialFlower.Helper.attributePool != null && !ArtificialFlower.Helper.attributePool.isEmpty()) {
            for (Attribute attr : ArtificialFlower.Helper.attributePool) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (id != null) mods.add(id.getNamespace());
            }
        } else {
            for (Attribute attr : ForgeRegistries.ATTRIBUTES.getValues()) {
                ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attr);
                if (id != null) mods.add(id.getNamespace());
            }
        }
        return mods;
    }

    @Unique
    private Set<String> qfh$collectEffectMods() {
        Set<String> mods = new TreeSet<>();
        if (ArtificialFlower.Helper.allEffectPool != null && !ArtificialFlower.Helper.allEffectPool.isEmpty()) {
            for (MobEffect effect : ArtificialFlower.Helper.allEffectPool) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (id != null) mods.add(id.getNamespace());
            }
        } else {
            for (MobEffect effect : ForgeRegistries.MOB_EFFECTS.getValues()) {
                ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
                if (id != null) mods.add(id.getNamespace());
            }
        }
        return mods;
    }

    @Override
    public AbstractConfigPanel qfh$getActivePanel() {
        if (qfh$attrPanel != null && qfh$attrPanel.isVisible()) {
            return qfh$attrPanel;
        }
        if (qfh$effectPanel != null && qfh$effectPanel.isVisible()) {
            return qfh$effectPanel;
        }
        return null;
    }

    @Override
    public AttributeConfigPanel qfh$getAttrPanel() {
        return qfh$attrPanel;
    }

    @Override
    public EffectConfigPanel qfh$getEffectPanel() {
        return qfh$effectPanel;
    }
    
    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void onRenderTooltip(GuiGraphics graphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (qfh$getActivePanel() != null) {
            ci.cancel();
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        boolean modalOpen = activePanel != null;

        // Active rolling green indicators on flower buttons
        for (int id = 0; id < 3; id++) {
            if (AutoRollManager.INSTANCE.isSlotActive(id)) {
                boolean inGrace = AutoRollManager.INSTANCE.isSlotInGrace(id);
                int fillColor = inGrace ? 0x60FFFF00 : 0x6000FF00;
                int outlineColor = inGrace ? 0xFFFFD700 : 0xFF00FF00;
                int bx = this.leftPos + 53;
                int by = this.topPos + 14 + 20 * id;
                graphics.fill(bx, by, bx + 10, by + 10, fillColor);
                graphics.renderOutline(bx - 1, by - 1, 12, 12, outlineColor);
            }
        }

        for (int id = 4; id < 6; id++) {
            if (AutoRollManager.INSTANCE.isSlotActive(id)) {
                boolean inGrace = AutoRollManager.INSTANCE.isSlotInGrace(id);
                int fillColor = inGrace ? 0x60FFFF00 : 0x6000FF00;
                int outlineColor = inGrace ? 0xFFFFD700 : 0xFF00FF00;
                int bx = this.leftPos + 139;
                int by = this.topPos + 16 + 26 * (id - 4);
                graphics.fill(bx, by, bx + 20, by + 20, fillColor);
                graphics.renderOutline(bx - 1, by - 1, 22, 22, outlineColor);
            }
        }

        // Calculate positions of config buttons placed behind "属性" and "药水效果"
        int attrTextW = this.font.width(Component.translatable("gui.enigmaticaddons.artificial_flower_attribute"));
        int attrIconX = this.leftPos + 10 + attrTextW + 3;
        int attrIconY = this.topPos + 5;
        boolean hoverAttr = mouseX >= this.leftPos + 8 && mouseX < attrIconX + 11 && mouseY >= this.topPos + 3 && mouseY < this.topPos + 15;
        qfh$renderSliderIcon(graphics, attrIconX, attrIconY, hoverAttr);

        int effectTextW = this.font.width(Component.translatable("gui.enigmaticaddons.artificial_flower_effect"));
        int effectIconX = this.leftPos + 100 + effectTextW + 3;
        int effectIconY = this.topPos + 5;
        boolean hoverEffect = mouseX >= this.leftPos + 98 && mouseX < effectIconX + 11 && mouseY >= this.topPos + 3 && mouseY < this.topPos + 15;
        qfh$renderSliderIcon(graphics, effectIconX, effectIconY, hoverEffect);

        // Pool probability info dynamically calculated from flower item
        ItemStack flower = ArtificialFlower.Helper.getFlowerStack(this.menu.player, false);
        int count = 0;
        if (flower != null && !flower.isEmpty() && flower.hasTag() && flower.getTag().contains("AllEffectCount")) {
            count = flower.getTag().getInt("AllEffectCount");
        }
        int divisor = Math.max(1, (count + 1) / 2);
        double prob = 100.0 / divisor;
        DecimalFormat df = new DecimalFormat("0.##");
        String probStr = df.format(prob) + "%";
        String poolText = Component.translatable("quartz_flower_helper.gui.pool_prob_short", probStr).getString();

        int poolX = this.leftPos + 92;
        int poolY = this.topPos + 72;
        int poolTextW = this.font.width(poolText);
        boolean hoverPool = mouseX >= poolX && mouseX <= poolX + poolTextW && mouseY >= poolY - 1 && mouseY <= poolY + 9;
        graphics.drawString(this.font, poolText, poolX, poolY, 0xFF404040, false);

        // Top-right question mark button [?]
        int qx = this.leftPos + this.imageWidth - 14;
        int qy = this.topPos + 3;
        boolean hoverQ = mouseX >= qx && mouseX < qx + 11 && mouseY >= qy && mouseY < qy + 11;
        graphics.fill(qx, qy, qx + 11, qy + 11, hoverQ ? 0x80555555 : 0x50222222);
        graphics.renderOutline(qx, qy, 11, 11, hoverQ ? 0xFFFFCC00 : 0xFF666666);
        graphics.drawString(this.font, "?", qx + 3, qy + 2, hoverQ ? 0xFFFFCC00 : 0xFFCCCCCC, false);

        // Operation instructions box rendered ABOVE the screen (default shown, persistent toggle)
        if (AutoRollConfig.getInstance().showHelp) {
            int hy = Math.max(2, this.topPos - 56);
            graphics.fill(this.leftPos, hy, this.leftPos + this.imageWidth, hy + 54, 0xD8101010);
            graphics.renderOutline(this.leftPos, hy, this.imageWidth, 54, 0xFF444444);

            graphics.drawString(this.font, Component.translatable("quartz_flower_helper.help.title").getString(), this.leftPos + 5, hy + 3, 0xFFFFCC00, false);
            graphics.drawString(this.font, Component.translatable("quartz_flower_helper.help.line1").getString(), this.leftPos + 5, hy + 13, 0xFFFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("quartz_flower_helper.help.line2").getString(), this.leftPos + 5, hy + 23, 0xFFFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("quartz_flower_helper.help.line3").getString(), this.leftPos + 5, hy + 33, 0xFFFFFFFF, false);
            graphics.drawString(this.font, Component.translatable("quartz_flower_helper.help.hide_tip").getString(), this.leftPos + 5, hy + 43, 0xFFAAAAAA, false);
        }

        // Tooltips for flower screen elements (only when modal is NOT open)
        if (!modalOpen) {
            if (hoverAttr) {
                graphics.renderTooltip(this.font, Component.translatable("quartz_flower_helper.tooltip.config_attr"), mouseX, mouseY);
            } else if (hoverEffect) {
                graphics.renderTooltip(this.font, Component.translatable("quartz_flower_helper.tooltip.config_effect"), mouseX, mouseY);
            } else if (hoverQ) {
                graphics.renderTooltip(this.font, Component.translatable("quartz_flower_helper.tooltip.toggle_help"), mouseX, mouseY);
            } else if (hoverPool) {
                List<Component> poolTooltip = new ArrayList<>();
                poolTooltip.add(Component.translatable("quartz_flower_helper.tooltip.pool_prob", probStr).withStyle(ChatFormatting.GOLD));
                poolTooltip.add(Component.translatable("quartz_flower_helper.tooltip.pool_count", count).withStyle(ChatFormatting.YELLOW));
                poolTooltip.add(Component.translatable("quartz_flower_helper.tooltip.pool_warn1").withStyle(ChatFormatting.RED));
                poolTooltip.add(Component.translatable("quartz_flower_helper.tooltip.pool_warn2").withStyle(ChatFormatting.RED));
                graphics.renderComponentTooltip(this.font, poolTooltip, mouseX, mouseY);
            }
        }

        // Render Centered Modal Dialog strictly at elevated Z-layer (400.0F)
        if (activePanel != null) {
            graphics.flush();
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 400.0F);

            activePanel.render(graphics, mouseX, mouseY, partialTicks);

            graphics.flush();
            graphics.pose().popPose();
        }

        // In-GUI toast notifications rendered below the flower screen
        FlowerNotificationManager.render(graphics, this.font, this.leftPos, this.topPos, this.imageWidth, this.imageHeight, this.width, this.height);
    }

    @Unique
    private void qfh$renderSliderIcon(GuiGraphics graphics, int bx, int by, boolean hovered) {
        int lineColor = hovered ? 0xFFFFCC00 : 0xFF777777;
        int knobColor = hovered ? 0xFFFFFFFF : 0xFFAAAAAA;

        // Custom pixel-art slider preferences icon (9x8)
        // Bar 1: line from bx to bx+8, knob at bx+2
        graphics.fill(bx, by + 1, bx + 9, by + 2, lineColor);
        graphics.fill(bx + 2, by, bx + 4, by + 3, knobColor);

        // Bar 2: line from bx to bx+8, knob at bx+6
        graphics.fill(bx, by + 4, bx + 9, by + 5, lineColor);
        graphics.fill(bx + 6, by + 3, bx + 8, by + 6, knobColor);

        // Bar 3: line from bx to bx+8, knob at bx+3
        graphics.fill(bx, by + 7, bx + 9, by + 8, lineColor);
        graphics.fill(bx + 3, by + 6, bx + 5, by + 9, knobColor);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ArtificialFlowerScreen self = (ArtificialFlowerScreen) (Object) this;
        AutoRollManager.INSTANCE.onScreenTick(self);
    }

    @Override
    public void onClose() {
        super.onClose();
        if (qfh$attrPanel != null) qfh$attrPanel.close();
        if (qfh$effectPanel != null) qfh$effectPanel.close();
        AutoRollManager.INSTANCE.onScreenClose();
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            activePanel.mouseClicked(mouseX, mouseY, button);
            cir.setReturnValue(true);
            return;
        }

        // Check top-right [?] button
        int qx = this.leftPos + this.imageWidth - 14;
        int qy = this.topPos + 3;
        if (button == 0 && mouseX >= qx && mouseX < qx + 11 && mouseY >= qy && mouseY < qy + 11) {
            AutoRollConfig.getInstance().showHelp = !AutoRollConfig.getInstance().showHelp;
            AutoRollConfig.save();
            cir.setReturnValue(true);
            return;
        }

        // Check "属性" area and its slider icon
        int attrTextW = this.font.width(Component.translatable("gui.enigmaticaddons.artificial_flower_attribute"));
        int attrIconX = this.leftPos + 10 + attrTextW + 3;
        if (button == 0 && mouseX >= this.leftPos + 8 && mouseX < attrIconX + 11 && mouseY >= this.topPos + 3 && mouseY < this.topPos + 15) {
            if (qfh$effectPanel != null) qfh$effectPanel.close();
            if (qfh$attrPanel != null) {
                qfh$attrPanel.open();
                cir.setReturnValue(true);
                return;
            }
        }

        // Check "药水效果" area and its slider icon
        int effectTextW = this.font.width(Component.translatable("gui.enigmaticaddons.artificial_flower_effect"));
        int effectIconX = this.leftPos + 100 + effectTextW + 3;
        if (button == 0 && mouseX >= this.leftPos + 98 && mouseX < effectIconX + 11 && mouseY >= this.topPos + 3 && mouseY < this.topPos + 15) {
            if (qfh$attrPanel != null) qfh$attrPanel.close();
            if (qfh$effectPanel != null) {
                qfh$effectPanel.open();
                cir.setReturnValue(true);
                return;
            }
        }

        // Right-click directly on flower buttons (button == 1) to toggle auto-roll
        if (button == 1) {
            // Check Attribute buttons (id 0..2)
            for (int id = 0; id < 3; id++) {
                double dx = mouseX - (this.leftPos + 53);
                double dy = mouseY - (this.topPos + 14 + 20 * id);
                if (dx >= 0.0 && dx < 10.0 && dy >= 0.0 && dy < 10.0) {
                    AutoRollManager.INSTANCE.toggleSlot(id);
                    cir.setReturnValue(true);
                    return;
                }
            }

            // Check Effect buttons (id 4..5)
            for (int id = 4; id < 6; id++) {
                double dx = mouseX - (this.leftPos + 139);
                double dy = mouseY - (this.topPos + 16 + 26 * (id - 4));
                if (dx >= 0.0 && dx < 20.0 && dy >= 0.0 && dy < 20.0) {
                    AutoRollManager.INSTANCE.toggleSlot(id);
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            return activePanel.mouseScrolled(mouseX, mouseY, delta);
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            return activePanel.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            return activePanel.mouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            return activePanel.keyPressed(keyCode, scanCode, modifiers);
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        AbstractConfigPanel activePanel = qfh$getActivePanel();
        if (activePanel != null) {
            return activePanel.charTyped(codePoint, modifiers);
        }
        return super.charTyped(codePoint, modifiers);
    }
}
