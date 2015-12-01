package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MessageBlocker;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.ValidTargetList;

public class ExternalCommandSpell extends TargetedSpell implements TargetedEntitySpell {
	
	private static MessageBlocker messageBlocker;
	
	private boolean castWithItem;
	private boolean castByCommand;
	private List<String> commandToExecute;
	private List<String> commandToExecuteLater;
	private int commandDelay;
	private List<String> commandToBlock;
	private List<String> temporaryPermissions;
	private boolean temporaryOp;
	private boolean requirePlayerTarget;
	private boolean blockChatOutput;
	private boolean executeAsTargetInstead;
	private boolean executeOnConsoleInstead;
	private String strCantUseCommand;
	private String strNoTarget;
	private String strBlockedOutput;
	
	private ConversationFactory convoFac;
	private Prompt convoPrompt;

	public ExternalCommandSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		commandToExecute = getConfigStringList("command-to-execute", null);
		commandToExecuteLater = getConfigStringList("command-to-execute-later", null);
		commandDelay = getConfigInt("command-delay", 0);
		commandToBlock = getConfigStringList("command-to-block", null);
		temporaryPermissions = getConfigStringList("temporary-permissions", null);
		temporaryOp = getConfigBoolean("temporary-op", false);
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		blockChatOutput = getConfigBoolean("block-chat-output", false);
		executeAsTargetInstead = getConfigBoolean("execute-as-target-instead", false);
		executeOnConsoleInstead = getConfigBoolean("execute-on-console-instead", false);
		strCantUseCommand = getConfigString("str-cant-use-command", "&4You don't have permission to do that.");
		strNoTarget = getConfigString("str-no-target", "No target found.");
		strBlockedOutput = getConfigString("str-blocked-output", "");
		
		if (requirePlayerTarget) {
			validTargetList = new ValidTargetList(true, false);
		}
		
