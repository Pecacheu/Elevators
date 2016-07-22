//Copyright (©) 2016, Pecacheu (Bryce Peterson), All Rights Reserved.
//Pecacheu's Elevator Plugin, Beta Eddition for ScriptCraft.

//Globals & Constants:
var elevators = {}, TITLE, CALL, ERROR, L_ST, L_END, MSG_GOTO_ST, MSG_GOTO_END, MSG_CALL, RADIUS_MAX,
TRAVEL_SPEED, MOVE_RES, DOOR_HOLD, SAVE_INT, BLOCKS, DOOR_SET, NOMV, M_ATLV, ATLV, C_UP, UP, C_DOWN, DOWN;

var MSG_NEW_CONF = "[ELEVATORS] Could not load config. Creating new config file...";

//Default Configuration File:
var defConfig = "### This is the config file for Pecacheu's Elevator plugin.\n"+
"### You can adjust values here to globally change the behavior of elevators.\n\n"+
"title: '&1[&3Elevator&1]' # Header for ELEVATOR signs.\n"+
"call: '&1[&3Call&1]' # Header for CALL signs.\n"+
"error: '[&4???&r]' # Header for non-working signs.\n"+
"selStart: '&8> &5' # Start of selected floor line.\n"+
"selEnd: ' &8<' # End of selected floor line.\n\n"+
"msgGotoStart: '&eTraveling to &a' # Start of player message.\n"+
"msgGotoEnd: '&e.' # End of player message.\n"+
"msgCall: '&eCalling elevator.' # Player message for CALL signs.\n\n"+
"# Status symbols displayed on CALL signs:\n\n"+
"noMove:   '&4⦿  ⦿  ⦿  ⦿  ⦿  ⦿'\n"+"mAtLevel: '&3⦿  ⦿  ⦿  ⦿  ⦿  ⦿'\n"+
"atLevel:  '&2⦿  ⦿  ⦿  ⦿  ⦿  ⦿'\n"+"callUp:   '&3▲  ▲  ▲  ▲  ▲  ▲'\n"+
"up:       '&4△  △  △  △  △  △'\n"+"callDown: '&3▼  ▼  ▼  ▼  ▼  ▼'\n"+
"down:     '&4▽  ▽  ▽  ▽  ▽  ▽'\n\n"+
"floorMaxRadius: 8  # Maximum floor size, in each direction from sign.\n"+
"travelSpeed: 25    # Elevator movment speed, in blocks-per-second.\n"+
"updateDelay: 5     # Delay between updates while moving. Increase to reduce lag.\n"+
"doorHoldTime: 4000 # Timeout before elevator doors are closed.\n"+
"saveInterval: 15   # Auto-save frequency in minutes.\n\n"+
"# List of blocks allowed as elevator floors:\n\n"+
"blockList:\n"+"- IRON_BLOCK\n"+"- GOLD_BLOCK\n"+
"- DIAMOND_BLOCK\n"+"- EMERALD_BLOCK\n"+"- GLASS\n\n"+
"doorBlock: 'THIN_GLASS' # Material used for elevator block-doors.\n\n"+
"# Stored elevator locations:\n\n"+
"### WARNING: DO NOT TOUCH THIS SETTING ###\n"+"elevators: {}";

