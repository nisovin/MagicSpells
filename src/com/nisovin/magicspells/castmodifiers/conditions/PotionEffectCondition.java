package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.Util;

public class PotionEffectCondition extends Condition {

	PotionEffectType effectType;
	int value;
	
	@Override
	public boolean setVar(String var) {
		if (var.contains(":")) {
			String[] s = var.split(":");
			effectType = Util.getPotionEffectType(s[0]);
			try {
				value = Integer.parseInt(s[1]);
			} catch (Exception e) {
				return false;
			}
		} else {
			effectType = Util.getPotionEffectType(var);
			value = -1;
		}
		return effectType != null;
	}

	@Override
	public boolean check(Player player) {
		return check(null, player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		boolean has = target.hasPotionEffect(effectType);
		if (value == -1) {
			return has;
		} else if (has) {
			for (PotionEffect effect : target.getActivePotionEffects()) {
				if (effect.getType() == effectType) {
					return effect.getAmplifier() == value;
				}
			}
		}
		return false;
	}
	
	@Override
	public boolean check(Player player, Location location) {
		return false;
	}	
	
}
