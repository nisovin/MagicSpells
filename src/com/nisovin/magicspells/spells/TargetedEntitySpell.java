package com.nisovin.magicspells.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.util.MagicConfig;

public abstract class TargetedEntitySpell extends TargetedSpell {

	private boolean obeyLos;
	private boolean targetPlayers;
	
	public TargetedEntitySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
	}
	
	public abstract boolean castAtEntity(Player caster, LivingEntity target, float power);

	protected LivingEntity getTarget(Player player) {
		return getTargetedEntity(player, minRange, range, targetPlayers, true, obeyLos, true);
	}
	
	protected LivingEntity getTarget(Player player, boolean targetNonPlayers) {
		return getTargetedEntity(player, minRange, range, targetPlayers, targetNonPlayers, obeyLos, true);
	}
	
	protected Player getTargetPlayer(Player player) {
		return getTargetedPlayer(player, minRange, range, obeyLos);
	}
	
}
