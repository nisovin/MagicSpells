package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.MagicConfig;

public abstract class TargetedSpell extends InstantSpell {

	protected int range;
	protected boolean playFizzleSound;
	protected String strCastTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = config.getInt("spells." + spellName + ".range", 20);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);
		strCastTarget = getConfigString("str-cast-target", "");
	}

	protected void sendMessageToTarget(Player caster, Player target) {
		sendMessage(target, strCastTarget, "%a", caster.getDisplayName());
	}
	
	/**
	 * Checks whether two locations are within a certain distance from each other.
	 * @param loc1 The first location
	 * @param loc2 The second location
	 * @param range The maximum distance
	 * @return true if the distance is less than the range, false otherwise
	 */
	protected boolean inRange(Location loc1, Location loc2, int range) {
		return loc1.distanceSquared(loc2) < range*range;
	}
	
	/**
	 * Plays the fizzle sound if it is enabled for this spell.
	 */
	protected void fizzle(Player player) {
		if (playFizzleSound) {
			player.playEffect(player.getLocation(), Effect.EXTINGUISH, 0);
		}
	}
	
	/**
	 * Gets the living entity a player is currently looking at
	 * @param player player to get target for
	 * @param range the maximum range to check
	 * @param targetPlayers whether to allow players as targets
	 * @param checkLos whether to obey line-of-sight restrictions
	 * @return the targeted LivingEntity, or null if none was found
	 */
	protected LivingEntity getTargetedEntity(Player player, int range, boolean targetPlayers, boolean checkLos) {
		return getTargetedEntity(player, range, targetPlayers, true, checkLos);
	}
	
	/**
	 * Gets the player a player is currently looking at, ignoring other living entities
	 * @param player the player to get the target for
	 * @param range the maximum range to check
	 * @param checkLos whether to obey line-of-sight restrictions
	 * @return the targeted Player, or null if none was found
	 */
	protected Player getTargetedPlayer(Player player, int range, boolean checkLos) {
		LivingEntity entity = getTargetedEntity(player, range, true, false, checkLos);
		if (entity instanceof Player) {
			return (Player)entity;
		} else {
			return null;
		}
	}
	
	protected LivingEntity getTargetedEntity(Player player, int range, boolean targetPlayers, boolean targetNonPlayers, boolean checkLos) {
		// get nearby living entities, filtered by player targeting options
		List<Entity> ne = player.getNearbyEntities(range, range, range);
		ArrayList<LivingEntity> entities = new ArrayList<LivingEntity>(); 
		for (Entity e : ne) {
			if (e instanceof LivingEntity) {
				if ((targetPlayers || !(e instanceof Player)) && (targetNonPlayers || e instanceof Player)) {
					entities.add((LivingEntity)e);
				}
			}
		}
		
		// find target
		LivingEntity target = null;
		BlockIterator bi;
		try {
			bi = new BlockIterator(player, range);
		} catch (IllegalStateException e) {
			return null;
		}
		Block b;
		Location l;
		int bx, by, bz;
		double ex, ey, ez;
		// loop through player's line of sight
		while (bi.hasNext()) {
			b = bi.next();
			bx = b.getX();
			by = b.getY();
			bz = b.getZ();			
			if (checkLos && !MagicSpells.losTransparentBlocks.contains(b.getTypeId())) {
				// line of sight is broken, stop without target
				break;
			} else {
				// check for entities near this block in the line of sight
				for (LivingEntity e : entities) {
					l = e.getLocation();
					ex = l.getX();
					ey = l.getY();
					ez = l.getZ();
					if ((bx-.75 <= ex && ex <= bx+1.75) && (bz-.75 <= ez && ez <= bz+1.75) && (by-1 <= ey && ey <= by+2.5)) {
						// entity is close enough, set target and stop
						target = e;
						
						// check for anti-magic-zone
						if (target != null && MagicSpells.noMagicZones != null && MagicSpells.noMagicZones.willFizzle(target.getLocation(), this)) {
							target = null;
							continue;
						}
						
						// call event listeners
						if (target != null) {
							SpellTargetEvent event = new SpellTargetEvent(this, player, target);
							Bukkit.getServer().getPluginManager().callEvent(event);
							if (event.isCancelled()) {
								target = null;
								continue;
							} else {
								target = event.getTarget();
							}
						}
						
						return target;
					}
				}
			}
		}
		
		return null;
	}
}
