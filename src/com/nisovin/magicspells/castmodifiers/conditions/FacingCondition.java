package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class FacingCondition extends Condition {

	String direction;
	
	@Override
	public boolean setVar(String var) {
		direction = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		return getDirection(player.getLocation()).equals(direction);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		return getDirection(target.getLocation()).equals(direction);
	}

	@Override
	public boolean check(Player player, Location location) {
		return getDirection(location).equals(direction);
	}
	
	public String getDirection(Location loc) {
        float y = loc.getYaw();
        if( y < 0 ){y += 360;}
        y %= 360;
        if (y <= 45 || y >= 315) {
        	return "south";
        } else if (y >= 45 && y <= 135) {
        	return "west";
        } else if (y >= 135 && y <= 225) {
        	return "north";
        } else {
        	return "east";
        }
   }

}
