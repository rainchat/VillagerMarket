package me.bestem0r.villagermarket.items;


import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.shops.VillagerShop.VillagerType;
import me.bestem0r.villagermarket.utilities.ColorBuilder;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class ShopItem extends ItemStack {

    private final JavaPlugin plugin;
    private VillagerType villagerType;
    boolean isEditor;
    private BigDecimal price;
    private int slot;
    private int limit;
    private List<String> menuLore;
    private final HashMap<UUID, Integer> playerLimit;
    private ShopItem.Mode mode;
    private String menuName;
    private String command;

    private ShopItem(JavaPlugin plugin, ItemStack itemStack) {
        super(itemStack);
        this.isEditor = false;
        this.limit = 0;
        this.playerLimit = new HashMap();
        this.plugin = plugin;
    }

    public BigDecimal getPrice() {
        return this.price;
    }

    public int getSlot() {
        return this.slot;
    }

    public ShopItem.Mode getMode() {
        return this.mode;
    }

    public void toggleEditor(boolean editor) {
        this.isEditor = editor;
    }

    public int getLimit() {
        return this.limit;
    }

    public HashMap<UUID, Integer> getPlayerLimit() {
        return this.playerLimit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public void setCommand(String command) {
        this.command = command;
        this.mode = ShopItem.Mode.COMMAND;
        NamespacedKey key = new NamespacedKey(this.plugin, "villagermarket-command");
        ItemMeta itemMeta = this.getItemMeta();
        itemMeta.getPersistentDataContainer().set(key, PersistentDataType.STRING, command);
        this.setItemMeta(itemMeta);
    }

    public void toggleMode() {
        switch(this.mode) {
            case BUY:
                this.mode = ShopItem.Mode.SELL;
                break;
            case SELL:
                this.mode = ShopItem.Mode.BUY;
        }

    }

    public int getPlayerLimit(Player player) {
        return (Integer)this.playerLimit.getOrDefault(player.getUniqueId(), 0);
    }

    public void increasePlayerLimit(Player player) {
        if (this.playerLimit.containsKey(player.getUniqueId())) {
            this.playerLimit.replace(player.getUniqueId(), (Integer)this.playerLimit.get(player.getUniqueId()) + 1);
        } else {
            this.playerLimit.put(player.getUniqueId(), 1);
        }

    }

    public void runCommand(Player player) {
        ConsoleCommandSender sender = Bukkit.getConsoleSender();
        Bukkit.dispatchCommand(sender, this.command.replaceAll("%player%", player.getName()));
    }

    public void addPlayerLimit(UUID uuid, int amount) {
        this.playerLimit.put(uuid, amount);
    }

    public void refreshLore(VillagerShop villagerShop) {
        FileConfiguration config = this.plugin.getConfig();
        int storageAmount = villagerShop.getAmountInStorage(this.asItemStack(ShopItem.LoreType.ITEM));
        int available = villagerShop.getAvailable(this);
        ShopItem.Mode itemMode = this.mode;
        if (!this.isEditor && this.mode != ShopItem.Mode.COMMAND) {
            itemMode = this.mode == ShopItem.Mode.BUY ? ShopItem.Mode.SELL : ShopItem.Mode.BUY;
        }

        String inventoryPath = this.isEditor ? ".edit_shopfront." : ".shopfront.";
        String typePath = this.villagerType == VillagerType.ADMIN ? "admin." : "player.";
        String modePath = itemMode.toString().toLowerCase();
        String lorePath = "menus" + inventoryPath + typePath + modePath + "_lore";
        this.menuLore = (new ColorBuilder(this.plugin)).path(lorePath).replace("%amount%", String.valueOf(super.getAmount())).replaceWithCurrency("%price%", this.price.stripTrailingZeros().toPlainString()).replace("%stock%", String.valueOf(storageAmount)).replace("%available%", String.valueOf(available)).replace("%limit%", this.limit == 0 ? config.getString("quantity.unlimited") : String.valueOf(this.limit)).buildLore();
        String namePath = "menus" + inventoryPath + "item_name";
        ItemMeta meta = super.getItemMeta();
        if (meta == null) {
            Bukkit.getLogger().severe("[VillagerMarket]: Invalid material type: " + super.getType());
        } else {
            String name = meta.hasDisplayName() ? meta.getDisplayName() : (meta.hasLocalizedName() ? meta.getLocalizedName() : WordUtils.capitalizeFully(this.getType().name().replaceAll("_", " ")));
            String mode = (new ColorBuilder(this.plugin)).path("menus" + inventoryPath + "modes." + itemMode.toString().toLowerCase()).build();
            this.menuName = (new ColorBuilder(this.plugin)).path(namePath).replace("%item_name%", name).replace("%mode%", mode).build();
        }
    }

    public ItemStack asItemStack(ShopItem.LoreType loreType) {
        ItemStack itemStack = new ItemStack(this);
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta == null) {
            Bukkit.getLogger().severe("[VillagerMarket] Invalid material type: " + super.getType());
            return itemStack;
        } else if (this.mode != ShopItem.Mode.COMMAND && loreType == ShopItem.LoreType.MENU) {
            itemMeta.setLore(this.menuLore);
            itemMeta.setDisplayName(this.menuName);
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        } else {
            if (this.mode == ShopItem.Mode.COMMAND && loreType == ShopItem.LoreType.MENU) {
                List<String> currentLore = itemMeta.getLore();
                if (currentLore != null) {
                    currentLore.addAll(this.menuLore);
                    itemMeta.setLore(currentLore);
                } else {
                    itemMeta.setLore(this.menuLore);
                }
            }

            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
    }

    public static enum Mode {
        BUY,
        SELL,
        COMMAND;

        private Mode() {
        }
    }

    public static enum LoreType {
        ITEM,
        MENU;

        private LoreType() {
        }
    }

    public static class Builder {
        private final ItemStack itemStack;
        private final JavaPlugin plugin;
        private VillagerType villagerType;
        private String entityUUID;
        private BigDecimal price;
        private int slot;
        private int amount = 1;
        private int buyLimit = 0;
        private ShopItem.Mode mode;

        public Builder(JavaPlugin plugin, ItemStack itemStack) {
            this.mode = ShopItem.Mode.SELL;
            this.plugin = plugin;
            this.itemStack = itemStack.clone();
        }

        public ShopItem.Builder villagerType(VillagerType villagerType) {
            this.villagerType = villagerType;
            return this;
        }

        public ShopItem.Builder entityUUID(String entityUUID) {
            this.entityUUID = entityUUID;
            return this;
        }

        public ShopItem.Builder price(BigDecimal price) {
            this.price = price;
            return this;
        }

        public ShopItem.Builder slot(int slot) {
            this.slot = slot;
            return this;
        }

        public ShopItem.Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public ShopItem.Builder mode(ShopItem.Mode mode) {
            this.mode = mode;
            return this;
        }

        public ShopItem.Builder buyLimit(int amount) {
            this.buyLimit = amount;
            return this;
        }

        public ShopItem build() {
            ShopItem shopItem = new ShopItem(this.plugin, this.itemStack);
            shopItem.villagerType = this.villagerType;
            shopItem.price = this.price;
            shopItem.slot = this.slot;
            shopItem.setAmount(this.amount);
            shopItem.limit = this.buyLimit;
            shopItem.mode = this.mode;
            return shopItem;
        }

        public String getEntityUUID() {
            return this.entityUUID;
        }
    }
}
