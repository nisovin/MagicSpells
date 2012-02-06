package com.nisovin.magicspells.spells.targeted;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class CrippleSpell extends TargetedSpell {

	private int strength;
	private int duration;
	private boolean targetPlayers;
	private boolean obeyLos;
	private String strNoTarget;
	
	public CrippleSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strength = getConfigInt("effect-strength", 5);
		duration = getConfigInt("effect-duration", 100);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {		
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail
				sendMessage(player, strNoTarget);
				fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			setMobEffect(target, 2, duration, strength);
		}
		
		return PostCastAction.HANDLE_NORMALLY;
	}

}
