package com.nisovin.magicspells.spells.passive;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;

public class PassiveManager {

	Map<PassiveTrigger, PassiveListener> listeners = new HashMap<PassiveTrigger, PassiveListener>();
	boolean initialized = false;
	
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		PassiveListener listener = listeners.get(trigger);
		if (listener == null) {
			listener = trigger.getNewListener();
			MagicSpells.registerEvents(listener);
			listeners.put(trigger, listener);
		}
		listener.registerSpell(spell, trigger, var);
	}
	
	public void initialize() {
		if (!initialized) {
			initialized = true;
			for (PassiveListener listener : listeners.values()) {
				listener.initialize();
			}
		}
	}
	
	public void turnOff() {
		for (PassiveListener listener : listeners.values()) {
			HandlerList.unregisterAll(listener);
			listener.turnOff();
		}
		listeners.clear();
	}
	
}
