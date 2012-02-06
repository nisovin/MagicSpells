package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellAnimation;

public class GeyserSpell extends TargetedSpell {
	
	private int damage;
	private double velocity;
	private int tickInterval;
	private int geyserHeight;
	private Material geyserType;
	private boolean ignoreArmor;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	private String strNoTarget;

	public GeyserSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigInt("damage", 0);
		velocity = getConfigInt("velocity", 10) / 10.0D;
		tickInterval = getConfigInt("animation-speed", 2);
		geyserHeight = getConfigInt("geyser-height", 4);
		String s = getConfigString("geyser-type", "water");
		if (s.equalsIgnoreCase("lava")) {
			geyserType = Material.STATIONARY_LAVA;
		} else {
			geyserType = Material.STATIONARY_WATER;
		}
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail -- no target
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			int dam = Math.round(damage*power);
			
			// check plugins
			if (target instanceof Player && checkPlugins) {
				EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.CUSTOM, dam);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				}
				dam = event.getDamage();
			}
			
			// do damage and launch target
			if (dam > 0) {
				if (ignoreArmor) {
					int health = target.getHealth() - dam;
					if (health < 0) health = 0;
					target.setHealth(health);
				} else {
					target.damage(dam);
				}
			}
			if (velocity > 0) {
				target.setVelocity(new Vector(0, velocity*power, 0));
			}
			
			// create animation
			if (geyserHeight > 0) {
				List<Entity> allNearby = target.getNearbyEntities(50, 50, 50);
				List<Player> playersNearby = new ArrayList<Player>();
				for (Entity e : allNearby) {
					if (e instanceof Player) {
						playersNearby.add((Player)e);
					}
				}
				new GeyserAnimation(target.getLocation(), playersNearby);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private class GeyserAnimation extends SpellAnimation {

		private Location start;
		private List<Player> nearby;
		
		public GeyserAnimation(Location start, List<Player> nearby) {
			super(0, tickInterval, true);
			this.start = start;
			this.nearby = nearby;
		}

		@Override
		protected void onTick(int tick) {
			if (tick > geyserHeight*2) {
				stop();
			} else if (tick < geyserHeight) {
				Block block = start.clone().add(0,tick,0).getBlock();
				if (block.getType() == Material.AIR) {
					for (Player p : nearby) {
						p.sendBlockChange(block.getLocation(), geyserType, (byte)0);
					}
				}
			} else {
				int n = geyserHeight-(tick-geyserHeight)-1; // top to bottom
				//int n = tick-height; // bottom to top
				Block block = start.clone().add(0, n, 0).getBlock();
				for (Player p : nearby) {
					p.sendBlockChange(block.getLocation(), block.getType(), block.getData());
				}
			}
		}
		
	}

}
