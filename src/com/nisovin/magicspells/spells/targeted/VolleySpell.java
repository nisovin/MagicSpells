package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class VolleySpell extends TargetedLocationSpell {

	private int arrows;
	private int speed;
	private int spread;
	
	public VolleySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		arrows = getConfigInt("arrows", 10);
		speed = getConfigInt("speed", 20);
		spread = getConfigInt("spread", 150);
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			
			Block target;
			try {
				target = player.getTargetBlock(null, range>0?range:100);
			} catch (IllegalStateException e) {
				target = null;
			}
			if (target == null || target.getType() == Material.AIR) {
				sendMessage(player, strNoTarget);
				fizzle(player);
				return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;
			} else {
				volley(player, target.getLocation(), power);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void volley(Player player, Location target, float power) {
		Location spawn = player.getLocation();
		spawn.setY(spawn.getY()+3);
		
		Vector v = target.toVector().subtract(spawn.toVector()).normalize();
		int arrows = Math.round(this.arrows*power);
		for (int i = 0; i < arrows; i++) {
			Arrow a = player.getWorld().spawnArrow(spawn, v, (speed/10.0F), (spread/10.0F));
			a.setShooter(player);
		}
		
		playGraphicalEffects(player, target);
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		volley(caster, target, power);
		return true;
	}

}