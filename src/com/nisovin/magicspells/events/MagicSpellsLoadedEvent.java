package com.nisovin.magicspells.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.MagicSpells;

@SuppressWarnings("serial")
public class MagicSpellsLoadedEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private MagicSpells plugin;
    
    public MagicSpellsLoadedEvent(MagicSpells plugin) {
    	this.plugin = plugin;
    }
    
    public MagicSpells getPlugin() {
    	return plugin;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
