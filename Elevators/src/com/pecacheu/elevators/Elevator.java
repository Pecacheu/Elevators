//Copyright (©) 2016, Pecacheu (Bryce Peterson, bbryce.com), All Rights Reserved.
//Pecacheu's Elevator Plugin!

package com.pecacheu.elevators;

import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Elevator {
	Floor floor; ChuList<ChuList<Block>> sList; ChuList<ChuList<Block>> csList;
	
	public Elevator(Floor _floor, ChuList<ChuList<Block>> _sList, ChuList<ChuList<Block>> _csList) {
		floor = _floor; if(_sList==null) sList = new ChuList<ChuList<Block>>(); else sList = _sList;
		if(_csList==null) csList = new ChuList<ChuList<Block>>(); else csList = _csList;
	}
	
	//------------------- Elevator Sign Functions -------------------
	
	//Determine Elevator Sign Positions:
	//Y coordinate is optional and can be set to 0.
	public static ChuList<Block> findSigns(Location loc) {
		World world=loc.getWorld(); int xPos=(int)loc.getX(), zPos=(int)loc.getZ(), bY=(int)loc.getY();
		ChuList<Block> a = new ChuList<Block>(); for(int h=0; h<256; h++) {
			Block bl = world.getBlockAt(xPos, h, zPos);
			if(bl.getType() == Material.WALL_SIGN && (Conf.TITLE.equals(Conf.lines(bl)[0]) || (bY!=0 && h == bY))) a.push(bl);
		} return a;
	}
	
	//Find elevator for sign, if any:
	public static Elevator findElev(Block sign) {
		Object[] eKeys = Conf.elevators.keySet().toArray(); Location sLoc = sign.getLocation();
		for(int m=0,k=eKeys.length; m<k; m++) { ChuList<ChuList<Block>> sList = Conf.elevators.get(eKeys[m]).sList;
			for(int i=0,l=sList.length; i<l; i++) for(int b=0,q=sList.get(i).length; b<q; b++)
			if(sList.get(i).get(b).getLocation().equals(sLoc)) return Conf.elevators.get(eKeys[m]);
		} return null;
	}
	
	//Determine if sign is in an existing elevator, as well as other things:
	//In fact, this is just most of the logic and processing for the [elevator] place event:
	public static Object inElev(Block sign, ChuList<Block> sList) {
		Object[] eKeys = Conf.elevators.keySet().toArray(); String cW = sign.getWorld().getName(); int cX = sign.getX(), cZ = sign.getZ();
		for(int m=0,k=eKeys.length; m<k; m++) {
			Floor fl = Conf.elevators.get(eKeys[m]).floor; String world = fl.world.getName();
			if(cW == world && (cX >= fl.xMin && cX <= fl.xMax) && (cZ >= fl.zMin && cZ <= fl.zMax)) { //Check if in elevator:
				if(fl.moving) return "cancel"; //Cancel if elevator is currently moving.
				ChuList<ChuList<Block>> sGroups = Conf.elevators.get(eKeys[m]).sList;
				for(int i=0,l=sGroups.length; i<l; i++) { //Check if in known column:
					Conf.elevators.get(eKeys[m]).sList.set(i, sList);
					//If new sign is in existing column, re-create other columns to match:
					if(sGroups.get(i).get(0).getX() == cX && sGroups.get(i).get(0).getZ() == cZ) {
						Conf.elevators.get(eKeys[m]).sList.set(i, sList); for(int v=0; v<l; v++) if(v!=i) {
							ChuList<Block> oSigns=sGroups.get(v); int sX=oSigns.get(0).getX(), sZ=oSigns.get(0).getZ();
							byte sData=oSigns.get(0).getData(); ChuList<Block> nSigns = new ChuList<Block>();
							for(int s=0,h=oSigns.length; s<h; s++) oSigns.get(s).setType(Conf.AIR);
							for(int s=0,h=sList.length; s<h; s++) { nSigns.set(s, fl.world.getBlockAt(sX, sList.get(s).getY(), sZ)); nSigns.get(s).setType(Material.WALL_SIGN);
							nSigns.get(s).setData(sData); Conf.setSign(nSigns.get(s), Conf.lines(sList.get(s))); } Conf.elevators.get(eKeys[m]).sList.set(v, nSigns);
						} return Conf.elevators.get(eKeys[m]).sList.get(0);
					}
				} return eKeys[m];
			}
		} return null;
	}
	
	//------------------- Call Sign Functions -------------------
	
	//Determine Elevator Call Sign Positions:
	public static ChuList<ChuList<Block>> findCallSigns(Elevator elev, Location newLoc) {
		Floor fl = elev.floor; World world = fl.world; ChuList<Block> sList = elev.sList.get(0);
		Predicate<Block> checkSign = (bl) -> { return (bl.getType() == Material.WALL_SIGN &&
		(Conf.CALL.equals(Conf.lines(bl)[0]) || (newLoc!=null && bl.getLocation().equals(newLoc)))); };
		ChuList<ChuList<Block>> csList = new ChuList<ChuList<Block>>();
		for(int j=0,g=sList.length; j<g; j++) { //Itterate through floors:
			csList.set(j, new ChuList<Block>()); //Scan perimeter for call signs:
			for(int xP=fl.xMin-1; xP<fl.xMax+1; xP++) { Block bl = world.getBlockAt(xP, sList.get(j).getY(), fl.zMin-2); if(checkSign.test(bl)) csList.get(j).push(bl); }
			for(int zP=fl.zMin-1; zP<fl.zMax+1; zP++) { Block bl = world.getBlockAt(fl.xMax+1, sList.get(j).getY(), zP); if(checkSign.test(bl)) csList.get(j).push(bl); }
			for(int xP=fl.xMax; xP>fl.xMin-2; xP--) { Block bl = world.getBlockAt(xP, sList.get(j).getY(), fl.zMax+1); if(checkSign.test(bl)) csList.get(j).push(bl); }
			for(int zP=fl.zMax; zP>fl.zMin-2; zP--) { Block bl = world.getBlockAt(fl.xMin-2, sList.get(j).getY(), zP); if(checkSign.test(bl)) csList.get(j).push(bl); }
		} return csList;
	} public static ChuList<ChuList<Block>> findCallSigns(Elevator elev) { return findCallSigns(elev, null); }
	
	//Find elevator for call sign, if any:
	public static CSData findElevCallSign(Block sign) {
		Object[] eKeys = Conf.elevators.keySet().toArray(); String sW = sign.getWorld().getName(); Location loc = sign.getLocation();
		BiPredicate<Integer,Integer> checkElev = (x,z) -> { return (loc.getX() == x && loc.getZ() == z); };
		for(int m=0,k=eKeys.length; m<k; m++) { Floor fl = Conf.elevators.get(eKeys[m]).floor; if(sW == fl.world.getName()) {
			Elevator elev = Conf.elevators.get(eKeys[m]);
			for(int j=0,g=elev.sList.get(0).length; j<g; j++) if(loc.getY() == elev.sList.get(0).get(j).getY()) {
				for(int xP=fl.xMin-1; xP<fl.xMax+1; xP++) if(checkElev.test(xP, fl.zMin-2)) return new CSData(elev, j);
				for(int zP=fl.zMin-1; zP<fl.zMax+1; zP++) if(checkElev.test(fl.xMax+1, zP)) return new CSData(elev, j);
				for(int xP=fl.xMax; xP>fl.xMin-2; xP--) if(checkElev.test(xP, fl.zMax+1)) return new CSData(elev, j);
				for(int zP=fl.zMax; zP>fl.zMin-2; zP--) if(checkElev.test(fl.xMin-2, zP)) return new CSData(elev, j);
				break;
			}
		}} return null;
	}
	
	//Update all elevator call signs:
	//fLvl: Current elevator height, fDir: Direction (or set to 2 for doors-closed but not moving), sNum: Destination floor, if any.
	public static ChuList<String> updateCallSigns(Elevator elev, double fLvl, int fDir, int sNum) {
		ChuList<Block> sList = elev.sList.get(0); ChuList<ChuList<Block>> csList = elev.csList; boolean locked = elev.floor.moving;
		ChuList<String> csInd = new ChuList<String>(sList.length); for(int m=0,k=sList.length,fNum=-1; m<k; m++) {
			String ind = Conf.NOMV; if(fNum == -1 && sList.get(m).getY() >= fLvl) fNum = m;
			if(m == fNum) ind = (fDir==2||locked) ? Conf.M_ATLV : Conf.ATLV; //Elevator is on level.
			else if(locked) { if(fDir>0) { //Going Up.
				if(m > fNum && m == sNum) ind = Conf.C_UP; //Elevator is below us and going to our floor.
				else ind = Conf.UP; //Elevator is above us or not going to our floor.
			} else { //Going Down.
				if(fNum == -1 && m == sNum) ind = Conf.C_DOWN; //Elevator is above us and going to our floor.
				else ind = Conf.DOWN; //Elevator is below us or not going to our floor.
			}}
			csInd.set(m, ind);
			if(csList.get(m)!=null) for(int i=0,l=csList.get(m).length; i<l; i++) Conf.setLine(csList.get(m).get(i), 3, ind);
		} return csInd;
	} public static ChuList<String> updateCallSigns(Elevator elev, double fLvl, int fDir) { return updateCallSigns(elev, fLvl, fDir, 0); }
	public static ChuList<String> updateCallSigns(Elevator elev, double fLvl) { return updateCallSigns(elev, fLvl, 0, 0); }
	
	//------------------- Elevator Functions -------------------
	
	//Move elevator car from fLevel to sLevel:
	//Speed is in blocks-per-second.
	public static void gotoFloor(Elevator elev, int fLevel, int sLevel, int selNum, int speed, Main plugin) {
		Floor floor = elev.floor; double step = speed * (Conf.MOVE_RES/1000) * (sLevel>fLevel?1:-1);
		updateCallSigns(elev, fLevel, 2); floor.moving = true; setDoors(floor, fLevel+2, false);
		plugin.setTimeout(() -> {
			int fID = Floor.addFloor(elev, fLevel, true);
			GotoTimer timer = new GotoTimer(); timer.set(plugin, elev, fLevel, sLevel, selNum, step, fID);
			BukkitTask fTmr = plugin.setTimeout(timer, Conf.MOVE_RES); timer.self = fTmr;
		}, 500);
	}
	
	//Find elevator for player, if any:
	public static Elevator playerInElev(Player player) {
		Object[] eKeys = Conf.elevators.keySet().toArray(); String pW = player.getWorld().getName(); Location pLoc = player.getLocation();
		for(int m=0,k=eKeys.length; m<k; m++) { Elevator elev = Conf.elevators.get(eKeys[m]); Floor fl = elev.floor; if(pW == fl.world.getName()) {
			int yMin = elev.sList.get(0).get(0).getY()-2, yMax = elev.sList.get(0).get(elev.sList.get(0).length-1).getY()+1;
			if((pLoc.getX() >= fl.xMin && pLoc.getX() <= fl.xMax) && (pLoc.getY() >= yMin && pLoc
			.getY() <= yMax) && (pLoc.getZ() >= fl.zMin && pLoc.getZ() <= fl.zMax)) return elev;
		}} return null;
	}
	
	//Remove all blocks in elevator:
	public static void resetElevator(Elevator elev, Block ignore, boolean noFloor) { //TODO DELETES KNOWN SIGNS
		Floor fl = elev.floor; int yMin = elev.sList.get(0).get(0).getY()-2, yMax = elev.sList.get(0).get(elev.sList.get(0).length-1).getY()+1;
		World world = fl.world;
		for(int y=yMin; y<yMax; y++) for(int x=fl.xMin; x<fl.xMax; x++) for(int z=fl.zMin; z<fl.zMax; z++) {
			Block bl = world.getBlockAt(x, y, z); if(y == yMin && !noFloor) bl.setType(fl.fType);
			else if(bl.getType() != Material.WALL_SIGN || !isKnownSign(bl, elev)) if(ignore==null || !bl.getLocation
			().equals(ignore.getLocation())) {BlockState s=bl.getState();s.setType(Conf.AIR);s.update(true);}
		}
	} public static void resetElevator(Elevator elev) { resetElevator(elev, null, false); }
	
	//Check if sign is a registered 'elevator' sign:
	public static boolean isKnownSign(Block sign, Elevator elev) {
		for(int i=0,l=elev.sList.length; i<l; i++) for(int k=0,b=elev.sList.get(i).length; k<b;
		k++) if(elev.sList.get(i).get(k).getLocation().equals(sign.getLocation())) return true;
		return false;
	}
	
	//Open/Close Elevator Doors:
	public static void setDoors(Floor fl, int h, boolean onOff) {
		BiPredicate<Integer,Integer> isCorner = (x,z) -> { return ((fl.xMax-fl.xMin<=2) ? (x < fl.xMin || x > fl.xMax-1) : (x <= fl
		.xMin || x >= fl.xMax-1)) && ((fl.zMax-fl.zMin<=2) ? (z < fl.zMin || z > fl.zMax-1) : (z <= fl.zMin || z >= fl.zMax-1)); };
		//Open/Close Doors and Barrier-Doors:
		Consumer<Block> setDoor = (d) -> { if(Conf.isDoor(d)) { byte dat = d.getData(); if(onOff &&
		dat < 4) d.setData((byte)(dat+4)); else if(!onOff && dat >= 4) d.setData((byte)(dat-4)); }};
		Consumer<Block> setBDoor = (bl) -> { if(isCorner.test(bl.getX(),bl.getZ())) { if(bl.getType() == Conf.AIR) bl.setType
		(Conf.DOOR_SET); } else if(bl.getType() == (onOff?Conf.DOOR_SET:Conf.AIR)) bl.setType(onOff?Conf.AIR:Conf.DOOR_SET); };
		//Cycle Around Elevator Perimeter:
		World world = fl.world; for(int yP=h-1; yP<=h+1; yP++) {
			for(int xP=fl.xMin; xP<fl.xMax+1; xP++) { Block bl = world.getBlockAt(xP, yP, fl.zMin-1); setBDoor.accept(bl); if(yP==h-1) setDoor.accept(bl); }
			for(int zP=fl.zMin; zP<fl.zMax+1; zP++) { Block bl = world.getBlockAt(fl.xMax, yP, zP); setBDoor.accept(bl); if(yP==h-1) setDoor.accept(bl); }
			for(int xP=fl.xMax-1; xP>fl.xMin-2; xP--) { Block bl = world.getBlockAt(xP, yP, fl.zMax); setBDoor.accept(bl); if(yP==h-1) setDoor.accept(bl); }
			for(int zP=fl.zMax-1; zP>fl.zMin-2; zP--) { Block bl = world.getBlockAt(fl.xMin-1, yP, zP); setBDoor.accept(bl); if(yP==h-1) setDoor.accept(bl); }
		}
	}
}

