//Copyright (©) 2016, Pecacheu (Bryce Peterson, bbryce.com), All Rights Reserved.

//------------------- Config Related Functions -------------------

package com.pecacheu.elevators;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

//Note on Set vs List: Sets cannot contain duplicate elements, whereas Lists can.

public class Conf {
	//Global Variables:
	public static TreeMap<String,Elevator> elevators = new TreeMap<String,Elevator>();
	public static ChuList<ChuList<FallingBlock>> movingFloors = new ChuList<ChuList<FallingBlock>>();
	public static BukkitTask CLTMR = null;
	
	//Global Config Settings:
	public static String TITLE, CALL, ERROR, L_ST, L_END, MSG_GOTO_ST, MSG_GOTO_END, MSG_CALL, NOMV, M_ATLV, ATLV, C_UP, UP, C_DOWN, DOWN;
	public static int RADIUS_MAX, TRAVEL_SPEED, MOVE_RES, DOOR_HOLD, SAVE_INT; public static ChuList<String> BLOCKS; public static Material DOOR_SET;
	
	//Constants:
	public static final String MSG_NEW_CONF = "§e[Elevators] §bCould not load config. Creating new config file...";
	public static final String MSG_ERR_CONF = "§e[Elevators] §cError while loading config!";
	public static final String CONFIG_PATH = "plugins/Elevators/config.yml";
	public static final Material AIR = Material.AIR;
	
