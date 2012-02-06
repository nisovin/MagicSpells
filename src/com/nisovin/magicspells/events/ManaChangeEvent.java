package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@SuppressWarnings("serial")
public class ManaChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
	private Player player;
	private int newAmt;
	private int maxMana;
	
	public ManaChangeEvent(Player player, int newAmt, int maxMana) {
		super("MAGIC_SPELLS_MANA_CHANGE");
		
		this.player = player;
		this.newAmt = newAmt;
		this.maxMana = maxMana;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public int getNewAmount() {
		return newAmt;
	}
	
	public int getMaxMana() {
		return maxMana;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
