package me.bestem0r.villagermarket.events.dynamic;

import me.bestem0r.villagermarket.VMPlugin;
import me.bestem0r.villagermarket.shops.PlayerShop;
import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import me.bestem0r.villagermarket.utilities.Methods;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.UUID;

public class ChangeName implements Listener {

    private final Player player;
    private final Entity villager;
    private final VillagerShop villagerShop;
    private final VMPlugin plugin;

    public ChangeName(VMPlugin plugin, Player player, String entityUUID) {
        this.plugin = plugin;
        this.player = player;
        this.villager = Bukkit.getEntity(UUID.fromString(entityUUID));
        this.villagerShop = Methods.shopFromUUID(UUID.fromString(entityUUID));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() == this.player) {
            String cancel = plugin.getConfig().getString("cancel");
            if (event.getMessage().equalsIgnoreCase(cancel)) {
                player.sendMessage(new ColorBuilder(plugin).path("messages.cancelled").addPrefix().build());
            } else {
                String name = ChatColor.translateAlternateColorCodes('&', event.getMessage());
                String customName = villagerShop instanceof PlayerShop ? new ColorBuilder(plugin)
                        .path("villager.custom_name")
                        .replace("%player%", player.getName())
                        .replace("%custom_name%", name)
                        .build() : name;

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (plugin.isCitizensEnabled() && CitizensAPI.getNPCRegistry().isNPC(villager)) {
                        CitizensAPI.getNPCRegistry().getNPC(villager).setName(customName);
                    } else {
                        villager.setCustomName(customName);
                    }
                });

                player.sendMessage(new ColorBuilder(plugin)
                        .path("messages.change_name_set")
                        .replace("%name%", name)
                        .addPrefix()
                        .build());
            }

            event.setCancelled(true);
            HandlerList.unregisterAll(this);
        }
    }
}
