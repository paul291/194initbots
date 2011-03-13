package fixedBots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.TilePosition;
import org.bwapi.proxy.model.Unit;
import org.bwapi.proxy.model.UnitType;

import edu.berkeley.nlp.starcraft.AbstractCerebrate;
import edu.berkeley.nlp.starcraft.Cerebrate;
import edu.berkeley.nlp.starcraft.Strategy;
import edu.berkeley.nlp.starcraft.overmind.Overmind;
import edu.berkeley.nlp.starcraft.scripting.Command;
import edu.berkeley.nlp.starcraft.scripting.JythonInterpreter;
import edu.berkeley.nlp.starcraft.scripting.Thunk;
import edu.berkeley.nlp.starcraft.util.UnitUtils;

public class TwoGateRush extends EmptyFixedBot {
	JythonInterpreter jython = new JythonInterpreter();
	private TilePosition myHome;
	private TilePosition mySPLoc;
	
	private List<Unit> myZealots;
	private List<Unit> myGateways;
	
	private final static int GAME_SPEED = 0;
	private final static int TILE_SIZE = 32;
	private final static int REL_X_BASE = 1470;
	private final static int REL_Y_BASE = 1520;
	private final static int REL_SP_LOC = 2;
	private final static int HOME_RADIUS = 300;
	private final static int NEARBY_DIA = 200;
	
	private int mySetupStage;
	private Position rallyPoint;
	private int SP_X_LOC;
	
