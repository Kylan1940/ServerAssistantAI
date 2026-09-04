package org.kylan1940.serverassistantai;

import org.bukkit.plugin.java.JavaPlugin;
import org.kylan1940.serverassistantai.command.AssistanceCommand;
import org.kylan1940.serverassistantai.utils.ConfigUpdater;

public final class ServerAssistantAI extends JavaPlugin {

    private static ServerAssistantAI instance;

    @Override
    public void onEnable() {

        instance = this;

        saveDefaultConfig();
        ConfigUpdater.update(this);
        saveResource("server-information.txt", false);

        getCommand("assistance").setExecutor(new AssistanceCommand(this));

        getLogger().info("ServerAssistantAI enabled.");

    }

    @Override
    public void onDisable() {
        getLogger().info("ServerAssistantAI disabled.");
    }

    public static ServerAssistantAI getInstance() {
        return instance;
    }

}
