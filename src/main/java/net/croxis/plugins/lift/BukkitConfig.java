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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Note that fields that are configurable in config.yml should have the same name, so
 * {@link #mapConfigurationToClassFields)} maps values correctly to fields of this class.
 * For that mapping, you should use boxed types instead of primitive types (Integer instead of int, ...)
 */
public class BukkitConfig extends Config {

	public static HashMap<Material, Double> blockSpeeds = new HashMap<>();
	public static HashSet<Material> floorMaterials = new HashSet<>();
    public static HashSet<Material> buttonMaterials = new HashSet<>();
    public static HashSet<Material> signMaterials = new HashSet<>();
	static boolean useNoCheatPlus = false;
	static boolean metrics = true;
	static boolean serverFlight;

    public void loadConfig(BukkitLift plugin){
        File configFile = new File(plugin.getDataFolder(), File.separator + "config.yml");
        if (!configFile.exists()) {
            copyDefaultConfig(plugin, configFile);
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "/config.yml"));
        config.setDefaults(getDefaultConfig(plugin));
        config.options().copyDefaults(true);

        mapConfigurationToClassFields(config, Config.class);
        mapConfigurationToClassFields(config.getConfigurationSection("messages"), Config.class);
        metrics = config.getBoolean("metrics");

        ConfigurationSection baseBlockSpeeds = config.getConfigurationSection("baseBlockSpeeds");
        baseBlockSpeeds.getKeys(false)
                .forEach(key -> blockSpeeds.put(Material.valueOf(key), baseBlockSpeeds.getDouble(key)));

        BiPredicate<List<String>, Material> anyMaterialMatch = (list, mat) -> list.stream()
                .anyMatch(configMat -> mat.name().matches(configMat.toUpperCase().replace("*", ".*?")));

        List<String> configFloorMaterials = config.getStringList("floorBlocks");
        Arrays.stream(Material.values())
                .filter(material -> anyMaterialMatch.test(configFloorMaterials, material))
                .forEach(floorMaterials::add);
        plugin.logInfo("Floor materials added: " + floorMaterials);

        List<String> configButtonMaterials = config.getStringList("buttonBlocks");
        Arrays.stream(Material.values())
                .filter(material -> anyMaterialMatch.test(configButtonMaterials, material))
                .forEach(buttonMaterials::add);
        plugin.logInfo("Button materials added: " + buttonMaterials);

        List<String> configSignMaterials = config.getStringList("signBlocks");
        Arrays.stream(Material.values())
                .filter(mat -> anyMaterialMatch.test(configSignMaterials, mat))
                .forEach(signMaterials::add);
        plugin.logInfo("Sign materials added: " + signMaterials);

        try {
            config.save(configFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not save config to " + configFile, e);
        }

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
