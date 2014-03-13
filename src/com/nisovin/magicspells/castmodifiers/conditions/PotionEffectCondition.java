package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.Util;

public class PotionEffectCondition extends Condition {

	PotionEffectType effectType;
	
	@Override
	public boolean setVar(String var) {
		effectType = Util.getPotionEffectType(var);
		return effectType != null;
	}

	@Override
	public boolean check(Player player) {
		return player.hasPotionEffect(effectType);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return target.hasPotionEffect(effectType);
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}	
	
}
