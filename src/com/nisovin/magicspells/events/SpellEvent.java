package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.nisovin.magicspells.Spell;

@SuppressWarnings("serial")
public class SpellEvent extends Event {

	private Spell spell;
	private Player caster;
	
	public SpellEvent(String type, Spell spell, Player caster) {
		super(type);
		this.spell = spell;
		this.caster = caster;
	}
	
	/**
	 * Gets the spell involved in the event.
	 * @return the spell
	 */
	public Spell getSpell() {
		return spell;
	}
	
	/**
	 * Gets the player casting the spell.
	 * @return the casting player
	 */
	public Player getCaster() {
		return caster;
	}
	
}
