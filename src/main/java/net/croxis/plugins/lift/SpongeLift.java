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

import com.google.inject.Inject;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import org.slf4j.Logger;
import org.spongepowered.api.GameRegistry;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.cause.NamedCause;
import org.spongepowered.api.event.filter.cause.Named;
import org.spongepowered.api.event.filter.cause.Root;
import org.spongepowered.api.event.game.state.GameStartingServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.text.format.TextStyles;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Plugin(id = "net.croxis.plugins.lift", name = "Lift", version = "55", authors = {"croxis", "gabizou"})
public class SpongeLift {

    public static SpongeLift instance;

    @Inject
    private Logger logger;

    @Inject
    private GameRegistry gameRegistry;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private File defaultConfig;

    @Inject
    @DefaultConfig(sharedRoot = true)
    private ConfigurationLoader<CommentedConfigurationNode> configManager;

    public static SpongeElevatorManager manager;
    public static boolean debug = false;
    public static boolean redstone = false;
    public static int maxLiftArea = 16;
    public static int maxHeight = 256;
    public HashMap<BlockType, Double> blockSpeeds = new HashMap<>();
    public HashSet<BlockType> floorMaterials = new HashSet<>();
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

    @Listener
    public void onServerStart(GameStartingServerEvent event) {
        if (instance != null) {
            throw new RuntimeException("Lift cannot be enabled more than once per server!");
        }
        getLogger().info("Lift Initiaing");

        CommentedConfigurationNode config = null;

        try {
            if (!this.defaultConfig.exists()) {
                this.defaultConfig.createNewFile();
                config = this.configManager.load();
                config.getNode("debug").setValue(debug);
                config.getNode("redstone").setValue(redstone);
                config.getNode("maxLiftArea").setValue(maxLiftArea);
                config.getNode("maxHeight").setValue(maxHeight);
                this.blockSpeeds.put(BlockTypes.IRON_BLOCK, 0.5);

                HashMap<String, Double> blockSpeedsString = new HashMap<>();
                blockSpeedsString.put("minecraft:iron_block", 0.5);

                config.getNode("blockSpeeds").setValue(blockSpeedsString);
                this.floorMaterials.add(BlockTypes.GLASS);
                this.floorMaterials.add(BlockTypes.STAINED_GLASS);

                HashSet<String> floorMaterialsString = new HashSet<>();
                floorMaterialsString.add("minecraft:glass");
                floorMaterialsString.add("minecraft:stained_glass");

                config.getNode("floorMaterials").setValue(floorMaterialsString);
                config.getNode("autoPlace").setValue(this.autoPlace);
                config.getNode("checkFloor").setValue(this.checkFloor);
                config.getNode("liftMobs").setValue(this.liftMobs);
                config.getNode("preventEntry").setValue(this.preventEntry);
                config.getNode("preventLeave").setValue(this.preventLeave);
                config.getNode("redstone").setValue(redstone);
                config.getNode("STRING_oneFloor").setValue(stringOneFloor);
                config.getNode("STRING_currentFloor").setValue(stringCurrentFloor);
                config.getNode("STRING_destFloor").setValue(stringDestination);
                config.getNode("STRING_cantEnter").setValue(stringCantEnter);
                config.getNode("STRING_cantLeave").setValue(stringCantLeave);
                this.configManager.save(config);
            }
            config = this.configManager.load();
            debug = config.getNode("debug").getBoolean();
            redstone = config.getNode("redstone").getBoolean();
            maxLiftArea = config.getNode("maxLiftArea").getInt();
            maxHeight = config.getNode("maxHeight").getInt();
            this.autoPlace = config.getNode("autoPlace").getBoolean();
            this.checkFloor = config.getNode("checkFloor").getBoolean();
            this.liftMobs = config.getNode("liftMobs").getBoolean();
            this.preventEntry = config.getNode("preventEntry").getBoolean();
            this.preventLeave = config.getNode("preventLeave").getBoolean();
            redstone = config.getNode("redstone").getBoolean();
            stringOneFloor = config.getNode("STRING_oneFloor").getString();
            stringCurrentFloor = config.getNode("STRING_currentFloor").getString();
            stringDestination = config.getNode("STRING_destFloor").getString();
            stringCantEnter = config.getNode("STRING_cantEnter").getString();
            stringCantLeave = config.getNode("STRING_cantLeave").getString();

            Map<Object, ? extends CommentedConfigurationNode> configSpeeds = config.getNode("blockSpeeds").getChildrenMap();
            Set<Object> keys = configSpeeds.keySet();
            this.logger.info("Loadingin keys: " + keys.toString());
            for (Object key : keys) {
                this.logger.info("Loadingin key: " + key.toString());
                String stringKey = key.toString();
                BlockType type = this.gameRegistry.getType(BlockType.class, stringKey).orElse(BlockTypes.IRON_BLOCK);
                this.logger.info("Loaded block: " + type);
                double speed = configSpeeds.get(stringKey).getDouble();
                this.blockSpeeds.put(type, speed);
            }
            this.logger.info("Block speeds: " + this.blockSpeeds.toString());

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

        if (this.preventEntry) {
            //movement event listenener
        }

        this.logger.debug("maxArea: " + Integer.toString(maxLiftArea));
        this.logger.debug("autoPlace: " + Boolean.toString(this.autoPlace));
        this.logger.debug("checkGlass: " + Boolean.toString(this.checkFloor));
        this.logger.debug("baseBlocks: " + this.blockSpeeds.toString());
        this.logger.debug("floorBlocks: " + this.floorMaterials.toString());
        instance = this;
        manager = new SpongeElevatorManager();
        manager.init(this);
        getLogger().info("Lift Initiated");
    }

    @Listener
    public void onPlayerSignClick(InteractBlockEvent.Primary event, @Root @Named(NamedCause.SOURCE) Player player) {
        // event.getInteractionType().LEFT_CLICK; //Broken?
        Location<World> signBlock = event.getTargetBlock().getLocation().get();
        if (signBlock.getBlockType().equals(BlockTypes.WALL_SIGN)) {
            Location<World> buttonBlock = signBlock.getRelative(Direction.DOWN);
            if (buttonBlock.getBlockType().equals(BlockTypes.STONE_BUTTON)) {
                Sign sign = ((Sign) signBlock.getTileEntity().get());
                SpongeElevator elevator = SpongeElevatorManager.createLift(buttonBlock, "Sign click by " + player.getIdentifier());

                if (!elevator.getFailReason().isEmpty()) {
                    player.sendMessage(Text.of(TextColors.RED, TextStyles.ITALIC, "Failed to generate lift due to: " + elevator.getFailReason()));
                    return;
                }
                if (elevator.getTotalFloors() == 2) {
                    player.sendMessage(Text.of(TextColors.RED, TextStyles.ITALIC, SpongeLift.instance.stringOneFloor));
                    return;
                }
                event.setCancelled(true); // Valid lift. Cancel interaction and lets start lifting up!

                int currentDestinationInt = 1;
                SpongeFloor currentFloor = elevator.getFloorFromY(currentDestinationInt);
                if (currentFloor == null) {
                    player.sendMessage(Text.of(TextColors.RED, TextStyles.ITALIC,
                            "Elevator generator says this floor does not exist. Check shaft for blockage"));
                    return;
                }

                String sign0 = stringCurrentFloor;
                String sign1 = Integer.toString(currentFloor.getFloor());
                String sign2 = "";
                String sign3 = "";
                final ListValue<Text> lines = sign.lines();

                try {
                    String[] splits = lines.get(2).toString().split(": ");
                    currentDestinationInt = Integer.parseInt(splits[1]);
                } catch (Exception e) {
                    currentDestinationInt = 0;
                    this.logger.debug("Non valid previous destination.");
                }
                currentDestinationInt++;
                if (currentDestinationInt == currentFloor.getFloor()) {
                    currentDestinationInt++;
                    this.logger.debug("Skipping current floor");
                }
                if (currentDestinationInt > elevator.getTotalFloors()) {
                    if (currentFloor.getFloor() == 1) {
                        currentDestinationInt = 2;
                    }
                    this.logger.debug("Rotating back to first floor.");
                }
                sign2 = TextColors.GREEN + stringDestination + " " + Integer.toString(currentDestinationInt);
                sign3 = elevator.getFloorFromN(currentDestinationInt).getName();
                lines.set(0, Text.of(sign0));
                lines.set(1, Text.of(sign1));
                lines.set(2, Text.of(sign2));
                lines.set(3, Text.of(sign3));
                sign.offer(lines);
                this.logger.debug("Completed sign update");
            }
        }
    }

    public Logger getLogger() {
        return this.logger;
    }

    public void debug(String string) {
        this.logger.info(string);
    }
}
