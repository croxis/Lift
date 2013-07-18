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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Level;

import org.spout.api.event.Listener;
import org.spout.api.material.Material;
import org.spout.api.plugin.Plugin;
import org.spout.cereal.config.ConfigurationException;
import org.spout.cereal.config.yaml.YamlConfiguration;
import org.spout.vanilla.material.VanillaMaterials;

public class SpoutLift extends Plugin implements Listener{

	public static boolean debug = false;
	public int liftArea = 16;
	public static int maxHeight = 256;
	public HashMap<Material, Double> blockSpeeds = new HashMap<Material, Double>();
	public Material floorBlock;
	public boolean autoPlace = false;
	public boolean checkGlass = false;
	public static SpoutElevatorManager manager;
	private boolean preventEntry = false;
	private boolean preventLeave = false;
	public static String stringDestination;
	public static String stringCurrentFloor;
	public static String stringOneFloor;
	public static String stringCantEnter;
	public static String stringCantLeave;

	public void onDisable() {
    	SpoutElevatorManager.elevators.clear();
    	getEngine().getScheduler().cancelTask(SpoutElevatorManager.taskid);
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	floorBlock = VanillaMaterials.GLASS;
    	
    	new SpoutLiftRedstoneListener(this);
    	new SpoutLiftPlayerListener(this);
    	manager = new SpoutElevatorManager(this);
    	
    	File file = new File(getDataFolder(), "config.yml");
		if (!file.exists()) {
			initializeConfig(file, "config.yml");
    	}
    	YamlConfiguration config = new YamlConfiguration(file);
    	try {
    		config.load();
    	} catch (ConfigurationException e) {
    		getLogger().severe("Unable to load the configuration file! Message:" + e.getMessage());
    	}
    	
    	liftArea = config.getNode("maxLiftArea").getInt();
    	maxHeight = config.getNode("maxHeight").getInt();
    	debug = config.getNode("debug").getBoolean();
    	autoPlace = config.getNode("autoPlace").getBoolean();
    	checkGlass = config.getNode("checkGlass").getBoolean();
    	preventEntry = config.getNode("preventEntry").getBoolean();
    	preventLeave = config.getNode("preventLeave").getBoolean();
    	Set<String> baseBlockKeys = config.getNode("baseBlockSpeeds").getChildren().keySet();
    	for (String key : baseBlockKeys){
    		blockSpeeds.put(Material.get(key), config.getNode("baseBlockSpeeds." + key).getDouble());
    	}
    	floorBlock = Material.get(config.getNode("floorBlock").getString());
    	stringOneFloor = config.getNode("STRING_oneFloor").getString();
    	stringCurrentFloor = config.getNode("STRING_currentFloor").getString();
    	stringDestination = config.getNode("STRING_dest").getString();
    	stringCantEnter = config.getNode("STRING_cantEnter").getString();
    	stringCantLeave = config.getNode("STRING_cantLeave").getString();
        
        if (preventEntry || preventLeave){
        	getEngine().getEventManager().registerEvents(this, this);
        }
		logDebug("maxArea: " + Integer.toString(liftArea));
		logDebug("autoPlace: " + Boolean.toString(autoPlace));
		logDebug("checkGlass: " + Boolean.toString(checkGlass));
		logDebug("baseBlocks: " + blockSpeeds.toString());
        
        try {
            SpoutMetrics metrics = new SpoutMetrics(this);
            metrics.start();
        } catch (Exception e) {
            // Failed to submit the stats :-(
        }
        
        System.out.println(this + " is now enabled!");
    }
	
	public void logDebug(String message){
		if (debug )
			this.getLogger().log(Level.INFO, "[DEBUG] " + message);
	}
	
	public void logInfo(String message){
		this.getLogger().log(Level.INFO, message);
	}
	
	/**
	 * Initialize a configuration file with the default file located in the .jar. Only creates the file if the default file doesn't exist.
	 * 
	 * @param file The path to to the file.
	 * @param fileName The fileName.
	 */
	// TODO: Maybe improve the 2nd parameter?
	protected void initializeConfig(File file, String fileName) {
		try {
			getLogger().info("Creating directories: " + file.toString() + " | " + file.getParentFile().toString());
			file.getParentFile().mkdirs();
			file.createNewFile();
		} catch (IOException e) {
			getLogger().severe("Error while trying to create the file " + file.getName() + "! Error is: " + e.getMessage());
		}
		URL url = this.getClass().getResource("/" + fileName);

		if (url != null) {
			try {
				InputStream defaultStream = url.openStream();
				FileOutputStream fos = new FileOutputStream(file);

				byte[] buffer = new byte[1024*4];
				int n = 0;
				while (-1 != (n = defaultStream.read(buffer))) {
					fos.write(buffer, 0, n);
				}
				fos.flush();
				fos.close();
				defaultStream.close();
			} catch (IOException e1) {
				getLogger().severe("Error while trying to copy the default file + " + file.getName() + ". Error is: " + e1.getMessage());
			}
		}
	}

}
