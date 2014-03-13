package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PurgeSpell extends InstantSpell {
	
	private int radius;
	
	public PurgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("range", 15);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.radius*power);
			List<Entity> entities = player.getNearbyEntities(range, range, range);
			boolean killed = false;
			for (Entity entity : entities) {
				if (entity instanceof LivingEntity && !(entity instanceof Player) && validTargetList.canTarget(player, (LivingEntity)entity)) {
					((LivingEntity)entity).setHealth(0);
					killed = true;
					playSpellEffects(EffectPosition.TARGET, entity);
				}
			}
			if (killed) {
				playSpellEffects(EffectPosition.CASTER, player);
			} else {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}	
	
}