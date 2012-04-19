package com.nisovin.magicspells.spells.instant;

import java.util.HashSet;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PurgeSpell extends InstantSpell {
	
	private int radius;
	private HashSet<EntityType> entityTypes;
	
	public PurgeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("range", 15);
		
		List<String> types = getConfigStringList("creature-types", null);
		if (types != null && types.size() > 0) {
			entityTypes = new HashSet<EntityType>();
			for (String type : types) {
				EntityType et = EntityType.fromName(type);
				if (et != null) {
					entityTypes.add(et);
				}
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.radius*power);
			List<Entity> entities = player.getNearbyEntities(range, range, range);
			boolean killed = false;
			for (Entity entity : entities) {
				if (entity instanceof LivingEntity && !(entity instanceof Player) && (entityTypes == null || entityTypes.contains(entity.getType()))) {
					((LivingEntity)entity).setHealth(0);
					killed = true;
					playGraphicalEffects(2, entity);
				}
			}
			if (killed) {
				playGraphicalEffects(1, player);
			} else {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}	
	
}