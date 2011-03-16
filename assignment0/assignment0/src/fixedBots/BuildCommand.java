package fixedBots;

import org.bwapi.proxy.model.TilePosition;

public class BuildCommand {
	String order;
	TilePosition loc;
	public BuildCommand(String s, TilePosition l){
		order = s;
		loc = l;
	}
	
	public BuildCommand(String s) {
		order = s;
		loc = null;
	}
	
	public boolean equals(BuildCommand c){
		return c.equals(order);
	}
}
