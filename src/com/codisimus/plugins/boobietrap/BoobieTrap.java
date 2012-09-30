package com.codisimus.plugins.boobietrap;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author Codisimus
 */
public class BoobieTrap extends JavaPlugin implements Listener {
    private static BukkitScheduler scheduler;
    private static Logger logger;
    private static HashMap<Location, Integer> explosives = new HashMap<Location, Integer>();
    private Properties p;
    private int delay = 5;
    private boolean noLandDamage = false;

    @Override
    public void onEnable () {
        //Register Events
        this.getServer().getPluginManager().registerEvents(this, this);
        scheduler = this.getServer().getScheduler();
        logger = this.getLogger();
        loadSettings();

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
        }
        logger.info("BoobieTrap " + this.getDescription().getVersion()
                + " (Build " + version.getProperty("Build") + ") is enabled!");
    }

    /**
     * Loads settings from the config.properties file
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = this.getDataFolder();
            if (!file.isDirectory()) {
                file.mkdir();
            }
            file = new File(file.getPath() + "/config.properties");
            if (!file.exists()) {
                this.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            delay = Integer.parseInt(loadValue("Delay"));
            noLandDamage = Boolean.parseBoolean(loadValue("NoLandDamage"));
        } catch (Exception missingProp) {
            logger.severe("Failed to load BoobieTrap " + this.getDescription().getVersion());
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private String loadValue(String key) {
        if (!p.containsKey(key)) {
            logger.severe("Missing value for " + key + " in config file");
            logger.severe("Please regenerate config file");
        }

        return p.getProperty(key);
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        final Block block = event.getClickedBlock();
        if (block.getType() != Material.CHEST) {
            return;
        }

        final Inventory inv = ((Chest) block.getState()).getInventory();
        if (isExplosive(inv)) {
            final Location location = block.getLocation();
            if (explosives.containsKey(location)) {
                return;
            }

            explosives.put(location, scheduler.scheduleAsyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    inv.clear();
                    block.setTypeId(0);
                    block.getWorld().createExplosion(location.add(0, 2, 0), 6F, !noLandDamage);
                }
            }, delay * 20L));
        }
    }

    @EventHandler (ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!explosives.containsKey(location)) {
            return;
        }

        scheduler.cancelTask(explosives.get(location));
        explosives.remove(location);
    }
    
    @EventHandler (ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (noLandDamage) {
            Block explosive = event.getLocation().subtract(0, 2, 0).getBlock();
            if (explosives.containsKey(explosive.getLocation())) {
                List<Block> blockList = event.blockList();
                blockList.clear();
                blockList.add(explosive);
            }
        }
    }

    public boolean isExplosive(Inventory inv) {
        if (inv.firstEmpty() < 9) {
            return false;
        }

        return inv.getItem(0).getType() == Material.TNT
                && inv.getItem(1).getType() == Material.REDSTONE
                && inv.getItem(2).getType() == Material.TNT
                && inv.getItem(3).getType() == Material.REDSTONE
                && inv.getItem(4).getType() == Material.LEVER
                && inv.getItem(5).getType() == Material.REDSTONE
                && inv.getItem(6).getType() == Material.TNT
                && inv.getItem(7).getType() == Material.REDSTONE
                && inv.getItem(8).getType() == Material.TNT;
    }
}
