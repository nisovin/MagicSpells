package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import com.nisovin.magicspells.castmodifiers.Condition;

public class OnTeamCondition extends Condition {

	String teamName;
	
	@Override
	public boolean setVar(String var) {
		teamName = var;
		return true;
	}

	@Override
	public boolean check(Player player) {
		Team team = Bukkit.getScoreboardManager().getMainScoreboard().getPlayerTeam(player);
		return team != null && team.getName().equals(teamName);
	}

	@Override
	public boolean check(Player player, LivingEntity target) {
		if (target instanceof Player) {
			return check((Player)target);
		} else {
			return false;
		}
	}

	@Override
	public boolean check(Player player, Location location) {
		return false;
	}

}
