package yankay.inventoryGoal;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.Sign;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.block.BlockBreakEvent;

public class ResourceCollectorListener implements Listener {

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
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof Chest chest)) return;

        Block chestBlock = chest.getBlock();

        Block[] adjacentBlocks = new Block[] {
                chestBlock.getRelative(1, 0, 0),
                chestBlock.getRelative(-1, 0, 0),
                chestBlock.getRelative(0, 0, 1),
                chestBlock.getRelative(0, 0, -1)
        };

        for (Block block : adjacentBlocks) {
            if (!(block.getState() instanceof Sign)) continue;

            Sign sign = (Sign) block.getState();

            if (!"[InventoryGoal]".equalsIgnoreCase(sign.getLine(0))) continue;

            String materialName = sign.getLine(1);
            String targetAmountLine = sign.getLine(2);

            String[] targetParts = targetAmountLine.split("/");
            if (targetParts.length != 2) continue;

            int requiredAmount;
            try {
                requiredAmount = Integer.parseInt(targetParts[1]);
            } catch (NumberFormatException e) {
                continue;
            }

            Material material = Material.matchMaterial(materialName.toUpperCase());
            if (material == null) continue;

            Inventory chestInventory = chest.getInventory();
            int collectedAmount = 0;

            for (ItemStack item : chestInventory) {
                if (item != null && item.getType() == material) {
                    collectedAmount += item.getAmount();
                }
            }

            sign.setLine(2, collectedAmount + "/" + requiredAmount);
            sign.update();

            Bukkit.getLogger().info("Updated sign: " + collectedAmount + "/" + requiredAmount + " of " + material.name());
            createArmorStandAboveChest(chestBlock, material);
            return;
        }
    }

    private void createArmorStandAboveChest(Block chestBlock, Material material) {
        Location aboveChestLocation = chestBlock.getLocation().add(0.5, 0.2, 0.5); // Сдвиг на 0.5 по X и 1 по Y
        ArmorStand armorStand = (ArmorStand) chestBlock.getWorld().spawnEntity(aboveChestLocation, EntityType.ARMOR_STAND);
        armorStand.setVisible(false);
        armorStand.setGravity(false);
        armorStand.setSmall(true);
        armorStand.setArms(false);
        armorStand.setBasePlate(false);
        armorStand.getEquipment().setHelmet(new ItemStack(material));
        armorStand.setCustomNameVisible(false);
    }


    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();

        if (block.getState() instanceof Chest) {
            Chest chest = (Chest) block.getState();
            ArmorStand armorStand = findArmorStandAboveChest(chest.getBlock());
            if (armorStand != null) {
                armorStand.remove();
            }
        } else if (block.getState() instanceof Sign) {
            Sign sign = (Sign) block.getState();
            if ("[InventoryGoal]".equalsIgnoreCase(sign.getLine(0))) {
                ArmorStand armorStand = findArmorStandAboveChest(block);
                if (armorStand != null) {
                    armorStand.remove();
                }
            }
        }
    }

    private ArmorStand findArmorStandAboveChest(Block chestBlock) {
        Location aboveChestLocation = chestBlock.getLocation().add(0.5, 0.2, 0.5);

        for (Entity entity : chestBlock.getWorld().getEntitiesByClass(ArmorStand.class)) {
            if (entity.getLocation().getBlock().equals(aboveChestLocation.getBlock())) {
                return (ArmorStand) entity;
            }
        }
        return null;
    }
}