//Default Configuration Settings:
var defaults = new org.bukkit.configuration.MemoryConfiguration();
defaults.set("title", "&1[&3Elevator&1]"); defaults.set("call", "&1[&3Call&1]");
defaults.set("error", "[&4???&r]"); defaults.set("selStart", "&8> &5"); defaults.set("selEnd", " &8<");
defaults.set("msgGotoStart", "&eTraveling to &a"); defaults.set("msgGotoEnd", "&e.");
defaults.set("msgCall", "&eCalling elevator."); defaults.set("noMove",   "&4⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
defaults.set("mAtLevel", "&3⦿  ⦿  ⦿  ⦿  ⦿  ⦿"); defaults.set("atLevel",  "&2⦿  ⦿  ⦿  ⦿  ⦿  ⦿");
defaults.set("callUp",   "&3▲  ▲  ▲  ▲  ▲  ▲"); defaults.set("up",       "&4△  △  △  △  △  △");
defaults.set("callDown", "&3▼  ▼  ▼  ▼  ▼  ▼"); defaults.set("down",     "&4▽  ▽  ▽  ▽  ▽  ▽");
defaults.set("floorMaxRadius", 8); defaults.set("travelSpeed", 25); defaults.set("updateDelay", 5);
defaults.set("doorHoldTime", 4000); defaults.set("saveInterval", 15); defaults.set("doorBlock", "THIN_GLASS");
defaults.set("blockList", java.util.Arrays.asList(["IRON_BLOCK","GOLD_BLOCK","DIAMOND_BLOCK","EMERALD_BLOCK","GLASS"]));

var CONFIG_PATH = "scriptcraft/Elevators/config.yml";

//------------------- Config Save & Load Functions: -------------------

function saveConfig() {
	var path = new java.io.File(CONFIG_PATH), data = "";
	
	//If Not Found, Create New Config File:
	if(!path.exists()) { data = newConfig(path); } else {
		//Read Current Config File:
		var file = new java.io.FileReader(path); var p = 0;
		while(p < 3000) { var read = file.read(); if(read < 0 || read >=
		65535) break; data += String.fromCharCode(read); p++; } file.close();
	}
	
	if(!data) return false;
	
	//Seperate And Overwrite BLOCKS Section:
	var bPos = data.lastIndexOf("blockList:"), bStr = data.substr(bPos);
	
	var bEnd = 0, nl = 0; while(bEnd < 600) {
		if(nl && bStr[bEnd] != '-') nl = 2;
		if(nl==2) { if(bStr[bEnd] == '\n') break; }
		else nl = (bStr[bEnd] == '\n') ? 1 : 0; bEnd++;
	} bEnd += bPos;
	
	var bConf = new org.bukkit.configuration.file.YamlConfiguration();
	bConf.set("blockList", java.util.Arrays.asList(BLOCKS));
	data = data.substring(0,bPos)+bConf.saveToString()+data.substring(bEnd);
	
	//Seperate And Overwrite Elevators Section:
	var sPos = data.lastIndexOf("elevators:"); data = data.substring(0,sPos);
	var eConf = new org.bukkit.configuration.file.YamlConfiguration();
	
	var eList = eConf.createSection("elevators"), eKeys = Object.keys(elevators);
	
	//Generate Compressed Elevator Data:
	for(var i=0,l=eKeys.length; i<l; i++) { //Data Format: [World, Signs1 X, Signs1 Z, Signs2 X, Signs2 Z...]
		var elev = elevators[eKeys[i]], eData = [elev[0][0].name];
		for(var k=1,d=elev.length; k<d; k++) { eData.push(elev[k][0].x); eData.push(elev[k][0].z); }
		eList.set(eKeys[i], java.util.Arrays.asList(eData));
	}
	
	//Append New Data And Save File:
	data += eConf.saveToString().slice(0,-1);
	var file = new java.io.FileWriter(path);
	file.write(String(data)); file.close();
	return true;
}

function newConfig(path) {
	server.getConsoleSender().sendMessage(MSG_NEW_CONF);
	java.nio.file.Files.createDirectories(path.toPath().getParent());
	var file = new java.io.FileWriter(path); file.write(defConfig); file.close();
	return defConfig;
}

/*function fromYAML(str) {
	var conf = new org.bukkit.configuration.file.YamlConfiguration();
	 return conf;
}*/

function loadConfig() {
	var path = new java.io.File(CONFIG_PATH), config; var pathFound = path.exists();
	if(pathFound) config = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(path);
	else config = new org.bukkit.configuration.file.YamlConfiguration();
	
	config.setDefaults(defaults); movingFloors = []; CLTMR = -1;
	
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
	DOOR_SET = org.bukkit.Material.valueOf(config.getString("doorBlock"));
	
	var bList = config.getStringList("blockList").toArray();
	
	//Remove any items that are not solid blocks:
	BLOCKS = []; for(var b=0,g=bList.length; b<g; b++) if(org.bukkit
	.Material.valueOf(bList[b]).isSolid()) BLOCKS.push(bList[b]);
	
	//Load Compressed Elevator Data:
	var eList = config.getConfigurationSection("elevators");
	if(eList) { var eKeys = eList.getKeys(false).toArray();
	elevators = {}; for(var i=0,l=eKeys.length; i<l; i++) {
		var eData = eList.getStringList(eKeys[i]).toArray(),
		world = server.getWorld(eData[0]), elev = [1];
		//Locate All Elevator Signs:
		for(var k=1,d=eData.length; k<d; k+=2) {
			var sList = findSigns(new org.bukkit.Location(world, Number(eData[k]), 0, Number(eData[k+1])));
			if(sList.length) elev.push(sList);
		}
		var dsList = elev[1];
		if(dsList && dsList[0].type == org.bukkit.Material.WALL_SIGN) {
			//Calculate Platform Height And Convert To Floor Number:
			elevators[eKeys[i]] = elev; var lev = getLevel(eKeys[i], true);
			var fNum = -1; for(var e=0,q=dsList.length; e<q; e++) if(fNum == -1 && dsList[e].y >= lev) fNum = e;
			//Generate Floor and Call Sign Data:
			var floor; if(fNum != -1) floor = findFloor(dsList[fNum]);
			if(floor) { elevators[eKeys[i]][0] = floor; elevators[eKeys[i]][0][7] = findCallSigns(eKeys[i]); }
			else delete elevators[eKeys[i]];
		}
	}}
	return pathFound;
}

setTimeout(function() {
	var success = loadConfig(); if(!success) saveConfig();
	setInterval(saveConfig, SAVE_INT*60000);
}, 200);

//NOTES:
//The floor size of the lowest block will be used.
//Only X,Z of known elevators is stored (so that they can only be created by players with perm node), everything else is calculated every time, even floor count.
//Floors are names, not numbers. If floor on sign is not found, default to first floor.
//Optional custom floor label is put on line 4. Otherwise, default labeling will be in format "Level (n)".
//Signs Must be at least 3 blokcs away. If signs are 4 blocks away, THIN_GLASS will be placed for 1 block above signs, as well as at and 1 below sign height.
//Elevator blocks shouldn't be reset until sings are right-clicked.
//Right-clicking updates set floor on all signs in elevator region, even ones not in same column.

//More TODO: Add sound effects.

//Major TODO:
//-- Call signs can be further away.
//-- Disable an elevator creating DOOR blocks using [nobdoor] on line 3rd line.
//-- Change how elevator car height is determined in updateCallSigns. Floors change too quickly, should change when platform is < 2 blocks away from sign.y-2.

//------------------- Elevator Create Sign Events: -------------------

events.signChange(function(event) { if(!event.isCancelled() && event.block.type == org.bukkit.Material.WALL_SIGN) {
var tLine = event.lines[0].toLowerCase(); if(tLine == "[call]" && true) { //<< Perms Check //Call Signs:
	
	event.setLine(0, "> ERROR <"); //Print error message first in case of plugin error.
	event.setLine(1, ""); event.setLine(2, "");
	
	//Check For Elevator:
	var ret = findElevCallSign(event.block), eID = ret[0], csNum = ret[1];
	if(!eID) { event.setLine(0, ERROR); return; }
	
	//Find All Call Signs:
	elevators[eID][0][7] = findCallSigns(eID, event.block.location);
	
	//Update Call Signs:
	var iList = updateCallSigns(eID, getLevel(eID));
	event.setLine(0, CALL); /*event.setLine(2, ">LVL HERE?<");*/ event.setLine(3, iList[csNum]);
	
	//event.setLine(2, c("&4████████")); event.setLine(3, c("&4▓▓▓▓▓▓▓▓"));
	
} else if(tLine == "[elevator]" && true) { //<< Perms Check //Elevator Signs:
	
	event.setLine(0, "> ERROR <"); //Print error message first in case of plugin error.
	event.setLine(1, ""); event.setLine(2, "");
	
	//Find All Signs in Elevator:
	var sGroups = findSigns(event.block), eIDItem = sGroups[0];
	
	//Check if Already in an Elevator:
	var eRet = inElev(event.block, sGroups);
	if(typeof eRet == "object") eIDItem = eRet[0]; else if(eRet ==
	"cancel") { event.setCancelled(true); return; } else if(eRet) { //Sign is in New Column:
		var nElev=elevators[eRet], world=sGroups[0].world, sX=sGroups[0].x,
		sZ=sGroups[0].z, sData=sGroups[0].data, oSigns=nElev[1], nSigns=[];
		for(var i=0,l=sGroups.length; i<l; i++) sGroups[i].type = org.bukkit.Material.AIR;
		for(i=0,l=oSigns.length; i<l; i++) { nSigns[i] = world.getBlockAt(sX, oSigns[i].y, sZ); nSigns[i].type = org.bukkit.Material
		.WALL_SIGN; nSigns[i].data = sData; setSign(nSigns[i], oSigns[i].state.getLines()); } nElev.push(nSigns); return;
	}
	
	//Detect Elevator Floor Size:
	var floor = findFloor(eIDItem); if(!floor) { for(var i=0, l=sGroups
	.length; i<l; i++) setError(sGroups[i]); event.setLine(0, ERROR); return; }
	
	//Generate New Elevator ID And Store Elevator Location:
	var eID = locToString(new org.bukkit.Location(floor[0],floor[1],0,floor[2]));
	if(!eRet) elevators[eID] = [floor, sGroups]; //Floor Size Data, Sign List 1, Sign List 2, etc.
	
	//Remove Elevator Obstructions:
	removeFallingBlocks(eID); resetElevator(eID, event.block);
	
	//Validate Sign Placment:
	var cErr; for(var i=0; i<sGroups.length; i++) {
		if(i > 0 && (sGroups[i].y - sGroups[i-1].y) < 3) { cErr = setError(sGroups[i], event.block); sGroups.splice(i,1); i--; } //Signs too close!
		else { addFloor(eID, sGroups[i].y-2, false, true); setDoors(floor, sGroups[i].y, i==0); }
	}
	
	//Update Sign Level Numbers for Non-Custom-Named Signs:
	for(var k=1,m=elevators[eID].length; k<m; k++) for(var f=0,d=elevators[eID][k].length; f<d; f++) {
		var sign = elevators[eID][k][f];
		if(sign.location.equals(event.block.location)) { if(!event.lines[3]) event.setLine(3, "Level "+(f+1)); }
		else if(/^Level \d+$/.test(sign.state.lines[3])) setLine(sign, 3, "Level "+(f+1));
		else if(sign.y == event.block.y) setSign(sign, event.lines);
	}
	
	//Reset Gravity of LivingEntities in elevator:
	var world = floor[0], minX = floor[1], minZ = floor[2], maxX = minX+floor[3],
	maxZ= minZ+floor[4], minY = sGroups[0].y-2, maxY = sGroups[sGroups.length-1].y+1;
	var eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
	for(k=0,m=eList.length; k<m; k++) { var loc = eList[k].location;
		if((loc.x >= minX && loc.x <= maxX) && (loc.y >= minY && loc.y <=
		maxY) && (loc.z >= minZ && loc.z <= maxZ)) eList[k].gravity = true;
	}
	
	if(cErr) event.setLine(0, ERROR); else event.setLine(0, TITLE);
	setTimeout(saveConfig, 200); //Save Changes To Config.
}}});

//------------------- Elevator Destroy Sign Events: -------------------

//events.blockBreak(function(event) { if() {
	//CANCEL IF NOT CORRECT PERMISSIONS.
	//Breaking an elevator sign deletes the whole column of signs.
	//Breaking the sign when only one column is left should delete sign & blocks only on level but add glass below player. Should probably open level's doors, too.
	//Use getBlockBelowPlayer(event.player) Copy function to here so it works without PecacheuLib.
	//Cancel break/place if elevator is moving.
	//Save if elevator sign destroyed.
	//For call signs, all you have to do is: elevators[eID][0][7] = findCallSigns(eID);
//}});

//------------------- Elevator Block-Clicking Events: -------------------

var CLTMR = -1;
events.playerInteract(function(event) { if(!event.item && true) { //<< Perms Check
	var act = event.action.toString(); if(act == "RIGHT_CLICK_BLOCK" && event.clickedBlock.type
	== org.bukkit.Material.WALL_SIGN) { var eID = findElev(event.clickedBlock); if(eID && !elevators[eID][0][6]) { //Select Floor:
		var elev = elevators[eID], dsList = elev[1];
		
		//Get Selected Floor:
		var selName = dsList[0].state.lines[1], selNum = 0; if(selName) selName = selName.substring(L_ST.length,selName.length-L_END.length);
		
		//Get List of Floor Names:
		var flNames = []; for(var i=0,l=dsList.length; i<l; i++) { flNames[i] = dsList[i].state.lines[3]; if(flNames[i] == selName) selNum = i; }
		
		//Update Floor Number:
		if(event.player.isSneaking()) { selNum--; if(selNum < 0) selNum = flNames.length-1; }
		else { selNum++; if(selNum >= flNames.length) selNum = 0; } var nFloor = L_ST+flNames[selNum]+L_END;
		for(i=1,l=elev.length; i<l; i++) for(var d=0,j=elev[i].length; d<j; d++) setLine(elev[i][d], 1, nFloor);
		
	} else { var ret = findElevCallSign(event.clickedBlock), eID = ret[0]; if(eID && !elevators[eID][0][6]) { //Call Sign Click:
		
		var fLevel = getLevel(eID), sLevel = event.clickedBlock.y-2;
		
		//Call elevator to floor:
		if(fLevel != sLevel) {
			event.player.sendMessage(MSG_CALL);
			gotoFloor(eID, fLevel, sLevel, ret[1], TRAVEL_SPEED);
		} else { setDoors(elevators[eID][0], sLevel+2, true); if(CLTMR != -1) clearTimeout(CLTMR);
		CLTMR = setTimeout(function() { setDoors(elevators[eID][0], sLevel+2, false); CLTMR = -1; }, DOOR_HOLD); } //Re-open doors if already on level.
		
	}}} else if(act == "LEFT_CLICK_AIR" || act == "RIGHT_CLICK_BLOCK") { var eID = playerInElev(event.player); if(eID && !elevators[eID][0][6]) { //Go To Floor:
		var elev = elevators[eID], floor = elev[0], dsList = elev[1];
		
		//Get Current And Slected Floors:
		var fLevel = getLevel(eID), flNum = -1, selName = dsList[0].state.lines[1], selNum = 0;
		if(selName) selName = selName.substring(L_ST.length,selName.length-L_END.length);
		
		for(var i=0,l=dsList.length; i<l; i++) { if(dsList[i].state.lines[3] ==
		selName) selNum = i; if(flNum == -1 && dsList[i].y >= fLevel) flNum = i; }
		var sLevel = dsList[selNum].y-2;
		
		if(flNum != selNum) {
			event.player.sendMessage(MSG_GOTO_ST+selName+MSG_GOTO_END);
			gotoFloor(eID, fLevel, sLevel, selNum, TRAVEL_SPEED);
		} else { setDoors(floor, sLevel+2, true); if(CLTMR != -1) clearTimeout(CLTMR);
		CLTMR = setTimeout(function() { setDoors(floor, sLevel+2, false); CLTMR = -1; }, DOOR_HOLD); }
	}}
}});

//------------------- Elevator Functions: -------------------

//Move elevator car from fLevel to sLevel:
//Speed is in blocks-per-second.
function gotoFloor(eID, fLevel, sLevel, selNum, speed) {
	var floor = elevators[eID][0], step = speed * (MOVE_RES/1000) * (sLevel>fLevel?1:-1);
	updateCallSigns(eID, fLevel, 2); floor[6] = true; setDoors(floor, fLevel+2, false);
	setTimeout(function() {
		var fID = addFloor(eID, fLevel, true), fPos = fLevel, mFlr = 3;
		var fTmr = setInterval(function() {
			//if(mFlr == 3) { moveFloor(fID, eID, fPos); }
			if(mFlr >= 3) { moveFloor(fID, eID, fPos); updateCallSigns(eID, fPos, sLevel>fLevel, selNum); mFlr = 0; }
			moveMobs(eID, fPos); mFlr++;
			if(sLevel>fLevel?(fPos >= sLevel):(fPos <= sLevel)) {
				clearInterval(fTmr); deleteFloor(fID, eID);
				addFloor(eID, sLevel, false); setTimeout(function(){addFloor(eID, sLevel, false)},50); updateCallSigns(eID, sLevel+2);
				setTimeout(function() {
					setDoors(floor, sLevel+2, true); floor[6] = false; updateCallSigns(eID, sLevel+2);
					if(CLTMR != -1) clearTimeout(CLTMR);
					CLTMR = setTimeout(function() { setDoors(floor, sLevel+2, false); CLTMR = -1; }, DOOR_HOLD);
				}, 500);
			} else fPos += step;
		}, MOVE_RES);
	}, 500);
}

//Find elevator for sign, if any:
function findElev(sign) {
	var eKeys = Object.keys(elevators), sLoc = sign.location;
	for(var m=0,k=eKeys.length; m<k; m++) for(var i=1,l=elevators[eKeys[m]].length;
	i<l; i++) for(var b=0,q=elevators[eKeys[m]][i].length; b<q; b++) if(elevators
	[eKeys[m]][i][b].location.equals(sLoc)) return eKeys[m]; return false;
}

//Find elevator for player, if any:
function playerInElev(player) {
	var eKeys = Object.keys(elevators), pW = player.world.name, pLoc = player.location;
	for(var m=0,k=eKeys.length; m<k; m++) { var fl = elevators[eKeys[m]][0]; if(pW == fl[0].name) {
		var minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ = minZ+fl[4], sList =
		elevators[eKeys[m]][1], minY = sList[0].y-2, maxY = sList[sList.length-1].y+1;
		if((pLoc.x >= minX && pLoc.x <= maxX) && (pLoc.y >= minY && pLoc.y
		<= maxY) && (pLoc.z >= minZ && pLoc.z <= maxZ)) return eKeys[m];
	}} return false;
}

//Find elevator for call sign, if any:
function findElevCallSign(sign) {
	var eKeys = Object.keys(elevators), sW = sign.world.name, loc = sign.location;
	function checkElev(x,z) {return(loc.x == x && loc.z == z)}
	for(var m=0,k=eKeys.length; m<k; m++) { var fl = elevators[eKeys[m]][0]; if(sW == fl[0].name) {
		var minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ = minZ+fl[4], sList = elevators[eKeys[m]][1];
		for(var j=0,g=sList.length; j<g; j++) if(loc.y == sList[j].y) {
			for(var xP=minX-1; xP<maxX+1; xP++) if(checkElev(xP, minZ-2)) return [eKeys[m], j];
			for(var zP=minZ-1; zP<maxZ+1; zP++) if(checkElev(maxX+1, zP)) return [eKeys[m], j];
			for(xP=maxX; xP>minX-2; xP--) if(checkElev(xP, maxZ+1)) return [eKeys[m], j];
			for(zP=maxZ; zP>minZ-2; zP--) if(checkElev(minX-2, zP)) return [eKeys[m], j];
			break;
		}
	}} return false;
}

//Determine if sign is in an existing elevator, as well as other things:
//In fact, this is just most of the logic and processing for the [elevator] place event:
function inElev(sign, sList) {
	var eKeys = Object.keys(elevators), cW = sign.world.name, cX = sign.x, cZ = sign.z;
	for(var m=0,k=eKeys.length; m<k; m++) {
		var fl = elevators[eKeys[m]][0], world = fl[0].name, minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ = minZ+fl[4];
		if(cW == world && (cX >= minX && cX <= maxX) && (cZ >= minZ && cZ <= maxZ)) { //Check if in elevator:
			if(fl[6]) return "cancel"; //Cancel if elevator is currently moving.
			for(var i=1,l=elevators[eKeys[m]].length; i<l; i++) { //Check if in known column:
				//If new sign is in existing column, re-create other columns to match:
				if(elevators[eKeys[m]][i][0].x == cX && elevators[eKeys[m]][i][0].z == cZ) {
					elevators[eKeys[m]][i] = sList; for(var v=1; v<l; v++) if(v!=i) {
						var oSigns=elevators[eKeys[m]][v], sX=oSigns[0].x, sZ=oSigns[0].z, sData=oSigns[0].data, nSigns=[];
						for(var s=0,h=oSigns.length; s<h; s++) oSigns[s].type = org.bukkit.Material.AIR;
						for(s=0,h=sList.length; s<h; s++) { nSigns[s] = fl[0].getBlockAt(sX, sList[s].y, sZ); nSigns[s].type = org.bukkit.Material
						.WALL_SIGN; nSigns[s].data = sData; setSign(nSigns[s], sList[s].state.getLines()); } elevators[eKeys[m]][v] = nSigns;
					} return elevators[eKeys[m]][1];
				}
			} return eKeys[m];
		}
	} return false;
}

//Determine Elevator Sign Positions:
//Y coordinate is optional and can be set to 0.
function findSigns(bOrLoc) {
	var world=bOrLoc.world, xPos=bOrLoc
	.x, zPos=bOrLoc.z, bY=bOrLoc.y, a=[];
	for(var h=0; h<256; h++) {
		var bl = world.getBlockAt(xPos, h, zPos);
		if(bl.type == org.bukkit.Material.WALL_SIGN &&
		(bl.state.lines[0] == TITLE || (bY && h == bY))) a.push(bl);
	} return a;
}

//Determine Elevator Call Sign Positions:
function findCallSigns(elevID, newLoc) {
	var fl = elevators[elevID][0], world = fl[0], minX = fl[1], minZ = fl
	[2], maxX = minX+fl[3], maxZ = minZ+fl[4], sList = elevators[elevID][1];
	function checkSign(bl) {return (bl.type == org.bukkit.Material.WALL_SIGN
	&& (bl.state.lines[0] == CALL || (newLoc && bl.location.equals(newLoc))))}
	var csList = []; for(var j=0,g=sList.length; j<g; j++) { //Itterate through floors:
		csList[j] = []; //Scan perimeter for call signs:
		for(var xP=minX-1; xP<maxX+1; xP++) { var bl = world.getBlockAt(xP, sList[j].y, minZ-2); if(checkSign(bl)) csList[j].push(bl); }
		for(var zP=minZ-1; zP<maxZ+1; zP++) { var bl = world.getBlockAt(maxX+1, sList[j].y, zP); if(checkSign(bl)) csList[j].push(bl); }
		for(xP=maxX; xP>minX-2; xP--) { var bl = world.getBlockAt(xP, sList[j].y, maxZ+1); if(checkSign(bl)) csList[j].push(bl); }
		for(zP=maxZ; zP>minZ-2; zP--) { var bl = world.getBlockAt(minX-2, sList[j].y, zP); if(checkSign(bl)) csList[j].push(bl); }
	} return csList;
}

//Determine Elevator Floor Size:
function findFloor(b) {
	var world=b.world, bX=b.x, h=b.y-2, bZ=b.z, fType=world.getBlockAt(bX, h, bZ).type;
	if(BLOCKS.indexOf(fType.toString()) == -1 || world.getBlockAt(bX, h+1, bZ).type.toString()
	!= "AIR") return false; var xP=1, xN=1, zP=1, zN=1, face=b.state.data.getFacing().toString();
	if(face !=  "WEST") while(xP <= RADIUS_MAX) { if(world.getBlockAt(bX+xP, h, bZ).type != fType) break; xP++; }
	if(face !=  "EAST") while(xN <= RADIUS_MAX) { if(world.getBlockAt(bX-xN, h, bZ).type != fType) break; xN++; }
	if(face != "NORTH") while(zP <= RADIUS_MAX) { if(world.getBlockAt(bX, h, bZ+zP).type != fType) break; zP++; }
	if(face != "SOUTH") while(zN <= RADIUS_MAX) { if(world.getBlockAt(bX, h, bZ-zN).type != fType) break; zN++; }
	if(xP > RADIUS_MAX || xN > RADIUS_MAX || zP > RADIUS_MAX || zN > RADIUS_MAX) return false;
	var xPos=bX-xN+1, zPos=bZ-zN+1, length=xP+xN-1, width=zP+zN-1;
	return [world, xPos, zPos, length, width, fType, false, []];
}

//Remove all blocks in elevator:
function resetElevator(elevID, ignore, noFloor) {
	var fl = elevators[elevID][0], sList = elevators[elevID][1];
	var world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+fl[3],
	maxZ = minZ+fl[4], minY = sList[0].y-2, maxY = sList[sList.length-1].y+1, fType = fl[5];
	for(var y=minY; y<maxY; y++) for(var x=minX; x<maxX; x++) for(var z=minZ; z<maxZ; z++) {
		var bl = world.getBlockAt(x, y, z); if(y == minY && !noFloor) bl.type = fType;
		else if(bl.type != org.bukkit.Material.WALL_SIGN || !isKnownSign(bl, elevID)) if(!ignore || !bl
		.location.equals(ignore.location)) {var s=bl.state;s.type=org.bukkit.Material.AIR;s.update(true)}
	}
}

//Check if sign is a registered 'elevator' sign:
function isKnownSign(sign, elevID) {
	for(var i=1,l=elevators[elevID].length; i<l; i++) for(var k=0,b=elevators[elevID][i].length; k<b;
	k++) if(elevators[elevID][i][k].location.equals(sign.location)) return true; return false;
}

//Open/Close Elevator Doors:
function setDoors(fl, h, onOff) {
	var world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+
	fl[3], maxZ = minZ+fl[4], matRem = org.bukkit.Material.AIR;
	function isCorner(x,z) {return (fl[3]<=2 ? (x < minX || x > maxX-1) : (x <= minX || x >= maxX-1))
	&& (fl[4]<=2 ? (z < minZ || z > maxZ-1) : (z <= minZ || z >= maxZ-1))}
	//Open/Close Doors and Barrier-Doors:
	function setDoor(d) {  if(isDoor(d)) { var dat = d.data; if(onOff && dat
	< 4) d.setData(dat+4); else if(!onOff && dat >= 4) d.setData(dat-4); }}
	function setBDoor(bl) { if(isCorner(bl.x,bl.z)) { if(bl.type == matRem) bl.type = DOOR_SET;
	} else if(bl.type == (onOff?DOOR_SET:matRem)) bl.type = (onOff?matRem:DOOR_SET); }
	//Cycle Around Elevator Perimeter:
	for(var yP=h-1; yP<=h+1; yP++) {
		for(var xP=minX; xP<maxX+1; xP++) { var bl = world.getBlockAt(xP, yP, minZ-1); setBDoor(bl); if(yP==h-1) setDoor(bl); }
		for(var zP=minZ; zP<maxZ+1; zP++) { var bl = world.getBlockAt(maxX, yP, zP); setBDoor(bl); if(yP==h-1) setDoor(bl); }
		for(xP=maxX-1; xP>minX-2; xP--) { var bl = world.getBlockAt(xP, yP, maxZ); setBDoor(bl); if(yP==h-1) setDoor(bl); }
		for(zP=maxZ-1; zP>minZ-2; zP--) { var bl = world.getBlockAt(minX-1, yP, zP); setBDoor(bl); if(yP==h-1) setDoor(bl); }
	}
}

//Update all elevator call signs:
//fLvl: Current elevator height, fDir: Direction (or set to 2 for doors-closed but not moving), sNum: Destination floor, if any.
function updateCallSigns(elevID, fLvl, fDir, sNum) {
	var elev = elevators[elevID], sList = elev[1], csList = elev[0][7], locked = elev[0][6];
	var csInd = []; for(var m=0,k=sList.length,fNum=-1; m<k; m++) {
		var ind = NOMV; if(fNum == -1 && sList[m].y >= fLvl) fNum = m;
		if(m == fNum) ind = (fDir==2||locked) ? M_ATLV : ATLV; //Elevator is on level.
		else if(locked) { if(fDir) { //Going Up.
			if(m > fNum && m == sNum) ind = C_UP; //Elevator is below us and going to our floor.
			else ind = UP; //Elevator is above us or not going to our floor.
		} else { //Going Down.
			if(fNum == -1 && m == sNum) ind = C_DOWN; //Elevator is above us and going to our floor.
			else ind = DOWN; //Elevator is below us or not going to our floor.
		}}
		csInd[m] = ind; if(csList[m]) for(var i=0,l=csList[m].length; i<l; i++) setLine(csList[m][i], 3, ind);
	}
	return csInd;
}

//------------------- Floor Movement Functions: -------------------

//Creates Floor or MovingFloor at height 'h'.
//If MovingFloor, returns new floorID.
//Deletes existing floor blocks unless 'dontDelete' is true.
var movingFloors = []; function addFloor(elevID, h, isMoving, dontDelete, forceID) {
	var fl = elevators[elevID][0], world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ = minZ+fl[4], fType = fl[5];
	if(!dontDelete) { removeFallingBlocks(elevID); resetElevator(elevID,null,true); }
	if(isMoving) { //Create FallingBlock Floor:
		var blocks = []; fl[6] = true;
		for(var x=minX; x<maxX; x++) for(var z=minZ; z<maxZ; z++) {
			blocks.push(fallingBlock(world, x, h, z, fType));
		}
		var ind; if(forceID!=null) ind = forceID;
		else ind = findFirstEmpty(movingFloors);
		movingFloors[ind] = blocks; return ind;
	} else { //Create Solid Floor:
		for(var x=minX; x<maxX; x++) for(var z=minZ; z<maxZ; z++) {
			var bl = world.getBlockAt(x, h, z); bl.type = fType;
		}
	}
}

//Moves a MovingFloor using floorID.
function moveFloor(floorID, elevID, h) {
	if(movingFloors[floorID]) {
		var bList = movingFloors[floorID];
		for(var i=0,l=bList.length; i<l; i++) bList[i].remove();
		addFloor(elevID, h, true, true, floorID);
	}
}

//Moves Players/Animals/NPCs With Elevator Floor.
function moveMobs(elevID, h) {
	var fl = elevators[elevID][0], world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ
	= minZ+fl[4], sList = elevators[elevID][1], minY = sList[0].y-2, maxY = sList[sList.length-1].y+1;
	var eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
	for(var i=0,l=eList.length; i<l; i++) { var loc = eList[i].location;
		if((loc.x >= minX && loc.x <= maxX) && (loc.y >= minY && loc.y <= maxY) && (loc.z >= minZ && loc.z <= maxZ)) { eList
		[i].gravity = false; eList[i].teleport(new org.bukkit.Location(world, loc.x, h+1, loc.z, loc.yaw, loc.pitch)); }
	}
}

//Deletes a MovingFloor instance.
function deleteFloor(floorID, elevID) {
	if(movingFloors[floorID]) {
		var bList = movingFloors[floorID];
		for(var i=0,l=bList.length; i<l; i++) bList[i].remove();
	} movingFloors[floorID] = null;
	//Restore Gravity to LivingEntities:
	var fl = elevators[elevID][0], world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+fl[3], maxZ
	= minZ+fl[4], sList = elevators[elevID][1], minY = sList[0].y-2, maxY = sList[sList.length-1].y+1;
	var eList = world.getEntitiesByClass(org.bukkit.entity.LivingEntity.class).toArray();
	for(i=0,l=eList.length; i<l; i++) { var loc = eList[i].location;
		if((loc.x >= minX && loc.x <= maxX) && (loc.y >= minY && loc.y <=
		maxY) && (loc.z >= minZ && loc.z <= maxZ)) eList[i].gravity = true;
	}
}

//Calculates Current Floor/MovingFloor Height.
function getLevel(elevID, noTypeCheck) {
	var sList = elevators[elevID][1], world = sList[0].world,
	minY = sList[0].y-2, maxY = sList[sList.length-1].y+1,
	xPos = sList[0].x, zPos = sList[0].z, fType;
	if(!noTypeCheck) fType = elevators[elevID][0][5];
	for(var y=minY; y<maxY; y++) { var bl = world.getBlockAt(xPos, y, zPos);
		if(noTypeCheck ? (BLOCKS.indexOf(bl.type.toString()) != -1) : (bl.type == fType)) return bl.y;
	} return minY;
}

//------------------- FallingBlock Management Functions: -------------------

/*function convertToFallingBlock(block) {
	var falling = block.world.spawnFallingBlock(block.location, block.type, block.data);
	falling.gravity = false; block.type = org.bukkit.Material.AIR; return falling;
}

function restoreFromFallingBlock(falling) {
	var loc = falling.location, block = falling.world.getBlockAt(round(loc.x), round(loc.y), round(loc.z));
	block.type = falling.material; block.data = falling.blockData; falling.remove(); return block;
}*/

function fallingBlock(world, x, y, z, type) {
	var falling = world.spawnFallingBlock(new org.bukkit.Location
	(world, x, y, z), type, 0); falling.gravity = false; return falling;
}

function removeFallingBlocks(elevID) {
	var fl = elevators[elevID][0], sList = elevators[elevID][1];
	var world = fl[0], minX = fl[1], minZ = fl[2], maxX = minX+fl[3],
	maxZ = minZ+fl[4], minY = sList[0].y-2, maxY = sList[sList.length-1].y+1;
	var el = world.getEntitiesByClass(org.bukkit.entity.EntityType.FALLING_BLOCK.class).toArray();
	
	for(var i=0,l=el.length; i<l; i++) {
		var loc = el[i].location;
		if((loc.x >= minX-0.5 && loc.x <= maxX+0.5) && (loc.y >= minY-0.5 && loc.y
		<= maxY+0.5) && (loc.z >= minZ-0.5 && loc.z <= maxZ+0.5)) el[i].remove();
	}
}

//-------------------  Useful Functions: -------------------

/*function serialize(data) {
	var str = "";
	if(typeof data == Object) {
		if(!data.class) {
			if(Array.isArray(data)) for(var i=0,l=data.length; i<l; i++) str += (i==0?"":"/")+serialize(data[i]);
			else { var dKeys = Object.keys(data); for(var i=0,l=dKeys.length; i<l; i++) str += (i==0?"":"/")+dKeys[i]+"="+serialize(data[dKeys[i]]); }
		} else {
			if(data.name) str = "W"+data.name; //WORLDS
			else if(data.location) { var loc = data.location; str = "B"+loc.x+","+loc.y+","+ //BLOCKS
			loc.z+","+loc.world.name; if(data.type) str += ","+data.type; if(data.data) str += ","+data.data; }
		}
	} else str = data.toString();
	return str;
}

function deserialize(str) {
	var dRaw = str.split("/"), data = [];
	for(var i=0,l=dRaw.length; i<l; i++) {
		if(dRaw[i][0] == "W") data.push(server.getWorld(dRaw[i].substr(1)))
		else if(dRaw[i][0] == "B") {
			dRaw.split(",")
			dRaw
		}
	}
}*/

function locToString(loc) {
	return loc.world.name+"-"+loc.x+"-"+loc.z;
}

function locFromString(str) {
	var data = str.split("-"), world = server.getWorld(data[0]); if(!world)
	return false; return new org.bukkit.Location(world, data[1], 0, data[2]);
}

function isDoor(b) {
	var t = b.type.toString(); return (t.substr(-5) == "_DOOR"
	|| t.substr(-11) == "_DOOR_BLOCK") && b.data < 8;
}

function setSign(sign, lines) {
	var state = sign.state;
	state.lines[0] = lines[0]||TITLE; state.lines[1] = lines[1]||"";
	state.lines[2] = lines[2]||""; state.lines[3] = lines[3]||"";
	state.update();
}

function setLine(sign, l, str) {
	var state = sign.state; state.lines[l] = str||""; state.update(true);
}

function setError(sign, currentSign) {
	if(currentSign && sign.equals(currentSign)) return true;
	setSign(sign, [ERROR]);
}

function findFirstEmpty(arr) {
	for(var i=0,l=arr.length; i<l; i++)
	if(!arr[i]) return i; return l;
}
//-------------------  PecacheuLib Functions: -------------------

function c(str) {
	var clr = str.split("&"), cStr = clr[0];
	for(var i=1,l=clr.length; i<l; i++) cStr
	+= (org.bukkit.ChatColor.getByChar(clr[i][0]).toString())+clr[i].substr(1);
	return cStr;
}