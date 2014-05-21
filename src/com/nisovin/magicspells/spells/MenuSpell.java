package com.nisovin.magicspells.spells;

import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.castmodifiers.ModifierSet;
import com.nisovin.magicspells.events.MagicSpellsGenericPlayerEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.Util;

public class MenuSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	Random random = new Random();
	
	String title;	
	int delay;
	boolean requireEntityTarget;
	boolean requireLocationTarget;
	boolean targetOpensMenuInstead;
	boolean bypassNormalCast;
	
	Map<String, MenuOption> options = new LinkedHashMap<String, MenuOption>();
	
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
		
		int maxSlot = 8;
		for (String optionName : getConfigKeys("options")) {
			int optionSlot = getConfigInt("options." + optionName + ".slot", -1);
			String optionSpellName = getConfigString("options." + optionName + ".spell", "");
			float optionPower = getConfigFloat("options." + optionName + ".power", 1);
			ItemStack optionItem;
			if (isConfigSection("options." + optionName + ".item")) {
				optionItem = Util.getItemStackFromConfig(getConfigSection("options." + optionName + ".item"));
			} else {
				optionItem = Util.getItemStackFromString(getConfigString("options." + optionName + ".item", "stone"));
			}
			int optionQuantity = getConfigInt("options." + optionName + ".quantity", 1);
			List<String> modifierList = getConfigStringList("options." + optionName + ".modifiers", null);
			if (optionSlot >= 0 && !optionSpellName.isEmpty() && optionItem != null) {
				optionItem.setAmount(optionQuantity);
				Util.setLoreData(optionItem, optionName);
				MenuOption option = new MenuOption();
				option.slot = optionSlot;
				option.name = optionName;
				option.spellName = optionSpellName;
				option.power = optionPower;
				option.item = optionItem;
				if (modifierList != null) {
					option.modifiers = new ModifierSet(modifierList);
				}
				options.put(optionName, option);
				if (optionSlot > maxSlot) {
					maxSlot = optionSlot;
				}
			}
		}
		size = ((maxSlot / 9) * 9) + 9;
		
		if (options.size() == 0) {
			MagicSpells.error("The MenuSpell '" + spellName + "' has no menu options!");
		}
		
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		for (MenuOption option : options.values()) {
			Spell spell = MagicSpells.getSpellByInternalName(option.spellName);
			if (spell != null) {
				option.spell = spell;
			} else {
				MagicSpells.error("The MenuSpell '" + internalName + "' has an invalid spell listed on '" + option.name + "'");
			}
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity entityTarget = null;
			Location locTarget = null;
			
			Player opener = player;
			
			if (requireEntityTarget) {
				TargetInfo<LivingEntity> targetInfo = getTargetedEntity(player, power);
				if (targetInfo != null) {
					entityTarget = targetInfo.getTarget();
				}
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
		for (MenuOption option : options.values()) {
			if (option.spell != null && inv.getItem(option.slot) == null) {
				if (option.modifiers != null) {
					MagicSpellsGenericPlayerEvent event = new MagicSpellsGenericPlayerEvent(opener);
					option.modifiers.apply(event);
					if (event.isCancelled()) continue;
				}
				ItemStack item = option.item.clone();
				ItemMeta meta = item.getItemMeta();
				meta.setDisplayName(MagicSpells.doVariableReplacements(opener, meta.getDisplayName()));
				List<String> lore = meta.getLore();
				if (lore != null && lore.size() > 1) {
					for (int i = 0; i < lore.size() - 1; i++) {
						lore.set(i, MagicSpells.doVariableReplacements(opener, lore.get(i)));
					}
					meta.setLore(lore);
				}
				item.setItemMeta(meta);
				inv.setItem(option.slot, item);
			}
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
			
			ItemStack item = event.getCurrentItem();
			if (item != null) {
				String data = Util.getLoreData(item);
				if (data != null && !data.isEmpty() && options.containsKey(data)) {
					MenuOption option = options.get(data);
					Spell spell = option.spell;
					if (spell != null) {
						float power = option.power;
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
	
	class MenuOption {
		String name;
		int slot;
		ItemStack item;
		String spellName;
		Spell spell;
		float power;
		ModifierSet modifiers;
	}

}
