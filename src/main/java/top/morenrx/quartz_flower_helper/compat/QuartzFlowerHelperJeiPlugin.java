package top.morenrx.quartz_flower_helper.compat;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import top.morenrx.quartz_flower_helper.QuartzFlowerHelper;
import top.morenrx.quartz_flower_helper.gui.AbstractConfigPanel;
import top.morenrx.quartz_flower_helper.gui.ArtificialFlowerScreenExtension;

import java.util.Collections;
import java.util.List;

@JeiPlugin
public class QuartzFlowerHelperJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(QuartzFlowerHelper.MODID, "jei_plugin");

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(ArtificialFlowerScreen.class, new IGuiContainerHandler<ArtificialFlowerScreen>() {
            @Override
            public @NotNull List<Rect2i> getGuiExtraAreas(@NotNull ArtificialFlowerScreen screen) {
                if (screen instanceof ArtificialFlowerScreenExtension ext) {
                    AbstractConfigPanel panel = ext.qfh$getActivePanel();
                    if (panel != null && panel.isVisible()) {
                        return List.of(new Rect2i(panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight()));
                    }
                }
                return Collections.emptyList();
            }
        });
    }
}
