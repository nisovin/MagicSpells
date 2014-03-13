package com.nisovin.magicspells.caster;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellReagents;

public class PlayerCaster extends EntityCaster {

	private Player player;
	
	public PlayerCaster(Player player) {
		this.player = player;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	@Override
	public Entity getEntity() {
		return player;
	}
	
	@Override
	public String getName() {
		return player.getName();
	}

	@Override
	public String getDisplayName() {
		return player.getDisplayName();
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}
	
	@Override
	public void sendMessage(String message) {
		if (message != null && !message.equals("")) {
			String [] msgs = message.replaceAll("&([0-9a-fk-or])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					//player.sendMessage(MagicSpells.textColor + msg);
				}
			}
		}
	}

	@Override
	public boolean hasPermission(String perm) {
		return player.hasPermission(perm);
	}

	@Override
	public boolean hasReagents(SpellReagents reagents) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeReagents(SpellReagents reagents) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean canCast(Spell spell) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void giveExp(int exp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isValid() {
		// TODO Auto-generated method stub
		return false;
	}
	
	
	
}
