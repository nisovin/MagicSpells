package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.Spell;

public class SpellForgetEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    
	private Spell spell;
	private Player forgetter;
	private boolean cancelled;

	public SpellForgetEvent(Spell spell, Player forgetter) {
		this.spell = spell;
		this.forgetter = forgetter;
	}
	
	public Player getForgetter() {
		return forgetter;
	}
	
	public Spell getSpell() {
		return spell;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
