//Copyright (©) 2016, Pecacheu (Bryce Peterson, bbryce.com), All Rights Reserved.
//Pecacheu's Elevator Plugin!

package com.pecacheu.elevators;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;

public class Floor {
	public World world; public int xMin, zMin, xMax, zMax; public Material fType; public boolean moving;
	
	public Floor(World _world, int _xMin, int _zMin, int _xMax, int _zMax, Material _fType, boolean _moving) {
		world = _world; xMin = _xMin; zMin = _zMin; xMax = _xMax; zMax = _zMax; fType = _fType; moving = _moving;
	}
	
	//Determine Elevator Floor Size:
	public static Floor findFloor(Block b) {
		World world=b.getWorld(); int bX=b.getX(), h=b.getY()-2, bZ=b.getZ(); Material fType=world.getBlockAt(bX, h, bZ).getType();
		if(Conf.BLOCKS.indexOf(fType.toString()) == -1 || world.getBlockAt(bX, h+1, bZ).getType() != Conf.AIR) return null;//throw new Exception("Floor Not Found!");
		int xP=1, xN=1, zP=1, zN=1; String face=((org.bukkit.material.Sign)b.getState().getData()).getFacing().toString();
		if(face !=  "WEST") while(xP <= Conf.RADIUS_MAX) { if(world.getBlockAt(bX+xP, h, bZ).getType() != fType) break; xP++; }
		if(face !=  "EAST") while(xN <= Conf.RADIUS_MAX) { if(world.getBlockAt(bX-xN, h, bZ).getType() != fType) break; xN++; }
		if(face != "NORTH") while(zP <= Conf.RADIUS_MAX) { if(world.getBlockAt(bX, h, bZ+zP).getType() != fType) break; zP++; }
		if(face != "SOUTH") while(zN <= Conf.RADIUS_MAX) { if(world.getBlockAt(bX, h, bZ-zN).getType() != fType) break; zN++; }
		
		if(xP > Conf.RADIUS_MAX || xN > Conf.RADIUS_MAX || zP > Conf.RADIUS_MAX || zN > Conf.RADIUS_MAX) return null;//throw new Exception("Floor Not Found!");
		int xPos=bX-xN+1, zPos=bZ-zN+1, length=xP+xN-1, width=zP+zN-1;
		return new Floor(world, xPos, zPos, xPos+length, zPos+width, fType, false);
	}
	
	//------------------- Floor Movement Functions -------------------
	
	//Creates Floor or MovingFloor at height 'h'.
	//If MovingFloor, returns new floorID.
	//Deletes existing floor blocks unless 'dontDelete' is true.
	public static int addFloor(Elevator elev, double h, boolean isMoving, boolean dontDelete, Integer forceID) {
		Floor fl = elev.floor; World world = fl.world;
		if(!dontDelete) { removeFallingBlocks(elev); Elevator.resetElevator(elev,null,true); }
		if(isMoving) { //Create FallingBlock Floor:
			ChuList<FallingBlock> blocks = new ChuList<FallingBlock>(); fl.moving = true;
			for(int x=fl.xMin; x<fl.xMax; x++) for(int z=fl.zMin; z<fl.zMax; z++) {
				blocks.push(fallingBlock(world, x, h, z, fl.fType));
			}
			int ind; if(forceID!=null) ind = forceID;
			else ind = Conf.findFirstEmpty(Conf.movingFloors);
			Conf.movingFloors.set(ind, blocks); return ind;
		} else { //Create Solid Floor:
			for(int x=fl.xMin; x<fl.xMax; x++) for(int z=fl.zMin; z<fl.zMax; z++) {
				Block bl = world.getBlockAt(x, (int)h, z); bl.setType(fl.fType);
			}
		} return 0;
	} public static int addFloor(Elevator elev, double h, boolean isMoving, boolean dontDelete) { return addFloor(elev, h, isMoving, dontDelete, null); }
	public static int addFloor(Elevator elev, double h, boolean isMoving) { return addFloor(elev, h, isMoving, false, null); }
	
