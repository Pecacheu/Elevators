//Copyright (©) 2016, Pecacheu (Bryce Peterson, bbryce.com), All Rights Reserved.
//Pecacheu's Elevator Plugin!

//Please note that this plugin's Java Classes are structured very
//abnormally due to being painstakingly translated from JavaScript.

//replace == and != of any kind with .equals

package com.pecacheu.elevators;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Main extends JavaPlugin implements Listener {
	static final String PERM_USE    = "elevators.use";
	static final String PERM_CREATE = "elevators.create";
	static final String PERM_RELOAD = "elevators.reload";
	
	private BukkitTask svTmr = null;
	
	@Override
	public void onEnable() {
		Conf.initDefaults();
		//Config Auto-save Timer:
		setTimeout(() -> {
			String err = Conf.loadConfig(); if(err=="1") Conf.saveConfig(this);
			else if(err!=null) Bukkit.getServer().getConsoleSender().sendMessage(Conf.MSG_ERR_CONF+"\n"+err);
			else Conf.saveConfig(this);
			svTmr = setInterval(() -> { Conf.saveConfig(this); }, Conf.SAVE_INT*60000);
		}, 200);
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	@Override
	public void onDisable() {
		if(svTmr != null) { svTmr.cancel(); svTmr = null; }
		HandlerList.unregisterAll();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(command.getName().equalsIgnoreCase("elev")) {
			if(args.length == 1 && args[0].equalsIgnoreCase("reload")) {
				setTimeout(() -> {
					String err = Conf.loadConfig(); if(err=="1") { if(sender instanceof
					Player) sender.sendMessage(Conf.MSG_NEW_CONF); Conf.saveConfig(this); }
					else if(err!=null) { Bukkit.getServer().getConsoleSender().sendMessage(Conf.MSG_ERR_CONF+"\n"+err);
					if(sender instanceof Player) sender.sendMessage(Conf.MSG_ERR_CONF); }
					else Conf.saveConfig(this);
					sender.sendMessage("§aElevators Plugin Reloaded!");
				}, 200);
			} else sender.sendMessage("§cUsage: /elev reload");
			return true;
		}
		return false;
	}
	
	//JavaScript-like Timer Functionality:
	//Call .cancel() on the returned value to cancel.
	
	public BukkitTask setTimeout(Runnable function, long millis) {
		return new BukkitRunnable() { public void run() {
			function.run();
		}}.runTaskLater(this, millis/50);
		//return function.runTaskLater(this, millis/50);
	} public BukkitTask setTimeout(BukkitRunnable function, long millis) { return function.runTaskLater(this, millis/50); }
	
	public BukkitTask setInterval(Runnable function, long millis) {
		long t=millis/50; return new BukkitRunnable() { public void run() {
			function.run();
		}}.runTaskTimer(this, t, t);
	} public BukkitTask setInterval(BukkitRunnable function, long millis) { long t=millis/50; return function.runTaskTimer(this, t, t); }
	
	//------------------- Elevator Create Sign Events -------------------
	
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onSignChange(SignChangeEvent event) { if(event.getBlock().getType() == Material.WALL_SIGN) {
	String tLine = event.getLines()[0]; if(tLine.equalsIgnoreCase("[call]") && Conf.hasPerm(event.getPlayer(), PERM_CREATE)) { //Call Signs:
		event.getPlayer().sendMessage("CALL SIGN");
		
		event.setLine(0, "> ERROR <"); //Print error message first in case of plugin error.
		event.setLine(1, ""); event.setLine(2, "");
		
		//Check For Elevator:
		CSData ret = Elevator.findElevCallSign(event.getBlock()); if(ret==null) { event.setLine(0, Conf.ERROR); return; }
		Elevator elev = ret.elev; int csNum = ret.index;
		
		//Find All Call Signs:
		elev.csList = Elevator.findCallSigns(elev, event.getBlock().getLocation());
		
		//Update Call Signs:
		ChuList<String> iList = Elevator.updateCallSigns(elev, Floor.getLevel(elev));
		event.setLine(0, Conf.CALL); event.setLine(3, iList.get(csNum));
		
	} else if(tLine.equalsIgnoreCase("[elevator]") && Conf.hasPerm(event.getPlayer(), PERM_CREATE)) { //Elevator Signs:
		
		event.setLine(0, "> ERROR <"); //Print error message first in case of plugin error.
		event.setLine(1, ""); event.setLine(2, "");
		
		//Find All Signs in Elevator:
		ChuList<Block> sList = Elevator.findSigns(event.getBlock().getLocation()); Block eIDItem = sList.get(0);
		event.getPlayer().sendMessage(sList.toString());
		
		//Check if Already in an Elevator:
		Object eRet = Elevator.inElev(event.getBlock(), sList); //<- This type-handling insanity courtesy of JavaScript.
		if(eRet instanceof ChuList) eIDItem = ((ChuList<Block>)eRet).get(0); else if
		(eRet == "cancel") { event.setCancelled(true); return; } else if(eRet != null) { //Sign is in New Column:
			event.getPlayer().sendMessage("NEW COLUMN");
			Elevator nElev=Conf.elevators.get(eRet); World world=sList.get(0).getWorld(); int sX=sList.get(0).getX(), sZ=sList
			.get(0).getZ(); byte sData=sList.get(0).getData(); ChuList<Block> oSigns=nElev.sList.get(0), nSigns=new ChuList<Block>();
			for(int s=0,h=sList.length; s<h; s++) sList.get(s).setType(Conf.AIR);
			for(int i=0,l=oSigns.length; i<l; i++) { nSigns.set(i, world.getBlockAt(sX, oSigns.get(i).getY(), sZ)); nSigns.get(i)
			.setType(Material.WALL_SIGN); nSigns.get(i).setData(sData); Conf.setSign(nSigns.get(i), Conf.lines(oSigns.get(i))); }
			nElev.sList.push(nSigns); setTimeout(() -> { Conf.saveConfig(this); }, 200); //Save Changes To Config.
			return;
		}
		
		//Detect Elevator Floor Size:
		Floor floor = Floor.findFloor(eIDItem); if(floor==null) { for(int i=0, l=sList.size
		(); i<l; i++) Conf.setError(sList.get(i)); event.setLine(0, Conf.ERROR); return; }
		
		//Generate New Elevator ID And Store Elevator Location:
		Elevator elev; String eID = Conf.locToString(new Location(floor.world,floor.xMin,0,floor.zMin));
		if(eRet==null) { event.getPlayer().sendMessage("NEW ELEV: "+eID); ChuList<ChuList<Block>> sGroups = new ChuList<ChuList<Block>>(); sGroups.push(sList);
		elev = new Elevator(floor, sGroups, null); Conf.elevators.put(eID, elev); } //Create New Elevator Object.
		else { elev = Conf.elevators.get(eID); event.getPlayer().sendMessage("NOT NEW ELEV: "+eID); }
		
		//Remove Elevator Obstructions:
		Floor.removeFallingBlocks(elev); Elevator.resetElevator(elev, event.getBlock(), false);
		
		//Validate Sign Placment:
		boolean cErr = false; for(int i=0; i<sList.length; i++) {
			if(i > 0 && (sList.get(i).getY() - sList.get(i-1).getY()) < 3) { cErr = Conf.setError(sList.get(i), event.getBlock()); sList.remove(i); i--; } //Signs too close!
			else { Floor.addFloor(elev, sList.get(i).getY()-2, false, true); Elevator.setDoors(floor, sList.get(i).getY(), i==0); }
		}
		
		//Update Sign Level Numbers for Non-Custom-Named Signs:
		for(int k=0,m=elev.sList.length; k<m; k++) for(int f=0,d=elev.sList.get(k).length; f<d; f++) {
			Block sign = elev.sList.get(k).get(f);
			if(sign.getLocation().equals(event.getBlock().getLocation())) { if(event.getLines()[3].length()==0) event.setLine(3, "Level "+(f+1)); }
			else if(Conf.lines(sign)[3].matches("^Level [0-9]+$")) Conf.setLine(sign, 3, "Level "+(f+1));
			else if(sign.getY() == event.getBlock().getY()) Conf.setSign(sign, event.getLines());
		}
		
		//Reset Gravity of LivingEntities in Elevator:
		Floor fl = floor; World world = fl.world; int yMin = elev.sList.get(0).get(0).getY()-2, yMax = elev.sList.get(0).get(elev.sList.get(0).length-1).getY()+1;
		Object[] eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
		for(int i=0,l=eList.length; i<l; i++) { Location loc = ((Entity)eList[i]).getLocation(); if((loc.getX() >= fl.xMin && loc.getX() <= fl.xMax)
		&& (loc.getY() >= yMin && loc.getY() <= yMax) && (loc.getZ() >= fl.zMin && loc.getZ() <= fl.zMax)) ((Entity)eList[i]).setGravity(true); }
		
		if(cErr) event.setLine(0, Conf.ERROR); else event.setLine(0, Conf.TITLE);
		setTimeout(() -> { Conf.saveConfig(this); }, 200); //Save Changes To Config.
	}}}
	
	//------------------- Elevator Destroy Sign Events -------------------
	
	//@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	//events.blockBreak(function(event) { if() {
		//CANCEL IF NOT CORRECT PERMISSIONS.
		//Breaking an elevator sign deletes the whole column of signs.
		//Breaking the sign when only one column is left should delete sign & blocks only on level but add glass below player. Should probably open level's doors, too.
		//Use getBlockBelowPlayer(event.player) Copy function to here so it works without PecacheuLib.
		//Cancel break/place if elevator is moving.
		//Save if elevator sign destroyed.
		//For call signs, all you have to do is: elevators[eID][0][7] = findCallSigns(eID);
	//}});
	
	//------------------- Elevator Block-Clicking Events -------------------
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) { if(event.getItem()==null) {
		String act = event.getAction().toString(); if(act == "RIGHT_CLICK_BLOCK" && !event.isCancelled()
		&& event.getClickedBlock().getType() == Material.WALL_SIGN) { Elevator elev = Elevator.findElev(event
		.getClickedBlock()); if(elev!=null && !elev.floor.moving && Conf.hasPerm(event.getPlayer(), PERM_USE)) { //Select Floor:
			ChuList<Block> dsList = elev.sList.get(0);
			
			//Get Selected Floor:
			String selName = Conf.lines(dsList.get(0))[1]; int selNum = 0; if(selName.length()!=0)
			selName = selName.substring(Conf.L_ST.length(),selName.length()-Conf.L_END.length());
			
			//Get List of Floor Names:
			ChuList<String> flNames = new ChuList<String>();
			for(int i=0,l=dsList.length; i<l; i++) { flNames.set(i, Conf.lines
			(dsList.get(i))[3]); if(selName.equals(flNames.get(i))) selNum = i; }
			
			//Update Floor Number:
			if(event.getPlayer().isSneaking()) { selNum--; if(selNum < 0) selNum = flNames.length-1; }
			else { selNum++; if(selNum >= flNames.length) selNum = 0; } String nFloor = Conf.L_ST+flNames.get(selNum)+Conf.L_END;
			for(int k=0,m=elev.sList.length; k<m; k++) for(int f=0,d=elev.sList.get(k).length; f<d; f++) Conf.setLine(elev.sList.get(k).get(f), 1, nFloor);
			
		} else if(elev==null) { CSData ret = Elevator.findElevCallSign(event.getClickedBlock());
		if(ret!=null && !ret.elev.floor.moving && Conf.hasPerm(event.getPlayer(), PERM_USE)) { //Call Sign Click:
			elev = ret.elev; int fLevel = Floor.getLevel(elev), sLevel = event.getClickedBlock().getY()-2;
			
			//Call elevator to floor:
			if(fLevel != sLevel) {
				event.getPlayer().sendMessage(Conf.MSG_CALL);
				Elevator.gotoFloor(elev, fLevel, sLevel, ret.index, Conf.TRAVEL_SPEED, this);
			} else {
				Elevator.setDoors(elev.floor, sLevel+2, true); if(Conf.CLTMR != null) Conf.CLTMR.cancel();
				final Elevator fElev = elev; /*final int fsLevel = sLevel;*/ Conf.CLTMR = this.setTimeout(()
				-> { Elevator.setDoors(fElev.floor, sLevel+2, false); Conf.CLTMR = null; }, Conf.DOOR_HOLD);
			} //Re-open doors if already on level.
			
		}}} else if(act == "LEFT_CLICK_AIR" || act == "RIGHT_CLICK_BLOCK") { Elevator elev = Elevator.playerInElev
		(event.getPlayer()); if(elev!=null && !elev.floor.moving && Conf.hasPerm(event.getPlayer(), PERM_USE)) { //Go To Floor:
			ChuList<Block> dsList = elev.sList.get(0);
			
			//Get Current And Slected Floors:
			int fLevel = Floor.getLevel(elev), flNum = -1, selNum = 0; String selName = Conf.lines(dsList.get(0))[1];
			if(selName.length()!=0) selName = selName.substring(Conf.L_ST.length(),selName.length()-Conf.L_END.length());
			
			for(int i=0,l=dsList.length; i<l; i++) { if(selName.equals(Conf.lines(dsList.get(i))[3])) selNum = i;
			if(flNum == -1 && dsList.get(i).getY() >= fLevel) flNum = i; } int sLevel = dsList.get(selNum).getY()-2;
			
			if(flNum != selNum) {
				event.getPlayer().sendMessage(Conf.MSG_GOTO_ST+selName+Conf.MSG_GOTO_END);
				Elevator.gotoFloor(elev, fLevel, sLevel, selNum, Conf.TRAVEL_SPEED, this);
			} else {
				Elevator.setDoors(elev.floor, sLevel+2, true); if(Conf.CLTMR != null) Conf.CLTMR.cancel();
				final Elevator fElev = elev; /*final int fsLevel = sLevel;*/ Conf.CLTMR = this.setTimeout(()
				-> { Elevator.setDoors(fElev.floor, sLevel+2, false); Conf.CLTMR = null; }, Conf.DOOR_HOLD);
			} //Re-open doors if already on level.
		}
	}}}
}