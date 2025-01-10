package yankay.inventoryGoal;

import org.bukkit.plugin.java.JavaPlugin;

public final class InventoryGoal extends JavaPlugin {

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("[InventoryGoal] Enabled by Yankay");
        getServer().getPluginManager().registerEvents(new ResourceCollectorListener(this), this);
    }

    @Override
    public void onDisable() {
        getLogger().info("[InventoryGoal] disabled by Yankay");
    }
}
