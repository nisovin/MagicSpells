package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.CastItem;

public class SpellSelectionChangedEvent extends SpellEvent {

    private static final HandlerList handlers = new HandlerList();
    
    private CastItem castItem;
    private Spellbook spellbook;
    
	public SpellSelectionChangedEvent(Spell spell, Player caster, CastItem castItem, Spellbook spellbook) {
		super(spell, caster);
		this.castItem = castItem;
	}
	
	public CastItem getCastItem() {
		return castItem;
	}
	
	public Spellbook getSpellbook() {
		return spellbook;
	}
	
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

}
