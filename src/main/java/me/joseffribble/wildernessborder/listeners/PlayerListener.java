package me.joseffribble.wildernessborder.listeners;

import me.joseffribble.wildernessborder.BorderVisualization;
import me.joseffribble.wildernessborder.WildernessBorder;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.ChunkLoadEvent;


/**
 * Listener for player-related events such as movement and chunk loading.
 */
public class PlayerListener implements Listener {
  private final WildernessBorder plugin;
  private final BorderVisualization borderVis;
  private final boolean debug;

  /**
   * Constructs a PlayerListener with the specified plugin and border visualization.
   *
   * @param plugin the WildernessBorder plugin instance
   * @param borderVis the BorderVisualization instance
   */
  public PlayerListener(WildernessBorder plugin, BorderVisualization borderVis) {
    this.plugin = plugin;
    this.borderVis = borderVis;
    this.debug = plugin.getConfig().getBoolean("debug", false);
  }

  /**
   * Handles the player move event.
   *
   * @param event the player move event
   */
  @EventHandler
  public void onPlayerMove(PlayerMoveEvent event) {
    if (debug) {
      plugin.getLogger().info("[DEBUG] PlayerMoveEvent triggered");
    }

    Player player = event.getPlayer();
    Location to = event.getTo();
    Location from = event.getFrom();
    
    if (to.getBlockX() == from.getBlockX()  
        && to.getBlockZ() == from.getBlockZ()) {
      if (debug) {
        plugin.getLogger().info("[DEBUG] Ignoring Y-axis only movement");
      }
      return;
    }
    
    if (debug) {
      plugin.getLogger().info(String.format("[DEBUG] Player %s moved from %d,%d to %d,%d", 
          player.getName(), 
          from.getBlockX(), from.getBlockZ(),
          to.getBlockX(), to.getBlockZ()));
    }
    
    borderVis.updatePlayerBorderInfo(player);
  }

  /**
   * Handles the chunk load event.
   *
   * @param event the chunk load event
   */
  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    Chunk chunk = event.getChunk();
    int regionX = chunk.getX() >> 5;
    int regionZ = chunk.getZ() >> 5;
    String regionKey = String.format("%s:%d:%d", 
        event.getWorld().getName(), regionX, regionZ);
    plugin.updateRegionTimestamp(regionKey);
  }
}