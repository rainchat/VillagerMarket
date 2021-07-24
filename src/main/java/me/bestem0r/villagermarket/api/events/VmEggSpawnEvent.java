package me.bestem0r.villagermarket.api.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class VmEggSpawnEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();


    private final Location location;
    private final Player player;
    private boolean cancelled;

    public VmEggSpawnEvent(Player player, Location location) {
        this.location = location;
        this.player = player;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Location getLocation() {
        return location;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }

}