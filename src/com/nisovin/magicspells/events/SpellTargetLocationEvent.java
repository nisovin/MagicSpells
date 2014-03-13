package com.nisovin.magicspells.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.Spell;

public class SpellTargetLocationEvent extends SpellEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

	private Location target;
	private boolean cancelled = false;
	
	public SpellTargetLocationEvent(Spell spell, Player caster, Location target) {
		super(spell, caster);
		this.target = target;
	}
	
	/**
	 * Gets the location that is being targeted by the spell.
	 * @return the targeted living entity
	 */
	public Location getTargetLocation() {
		return target;
	}
	
	/**
	 * Sets the spell's target to the provided location.
	 * @param target the new target
	 */
	public void setTargetLocation(Location target) {
		this.target = target;
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