	//Moves a MovingFloor using floorID.
	public static void moveFloor(int floorID, Elevator elev, double h) {
		if(Conf.movingFloors.get(floorID)!=null) {
			ChuList<FallingBlock> bList = Conf.movingFloors.get(floorID);
			for(int i=0,l=bList.length; i<l; i++) bList.get(i).remove();
			addFloor(elev, h, true, true, floorID);
		}
	}
	
	//Moves Players/Animals/NPCs With Elevator Floor.
	public static void moveMobs(Elevator elev, double h) {
		Floor fl = elev.floor; World world = fl.world; int yMin = elev.sList.get(0).get(0).getY()-2, yMax = elev.sList.get(0).get(elev.sList.get(0).length-1).getY()+1;
		Object[] eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
		for(int i=0,l=eList.length; i<l; i++) { Location loc = ((Entity)eList[i]).getLocation();
			if((loc.getX() >= fl.xMin && loc.getX() <= fl.xMax) && (loc.getY() >= yMin && loc.getY() <= yMax) && (loc.getZ() >= fl.zMin && loc.getZ() <= fl.zMax))
			{ ((Entity)eList[i]).setGravity(false); ((Entity)eList[i]).teleport(new Location(world, loc.getX(), h+1, loc.getZ(), loc.getYaw(), loc.getPitch())); }
		}
	}
	
	//Deletes a MovingFloor instance.
	public static void deleteFloor(int floorID, Elevator elev) {
		if(Conf.movingFloors.get(floorID)!=null) {
			ChuList<FallingBlock> bList = Conf.movingFloors.get(floorID);
			for(int i=0,l=bList.length; i<l; i++) bList.get(i).remove();
		} Conf.movingFloors.set(floorID, null);
		//Restore Gravity to LivingEntities:
		Floor fl = elev.floor; World world = fl.world; int yMin = elev.sList.get(0).get(0).getY()-2, yMax = elev.sList.get(0).get(elev.sList.get(0).length-1).getY()+1;
		Object[] eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
		for(int i=0,l=eList.length; i<l; i++) { Location loc = ((Entity)eList[i]).getLocation(); if((loc.getX() >= fl.xMin && loc.getX() <= fl.xMax)
		&& (loc.getY() >= yMin && loc.getY() <= yMax) && (loc.getZ() >= fl.zMin && loc.getZ() <= fl.zMax)) ((Entity)eList[i]).setGravity(true); }
	}
	
	//Calculates Current Floor/MovingFloor Height.
	public static int getLevel(Elevator elev, boolean noTypeCheck) {
		ChuList<Block> sList = elev.sList.get(0); World world = sList.get(0).getWorld();
		int yMin = sList.get(0).getY()-2, yMax = sList.get(sList.length-1).getY()+1, xPos = sList.get(0).getX(), zPos = sList.get(0).getZ();
		for(int y=yMin; y<yMax; y++) { Block bl = world.getBlockAt(xPos, y, zPos);
			if(noTypeCheck ? (Conf.BLOCKS.indexOf(bl.getType().toString()) != -1) : (bl.getType() == elev.floor.fType)) return bl.getY();
		} return yMin;
	} public static int getLevel(Elevator elev) { return getLevel(elev, false); }
	
	//------------------- FallingBlock Management Functions -------------------
	
	public static FallingBlock fallingBlock(World world, double x, double y, double z, Material type) {
		FallingBlock falling = world.spawnFallingBlock(new Location(world,
		x, y, z), type, (byte)0); falling.setGravity(false); return falling;
	}
	
	public static void removeFallingBlocks(Elevator elev) {
		Floor fl = elev.floor; World world = fl.world; ChuList<Block> sList = elev.sList.get(0);
		int yMin = sList.get(0).getY()-2, yMax = sList.get(sList.length-1).getY()+1;
		Object[] el = world.getEntitiesByClass(org.bukkit.entity.FallingBlock.class).toArray();
		for(int i=0,l=el.length; i<l; i++) {
			Location loc = ((Entity)el[i]).getLocation();
			if((loc.getX() >= fl.xMin-0.5 && loc.getX() <= fl.xMax+0.5) && (loc.getY() >= yMin-0.5 && loc
			.getY() <= yMax+0.5) && (loc.getZ() >= fl.zMin-0.5 && loc.getZ() <= fl.zMax+0.5)) ((Entity)el[i]).remove();
		}
	}
}