package com.nisovin.magicspells.spells.targeted;

import java.util.ArrayList;
import java.util.List;
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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.MagicConfig;

public class SpawnMonsterSpell extends TargetedSpell implements TargetedLocationSpell {

	private String location;
	private EntityType entityType;
	private boolean allowSpawnInMidair;
	private boolean baby;
	private boolean tamed;
	
	private ItemStack holding;
	private ItemStack helmet;
	private ItemStack chestplate;
	private ItemStack leggings;
	private ItemStack boots;
	private float holdingDropChance;
	private float helmetDropChance;
	private float chestplateDropChance;
	private float leggingsDropChance;
	private float bootsDropChance;
	private List<PotionEffect> potionEffects;
	private int duration;
	private String nameplateText;
	private boolean useCasterName;
	private boolean removeAI;
	private boolean addLookAtPlayerAI;
	
	private String[] attributeTypes;
	private double[] attributeValues;
	private int[] attributeOperations;
	
	private Random random = new Random();
	
	public SpawnMonsterSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		location = getConfigString("location", "target");
		entityType = Util.getEntityType(getConfigString("entity-type", "wolf"));
		allowSpawnInMidair = getConfigBoolean("allow-spawn-in-midair", false);
		baby = getConfigBoolean("baby", false);
		tamed = getConfigBoolean("tamed", false);
		holding = Util.getItemStackFromString(getConfigString("holding", "0"));
		if (holding != null && holding.getType() != Material.AIR) {
			holding.setAmount(1);
		}
		helmet = Util.getItemStackFromString(getConfigString("helmet", "0"));
		if (helmet != null && helmet.getType() != Material.AIR) {
			helmet.setAmount(1);
		}
		chestplate = Util.getItemStackFromString(getConfigString("chestplate", "0"));
		if (chestplate != null && chestplate.getType() != Material.AIR) {
			chestplate.setAmount(1);
		}
		leggings = Util.getItemStackFromString(getConfigString("leggings", "0"));
		if (leggings != null && leggings.getType() != Material.AIR) {
			leggings.setAmount(1);
		}
		boots = Util.getItemStackFromString(getConfigString("boots", "0"));
		if (boots != null && boots.getType() != Material.AIR) {
			boots.setAmount(1);
		}
		holdingDropChance = getConfigFloat("holding-drop-chance", 0) / 100F;
		helmetDropChance = getConfigFloat("helmet-drop-chance", 0) / 100F;
		chestplateDropChance = getConfigFloat("chestplate-drop-chance", 0) / 100F;
		leggingsDropChance = getConfigFloat("leggings-drop-chance", 0) / 100F;
		bootsDropChance = getConfigFloat("boots-drop-chance", 0) / 100F;
		
		List<String> list = getConfigStringList("potion-effects", null);
		if (list != null && list.size() > 0) {
			potionEffects = new ArrayList<PotionEffect>();
			for (String data : list) {
				String[] split = data.split(" ");
				try {
					PotionEffectType type = Util.getPotionEffectType(split[0]);
					if (type == null) throw new Exception("");
					int duration = 600;
					if (split.length > 1) duration = Integer.parseInt(split[1]);
					int strength = 0;
					if (split.length > 2) strength = Integer.parseInt(split[2]);
					boolean ambient = false;
					if (split.length > 3 && split[3].equalsIgnoreCase("ambient")) ambient = true;
					potionEffects.add(new PotionEffect(type, duration, strength, ambient));
				} catch (Exception e) {
					MagicSpells.debug("Invalid potion effect string on '" + internalName + "' spell: " + data);
				}
			}
		}
		
		duration = getConfigInt("duration", 0);
		nameplateText = getConfigString("nameplate-text", "");
		removeAI = getConfigBoolean("remove-ai", false);
		addLookAtPlayerAI = getConfigBoolean("add-look-at-player-ai", false);
		
		List<String> attributes = getConfigStringList("attributes", null);
		if (attributes != null && attributes.size() > 0) {
			attributeTypes = new String[attributes.size()];
			attributeValues = new double[attributes.size()];
			attributeOperations = new int[attributes.size()];
			for (int i = 0; i < attributes.size(); i++) {
				String s = attributes.get(i);
				try {
					String[] data = s.split(" ");
					String type = data[0];
					double val = Double.parseDouble(data[1]);
					int op = 0;
					if (data.length > 2) {
						if (data[2].equalsIgnoreCase("mult")) {
							op = 1;
						} else if (data[2].toLowerCase().contains("add") && data[2].toLowerCase().contains("perc")) {
							op = 2;
						}
					}
					attributeTypes[i] = type;
					attributeValues[i] = val;
					attributeOperations[i] = op;
				} catch (Exception e) {
					MagicSpells.error("Invalid attribute on '" + spellName + "' spell: " + s);
				}
			}
		}
		
		if (entityType == null || !entityType.isAlive()) {
			MagicSpells.error("SpawnMonster spell '" + spellName + "' has an invalid entity-type!");
		}
		
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = null;
			
