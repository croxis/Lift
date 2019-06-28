package net.croxis.plugins.lift;

import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.vehicle.VehicleExitEvent;

public class BukkitVehicleListener implements Listener {
    private final BukkitLift plugin;

    public BukkitVehicleListener(BukkitLift plugin) {
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityEject(VehicleExitEvent event){
        LivingEntity ejector = event.getExited();
        if (BukkitElevatorManager.isInALift(event.getVehicle())) {
            if (ejector instanceof Player) {
                ejector.sendMessage(BukkitConfig.stringUnsafe);
            }
            event.setCancelled(true);
            this.plugin.logDebug("Canceled ejection for " + ejector);
        }
    }
}
