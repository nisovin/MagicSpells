package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.events.SpellTargetLocationEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class LightningSpell extends TargetedSpell implements TargetedLocationSpell {
	
	private boolean requireEntityTarget;
	private boolean checkPlugins;
	private double additionalDamage;
	private boolean noDamage;
	
	public LightningSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		additionalDamage = getConfigFloat("additional-damage", 0);
		noDamage = getConfigBoolean("no-damage", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = null;
			LivingEntity entityTarget = null;
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo != null) {
					entityTarget = targetInfo.getTarget();
					power = targetInfo.getPower();
				}
				if (entityTarget != null && entityTarget instanceof Player && checkPlugins) {
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entityTarget, DamageCause.ENTITY_ATTACK, 1 + additionalDamage);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						entityTarget = null;
					}					
				}
				if (entityTarget != null) {
					target = entityTarget.getLocation().getBlock();
					if (additionalDamage > 0) {
						entityTarget.damage(additionalDamage * power, player);
					}
				} else {
					return noTarget(player);
				}
			} else {
				try {
					target = getTargetedBlock(player, power);
				} catch (IllegalStateException e) {	
					target = null;
				}
				if (target != null) {
					SpellTargetLocationEvent event = new SpellTargetLocationEvent(this, player, target.getLocation(), power);
					Bukkit.getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						target = null;
					} else {
						target = event.getTargetLocation().getBlock();
						power = event.getPower();
					}
				}
			}
			if (target != null) {
				lightning(target.getLocation());
				playSpellEffects(player, target.getLocation());
				if (entityTarget != null) {
					sendMessages(player, entityTarget);
					return PostCastAction.NO_MESSAGES;
				}
			} else {
				return noTarget(player);
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
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		lightning(target);
		playSpellEffects(EffectPosition.CASTER, target);
		return true;
	}
}