		if (blockChatOutput) {
			if (Bukkit.getPluginManager().isPluginEnabled("ProtocolLib")) {
				if (messageBlocker == null) {
					messageBlocker = new MessageBlocker();
				}
			} else {
				convoPrompt = new StringPrompt() {	
					public String getPromptText(ConversationContext arg0) {
						return strBlockedOutput;
					}
					public Prompt acceptInput(ConversationContext arg0, String arg1) {
						return Prompt.END_OF_CONVERSATION;
					}
				};
				convoFac = new ConversationFactory(MagicSpells.plugin).withModality(true).withFirstPrompt(convoPrompt).withTimeout(1);
			}
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target if necessary
			Player target = null;
			if (requirePlayerTarget) {
				TargetInfo<Player> targetInfo = getTargetedPlayer(player, power);
				if (targetInfo == null) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					target = targetInfo.getTarget();
				}
			}
			process(player, target, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void process(CommandSender sender, Player target, String[] args) {
		// get actual sender
		CommandSender actualSender;
		if (executeAsTargetInstead) {
			actualSender = target;
		} else if (executeOnConsoleInstead) {
			actualSender = Bukkit.getConsoleSender();
		} else {
			actualSender = sender;
		}
		if (actualSender == null) {
			return;
		}
		
		// grant permissions and op
		boolean opped = false;
		if (actualSender instanceof Player) {
			if (temporaryPermissions != null) {
				for (String perm : temporaryPermissions) {
					if (!actualSender.hasPermission(perm)) {
						actualSender.addAttachment(MagicSpells.plugin, perm.trim(), true, 5);
					}
				}
			}
			if (temporaryOp && !actualSender.isOp()) {
				opped = true;
				actualSender.setOp(true);
			}
		}
		
		// perform commands
		try {
			if (commandToExecute != null && commandToExecute.size() > 0) {

				Conversation convo = null;
				if (sender != null && sender instanceof Player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer((Player)sender);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation((Player)sender);
						convo.begin();
					}
				}
				
				int delay = 0;				
				for (String comm : commandToExecute) {
					if (comm != null && !comm.isEmpty()) {
						if (args != null && args.length > 0) {
							for (int i = 0; i < args.length; i++) {
								comm = comm.replace("%"+(i+1), args[i]);
							}
						}
						if (sender != null) {
							comm = comm.replace("%a", sender.getName());
						}
						if (target != null) {
							comm = comm.replace("%t", target.getName());
						}
						if (comm.startsWith("DELAY ")) {
							String[] split = comm.split(" ");
							delay += Integer.parseInt(split[1]);
						} else if (delay > 0) {
							final CommandSender s = actualSender;
							final String c = comm;
							MagicSpells.scheduleDelayedTask(new Runnable() {
								public void run() {
									Bukkit.dispatchCommand(s, c);
								}
							}, delay);
						} else {
							Bukkit.dispatchCommand(actualSender, comm);
						}
					}
				}
				if (blockChatOutput && messageBlocker != null && sender != null && sender instanceof Player) {
					messageBlocker.removePlayer((Player)sender);
				} else if (convo != null) {
					convo.abandon();
				}
			}
		} catch (Exception e) {
			// catch all exceptions to make sure we don't leave someone opped
			e.printStackTrace();
		}
		
		// deop
		if (opped) {
			actualSender.setOp(false);
		}
		
		// effects
		if (sender != null && sender instanceof Player) {
			if (target != null) {
				playSpellEffects((Player)sender, target);
			} else {
				playSpellEffects(EffectPosition.CASTER, (Player)sender);
			}
		} else if (sender != null && sender instanceof BlockCommandSender) {
			playSpellEffects(EffectPosition.CASTER, ((BlockCommandSender)sender).getBlock().getLocation());
		}
		// add delayed command
		if (commandToExecuteLater != null && commandToExecuteLater.size() > 0 && !commandToExecuteLater.get(0).isEmpty()) {
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedCommand(sender, target), commandDelay);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player) {
			process(caster, (Player)target, null);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player) {
			process(null, (Player)target, null);
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (!requirePlayerTarget) {
			process(sender, null, args);
			return true;
		} else {
			return false;
		}
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.getPlayer().isOp() && commandToBlock != null && commandToBlock.size() > 0) {
			String msg = event.getMessage();
			for (String comm : commandToBlock) {
				comm = comm.trim();
				if (!comm.equals("") && msg.startsWith("/" + commandToBlock)) {
					event.setCancelled(true);
					sendMessage(event.getPlayer(), strCantUseCommand);
					return;
				}
			}
		}
	}
	
	public boolean requiresPlayerTarget() {
		return requirePlayerTarget;
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	@Override
	public void turnOff() {
		if (messageBlocker != null) {
			messageBlocker.turnOff();
			messageBlocker = null;
		}
	}
	
	private class DelayedCommand implements Runnable {

		private CommandSender sender;
		private Player target;
		
		public DelayedCommand(CommandSender sender, Player target) {
			this.sender = sender;
			this.target = target;
		}
		
		@Override
		public void run() {
			// get actual sender
			CommandSender actualSender;
			if (executeAsTargetInstead) {
				actualSender = target;
			} else if (executeOnConsoleInstead) {
				actualSender = Bukkit.getConsoleSender();
			} else {
				actualSender = sender;
			}
			if (actualSender == null) {
				return;
			}
			
			// grant permissions
			boolean opped = false;
			if (actualSender instanceof Player) {
				if (temporaryPermissions != null) {
					for (String perm : temporaryPermissions) {
						if (!actualSender.hasPermission(perm)) {
							actualSender.addAttachment(MagicSpells.plugin, perm, true, 5);
						}
					}
				}
				if (temporaryOp && !actualSender.isOp()) {
					opped = true;
					actualSender.setOp(true);
				}
			}
			
			// run commands
			try {
				Conversation convo = null;
				if (sender != null && sender instanceof Player) {
					if (blockChatOutput && messageBlocker != null) {
						messageBlocker.addPlayer((Player)sender);
					} else if (convoFac != null) {
						convo = convoFac.buildConversation((Player)sender);
						convo.begin();
					}
				}
				for (String comm : commandToExecuteLater) {
					if (comm != null && !comm.isEmpty()) {
						if (sender != null) {
							comm = comm.replace("%a", sender.getName());
						}
						if (target != null) {
							comm = comm.replace("%t", target.getName());
						}
						Bukkit.dispatchCommand(actualSender, comm);
					}
				}
				if (blockChatOutput && messageBlocker != null && sender != null && sender instanceof Player) {
					messageBlocker.removePlayer((Player)sender);
				} else if (convo != null) {
					convo.abandon();
				}
			} catch (Exception e) {
				// catch exceptions to make sure we don't leave someone opped
				e.printStackTrace();
			}
			
			// deop
			if (opped) {
				actualSender.setOp(false);
			}
			
			// graphical effect
			if (sender != null) {
				if (sender instanceof Player) {
					playSpellEffects(EffectPosition.DISABLED, (Player)sender);
				} else if (sender instanceof BlockCommandSender) {
					playSpellEffects(EffectPosition.DISABLED, ((BlockCommandSender)sender).getBlock().getLocation());
				}
			}
		}
		
	}

}
