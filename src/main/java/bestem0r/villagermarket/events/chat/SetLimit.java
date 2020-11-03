package bestem0r.villagermarket.events.chat;

import bestem0r.villagermarket.VMPlugin;
import bestem0r.villagermarket.items.ShopItem;
import bestem0r.villagermarket.shops.ShopMenu;
import bestem0r.villagermarket.shops.VillagerShop;
import bestem0r.villagermarket.utilities.Color;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class SetLimit implements Listener {

    private final Player player;
    private final VillagerShop villagerShop;
    private final ShopItem shopItem;

    public SetLimit(Player player, VillagerShop villagerShop, ShopItem shopItem) {
        this.player = player;
        this.villagerShop = villagerShop;
        this.shopItem = shopItem;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        if (event.getPlayer() != player) return;

        String message = event.getMessage();
        String cancel = VMPlugin.getInstance().getConfig().getString("cancel");

        event.setCancelled(true);
        if (message.equalsIgnoreCase(cancel)) {
            player.sendMessage(new Color.Builder().path("messages.cancelled").addPrefix().build());
            HandlerList.unregisterAll(this);
            return;
        }
        if (!canConvert(message)) {
            player.sendMessage(new Color.Builder().path("messages.not_number").addPrefix().build());
            return;
        }
        shopItem.setLimit(Integer.parseInt(message));
        villagerShop.updateShopInventories();
        Bukkit.getScheduler().runTask(VMPlugin.getInstance(), () -> villagerShop.openInventory(player, ShopMenu.EDIT_SHOPFRONT));
        HandlerList.unregisterAll(this);
    }

    private Boolean canConvert(String string) {
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }
}
