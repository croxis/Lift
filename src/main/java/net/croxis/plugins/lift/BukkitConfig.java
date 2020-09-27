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
import org.bukkit.configuration.file.FileConfiguration;

public class BukkitConfig extends Config{
	public static HashMap<Material, Double> blockSpeeds = new HashMap<>();
	public static HashSet<Material> floorMaterials = new HashSet<>();
    public static HashSet<Material> buttonMaterials = new HashSet<>();
    public static HashSet<Material> signMaterials = new HashSet<>();
	static boolean useNoCheatPlus = false;
	static boolean metricbool = true;
	static boolean serverFlight;

    public void loadConfig(BukkitLift plugin){
        final FileConfiguration configuration = plugin.getConfig();

        liftArea = configuration.getInt("maxLiftArea");
        maxHeight = configuration.getInt("maxHeight");
        debug = configuration.getBoolean("debug");
        liftMobs = configuration.getBoolean("liftMobs");
        autoPlace = configuration.getBoolean("autoPlace");
        checkFloor = configuration.getBoolean("checkFloor", false);
        preventEntry = configuration.getBoolean("preventEntry", false);
        preventLeave = configuration.getBoolean("preventLeave", false);
        redstone = configuration.getBoolean("redstone", false);
        Set<String> baseBlockKeys = configuration.getConfigurationSection("baseBlockSpeeds").getKeys(false);
        for (String key : baseBlockKeys){
            blockSpeeds.put(Material.valueOf(key), configuration.getDouble("baseBlockSpeeds." + key));
        }
        List<String> configFloorMaterials = configuration.getStringList("floorBlocks");
        for (String key : configFloorMaterials){
            if (key.contains("*")){
                // Probably be smarter to iterate through the material list first, then see if config matches
                for (Material material : Material.values()){
                    if (material.toString().matches(key.replace("*", ".*?"))){
                        floorMaterials.add(material);
                        plugin.logInfo("Floor material added: " + material.toString());
                    }

                }
            } else {
                floorMaterials.add(Material.valueOf(key));
                plugin.logInfo("Floor material added: " + key);
            }
        }

        List<String> configButtonMaterials = configuration.getStringList("buttonBlocks");
        for (String key : configButtonMaterials){
            if (key.contains("*")){
                // Probably be smarter to iterate through the material list first, then see if config matches
                for (Material material : Material.values()){
                    if (material.toString().matches(key.replace("*", ".*?"))){
                        buttonMaterials.add(material);
                        plugin.logInfo("Button material added: " + material.toString());
                    }

                }
            } else {
                buttonMaterials.add(Material.valueOf(key));
                plugin.logInfo("Button material added: " + key);
            }
        }

        List<String> configSignMaterials = configuration.getStringList("signBlocks");
        for (String key : configSignMaterials){
            if (key.contains("*")){
                // Probably be smarter to iterate through the material list first, then see if config matches
                for (Material material : Material.values()){
                    if (material.toString().matches(key.replace("*", ".*?"))){
                        signMaterials.add(material);
                        plugin.logInfo("Sign material added: " + material.toString());
                    }

                }
            } else {
                signMaterials.add(Material.valueOf(key));
                plugin.logInfo("Sign material added: " + key);
            }
        }

        stringOneFloor = configuration.getString("STRING_oneFloor", "There is only one floor silly.");
        stringCurrentFloor = configuration.getString("STRING_currentFloor", "Current Floor:");
        stringDestination = configuration.getString("STRING_dest", "Dest:");
        stringCantEnter = configuration.getString("STRING_cantEnter", "Can't enter elevator in use");
        stringCantLeave = configuration.getString("STRING_cantLeave", "Can't leave elevator in use");
        stringUnsafe = configuration.getString("STRING_unsafe", "It is unsafe to leave a vehicle in a lift!");
        stringScrollSelectEnabled = configuration.getString("STRING_scrollSelectEnabled", "§7Scrollable floor selection enabled. Click on sign with an item for default mode");
        stringScrollSelectDisabled = configuration.getString("STRING_scrollSelectDisabled", "§7Scrollable floor selection disabled");

        metricbool = configuration.getBoolean("metrics", true);
        plugin.saveConfig();

        serverFlight = plugin.getServer().getAllowFlight();

        if (preventEntry){
            Bukkit.getServer().getPluginManager().registerEvents(plugin, plugin);
        }

        if(plugin.getServer().getPluginManager().getPlugin("NoCheatPlus") != null){
            useNoCheatPlus = true;
            plugin.logDebug("Hooked into NoCheatPlus");
        }
    }
}