			if (location.equalsIgnoreCase("target")) {
				Block block = getTargetedBlock(player, power);
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
				loc = getRandomLocationFrom(player.getLocation(), getRange(power));				
			}
			
			if (loc == null) {
				return noTarget(player);
			}
			
			spawnMob(player, player.getLocation(), loc);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	private Location getRandomLocationFrom(Location location, int range) {
		World world = location.getWorld();
		int attempts = 0;
		int x, y, z;
		Block block, block2;
		while (attempts < 10) {
			x = location.getBlockX() + random.nextInt(range * 2) - range;
			y = location.getBlockY() + 2;
			z = location.getBlockZ() + random.nextInt(range * 2) - range;	
			
			block = world.getBlockAt(x, y, z);
			if (block.getType() == Material.STATIONARY_WATER || block.getType() == Material.WATER) {
				return block.getLocation();
			} else if (BlockUtils.isPathable(block)) {
				if (allowSpawnInMidair) {
					return block.getLocation();
				}
				int c = 0;
				while (c < 5) {
					block2 = block.getRelative(BlockFace.DOWN);
					if (BlockUtils.isPathable(block2)) {
						block = block2;
					} else {
						return block.getLocation();
					}
					c++;
				}
			}
			
			attempts++;
		}
		return null;
	}
	
	private void spawnMob(Player player, Location source, Location loc) {
		if (entityType != null) {
			// spawn it
			loc.setYaw((float) (Math.random() * 360));
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
			if (tamed && entity instanceof Tameable && player != null) {
				((Tameable)entity).setTamed(true);
				((Tameable)entity).setOwner(player);
			}
			// set held item
			if (holding != null && holding.getType() != Material.AIR) {
				if (entity instanceof Enderman) {
					((Enderman)entity).setCarriedMaterial(holding.getData());
				} else if (entity instanceof Skeleton || entity instanceof Zombie) {
					EntityEquipment equip = ((LivingEntity)entity).getEquipment();
					equip.setItemInHand(holding.clone());
					equip.setItemInHandDropChance(holdingDropChance);
				}
			}
			// set armor
			EntityEquipment equip = ((LivingEntity)entity).getEquipment();
			equip.setHelmet(helmet);
			equip.setChestplate(chestplate);
			equip.setLeggings(leggings);
			equip.setBoots(boots);
			equip.setHelmetDropChance(helmetDropChance);
			equip.setChestplateDropChance(chestplateDropChance);
			equip.setLeggingsDropChance(leggingsDropChance);
			equip.setBootsDropChance(bootsDropChance);
			// set nameplate text
			if (entity instanceof LivingEntity) {
				if (useCasterName && player != null) {
					((LivingEntity)entity).setCustomName(player.getDisplayName());
					((LivingEntity)entity).setCustomNameVisible(true);
				} else if (nameplateText != null && !nameplateText.isEmpty()) {
					((LivingEntity)entity).setCustomName(nameplateText);
					((LivingEntity)entity).setCustomNameVisible(true);
				}
			}
			// add potion effects
			if (potionEffects != null) {
				((LivingEntity)entity).addPotionEffects(potionEffects);
			}
			// add attributes
			if (attributeTypes != null && attributeTypes.length > 0) {
				for (int i = 0; i < attributeTypes.length; i++) {
					if (attributeTypes[i] != null) {
						//System.out.println("adding attr " + attributeTypes[i] + " " + attributeValues[i] + " " + attributeOperations[i]);
						MagicSpells.getVolatileCodeHandler().addEntityAttribute((LivingEntity)entity, attributeTypes[i], attributeValues[i], attributeOperations[i]);
					}
				}
			}
			// set AI
			if (removeAI) {
				MagicSpells.getVolatileCodeHandler().removeAI((LivingEntity)entity);
				if (addLookAtPlayerAI) {
					MagicSpells.getVolatileCodeHandler().addAILookAtPlayer((LivingEntity)entity, 10);
				}
			}
			// play effects
			if (player != null) {
				playSpellEffects(player, entity);
			} else {
				playSpellEffects(source, entity);
			}
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
		if (location.equalsIgnoreCase("target")) {
			spawnMob(caster, caster.getLocation(), target);
		} else if (location.equalsIgnoreCase("caster")) {
			spawnMob(caster, caster.getLocation(), caster.getLocation());
		} else if (location.equalsIgnoreCase("random")) {
			Location loc = getRandomLocationFrom(target, getRange(power));
			if (loc != null) {
				spawnMob(caster, caster.getLocation(), loc);
			}
		}
		return true;
	}
	
	@Override
	public boolean castAtLocation(Location target, float power) {
		if (location.equalsIgnoreCase("target")) {
			spawnMob(null, target, target);
		} else if (location.equalsIgnoreCase("caster")) {
			spawnMob(null, target, target);
		} else if (location.equalsIgnoreCase("random")) {
			Location loc = getRandomLocationFrom(target, getRange(power));
			if (loc != null) {
				spawnMob(null, target, loc);
			}
		}
		return true;
	}
	
}
