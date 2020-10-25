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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Note that fields that are configurable in config.yml should have the same name, so
 * {@link #mapConfigurationToClassFields)} maps values correctly to fields of this class.
 * For that mapping, you should use boxed types instead of primitive types (Integer instead of int, ...)
 */
public class Config {

	static Boolean debug;
	static Boolean redstone;
	static Integer maxLiftArea;
	static Integer maxHeight;
	static Boolean autoPlace;
	static Boolean checkFloor;
	static Boolean serverFlight;
	static Boolean liftMobs;
	static Boolean preventEntry;
	static Boolean preventLeave;
	static String destination;
	static String currentFloor;
	static String oneFloor;
	static String cantEnter;
	static String cantLeave;
	static String unsafe;
	static String scrollSelectEnabled;
	static String scrollSelectDisabled;
    static Integer signVersion = 2;

	protected void mapConfigurationToClassFields(ConfigurationSection section, Class<? extends Config> clazz) {
		if (section == null) {
			return;
		}
		Map<String, Field> classFields = Arrays.stream(clazz.getDeclaredFields())
				.collect(Collectors.toMap(Field::getName, field -> field));

		section.getKeys(false)
				.stream()
				.filter(classFields::containsKey)
				.forEach(name -> {
							try {
								Class<?> fieldType = classFields.get(name).getType();
								Object value = fieldType != String.class ? section.getObject(name, fieldType)
										: section.getString(name).replace("&", "§");
								classFields.get(name).set(clazz, value);
							} catch (IllegalAccessException e) {
								throw new RuntimeException(e);
							}
						}
				);
	}

	protected static YamlConfiguration getDefaultConfig(BukkitLift plugin) {
		File defaultConfigFile = new File(plugin.getDataFolder(), File.separator + "default" + File.separator + "config.yml");
		copyDefaultConfig(plugin, defaultConfigFile);
		return YamlConfiguration.loadConfiguration(defaultConfigFile);
	}

	protected static void copyDefaultConfig(BukkitLift plugin, File dest) {
		try (InputStream in = plugin.getResource("config.yml")) {
			if (in == null) {
				throw new IOException("Error while preparing copy of default config");
			}
			Path destPath = dest.toPath();
			Files.createDirectories(destPath.getParent());
			Files.deleteIfExists(destPath);
			Files.createFile(destPath);
			Files.copy(in, destPath, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new RuntimeException("Lift could not copy default config file!", e);
		}
	}
}
