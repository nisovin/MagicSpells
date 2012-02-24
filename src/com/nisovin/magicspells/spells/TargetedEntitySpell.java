package com.nisovin.magicspells.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.MagicConfig;

public abstract class TargetedEntitySpell extends TargetedSpell {

	public TargetedEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);
	}
	
	public abstract boolean castAtEntity(Player caster, LivingEntity target, float power);

}
