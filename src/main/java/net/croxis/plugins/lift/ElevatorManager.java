package net.croxis.plugins.lift;

import java.util.HashSet;

import net.h31ix.anticheat.api.AnticheatAPI;
import net.h31ix.anticheat.manage.CheckType;

import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.getspout.spoutapi.SpoutManager;

public class ElevatorManager {
	private static Lift plugin;
	public static HashSet<Elevator> lifts = new HashSet<Elevator>();

	public ElevatorManager(Lift plugin) {
		ElevatorManager.plugin = plugin;
	}
	
	public static void endLift(Elevator lift){
		plugin.logDebug("Halting lift");
		for (Block b : lift.glassBlocks)
			b.setType(plugin.floorBlock);
		for (Entity p : lift.passengers){
			plugin.fallers.remove(p);
			if (p instanceof Player){
				Player pl = (Player) p;
				if (!plugin.flyers.contains(pl))
					pl.setAllowFlight(false);
				//((Player) p).setAllowFlight(plugin.serverFlight);
				if (plugin.useAntiCheat)
					AnticheatAPI.unexemptPlayer((Player) p, CheckType.FLY);
			}
			if (plugin.useSpout){
				if (p instanceof Player){
					SpoutManager.getPlayer((Player) p).setGravityMultiplier(1);
					SpoutManager.getPlayer((Player) p).setCanFly(false);
				}					
			}
		}
		lifts.remove(lift);
		plugin.getServer().getScheduler().cancelTask(lift.taskid);
		lift.clear();
	}
	
	public static void removePlayer(Player player){
		for (Elevator elevator : lifts){
			if (elevator.passengers.contains(player))
				elevator.passengers.remove(player);
			if (plugin.fallers.contains(player))
				plugin.fallers.remove(player);
			if (plugin.flyers.contains(player))
				plugin.flyers.remove(player);
		}
	}
	
}
