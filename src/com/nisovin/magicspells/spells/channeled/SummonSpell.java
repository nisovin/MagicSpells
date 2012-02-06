package com.nisovin.magicspells.spells.channeled;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.spells.ChanneledSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SummonSpell extends ChanneledSpell {

	private boolean requireExactName;
	private boolean requireAcceptance;
	private int maxAcceptDelay;
	private String acceptCommand;
	private String strUsage;
	private String strNoTarget;
	private String strSummonPending;
	private String strSummonAccepted;
	private String strSummonExpired;
	
	private HashMap<Player,Location> pendingSummons;
	private HashMap<Player,Long> pendingTimes;
	
	public SummonSpell(MagicConfig config, String spellName) {
		super(config, spellName);		
		
		requireExactName = getConfigBoolean("require-exact-name", false);
		requireAcceptance = getConfigBoolean("require-acceptance", true);
		maxAcceptDelay = getConfigInt("max-accept-delay", 90);
		acceptCommand = getConfigString("accept-command", "accept");
		strUsage = getConfigString("str-usage", "Usage: /cast summon <playername>, or /cast summon \nwhile looking at a sign with a player name on the first line.");
		strNoTarget = getConfigString("str-no-target", "Target player not found.");
		strSummonPending = getConfigString("str-summon-pending", "You are being summoned! Type /accept to teleport.");
		strSummonAccepted = getConfigString("str-summon-accepted", "You have been summoned.");
		strSummonExpired = getConfigString("str-summon-expired", "The summon has expired.");

		if (requireAcceptance) {
			pendingSummons = new HashMap<Player,Location>();
			pendingTimes = new HashMap<Player,Long>();
		}
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target name
			String targetName = "";
			if (args != null && args.length > 0) {
				targetName = args[0];
			} else {
				Block block = player.getTargetBlock(null, 10);
				if (block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
					Sign sign = (Sign)block.getState();
					targetName = sign.getLine(0);
				}
			}
			
			// check usage
			if (targetName.equals("")) {
				// fail -- show usage
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get player
			Player target = null;
			if (requireExactName) {
				target = Bukkit.getServer().getPlayer(targetName);
				if (target != null && !target.getName().equalsIgnoreCase(targetName)) {
					target = null;
				}
			} else {
				List<Player> players = Bukkit.getServer().matchPlayer(targetName);
				if (players != null && players.size() == 1) {
					target = players.get(0);
				}
			}
			if (target == null) {
				// fail -- no player target
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// start channel
			boolean success = addChanneler(target.getName(), player);
			if (!success) {
				// failed to channel -- don't charge stuff or cooldown
				return PostCastAction.ALREADY_HANDLED;
			}
			
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	protected void finishSpell(String key, Location location) {
		Player target = Bukkit.getServer().getPlayer(key);
		if (target != null) {
			if (requireAcceptance) {
				pendingSummons.put(target, location);
				pendingTimes.put(target, System.currentTimeMillis());
				sendMessage(target, strSummonPending);
			} else {
				target.teleport(location);
				sendMessage(target, strSummonAccepted);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (requireAcceptance && event.getMessage().equalsIgnoreCase("/" + acceptCommand) && pendingSummons.containsKey(event.getPlayer())) {
			Player player = event.getPlayer();
			if (maxAcceptDelay > 0 && pendingTimes.get(player) + maxAcceptDelay*1000 < System.currentTimeMillis()) {
				// waited too long
				sendMessage(player, strSummonExpired);
			} else {
				// all ok, teleport
				player.teleport(pendingSummons.get(player));
				sendMessage(player, strSummonAccepted);
			}
			pendingSummons.remove(player);
			pendingTimes.remove(player);
			event.setCancelled(true);
		}
	}

}
