package fixedBots;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;

import edu.berkeley.nlp.starcraft.util.UnitUtils;


public class LurkerDrop extends EmptyFixedBot{
	String hatchery = "Zerg Hatchery";
	String overlord = "Zerg Overlord";
	String zergling = "Zerg Zergling";
	String spawningPool = "Zerg Spawning Pool";
	String den = "Zerg Hydralisk Den";
	String hydralisk = "Zerg Hydralisk";
	String drone = "Zerg Drone";
	String lurker = "Zerg Lurker";
	String lurkerEgg = "Zerg Lurker Egg";
	String larva = "Zerg Larva";
	String extractor = "Zerg Extractor";
	String lair = "Zerg Lair";
	String lingSpeed = "Zerg Zergling Speed Upgrade";
	String drop = "Zerg Overlord Drop Upgrade";
	String lurker_up = "Zerg Lurker Upgrade";
	
	boolean buildLock = false;
	List<BuildCommand> buildOrder = new ArrayList<BuildCommand>();
	Unit lastBuilder = null;
	BuildCommand lastOrder = null;
	List<Unit> bases = new ArrayList<Unit>();
	List<Unit> larvae = new ArrayList<Unit>();
	List<Unit> drones = new ArrayList<Unit>();
	List<Unit> ovies = new ArrayList<Unit>();
	List<Unit> lings = new ArrayList<Unit>();
	List<Unit> hydras = new ArrayList<Unit>();
	List<Unit> lurkers = new ArrayList<Unit>();
	
	public void buildNext(){
		if(!buildOrder.isEmpty()&&!buildLock){
			if(buildOrder.get(0).equals(lingSpeed)){
				List<ROUnit> sp = UnitUtils.getAllMy(UnitType.getUnitType(spawningPool));
				if(sp.isEmpty()){
					System.out.println("No spawn pool");
					return;
				}else if(getMinerals() < 100 && Game.getInstance().self().gas() < 100){
					((Unit)sp.get(0)).upgrade(UpgradeType.METABOLIC_BOOST);
				}
			}else if(buildOrder.get(0).equals(drop)){
				List<ROUnit> lairs = UnitUtils.getAllMy(UnitType.getUnitType(lair));
				if(lairs.isEmpty()){
					System.out.println("No lairs");
				}else if(getMinerals() < 200 && Game.getInstance().self().gas() < 200){
					((Unit)lairs.get(0)).upgrade(UpgradeType.PNEUMATIZED_CARAPACE);
				}
			}else if(buildOrder.get(0).equals(lurker_up)){
				List<ROUnit> dens = UnitUtils.getAllMy(UnitType.getUnitType(den));
				if(dens.isEmpty()){
					System.out.println("No dens");
					return;
				} else if(getMinerals() < 200 && Game.getInstance().self().gas() < 200){
					((Unit)dens.get(0)).research(TechType.LURKER_ASPECT);
				}
			}else if(createUnit(UnitType.getUnitType(buildOrder.get(0).order),buildOrder.get(0).loc))
				lastOrder = buildOrder.remove(0);
			
		}else if(buildLock){
			//check to see if builder still actually going to build 
			if(!lastBuilder.getOrder().equals(Order.DRONE_BUILD)){
				buildLock = false;
				buildOrder.add(0,lastOrder);
			}
		}
	}
	
