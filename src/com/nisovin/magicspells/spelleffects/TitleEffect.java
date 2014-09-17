package com.nisovin.magicspells.spelleffects;

import net.minecraft.server.v1_7_R4.ChatComponentText;
import net.minecraft.server.v1_7_R4.PlayerConnection;

import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.spigotmc.ProtocolInjector.PacketTitle;

public class TitleEffect extends SpellEffect {

	String title = null;
	String subtitle = null;
	int fadeIn = 10;
	int stay = 40;
	int fadeOut = 10;
	
	@Override
	public void loadFromString(String string) {
	}

	@Override
	protected void loadFromConfig(ConfigurationSection config) {
		title = config.getString("title", title);
		if (title != null) title = ChatColor.translateAlternateColorCodes('&', title);
		subtitle = config.getString("subtitle", subtitle);
		if (subtitle != null) subtitle = ChatColor.translateAlternateColorCodes('&', subtitle);
		fadeIn = config.getInt("fade-in", fadeIn);
		stay = config.getInt("stay", stay);
		fadeOut = config.getInt("fade-out", fadeOut);
	}
	
	@Override
	protected void playEffectEntity(Entity entity) {
		if (entity instanceof Player) {
			PlayerConnection conn = ((CraftPlayer)entity).getHandle().playerConnection;
			if (conn.networkManager.getVersion() >= 47) {
				PacketTitle packet = new PacketTitle(PacketTitle.Action.TIMES, fadeIn, stay, fadeOut);
				conn.sendPacket(packet);
				if (title != null) {
					packet = new PacketTitle(PacketTitle.Action.TITLE, new ChatComponentText(title));
					conn.sendPacket(packet);
				}
				if (subtitle != null) {
					packet = new PacketTitle(PacketTitle.Action.SUBTITLE, new ChatComponentText(subtitle));
					conn.sendPacket(packet);
				}
			}
		}
	}

}
