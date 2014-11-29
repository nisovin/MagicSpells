package com.nisovin.magicspells.volatilecode;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;

public class VolatileCodeProtocolLib extends VolatileCodeDisabled {

	protected ProtocolManager protocolManager;

	public VolatileCodeProtocolLib() {
		protocolManager = ProtocolLibrary.getProtocolManager();
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		PacketContainer packet = protocolManager.createPacket(62);
		packet.getStrings().write(0, sound);
		int p = (int)(pitch * 63D);
		if (p < 0) p = 0;
		if (p > 255) p = 255;
		packet.getIntegers()
			.write(0, (int)(location.getX() * 8D))
			.write(1, (int)(location.getY() * 8D))
			.write(2, (int)(location.getZ() * 8D))
			.write(3, p);
		packet.getFloat().write(0, volume);
		protocolManager.broadcastServerPacket(packet, location, volume > 1.0 ? (int)(16 * volume) : 16);
	}

	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		Location loc = player.getLocation();
		PacketContainer packet = protocolManager.createPacket(62);
		packet.getStrings().write(0, sound);
		int p = (int)(pitch * 63D);
		if (p < 0) p = 0;
		if (p > 255) p = 255;
		packet.getIntegers()
			.write(0, (int)(loc.getX() * 8D))
			.write(1, (int)(loc.getY() * 8D))
			.write(2, (int)(loc.getZ() * 8D))
			.write(3, p);
		packet.getFloat().write(0, volume);
		try {
			protocolManager.sendServerPacket(player, packet);
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset) {
		PacketContainer packet = protocolManager.createPacket(63);
		packet.getStrings().write(0, name);
		packet.getFloat()
			.write(0, (float)location.getX())
			.write(1, (float)location.getY() + yOffset)
			.write(2, (float)location.getZ())
			.write(3, spreadHoriz)
			.write(4, spreadVert)
			.write(5, spreadHoriz)
			.write(6, speed);
		packet.getIntegers().write(0, count);
		packet.getBooleans().write(0, radius > 200);
		protocolManager.broadcastServerPacket(packet, location, radius);
	}
	
}
