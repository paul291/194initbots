package fixedBots;

import java.util.ArrayList;
import java.util.List;

import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

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
	
	boolean buildLock = false;
	List<BuildCommand> buildOrder = new ArrayList<BuildCommand>();
	Unit lastBuilder = null;
	BuildCommand lastOrder = null;
	
	public void buildNext(){
		if(!buildOrder.isEmpty()&&!buildLock){
			if(createUnit(UnitType.getUnitType(buildOrder.get(0).order),buildOrder.get(0).loc))
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
		if(t.equals(UnitType.getUnitType(overlord))){
			if(UnitUtils.getAllMy(UnitType.getUnitType(hatchery)).isEmpty() && getMinerals() >= 100){
				myBases.get(0).train(t);
				return true;
			}
		}else if(t.equals(UnitType.getUnitType(drone))){
			List larvae = UnitUtils.getAllMy(UnitType.getUnitType(larva));
			if(getMinerals() > 50 && getSupply() > 0 && !larvae.isEmpty()){
				Unit morpher = (Unit) larvae.get(0);
				morpher.morph(UnitType.getUnitType(drone));
				return true;
			}
		}else if(t.equals(spawningPool)){
			List drones = UnitUtils.getAllMy(UnitType.getUnitType(drone));
			if(getMinerals() > 150 && !drones.isEmpty()){
				Unit morpher = findClosest(drones,area);
				TilePosition tp;
				for(int i = 8; i < 15; i++){
					tp = findBuildRadius(area,i,morpher,t);
					if(tp!=null){
						morpher.build(tp,t);
						lastBuilder = morpher;
						buildLock = true;
						return true;
					}
				}
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
		
	}
	
	@Override
	public void onFrame(){
		
	}
	
	@Override
	public void onUnitShow(ROUnit unit){
		super.onUnitCreate(unit);
		if(unit.getType().isBuilding()){
			myMap.addBuilding(unit);
		}
		//if(unit.getTilePosition().equals(scoutTarget))
			//scoutTarget = null;
	}
	
	@Override
	public void onUnitCreate(ROUnit unit){
		
	}

}

