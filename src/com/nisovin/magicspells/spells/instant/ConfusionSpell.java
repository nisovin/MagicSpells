package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ConfusionSpell extends InstantSpell {

	private int range;
	
	public ConfusionSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 10);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			int range = Math.round(this.range*power);
			List<Entity> entities = player.getNearbyEntities(range, range, range);
			List<LivingEntity> monsters = new ArrayList<LivingEntity>();
			for (Entity e : entities) {
				if (e instanceof LivingEntity && validTargetList.canTarget(player, (LivingEntity)e)) {
					monsters.add((LivingEntity)e);
				}
			}
			for (int i = 0; i < monsters.size(); i++) {
				int next = i+1;
				if (next >= monsters.size()) {
					next = 0;
				}
				MagicSpells.getVolatileCodeHandler().setTarget(monsters.get(i), monsters.get(next));
				playSpellEffects(EffectPosition.TARGET, monsters.get(i));
			}
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
