package com.nisovin.magicspells.spells.passive;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.wrappers.EnumWrappers.ResourcePackStatus;
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
			listener = new PacketAdapter(MagicSpells.plugin, PacketType.Play.Client.RESOURCE_PACK_STATUS) {
				@Override
				public void onPacketReceiving(PacketEvent event) {
					Player player = event.getPlayer();
					ResourcePackStatus status = event.getPacket().getResourcePackStatus().read(0);
					if (status == ResourcePackStatus.SUCCESSFULLY_LOADED) {
						activate(player, spellsLoaded);
					} else if (status == ResourcePackStatus.DECLINED) {
						activate(player, spellsDeclined);
					} else if (status == ResourcePackStatus.FAILED_DOWNLOAD) {
						activate(player, spellsFailed);
					} else if (status == ResourcePackStatus.ACCEPTED) {
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

}
