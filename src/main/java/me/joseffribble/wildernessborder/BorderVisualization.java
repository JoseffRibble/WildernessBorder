package me.joseffribble.wildernessborder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * This class is responsible for visualizing the border using particles
 * and updating player information about the border.
 */
public class BorderVisualization {
  private final WildernessBorder plugin;
  private final Map<UUID, Long> lastActionBarDisplay;
  private boolean debug;

  /**
   * Constructs a new BorderVisualization instance.
   *
   * @param plugin the WildernessBorder plugin instance
   */
  public BorderVisualization(WildernessBorder plugin) {
    this.plugin = plugin;
    this.lastActionBarDisplay = new HashMap<>();
    this.debug = plugin.getConfig().getBoolean("debug", false);
  }

  /**
   * Creates a new BorderVisualization instance.
   *
   * @param plugin the WildernessBorder plugin instance
   * @return a new BorderVisualization instance
   */
  public static BorderVisualization create(WildernessBorder plugin) {
    return new BorderVisualization(plugin);
  }
    
  /**
   * Updates the player's action bar with information about the border.
   *
   * @param player the player whose border information is to be updated
   */
  public void updatePlayerBorderInfo(Player player) {
    Location loc = player.getLocation();
    int radius = plugin.getProtectedRadius();
    int x = Math.abs(loc.getBlockX());
    int z = Math.abs(loc.getBlockZ());
    
    if (debug) {
      plugin.getLogger().info(String.format("[DEBUG] Border check - Player: %s", player.getName()));
      plugin.getLogger().info(String.format("[DEBUG] Position: x=%d, z=%d (radius=%d)", 
                                            x, z, radius));
      plugin.getLogger().info(String.format("[DEBUG] Distance to border: x=%d, z=%d", 
          Math.abs(radius - x), Math.abs(radius - z)));
    }
    
    long now = System.currentTimeMillis();
    UUID playerId = player.getUniqueId();
    Long lastDisplay = lastActionBarDisplay.get(playerId);
    
    try {
      // Outside border
      if (x > radius || z > radius) {
        player.sendActionBar(
            Component.text("⚠ Wilderness Zone ⚠")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
        );
        lastActionBarDisplay.put(playerId, now);
        
        // Sound effect only when crossing border
        if (lastDisplay == null || now - lastDisplay > 1000) {
          player.playSound(loc, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 0.5f);
        }
      } 
    } catch (Exception e) {
      plugin.getLogger().warning("Border visualization error for " 
                                + player.getName() + ": " + e.getMessage());
    }
  }
}