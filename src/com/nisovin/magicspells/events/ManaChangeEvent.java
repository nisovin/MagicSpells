package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is fired whenever a player's mana value is changed.
 *
 */
public class ManaChangeEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
	private Player player;
	private int newAmt;
	private int maxMana;
	
	public ManaChangeEvent(Player player, int newAmt, int maxMana) {		
		this.player = player;
		this.newAmt = newAmt;
		this.maxMana = maxMana;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	/**
	 * The amount of mana the player now has.
	 * @return mana amount
	 */
	public int getNewAmount() {
		return newAmt;
	}
	
	/**
	 * The maximum amount of mana the player can have.
	 * @return max mana
	 */
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
