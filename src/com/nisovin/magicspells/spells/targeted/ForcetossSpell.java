package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class ForcetossSpell extends TargetedSpell implements TargetedEntitySpell {

	private int damage;
	private float hForce;
	private float vForce;
	private boolean checkPlugins;
	private boolean powerAffectsForce;
	
	public ForcetossSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigInt("damage", 0);
		hForce = getConfigInt("horizontal-force", 20) / 10.0F;
		vForce = getConfigInt("vertical-force", 10) / 10.0F;
		checkPlugins = getConfigBoolean("check-plugins", true);
		powerAffectsForce = getConfigBoolean("power-affects-force", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target
			TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
			if (targetInfo == null) {
				return noTarget(player);
			}
			LivingEntity target = targetInfo.getTarget();
			power = targetInfo.getPower();
			
			// do damage
			if (damage > 0) {
				double damage = this.damage * power;
				if (target instanceof Player && checkPlugins) {
					EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, damage);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						return noTarget(player);
					}
					damage = event.getDamage();
				}
				target.damage(damage);
			}
			
			// throw target
			toss(player, target, power);
			
			sendMessages(player, target);
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void toss(Player player, LivingEntity target, float power) {
		if (!powerAffectsForce) power = 1f;
		Vector v = target.getLocation().toVector().subtract(player.getLocation().toVector())
				.setY(0)
				.normalize()
				.multiply(hForce*power)
				.setY(vForce*power);
		target.setVelocity(v);
		playSpellEffects(player, target);
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			toss(caster, target, power);
			return true;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}

}
