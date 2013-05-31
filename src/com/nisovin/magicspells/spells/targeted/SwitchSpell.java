package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SwitchSpell extends TargetedEntitySpell {

	private int switchBack;
	private boolean targetPlayers;
	private boolean obeyLos;
	
	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		switchBack = getConfigInt("switch-back", 0);
		targetPlayers = getConfigBoolean("target-players", false);
		obeyLos = getConfigBoolean("obey-los", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			LivingEntity target = getTargetedEntity(player, minRange, range, targetPlayers, obeyLos);
			if (target == null) {
				return noTarget(player);
			}
			
			// teleport
			playSpellEffects(player, target);
			switchPlaces(player, target);			
			
			// send messages
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void switchPlaces(final Player player, final LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location casterLoc = player.getLocation();
		player.teleport(targetLoc);
		target.teleport(casterLoc);
		
		if (switchBack > 0) {
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					if (!player.isDead() && !target.isDead()) {
						Location targetLoc = target.getLocation();
						Location casterLoc = player.getLocation();
						player.teleport(targetLoc);
						target.teleport(casterLoc);
					}
				}
			}, switchBack);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (targetPlayers || !(target instanceof Player)) {
			switchPlaces(caster, target);
			return true;
		}
		return false;
	}

}
