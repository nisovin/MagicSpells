package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.spells.instant.MarkSpell;

public class HasMarkCondition extends Condition {

	MarkSpell spell;
	
	@Override
	public boolean setVar(String var) {
		Spell s = MagicSpells.getSpellByInternalName(var);
		if (s != null && s instanceof MarkSpell) {
			spell = (MarkSpell)s;
			return true;
		}
		return false;
	}

	@Override
	public boolean check(Player player) {
		return spell.getMarks().containsKey(player.getName().toLowerCase());
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		}
		return false;
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
