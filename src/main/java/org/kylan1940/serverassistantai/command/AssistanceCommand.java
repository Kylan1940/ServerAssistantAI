package org.kylan1940.serverassistantai.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.kylan1940.serverassistantai.ai.AIService;
import org.kylan1940.serverassistantai.ai.CooldownManager;
import org.kylan1940.serverassistantai.message.MessageUtil;

import java.util.concurrent.CompletableFuture;

public class AssistanceCommand implements CommandExecutor {

    private final JavaPlugin plugin;
    private final AIService aiService;
    private final CooldownManager cooldownManager;

    public AssistanceCommand(JavaPlugin plugin) {
        this.plugin = plugin;
        this.aiService = new AIService(plugin);
        this.cooldownManager = new CooldownManager();
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {

            if (!sender.hasPermission("serverassistant.reload")) {
                MessageUtil.send(sender, "message.reload-no-permission");
                return true;
            }

            plugin.reloadConfig();

            MessageUtil.send(sender, "message.reload-success");
            return true;
        }

        if (!(sender instanceof Player player)) {
            MessageUtil.send(sender, "message.player-only");
            return true;
        }

        if (args.length == 0) {
            MessageUtil.send(player, "message.usage");
            return true;
        }

        String prompt = String.join(" ", args).trim();

        int maxLength = plugin.getConfig()
                .getInt("assistant.max-prompt-length", 300);

        if (prompt.length() > maxLength) {
            MessageUtil.send(
                    player,
                    "message.prompt-too-long",
                    "%max%",
                    String.valueOf(maxLength)
            );
            return true;
        }

        String apiKey = plugin.getConfig()
                .getString("ai.api-key", "");

        if (apiKey.isBlank() || apiKey.equals("YOUR_GEMINI_API_KEY")) {
            MessageUtil.send(player, "message.no-api-key");
            return true;
        }

        long cooldown = plugin.getConfig()
                .getLong("ai.cooldown", 3);

        long remaining = cooldownManager.getRemaining(
                player.getUniqueId(),
                cooldown
        );

        if (remaining > 0) {
            MessageUtil.send(
                    player,
                    "message.cooldown",
                    "%seconds%",
                    String.valueOf(remaining)
            );
            return true;
        }

        cooldownManager.start(player.getUniqueId());

        player.sendActionBar(
                MessageUtil.get("message.thinking")
        );

        CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return aiService.ask(prompt);
                    } catch (Exception exception) {
                        plugin.getLogger().warning(
                                "Failed to get AI response: "
                                        + exception.getMessage()
                        );

                        return null;
                    }
                })
                .thenAccept(response -> {

                    plugin.getServer().getScheduler().runTask(
                            plugin,
                            () -> {

                                if (response == null || response.isBlank()) {
                                    MessageUtil.send(
                                            player,
                                            "message.error"
                                    );
                                    return;
                                }

                                MessageUtil.send(
                                        player,
                                        "message.response",
                                        "%response%",
                                        response
                                );
                            }
                    );
                });

        return true;
    }

}