package com.nisovin.magicspells.spelleffects;

import java.io.File;
import java.util.Collection;
import java.util.TreeSet;

import org.bukkit.Location;

import com.nisovin.magicspells.MagicSpells;

public class SoundEffect extends SpellEffect {
	
	@Override
	public void playEffect(Location location, String param) {
		String sound = "random.pop";
		float volume = 1.0F;
		float pitch = 1.0F;
		if (param != null && param.length() > 0) {
			String[] data = param.split(" ");
			sound = data[0];
			if (data.length > 1) {
				volume = Float.parseFloat(data[1]);
			}
			if (data.length > 2) {
				pitch = Float.parseFloat(data[2]);
			}
			if (sound.equals("random.wood_click")) {
				sound = "random.wood click";
			} else if (sound.equals("mob.ghast.affectionate_scream")) {
				sound = "mob.ghast.affectionate scream";
			}
		}
		MagicSpells.getVolatileCodeHandler().playSound(location, sound, volume, pitch);
	}
	
	public static void main(String[] args) {
		Collection<String> sounds = new TreeSet<String>();
		File file = new File("C:\\Users\\Justin.Baker\\AppData\\Roaming\\.minecraft\\resources\\sound3");
		parseFolder(file, "", sounds);
		for (String sound : sounds) {
			System.out.println("   * " + sound);
		}
	}
	
	static void parseFolder(File folder, String path, Collection<String> sounds) {
		File[] files = folder.listFiles();
		for (File file : files) {			
			if (file.isDirectory()) {
				parseFolder(file, path + file.getName() + ".", sounds);
			} else if (file.getName().endsWith(".ogg")) {
				String name = path + file.getName().replace(".ogg", "").replaceAll("[0-9]+$", "").replace(" ", "_");
				if (!sounds.contains(name)) {
					sounds.add(name);
				}
			}
		}
	}
	
}
