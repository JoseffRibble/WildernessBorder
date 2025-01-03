package me.joseffribble.wildernessborder.commands;

import java.util.ArrayList;
import java.util.List;
import me.joseffribble.wildernessborder.WildernessBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.scheduler.BukkitRunnable;


/**
 * Command executor and tab completer for the WildernessBorder plugin.
 */
public class WildernessBorderCommand implements CommandExecutor, TabCompleter {
  private final WildernessBorder plugin;

  /**
   * Constructor for WildernessBorderCommand.
   *
   * @param plugin the instance of the WildernessBorder plugin
   */
  public WildernessBorderCommand(WildernessBorder plugin) {
    this.plugin = plugin;
  }

  /**
   * Creates a new instance of WildernessBorderCommand.
   *
   * @param plugin the plugin instance
   * @return the command executor
   */
  public static WildernessBorderCommand create(WildernessBorder plugin) {
    return new WildernessBorderCommand(plugin);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission("wildernessborder.admin")) {
      sender.sendMessage("§cYou don't have permission to use this command.");
      return true;
    }

    if (args.length < 1) {
      return false;
    }

    switch (args[0].toLowerCase()) {
      case "radius":
        if (args.length == 1) {
          sender.sendMessage(String.format("§aCurrent protected radius: %d blocks", 
              plugin.getProtectedRadius()));
          return true;
        }
        if (args.length != 2) {
          return false;
        }
        try {
          int requestedRadius = Integer.parseInt(args[1]);
          int alignedRadius = ((Math.abs(requestedRadius) + 511) / 512) * 512;
          plugin.setProtectedRadius(alignedRadius);
          sender.sendMessage(String.format("§aProtected radius set to %d blocks (aligned from %d)", 
              alignedRadius, requestedRadius));
        } catch (NumberFormatException e) {
          sender.sendMessage("§cInvalid number format");
        }
        return true;

      case "reload":
        plugin.reloadConfig();
        plugin.loadConfig();
        sender.sendMessage("§aConfiguration reloaded");
        return true;

      case "regen":
        if (!sender.hasPermission("wildernessborder.regen")) {
          sender.sendMessage("§cYou don't have permission to regenerate the wilderness.");
          return true;
        }
        sender.sendMessage("§aForcing deletion of regions outside protected radius...");

        new BukkitRunnable() {
          @Override
          public void run() {
            plugin.cleanupRegions(true);
            sender.sendMessage("§aFinished forced cleanup of wilderness regions.");
          }
        }.runTaskAsynchronously(plugin);

        return true;

      default:
        return false;
    }
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, 
                                    String alias, String[] args) {
    List<String> completions = new ArrayList<>();
    
    if (args.length == 1) {
      completions.add("radius");
      completions.add("reload");
      completions.add("regen");
    }
    
    return completions;
  }
}