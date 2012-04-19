package com.nisovin.magicspells.spells.command;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.spells.CommandSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;

public class KeybindSpell extends CommandSpell {

	private ItemStack wandItem;
	private ItemStack defaultSpellIcon;
	
	private HashMap<String,Keybinds> playerKeybinds;
	
	public KeybindSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		String temp = getConfigString("wand-item", Material.BLAZE_ROD.getId()+"");
		if (temp.contains(":")) {
			String[] data = temp.split(":");
			wandItem = new ItemStack(Integer.parseInt(data[0]), 0, Short.parseShort(data[1]));
		} else {
			wandItem = new ItemStack(Integer.parseInt(temp), 0, (short)0);
		}
		temp = getConfigString("default-spell-icon", Material.REDSTONE.getId()+"");
		if (temp.contains(":")) {
			String[] data = temp.split(":");
			defaultSpellIcon = new ItemStack(Integer.parseInt(data[0]), 0, Short.parseShort(data[1]));
		} else {
			defaultSpellIcon = new ItemStack(Integer.parseInt(temp), 0, (short)0);
		}
		
		playerKeybinds = new HashMap<String, KeybindSpell.Keybinds>();
	}
	
	@Override
	protected void initialize() {
		super.initialize();
		for (Player p : Bukkit.getOnlinePlayers()) {
			loadKeybinds(p);
		}
	}

	private void loadKeybinds(Player player) {
		System.out.println("test");
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + player.getName().toLowerCase() + ".txt");
		if (file.exists()) {		
			System.out.println("test2");	
			try {
				Keybinds keybinds = new Keybinds(player);				
				YamlConfiguration conf = new YamlConfiguration();
				conf.load(file);
				for (String key : conf.getKeys(false)) {
					System.out.println("test"+key);
					int slot = Integer.parseInt(key);
					String spellName = conf.getString(key);
					Spell spell = MagicSpells.getSpellByInternalName(spellName);
					if (spell != null) {
						System.out.println("test"+key+" "+spellName);
						keybinds.setKeybind(slot, spell);
					}
				}
				playerKeybinds.put(player.getName(), keybinds);
			} catch (Exception e) {
				MagicSpells.plugin.getLogger().severe("Failed to load player keybinds for " + player.getName());
				e.printStackTrace();
			}
		}
	}
	
	private void saveKeybinds(Keybinds keybinds) {		
		File file = new File(MagicSpells.plugin.getDataFolder(), "spellbooks" + File.separator + "keybinds-" + keybinds.player.getName().toLowerCase() + ".txt");
		YamlConfiguration conf = new YamlConfiguration();
		Spell[] binds = keybinds.keybinds;
		for (int i = 0; i < binds.length; i++) {
			if (binds[i] != null) {
				conf.set(i+"", binds[i].getInternalName());
			}
		}
		try {
			conf.save(file);
		} catch (IOException e) {
			MagicSpells.plugin.getLogger().severe("Failed to save keybinds for " + keybinds.player.getName());
			e.printStackTrace();
		}
	}
	
	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args.length != 1) {
				// fail
				player.sendMessage("invalid args");
				return PostCastAction.ALREADY_HANDLED;
			}
			
			Keybinds keybinds = playerKeybinds.get(player.getName());
			if (keybinds == null) {
				keybinds = new Keybinds(player);
				playerKeybinds.put(player.getName(), keybinds);
			}
			
			int slot = player.getInventory().getHeldItemSlot();
			ItemStack item = player.getItemInHand();
			
			if (args[0].equalsIgnoreCase("clear")) {
				keybinds.clearKeybind(slot);
				saveKeybinds(keybinds);
			} else if (args[0].equalsIgnoreCase("clearall")) {
				keybinds.clearKeybinds();
				saveKeybinds(keybinds);
			} else {
				if (item != null && item.getType() != Material.AIR) {
					// fail
					player.sendMessage("not empty");
					return PostCastAction.ALREADY_HANDLED;
				}
				
				Spell spell = MagicSpells.getSpellbook(player).getSpellByName(args[0]);
				if (spell == null) {
					// fail
					player.sendMessage("no spell");
					return PostCastAction.ALREADY_HANDLED;
				}
				
				keybinds.setKeybind(slot, spell);
				keybinds.select(slot);
				saveKeybinds(keybinds);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds != null) {
			System.out.println("old: " + event.getPreviousSlot() + "; new: " + event.getNewSlot());
			keybinds.deselect(event.getPreviousSlot());
			keybinds.select(event.getNewSlot());
		}
	}
	
	@EventHandler
	public void onAnimate(PlayerAnimationEvent event) {
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds != null) {
			boolean casted = keybinds.castKeybind(event.getPlayer().getInventory().getHeldItemSlot());
			if (casted) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.isCancelled()) return;
		
		Keybinds keybinds = playerKeybinds.get(event.getPlayer().getName());
		if (keybinds != null && keybinds.hasKeybind(event.getPlayer().getInventory().getHeldItemSlot())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		loadKeybinds(event.getPlayer());
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		return false;
	}
	
	protected void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, item);
	}
	
	private class Keybinds {
		private Player player;
		private Spell[] keybinds;
		
		public Keybinds(Player player) {
			this.player = player;
			this.keybinds = new Spell[10];
		}
		
		public void deselect(int slot) {
			Spell spell = keybinds[slot];
			if (spell != null) {
				ItemStack spellIcon = spell.getSpellIcon();
				if (spellIcon == null) {
					spellIcon = defaultSpellIcon;
				}
				//player.getInventory().setItem(slot, new ItemStack(spellIcon.getType(), 0, spellIcon.getDurability()));
				sendFakeSlotUpdate(player, slot, spellIcon);
			}
		}
		
		public void select(int slot) {
			Spell spell = keybinds[slot];
			if (spell != null) {
				sendFakeSlotUpdate(player, slot, wandItem);
			}
		}
		
		public boolean hasKeybind(int slot) {
			return (keybinds[slot] != null);
		}
		
		public boolean castKeybind(int slot) {
			Spell spell = keybinds[slot];
			if (spell != null) {
				spell.cast(player);
				return true;
			}
			return false;
		}
		
		public void setKeybind(int slot, Spell spell) {
			keybinds[slot] = spell;
		}
		
		public void clearKeybind(int slot) {
			keybinds[slot] = null;
			sendFakeSlotUpdate(player, slot, null);
		}
		
		public void clearKeybinds() {
			for (int i = 0; i < keybinds.length; i++) {
				if (keybinds[i] != null) {
					keybinds[i] = null;
					sendFakeSlotUpdate(player, i, null);
				}
			}
		}
	}

}