	public static MemoryConfiguration defaults = new MemoryConfiguration();
	public static void initDefaults() {
		defaults.set("title", "&1[&3Elevator&1]"); defaults.set("call", "&1[&3Call&1]");
		defaults.set("error", "[&4???&r]"); defaults.set("selStart", "&8> &5"); defaults.set("selEnd", " &8<");
		defaults.set("msgGotoStart", "&eTraveling to &a"); defaults.set("msgGotoEnd", "&e.");
		defaults.set("msgCall", "&eCalling elevator."); defaults.set("noMove",   "&4⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
		defaults.set("mAtLevel", "&3⦿  ⦿  ⦿  ⦿  ⦿  ⦿"); defaults.set("atLevel",  "&2⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
		defaults.set("callUp",   "&3▲  ▲  ▲  ▲  ▲  ▲"); defaults.set("up",       "&4△  △  △  △  △  △");
		defaults.set("callDown", "&3▼  ▼  ▼  ▼  ▼  ▼"); defaults.set("down",     "&4▽  ▽  ▽  ▽  ▽  ▽");
		defaults.set("floorMaxRadius", 8); defaults.set("travelSpeed", 25); defaults.set("updateDelay", 50);
		defaults.set("doorHoldTime", 4000); defaults.set("saveInterval", 15); defaults.set("doorBlock", "THIN_GLASS");
		defaults.set("blockList", java.util.Arrays.asList("IRON_BLOCK","GOLD_BLOCK","DIAMOND_BLOCK","EMERALD_BLOCK","GLASS"));
	}
	
	//------------------- Config Save & Load Functions -------------------
	
	public static boolean saveConfig(JavaPlugin plugin) {
		File path = new File(CONFIG_PATH); String data = "";
		
		//If Not Found, Create New Config File:
		if(!path.exists()) { data = newConfig(path, plugin); } else {
			//Read Current Config File:
			try {
				FileReader file = new FileReader(path); int p = 0;
				while(p < 3000) { int read = file.read(); if(read < 0 || read >=
				65535) break; data += fromCharCode(read); p++; } file.close();
			} catch (IOException e) { return false; }
		}
		
		if(data.length() == 0) return false;
		
		//Seperate And Overwrite BLOCKS Section:
		int bPos = data.lastIndexOf("blockList:"); String bStr = data.substring(bPos);
		
		int bEnd = 0, nl = 0; while(bEnd < 600) {
			if(nl==1 && bStr.charAt(bEnd) != '-') nl = 2;
			if(nl==2) { if(bStr.charAt(bEnd) == '\n') break; }
			else nl = (bStr.charAt(bEnd) == '\n') ? 1 : 0; bEnd++;
		} bEnd += bPos;
		
		//java.util.ArrayList<String> bList = new java.util.ArrayList<String>();
		//for(int i=0,l=BLOCKS.length; i<l; i++) bList.add(BLOCKS.get(i));
		
		YamlConfiguration bConf = new YamlConfiguration(); bConf.set("blockList", BLOCKS);
		data = data.substring(0,bPos)+bConf.saveToString()+data.substring(bEnd);
		
		//Seperate And Overwrite Elevators Section:
		int sPos = data.lastIndexOf("elevators:"); data = data.substring(0,sPos);
		YamlConfiguration eConf = new YamlConfiguration();
		
		ConfigurationSection eList = eConf.createSection("elevators"); Object[] eKeys = elevators.keySet().toArray();
		
		//Generate Compressed Elevator Data:
		for(int i=0,l=eKeys.length; i<l; i++) { //Data Format: [World, Signs1 X, Signs1 Z, Signs2 X, Signs2 Z...]
			Elevator elev = elevators.get(eKeys[i]); ChuList<String> eData = new ChuList<String>(); eData.push(elev.floor.world.getName());
			for(int k=0,d=elev.sList.length; k<d; k++) { eData.push(Integer.toString(elev.sList
			.get(k).get(0).getX())); eData.push(Integer.toString(elev.sList.get(k).get(0).getZ())); }
			eList.set((String)eKeys[i], eData);//.toArray());
		}
		
		//Append New Data And Save File:
		String eStr = eConf.saveToString(); eStr.substring(0,eStr.length()-1); data += eStr;
		FileWriter file; try { file = new FileWriter(path); file.write(data); file.close(); }
		catch (IOException e) { return false; } return true;
	}
	
	private static String newConfig(File file, JavaPlugin plugin) {
		Bukkit.getServer().getConsoleSender().sendMessage(MSG_NEW_CONF);
		Path path = file.toPath(); try { java.nio.file.Files.createDirectories(path.getParent()); }
		catch (IOException e) { return ""; } return unpackFile("config.yml", file, plugin);
	}
	
	public static String loadConfig() { try {
		File path = new File(CONFIG_PATH); YamlConfiguration config; boolean pathFound = path.exists();
		if(pathFound) config = YamlConfiguration.loadConfiguration(path); else config = new YamlConfiguration();
		
		config.setDefaults(defaults); movingFloors = new ChuList<ChuList<FallingBlock>>(); CLTMR = null;
		
		//Load Global Settings:
		TITLE = c(config.getString("title"));
		CALL = c(config.getString("call"));
		ERROR = c(config.getString("error"));
		L_ST = c(config.getString("selStart"));
		L_END = c(config.getString("selEnd"));
		
		MSG_GOTO_ST = c(config.getString("msgGotoStart"));
		MSG_GOTO_END = c(config.getString("msgGotoEnd"));
		MSG_CALL = c(config.getString("msgCall"));
		
		MSG_GOTO_ST = c(config.getString("msgGotoStart"));
		MSG_GOTO_END = c(config.getString("msgGotoEnd"));
		MSG_CALL = c(config.getString("msgCall"));
		
		NOMV = c(config.getString("noMove"));
		M_ATLV = c(config.getString("mAtLevel"));
		ATLV = c(config.getString("atLevel"));
		C_UP = c(config.getString("callUp"));
		UP = c(config.getString("up"));
		C_DOWN = c(config.getString("callDown"));
		DOWN = c(config.getString("down"));
		
		RADIUS_MAX = config.getInt("floorMaxRadius");
		TRAVEL_SPEED = config.getInt("travelSpeed");
		MOVE_RES = config.getInt("updateDelay");
		DOOR_HOLD = config.getInt("doorHoldTime");
		SAVE_INT = config.getInt("saveInterval");
		DOOR_SET = Material.valueOf(config.getString("doorBlock"));
		
		java.util.List<String> bList = config.getStringList("blockList");
		
		//Remove any items that are not solid blocks:
		BLOCKS = new ChuList<String>(); for(int b=0,g=bList.size(); b<g; b++)
		if(Material.valueOf(bList.get(b)).isSolid()) BLOCKS.push(bList.get(b));
		
		//Load Compressed Elevator Data:
		ConfigurationSection eList = config.getConfigurationSection("elevators");
		if(eList != null) { Object[] eKeys = eList.getKeys(false).toArray();
		elevators.clear(); for(int i=0,l=eKeys.length; i<l; i++) {
			Object[] eData = eList.getStringList((String)eKeys[i]).toArray();
			World world = Bukkit.getServer().getWorld((String)eData[0]);
			//Locate All Elevator Signs:
			ChuList<ChuList<Block>> sGroups = new ChuList<ChuList<Block>>(); for(int k=1,d=eData.length; k<d; k+=2) {
				ChuList<Block> sList = Elevator.findSigns(new Location(world, Integer
				.parseInt((String)eData[k]), 0, Integer.parseInt((String)eData[k+1])));
				if(sList.length > 0) sGroups.push(sList);
			}
			//Elevator elev = new Elevator();
			ChuList<Block> dsList = sGroups.get(0);
			if(dsList != null && dsList.get(0).getType() == Material.WALL_SIGN) {
				//Calculate Platform Height And Convert To Floor Number:
				//elevators.put(eKeys[i], elev);
				Elevator elev = new Elevator(null, sGroups, null); int lev = Floor.getLevel(elev, true);
				int fNum = -1; for(int e=0,q=dsList.length; e<q; e++) if(fNum == -1 && dsList.get(e).getY() >= lev) fNum = e;
				//Generate Floor and Call Sign Data:
				Floor floor=null; if(fNum != -1) floor = Floor.findFloor(dsList.get(fNum));
				if(floor!=null) { elev.floor = floor; elev.csList = Elevator.findCallSigns(elev); elevators.put((String)eKeys[i], elev); }
			}
		}}
		return pathFound?null:"1";
	} catch(Exception e) { return e.getMessage(); }}
	
	//-------------------  Useful Functions -------------------
	
	public static String locToString(Location loc) {
		return loc.getWorld().getName()+"-"+(int)loc.getX()+"-"+(int)loc.getZ();
	}
	
	public static Location locFromString(String str, JavaPlugin plugin) {
		String[] data = str.split("-"); World world = plugin.getServer().getWorld(data[0]); if(world==null)
		return null; return new Location(world, Integer.parseInt(data[1]), 0, Integer.parseInt(data[2]));
	}
	
	public static boolean isDoor(Block b) {
		String t = b.getType().toString(); return ((t.length()>5 && t.substring(t.length()-5) ==
		"_DOOR") || (t.length()>11 && t.substring(t.length()-11) == "_DOOR_BLOCK")) && b.getData() < 8;
	}
	
	//Write lines to sign:
	public static void setSign(Block sign, String[] lines) {
		org.bukkit.block.Sign state = ((org.bukkit.block.Sign)sign.getState());
		state.setLine(0, lines[0]==null?TITLE:lines[0]); state.setLine(1, lines[1]==null?"":lines[1]);
		state.setLine(2, lines[2]==null?"":lines[2]); state.setLine(3, lines[3]==null?"":lines[3]);
		state.update();
	} public static void setSign(Block sign, String lineOne) {
		org.bukkit.block.Sign state = ((org.bukkit.block.Sign)sign.getState());
		state.setLine(0, lineOne==null?TITLE:lineOne); state.setLine(1, "");
		state.setLine(2, ""); state.setLine(3, ""); state.update();
	}
	
	public static void setLine(Block sign, int l, String str) {
		org.bukkit.block.Sign state = ((org.bukkit.block.Sign)sign.getState()); state.setLine(l, str==null?"":str); state.update(true);
	}
	
	public static boolean setError(Block sign, Block currentSign) {
		if(sign.getLocation().equals(currentSign.getLocation())) return true;
		setSign(sign, ERROR); return false;
	} public static void setError(Block sign) {
		setSign(sign, ERROR);
	}
	
	//Read lines from sign:
	public static String[] lines(Block sign) {
		return ((org.bukkit.block.Sign)sign.getState()).getLines();
	}
	
	public static int findFirstEmpty(ChuList<ChuList<FallingBlock>> list) {
		int l=list.length; for(int i=0; i<l; i++) if(list.get(i)==null) return i; return l;
	}
	
	//Check if player has permission:
	public static boolean hasPerm(Player player, String perm) {
		return player.hasPermission(perm);// || player.hasPermission(perm
		//.substring(0,perm.lastIndexOf(".")+1)+"*") || player.hasPermission("*");
	}
	
	//Unpack a file internal to the JAR.
	public static String unpackFile(String internalPath, File dest, JavaPlugin plugin) {
		InputStream stream = plugin.getClass().getResourceAsStream(internalPath);
		//try { Files.copy(stream, dest); } catch (IOException e) { return ""; }
		String str = ""; int p = 0; try {
			while(p < 3000) { int read = stream.read(); if(read < 0 || read >=
			65535) break; str += fromCharCode(read); p++; } stream.close();
			FileWriter file = new FileWriter(dest); file.write(str); file.close();
		} catch (IOException e) { return ""; } return str;
	}
	
	//Emulate JavaScript's fromCharCode Function:
	public static String fromCharCode(int... codePoints) {
		return new String(codePoints, 0, codePoints.length);
	}
	
	//-------------------  PecacheuLib Functions -------------------
	
	public static String c(String str) {
		String clr[] = str.split("&"), cStr = clr[0];
		for(int i=1,l=clr.length; i<l; i++) cStr += org.bukkit.ChatColor
		.getByChar(clr[i].charAt(0)).toString()+clr[i].substring(1);
		return cStr;
	}
}