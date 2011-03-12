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
