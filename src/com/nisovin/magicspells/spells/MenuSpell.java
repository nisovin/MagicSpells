package com.nisovin.magicspells.spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	Random random = new Random();
	
	String title;	
	int delay;
	boolean requireEntityTarget;
	boolean requireLocationTarget;
	boolean targetOpensMenuInstead;
	boolean bypassNormalCast;
	
	List<ItemStack> items;
	List<Spell> spells;
	List<String> confSpells;
	float[] powers;
	int size = 9;
	
	Map<String, Float> castPower = new HashMap<String, Float>();
	Map<String, LivingEntity> castEntityTarget = new HashMap<String, LivingEntity>();
	Map<String, Location> castLocTarget = new HashMap<String, Location>();
	
	public MenuSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		title = ChatColor.translateAlternateColorCodes('&', getConfigString("title", "Window Title " + spellName));
		delay = getConfigInt("delay", 0);
		requireEntityTarget = getConfigBoolean("require-entity-target", false);
		requireLocationTarget = getConfigBoolean("require-location-target", false);
		targetOpensMenuInstead = getConfigBoolean("target-opens-menu-instead", false);
		bypassNormalCast = getConfigBoolean("bypass-normal-cast", true);
		
		List<String> confItems = getConfigStringList("items", null);
		confSpells = getConfigStringList("spells", null);
		List<String> confPowers = getConfigStringList("powers", null);

		items = new ArrayList<ItemStack>();
		spells = new ArrayList<Spell>();
		if (confItems != null && confItems.size() > 0) {
			powers = new float[confItems.size()];
			size = (items.size()-1) / 9 + 9;
		
			for (int i = 0; i < confItems.size(); i++) {
				items.add(null);
				spells.add(null);
				powers[i] = 1;
				String[] s = Util.splitParams(confItems.get(i));
				if (s.length > 2 && s[2].matches("^[0-9]+%?$")) {
					int chance = Integer.parseInt(s[2].replace("%", ""));
					if (random.nextInt(100) > chance) continue;
				}
				ItemStack item = Util.getItemStackFromString(s[0]);
				if (item != null) {
					if (s.length > 1 && s[1].matches("^[0-9]+$")) {
						item.setAmount(Integer.parseInt(s[1]));
					}
					items.set(i, item);
					if (confPowers != null && confPowers.size() > i) {
						try {
							String pow = confPowers.get(i);
							if (pow != null && !pow.isEmpty()) {
								powers[i] = Float.parseFloat(pow);
							}
						} catch (NumberFormatException e) {
						}
					}
				}
			}
		}
		
		if (items.size() == 0) {
			MagicSpells.error("The MenuSpell '" + spellName + "' has no menu items!");
		}
		
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		for (int i = 0; i < confSpells.size(); i++) {
			Spell spell = MagicSpells.getSpellByInternalName(confSpells.get(i));
			if (spell != null) {
				spells.set(i, spell);
			} else {
				MagicSpells.error("The MenuSpell '" + internalName + "' has an invalid spell listed: " + confSpells.get(i));
			}
		} 
		
		if (items.size() != confSpells.size()) {
			MagicSpells.error("The MenuSpell '" + internalName + "' has mismatched items and spells!");
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity entityTarget = null;
			Location locTarget = null;
			
			Player opener = player;
			
			if (requireEntityTarget) {
				entityTarget = getTargetedEntity(player, power);
				if (entityTarget == null) {
					return noTarget(player);
				}
				if (targetOpensMenuInstead) {
					if (entityTarget instanceof Player) {
						opener = (Player)entityTarget;
						entityTarget = null;
					} else {
						return noTarget(player);
					}
				}
			} else if (requireLocationTarget) {
				Block block = getTargetedBlock(player, power);
				if (block.getType() == Material.AIR) {
					return noTarget(player);
				} else {
					locTarget = block.getLocation();
				}
			}
			
			open(player, opener, entityTarget, locTarget, power);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	void open(final Player caster, Player opener, LivingEntity entityTarget, Location locTarget, final float power) {
		if (delay < 0) {
			openMenu(caster, opener, entityTarget, locTarget, power);
		} else {
			final Player p = opener;
			final LivingEntity e = entityTarget;
			final Location l = locTarget;
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					openMenu(caster, p, e, l, power);
				}
			}, delay);
		}
	}
	
	void openMenu(Player caster, Player opener, LivingEntity entityTarget, Location locTarget, float power) {
		castPower.put(opener.getName(), power);
		if (entityTarget != null) {
			castEntityTarget.put(opener.getName(), entityTarget);
		}
		if (locTarget != null) {
			castLocTarget.put(opener.getName(), locTarget);
		}
		
		Inventory inv = Bukkit.createInventory(opener, size, title);
		for (int i = 0; i < items.size(); i++) {
			inv.setItem(i, items.get(i));
		}
		opener.openInventory(inv);
		
		if (entityTarget != null && caster != null) {
			playSpellEffects(caster, entityTarget);
		} else {
			if (caster != null) {
				playSpellEffects(EffectPosition.CASTER, caster);
			}
			playSpellEffects(EffectPosition.SPECIAL, opener);
			if (locTarget != null) {
				playSpellEffects(EffectPosition.TARGET, locTarget);
			}
		}
	}
	
	@EventHandler
	public void onInvClick(InventoryClickEvent event) {
		if (event.getInventory().getTitle().equals(title)) {
			event.setCancelled(true);
			final Player player = (Player)event.getWhoClicked();
			
			int slot = event.getRawSlot();
			if (slot >= 0 && spells.size() > slot && event.getCurrentItem() != null) {
				Spell spell = spells.get(slot);
				if (spell != null) {
					float power = powers[slot];
					if (castPower.containsKey(player.getName())) {
						power *= castPower.get(player.getName()).floatValue();
					}
					if (spell instanceof TargetedEntitySpell && castEntityTarget.containsKey(player.getName())) {
						((TargetedEntitySpell)spell).castAtEntity(player, castEntityTarget.get(player.getName()), power);
					} else if (spell instanceof TargetedLocationSpell && castLocTarget.containsKey(player.getName())) {
						((TargetedLocationSpell)spell).castAtLocation(player, castLocTarget.get(player.getName()), power);
					} else if (bypassNormalCast) {
						spell.castSpell(player, SpellCastState.NORMAL, power, null);
					} else {
						spell.cast(player, power, null);
					}
				}
			}
			
			castPower.remove(player.getName());
			castEntityTarget.remove(player.getName());
			castLocTarget.remove(player.getName());
			
			MagicSpells.scheduleDelayedTask(new Runnable() {
				public void run() {
					player.closeInventory();
				}
			}, 0);
		}
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		castPower.remove(event.getPlayer().getName());
		castEntityTarget.remove(event.getPlayer().getName());
		castLocTarget.remove(event.getPlayer().getName());
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!requireEntityTarget) return false;
		if (!validTargetList.canTarget(caster, target)) return false;
		Player opener = caster;
		if (targetOpensMenuInstead) {
			if (target instanceof Player) {
				opener = (Player)target;
				target = null;
			} else {
				return false;
			}
		}
		open(caster, opener, target, null, power);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!requireEntityTarget || !targetOpensMenuInstead) return false;
		if (!validTargetList.canTarget(target)) return false;
		if (target instanceof Player) {
			open(null, (Player)target, null, null, power);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtLocation(Player caster, Location target, float power) {
		if (!requireLocationTarget) return false;
		open(caster, caster, null, target, power);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (args.length == 1) {
			Player player = Bukkit.getPlayer(args[0]);
			if (player != null) {
				open(null, player, null, null, 1);
				return true;
			}
		}
		return false;
	}

}
