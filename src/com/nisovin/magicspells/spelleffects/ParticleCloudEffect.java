package com.nisovin.magicspells.spelleffects;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.AreaEffectCloud;

import com.nisovin.magicspells.util.Util_1_9;

class ParticleCloudEffect extends SpellEffect {
	
	Particle particle = Particle.EXPLOSION_NORMAL;
	float radius = 5f;
	float radiusPerTick = 0f;
	int duration = 60;
	int color = 0xFF0000;
	float yOffset = 0F;

	@Override
	public void loadFromString(String string) {
	}

	@Override
	public void loadFromConfig(ConfigurationSection config) {
		particle = Util_1_9.getParticleFromName(config.getString("particle-name", "explode"));
		radius = (float)config.getDouble("radius", radius);
		radiusPerTick = (float)config.getDouble("radius-per-tick", radiusPerTick);
		duration = config.getInt("duration", duration);
		color = config.getInt("color", color);
		yOffset = (float)config.getDouble("y-offset", yOffset);
	}

	@Override
	public void playEffectLocation(Location location) {
		AreaEffectCloud aec = location.getWorld().spawn(location.clone().add(0, yOffset, 0), AreaEffectCloud.class);
		aec.setParticle(particle);
		aec.setRadius(radius);
		aec.setRadiusPerTick(radiusPerTick);
		aec.setDuration(duration);
		aec.setColor(Color.fromRGB(color));
	}
	
}
