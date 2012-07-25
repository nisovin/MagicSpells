package com.nisovin.magicspells.spells;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;

public abstract class TargetedSpell extends InstantSpell {

	protected int range;
	protected boolean alwaysActivate;
	protected boolean playFizzleSound;
	protected String strCastTarget;
	protected String strNoTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = getConfigInt("range", 20);
		alwaysActivate = getConfigBoolean("always-activate", false);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);
		strCastTarget = getConfigString("str-cast-target", "");
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	protected void sendMessageToTarget(Player caster, Player target) {
		sendMessage(target, strCastTarget, "%a", caster.getDisplayName());
	}
	
	protected void sendMessages(Player caster, LivingEntity target) {
		EntityType entityType = target.getType();
		String entityName;
		Player playerTarget = null;
		if (target instanceof Player) {
			playerTarget = (Player)target;
			entityName = playerTarget.getDisplayName();
		} else {
			entityName = MagicSpells.getEntityNames().get(entityType);
			if (entityName == null) {
				entityName = entityType.getName();
			}
		}
		sendMessage(caster, strCastSelf, "%a", caster.getDisplayName(), "%t", entityName);
		if (playerTarget != null) {
			sendMessage(playerTarget, strCastTarget, "%a", caster.getDisplayName(), "%t", entityName);
		}
		sendMessageNear(caster, playerTarget, formatMessage(strCastOthers, "%a", caster.getDisplayName(), "%t", entityName), broadcastRange);
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
	 * This should be called if a target should not be found. It sends the no target message
	 * and returns the appropriate return value.
	 * @param player the casting player
	 * @return the appropriate PostcastAction value
	 */
	protected PostCastAction noTarget(Player player) {
		return noTarget(player, strNoTarget);
	}
	
	/**
	 * This should be called if a target should not be found. It sends the provided message
	 * and returns the appropriate return value.
	 * @param player the casting player
	 * @param message the message to send
	 * @return
	 */
	protected PostCastAction noTarget(Player player, String message) {
		fizzle(player);
		sendMessage(player, message);
		return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;		
	}
	
}