	public boolean createUnit(UnitType t, TilePosition area){
		if(area==null){
			List<ROUnit> bases = UnitUtils.getAllMy(UnitType.getUnitType(hatchery));
			area = ((Unit)bases.get(0)).getTilePosition();
		}
		 if(t.equals(UnitType.getUnitType(spawningPool)) || t.equals(UnitType.getUnitType(den)) 
				|| t.equals(UnitType.getUnitType(hatchery))){
			if(getMinerals() >= t.mineralPrice() && Game.getInstance().self().gas() >= t.gasPrice()&& !drones.isEmpty()){
				Unit morpher = (Unit)findClosest(drones,area);
				TilePosition tp;
				for(int i = 8; i < 15; i++){
					tp = findBuildRadius(area,i,morpher,t);
					if(tp!=null){
						morpher.build(tp,t);
						lastBuilder = morpher;
						buildLock = true;
						System.out.println("Building" + t.getName());
						return true;
					}
				}
			}
		}else if(t.equals(UnitType.getUnitType(zergling)) || t.equals(UnitType.getUnitType(hydralisk))
				|| t.equals(UnitType.getUnitType(drone)) || t.equals(UnitType.getUnitType(overlord))){
			if(getMinerals() >= t.mineralPrice() && getSupply() >= t.supplyRequired()
					&& Game.getInstance().self().gas() >= t.gasPrice() && !larvae.isEmpty()){
				Unit morpher = (Unit)findClosest(larvae,area);
				morpher.morph(t);
				return true;
			}
		}else if(t.equals(UnitType.getUnitType(extractor))){
			if(getMinerals() >= t.mineralPrice())
				return false;
			Set<ROUnit> geysers = myMap.getGasSpots();
			ArrayList<Unit> geyserList = new ArrayList<Unit>();
			for(ROUnit g: geysers){
				geyserList.add((Unit) g);
			}
			ROUnit geyser = findClosest(geyserList, area);
			Unit morpher = (Unit) findClosest(drones,area);
			morpher.build(geyser.getTilePosition(), t);
			lastBuilder = morpher;
			buildLock = true;
			System.out.println("Building extractor");
			return true;
		}else if(t.equals(lurker)){
			List<ROUnit> hydras = UnitUtils.getAllMy(UnitType.getUnitType(hydralisk));
			if(getMinerals() >= t.mineralPrice() && getSupply() >= 1
					&& Game.getInstance().self().gas() >= t.gasPrice() && !hydras.isEmpty()){
				((Unit)hydras.get(0)).morph(t);
				return true;
			}
		}else if(t.equals(lair)){
			if(getMinerals() >= t.mineralPrice() && Game.getInstance().self().gas() >= t.gasPrice()){
				Unit morpher = (Unit) findClosest(bases,area);
				morpher.morph(t);
				System.out.println("Building Lair");
				return true;
			}
		}
		return false;
	}
	
	public TilePosition findBuildRadius(TilePosition c, int radius, Unit u, UnitType t){
		TilePosition tp;
		TilePosition next;
		TilePosition prev;
		TilePosition top;
		TilePosition bottom;
		for(int y = -radius; y <= radius; y+=1){
			for(int x = -radius; x <= radius; x+=1){
				tp = new TilePosition(c.x()+x,c.y()+y);
				next = new TilePosition(c.x()+x+2,c.y()+y);
				prev = new TilePosition(c.x()+x-2,c.y()+y);
				top = new TilePosition(c.x()+x,c.y()+y+2);
				bottom = new TilePosition(c.x()+x,c.y()+y-2);
				if(u.canBuildHere(tp, t))
					return tp;
			}
		}
		return null;
	}
	
	@Override
	public void setUpBuildOrder() {
		// TODO Auto-generated method stub
		for(int i = 0; i < 5; i++) {
			buildOrder.add(new BuildCommand(drone));
		}
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(spawningPool));
		buildOrder.add(new BuildCommand(extractor));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		//3 drones on gas
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(zergling));
		buildOrder.add(new BuildCommand(lingSpeed));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(lair));
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drop));
		buildOrder.add(new BuildCommand(den));
		buildOrder.add(new BuildCommand(lurker_up));
		buildOrder.add(new BuildCommand(hydralisk));
		buildOrder.add(new BuildCommand(hydralisk));
		buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(extractor));
	}
	
	@Override
	public void onFrame(){
		
		  for(Unit u: drones) {
			  	if(u.isIdle()) {
			  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
			  		((Unit)u).rightClick(closestPatch);
			  	}
		}  
		buildNext();
	}
	
	@Override
	public void onUnitShow(ROUnit unit){
		if(unit.getType().isBuilding()){
			myMap.addBuilding(unit);
		}
		//if(unit.getTilePosition().equals(scoutTarget))
			//scoutTarget = null;
	}
	
	@Override
	public void onUnitCreate(ROUnit unit){
		if(!unit.getPlayer().equals(Game.getInstance().self()))
				return;
		if(((Unit)unit).getType().isBuilding()){
			buildLock = false;
		}
		
		Unit u = (Unit) unit;
		if(u.getType().equals(UnitType.getUnitType(hatchery)))
			bases.add(u);
		if(u.getType().equals(UnitType.getUnitType(larva)))
			larvae.add(u);
		if(u.getType().equals(UnitType.getUnitType(drone)))
			drones.add(u);
		if(u.getType().equals(UnitType.getUnitType(overlord)))
			ovies.add(u);
		if(u.getType().equals(UnitType.getUnitType(zergling)))
			lings.add(u);
		if(u.getType().equals(UnitType.getUnitType(hydralisk)))
			hydras.add(u);
		if(u.getType().equals(UnitType.getUnitType(lurker)))
			lurkers.add(u);
		
	}

}
