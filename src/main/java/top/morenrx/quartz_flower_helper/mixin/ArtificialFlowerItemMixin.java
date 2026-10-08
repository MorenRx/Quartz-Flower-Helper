package top.morenrx.quartz_flower_helper.mixin;

import auviotre.enigmatic.addon.contents.items.ArtificialFlower;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.DecimalFormat;
import java.util.List;

@Mixin(ArtificialFlower.class)
public abstract class ArtificialFlowerItemMixin {

    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void qfh$appendProbability(ItemStack stack, @Nullable Level worldIn, List<Component> tooltip, TooltipFlag flag, CallbackInfo ci) {
        int count = 0;
        if (stack != null && !stack.isEmpty() && stack.hasTag() && stack.getTag().contains("AllEffectCount")) {
            count = stack.getTag().getInt("AllEffectCount");
        }
        int divisor = Math.max(1, (count + 1) / 2);
        double prob = 100.0 / divisor;
        DecimalFormat df = new DecimalFormat("0.##");
        String probStr = df.format(prob) + "%";

        tooltip.add(Component.translatable("quartz_flower_helper.tooltip.flower_item_prob_label").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(probStr).withStyle(ChatFormatting.YELLOW))
                .append(Component.translatable("quartz_flower_helper.tooltip.flower_item_prob_count", count).withStyle(ChatFormatting.GRAY)));
    }
}
