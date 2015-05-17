package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class FlightPathSpell extends InstantSpell {

	static FlightHandler flightHandler;

	float targetX;
	float targetY;
	float targetZ;
	int cruisingAltitude;
	float speed;
	EntityType mount;
	
	public FlightPathSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		targetX = getConfigFloat("x", 0);
		targetY = getConfigFloat("y", 70);
		targetZ = getConfigFloat("z", 0);
		cruisingAltitude = getConfigInt("cruising-altitude", 150);
		speed = getConfigFloat("speed", 1.5f);
		mount = Util.getEntityType(getConfigString("mount", ""));
		
		if (flightHandler == null) {
			flightHandler = new FlightHandler();
		}
		
	}
	
	@Override
	public void initialize() {
		flightHandler.init();
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			ActiveFlight flight = new ActiveFlight(player, mount, this);
			flightHandler.addFlight(flight);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
	public void turnOff() {
		if (flightHandler != null) {
			flightHandler.turnOff();
			flightHandler = null;
		}
	}	
	
	class FlightHandler implements Runnable, Listener {

		boolean inited = false;
		
		Map<String, ActiveFlight> flights = new HashMap<String, ActiveFlight>();
		int task = -1;
		
		public void addFlight(ActiveFlight flight) {
			flights.put(flight.player.getName(), flight);
			flight.start();
			if (task < 0) {
				task = MagicSpells.scheduleRepeatingTask(this, 0, 5);
			}
		}
		
		void init() {
			if (!inited) {
				inited = true;
				MagicSpells.registerEvents(this);
			}
		}

		void cancel(Player player) {
			ActiveFlight flight = flights.remove(player.getName());
			if (flight != null) {
				flight.cancel();
			}
		}
		
		void turnOff() {
			for (ActiveFlight flight : flights.values()) {
				flight.cancel();
			}
			MagicSpells.cancelTask(task);
			flights.clear();
		}
		
		@EventHandler
		void onTeleport(PlayerTeleportEvent event) {
			cancel(event.getPlayer());
		}
		
		@EventHandler
		void onPlayerDeath(PlayerDeathEvent event) {
			cancel(event.getEntity());
		}
		
		@EventHandler
		void onQuit(PlayerQuitEvent event) {
			cancel(event.getPlayer());
		}
		
		@Override
		public void run() {
			Iterator<ActiveFlight> iter = flights.values().iterator();
			while (iter.hasNext()) {
				ActiveFlight flight = iter.next();
				if (flight.isDone()) {
					iter.remove();
				} else {
					flight.fly();
				}
			}
			if (flights.size() == 0) {
				MagicSpells.cancelTask(task);
				task = -1;
			}
		}
		
	}
	
	class ActiveFlight {
		Player player;
		EntityType mountType;
		Entity mount;
		Entity entityToPush;
		FlightPathSpell spell;
		FlightState state;
		boolean wasFlyingAllowed;
		boolean wasFlying;
		
		Location lastLocation;
		int sameLocCount = 0;
		
		public ActiveFlight(Player player, EntityType mountType, FlightPathSpell spell) {
			this.player = player;
			this.mountType = mountType;
			this.spell = spell;
			this.state = FlightState.TAKE_OFF;
			this.wasFlyingAllowed = player.getAllowFlight();
			this.wasFlying = player.isFlying();
			this.lastLocation = player.getLocation();
		}
		
		void start() {
			player.setAllowFlight(true);
			spell.playSpellEffects(EffectPosition.CASTER, player);
			if (mountType == null) {
				entityToPush = player;
			} else {
				mount = player.getWorld().spawnEntity(player.getLocation(), mountType);
				entityToPush = mount;
				if (player.getVehicle() != null) {
					player.getVehicle().eject();
				}
				mount.setPassenger(player);
			}
		}
		
		void fly() {
			if (state == FlightState.DONE) return;
			
			// check for stuck
			if (player.getLocation().distanceSquared(lastLocation) < 0.4) {
				sameLocCount++;
			}
			if (sameLocCount > 12) {
				MagicSpells.error("Flight stuck '" + spell.getInternalName() + "' at " + player.getLocation());
				cancel();
				return;
			}
			lastLocation = player.getLocation();
			
			// do flight
			if (state == FlightState.TAKE_OFF) {
				player.setFlying(false);
				double y = entityToPush.getLocation().getY();
				if (y >= cruisingAltitude) {
					entityToPush.setVelocity(new Vector(0, 0, 0));
					state = FlightState.CRUISING;
				} else {
					entityToPush.setVelocity(new Vector(0, 2, 0));
				}
			} else if (state == FlightState.CRUISING) {
				player.setFlying(true);
				double x = entityToPush.getLocation().getX();
				double z = entityToPush.getLocation().getZ();
				if ((targetX - 1 <= x && x <= targetX + 1) && (targetZ - 1 <= z && z <= targetZ + 1)) {
					entityToPush.setVelocity(new Vector(0, 0, 0));
					state = FlightState.LANDING;
				} else {
					Vector t = new Vector(targetX, cruisingAltitude, targetZ);
					Vector v = t.subtract(entityToPush.getLocation().toVector());
					double len = v.lengthSquared();
					v.normalize().multiply(len > 25 ? speed : 0.3);
					entityToPush.setVelocity(v);
				}
			} else if (state == FlightState.LANDING) {
				player.setFlying(false);
				Location l = entityToPush.getLocation();
				if (l.getBlock().getType() != Material.AIR || l.subtract(0, 1, 0).getBlock().getType() != Material.AIR || l.subtract(0, 2, 0).getBlock().getType() != Material.AIR) {
					player.setFallDistance(0f);
					cancel();
				} else {
					entityToPush.setVelocity(new Vector(0, -1, 0));
					player.setFallDistance(0f);
				}
			}
			
			spell.playSpellEffects(EffectPosition.SPECIAL, player);
		}
		
		void cancel() {
			if (state != FlightState.DONE) {
				state = FlightState.DONE;
				player.setFlying(wasFlying);
				player.setAllowFlight(wasFlyingAllowed);
				if (mount != null) {
					mount.eject();
					mount.remove();
				}
				spell.playSpellEffects(EffectPosition.DELAYED, player);
				
				player = null;
				mount = null;
				entityToPush = null;
				spell = null;
			}
		}
		
		boolean isDone() {
			return state == FlightState.DONE;
		}
		
	}
	
	enum FlightState {
		TAKE_OFF,
		CRUISING,
		LANDING,
		DONE
	}

	
}
