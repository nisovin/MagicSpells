package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;

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
			List<Monster> monsters = new ArrayList<Monster>();
			for (Entity e : entities) {
				if (e instanceof Monster) {
					monsters.add((Monster)e);
				}
			}
			for (int i = 0; i < monsters.size(); i++) {
				int next = i+1;
				if (next >= monsters.size()) {
					next = 0;
				}
				monsters.get(i).setTarget(monsters.get(next));
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}
