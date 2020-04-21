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

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;
import org.slf4j.Logger;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;

import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.ConfigurationOptions;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.scheduler.Task;


@Plugin(id = "lift", name = "Lift", version = "63", authors = {"croxis"}, description="")
public class SpongeLift {
    public static SpongeElevatorManager manager;

    SpongeLiftRedstoneListener redstoneListener;
    SpongeLiftPlayerListener playerListener;

    Task spongeManagerTask;

	@Inject
	private Logger logger;

    @Inject
    PluginContainer container;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configLoader;

    public static SpongeConfig config = new SpongeConfig();
	
	@Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Hey! The server has started!
        // Try instantiating your logger in here.
        // (There's a guide for that)
		getLogger().info("Loading Lift");

        configLoader = HoconConfigurationLoader.builder().setPath(defaultConfig).build();
        CommentedConfigurationNode rootNode;

        ConfigurationOptions configOptions = ConfigurationOptions.defaults().setShouldCopyDefaults(true);

        try {
            rootNode = configLoader.load(configOptions);
        } catch(IOException e) {
            // config file doesn't exist, create new
            rootNode = configLoader.createEmptyNode(configOptions);
        }
        
        SpongeConfig.debug = rootNode.getNode("debug").getBoolean(true);
        SpongeConfig.redstone = rootNode.getNode("redstone").getBoolean(true);
        SpongeConfig.liftArea = rootNode.getNode("liftArea").getInt(16);
        SpongeConfig.maxHeight = rootNode.getNode("maxHeight").getInt(256);
        SpongeConfig.autoPlace = rootNode.getNode("autoPlace").getBoolean(false);
        SpongeConfig.checkFloor = rootNode.getNode("checkFloor").getBoolean(false);
        SpongeConfig.serverFlight = false; // TODO: How to get from server config?
        SpongeConfig.liftMobs = rootNode.getNode("liftMobs").getBoolean(false);
        SpongeConfig.preventEntry = rootNode.getNode("preventEntry").getBoolean(false);
        SpongeConfig.preventLeave = rootNode.getNode("preventLeave").getBoolean(false);
        SpongeConfig.stringDestination = rootNode.getNode("stringDestination").getString("§1Dest");
        SpongeConfig.stringCurrentFloor = rootNode.getNode("stringCurrentFloor").getString("§4Current Floor");
        SpongeConfig.stringOneFloor = rootNode.getNode("stringOneFloor").getString("");
        SpongeConfig.stringCantEnter = rootNode.getNode("stringCantEnter").getString("");
        SpongeConfig.stringCantLeave = rootNode.getNode("stringCantLeave").getString("");

        GameRegistry registry = Sponge.getRegistry();

        Map<Object, ? extends CommentedConfigurationNode> blockSpeedsNodes = rootNode.getNode("baseBlockSpeeds").getChildrenMap();
        
        if (blockSpeedsNodes.isEmpty()) {
            SpongeConfig.blockSpeeds.put(BlockTypes.IRON_BLOCK, rootNode.getNode("baseBlockSpeeds", BlockTypes.IRON_BLOCK.getId()).getDouble(0.5D));
        } else {
            for (Map.Entry<Object, ? extends CommentedConfigurationNode> entry : blockSpeedsNodes.entrySet()) {
                String id = (String) entry.getKey();
        
                BlockType blockType;
        
                try {
                    blockType = registry.getType(BlockType.class, id).orElseThrow(() -> new ObjectMappingException("Block not found: " + id));
                } catch (Exception e) {
                    getLogger().error("An error occurred while adding base block material: {}", e.getMessage());
            
                    continue;
                }
        
                SpongeConfig.blockSpeeds.put(blockType, entry.getValue().getDouble());
            }
        }
        
        List<String> floorMaterialsList = new ArrayList<>();
        
        try {
            floorMaterialsList = rootNode.getNode("floorMaterials").getList(TypeToken.of(String.class), Arrays.asList(BlockTypes.GLASS.getId()));
        } catch (ObjectMappingException e) {
            getLogger().error("An error occurred while loading floor block list: {}", e.getMessage());
    
            floorMaterialsList.add(BlockTypes.GLASS.getId());
        }
        
        for (String id : floorMaterialsList) {
            BlockType blockType;
    
            try {
                blockType = registry.getType(BlockType.class, id).orElseThrow(() -> new ObjectMappingException("Block not found: " + id));
            } catch (Exception e) {
                getLogger().error("An error occurred while adding floor material: {}", e.getMessage());
        
                continue;
            }
            
            SpongeConfig.floorMaterials.add(blockType);
        }

        try {
            configLoader.save(rootNode);
        } catch(IOException e) {
            // error
            e.printStackTrace();
        }

        if (SpongeConfig.preventEntry){
            Sponge.getEventManager().registerListeners(this, new SpongeMovePreventListener());
        }

		redstoneListener = new SpongeLiftRedstoneListener(this);
        playerListener = new SpongeLiftPlayerListener(this);
        manager = new SpongeElevatorManager(this);
        Sponge.getEventManager().registerListeners(this, redstoneListener);
        Sponge.getEventManager().registerListeners(this, playerListener);
        startListeners();
        debug("maxArea: " + Integer.toString(SpongeConfig.liftArea));
        debug("autoPlace: " + Boolean.toString(SpongeConfig.autoPlace));
        debug("checkGlass: " + Boolean.toString(SpongeConfig.checkFloor));
        debug("baseBlocks: " + SpongeConfig.blockSpeeds.toString());
        debug("floorBlocks: " + SpongeConfig.floorMaterials.toString());
        getLogger().info("Started SpongeLift");
    }

    @Listener
    public void reload(GameReloadEvent event) {
        SpongeElevatorManager.reset();
        getLogger().info("Restarting SpongeLift");
    }

    private void startListeners() {
        Task.Builder taskBuilder = Task.builder();
        spongeManagerTask = taskBuilder.execute(
                () -> {
                    manager.run();
                }
        ).intervalTicks(1).name("LiftManager").submit(this);
        getLogger().info("Started listener.");
    }

    void debug(String message) {
	    if (SpongeConfig.debug) {
            logger.info("[Lift Debug] " + message);
        }
    }
	
	Logger getLogger() {
	    return logger;
	}

    Double getBlockSpeed(BlockType material) {
        try {
            return SpongeConfig.blockSpeeds.get(material);
        } catch (Exception e) {
            logger.warn("There was an exception getting the block speed for " + material.toString());
            return 0.0;
        }
    }

}
