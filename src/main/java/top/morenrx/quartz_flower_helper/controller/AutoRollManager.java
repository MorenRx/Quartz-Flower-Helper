package top.morenrx.quartz_flower_helper.controller;

import auviotre.enigmatic.addon.client.screens.ArtificialFlowerScreen;
import auviotre.enigmatic.addon.contents.gui.ArtificialFlowerMenu;
import auviotre.enigmatic.addon.registries.EnigmaticAddonItems;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import top.morenrx.quartz_flower_helper.config.AutoRollConfig;
import top.morenrx.quartz_flower_helper.config.TargetCriteria;
import top.morenrx.quartz_flower_helper.gui.FlowerNotificationManager;

import java.util.*;

public class AutoRollManager {
    public static final AutoRollManager INSTANCE = new AutoRollManager();

    private final TargetCriteria criteria = new TargetCriteria();
    private final Set<Integer> activeSlots = new LinkedHashSet<>();
    private final Map<Integer, Integer> slotGraceTimers = new HashMap<>();

    // 缺少材料后自动停止刷新的延迟
    private static final int MATERIAL_BUFFER_TICKS = 60;
    // 创造模式自动重试延迟
    private static final int CREATIVE_WATCHDOG_TICKS = 10;
    // 生存模式自动重试延迟
    private static final int SURVIVAL_WATCHDOG_TICKS = 200;

    private boolean isRolling = false;
    private boolean awaitingResponse = false;
    private int lastRolledSlot = -1;
    private int watchdogTimer = 0;
    private int materialResponseTicks = -1;

    private AutoRollManager() {
    }

    public TargetCriteria getCriteria() {
        return criteria;
    }

    public boolean isSlotActive(int slotId) {
        return activeSlots.contains(slotId);
    }

    public boolean isSlotInGrace(int slotId) {
        return slotGraceTimers.containsKey(slotId);
    }

    private static Component getSlotName(int slotId) {
        return switch (slotId) {
            case 0 -> Component.translatable("quartz_flower_helper.slot.0");
            case 1 -> Component.translatable("quartz_flower_helper.slot.1");
            case 2 -> Component.translatable("quartz_flower_helper.slot.2");
            case 4 -> Component.translatable("quartz_flower_helper.slot.4");
            case 5 -> Component.translatable("quartz_flower_helper.slot.5");
            default -> Component.translatable("quartz_flower_helper.slot.unknown", slotId);
        };
    }

