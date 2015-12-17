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
    String spellDamageType;
    long timestamp;
    float modifier;
    
    public SpellApplyDamageEvent(Spell spell, Player caster, LivingEntity target, double damage, DamageCause cause, String spellDamageType) {
		super(spell, caster);
    	this.target = target;
		this.damage = damage;
		this.cause = cause;
		this.spellDamageType = spellDamageType;
		this.timestamp = System.currentTimeMillis();
		this.modifier = 1.0f;
	}
    
    public void applyDamageModifier(float modifier) {
    	this.modifier *= modifier;
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
    
    public float getDamageModifier() {
    	return modifier;
    }
    
    public double getFinalDamage() {
    	return damage * modifier;
    }
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
}
