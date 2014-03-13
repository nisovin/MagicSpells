package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class RoarSpell extends InstantSpell {

	private int radius;
	private boolean cancelIfNoTargets;
	private String strNoTarget;
	
	public RoarSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("range", 8);
		cancelIfNoTargets = getConfigBoolean("cancel-if-no-targets", true);
		strNoTarget = getConfigString("str-no-target", "No targets found.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int count = 0;
			List<Entity> entities = player.getNearbyEntities(radius, radius, radius);
			for (Entity entity : entities) {
				if (entity instanceof LivingEntity && !(entity instanceof Player) && validTargetList.canTarget(player, (LivingEntity)entity)) {
					MagicSpells.getVolatileCodeHandler().setTarget((LivingEntity)entity, player);
					count++;
					playSpellEffects(EffectPosition.TARGET, entity);
				}
			}
			
			if (cancelIfNoTargets && count == 0) {
				// nothing affected
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
