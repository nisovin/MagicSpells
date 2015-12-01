package com.nisovin.magicspells.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.Spell;

public class SpellApplyDamageEvent extends SpellEvent {

    private static final HandlerList handlers = new HandlerList();

    LivingEntity target;
    double damage;
    DamageCause cause;
    long timestamp;
    
    public SpellApplyDamageEvent(Spell spell, Player caster, LivingEntity target, double damage, DamageCause cause) {
		super(spell, caster);
    	this.target = target;
		this.damage = damage;
		this.cause = cause;
		this.timestamp = System.currentTimeMillis();
	}
    
    public LivingEntity getTarget() {
    	return target;
    }
    
    public double getDamage() {
    	return damage;
    }
    
    public DamageCause getCause() {
    	return cause;
    }
    
    public long getTimestamp() {
    	return timestamp;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}
