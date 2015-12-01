package com.nisovin.magicspells.spells.passive;

import java.util.HashMap;
import java.util.Map;

import com.nisovin.magicspells.MagicSpells;

public class PassiveTrigger {
	
	private static Map<String, PassiveTrigger> map = new HashMap<String, PassiveTrigger>();
	
	public static PassiveTrigger TAKE_DAMAGE = addTrigger("takedamage", TakeDamageListener.class);
	public static PassiveTrigger GIVE_DAMAGE = addTrigger("givedamage", GiveDamageListener.class);
	public static PassiveTrigger FATAL_DAMAGE = addTrigger("fataldamage", FatalDamageListener.class);
	public static PassiveTrigger KILL = addTrigger("kill", KillListener.class);
	public static PassiveTrigger DEATH = addTrigger("death", DeathListener.class);
	public static PassiveTrigger RESPAWN = addTrigger("respawn", RespawnListener.class);
	public static PassiveTrigger JOIN = addTrigger("join", JoinListener.class);
	public static PassiveTrigger QUIT = addTrigger("quit", QuitListener.class);
	public static PassiveTrigger BLOCK_BREAK = addTrigger("blockbreak", BlockBreakListener.class);
	public static PassiveTrigger BLOCK_PLACE = addTrigger("blockplace", BlockPlaceListener.class);
	public static PassiveTrigger RIGHT_CLICK = addTrigger("rightclick", RightClickItemListener.class);
	public static PassiveTrigger RIGHT_CLICK_BLOCK_TYPE = addTrigger("rightclickblocktype", RightClickBlockTypeListener.class);
	public static PassiveTrigger RIGHT_CLICK_BLOCK_COORD = addTrigger("rightclickblockcoord", RightClickBlockCoordListener.class);
	public static PassiveTrigger LEFT_CLICK_BLOCK_TYPE = addTrigger("leftclickblocktype", LeftClickBlockTypeListener.class);
	public static PassiveTrigger LEFT_CLICK_BLOCK_COORD = addTrigger("leftclickblockcoord", LeftClickBlockCoordListener.class);
	public static PassiveTrigger RIGHT_CLICK_ENTITY = addTrigger("rightclickentity", RightClickEntityListener.class);
	public static PassiveTrigger SPELL_CAST = addTrigger("spellcast", SpellCastListener.class);
	public static PassiveTrigger SPELL_CASTED = addTrigger("spellcasted", SpellCastedListener.class);
	public static PassiveTrigger SPELL_TARGET = addTrigger("spelltarget", SpellTargetListener.class);
	public static PassiveTrigger SPELL_TARGETED = addTrigger("spelltargeted", SpellTargetedListener.class);
	public static PassiveTrigger SPRINT = addTrigger("sprint", SprintListener.class);
	public static PassiveTrigger STOP_SPRINT = addTrigger("stopsprint", SprintListener.class);
	public static PassiveTrigger SNEAK = addTrigger("sneak", SneakListener.class);
	public static PassiveTrigger STOP_SNEAK = addTrigger("stopsneak", SneakListener.class);
	public static PassiveTrigger FLY = addTrigger("fly", FlyListener.class);
	public static PassiveTrigger STOP_FLY = addTrigger("stopfly", FlyListener.class);
	public static PassiveTrigger HOT_BAR_SELECT = addTrigger("hotbarselect", HotBarListener.class);
	public static PassiveTrigger HOT_BAR_DESELECT = addTrigger("hotbardeselect", HotBarListener.class);
	public static PassiveTrigger DROP_ITEM = addTrigger("dropitem", DropItemListener.class);
	public static PassiveTrigger PICKUP_ITEM = addTrigger("pickupitem", PickupItemListener.class);
	public static PassiveTrigger CRAFT = addTrigger("craft", CraftListener.class);
	public static PassiveTrigger FISH = addTrigger("fish", FishListener.class);
	public static PassiveTrigger SHOOT = addTrigger("shoot", ShootListener.class);
	public static PassiveTrigger TELEPORT = addTrigger("teleport", TeleportListener.class);
	public static PassiveTrigger BUFF = addTrigger("buff", BuffListener.class);
	public static PassiveTrigger TICKS = addTrigger("ticks", TicksListener.class);
	public static PassiveTrigger RESOURCE_PACK = addTrigger("resourcepack", ResourcePackListener.class);
		
	public static PassiveTrigger addTrigger(String name, Class<? extends PassiveListener> listener) {
		PassiveTrigger trigger = new PassiveTrigger(name, listener);
		map.put(trigger.getName(), trigger);
		return trigger;
	}
	
	public static PassiveTrigger getByName(String name) {
		return map.get(name);
	}
	
	String name;
	Class<? extends PassiveListener> listenerClass;
	PassiveListener listener;
	
	PassiveTrigger(String name, Class<? extends PassiveListener> listener) {
		this.name = name;
		this.listenerClass = listener;
	}
	
	public String getName() {
		return name;
	}
	
	public PassiveListener getListener() {
		if (listener == null) {
			try {
				listener = listenerClass.newInstance();
				MagicSpells.registerEvents(listener);
			} catch (Exception e) {
				MagicSpells.handleException(e);
			}
		}
		return listener;
	}
	
}
