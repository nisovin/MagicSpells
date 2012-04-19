package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

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
				if (entity instanceof Monster) {
					((Monster) entity).setTarget(player);
					count++;
					playGraphicalEffects(2, entity);
				}
			}
			
			if (cancelIfNoTargets && count == 0) {
				// nothing affected
				sendMessage(player, strNoTarget);
				//fizzle(player);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				playGraphicalEffects(1, player);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
