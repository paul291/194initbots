package fixedBots;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Order;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TechType;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;
import org.bwapi.proxy.model.UpgradeType;

import edu.berkeley.nlp.starcraft.overmind.Overmind;
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
	String ovieSpeed = "Zerg Overlord Speed Upgrade";
	
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
	Unit spawnPool;
	Unit hydraDen;
	Unit extractDrone;
	Unit myExtractor;
	
	private Set<TilePosition> scouted = new HashSet<TilePosition>();
	private boolean toScout = false;
	private Unit scout;
	private TilePosition scoutTarget;
	
	boolean dropTech;
	boolean lurkTech;
	
	public void buildNext(){
		if(!buildOrder.isEmpty()&&!buildLock){
			if(buildOrder.get(0).order.equals(lingSpeed)){
				if(spawnPool == null || !spawnPool.isCompleted()){
					System.out.println("No spawn pool");
					return;
				}else if(getMinerals() >= 100 && Game.getInstance().self().gas() >= 100){
					System.out.println("Upgrading");
					spawnPool.upgrade(UpgradeType.METABOLIC_BOOST);
					buildOrder.remove(0);
				}
			}else if(buildOrder.get(0).order.equals(drop)){
				List<ROUnit> lairs = UnitUtils.getAllMy(UnitType.getUnitType(lair));
				if(lairs.isEmpty()){
					System.out.println("No lairs");
				}else if(getMinerals() >= 200 && Game.getInstance().self().gas() >= 200){
					if(lairs.get(0).isBeingConstructed()||lairs.get(0).isUpgrading())
						return;
					UnitUtils.assumeControl(lairs.get(0)).upgrade(UpgradeType.VENTRAL_SACS);
					buildOrder.remove(0);
					dropTech = true;
				}
			}else if(buildOrder.get(0).order.equals(lurker_up)){
				List<ROUnit> dens = UnitUtils.getAllMy(UnitType.getUnitType(den));
				if(dens.isEmpty()){
					System.out.println("No dens");
					return;
				} else if(getMinerals() >= 200 && Game.getInstance().self().gas() >= 200){
					if(hydraDen == null || hydraDen.isBeingConstructed())
						return;
					hydraDen.research(TechType.LURKER_ASPECT);
					lurkTech = true;
					buildOrder.remove(0);
				}
			}else if(buildOrder.get(0).order.equals(ovieSpeed)) {
				List<ROUnit> lairs = UnitUtils.getAllMy(UnitType.getUnitType(lair));
				if(lairs.isEmpty()){
					System.out.println("No lairs");
				}else if(getMinerals() >= 150 && Game.getInstance().self().gas() >= 150){
					if(lairs.get(0).isBeingConstructed()||lairs.get(0).isUpgrading())
						return;
					UnitUtils.assumeControl(lairs.get(0)).upgrade(UpgradeType.PNEUMATIZED_CARAPACE);
					buildOrder.remove(0);
				}
			}else if(createUnit(UnitType.getUnitType(buildOrder.get(0).order),buildOrder.get(0).loc)){
				lastOrder = buildOrder.remove(0);
			}
		}else if(buildLock){
			//check to see if builder still actually going to build 
			if(lastBuilder.getOrder().equals(Order.MINING_MINERALS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_GAS)||
					lastBuilder.getOrder().equals(Order.MOVE_TO_MINERALS)||
					lastBuilder.getOrder().equals(Order.GUARD)||
					lastBuilder.isGatheringGas()||
					lastBuilder.isGatheringMinerals()){
				buildLock = false;
				drones.add(lastBuilder);
				buildOrder.add(0,lastOrder);
			}
		}else{
			System.out.println(lastBuilder.getOrder());
		}
	}
	
	public boolean createUnit(UnitType t, TilePosition area){
		if(area==null){
			area = bases.get(0).getTilePosition();
		}
		//System.out.println("Trying to build " + t.getName());
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
						drones.remove(morpher);
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
				if(t.equals(UnitType.getUnitType(zergling)) && (spawnPool==null ||!spawnPool.isCompleted()))
					return false;
				if(t.equals(UnitType.getUnitType(hydralisk)) && (hydraDen==null ||!hydraDen.isCompleted()))
					return false;
				morpher.morph(t);
				larvae.remove(morpher);
				return true;
			}
		}else if(t.equals(UnitType.getUnitType(extractor))){
			if(getMinerals() >= t.mineralPrice())
				return false;
			Set<ROUnit> geysers = myMap.getGasSpots();
			ArrayList<Unit> geyserList = new ArrayList<Unit>();
			//Unit morpher = (Unit)findClosest(drones,area);
			Unit morpher = drones.get(0);
			ROUnit closestPatch = UnitUtils.getClosest(drones.get(0), Game.getInstance().getGeysers());
			if (closestPatch != null) {
				drones.get(0).build(closestPatch.getTilePosition(), UnitType.getUnitType("Zerg Extractor"));
			}else
				return false;
			//System.out.println(geyser);
			//morpher.build(geyser.getTilePosition(), t);
			//drones.remove(2);
			lastBuilder = morpher;
			buildLock = true;
			System.out.println("Building extractor");
			return true;
		}else if(t.equals(UnitType.getUnitType(lurker))){
			if(lurkTech&&!hydraDen.isResearching()&&getMinerals() >= t.mineralPrice() && getSupply() >= 1
					&& Game.getInstance().self().gas() >= t.gasPrice() && !hydras.isEmpty()){
				hydras.get(0).morph(t);
				hydras.remove(0);
				return true;
			}else if(hydras.isEmpty()){
				buildOrder.add(new BuildCommand(hydralisk));
			}
		}else if(t.equals(UnitType.getUnitType(lair))){
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
		//buildOrder.add(new BuildCommand(extractor));
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
		//buildOrder.add(new BuildCommand(ovieSpeed));
		buildOrder.add(new BuildCommand(hydralisk));
		buildOrder.add(new BuildCommand(hydralisk));
		//buildOrder.add(new BuildCommand(overlord));
		buildOrder.add(new BuildCommand(drone));
		buildOrder.add(new BuildCommand(drone));
		//buildOrder.add(new BuildCommand(drone));
		for(int i = 0; i<5; i++){
			buildOrder.add(new BuildCommand(hydralisk));
		}
		for(int i = 0; i<6; i++){
			buildOrder.add(new BuildCommand(lurker));
		}
		buildOrder.add(new BuildCommand(ovieSpeed));
		
		//buildOrder.add(new BuildCommand(extractor));
	}
	
	@Override
	public void onFrame(){
		for(Unit u: drones) {
			  	if(u.isIdle()) {
			  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
			  		u.rightClick(closestPatch);
			  	}
		} 
		gasFrame();
		buildNext();
		attack();
		scout();
		if(drones.size() >=9)
			toScout = true;
	}
	
	public void scout(){
		if(ovies.isEmpty())
			return;
		else if(scout == null)
			scout = ovies.get(0);
		
		if(toScout&&!scouted.containsAll(myMap.getStartSpots())){
			if(scoutTarget != null) return;
			for(TilePosition tp: myMap.getStartSpots()){
				if(scouted.contains(tp)) continue;
				scoutTarget = tp;
			}
			if(scoutTarget == null){
				scout.rightClick(Game.getInstance().self().getStartLocation());
			}
			scout.rightClick(scoutTarget);
		}
		
		if(scoutTarget!=null){
			if(close(scout.getTilePosition(), scoutTarget))
				scoutTarget = null;
		}
	}
	
	public void attack(){
		for(Unit u: lurkers){
			if(!u.isBurrowed())
				u.burrow();
		}
	}
	
	public void gasFrame(){
		if(myExtractor == null && spawnPool!=null && spawnPool.isBeingConstructed()){
			if(extractDrone==null)
				extractDrone = drones.get(0);
			ROUnit closestPatch = UnitUtils.getClosest(drones.get(0), Game.getInstance().getGeysers());
			if (closestPatch != null) {
				extractDrone.build(closestPatch.getTilePosition(), UnitType.getUnitType("Zerg Extractor"));
				drones.remove(extractDrone);
			}
		}
		if(myExtractor!=null&&myExtractor.isCompleted()){
			int gas = 0;
			for(Unit d: drones){
				if(d==null) continue;
				if(gas==3)
					break;
				if(!d.isGatheringGas())
					d.rightClick(myExtractor);
				gas++;
			}
		}
	}
	
	@Override
	public void onStart(){
		super.onStart();
		setUpBuildOrder();
		scouted.add(Game.getInstance().self().getStartLocation());
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
		
		Unit u = UnitUtils.assumeControl(unit);
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
	@Override
	public void onUnitMorph(ROUnit unit){
		if(!unit.getPlayer().equals(Game.getInstance().self()))
			return;
		if(((Unit)unit).getType().isBuilding()){
			buildLock = false;
		}

		Unit u = UnitUtils.assumeControl(unit);
		if(u.getType().equals(UnitType.getUnitType(hatchery)))
			bases.add(u);
		//if(u.getType().equals(UnitType.getUnitType(larva)))
			//larvae.add(u);
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
		if(u.getType().equals(UnitType.getUnitType(spawningPool)))
			spawnPool = u;
		if(u.getType().equals(UnitType.getUnitType(den)))
			hydraDen = u;
		if(u.getType().equals(UnitType.getUnitType(extractor)))
			myExtractor = u;
	}
	
	public static void main(String[] args) {
		ProxyBotFactory factory = new ProxyBotFactory() {

			@Override
			public ProxyBot getBot(Game g) {
				return new Overmind(new MineMineMine(), new Properties());
			}

		};
		new ProxyServer(factory, ProxyServer.extractPort(args.length> 0 ? args[0] : null)).run();

	}

}
