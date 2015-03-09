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
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;

import org.slf4j.Logger;
import org.spongepowered.api.block.BlockLoc;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.data.Sign;
import org.spongepowered.api.event.entity.living.player.PlayerInteractBlockEvent;
import org.spongepowered.api.event.state.ServerStartingEvent;
import org.spongepowered.api.event.state.ServerStoppingEvent;
import org.spongepowered.api.Game;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.service.config.DefaultConfig;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.event.Subscribe;

import com.google.common.base.Optional;
import com.google.inject.Inject;

@Plugin(id="Lift", name="Lift", version="52")
public class SpongeLift {
	public static SpongeLift instance;
	@Inject
	private Logger logger;
	
	@Inject
	private GameRegistry gameRegistry;
	
	@Inject
	@DefaultConfig(sharedRoot=true)
	private File defaultConfig;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	
	public static boolean debug = false;
	public static boolean redstone = false;
	public static int maxLiftArea = 16;
	public static int maxHeight = 256;
	public HashMap<BlockType, Double> blockSpeeds = new HashMap<BlockType, Double>();
	public HashSet<BlockType> floorMaterials = new HashSet<BlockType>();
	public boolean autoPlace = false;
	public boolean checkFloor = false;
	public boolean serverFlight = false;
	public boolean liftMobs = false;
	//public static BukkitElevatorManager manager;
	private boolean preventEntry = false;
	public boolean preventLeave = false;
	public static String stringDestination;
	public static String stringCurrentFloor;
	public static String stringOneFloor;
	public static String stringCantEnter;
	public static String stringCantLeave;

	@Subscribe
	public void onServerStart(ServerStartingEvent event){
		if (instance != null) {
			throw new RuntimeException("Lift cannot be enabled more than once per server!");
			}
		getLogger().info("Lift Initiaing");
		
		CommentedConfigurationNode config = null;

		 try {
		     if (!defaultConfig.exists()) {
		         defaultConfig.createNewFile();
		         config = configManager.load();
		         config.getNode("debug").setValue(debug);
		         config.getNode("redstone").setValue(redstone);
		         config.getNode("maxLiftArea").setValue(maxLiftArea);
		         config.getNode("maxHeight").setValue(maxHeight);
		         blockSpeeds.put( BlockTypes.IRON_BLOCK, 0.5);
		         config.getNode("blockSpeeds").setValue(blockSpeeds);
		         floorMaterials.add(BlockTypes.GLASS);
		         floorMaterials.add(BlockTypes.STAINED_GLASS);
		         config.getNode("floorMaterials").setValue(floorMaterials);
		         config.getNode("autoPlace").setValue(autoPlace);
		         config.getNode("checkFloor").setValue(checkFloor);
		         config.getNode("liftMobs").setValue(liftMobs);
		         config.getNode("preventEntry").setValue(preventEntry);
		         config.getNode("preventLeave").setValue(preventLeave);
		         config.getNode("redstone").setValue(redstone);
		         config.getNode("STRING_oneFloor").setValue(stringOneFloor);
		         config.getNode("STRING_currentFloor").setValue(stringCurrentFloor);
		         config.getNode("STRING_destFloor").setValue(stringDestination);
		         config.getNode("STRING_cantEnter").setValue(stringCantEnter);
		         config.getNode("STRING_cantLeave").setValue(stringCantLeave);
		         configManager.save(config);
		     }
		     config = configManager.load();
		     debug = config.getNode("debug").getBoolean();
	         redstone = config.getNode("redstone").getBoolean();
	         maxLiftArea = config.getNode("maxLiftArea").getInt();
	         maxHeight =config.getNode("maxHeight").getInt();
	         autoPlace = config.getNode("autoPlace").getBoolean();
	        checkFloor =  config.getNode("checkFloor").getBoolean();
	         liftMobs = config.getNode("liftMobs").getBoolean();
	         preventEntry = config.getNode("preventEntry").getBoolean();
	         preventLeave = config.getNode("preventLeave").getBoolean();
	         redstone = config.getNode("redstone").getBoolean();
	         stringOneFloor = config.getNode("STRING_oneFloor").getString();
	         stringCurrentFloor = config.getNode("STRING_currentFloor").getString();
	         stringDestination = config.getNode("STRING_destFloor").getString();
	         stringCantEnter = config.getNode("STRING_cantEnter").getString();
	         stringCantLeave = config.getNode("STRING_cantLeave").getString();
	         
	         Map<Object, ? extends CommentedConfigurationNode> configSpeeds = config.getNode("blockSpeeds").getChildrenMap();
	         Set<Object> keys = configSpeeds.keySet();
	         
	         for (Object key: keys){
	        	 String stringKey = key.toString();
	        	 Optional<BlockType> type = gameRegistry.getBlock(stringKey);
	        	 double speed = configSpeeds.get(stringKey).getDouble();
	        	 blockSpeeds.put(type.get(), speed);
	         }
	         
	         List<String> configFloorMaterials = config.getNode("floorMaterials").getList(null);
	         for (String key: configFloorMaterials){
	        	 floorMaterials.add(gameRegistry.getBlock(key).get());
	         }
		 } catch (IOException exception) {
		     getLogger().error("The default configuration could not be loaded or created!");
		 }

		// serverFlight = this.getServer().getAllowFlight();
		 // Will need to reevaluate the methodology for sponge
		 
		 //new BukkitLiftRedstoneListener(this);
	    //	new BukkitLiftPlayerListener(this);
	    //	manager = new BukkitElevatorManager(this);
		 
		 if (preventEntry){
			 //movement event listenener
		 }
		 
		 
		 
		logger.debug("maxArea: " + Integer.toString(maxLiftArea));
		logger.debug("autoPlace: " + Boolean.toString(autoPlace));
		logger.debug("checkGlass: " + Boolean.toString(checkFloor));
		logger.debug("baseBlocks: " + blockSpeeds.toString());
		logger.debug("floorBlocks: " + floorMaterials.toString());
		instance = this;
		getLogger().info("Lift Initiated");
	}
	
	@Subscribe
	public void onShutdown(ServerStoppingEvent event) {
		instance = null;
	}
	
	@Subscribe
	public void onPlayerSignClick(PlayerInteractBlockEvent event){
		// event.getInteractionType().LEFT_CLICK; //Broken?
		BlockLoc signBlock = event.getBlock();
		if (signBlock.getType().getId().contains("wall_sign")){
			BlockLoc buttonBlock = signBlock.getRelative(Direction.DOWN) ;
			if (buttonBlock.getType().getId().contains("button")){
				Sign sign = signBlock.getData(Sign.class).get();
				SpongeElevator elevator = SpongeElevatorManager.createLift(buttonBlock, "Sign click by " + event.getPlayer().getIdentifier());
				
				if (!elevator.getFailReason().isEmpty()){
					event.getPlayer().sendMessage("Failed to generate lift due to: " + elevator.getFailReason());
					return;
				}
				
				
				event.setCancelled(true);
			}
		}
	}
	
	public Logger getLogger(){
		return logger;
	}
	
	public File getDefaultConfig() {
	    return defaultConfig;
	}

	public ConfigurationLoader<CommentedConfigurationNode> getConfigManager() {
	    return configManager;
	}
}
