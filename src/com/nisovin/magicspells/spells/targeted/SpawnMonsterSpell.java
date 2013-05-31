package com.nisovin.magicspells.spells.targeted;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicConfig;

public class SpawnMonsterSpell extends TargetedLocationSpell {

	private String location;
	private EntityType entityType;
	private boolean baby;
	private boolean tamed;
	private ItemStack holding;
	private int duration;
	private String nameplateText;
	private boolean useCasterName;
	
	private Random random = new Random();
	
	public SpawnMonsterSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		location = getConfigString("location", "target");
		entityType = EntityType.fromName(getConfigString("entity-type", "wolf"));
		baby = getConfigBoolean("baby", false);
		tamed = getConfigBoolean("tamed", false);
		holding = Util.getItemStackFromString(getConfigString("holding", "0"));
		if (holding != null && holding.getTypeId() > 0) {
			holding.setAmount(1);
		}
		duration = getConfigInt("duration", 0);
		nameplateText = getConfigString("nameplate-text", "");
		useCasterName = getConfigBoolean("use-caster-name", false);
		
		if (entityType == null) {
			MagicSpells.error("SpawnMonster spell '" + spellName + "' has an invalid entity-type!");
		}
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			
			if (location.equalsIgnoreCase("target")) {
				Block block = player.getTargetBlock(MagicSpells.getTransparentBlocks(), range);
				if (block != null && block.getType() != Material.AIR) { 
					if (BlockUtils.isPathable(block)) {
						loc = block.getLocation();
					} else if (BlockUtils.isPathable(block.getRelative(BlockFace.UP))) {
						loc = block.getLocation().add(0, 1, 0);
					}
				}
			} else if (location.equalsIgnoreCase("caster")) {
				loc = player.getLocation();
			} else if (location.equalsIgnoreCase("random")) {				
				Location playerLoc = player.getLocation();
				World world = playerLoc.getWorld();
				
				int attempts = 0;
				int x, y, z;
				Block block, block2;
				while (loc == null && attempts < 10) {
					x = playerLoc.getBlockX() + random.nextInt(range * 2) - range;
					y = playerLoc.getBlockY() + 2;
					z = playerLoc.getBlockZ() + random.nextInt(range * 2) - range;	
					
					block = world.getBlockAt(x, y, z);
					if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
						loc = block.getLocation();
						break;
					} else if (BlockUtils.isPathable(block)) {
						int c = 0;
						while (c < 5) {
							block2 = block.getRelative(BlockFace.DOWN);
							if (BlockUtils.isPathable(block2)) {
								block = block2;
							} else {
								loc = block.getLocation();
								break;
							}
							c++;
						}
						if (loc != null) {
							break;
						}
					}
					
					attempts++;
				}				
			}
			
			if (loc == null) {
				return noTarget(player);
			}
			
			spawnMob(player, loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private void spawnMob(Player player, Location loc) {
		if (entityType != null) {
			// spawn it
			final Entity entity = loc.getWorld().spawnEntity(loc.add(.5, .1, .5), entityType);
			// set as baby
			if (baby) {
				if (entity instanceof Ageable) {
					((Ageable)entity).setBaby();
				} else if (entity instanceof Zombie) {
					((Zombie)entity).setBaby(true);
				}
			}
			// set as tamed
			if (tamed && entity instanceof Tameable) {
				((Tameable)entity).setTamed(true);
				((Tameable)entity).setOwner(player);
			}
			// set held item
			if (holding != null && holding.getTypeId() > 0) {
				if (entity instanceof Enderman) {
					((Enderman)entity).setCarriedMaterial(new MaterialData(holding.getTypeId(), (byte)holding.getDurability()));
				} else if (entity instanceof Skeleton || entity instanceof Zombie) {
					EntityEquipment equip = ((LivingEntity)entity).getEquipment();
					equip.setItemInHand(holding.clone());
				}
			}
			// set nameplate text
			if (entity instanceof LivingEntity) {
				if (useCasterName) {
					((LivingEntity)entity).setCustomName(player.getDisplayName());
					((LivingEntity)entity).setCustomNameVisible(true);
				} else if (nameplateText != null && !nameplateText.isEmpty()) {
					((LivingEntity)entity).setCustomName(nameplateText);
					((LivingEntity)entity).setCustomNameVisible(true);
				}
			}
			// play effects
			playSpellEffects(player, entity);
			// schedule removal
			if (duration > 0) {
				MagicSpells.scheduleDelayedTask(new Runnable() {
					public void run() {
						entity.remove();
					}
				}, duration);
			}
		}
	}
	
	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		spawnMob(caster, target);
		return true;
	}
	
}
