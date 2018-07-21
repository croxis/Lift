/*
 * This file is part of Lift.
 *
 * Copyright (c) ${project.inceptionYear}-2013, croxis <https://github.com/croxis/>
 *
 * Lift is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Lift is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Lift. If not, see <http://www.gnu.org/licenses/>.
 */
package net.croxis.plugins.lift;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;

public class BukkitConfig extends Config{
	public static HashMap<Material, Double> blockSpeeds = new HashMap<>();
	public static HashSet<Material> floorMaterials = new HashSet<>();
	static boolean useNoCheatPlus = false;
	static boolean metricbool = true;
	static boolean serverFlight;

    public void loadConfig(BukkitLift plugin){
        plugin.getConfig().options().copyDefaults(true);
        liftArea = plugin.getConfig().getInt("maxLiftArea");
        BukkitConfig.maxHeight = plugin.getConfig().getInt("maxHeight");
        BukkitConfig.debug = plugin.getConfig().getBoolean("debug");
        BukkitConfig.liftMobs = plugin.getConfig().getBoolean("liftMobs");
        BukkitConfig.autoPlace = plugin.getConfig().getBoolean("autoPlace");
        BukkitConfig.checkFloor = plugin.getConfig().getBoolean("checkFloor", false);
        BukkitConfig.preventEntry = plugin.getConfig().getBoolean("preventEntry", false);
        BukkitConfig.preventLeave = plugin.getConfig().getBoolean("preventLeave", false);
        BukkitConfig.redstone = plugin.getConfig().getBoolean("redstone", false);
        Set<String> baseBlockKeys = plugin.getConfig().getConfigurationSection("baseBlockSpeeds").getKeys(false);
        for (String key : baseBlockKeys){
            BukkitConfig.blockSpeeds.put(Material.valueOf(key), plugin.getConfig().getDouble("baseBlockSpeeds." + key));
        }
        List<String> configFloorMaterials = plugin.getConfig().getStringList("floorBlocks");
        for (String key : configFloorMaterials){
            BukkitConfig.floorMaterials.add(Material.valueOf(key));
        }
        BukkitConfig.stringOneFloor = plugin.getConfig().getString("STRING_oneFloor", "There is only one floor silly.");
        BukkitConfig.stringCurrentFloor = plugin.getConfig().getString("STRING_currentFloor", "Current Floor:");
        BukkitConfig.stringDestination = plugin.getConfig().getString("STRING_dest", "Dest:");
        BukkitConfig.stringCantEnter = plugin.getConfig().getString("STRING_cantEnter", "Can't enter elevator in use");
        BukkitConfig.stringCantLeave = plugin.getConfig().getString("STRING_cantLeave", "Can't leave elevator in use");

        BukkitConfig.metricbool = plugin.getConfig().getBoolean("metrics", true);
        plugin.saveConfig();

        BukkitConfig.serverFlight = plugin.getServer().getAllowFlight();

        if (BukkitConfig.preventEntry){
            Bukkit.getServer().getPluginManager().registerEvents(plugin, plugin);
        }

        if(plugin.getServer().getPluginManager().getPlugin("NoCheatPlus") != null){
            BukkitConfig.useNoCheatPlus = true;
            plugin.logDebug("Hooked into NoCheatPlus");
        }
    }
}
