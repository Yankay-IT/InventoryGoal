package yankay.inventoryGoal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.inventory.ItemStack;

public class ResourceCollectorListener implements Listener {
    private final InventoryGoal plugin;

    public ResourceCollectorListener(InventoryGoal plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        String materialName = event.getLine(0).trim();
        String amountStr = event.getLine(1).trim();

        if (materialName.isEmpty() || amountStr.isEmpty()) {
            return;
        }

        Material material = Material.matchMaterial(materialName.toUpperCase());
        if (material == null) {
            event.getPlayer().sendMessage("[InventoryGoal] §cНеизвестный материал: " + materialName);
            return;
        }

        int requiredAmount;
        try {
            requiredAmount = Integer.parseInt(amountStr);
        } catch (NumberFormatException e) {
            event.getPlayer().sendMessage("[InventoryGoal] §cНекорректное количество ресурсов. Используйте формат: Material:Amount");
            return;
        }

        event.setLine(0, "[InventoryGoal]");
        event.setLine(1, materialName.toLowerCase());
        event.setLine(2, "0/" + requiredAmount);
        event.setLine(3, "");

        event.getPlayer().sendMessage("[InventoryGoal] §aТабличка создана для сбора: " + material.name() + " (" + requiredAmount + ") | by Yankay");

        spawnArmorStandAboveChest(event.getBlock(), material);
    }

    private void spawnArmorStandAboveChest(Block signBlock, Material material) {
        if (!plugin.getConfig().getBoolean("armor-stand.enable-spawn", true)) {
            return;
        }

        Chest chest = findChestNearby(signBlock);
        if (chest != null) {
            Location aboveChestLocation = chest.getLocation().add(0.5, 0.2, 0.5);
            ArmorStand armorStand = (ArmorStand) chest.getWorld().spawnEntity(aboveChestLocation, EntityType.ARMOR_STAND);
            armorStand.setVisible(false);
            armorStand.setGravity(false);
            armorStand.setSmall(true);
            armorStand.setArms(false);
            armorStand.setBasePlate(false);
            armorStand.getEquipment().setHelmet(new ItemStack(material));
            armorStand.setCustomName("InventoryGoalArmorStand:" + chest.getLocation().toString());
            armorStand.setPersistent(true);
            armorStand.setCustomNameVisible(false);
        }
    }

    private Chest findChestNearby(Block signBlock) {
        Block[] adjacentBlocks = new Block[]{
                signBlock.getRelative(1, 0, 0),
                signBlock.getRelative(-1, 0, 0),
                signBlock.getRelative(0, 0, 1),
                signBlock.getRelative(0, 0, -1)
        };

        for (Block block : adjacentBlocks) {
            if (block.getState() instanceof Chest) {
                return (Chest) block.getState();
            }
        }
        return null;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getState() instanceof Chest) {
            if (hasInventoryGoalSign(block)) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("[InventoryGoal] §cВы не можете ломать сундук, пока на нём установлена табличка!");
            }
        }

        if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if ("[InventoryGoal]".equalsIgnoreCase(sign.getLine(0))) {
                removeArmorStandAboveChest(block);
            }
        }
    }

    private boolean hasInventoryGoalSign(Block chestBlock) {
        Block[] adjacentBlocks = new Block[]{
                chestBlock.getRelative(1, 0, 0),
                chestBlock.getRelative(-1, 0, 0),
                chestBlock.getRelative(0, 0, 1),
                chestBlock.getRelative(0, 0, -1)
        };

        for (Block block : adjacentBlocks) {
            if (block.getState() instanceof Sign sign) {
                if ("[InventoryGoal]".equalsIgnoreCase(sign.getLine(0))) {
                    return true;
                }
            }
        }

        return false;
    }

    private void removeArmorStandAboveChest(Block signBlock) {
        Chest chest = findChestNearby(signBlock);
        if (chest != null) {
            Location aboveChestLocation = chest.getLocation().add(0.5, 0.2, 0.5);

            for (Entity entity : chest.getWorld().getEntitiesByClass(ArmorStand.class)) {
                if (entity.getCustomName() != null && entity.getCustomName().startsWith("InventoryGoalArmorStand:")) {
                    if (entity.getLocation().getBlock().equals(aboveChestLocation.getBlock())) {
                        entity.remove();
                        return;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        ArmorStand armorStand = event.getRightClicked();

        if (armorStand.getCustomName() != null && armorStand.getCustomName().startsWith("InventoryGoalArmorStand:")) {
            event.setCancelled(true);
            event.getPlayer().sendMessage("[InventoryGoal] §cВы не можете взаимодействовать с этим бронестендом!");
        }
    }
}
