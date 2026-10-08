package top.morenrx.quartz_flower_helper.config;

import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;

public class TargetCriteria {
    private final Set<ResourceLocation> selectedAttributes = new HashSet<>();
    private final Set<ResourceLocation> selectedProvideEffects = new HashSet<>();
    private final Set<ResourceLocation> selectedImmuneEffects = new HashSet<>();

    private double minAttrPercent = 0;
    private double maxAttrPercent = 0;

    public Set<ResourceLocation> getSelectedAttributes() {
        return selectedAttributes;
    }

    public Set<ResourceLocation> getSelectedProvideEffects() {
        return selectedProvideEffects;
    }

    public Set<ResourceLocation> getSelectedImmuneEffects() {
        return selectedImmuneEffects;
    }

    public double getMinAttrPercent() {
        return minAttrPercent;
    }

    public void setMinAttrPercent(double minAttrPercent) {
        this.minAttrPercent = minAttrPercent;
    }

    public double getMaxAttrPercent() {
        return maxAttrPercent;
    }

    public void setMaxAttrPercent(double maxAttrPercent) {
        this.maxAttrPercent = maxAttrPercent;
    }
}
