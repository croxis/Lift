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
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.message.Messages;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.util.event.Subscribe;

import com.google.inject.Inject;

@Plugin(id="Lift", name="Lift", version="52")
public class SpongeLift {
	public static SpongeLift instance;
	
	@Inject
	private Logger logger;
	
	static public Game game;
	
	@Inject
	private GameRegistry gameRegistry;
	
	@Inject
	@DefaultConfig(sharedRoot=true)
	private File defaultConfig;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	public static SpongeElevatorManager manager;
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
	public static String stringDestination = "Dest:";
	public static String stringCurrentFloor = "Current Floor:";
	public static String stringOneFloor = "There is only one floor silly.";
	public static String stringCantEnter = "Can't enter elevator in use";
	public static String stringCantLeave = "Can't leave elevator in use";
	
	@Inject
	public SpongeLift(Game game){
		this.game = game;
	}

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
		         
		         HashMap<String, Double> blockSpeedsString = new HashMap<String, Double>();
		         blockSpeedsString.put("minecraft:iron_block", 0.5);
		         
		         config.getNode("blockSpeeds").setValue(blockSpeedsString);
		         floorMaterials.add(BlockTypes.GLASS);
		         floorMaterials.add(BlockTypes.STAINED_GLASS);
		         
		         HashSet<String> floorMaterialsString = new HashSet<String>();
		         floorMaterialsString.add("minecraft:glass");
		         floorMaterialsString.add("minecraft:stained_glass");
		         
		         config.getNode("floorMaterials").setValue(floorMaterialsString);
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
	         logger.info("Loadingin keys: " + keys.toString());
	         for (Object key: keys){
	        	 logger.info("Loadingin key: " + key.toString());
	        	 String stringKey = key.toString();
	        	 BlockType type = gameRegistry.getBlock(stringKey).get();
	        	 logger.info("Loaded block: " + gameRegistry.getBlock("minecraft:iron_block").get());
	        	 double speed = configSpeeds.get(stringKey).getDouble();
	        	 blockSpeeds.put(type, speed);
	         }
	         logger.info("Block speeds: " + blockSpeeds.toString());
	         
	         //FIXME
	         //List<String> configFloorMaterials = config.getNode("floorMaterials").getList(arg0);
	         //for (String key: configFloorMaterials){
	        //	 floorMaterials.add(gameRegistry.getBlock(key).get());
	         //}
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
		manager = new SpongeElevatorManager(this.game);
		manager.init();
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
				if (elevator.getTotalFloors() == 2){
					event.getPlayer().sendMessage(SpongeLift.instance.stringOneFloor);
					return;
				}
				//FIXME: Sponge throws errors
				//event.setCancelled(true); // Valid lift. Cancel interaction and lets start lifting up!
				
				int currentDestinationInt = 1;
				SpongeFloor currentFloor = elevator.getFloorFromY(currentDestinationInt);
				if (currentFloor == null){
					event.getPlayer().sendMessage("Elevator generator says this floor does not exist. Check shaft for blockage");
					return;
				}
				
				String sign0 = stringCurrentFloor;
				String sign1 = Integer.toString(currentFloor.getFloor());
				String sign2 = "";
				String sign3 = "";
				
				try {
					String[] splits = sign.getLine(2).toString().split(": ");
					currentDestinationInt = Integer.parseInt(splits[1]);
				} catch (Exception e){
					currentDestinationInt = 0;
					logger.debug("Non valid previous destination.");
				}
				currentDestinationInt++;
				if (currentDestinationInt == currentFloor.getFloor()){
					currentDestinationInt++;
					logger.debug("Skipping current floor");
				}
				if (currentDestinationInt > elevator.getTotalFloors()){
					if (currentFloor.getFloor() == 1)
						currentDestinationInt = 2;
					logger.debug("Rotating back to first floor.");
				}
				sign2 = TextColors.GREEN + stringDestination + " " + Integer.toString(currentDestinationInt);
				sign3 = elevator.getFloorFromN(currentDestinationInt).getName();
				sign.setLine(0, Messages.of(sign0));
				sign.setLine(1, Messages.of(sign1));
				sign.setLine(2, Messages.of(sign2));
				sign.setLine(3, Messages.of(sign3));
				//sign.update();
				logger.debug("Completed sign update");
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
	
	public void debug(String string){
		this.logger.info(string);
	}
}
