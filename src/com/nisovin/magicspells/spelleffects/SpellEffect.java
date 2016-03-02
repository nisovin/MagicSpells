package com.nisovin.magicspells.spelleffects;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.ModifierSet;

/**
 * 
 * Represents a graphical effect that can be used with the 'effects' option of a spell.
 *
 */
public abstract class SpellEffect {
	
	// for normal/line
	double heightOffset = 0;
	double forwardOffset = 0;
	int delay = 0;
	
	// for line
	double distanceBetween = 1;
	
	// for buff
	int effectInterval = 20;

	// for orbit
	float orbitRadius = 1;
	float secondsPerRevolution = 3;
	boolean counterClockwise = false;
	int tickInterval = 2;
	float ticksPerSecond;
	float distancePerTick;
	int ticksPerRevolution;
	float orbitYOffset = 0;
	
	ModifierSet modifiers = null;
	
	int taskId = -1;
	
	public abstract void loadFromString(String string);
	
	public final void loadFromConfiguration(ConfigurationSection config) {
		heightOffset = config.getDouble("height-offset", heightOffset);
		forwardOffset = config.getDouble("forward-offset", forwardOffset);
		delay = config.getInt("delay", delay);
		
		distanceBetween = config.getDouble("distance-between", distanceBetween);
		
		effectInterval = config.getInt("effect-interval", effectInterval);

		orbitRadius = (float)config.getDouble("orbit-radius", orbitRadius);
		secondsPerRevolution = (float)config.getDouble("orbit-seconds-per-revolution", secondsPerRevolution);
		counterClockwise = config.getBoolean("orbit-counter-clockwise", counterClockwise);
		tickInterval = config.getInt("orbit-tick-interval", tickInterval);
		ticksPerSecond = 20F / (float)tickInterval;
		distancePerTick = 6.28F / (ticksPerSecond * secondsPerRevolution);
		ticksPerRevolution = Math.round(ticksPerSecond * secondsPerRevolution);
		orbitYOffset = (float)config.getDouble("orbit-y-offset", orbitYOffset);
		
		List<String> list = config.getStringList("modifiers");
		if (list != null) {
			modifiers = new ModifierSet(list);
		}
		
		loadFromConfig(config);
	}
	
	protected abstract void loadFromConfig(ConfigurationSection config);
	
