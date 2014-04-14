package com.nisovin.magicspells.volatilecode;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.targeted.DisguiseSpell.Disguise;
import com.nisovin.magicspells.util.DisguiseManager;
import com.nisovin.magicspells.util.MagicConfig;

public class DisguiseManagerEmpty extends DisguiseManager {

	public DisguiseManagerEmpty(MagicConfig config) {
		super(config);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void cleanup() {
	}

	@Override
	protected void sendDestroyEntityPackets(Player disguised) {
	}

	@Override
	protected void sendDestroyEntityPackets(Player disguised, int entityId) {
	}

	@Override
	protected void sendDisguisedSpawnPackets(Player disguised, Disguise disguise) {
	}

	@Override
	protected void sendPlayerSpawnPackets(Player player) {
	}

}
