package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class LifewalkSpell extends BuffSpell {
	
	private HashSet<String> lifewalkers;
	private Grower grower;
	private Random random;
	
	private int tickInterval;
	private int redFlowerChance;
	private int yellowFlowerChance;
	private int saplingChance;
	private int tallgrassChance;
	private int fernChance;
	
	public LifewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		lifewalkers = new HashSet<String>();
		random = new Random();
		
		tickInterval = getConfigInt("tick-interval", 15);
		redFlowerChance = getConfigInt("red-flower-chance", 15);
		yellowFlowerChance = getConfigInt("yellow-flower-chance", 15);
		saplingChance = getConfigInt("sapling-chance", 5);
		tallgrassChance = getConfigInt("tallgrass-chance", 25);
		fernChance = getConfigInt("fern-chance", 15);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (lifewalkers.contains(player.getName())) {
			turnOff(player);
			if (toggle) {
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		if (state == SpellCastState.NORMAL) {
			lifewalkers.add(player.getName());
			if (grower == null) {
				grower = new Grower();
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}	
	
	@Override
	public void turnOff(Player player) {
		if (lifewalkers.contains(player.getName())) {
			super.turnOff(player);
			lifewalkers.remove(player.getName());
			sendMessage(player, strFade);
			if (lifewalkers.size() == 0 && grower != null) {
				grower.stop();
				grower = null;
			}
		}
	}
	
	@Override
	protected void turnOff() {
		lifewalkers.clear();
		if (grower != null) {
			grower.stop();
			grower = null;
		}
	}

	private class Grower implements Runnable {
		int taskId;
		String[] strArr = new String[0];
		
		public Grower() {
			taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);
		}
		
		public void stop() {
			Bukkit.getServer().getScheduler().cancelTask(taskId);
		}
		
		public void run() {
			for (String s : lifewalkers.toArray(strArr)) {
				Player player = Bukkit.getServer().getPlayer(s);
				if (player != null) {
					if (isExpired(player)) {
						turnOff(player);
						continue;
					}
					Block feet = player.getLocation().getBlock();
					Block ground = feet.getRelative(BlockFace.DOWN);
					if (feet.getType() == Material.AIR && (ground.getType() == Material.DIRT || ground.getType() == Material.GRASS)) {
						if (ground.getType() == Material.DIRT) {
							ground.setType(Material.GRASS);
						}
						int rand = random.nextInt(100);
						if (rand < redFlowerChance) {
							feet.setType(Material.RED_ROSE);
							addUse(player);
							chargeUseCost(player);
						} else {
							rand -= redFlowerChance;
							if (rand < yellowFlowerChance) {
								feet.setType(Material.YELLOW_FLOWER);
								addUse(player);
								chargeUseCost(player);
							} else {
								rand -= yellowFlowerChance;
								if (rand < saplingChance) {
									feet.setType(Material.SAPLING);
									addUse(player);
									chargeUseCost(player);
								} else {
									rand -= saplingChance;
									if (rand < tallgrassChance) {
										feet.setTypeId(31);
										feet.setData((byte)1);
										addUse(player);
										chargeUseCost(player);
									} else {
										rand -= tallgrassChance;
										if (rand < fernChance) {
											feet.setTypeId(31);
											feet.setData((byte)2);
											addUse(player);
											chargeUseCost(player);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return lifewalkers.contains(player.getName());
	}



}