	/**
	 * Plays an effect on the specified entity.
	 * @param entity the entity to play the effect on
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public final void playEffect(final Entity entity) {
		if (delay <= 0) {
			playEffectEntity(entity);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					playEffectEntity(entity);
				}
			}, delay);
		}
	}
	
	protected void playEffectEntity(Entity entity) {
		playEffectLocationReal(entity == null ? null : entity.getLocation());
	}
	
	/**
	 * Plays an effect at the specified location.
	 * @param location location to play the effect at
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public final void playEffect(final Location location) {
		if (delay <= 0) {
			playEffectLocationReal(location);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					playEffectLocationReal(location);
				}
			}, delay);
		}
	}
	
	private void playEffectLocationReal(Location location) {
		if (location == null) {
			playEffectLocation(null);
		} else if (heightOffset != 0 || forwardOffset != 0) {
			Location loc = location.clone();
			if (heightOffset != 0) {
				loc.setY(loc.getY() + heightOffset);
			}
			if (forwardOffset != 0) {
				loc.add(loc.getDirection().setY(0).normalize().multiply(forwardOffset));
			}
			playEffectLocation(loc);
		} else {
			playEffectLocation(location);
		}
	}
	
	protected void playEffectLocation(Location location) {
		
	}
	
	/**
	 * Plays an effect between two locations (such as a smoke trail type effect).
	 * @param location1 the starting location
	 * @param location2 the ending location
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public final void playEffect(final Location location1, final Location location2) {
		if (delay <= 0) {
			playEffectLine(location1, location2);
		} else {
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					playEffectLine(location1, location2);
				}
			}, delay);
		}
	}
	
	protected void playEffectLine(Location location1, Location location2) {
		int c = (int)Math.ceil(location1.distance(location2) / distanceBetween) - 1;
		if (c <= 0) return;
		Vector v = location2.toVector().subtract(location1.toVector()).normalize().multiply(distanceBetween);
		Location l = location1.clone();
		if (heightOffset != 0) {
			l.setY(l.getY() + heightOffset);
		}
		
		for (int i = 0; i < c; i++) {
			l.add(v);
			playEffect(l);
		}
	}
	
	public void playEffectWhileActiveOnEntity(final Entity entity, final SpellEffectActiveChecker checker) {
		taskId = MagicSpells.scheduleRepeatingTask(new Runnable() {
			public void run() {
				if (checker.isActive(entity)) {
					playEffect(entity);
				}
			}
		}, 0, effectInterval);
	}
	
	public void playEffectWhileActiveOrbit(final Entity entity, final SpellEffectActiveChecker checker) {
		new OrbitTracker(entity, checker);
	}
	
	public interface SpellEffectActiveChecker {
		public boolean isActive(Entity entity);
	}
	
	class OrbitTracker implements Runnable {
		
		Entity entity;
		SpellEffectActiveChecker checker;
		Vector currentPosition;
		int taskId;
		
		int counter = 0;
		
		public OrbitTracker(Entity entity, SpellEffectActiveChecker checker) {
			this.entity = entity;
			this.checker = checker;
			this.currentPosition = entity.getLocation().getDirection().setY(0);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
		}
		
		@Override
		public void run() {
			// check for valid and alive caster and target
			if (!entity.isValid()) {
				stop();
				return;
			}
			
			// check if duration is up
			if (counter++ % ticksPerRevolution == 0 && !checker.isActive(entity)) {
				stop();
				return;
			}
			
			// move projectile and calculate new vector
			Location loc = getLocation();
			
			// show effect
			playEffect(loc);
			
		}
		
		private Location getLocation() {
			Vector perp;
			if (counterClockwise) {
				perp = new Vector(currentPosition.getZ(), 0, -currentPosition.getX());
			} else {
				perp = new Vector(-currentPosition.getZ(), 0, currentPosition.getX());
			}
			currentPosition.add(perp.multiply(distancePerTick)).normalize();
			return entity.getLocation().add(0, orbitYOffset, 0).add(currentPosition.clone().multiply(orbitRadius));
		}
		
		public void stop() {
			MagicSpells.cancelTask(taskId);
			entity = null;
			currentPosition = null;
		}
		
	}
	
	private static HashMap<String, Class<? extends SpellEffect>> effects = new HashMap<String, Class<? extends SpellEffect>>();
	
	/**
	 * Gets the GraphicalEffect by the provided name.
	 * @param name the name of the effect
	 * @return
	 */
	public static SpellEffect createNewEffectByName(String name) {
		Class<? extends SpellEffect> clazz = effects.get(name.toLowerCase());
		if (clazz != null) {
			try {
				return clazz.newInstance();
			} catch (Exception e) {
				return null;
			}
		}
		return null;
	}
	
	/**
	 * Adds an effect with the provided name to the list of available effects.
	 * This will replace an existing effect if the same name is used.
	 * @param name the name of the effect
	 * @param effect the effect to add
	 */
	public static void addEffect(String name, Class<? extends SpellEffect> effect) {
		effects.put(name.toLowerCase(), effect);
	}
	
	static {
		effects.put("actionbartext", ActionBarTextEffect.class);
		effects.put("angry", AngryEffect.class);
		effects.put("bigsmoke", BigSmokeEffect.class);
		effects.put("blockbreak", BlockBreakEffect.class);
		effects.put("bluesparkle", BlueSparkleEffect.class);
		effects.put("broadcast", BroadcastEffect.class);
		effects.put("cloud", CloudEffect.class);
		effects.put("dragondeath", DragonDeathEffect.class);
		effects.put("ender", EnderSignalEffect.class);
		effects.put("explosion", ExplosionEffect.class);
		effects.put("fireworks", FireworksEffect.class);
		effects.put("greensparkle", GreenSparkleEffect.class);
		effects.put("hearts", HeartsEffect.class);
		effects.put("itemcooldown", ItemCooldownEffect.class);
		effects.put("itemspray", ItemSprayEffect.class);
		effects.put("lightning", LightningEffect.class);
		effects.put("nova", NovaEffect.class);
		effects.put("particles", ParticlesEffect.class);
		effects.put("particlecloud", ParticleCloudEffect.class);
		effects.put("particleline", ParticleLineEffect.class);
		effects.put("potion", PotionEffect.class);
		effects.put("smoke", SmokeEffect.class);
		effects.put("smokeswirl", SmokeSwirlEffect.class);
		effects.put("smoketrail", SmokeTrailEffect.class);
		effects.put("sound", SoundEffect.class);
		effects.put("soundpersonal", SoundPersonalEffect.class);
		effects.put("spawn", MobSpawnerEffect.class);
		effects.put("splash", SplashPotionEffect.class);
		effects.put("title", TitleEffect.class);
	}
	
}
