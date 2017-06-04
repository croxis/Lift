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

import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;

import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import org.spongepowered.api.scheduler.Task;


@Plugin(id = "lift", name = "Lift", version = "56", authors = {"croxis"}, description="")
public class SpongeLift {
    public static SpongeElevatorManager manager;

    SpongeLiftRedstoneListener redstoneListener;
    SpongeLiftPlayerListener playerListener;

    Task spongeManagerTask;

	@Inject
	private static Logger logger;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private HoconConfigurationLoader configManager;

    //@Inject
    //@DefaultConfig(sharedRoot = true)
    //private ConfigurationLoader<CommentedConfigurationNode> loader;


    public static SpongeConfig config = new SpongeConfig();
	
	@Listener
    public void onServerStart(GameStartedServerEvent event) {
        // Hey! The server has started!
        // Try instantiating your logger in here.
        // (There's a guide for that)
		getLogger().info("Loading Lift");
		//ConfigurationNode rootNode = configManager.createEmptyNode(ConfigurationOptions.defaults());
        ConfigurationLoader<CommentedConfigurationNode> loader =
                HoconConfigurationLoader.builder().setPath(defaultConfig).build();
        ConfigurationNode rootNode;
        try {
            rootNode = loader.load();
        } catch(IOException e) {
            //error
            rootNode = loader.createEmptyNode(ConfigurationOptions.defaults());
        }
        SpongeConfig.debug = rootNode.getNode("debug").getBoolean(false);
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

        boolean metricsbool = rootNode.getNode("metrics").getBoolean(true);

        try {
            loader.save(rootNode);
        } catch(IOException e) {
            // error
        }

        if (SpongeConfig.preventEntry){
            Sponge.getEventManager().registerListeners(this, new SpongeMovePreventListener());
        }


		redstoneListener = new SpongeLiftRedstoneListener(this);
        playerListener = new SpongeLiftPlayerListener(this);
        manager = new SpongeElevatorManager(this);
		startListeners();
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
    }

    static void debug(String message) {
	    if (SpongeConfig.debug) {
            logger.debug(message);
        }
    }
	
	Logger getLogger() {
	    return logger;
	}

    static Double getBlockSpeed(BlockType material) {
        try {
            return SpongeConfig.blockSpeeds.get(material);
        } catch (Exception e) {
            logger.warn("There was an exception getting the block speed for " + material.toString());
            return 0.0;
        }
    }

}
