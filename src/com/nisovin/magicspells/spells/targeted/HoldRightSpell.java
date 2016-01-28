package com.nisovin.magicspells.spells.targeted;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class HoldRightSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	Subspell spell;
	boolean targetEntity;
	boolean targetLocation;
	int resetTime;
	float maxDuration;
	float maxDistance;
	
	// TODO: fix leak
	Map<String, CastData> casting = new HashMap<String, CastData>();
	
	public HoldRightSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		spell = new Subspell(getConfigString("spell", ""));
		targetEntity = getConfigBoolean("target-entity", true);
		targetLocation = getConfigBoolean("target-location", false);
		resetTime = getConfigInt("reset-time", 250);
		maxDuration = getConfigFloat("max-duration", 0);
		maxDistance = getConfigFloat("max-distance", 0);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (!spell.process()) {
			MagicSpells.error("Invalid spell on " + internalName);
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			CastData data = casting.get(player.getName());
			if (data != null && data.isValid(player)) {
				data.cast(player);
				return PostCastAction.ALREADY_HANDLED;
			}
			data = null;
			if (targetEntity) {
				TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
				if (target != null) {
					data = new CastData(target.getTarget(), target.getPower());
				} else {
					return noTarget(player);
				}
			} else if (targetLocation) {
				Block block = getTargetedBlock(player, power);
				if (block != null && block.getType() != Material.AIR) {
					data = new CastData(block.getLocation().add(0.5, 0.5, 0.5), power);
				} else {
					return noTarget(player);
				}				
			} else {
				data = new CastData(power);
			}
			if (data != null) {
				data.cast(player);
				casting.put(player.getName(), data);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (!targetLocation) return false;
		CastData data = casting.get(caster.getName());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getName(), data);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!targetEntity) return false;
		CastData data = casting.get(caster.getName());
		if (data != null && data.isValid(caster)) {
			data.cast(caster);
			return true;
		}
		data = new CastData(target, power);
		data.cast(caster);
		casting.put(caster.getName(), data);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	class CastData {
		long start = System.currentTimeMillis();
		long lastCast = 0;
		LivingEntity targetEntity = null;
		Location targetLocation = null;
		float power = 1f;
		
		public CastData(LivingEntity target, float power) {
			targetEntity = target;
			this.power = power;
		}
		
		public CastData(Location target, float power) {
			targetLocation = target;
			this.power = power;
		}
		
		public CastData(float power) {
			this.power = power;
		}
		
		boolean isValid(Player player) {
			if (lastCast < System.currentTimeMillis() - resetTime) return false;
			if (maxDuration > 0 && System.currentTimeMillis() - start > maxDuration * 1000) return false;
			if (maxDistance > 0) {
				Location l = targetLocation;
				if (targetEntity != null) l = targetEntity.getLocation();
				if (l == null) return false;
				if (!l.getWorld().equals(player.getWorld())) return false;
				if (l.distanceSquared(player.getLocation()) > maxDistance * maxDistance) return false;
			}
			return true;
		}
		
		void cast(Player caster) {
			lastCast = System.currentTimeMillis();
			if (targetEntity != null) {
				spell.castAtEntity(caster, targetEntity, power);
			} else if (targetLocation != null) {
				spell.castAtLocation(caster, targetLocation, power);
			} else {
				spell.cast(caster, power);
			}
		}
	}

}