    public void toggleSlot(int slotId) {
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof ArtificialFlowerScreen screen) || mc.player == null) return;

        if (activeSlots.contains(slotId)) {
            activeSlots.remove(slotId);
            slotGraceTimers.remove(slotId);
            FlowerNotificationManager.warning(Component.translatable("quartz_flower_helper.msg.stopped_slot", getSlotName(slotId)).withStyle(ChatFormatting.YELLOW));
            if (activeSlots.isEmpty()) {
                stopAll();
            } else if (isRolling && !awaitingResponse) {
                Integer nextSlot = getNextReadySlot(screen.getMenu());
                if (nextSlot != null) {
                    sendRollForSlot(screen, nextSlot);
                }
            }
        } else {
            if (isAttributeSlot(slotId) && criteria.getSelectedAttributes().isEmpty()) {
                FlowerNotificationManager.error(Component.translatable("quartz_flower_helper.msg.select_attr_first").withStyle(ChatFormatting.RED));
                return;
            }
            if (slotId == 4 && criteria.getSelectedProvideEffects().isEmpty()) {
                FlowerNotificationManager.error(Component.translatable("quartz_flower_helper.msg.select_provide_first").withStyle(ChatFormatting.RED));
                return;
            }
            if (slotId == 5 && criteria.getSelectedImmuneEffects().isEmpty()) {
                FlowerNotificationManager.error(Component.translatable("quartz_flower_helper.msg.select_immune_first").withStyle(ChatFormatting.RED));
                return;
            }

            activeSlots.add(slotId);
            FlowerNotificationManager.info(Component.translatable("quartz_flower_helper.msg.started_slot", getSlotName(slotId)).withStyle(ChatFormatting.GREEN));

            if (!isRolling) {
                isRolling = true;
                watchdogTimer = 0;
                materialResponseTicks = -1;
                lastRolledSlot = -1;
                slotGraceTimers.clear();
                if (!mc.player.isCreative()) {
                    ensureMaterialSupplied(screen.getMenu());
                }
                Integer nextSlot = getNextReadySlot(screen.getMenu());
                if (nextSlot != null) {
                    sendRollForSlot(screen, nextSlot);
                }
            }
        }
    }

    public void stopAll() {
        isRolling = false;
        awaitingResponse = false;
        lastRolledSlot = -1;
        materialResponseTicks = -1;
        slotGraceTimers.clear();
        activeSlots.clear();
    }

    public void onScreenClose() {
        stopAll();
        FlowerNotificationManager.clear();
        AutoRollConfig.save();
    }

    public void onScreenTick(ArtificialFlowerScreen screen) {
        if (!isRolling || activeSlots.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        boolean isCreative = mc.player != null && mc.player.isCreative();

        ArtificialFlowerMenu menu = screen.getMenu();

        if (!isCreative) {
            List<Integer> expiredSlots = new ArrayList<>();

            for (int slotId : new ArrayList<>(activeSlots)) {
                if (isMaterialAvailable(menu, slotId)) {
                    slotGraceTimers.remove(slotId);
                } else {
                    int timer = slotGraceTimers.getOrDefault(slotId, MATERIAL_BUFFER_TICKS);
                    timer--;
                    if (timer <= 0) {
                        expiredSlots.add(slotId);
                        slotGraceTimers.remove(slotId);
                    } else {
                        slotGraceTimers.put(slotId, timer);
                    }
                }
            }

            if (!expiredSlots.isEmpty()) {
                boolean playedSound = false;
                for (int slotId : expiredSlots) {
                    activeSlots.remove(slotId);
                    boolean isAttr = isAttributeSlot(slotId);
                    Component materialName = Component.translatable(isAttr ? "quartz_flower_helper.material.lapis" : "quartz_flower_helper.material.quartz");
                    FlowerNotificationManager.error(Component.translatable("quartz_flower_helper.msg.out_of_material", materialName, getSlotName(slotId)).withStyle(ChatFormatting.RED));
                    if (!playedSound && mc.player != null) {
                        mc.player.playSound(SoundEvents.NOTE_BLOCK_BASS.get(), 1.0f, 0.6f);
                        playedSound = true;
                    }
                }
                if (activeSlots.isEmpty()) {
                    stopAll();
                    return;
                }
            }

            ensureMaterialSupplied(menu);
        } else {
            slotGraceTimers.clear();
        }

        if (awaitingResponse) {
            if (!isCreative && materialResponseTicks >= 0) {
                materialResponseTicks++;
                if (materialResponseTicks >= 1) {
                    materialResponseTicks = -1;
                    awaitingResponse = false;
                    watchdogTimer = 0;
                    Integer nextSlot = getNextReadySlot(menu);
                    if (nextSlot != null) {
                        sendRollForSlot(screen, nextSlot);
                    }
                }
            } else {
                watchdogTimer++;
                int timeout = isCreative ? CREATIVE_WATCHDOG_TICKS : SURVIVAL_WATCHDOG_TICKS;
                if (watchdogTimer >= timeout) {
                    watchdogTimer = 0;
                    awaitingResponse = false;
                    Integer nextSlot = getNextReadySlot(menu);
                    if (nextSlot != null) {
                        sendRollForSlot(screen, nextSlot);
                    }
                }
            }
        } else {
            Integer nextSlot = getNextReadySlot(menu);
            if (nextSlot != null) {
                sendRollForSlot(screen, nextSlot);
            }
        }
    }

    public void onContainerSetSlot(ClientboundContainerSetSlotPacket packet) {
        if (!isRolling || activeSlots.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof ArtificialFlowerScreen screen) || mc.player == null) return;
        if (packet.getContainerId() != mc.player.containerMenu.containerId) return;

        if (packet.getItem().is(EnigmaticAddonItems.ARTIFICIAL_FLOWER)) {
            awaitingResponse = false;
            watchdogTimer = 0;
            materialResponseTicks = -1;

            CompoundTag tag = packet.getItem().getTag();

            Iterator<Integer> it = activeSlots.iterator();
            while (it.hasNext()) {
                int slotId = it.next();
                if (isTargetReached(tag, slotId)) {
                    it.remove();
                    slotGraceTimers.remove(slotId);
                    mc.player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.2f);
                    FlowerNotificationManager.success(Component.translatable("quartz_flower_helper.msg.target_reached", getSlotName(slotId)).withStyle(ChatFormatting.GOLD));
                }
            }

            if (activeSlots.isEmpty()) {
                stopAll();
                return;
            }

            Integer nextSlot = getNextReadySlot(screen.getMenu());
            if (nextSlot != null) {
                sendRollForSlot(screen, nextSlot);
            }
            return;
        }

        boolean isCreative = mc.player != null && mc.player.isCreative();
        if (!isCreative && lastRolledSlot != -1 && awaitingResponse) {
            int expectedMaterialSlot = isAttributeSlot(lastRolledSlot) ? 0 : 1;
            if (packet.getSlot() == expectedMaterialSlot) {
                materialResponseTicks = 0;
            }
        }
    }

    private boolean isTargetReached(CompoundTag tag, int slotId) {
        if (tag == null) return false;

        if (isAttributeSlot(slotId)) {
            String attrIdKey = "AttributeId" + (slotId + 1);
            String attrModifierKey = "AttributeModifier" + (slotId + 1);

            if (!tag.contains(attrIdKey) || !tag.contains(attrModifierKey)) return false;

            ResourceLocation attrId = ResourceLocation.tryParse(tag.getString(attrIdKey));
            if (attrId == null) return false;
            if (!criteria.getSelectedAttributes().isEmpty() && !criteria.getSelectedAttributes().contains(attrId)) {
                return false;
            }

            CompoundTag modTag = tag.getCompound(attrModifierKey);
            double amount = modTag.getDouble("Amount") * 100.0;
            return amount >= criteria.getMinAttrPercent() && amount <= criteria.getMaxAttrPercent();
        } else if (slotId == 4) {
            String effectKey = "PotionEffect0";
            if (!tag.contains(effectKey)) return false;

            ResourceLocation effectId = ResourceLocation.tryParse(tag.getString(effectKey));
            if (effectId == null) return false;
            return criteria.getSelectedProvideEffects().isEmpty() || criteria.getSelectedProvideEffects().contains(effectId);
        } else if (slotId == 5) {
            String effectKey = "PotionEffect1";
            if (!tag.contains(effectKey)) return false;

            ResourceLocation effectId = ResourceLocation.tryParse(tag.getString(effectKey));
            if (effectId == null) return false;
            return criteria.getSelectedImmuneEffects().isEmpty() || criteria.getSelectedImmuneEffects().contains(effectId);
        }

        return false;
    }

    private void sendRollForSlot(ArtificialFlowerScreen screen, int targetSlot) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null) return;

        ArtificialFlowerMenu menu = screen.getMenu();

        if (!player.isCreative()) {
            boolean isAttr = isAttributeSlot(targetSlot);

            Item requiredItem = isAttr ? Items.LAPIS_LAZULI : Items.QUARTZ;
            int requiredCount = isAttr ? (menu.costMode == 0 ? 2 : menu.costMode == 1 ? 4 : 8) : 4;
            int materialSlotIndex = isAttr ? 0 : 1;

            ItemStack currentMaterialInSlot = menu.slots.get(materialSlotIndex).getItem();
            boolean slotHasEnough = currentMaterialInSlot.is(requiredItem) && currentMaterialInSlot.getCount() >= requiredCount;

            if (!slotHasEnough) {
                int inventorySlotIndex = findItemSlot(menu, requiredItem);
                if (inventorySlotIndex == -1) return;
                quickMoveItem(menu, inventorySlotIndex);
                return;
            }
        }

        lastRolledSlot = targetSlot;
        awaitingResponse = true;
        materialResponseTicks = -1;
        watchdogTimer = 0;
        mc.gameMode.handleInventoryButtonClick(menu.containerId, targetSlot);
    }

    private void ensureMaterialSupplied(ArtificialFlowerMenu menu) {
        boolean needsLapis = activeSlots.stream().anyMatch(AutoRollManager::isAttributeSlot);
        boolean needsQuartz = activeSlots.contains(4) || activeSlots.contains(5);

        if (needsLapis) {
            ItemStack lapisInSlot = menu.slots.get(0).getItem();
            if (lapisInSlot.isEmpty() || (lapisInSlot.is(Items.LAPIS_LAZULI) && lapisInSlot.getCount() <= 16)) {
                int invSlot = findItemSlot(menu, Items.LAPIS_LAZULI);
                if (invSlot != -1) {
                    quickMoveItem(menu, invSlot);
                }
            }
        }

        if (needsQuartz) {
            ItemStack quartzInSlot = menu.slots.get(1).getItem();
            if (quartzInSlot.isEmpty() || (quartzInSlot.is(Items.QUARTZ) && quartzInSlot.getCount() <= 16)) {
                int invSlot = findItemSlot(menu, Items.QUARTZ);
                if (invSlot != -1) {
                    quickMoveItem(menu, invSlot);
                }
            }
        }
    }

    private void quickMoveItem(ArtificialFlowerMenu menu, int inventorySlotIndex) {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return;
        connection.send(new ServerboundContainerClickPacket(
                menu.containerId,
                menu.getStateId(),
                inventorySlotIndex,
                0,
                ClickType.QUICK_MOVE,
                ItemStack.EMPTY,
                Int2ObjectMaps.emptyMap()
        ));
    }

    private Integer getNextReadySlot(ArtificialFlowerMenu menu) {
        if (activeSlots.isEmpty()) return null;
        Minecraft mc = Minecraft.getInstance();
        boolean isCreative = mc.player != null && mc.player.isCreative();
        List<Integer> slots = new ArrayList<>(activeSlots);
        int startIndex = 0;
        if (lastRolledSlot != -1) {
            int prevIdx = slots.indexOf(lastRolledSlot);
            if (prevIdx != -1) {
                startIndex = (prevIdx + 1) % slots.size();
            }
        }
        for (int i = 0; i < slots.size(); i++) {
            int candidate = slots.get((startIndex + i) % slots.size());
            if (isCreative || isMaterialAvailable(menu, candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isMaterialAvailable(ArtificialFlowerMenu menu, int slotId) {
        boolean isAttr = isAttributeSlot(slotId);
        Item requiredItem = isAttr ? Items.LAPIS_LAZULI : Items.QUARTZ;
        int requiredCount = isAttr ? (menu.costMode == 0 ? 2 : menu.costMode == 1 ? 4 : 8) : 4;
        int materialSlotIndex = isAttr ? 0 : 1;
        ItemStack currentMaterialInSlot = menu.slots.get(materialSlotIndex).getItem();
        if (currentMaterialInSlot.is(requiredItem) && currentMaterialInSlot.getCount() >= requiredCount) {
            return true;
        }
        return findItemSlot(menu, requiredItem) != -1;
    }

    private int findItemSlot(ArtificialFlowerMenu menu, Item targetItem) {
        for (int i = 3; i < menu.slots.size(); i++) {
            Slot s = menu.slots.get(i);
            if (s.container instanceof Inventory && s.hasItem() && s.getItem().is(targetItem)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isAttributeSlot(int slotId) {
        return slotId >= 0 && slotId < 3;
    }
}
