package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.ItemNameResolver.ItemTypeAndData;
import com.nisovin.magicspells.util.MagicConfig;

public class PulserSpell extends TargetedLocationSpell {

	private int totalPulses;
	private int interval;
	private int capPerPlayer;
	private int maxDistanceSquared;
	private int typeId;
	private byte data;
	private boolean unbreakable;
	private boolean onlyCountOnSuccess;
	private List<String> spellNames;
	private List<TargetedLocationSpell> spells;

	private String strAtCap;

	private HashMap<Block, Pulser> pulsers;
	private PulserTicker ticker;

	public PulserSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		totalPulses = getConfigInt("total-pulses", 5);
		interval = getConfigInt("interval", 30);
		capPerPlayer = getConfigInt("cap-per-player", 10);
		maxDistanceSquared = getConfigInt("max-distance", 30);
		maxDistanceSquared *= maxDistanceSquared;
		ItemTypeAndData type = MagicSpells.getItemNameResolver().resolve(getConfigString("block-type", "diamond_block"));
		typeId = type.id;
		data = (byte) type.data;
		unbreakable = getConfigBoolean("unbreakable", false);
		onlyCountOnSuccess = getConfigBoolean("only-count-on-success", false);
		spellNames = getConfigStringList("spells", null);

		strAtCap = getConfigString("str-at-cap", "You have too many effects at once.");

		pulsers = new HashMap<Block, Pulser>();
		ticker = new PulserTicker();
	}

	@Override
	public void initialize() {
		super.initialize();
		spells = new ArrayList<TargetedLocationSpell>();
		if (spellNames != null && spellNames.size() > 0) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null && spell instanceof TargetedLocationSpell) {
					spells.add((TargetedLocationSpell) spell);
				}
			}
		}
		if (spells.size() == 0) {
			MagicSpells.error("Pulse spell '" + internalName + "' has no spells defined!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state,
			float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (capPerPlayer > 0) {
				int count = 0;
				for (Pulser pulser : pulsers.values()) {
					if (pulser.caster.equals(player)) {
						count++;
						if (count >= capPerPlayer) {
							sendMessage(player, strAtCap);
							return PostCastAction.ALREADY_HANDLED;
						}
					}
				}
			}
			Block target = player.getTargetBlock(
					MagicSpells.getTransparentBlocks(), range);
			if (target == null || target.getType() == Material.AIR) {
				return noTarget(player);
			}
			target = target.getRelative(BlockFace.UP);
			if (target.getType() != Material.AIR
					&& target.getType() != Material.SNOW
					&& target.getType() != Material.LONG_GRASS) {
				return noTarget(player);
			}
			createPulser(player, target, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void createPulser(Player caster, Block block, float power) {
		block.setTypeIdAndData(typeId, data, true);
		pulsers.put(block, new Pulser(caster, block, power));
		ticker.start();
		playSpellEffects(caster, block.getLocation());
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		Block block = target.getBlock();
		if (block.getType() == Material.AIR 
				|| block.getType() == Material.SNOW
				|| block.getType() == Material.LONG_GRASS) {
			createPulser(caster, block, power);
			return true;
		} else {
			block = block.getRelative(BlockFace.UP);
			if (block.getType() == Material.AIR
					|| block.getType() == Material.SNOW
					|| block.getType() == Material.LONG_GRASS) {
				createPulser(caster, block, power);
				return true;
			} else {
				return false;
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Pulser pulser = pulsers.get(event.getBlock());
		if (pulser != null) {
			event.setCancelled(true);
			if (!unbreakable) {
				pulser.stop();
				event.getBlock().setType(Material.AIR);
				pulsers.remove(event.getBlock());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (pulsers.size() > 0) {
			Iterator<Block> iter = event.blockList().iterator();
			while (iter.hasNext()) {
				Block b = iter.next();
				Pulser pulser = pulsers.get(b);
				if (pulser != null) {
					iter.remove();
					if (!unbreakable) {
						pulser.stop();
						pulsers.remove(b);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onPiston(BlockPistonExtendEvent event) {
		if (pulsers.size() > 0) {
			for (Block b : event.getBlocks()) {
				Pulser pulser = pulsers.get(b);
				if (pulser != null) {
					event.setCancelled(true);
					if (!unbreakable) {
						pulser.stop();
						pulsers.remove(b);
					}
				}
			}
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		if (pulsers.size() > 0) {
			Player player = event.getEntity();
			Iterator<Pulser> iter = pulsers.values().iterator();
			while (iter.hasNext()) {
				Pulser pulser = iter.next();
				if (pulser.caster.equals(player)) {
					pulser.stop();
					iter.remove();
				}
			}
		}
	}

	@Override
	public void turnOff() {
		for (Pulser p : new ArrayList<Pulser>(pulsers.values())) {
			p.stop();
		}
		pulsers.clear();
		ticker.stop();
	}

	public class Pulser {

		Player caster;
		Block block;
		Location location;
		float power;
		int pulseCount;

		public Pulser(Player caster, Block block, float power) {
			this.caster = caster;
			this.block = block;
			this.location = block.getLocation().add(0.5, 0.5, 0.5);
			this.power = power;
			this.pulseCount = 0;
		}

		public boolean pulse() {
			if (caster.isValid() && caster.isOnline()
					&& block.getTypeId() == typeId
					&& block.getChunk().isLoaded()) {
				if (maxDistanceSquared > 0
						&& (!location.getWorld().equals(caster.getLocation().getWorld()) || location.distanceSquared(caster.getLocation()) > maxDistanceSquared)) {
					stop();
					return true;
				} else {
					boolean activated = false;
					for (TargetedLocationSpell spell : spells) {
						activated = spell.castAtLocation(caster, location, power) || activated;
					}
					playSpellEffects(EffectPosition.DELAYED, location);
					if (totalPulses > 0 && (activated || !onlyCountOnSuccess)) {
						pulseCount += 1;
						if (pulseCount >= totalPulses) {
							stop();
							return true;
						}
					}
					return false;
				}
			} else {
				stop();
				return true;
			}
		}

		public void stop() {
			block.setType(Material.AIR);
		}

	}

	public class PulserTicker implements Runnable {

		private int taskId = -1;

		public void start() {
			if (taskId < 0) {
				taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(
						MagicSpells.plugin, this, 0, interval);
			}
		}

		public void stop() {
			if (taskId > 0) {
				Bukkit.getScheduler().cancelTask(taskId);
				taskId = -1;
			}
		}

		public void run() {
			for (Map.Entry<Block, Pulser> entry : new HashMap<Block, Pulser>(pulsers).entrySet()) {
				boolean remove = entry.getValue().pulse();
				if (remove) {
					pulsers.remove(entry.getKey());
				}
			}
			if (pulsers.size() == 0) {
				stop();
			}
		}
	}

}
