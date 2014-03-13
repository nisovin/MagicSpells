package com.nisovin.magicspells.spells.passive;

import java.util.HashMap;
import java.util.Map;

public enum PassiveTrigger {
	
	TAKE_DAMAGE("takedamage", TakeDamageListener.class),
	GIVE_DAMAGE("givedamage", GiveDamageListener.class),
	KILL("kill", KillListener.class),
	DEATH("death", DeathListener.class),
	RESPAWN("respawn", RespawnListener.class),
	JOIN("join", JoinListener.class),
	QUIT("quit", QuitListener.class),
	BLOCK_BREAK("blockbreak", BlockBreakListener.class),
	BLOCK_PLACE("blockplace", BlockPlaceListener.class),
	RIGHT_CLICK("rightclick", RightClickItemListener.class),
	RIGHT_CLICK_BLOCK_TYPE("rightclickblocktype", RightClickBlockTypeListener.class),
	RIGHT_CLICK_BLOCK_COORD("rightclickblockcoord", RightClickBlockCoordListener.class),
	RIGHT_CLICK_ENTITY("rightclickentity", RightClickEntityListener.class),
	SPELL_CAST("spellcast", SpellCastListener.class),
	SPELL_CASTED("spellcasted", SpellCastedListener.class),
	SPELL_TARGET("spelltarget", SpellTargetListener.class),
	SPELL_TARGETED("spelltargeted", SpellTargetedListener.class),
	SPRINT("sprint", SprintListener.class),
	STOP_SPRINT("stopsprint", SprintListener.class),
	SNEAK("sneak", SneakListener.class),
	STOP_SNEAK("stopsneak", SneakListener.class),
	FLY("fly", FlyListener.class),
	STOP_FLY("stopfly", FlyListener.class),
	HOT_BAR_SELECT("hotbarselect", HotBarListener.class),
	HOT_BAR_DESELECT("hotbardeselect", HotBarListener.class),
	DROP_ITEM("dropitem", DropItemListener.class),
	PICKUP_ITEM("pickupitem", PickupItemListener.class),
	FISH("fish", FishListener.class),
	SHOOT("shoot", ShootListener.class),
	TELEPORT("teleport", TeleportListener.class),
	BUFF("buff", BuffListener.class),
	TICKS("ticks", TicksListener.class);
	
	static Map<String, PassiveTrigger> map = new HashMap<String, PassiveTrigger>();
	static {
		for (PassiveTrigger trigger : values()) {
			map.put(trigger.getName(), trigger);
		}
	}
	
	public static PassiveTrigger getByName(String name) {
		return map.get(name);
	}
	
	String name;
	Class<? extends PassiveListener> listener;
	
	PassiveTrigger(String name, Class<? extends PassiveListener> listener) {
		this.name = name;
		this.listener = listener;
	}
	
	public String getName() {
		return name;
	}
	
	public PassiveListener getNewListener() {
		try {
			return listener.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}
