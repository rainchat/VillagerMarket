package me.bestem0r.villagermarket;

import me.bestem0r.villagermarket.shops.VillagerShop;
import me.bestem0r.villagermarket.utilities.Methods;
import org.bukkit.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;

import java.util.UUID;


public class EntityInfo {

    private final VMPlugin plugin;
    private final FileConfiguration config;
    private final VillagerShop shop;
    private String name = "Villager Shop";
    private Profession profession;
    private int chunkX = 0;
    private int chunkZ = 0;
    private Location location = null;

    public EntityInfo(VMPlugin plugin, FileConfiguration config, VillagerShop shop) {
        this.plugin = plugin;
        this.config = config;

        this.shop = shop;

        if (config.getString("entity.name") != null) {
            this.name = config.getString("entity.name");
            double x = config.getDouble("entity.location.x");
            double y = config.getDouble("entity.location.y");
            double z = config.getDouble("entity.location.z");
            String professionString = config.getString("entity.profession");
            if (this.isProfession(professionString)) {
                this.profession = Villager.Profession.valueOf(professionString);
            } else {
                this.profession = Villager.Profession.NONE;
            }

            World world = Bukkit.getWorld(config.getString("entity.location.world"));
            if (world != null) {
                this.location = new Location(world, x, y, z);
                this.chunkX = this.location.getChunk().getX();
                this.chunkZ = this.location.getChunk().getZ();
            }
        }
    }

    public void setLocation(Location location) {
        this.location = location;
        this.chunkX = location.getChunk().getX();
        this.chunkZ = location.getChunk().getZ();
    }

    private boolean isProfession(String s) {
        if (s == null) {
            return false;
        } else {
            Villager.Profession[] var2 = Villager.Profession.values();
            int var3 = var2.length;

            for (int var4 = 0; var4 < var3; ++var4) {
                Villager.Profession profession = var2[var4];
                if (s.equals(profession.name())) {
                    return true;
                }
            }

            return false;
        }
    }


    public void save() {
        if (this.plugin.isEnabled()) {
            Bukkit.getScheduler().runTask(this.plugin, this::saveSync);
        } else {
            this.saveSync();
        }

    }

    private void saveSync() {
        Entity entity = Bukkit.getEntity(this.shop.getEntityUUID());
        if (entity != null) {
            if (this.plugin.isEnabled()) {
                Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                    this.saveInfo(entity);
                });
            } else {
                this.saveInfo(entity);
            }
        }

    }

    private void saveInfo(Entity entity) {
        if (entity instanceof Villager) {
            this.config.set("entity.profession", ((Villager) entity).getProfession().name());
        }

        this.config.set("entity.name", entity.getName());
        this.config.set("entity.location.x", entity.getLocation().getX());
        this.config.set("entity.location.y", entity.getLocation().getY());
        this.config.set("entity.location.z", entity.getLocation().getZ());
        this.config.set("entity.location.world", entity.getLocation().getWorld().getName());
    }


    public boolean hasStoredData() {
        return this.location != null;
    }

    public boolean isInChunk(Chunk chunk) {
        return chunk.getX() == this.chunkX && chunk.getZ() == this.chunkZ;
    }

    public UUID getEntityUUID() {
        return this.shop.getEntityUUID();
    }

    public boolean exists() {
        return Bukkit.getEntity(this.shop.getEntityUUID()) != null;
    }

    public void appendToExisting() {
        Entity entity = Bukkit.getEntity(this.shop.getEntityUUID());
        if (entity instanceof Villager) {
            Villager villager = (Villager) entity;
            villager.setCustomName(this.name);
            villager.setProfession(this.profession);
        }

    }

    public void reCreate() {
        UUID uuid = Methods.spawnShop(this.plugin, this.location, "none");
        this.shop.changeUUID(uuid);
        Villager villager = (Villager) Bukkit.getEntity(uuid);
        if (villager != null) {
            this.appendToExisting();
        } else {
            Bukkit.getLogger().severe(ChatColor.RED + "Unable to (re)spawn Villager! Does WorldGuard deny mobs pawn?");
        }

    }
}
