package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LightningSpell extends TargetedLocationSpell {
	
	private boolean requireEntityTarget;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private int additionalDamage;
	private boolean noDamage;
	private String strCastFail;
	private String strNoTarget;
	
	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireEntityTarget = config.getBoolean("spells." + spellName + ".require-entity-target", false);
		obeyLos = config.getBoolean("spells." + spellName + ".obey-los", true);
		targetPlayers = config.getBoolean("spells." + spellName + ".target-players", false);
		checkPlugins = config.getBoolean("spells." + spellName + ".check-plugins", true);
		additionalDamage = getConfigInt("additional-damage", 0);
		noDamage = config.getBoolean("spells." + spellName + ".no-damage", false);		
		strCastFail = config.getString("spells." + spellName + ".str-cast-fail", "");
		strNoTarget = config.getString("spells." + spellName + ".str-no-target", "Unable to find target.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = null;
			if (requireEntityTarget) {
				LivingEntity e = getTargetedEntity(player, range>0?range:100, targetPlayers, obeyLos);
				if (e != null && e instanceof Player && checkPlugins) {
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, e, DamageCause.ENTITY_ATTACK, 0);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						e = null;
					}					
				}
				if (e != null) {
					target = e.getLocation().getBlock();
					if (additionalDamage > 0) {
						e.damage(Math.round(additionalDamage*power), player);
					}
				} else {
					sendMessage(player, strNoTarget);
					fizzle(player);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else {
				target = player.getTargetBlock(null, range>0?range:500);
				if (target.getWorld().getHighestBlockYAt(target.getLocation()) != target.getY()+1) {
					target = null;
				}
			}
			if (target != null) {
				lightning(target.getLocation());
			} else {
				sendMessage(player, strCastFail);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void lightning(Location target) {
		if (noDamage) {
			target.getWorld().strikeLightningEffect(target);
		} else {				
			target.getWorld().strikeLightning(target);
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		lightning(target);
		return true;
	}
}
