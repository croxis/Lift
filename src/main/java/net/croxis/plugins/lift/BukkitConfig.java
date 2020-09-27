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

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.function.BiPredicate;

public class BukkitConfig extends Config{
	public static HashMap<Material, Double> blockSpeeds = new HashMap<>();
	public static HashSet<Material> floorMaterials = new HashSet<>();
    public static HashSet<Material> buttonMaterials = new HashSet<>();
    public static HashSet<Material> signMaterials = new HashSet<>();
	static boolean useNoCheatPlus = false;
	static boolean metricbool = true;
	static boolean serverFlight;

    public void loadConfig(BukkitLift plugin){
        plugin.reloadConfig();
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

        BiPredicate<List<String>, Material> anyMaterialMatch = (list, mat) -> list.stream()
                .anyMatch(configMat -> mat.name().matches(configMat.toUpperCase().replace("*", ".*?")));

        List<String> configFloorMaterials = configuration.getStringList("floorBlocks");
        Arrays.stream(Material.values())
                .filter(material -> anyMaterialMatch.test(configFloorMaterials, material))
                .forEach(floorMaterials::add);
        plugin.logInfo("Floor materials added: " + floorMaterials);

        List<String> configButtonMaterials = configuration.getStringList("buttonBlocks");
        Arrays.stream(Material.values())
                .filter(material -> anyMaterialMatch.test(configButtonMaterials, material))
                .forEach(buttonMaterials::add);
        plugin.logInfo("Button materials added: " + buttonMaterials);

        List<String> configSignMaterials = configuration.getStringList("signBlocks");
        Arrays.stream(Material.values())
                .filter(mat -> anyMaterialMatch.test(configSignMaterials, mat))
                .forEach(signMaterials::add);
        plugin.logInfo("Sign materials added: " + signMaterials);

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
