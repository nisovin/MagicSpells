package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.MagicConfig;

public class SummonSpell extends TargetedSpell implements TargetedEntitySpell, TargetedEntityFromLocationSpell {

	private boolean requireExactName;
	private boolean requireAcceptance;
	private int maxAcceptDelay;
	private String acceptCommand;
	private String strUsage;
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
			// get target name and landing location
			String targetName = "";
			Location landLoc = null;
			if (args != null && args.length > 0) {
				targetName = args[0];
				landLoc = player.getLocation().add(0, .25, 0);
			} else {
				Block block = getTargetedBlock(player, 10);
				if (block != null && (block.getType() == Material.WALL_SIGN || block.getType() == Material.SIGN_POST)) {
					Sign sign = (Sign)block.getState();
					targetName = sign.getLine(0);
					landLoc = block.getLocation().add(.5, .25, .5);
				}
			}
			
			// check usage
			if (targetName.equals("")) {
				// fail -- show usage
				sendMessage(player, strUsage);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// check location
			if (landLoc == null || !BlockUtils.isSafeToStand(landLoc.clone())) {
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
				return noTarget(player);
			}
			
			// teleport player
			if (requireAcceptance) {
				pendingSummons.put(target, landLoc);
				pendingTimes.put(target, System.currentTimeMillis());
				sendMessage(target, strSummonPending, "%a", player.getDisplayName());
			} else {
				target.teleport(landLoc);
				sendMessage(target, strSummonAccepted, "%a", player.getDisplayName());
			}
			
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
			
		}
		return PostCastAction.HANDLE_NORMALLY;
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
	
	@Override
	public List<String> tabComplete(CommandSender sender, String partial) {
		if (partial.contains(" ")) {
			return null;
		}
		return tabCompletePlayerName(sender, partial);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		return target.teleport(caster);
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return target.teleport(from);
	}

}