class GotoTimer extends BukkitRunnable {
	public BukkitTask self;
	private Main plugin; private Elevator elev; private Floor floor;
	private int fLevel, sLevel, selNum, fID; private double fPos, step; private byte mFlr = 3;
	public void set(Main plugin, Elevator elev, int fLevel, int sLevel, int selNum, double step, int fID) {
		this.plugin = plugin; this.elev = elev; this.floor = elev.floor; this.step = step; this.fPos
		= fLevel; this.fLevel = fLevel; this.sLevel = sLevel; this.selNum = selNum; this.fID = fID;
	}
	public void run() {
		//if(mFlr == 3) { moveFloor(fID, elev, fPos); }
		if(mFlr >= 3) { Floor.moveFloor(fID, elev, fPos); Elevator.updateCallSigns(elev, fPos, (sLevel>fLevel)?1:0, selNum); mFlr = 0; }
		Floor.moveMobs(elev, fPos); mFlr++;
		if(sLevel>fLevel?(fPos >= sLevel):(fPos <= sLevel)) {
			self.cancel(); Floor.deleteFloor(fID, elev);
			Floor.addFloor(elev, sLevel, false); plugin.setTimeout(() -> { Floor.addFloor(elev, sLevel, false); },50);
			Elevator.updateCallSigns(elev, sLevel+2);
			plugin.setTimeout(() -> {
				Elevator.setDoors(floor, sLevel+2, true); floor.moving = false; Elevator.updateCallSigns(elev, sLevel+2);
				if(Conf.CLTMR != null) Conf.CLTMR.cancel();
				Conf.CLTMR = plugin.setTimeout(() -> { Elevator.setDoors(floor, sLevel+2, false); Conf.CLTMR = null; }, Conf.DOOR_HOLD);
			}, 500);
		} else fPos += step;
	}
}

class CSData {
	Elevator elev; int index;
	public CSData(Elevator _elev, int _index) {
		elev = _elev; index = _index;
	}
}