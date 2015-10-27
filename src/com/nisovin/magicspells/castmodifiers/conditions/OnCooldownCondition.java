package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.castmodifiers.Condition;

public class OnCooldownCondition extends Condition {

	Spell spell;
	
	@Override
	public boolean setVar(String var) {
		spell = MagicSpells.getSpellByInternalName(var);
		return spell != null;
	}

	@Override
	public boolean check(Player player) {
		return spell.onCooldown(player);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return spell.onCooldown((Player)target);
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
