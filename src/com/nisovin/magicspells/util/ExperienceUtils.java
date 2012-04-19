package com.nisovin.magicspells.util;

import java.util.Arrays;

import org.bukkit.entity.Player;

/**
 * Original credit to desht
 *
 */
public class ExperienceUtils {
	public static final int MAX_LEVEL_SUPPORTED = 200;

	private static final int xpRequiredForNextLevel[] = new int[MAX_LEVEL_SUPPORTED];
	private static final int xpTotalToReachLevel[] = new int[MAX_LEVEL_SUPPORTED];

	// Initialise the xp lookup table.  Basing this on observations noted in https://bukkit.atlassian.net/browse/BUKKIT-47
	// 7 xp to get to level 1, 17 to level 2, 31 to level 3...
	// At each level, the increment to get to the next level increases alternately by 3 and 4
	static {
		xpTotalToReachLevel[0] = 0;
		int incr = 7;
		for (int i = 1; i < xpTotalToReachLevel.length; i++) {
			xpRequiredForNextLevel[i - 1] = incr;
			xpTotalToReachLevel[i] = xpTotalToReachLevel[i - 1] + incr;
			incr += (i % 2 == 0) ? 4 : 3;
		}
	}
	
	public static void changeExp(Player player, int amt) {
		int xp = getCurrentExp(player) + amt;
		if (xp < 0) xp = 0;
		
		int newLvl = getLevelFromExp(xp);
		if (newLvl >= MAX_LEVEL_SUPPORTED) {
			return;
		} else if (player.getLevel() != newLvl) {
			player.setLevel(newLvl);
		}
		
		float pct = ((float)(xp - xpTotalToReachLevel[newLvl]) / (float)xpRequiredForNextLevel[newLvl]);
		player.setExp(pct);
	}
	
	public static int getCurrentExp(Player player) {
		int lvl = player.getLevel();
		return xpTotalToReachLevel[lvl] + (int) (xpRequiredForNextLevel[lvl] * player.getExp());
	}
	
	public static boolean hasExp(Player player, int amt) {
		if (player.getLevel() >= MAX_LEVEL_SUPPORTED) return false;
		return getCurrentExp(player) >= amt;
	}
	
	public static int getLevelFromExp(int exp) {
		if (exp <= 0) return 0;
		int pos = Arrays.binarySearch(xpTotalToReachLevel, exp);
		return pos < 0 ? -pos - 2 : pos;
	}
}