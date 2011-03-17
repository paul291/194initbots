package fixedBots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.bwapi.proxy.ProxyBot;
import org.bwapi.proxy.ProxyBotFactory;
import org.bwapi.proxy.ProxyServer;
import org.bwapi.proxy.model.Color;
import org.bwapi.proxy.model.Game;
import org.bwapi.proxy.model.Position;
import org.bwapi.proxy.model.ROUnit;
import org.bwapi.proxy.model.Race;
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

public abstract class EmptyFixedBot extends AbstractCerebrate implements Strategy {
	JythonInterpreter jython = new JythonInterpreter();
	protected List<Unit> myBases;
	protected List<Unit> workers;
	protected Race myRace;
	protected SCMap myMap;
  
	private final static int GAME_SPEED = 0;
	private final static int TILE_SIZE = 32;
	
	@Override
  public List<Cerebrate> getTopLevelCerebrates() {
		initializeJython();
	  return Arrays.<Cerebrate>asList(jython,this);
  }


	@Override
  public void onFrame() {
	  for(Unit u: workers) {
	  	if(u.isIdle()) {
	  		ROUnit closestPatch = UnitUtils.getClosest(u, Game.getInstance().getMinerals());
	  		u.rightClick(closestPatch);
	  	}
	  }
	  if(Game.getInstance().self().minerals() >= 50){
		  UnitType uT = myRace.getWorker();
		  myBases.get(0).train(uT);
	  }
  }

	@Override
  public void onStart() {
		workers = new ArrayList<Unit>();
		myBases = new ArrayList<Unit>();
		myRace = Game.getInstance().self().getRace();
		myMap = new SCMap();
		//Game.getInstance().setLocalSpeed(GAME_SPEED);
		for(ROUnit u: Game.getInstance().self().getUnits()) {
			if(u.getType().isWorker()) {
				workers.add(UnitUtils.assumeControl(u));
			} else if(u.getType().isResourceDepot()) {
				myBases.add(UnitUtils.assumeControl(u));
			}
		}
  }
	
	public abstract void setUpBuildOrder();

	@Override
  public void onUnitCreate(ROUnit unit) {
	  
  }

	@Override
  public void onUnitDestroy(ROUnit unit) {
		if(unit.getType().isBuilding()){
			myMap.removeBuilding(unit);
		}
  }

	@Override
  public void onUnitHide(ROUnit unit) {
	  
  }

	@Override
  public void onUnitMorph(ROUnit unit) {
	  
  }
	
	@Override
  public void onUnitShow(ROUnit unit) {

		if(unit.getType().isBuilding()){
			myMap.addBuilding(unit);
		}
	  
  }
	

	@Override
  public void onEnd(boolean isWinnerFlag) {
	  
  }
	
	
	public int getMinerals(){
		return Game.getInstance().self().minerals();
	}
	
	public int getSupply(){
		return Game.getInstance().self().supplyTotal() - Game.getInstance().self().supplyUsed();
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
		return p;
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
	
	public List<ROUnit> enemyUnits(){
		List<ROUnit> units = new ArrayList<ROUnit>();
		for(ROUnit u: Game.getInstance().getAllUnits()){
			if(Game.getInstance().self().isEnemy(u.getPlayer()))
				units.add(u);
		}
		return units;
	}
	
	
	public ROUnit findClosest(List<Unit> units, TilePosition p){
		int x = p.x()*TILE_SIZE;
		int y = p.y()*TILE_SIZE;
		return findClosest(units,new Position(x,y));
	}
	
	public ROUnit findClosest(List<Unit> units, Position p){
		double best = 10000;
		ROUnit bestu = null;
	
		for(ROUnit u: units){
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
	
	public boolean close(TilePosition t1, TilePosition t2){
		int x = Math.abs(t1.x() - t2.x());
		int y = Math.abs(t1.y() - t2.y());
		return x+y < 9;
	}
	
	public boolean close(List<ROUnit> units, TilePosition t1) {
		for(ROUnit u : units) {
			if(close(u.getTilePosition(), t1))
				return true;
		}
		return false;
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
}
