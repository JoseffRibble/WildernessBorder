package me.joseffribble.wildernessborder;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import me.joseffribble.wildernessborder.commands.WildernessBorderCommand;
import me.joseffribble.wildernessborder.listeners.PlayerListener;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;


/**
 * Main class for the WildernessBorder plugin.
 * This class handles the initialization and configuration of the plugin,
 * as well as the cleanup of regions outside the protected radius.
 */
public class WildernessBorder extends JavaPlugin implements Listener {
  private int protectedRadius;
  @SuppressWarnings("unused")
  private int regenerationInterval;
  private int deletionDelay;
  private int playerCheckRadius;
  private Map<String, Long> regionLastVisited;
  private BorderVisualization borderVis;

  @Override
  public void onEnable() {
    saveDefaultConfig();
    loadConfig();
    
    regionLastVisited = new HashMap<>();
    getServer().getPluginManager().registerEvents(this, this);
    borderVis = new BorderVisualization(this);
    
    PluginCommand command = getCommand("wildernessborder");
    if (command != null) {
      WildernessBorderCommand commandExecutor = new WildernessBorderCommand(this);
      command.setExecutor(commandExecutor);
      command.setTabCompleter(commandExecutor);
    }
    
    PlayerListener playerListener = new PlayerListener(this, borderVis);
    getServer().getPluginManager().registerEvents(playerListener, this);
    getLogger().info("Registered PlayerListener");
    
    // Schedule automatic region checks every 5 minutes; not forced
    new BukkitRunnable() {
      @Override
      public void run() {
        cleanupRegions(false);
      }
    }.runTaskTimer(this, 20L * 300, 20L * 300);
    
    getLogger().info("WildernessBorder enabled!");
  }

  /**
   * Loads the configuration settings from the config file.
   */
  public void loadConfig() {
    FileConfiguration config = getConfig();
    
    // Get and align radius to region borders
    int rawRadius = config.getInt("protected-radius", 5000);
    int absRadius = Math.abs(rawRadius);
    protectedRadius = ((absRadius + 511) / 512) * 512; // Align to region size
    
    if (rawRadius != protectedRadius) {
      getLogger().info("Aligning radius " + rawRadius + " to region border: " + protectedRadius);
      config.set("protected-radius", protectedRadius);
      saveConfig();
    }
    
    regenerationInterval = config.getInt("regeneration-interval-hours", 168);
    deletionDelay = config.getInt("deletion-delay-hours", 2) * 3600000;
    playerCheckRadius = config.getInt("player-check-radius", 128);
  }

  /**
   * Cleans up regions that are outside the protected radius. If force is true,
   * all matching regions are deleted regardless of the configured time limit.
   *
   * @param force whether to override the normal deletionDelay
   */
  public void cleanupRegions(boolean force) {
    ConfigurationSection worldsSection = getConfig().getConfigurationSection("worlds");
    if (worldsSection == null) {
      return;
    }

    for (String worldName : worldsSection.getKeys(false)) {
      if (!worldsSection.getBoolean(worldName + ".enabled", false)) {
        continue;
      }

      World world = getServer().getWorld(worldName);
      if (world == null) {
        continue;
      }

      File regionDir = new File(world.getWorldFolder(), "region");
      if (!regionDir.exists() || !regionDir.isDirectory()) {
        continue;
      }

      File[] regionFiles = regionDir.listFiles((dir, name) -> name.endsWith(".mca"));
      if (regionFiles == null) {
        continue;
      }

      for (File regionFile : regionFiles) {
        // Extract regionX, regionZ from file name
        String fileName = regionFile.getName(); // e.g. "r.2.-1.mca"
        if (!fileName.startsWith("r.") || !fileName.endsWith(".mca")) {
          continue;
        }

        String[] coords = fileName.substring(2, fileName.length() - 4).split("\\.");
        try {
          int regionX = Integer.parseInt(coords[0]);
          int regionZ = Integer.parseInt(coords[1]);

          // Check for forced cleanup or if region is older than configured delay
          String regionKey = String.format("%s:%d:%d", world.getName(), regionX, regionZ);
          Long lastVisit = regionLastVisited.get(regionKey);
          boolean outdated = (lastVisit != null 
                              && (System.currentTimeMillis() - lastVisit) >= deletionDelay);

          if (isRegionOutsideRadius(regionX, regionZ, protectedRadius) 
              && !playersNearby(world, regionX, regionZ, playerCheckRadius)
              && (force || outdated)) {
            // Unload chunks in region
            for (int x = 0; x < 32; x++) {
              for (int z = 0; z < 32; z++) {
                int chunkX = (regionX * 32) + x;
                int chunkZ = (regionZ * 32) + z;
                if (world.isChunkLoaded(chunkX, chunkZ)) {
                  world.unloadChunk(chunkX, chunkZ, false);
                }
              }
            }
            // Delete region file
            if (regionFile.delete()) {
              getLogger().info("Deleted region file at "
                  + regionX + "," + regionZ + " in " + world.getName());
              regionLastVisited.remove(regionKey);
            }
          }
        } catch (NumberFormatException ignored) {
          getLogger().warning("Invalid region file name: " + fileName);
        }
      }
    }
  }

