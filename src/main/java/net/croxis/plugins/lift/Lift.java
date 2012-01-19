package net.croxis.plugins.lift;

import java.util.HashSet;

import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class Lift extends JavaPlugin {
	boolean debug = false;
	boolean useSpout = false;
	public HashSet<Entity> fallers = new HashSet<Entity>();
	public HashSet<Elevator> lifts = new HashSet<Elevator>();
	public double liftSpeed = 0.5;
	public int liftArea = 16;
    public void onDisable() {
    	lifts.clear();
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	new LiftRedstoneListener(this);
    	new LiftPlayerListener(this);
    	
    	liftSpeed = this.getConfig().getDouble("liftSpeed");
    	liftArea = this.getConfig().getInt("maxLiftArea");
    	this.debug = this.getConfig().getBoolean("debug");
    	this.getConfig().options().copyDefaults(true);
        saveConfig();
        Plugin test = getServer().getPluginManager().getPlugin("Spout");
        if(test != null) {
        	useSpout = true;
        }
        System.out.println(this + " is now enabled!");
    }
}
