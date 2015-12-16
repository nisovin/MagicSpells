package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class CombustSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private int fireTicks;
	private int fireTickDamage;
	private boolean preventImmunity;
	private boolean checkPlugins;
	
	private HashMap<Integer, CombustData> combusting = new HashMap<Integer, CombustData>();
	
	public CombustSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		fireTicks = getConfigInt("fire-ticks", 100);
		fireTickDamage = getConfigInt("fire-tick-damage", 1);
		preventImmunity = getConfigBoolean("prevent-immunity", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}
	
	public int getDuration() {
		return fireTicks;
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			} else {
				boolean combusted = combust(player, target.getTarget(), target.getPower());
				if (!combusted) {
					return noTarget(player);
				}
				sendMessages(player, target.getTarget());
				return PostCastAction.NO_MESSAGES;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean combust(Player player, final LivingEntity target, float power) {
		if (target instanceof Player && checkPlugins && player != null) {
			// call other plugins
			EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, (double)1);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			}
		}
		int duration = Math.round(fireTicks*power);
		combusting.put(target.getEntityId(), new CombustData(power));
		Bukkit.getPluginManager().callEvent(new SpellApplyDamageEvent(this, player, target, fireTickDamage, DamageCause.FIRE_TICK, ""));
		target.setFireTicks(duration);
		if (player != null) {
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				CombustData data = combusting.get(target.getEntityId());
				if (data != null) {
					combusting.remove(target.getEntityId());
				}
			}
		}, duration+2);
		return true;
	}
	
	@EventHandler
	public void onEntityDamage(final EntityDamageEvent event) {
		if (event.isCancelled() || event.getCause() != DamageCause.FIRE_TICK) return;
		
		CombustData data = combusting.get(event.getEntity().getEntityId());
		if (data != null) {
			event.setDamage(Math.round(fireTickDamage * data.power));
			if (preventImmunity) {
				Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						((LivingEntity)event.getEntity()).setNoDamageTicks(0);
					}
				}, 0);
			}
		}
	}
	
	private class CombustData {
		float power;
		
		CombustData(float power) {
			this.power = power;
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			return combust(caster, target, power);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			return combust(null, target, power);
		}
	}
}