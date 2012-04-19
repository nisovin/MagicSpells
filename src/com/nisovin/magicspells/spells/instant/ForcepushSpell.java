package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ForcepushSpell extends InstantSpell {
	
	private int radius;
	private boolean targetPlayers;
	private int force;
	private int yForce;
	private int maxYForce;
	
	public ForcepushSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		radius = getConfigInt("range", 3);
		targetPlayers = getConfigBoolean("target-players", false);
		force = getConfigInt("pushback-force", 30);
		yForce = getConfigInt("additional-vertical-force", 15);
		maxYForce = getConfigInt("max-vertical-force", 20);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			knockback(player, radius, power, targetPlayers);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	public void knockback(Player player, int range, float power, boolean targetPlayers) {
	    Vector p = player.getLocation().toVector();
		List<Entity> entities = player.getNearbyEntities(range, range, range);
		Vector e, v;
		for (Entity entity : entities) {
			if (entity instanceof LivingEntity && (targetPlayers || !(entity instanceof Player))) {
				e = entity.getLocation().toVector();
				v = e.subtract(p).normalize().multiply(force/10.0*power);
				if (force != 0) {
					v.setY(v.getY() * (yForce/10.0*power));
				} else {
					v.setY(yForce/10.0*power);
				}
				if (v.getY() > (maxYForce/10.0)) {
					v.setY(maxYForce/10.0);
				}
				entity.setVelocity(v);
				playGraphicalEffects(2, entity);
			}
	    }
		playGraphicalEffects(1, player);
	}

}