	@Override
  public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
	  return Arrays.<Cerebrate>asList(jython,this);
  }


	@Override
  public void onFrame() {
	  
	  for(Unit u: workers) { //workers mine
	  	if(u.isIdle()) {
	  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
	  		u.rightClick(closestPatch);
	  	}
	  }
	  
	  //build up and attack
	  if(mySetupStage < 4)
		  setup();
	  else{
		  battleCycle();
		  myZealots.clear();
		  List <ROUnit> zealots = UnitUtils.getAllMy(UnitType.getUnitType("Protoss Zealots"));
		  for(ROUnit z: zealots){
			  if(true){
				  myZealots.add(UnitUtils.assumeControl(z));
			  }
		  }
		  
	  }
	  
  }
	
	public List<ROUnit> enemyUnits(){
		List<ROUnit> units = new ArrayList<ROUnit>();
		for(ROUnit u: Game.getInstance().getAllUnits()){
			if(Game.getInstance().self().isEnemy(u.getPlayer()))
				units.add(u);
		}
		return units;
	}
	
	public void setup(){
		switch(mySetupStage){
			case 0: case 1:
				if(createUnit("Protoss Probe",findFurthest(myBases,rallyPoint)))
					mySetupStage++;
				break;
			case 2:
				if(createUnit("Protoss Gateway",findClosest(workers,mySPLoc)))
					mySetupStage++;
				break;
			case 6:
				if(createUnit("Protoss Pylon",findFurthest(workers,rallyPoint)))
					mySetupStage++;
				break;
			default:
				break;
		}
	}
	
	public Unit findClosest(List<Unit> units, TilePosition p){
		int x = p.x()*TILE_SIZE;
		int y = p.y()*TILE_SIZE;
		return findClosest(units,new Position(x,y));
	}
	
	public Unit findClosest(List<Unit> units, Position p){
		double best = 10000;
		Unit bestu = null;
	
		for(Unit u: units){
			double d = u.getDistance(p);
			if(d < best){
				best = d;
				bestu = u;
			}
		}
		return bestu;
	}
	
	public Unit findFurthest(List<Unit> units, Position p){
		double best = -10000;
		Unit bestu = null;
	
		for(Unit u: units){
			double d = u.getDistance(p);
			if(d > best){
				best = d;
				bestu = u;
			}
		}
		return bestu;
	}
	
	public void battleCycle(){
		send();
	}
	
	public void send(){
		
		for(Unit u: myZealots){
			int homeDist = Math.abs(u.getPosition().x()-myHome.x()*TILE_SIZE);
			if(homeDist < HOME_RADIUS && enemyUnits().isEmpty())
				u.rightClick(rallyPoint);
			else{
				if(u.isIdle()){
					Unit t = selectTarget(u);
					if(t!=null)
						u.rightClick(t);
					else{
						Position p = randomNearby(u,NEARBY_DIA);
						u.attackMove(p);
					}
				}
				
				if(u.getOrderTarget()!=null && u.getOrderTarget().getType().isBuilding()){
					Unit t = selectTarget(u);
					if(t!=null&&!t.getType().isBuilding())
						u.rightClick(t);
				}
			}
		}
	}
	
	public Position randomNearby(Unit u, int dist){
		int dx = (int)(Math.random()*dist-dist/2);
		int dy = (int)(Math.random()*dist-dist/2);
		int x = u.getPosition().x();
		int y = u.getPosition().y();
		int newx = x+dx;
		int newy = y+dy;
		Position p =  new Position(x+dx,y+dy);
		if(Game.getInstance().mapHeight()*TILE_SIZE>newy 
			&& Game.getInstance().mapWidth()*TILE_SIZE > newx
			&& newx > 0 && newy > 0)
			return p;
		else
			return rallyPoint;
	}
	
	
	public Unit selectTarget(Unit u){
		Unit target = null;
		target = findEnemyType("Protoss Zealot",u);
		if(target == null)
			target = findEnemyType("Protoss Probe",u);
		if(target == null)
			target = findEnemyType("building",u);
		return target;
	}
	
	public Unit findEnemyType(String type, Unit me){
		List<ROUnit> enemies = enemyUnits();
		if(type.equals("building")){
			if(!enemies.isEmpty()){
				//if(u.getType().isBuilding()){
					return (Unit) UnitUtils.getClosest(me, enemies);
				//}
			}
		}else{
			List <ROUnit> possibleTargets = new ArrayList<ROUnit>();
			for(ROUnit u: enemies){
				if(u.getType().getName().equals(type)){
					possibleTargets.add(u);
				}
			}
			return (Unit) UnitUtils.getClosest(me, possibleTargets);
		}
		
		return null;
	}
	
	public boolean createUnit(String name, Unit u){
		if(name.equals("Protoss Probe")){
			if(Game.getInstance().self().minerals() >= 50 && getSupply() > 1){
				  myBases.get(0).train(UnitType.getUnitType(name));
			  }
		}else if(name.equals("Protoss Gateway")){
			if(getMinerals() >= 150){
				  if(u.canBuildHere(mySPLoc, UnitType.getUnitType(name)))
				  	u.build(mySPLoc, UnitType.getUnitType(name));
				  else{
					TilePosition p = new TilePosition(myHome.x(),myHome.y()+3);
					u.build(p, UnitType.getUnitType(name));
				  }
				  return true;
			}
		}else if(name.equals("Protoss Zealot")){
			if(getMinerals() >= 100 && getSupply() > 2){
				myGateways.get(0).train(UnitType.getUnitType(name));
				return true;
			}
		}else if(name.equals("Protoss Pylon")){
			if(getMinerals() >= 100){
				TilePosition p = new TilePosition(myHome.x(),myHome.y()-2);
				u.build(p, UnitType.getUnitType(name));
				return true;
			}
		}
		
		return false;
	}
	
	public int getMinerals(){
		return Game.getInstance().self().minerals();
	}
	
	public int getSupply(){
		return Game.getInstance().self().supplyTotal() - Game.getInstance().self().supplyUsed();
	}

	@Override
  public void onStart() {
		//System.out.println(Game.getInstance().mapHeight()*TILE_SIZE);
		
		Game.getInstance().setLocalSpeed(GAME_SPEED);
		mySetupStage = 0;
		myHome = Game.getInstance().self().getStartLocation();
		super.onStart();
		if(myHome.x()*TILE_SIZE > Game.getInstance().getMapHeight()/2*TILE_SIZE){
			rallyPoint = new Position(-REL_X_BASE+myHome.x()*TILE_SIZE,-REL_Y_BASE+myHome.y()*TILE_SIZE);
			SP_X_LOC = 4;
		}
		else{
			SP_X_LOC = -3;
			rallyPoint = new Position(REL_X_BASE+myHome.x()*TILE_SIZE,REL_Y_BASE+myHome.y()*TILE_SIZE);
		}
		
		mySPLoc = new TilePosition(myHome.x()+SP_X_LOC,(myHome.y()-REL_SP_LOC));
	}
	
	public void printLoc(Position pos){
		System.out.println("(" + pos.x() + "," + pos.y() + ")");
	}

	@Override
  public void onUnitCreate(ROUnit unit) {
		if(unit.getType().getName().equals("Protoss Probe") && !workers.contains(unit))
			workers.add(UnitUtils.assumeControl(unit));
  }


	@Override
  public void onUnitHide(ROUnit unit) {
	  
  }

	@Override
  public void onUnitMorph(ROUnit unit) {
  }

	@Override
  public void onEnd(boolean isWinnerFlag) {
	  
  }
	
	// Feel free to add command and things here.
	// bindFields will bind all member variables of the object
	// commands should be self explanatory...
	protected void initializeJython() {
		jython.bindFields(this);
		jython.bind("game", Game.getInstance());
		jython.bindIntCommand("speed",new Command<Integer>() {
			@Override
      public void call(Integer arg) {
				Game.getInstance().printf("Setting speed to %d",arg);
	      Game.getInstance().setLocalSpeed(arg);	      
      }
		});
		jython.bindThunk("reset",new Thunk() {

			@Override
      public void call() {
				initializeJython();
	      
      }
			
		});
		
  }


	@Override
	public void setUpBuildOrder() {
		// TODO Auto-generated method stub
		
	}

}


