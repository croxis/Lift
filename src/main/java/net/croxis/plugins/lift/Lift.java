package net.croxis.plugins.lift;

import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Lift extends JavaPlugin {
	boolean debug = false;
	private final LiftRedstoneListener redstoneListener = new LiftRedstoneListener(this);
	private final LiftPlayerListener playerListener = new LiftPlayerListener(this);
    public void onDisable() {
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	PluginManager pm = getServer().getPluginManager();
    	pm.registerEvent(Event.Type.REDSTONE_CHANGE, this.redstoneListener, Event.Priority.Low, this);
    	pm.registerEvent(Event.Type.PLAYER_INTERACT, this.playerListener, Event.Priority.Low, this);
        System.out.println(this + " is now enabled!");
    }
}
