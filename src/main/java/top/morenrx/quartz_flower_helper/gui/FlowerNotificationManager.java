package top.morenrx.quartz_flower_helper.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class FlowerNotificationManager {

    public enum NotificationType {
        INFO(0x2196F3, 0xFF64B5F6),      // Blue
        SUCCESS(0x4CAF50, 0xFF81C784),   // Green
        WARNING(0xFFA000, 0xFFFFB74D),   // Amber
        ERROR(0xE53935, 0xFFE57373);     // Red

        public final int borderColor;
        public final int textColor;

        NotificationType(int borderColor, int textColor) {
            this.borderColor = borderColor;
            this.textColor = textColor;
        }
    }

    private static final List<NotificationItem> notifications = new ArrayList<>();
    private static final long DURATION_MS = 3800L;
    private static final long FADE_MS = 600L;

    public static synchronized void notify(Component message, NotificationType type) {
        long now = System.currentTimeMillis();
        // If an identical message is already displayed, refresh its timer and type
        for (NotificationItem item : notifications) {
            if (item.message.getString().equals(message.getString())) {
                item.timestamp = now;
                item.type = type;
                return;
            }
        }
        notifications.add(new NotificationItem(message, type, now));
        // Keep at most 3 active notifications
        while (notifications.size() > 3) {
            notifications.remove(0);
        }
    }

    public static void info(Component message) {
        notify(message, NotificationType.INFO);
    }

    public static void success(Component message) {
        notify(message, NotificationType.SUCCESS);
    }

    public static void warning(Component message) {
        notify(message, NotificationType.WARNING);
    }

    public static void error(Component message) {
        notify(message, NotificationType.ERROR);
    }

    public static void notify(Component message) {
        notify(message, NotificationType.INFO);
    }

    public static synchronized void clear() {
        notifications.clear();
    }

    public static synchronized void render(GuiGraphics graphics, Font font, int leftPos, int topPos, int imageWidth, int imageHeight, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        notifications.removeIf(item -> (now - item.timestamp) > DURATION_MS);

        if (notifications.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 450.0F);

        int currentY = topPos + imageHeight + 4;

        for (NotificationItem item : notifications) {
            long elapsed = now - item.timestamp;
            float alpha = 1.0f;
            if (elapsed > (DURATION_MS - FADE_MS)) {
                alpha = (float) (DURATION_MS - elapsed) / (float) FADE_MS;
            } else if (elapsed < 200L) {
                alpha = (float) elapsed / 200.0f;
            }
            alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            int alphaInt = (int) (alpha * 255.0f);
            if (alphaInt <= 4) continue;

            int textW = font.width(item.message);
            int baseBorderColor = item.type != null ? item.type.borderColor : 0x555560;

            // Scale text slightly for smooth fit
            float scale = 0.9f;
            int scaledTextW = (int) (textW * scale);
            int cardW = Math.max(imageWidth, scaledTextW + 16);
            if (cardW > screenWidth - 16) {
                cardW = screenWidth - 16;
            }

            int cardX = leftPos + (imageWidth - cardW) / 2;
            int cardH = 15;

            int bg = (Math.min(235, (int) (alpha * 235)) << 24) | 0x141418;
            int border = (alphaInt << 24) | (baseBorderColor & 0x00FFFFFF);

            graphics.fill(cardX, currentY, cardX + cardW, currentY + cardH, bg);
            graphics.renderOutline(cardX, currentY, cardW, cardH, border);

            graphics.pose().pushPose();
            int textX = cardX + (cardW - scaledTextW) / 2;
            int textY = currentY + (cardH - (int) (8 * scale)) / 2;
            graphics.pose().translate(textX, textY, 0.0F);
            graphics.pose().scale(scale, scale, 1.0f);

            int textColor = (alphaInt << 24) | 0xFFFFFF;
            graphics.drawString(font, item.message, 0, 0, textColor, false);

            graphics.pose().popPose();

            currentY += cardH + 2;
        }

        graphics.pose().popPose();
    }

    private static class NotificationItem {
        final Component message;
        NotificationType type;
        long timestamp;

        NotificationItem(Component message, NotificationType type, long timestamp) {
            this.message = message;
            this.type = type;
            this.timestamp = timestamp;
        }
    }
}
