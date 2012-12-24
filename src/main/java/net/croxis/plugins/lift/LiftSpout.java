package net.croxis.plugins.lift;

import java.util.logging.Level;

import org.spout.api.UnsafeMethod;
import org.spout.api.plugin.CommonPlugin;

public class LiftSpout extends CommonPlugin{

	private boolean debug = false;
	public static ElevatorManager manager;

	@Override
	@UnsafeMethod
	public void onDisable() {
		// TODO Auto-generated method stub
		
	}

	@Override
	@UnsafeMethod
	public void onEnable() {
		//manager = new ElevatorManager(this);
		
	}
	
	public void logDebug(String message){
		if (debug )
			this.getLogger().log(Level.INFO, "[DEBUG] " + message);
	}
	
	public void logInfo(String message){
		this.getLogger().log(Level.INFO, message);
	}

}
