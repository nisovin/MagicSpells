package com.nisovin.magicspells.spells.instant;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkull;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntityFromLocationSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class WitherSkullSpell extends InstantSpell implements TargetedEntityFromLocationSpell {

	boolean charged;
	double velocity;
	
	public WitherSkullSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		charged = getConfigBoolean("charged", false);
		velocity = getConfigFloat("velocity", 2);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			WitherSkull skull = player.launchProjectile(WitherSkull.class, player.getLocation().getDirection().multiply(velocity * power));
			skull.setCharged(charged);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntityFromLocation(Player caster, Location from, LivingEntity target, float power) {
		Vector v = target.getLocation().toVector().subtract(from.toVector()).normalize();
		v.multiply(velocity * power);
		WitherSkull skull = from.getWorld().spawn(from.clone().setDirection(v), WitherSkull.class);
		skull.setCharged(charged);
		skull.setVelocity(v);
		skull.setDirection(v);
		
		if (caster != null) {
			playSpellEffects(EffectPosition.CASTER, caster);
		} else {
			playSpellEffects(EffectPosition.CASTER, from);
		}
		
		return true;
	}

	@Override
	public boolean castAtEntityFromLocation(Location from, LivingEntity target, float power) {
		return castAtEntityFromLocation(null, from, target, power);
	}

	
	
}
