package com.codisimus.plugins.boobietrap;

import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Properties;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

/**
 * @author Codisimus
 */
public class BoobieTrap extends JavaPlugin implements Listener {
    static BukkitScheduler scheduler;
    static HashMap<Location, Integer> explosives = new HashMap<Location, Integer>();
    Properties p;
    int timer = 5;

    @Override
    public void onDisable () {
    }
    
    @Override
    public void onEnable () {
        //Register Events
        this.getServer().getPluginManager().registerEvents(this, this);
        scheduler = this.getServer().getScheduler();
        loadSettings();
        
        System.out.println("BoobieTrap "+this.getDescription().getVersion()+" is enabled!");
    }
    
    /**
     * Loads settings from the config.properties file
     * 
     */
    public void loadSettings() {
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = this.getDataFolder();
            if (!file.isDirectory())
                file.mkdir();
            file = new File(file.getPath()+"/config.properties");
            if (!file.exists())
                this.saveResource("config.properties", true);
            
            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);
            
            timer = Integer.parseInt(loadValue("Timer"));
        }
        catch (Exception missingProp) {
            System.err.println("Failed to load BoobieTrap "+this.getDescription().getVersion());
            missingProp.printStackTrace();
        }
        finally {
            try {
                fis.close();
            }
            catch (Exception e) {
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
        //Print an error if the key is not found
        if (!p.containsKey(key)) {
            System.err.println("[BoobieTrap] Missing value for "+key+" in config file");
            System.err.println("[BoobieTrap] Please regenerate config file");
        }
        
        return p.getProperty(key);
    }
    
    @EventHandler (ignoreCancelled=true, priority = EventPriority.MONITOR)
    public void onChestOpen(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        
        final Block block = event.getClickedBlock();
        if (block.getType() != Material.CHEST)
            return;
        
        final Inventory inv = ((Chest)block.getState()).getInventory();
        if (isExplosive(inv)) {
            final Location location = block.getLocation();
            if (explosives.containsKey(location))
                return;
            
            explosives.put(location, scheduler.scheduleAsyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    inv.clear();
                    block.getWorld().createExplosion(location.add(0, 2, 0), 6F, true);
                }
            }, timer * 20L));
        }
    }
    
    @EventHandler (ignoreCancelled=true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event) {
        Location location = event.getBlock().getLocation();
        if (!explosives.containsKey(location))
            return;
        
        scheduler.cancelTask(explosives.get(location));
        explosives.remove(location);
    }
    
    public boolean isExplosive(Inventory inv) {
        if (inv.firstEmpty() < 9)
            return false;
        
        return inv.getItem(0).getType() == Material.TNT && inv.getItem(1).getType() == Material.REDSTONE &&
                inv.getItem(2).getType() == Material.TNT && inv.getItem(3).getType() == Material.REDSTONE &&
                inv.getItem(4).getType() == Material.LEVER &&
                inv.getItem(5).getType() == Material.REDSTONE && inv.getItem(6).getType() == Material.TNT &&
                inv.getItem(7).getType() == Material.REDSTONE && inv.getItem(8).getType() == Material.TNT;
    }
}