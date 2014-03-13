package com.nisovin.magicspells.spells.passive;

import org.bukkit.event.Listener;

import com.nisovin.magicspells.spells.PassiveSpell;

public abstract class PassiveListener implements Listener {

	public abstract void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var);
	
	public void initialize() {
		
	}
	
	public void turnOff() {
		
	}
	
}
