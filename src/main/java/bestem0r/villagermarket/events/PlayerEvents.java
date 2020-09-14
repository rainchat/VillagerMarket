package bestem0r.villagermarket.events;

import bestem0r.villagermarket.DataManager;
import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.shops.AdminShop;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import bestem0r.villagermarket.utilities.ColorBuilder;
import bestem0r.villagermarket.utilities.Config;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.Objects;

public class PlayerEvents implements Listener {

    DataManager dataManager;

    public PlayerEvents(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String playerUUID = player.getUniqueId().toString();
        if (Config.getPendingConfig().get(playerUUID) != null) {
            int pendingAmount = (int) Config.getPendingConfig().get(playerUUID);
            if (pendingAmount == 0) {
                return;
            }
            Economy economy = VMPlugin.getEconomy();
            economy.depositPlayer(player, pendingAmount);
            player.sendMessage(VMPlugin.getPrefix() + new Color.Builder()
                    .path("messages.received")
                    .replace("%amount%", String.valueOf(pendingAmount))
                    .build());
            Config.getPendingConfig().set(playerUUID, 0);
            Config.savePending();
        }
    }
    @EventHandler
    public void playerRightClick(PlayerInteractEntityEvent event) {

        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        String entityUUID = entity.getUniqueId().toString();

        if (dataManager.getRemoveVillager().contains(player)) {
            if (dataManager.getVillagers().containsKey(entityUUID)) {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.villager_removed"));
                player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.remove_villager")), 0.5f, 1);
                File file = new File(Objects.requireNonNull(Bukkit.getServer().getPluginManager().getPlugin("VillagerMarket")).getDataFolder() + "/Shops/", entityUUID + ".yml");
                if (file.exists()) {
                    file.delete();
                    dataManager.getVillagers().remove(entityUUID);
                }
                entity.remove();

            } else {
                player.sendMessage(VMPlugin.getPrefix() + ColorBuilder.color("messages.no_villager_shop"));
            }
            dataManager.getRemoveVillager().remove(player);
        }

        if(dataManager.getVillagers().containsKey(entityUUID)) {
            event.setCancelled(true);
            if(event.getHand() == EquipmentSlot.OFF_HAND) { return; }
            VillagerShop villagerShop = dataManager.getVillagers().get(entityUUID);
            Inventory inventory;

            if (villagerShop instanceof AdminShop) {
                if (player.hasPermission("villagermarket.admin")) {
                    inventory = villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP);
                } else {
                    inventory = villagerShop.getInventory(VillagerShop.ShopMenu.SHOPFRONT);
                }
            } else {
                if (villagerShop.getOwnerUUID().equals("null")) {
                    inventory = villagerShop.getInventory(VillagerShop.ShopMenu.BUY_SHOP);
                } else if (villagerShop.getOwnerUUID().equals(player.getUniqueId().toString())) {
                    inventory = villagerShop.getInventory(VillagerShop.ShopMenu.EDIT_SHOP);
                } else {
                    inventory = villagerShop.getInventory(VillagerShop.ShopMenu.SHOPFRONT);
                }
            }

            dataManager.getClickMap().put(player.getUniqueId().toString(), entityUUID);

            player.openInventory(inventory);
            player.playSound(player.getLocation(), Sound.valueOf(VMPlugin.getInstance().getConfig().getString("sounds.open_shop")), 0.5f, 1);

        }
    }

    @EventHandler
    public void onCloseInventory(InventoryCloseEvent event) {
        Player player = (Player) event.getPlayer();
        if (!dataManager.getClickMap().containsKey(player.getUniqueId().toString())) { return; }
        String entityUUID = dataManager.getClickMap().get(player.getUniqueId().toString());
        ItemStack[] items = event.getView().getTopInventory().getContents();

        String title = ChatColor.stripColor(event.getView().getTitle());
        if (title.equalsIgnoreCase(ChatColor.stripColor(ColorBuilder.color("menus.edit_storage.title")))) {
            dataManager.getVillagers().get(entityUUID).updateShopInventories();
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        for (Entity entity : dataManager.getVillagerEntities()) {
            if (entity.getNearbyEntities(5, 5, 5).contains(player)) {
                entity.teleport(entity.getLocation().setDirection(player.getLocation().subtract(entity.getLocation()).toVector()));
            }
        }
    }

    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (dataManager.getAmountHashMap().containsKey(player.getUniqueId().toString())) {
            event.setCancelled(true);
        }
    }
}
