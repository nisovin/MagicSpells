package com.nisovin.magicspells.spells.instant;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Scanner;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class MarkSpell extends InstantSpell {
	
	private boolean permanentMarks;
	
	public static HashMap<String,MagicLocation> marks;

	public MarkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		permanentMarks = config.getBoolean("spells." + spellName + ".permanent-marks", true);
		
		marks = new HashMap<String,MagicLocation>();
		
		if (permanentMarks) {
			loadMarks();
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			marks.put(player.getName(), new MagicLocation(player.getLocation()));
			if (permanentMarks) {
				saveMarks();
			}
		}
		return PostCastAction.HANDLE_NORMALLY;		
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (!permanentMarks) {
			marks.remove(event.getPlayer().getName());
		}
	}
	
	private void loadMarks() {
		try {
			Scanner scanner = new Scanner(new File(MagicSpells.plugin.getDataFolder(), "marks.txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (!line.equals("")) {
					try {
						String[] data = line.split(":");
						MagicLocation loc = new MagicLocation(data[1], Double.parseDouble(data[2]), Double.parseDouble(data[3]), Double.parseDouble(data[4]), Float.parseFloat(data[5]), Float.parseFloat(data[6]));
						marks.put(data[0], loc);
					} catch (Exception e) {
						MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Failed to load mark: " + line);
					}
				}
			}
			scanner.close();
		} catch (Exception e) {
		}
	}
	
	private void saveMarks() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MagicSpells.plugin.getDataFolder(), "marks.txt"), false));
			for (String name : marks.keySet()) {
				MagicLocation loc = marks.get(name);
				writer.append(name + ":" + loc.getWorld() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ() + ":" + loc.getYaw() + ":" + loc.getPitch());
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Error saving marks");
		}		
	}

}