  /**
   * Checks if there are any players within the given distance of the region.
   *
   * @param world the world
   * @param regionX region X
   * @param regionZ region Z
   * @param distance max distance from region center
   * @return true if players are found
   */
  private boolean playersNearby(World world, int regionX, int regionZ, int distance) {
    int regionBlockX = regionX * 512;
    int regionBlockZ = regionZ * 512;
    for (Player player : world.getPlayers()) {
      Location loc = player.getLocation();
      // Check bounding box
      if (Math.abs(loc.getBlockX() - regionBlockX) <= distance 
          && Math.abs(loc.getBlockZ() - regionBlockZ) <= distance) {
        return true;
      }
    }
    return false;
  }

  /**
   * Updates the timestamp for the last visit to a region.
   *
   * @param regionKey the key representing the region
   */
  public void updateRegionTimestamp(String regionKey) {
    regionLastVisited.put(regionKey, System.currentTimeMillis());
  }

  /**
   * Checks if a region is outside the protected radius.
   *
   * @param regionX the X coordinate of the region
   * @param regionZ the Z coordinate of the region
   * @param radius the protected radius
   * @return true if the region is outside the radius, false otherwise
   */
  public boolean isRegionOutsideRadius(int regionX, int regionZ, int radius) {
    int minBlockX = regionX * 512;
    int minBlockZ = regionZ * 512;
    int maxBlockX = minBlockX + 511;
    int maxBlockZ = minBlockZ + 511;
    
    return (Math.abs(minBlockX) > radius && Math.abs(maxBlockX) > radius)
           || (Math.abs(minBlockZ) > radius && Math.abs(maxBlockZ) > radius);
  }

  /**
   * Handles the ChunkLoadEvent to update the timestamp for the last visit to a region.
   *
   * @param event the ChunkLoadEvent
   */
  @EventHandler
  public void onChunkLoad(ChunkLoadEvent event) {
    int regionX = event.getChunk().getX() >> 5;
    int regionZ = event.getChunk().getZ() >> 5;
    String regionKey = String.format("%s:%d:%d", 
        event.getWorld().getName(), regionX, regionZ);
    regionLastVisited.put(regionKey, System.currentTimeMillis());
  }

  public int getProtectedRadius() {
    return protectedRadius;
  }

  /**
   * Sets the protected radius and aligns it to the region size.
   *
   * @param radius the new protected radius
   */
  public void setProtectedRadius(int radius) {
    int absRadius = Math.abs(radius);
    this.protectedRadius = ((absRadius + 511) / 512) * 512;
    getConfig().set("protected-radius", this.protectedRadius);
    saveConfig();
  }

  public BorderVisualization getBorderVisualization() {
    return borderVis;
  }
}