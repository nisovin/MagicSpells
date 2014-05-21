package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;

public class OrbitSpell extends TargetedSpell implements TargetedEntitySpell {

	float orbitRadius;
	float secondsPerRevolution;
	boolean counterClockwise;
	
	String particleName;
	float particleSpeed;
	int particleCount;
	float particleHorizontalSpread;
	float particleVerticalSpread;

	int tickInterval;
	float ticksPerSecond;
	float distancePerTick;
	
	int maxDuration;
	int renderDistance;
	float yOffset;
	
	boolean targetPlayers;
	boolean targetNonPlayers;
	
	public OrbitSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		orbitRadius = getConfigFloat("orbit-radius", 1);
		secondsPerRevolution = getConfigFloat("seconds-per-revolution", 3);
		counterClockwise = getConfigBoolean("counter-clockwise", false);
		
		particleName = getConfigString("particle-name", "reddust");
		particleSpeed = getConfigFloat("particle-speed", 0.3F);
		particleCount = getConfigInt("particle-count", 15);
		particleHorizontalSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		particleVerticalSpread = getConfigFloat("particle-vertical-spread", 0.3F);

		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float)tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
		
		maxDuration = getConfigInt("max-duration", 20) * 1000;
		yOffset = getConfigFloat("y-offset", 0.6F);
		renderDistance = getConfigInt("render-distance", 32);
		
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", false);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(player, power);
			if (target == null) {
				return noTarget(player);
			}			
			new ParticleTracker(player, target.getTarget(), target.getPower());
			playSpellEffects(player, target.getTarget());
			sendMessages(player, target.getTarget());
			return PostCastAction.NO_MESSAGES;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		new ParticleTracker(caster, target, power);
		playSpellEffects(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		return false;
	}
	
	class ParticleTracker implements Runnable {
		
		Player caster;
		LivingEntity target;
		float power;
		long startTime;
		Vector currentPosition;
		int taskId;
		
		int counter = 0;
		
		public ParticleTracker(Player caster, LivingEntity target, float power) {
			this.caster = caster;
			this.target = target;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.currentPosition = target.getLocation().getDirection().setY(0);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
		}
		
		@Override
		public void run() {
			// check for valid and alive caster and target
			if (!caster.isValid() || !target.isValid()) {
				stop();
				return;
			}
			
			// check if duration is up
			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop();
				return;
			}
			
			// move projectile and calculate new vector
			Location loc = getLocation();
			
			// show particle
			MagicSpells.getVolatileCodeHandler().playParticleEffect(loc, particleName, particleHorizontalSpread, particleVerticalSpread, particleSpeed, particleCount, renderDistance, 0F);
			
		}
		
		private Location getLocation() {
			Vector perp;
			if (counterClockwise) {
				perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			} else {
				perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			}
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return target.getLocation().add(0, yOffset, 0).add(currentPosition.clone().multiply(orbitRadius));
		}
		
		public void stop() {
			if (target.isValid()) {
				playSpellEffects(EffectPosition.DELAYED, getLocation());
			}
			MagicSpells.cancelTask(taskId);
			caster = null;
			target = null;
			currentPosition = null;
		}
		
	}

}
