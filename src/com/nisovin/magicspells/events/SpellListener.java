package com.nisovin.magicspells.events;

import org.bukkit.event.CustomEventListener;
import org.bukkit.event.Event;

/**
 * Handles events related to spells
 * 
 */
@Deprecated
public class SpellListener extends CustomEventListener {

	@Override
	public void onCustomEvent(Event event) {
		if (event instanceof SpellTargetEvent) {
			onSpellTarget((SpellTargetEvent)event);
		} else if (event instanceof SpellCastEvent) {
			onSpellCast((SpellCastEvent)event);
		} else if (event instanceof SpellLearnEvent) {
			onSpellLearn((SpellLearnEvent)event);
		} else if (event instanceof ManaChangeEvent) {
			onManaChange((ManaChangeEvent)event);
		}
	}
	
	/**
	 * Called when an instant spell targets a LivingEntity.
	 * @param event relevant event details
	 */
	@Deprecated
	public void onSpellTarget(SpellTargetEvent event) {
		
	}
	
	/**
	 * Called when a spell is cast.
	 * @param event relevant event details
	 */
	@Deprecated
	public void onSpellCast(SpellCastEvent event) {
		
	}
	
	/**
	 * Called when a spell is learned.
	 * @param event relevant event details
	 */
	@Deprecated
	public void onSpellLearn(SpellLearnEvent event) {
		
	}
	
	/**
	 * Called when a player's mana level changes for any reason
	 * @param event relevant event details
	 */
	@Deprecated
	public void onManaChange(ManaChangeEvent event) {
		
	}
	
}
