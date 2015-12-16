package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellApplyDamageEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.SpellDamageSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell, SpellDamageSpell {

	private double damage;
	private String spellDamageType;
	private boolean ignoreArmor;
	private boolean checkPlugins;
	private DamageCause damageType;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigFloat("damage", 4);
		spellDamageType = getConfigString("spell-damage-type", "");
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		
		String type = getConfigString("damage-type", "ENTITY_ATTACK");
		for (DamageCause cause : DamageCause.values()) {
			if (cause.name().equalsIgnoreCase(type)) {
				damageType = cause;
				break;
			}
		}
		if (damageType == null) {
			damageType = DamageCause.ENTITY_ATTACK;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				// fail -- no target
				return noTarget(player);
			} else {
				boolean done = causePain(player, target.getTarget(), target.getPower());
				if (!done) {
					return noTarget(player);
				} else {
					sendMessages(player, target.getTarget());
					return PostCastAction.NO_MESSAGES;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public String getSpellDamageType() {
		return spellDamageType;
	}
	
	private boolean causePain(Player player, LivingEntity target, float power) {
		if (target.isDead()) return false;
		double dam = damage * power;
		if (target instanceof Player && checkPlugins && player != null) {
			// handle the event myself so I can detect cancellation properly
			EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, damageType, dam);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			}
			dam = event.getDamage();
			target.setLastDamageCause(event);
		}
		SpellApplyDamageEvent event = new SpellApplyDamageEvent(this, player, target, dam, damageType, spellDamageType);
		Bukkit.getPluginManager().callEvent(event);
		dam = event.getFinalDamage();
		if (ignoreArmor) {
			double health = target.getHealth();
			if (health > target.getMaxHealth()) health = target.getMaxHealth();
			health = health - dam;
			if (health < 0) health = 0;
			if (health > target.getMaxHealth()) health = target.getMaxHealth();
			if (health == 0 && player != null) {
				MagicSpells.getVolatileCodeHandler().setKiller(target, player);
			}
			target.setHealth(health);
			target.playEffect(EntityEffect.HURT);
		} else {
			target.damage(dam, player);
		}
		if (player != null) {
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			return causePain(caster, target, power);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			return causePain(null, target, power);
		}
	}

}
