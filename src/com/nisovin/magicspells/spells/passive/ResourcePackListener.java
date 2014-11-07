package com.nisovin.magicspells.spells.passive;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.v1_7_R4.EnumProtocol;
import net.minecraft.server.v1_7_R4.Packet;
import net.minecraft.server.v1_7_R4.PacketDataSerializer;

import org.bukkit.entity.Player;
import org.spigotmc.ProtocolInjector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.PassiveSpell;

public class ResourcePackListener extends PassiveListener {

	PacketListener listener;
	
	List<PassiveSpell> spellsLoaded = new ArrayList<PassiveSpell>();
	List<PassiveSpell> spellsDeclined = new ArrayList<PassiveSpell>();
	List<PassiveSpell> spellsFailed = new ArrayList<PassiveSpell>();
	List<PassiveSpell> spellsAccepted = new ArrayList<PassiveSpell>();
	
	@Override
	public void registerSpell(PassiveSpell spell, PassiveTrigger trigger, String var) {
		addPacketListener();
		if (var.equalsIgnoreCase("loaded")) {
			spellsLoaded.add(spell);
		} else if (var.equalsIgnoreCase("declined")) {
			spellsDeclined.add(spell);
		} else if (var.equalsIgnoreCase("failed")) {
			spellsFailed.add(spell);
		} else if (var.equalsIgnoreCase("accepted")) {
			spellsAccepted.add(spell);
		}
	}
	
	void addPacketListener() {
		if (listener == null) {
			
			try {
				Method method = ProtocolInjector.class.getDeclaredMethod("addPacket", EnumProtocol.class, boolean.class, int.class, Class.class);
				method.setAccessible(true);
				method.invoke(null, EnumProtocol.PLAY, false, 25, PacketPlayResourcePackStatus.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			listener = new PacketAdapter(MagicSpells.plugin, PacketType.Play.Client.RESOURCE_PACK_STATUS) {
				@Override
				public void onPacketReceiving(PacketEvent event) {
					Player player = event.getPlayer();
					int status = event.getPacket().getIntegers().read(0);
					if (status == 0) {
						activate(player, spellsLoaded);
					} else if (status == 1) {
						activate(player, spellsDeclined);
					} else if (status == 2) {
						activate(player, spellsFailed);
					} else if (status == 3) {
						activate(player, spellsAccepted);
					}
				}
			};
			ProtocolLibrary.getProtocolManager().addPacketListener(listener);
		}
	}
	
	void activate(Player player, List<PassiveSpell> spells) {
		for (PassiveSpell spell : spells) {
			spell.activate(player);
		}
	}

	@Override
	public void turnOff() {
		ProtocolLibrary.getProtocolManager().removePacketListener(listener);
		spellsLoaded.clear();
		spellsDeclined.clear();
		spellsFailed.clear();
		spellsAccepted.clear();
	}
	
	public static class PacketPlayResourcePackStatus extends Packet {
		
		String hash;
		int status;
		
	    public void a(PacketDataSerializer packetdataserializer)
	      throws IOException
	    {
	      hash = packetdataserializer.c(255);
	      status = packetdataserializer.a();
	    }

	    public void b(PacketDataSerializer packetdataserializer)
	      throws IOException
	    {
	    }

	    public void handle(net.minecraft.server.v1_7_R4.PacketListener packetlistener)
	    {
	    }
	}

}
