package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class SwitchSpell extends TargetedSpell implements TargetedEntitySpell {

	private int switchBack;
	
	public SwitchSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		switchBack = getConfigInt("switch-back", 0);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}
			
			// teleport
			playSpellEffects(player, target.getTarget());
			switchPlaces(player, target.getTarget());			
			
			// send messages
			sendMessages(player, target.getTarget());
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
		if (validTargetList.canTarget(caster, target)) {
			switchPlaces(caster, target);
			return true;
		}
		return false;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
