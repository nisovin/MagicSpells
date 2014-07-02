package com.nisovin.magicspells.spells;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public abstract class TargetedSpell extends InstantSpell {

	protected boolean alwaysActivate;
	protected boolean playFizzleSound;
	protected boolean targetSelf;
	protected String spellNameOnFail;
	protected Subspell spellOnFail;
	protected String strCastTarget;
	protected String strNoTarget;

	public TargetedSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		alwaysActivate = getConfigBoolean("always-activate", false);
		playFizzleSound = getConfigBoolean("play-fizzle-sound", false);
		targetSelf = getConfigBoolean("target-self", false);
		spellNameOnFail = getConfigString("spell-on-fail", null);
		strCastTarget = getConfigString("str-cast-target", "");
		strNoTarget = getConfigString("str-no-target", "");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (spellNameOnFail != null && !spellNameOnFail.isEmpty()) {
			spellOnFail = new Subspell(spellNameOnFail);
			if (!spellOnFail.process()) {
				spellOnFail = null;
				MagicSpells.error("Invalid spell-on-fail for spell " + internalName);
			}
		}
	}
	
	protected void sendMessages(Player caster, LivingEntity target) {
		String targetName = getTargetName(target);
		Player playerTarget = null;
		if (target instanceof Player) {
			playerTarget = (Player)target;
		}
		sendMessage(caster, prepareMessage(strCastSelf, caster, targetName, playerTarget));
		if (playerTarget != null) {
			sendMessage(playerTarget, prepareMessage(strCastTarget, caster, targetName, playerTarget));
		}
		sendMessageNear(caster, playerTarget, prepareMessage(strCastOthers, caster, targetName, playerTarget), broadcastRange);
	}
	
	private String prepareMessage(String message, Player caster, String targetName, Player playerTarget) {
		if (message != null && !message.isEmpty()) {
			message = message.replace("%a", caster.getDisplayName());
			message = message.replace("%t", targetName);
			if (playerTarget != null && MagicSpells.getVariableManager() != null && message.contains("%targetvar")) {
				Matcher matcher = chatVarTargetMatchPattern.matcher(message);
				while (matcher.find()) {
					String varText = matcher.group();
					String[] varData = varText.substring(5, varText.length() - 1).split(":");
					double val = MagicSpells.getVariableManager().getValue(varData[0], playerTarget);
					String sval = varData.length == 1 ? Util.getStringNumber(val, -1) : Util.getStringNumber(val, Integer.parseInt(varData[1]));
					message = message.replace(varText, sval);
				}
			}
			if (caster != null && MagicSpells.getVariableManager() != null && message.contains("%castervar")) {
				Matcher matcher = chatVarCasterMatchPattern.matcher(message);
				while (matcher.find()) {
					String varText = matcher.group();
					String[] varData = varText.substring(5, varText.length() - 1).split(":");
					double val = MagicSpells.getVariableManager().getValue(varData[0], caster);
					String sval = varData.length == 1 ? Util.getStringNumber(val, -1) : Util.getStringNumber(val, Integer.parseInt(varData[1]));
					message = message.replace(varText, sval);
				}
			}
		}
		return message;
	}
	static private Pattern chatVarCasterMatchPattern = Pattern.compile("%castervar:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	static private Pattern chatVarTargetMatchPattern = Pattern.compile("%targetvar:[A-Za-z0-9_]+(:[0-9]+)?%", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
	
	protected String getTargetName(LivingEntity target) {
		if (target instanceof Player) {
			return ((Player)target).getDisplayName();
		} else {
			String name = MagicSpells.getEntityNames().get(target.getType());
			if (name != null) {
				return name;
			} else {
				return "unknown";
			}
		}
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
			player.playEffect(player.getLocation(), Effect.EXTINGUISH, null);
		}
	}
	
	@Override
	protected TargetInfo<LivingEntity> getTargetedEntity(Player player, float power, boolean forceTargetPlayers, ValidTargetChecker checker) {
		if (targetSelf) {
			return new TargetInfo<LivingEntity>(player, power);
		} else {
			return super.getTargetedEntity(player, power, forceTargetPlayers, checker);
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
		if (spellOnFail != null) {
			spellOnFail.cast(player, 1.0F);
		}
		return alwaysActivate ? PostCastAction.NO_MESSAGES : PostCastAction.ALREADY_HANDLED;		
	}
	
}
