package net.croxis.plugins.lift;

import java.util.HashSet;

import org.bukkit.Material;
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
	public Material baseMaterial = Material.IRON_BLOCK;
	public boolean autoPlace = false;
    public void onDisable() {
    	lifts.clear();
        System.out.println(this + " is now disabled!");
    }

    public void onEnable() {
    	new LiftRedstoneListener(this);
    	new LiftPlayerListener(this);
    	
    	liftSpeed = this.getConfig().getDouble("liftSpeed");
    	liftArea = this.getConfig().getInt("maxLiftArea");
    	debug = this.getConfig().getBoolean("debug");
    	baseMaterial = Material.valueOf(this.getConfig().getString("baseBlock", "IRON_BLOCK"));
    	autoPlace = this.getConfig().getBoolean("autoPlace");
    	this.getConfig().options().copyDefaults(true);
        saveConfig();
        Plugin test = getServer().getPluginManager().getPlugin("Spout");
        if(test != null) {
        	useSpout = true;
        }
        System.out.println(this + " is now enabled!");
    }
    
    public void removeLift(Elevator elevator){
    	
    }
}
