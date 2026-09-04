package org.kylan1940.serverassistantai.message;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.kylan1940.serverassistantai.ServerAssistantAI;

import java.util.List;

public final class MessageUtil {

    private MessageUtil() {
    }

    public static void send(CommandSender sender, String path, String... placeholders) {

        List<String> messages = ServerAssistantAI.getInstance().getConfig().getStringList(path);

        if (messages.isEmpty()) {
            String message = ServerAssistantAI.getInstance().getConfig().getString(path);

            if (message == null || message.isEmpty()) {
                return;
            }

            sender.sendMessage(color(replace(message, placeholders)));
            return;
        }

        for (String message : messages) {
            sender.sendMessage(color(replace(message, placeholders)));
        }
    }

    private static String replace(String message, String... placeholders) {

        String prefix = ServerAssistantAI.getInstance().getConfig().getString("prefix", "");
        String name = ServerAssistantAI.getInstance().getConfig().getString("name", "");

        message = message.replace("%prefix%", prefix);
        message = message.replace("%name%", name);

        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            message = message.replace(placeholders[i], placeholders[i + 1]);
        }

        return message;
    }

    private static String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String get(String path, String... placeholders) {

        String message = ServerAssistantAI.getInstance()
                .getConfig()
                .getString(path);

        if (message == null || message.isEmpty()) {
            return "";
        }

        return color(replace(message, placeholders));
    }

}