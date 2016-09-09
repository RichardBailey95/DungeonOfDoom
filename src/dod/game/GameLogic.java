package dod.game;

import java.io.FileNotFoundException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Random;

import dod.game.items.GameItem;

/**
 * This class controls the game logic and other such magic.
 */
public class GameLogic {
    Map map;

    // Has a player won already?
    private boolean playerWon = false;

    // For now we only have one player and one listener, but these could later
    // become lists of players
    private ArrayList<Player> player = new ArrayList<Player>();
    
    // Tracks which players turn it is. -1 indicates the game hasn't started.
    private int playerTurn = -1;

    /**
     * Constructor that specifies the map which the game should be played on.
     * 
     * @param mapFile
     *            The name of the file to load the map from.
     * @throws FileNotFoundException
     *             , ParseException
     */
    public GameLogic(String mapFile) throws FileNotFoundException,
	    ParseException {
	this.map = new Map(mapFile);

	// Check if there is enough gold to win
	if (this.map.remainingGold() < this.map.getGoal()) {
	    throw new IllegalStateException(
		    "There isn't enough gold on this map for you to win");
	}
    }

    /**
     * Starts a new game of the Dungeon of Dooooooooooooom.
     * @throws CommandException 
     */
    public void startNewGame() throws CommandException {
	if (player.get(0) == null) {
	    throw new IllegalStateException(
		    "FAIL: There is no player on the map");
	}
	for(int i=0; i<player.size(); i++) {
     	player.get(i).sendMessage("SERVER GAME");
		player.get(i).sendMessage("Server: The game has begun");

    }
	playerTurn = 0;
	startTurn();
    }

    /**
	     * Adds a new player to the game. For now, only one player can be added.
	     * 
	     * @param listener
	     *            the PlayerListener which will listen on behalf of that player,
	     *            so messages can be sent to the player
	     */
	    public void addPlayer(PlayerListener listener, int playerID) {
	//	if (this.player != null) {
		    // So far we can only add one player.
	//	    throw new UnsupportedOperationException(
	//		    "FAIL: Only one player can be added.");
	//	}
	
		Player newPlayer = new Player("TempName", generateRandomStartLocation(), listener, playerID);
		System.out.println("I will try add the player to the list of players.");
		player.add(newPlayer);
	    }

	/**
     * Handles the client message HELLO
     * 
     * @param newName
     *            the name of the player to say hello to
     * @return the message to be passed back to the command line
     * @throws CommandException
     */
    public void clientHello(String newName, int playerID) throws CommandException {
	assertPlayerExists(playerID);

	// Change the player name and then say hello to them
	player.get(playerID).setName(newName);
    }

    /**
     * Handles the client message LOOK Shows the portion of the map that the
     * player can currently see.
     * 
     * @return the part of the map that the player can currently see.
     */
    public synchronized String clientLook(int playerID) {
	assertPlayerExists(playerID);

	// Work out how far the player can see
	final int distance = player.get(playerID).lookDistance();

	String lookReply = "";
	// Iterate through the rows.
	for (int rowOffset = -distance; rowOffset <= distance; ++rowOffset) {
	    String line = "";

	    // Iterate through the columns.
	    for (int colOffset = -distance; colOffset <= distance; ++colOffset) {

		// Work out the location
		final Location location = player.get(playerID).getLocation().atOffset(
			colOffset, rowOffset);

		char content = '?';
		
		if (!player.get(playerID).canSeeTile(rowOffset, colOffset)) {
		    // It's outside the FoV so we don't know what it is.
		    content = 'X';
		} else if (!this.map.insideMap(location)) {
		    // It's outside the map, so just call it a wall.
		    content = '#';
	    } else {
		    // Look up and see what's on the map
		    content = this.map.getMapCell(location).toChar();
		    for(int i=0; i<player.size(); i++) {
		    	if (!(i == playerID)) {
		    		Location playerLocation = player.get(i).getLocation();
		    		if(player.get(i).isDead()){
		    			//Do not show any dead players.
		    		} else if(location.getRow() == playerLocation.getRow()){
		    			if(location.getCol() == playerLocation.getCol()){
		    				content = 'P';
		    			}
		    		}
		    	}
		    }
		}
		
		// Add to the line
		line += content;
	    }

	    // Send a line of the look message
	    lookReply += line + System.getProperty("line.separator");
	}

	return lookReply;
    }

    /**
     * Handles the client message MOVE
     * 
     * Move the player in the specified direction - assuming there isn't a wall
     * in the way
     * 
     * @param direction
     *            The direction (NESW) to move the player
     * @return An indicator of the success or failure of the movement.
     * @throws CommandException
     */
    public synchronized void clientMove(CompassDirection direction, int playerID) throws CommandException {
	assertPlayerExists(playerID);
	ensureNoWinner();
	assertPlayerAP(playerID);
	assertPlayerTurn(playerID);

	// Work out where the move would take the player
	final Location location = player.get(playerID).getLocation().atCompassDirection(
		direction);

	// Ensure that the movement is within the bounds of the map and not
	// into a wall
	if (!this.map.insideMap(location)
		|| !this.map.getMapCell(location).isWalkable()) {
		player.get(playerID).sendMessage("Server: You cannot move into a wall");
	    throw new CommandException("can't move into a wall");
	}
	
	// Ensure that the movement is not into another player
	for(int i=0; i<player.size(); i++){
		Location playerLocation = player.get(i).getLocation();
		if(player.get(i).isDead()){
			//Do not restrict movement over any dead players.
		} else if(location.getCol() == playerLocation.getCol()){
			if(location.getRow() == playerLocation.getRow()){
	    		player.get(playerID).sendMessage("Server: You cannot move into another player");
				throw new CommandException("can't move into a player");
			}
		}
	}
	// Costs one action point
	player.get(playerID).decrementAp();

	// Move the player
	player.get(playerID).setLocation(location);
	
	// Notify the other players of a change
	for(int i=0; i<player.size(); i++) {
    	if (!(i == playerTurn)) {
    		player.get(i).change();
    	}
    }

	// Continue or end the turn
	if (player.get(playerTurn).remainingAp() == 0) {
		clientEndTurn(playerID);
	} else {
		advanceTurn(playerID);
	}
	
	return;
    }

    /**
     * Handles the client message ATTACK
     * 
     * Note: In the single player version of the game this doesn't do anything
     * 
     * @param direction
     *            The direction in which to attack
     * @return A message indicating the success or failure of the attack
     * @throws CommandException
     */
    public void clientAttack(CompassDirection direction, int playerID)
	    throws CommandException {
	assertPlayerExists(playerID);
	ensureNoWinner();
	assertPlayerAP(playerID);
	assertPlayerTurn(playerID);

	// Work out which square we're targeting
	// Location location =
	// this.player.getLocation().locationAtCompassDirection(direction);

	/**
	 * TODO .... This code does not need to be filled in until Coursework 3!
	 * 
	 * 1. Work out which player the attack is on...
	 * 
	 * 2. Have you hit the target? - hint, you might want to make the chance
	 * of a successful attack 75%?
	 * 
	 * 2.1 if the player has hit the target then hp of the target should be
	 * reduced based on this formula...
	 * 
	 * damage = 1 + player.sword - target.armour
	 * 
	 * i.e. the damage inflicted is 1 + 1 if the player has a sword and - 1
	 * if the target has armour.
	 * 
	 * Player and target are informed about the attack as set out in the
	 * wire_spec
	 * 
	 * 2.2 if the player has missed the target then nothing happens.
	 * Optionally, the player and target can be informed about the failed
	 * attack
	 * 
	 */

	throw new CommandException("attacking (" + direction.toString()
		+ ") has not been implemented");
    }

    /**
     * Handles the client message PICKUP. Generally it decrements AP, and gives
     * the player the item that they picked up Also removes the item from the
     * map
     * 
     * @return A message indicating the success or failure of the action of
     *         picking up.
     * @throws CommandException
     */
    public synchronized void clientPickup(int playerID) throws CommandException {
	assertPlayerExists(playerID);
	ensureNoWinner();
	assertPlayerAP(playerID);
	assertPlayerTurn(playerID);

	final Tile playersTile = this.map.getMapCell(player.get(playerID).getLocation());

	// Check that there is something to pick up
	if (!playersTile.hasItem()) {
		player.get(playerID).sendMessage("Server: There is nothing to pick up");
	    throw new CommandException("nothing to pick up");
	}

	// Get the item
	final GameItem item = playersTile.getItem();

	if (player.get(playerID).hasItem(item)) {
		player.get(playerID).sendMessage("Server: You already have that item");
	    throw new CommandException("already have item");
	}

	player.get(playerID).giveItem(item);
	playersTile.removeItem();
	if(item.toString().equals("health potion")) {
		player.get(playerID).zeroAP();
	} else {
		player.get(playerID).decrementAp();
	}
	
	// Notify the other players of a change
	for(int i=0; i<player.size(); i++) {
    	if (!(i == playerTurn)) {
    		player.get(i).change();
    	}
    }
	
	if (player.get(playerTurn).remainingAp() == 0) {
		clientEndTurn(playerID);
	} else {
		advanceTurn(playerID);
	}
    }

    /**
     * Returns the current message to the client. Note that this becomes
     * important when using multiple clients across a network, where this could
     * send to multiple players
     * 
     * @param message
     *            The message to be shouted
     */
    public void clientShout(String message, int playerID) {
    	message = player.get(playerID).getName() + ": " + message;
    	for(int i=0; i<player.size(); i++) {
      		player.get(i).sendMessage(message);
        }
    }
    
    public void clientWhisper(String message, int playerID, String playerWhispered) {
    	boolean whispered = false;
    	for(int i=0; i<player.size(); i++) {
    		if (player.get(i).getName().equals(playerWhispered)) {
    			player.get(i).sendWhisper("From "+player.get(playerID).getName() + ": " + message);
    			whispered = true;
    		}
    	}
    	if(!whispered){
        	player.get(playerID).sendMessage("Server: Player does not exist");
    	} else {
        	player.get(playerID).sendWhisper("To "+playerWhispered+": "+message);

    	}
    }

    /**
     * Handles the client message ENDTURN
     * 
     * Just sets the AP to zero and advances as normal.
     * 
     * @return A message indicating the status of ending a turn (currently
     *         always successful).
     * @throws CommandException 
     */
    public void clientEndTurn(int playerID) throws CommandException {
	assertPlayerExists(playerID);
	assertPlayerTurn(playerID);
	player.get(playerTurn).endTurn();
	advanceTurn(playerID);
    }

    /**
     * Sets the player's position. This is used as a cheating or debug command.
     * It is particularly useful for testing, as it gets rounds the randomness
     * of the player start position.
     * 
     * @param col
     *            the column of the location to put the player
     * @param row
     *            the row to location to put the player
     * @throws CommandException
     */
    public synchronized void setPlayerPosition(int col, int row, int playerID) throws CommandException {
	assertPlayerExists(playerID);
	assertPlayerTurn(playerID);
	final Location location = new Location(col, row);

	if (!this.map.insideMap(location)) {
	    throw new CommandException("invalid position");
	}

	if (!this.map.getMapCell(location).isWalkable()) {
		player.get(playerID).sendMessage("Server: You can't walk on this tile");
	    throw new CommandException("cannot walk on this tile");
	}

	player.get(playerID).setLocation(location);
	
	// Notify other players of a change
	for(int i=0; i<player.size(); i++) {
    	if (!(i == playerTurn)) {
    		player.get(i).change();
    	}
    }
    }

    /**
     * Passes the goal back
     * 
     * @return the current goal
     */
    public int getGoal() {
	return this.map.getGoal();
    }

    /**
     * Generates a randomised start location
     * 
     * @return a random location where a player can start
     */
    private Location generateRandomStartLocation() {
	if (!atLeastOneNonWallLocation()) {
	    throw new IllegalStateException(
		    "There is no free tile available for the player to be placed");
	}

	while (true) {
	    // Generate a random location
	    final Random random = new Random();
	    final int randomRow = random.nextInt(this.map.getMapHeight());
	    final int randomCol = random.nextInt(this.map.getMapWidth());

	    final Location location = new Location(randomCol, randomRow);

	    if (this.map.getMapCell(location).isWalkable()) {
		// If it's not a wall then we can put them there
		return location;
	    }
	}
    }

    /**
     * Searches a possible tile to use by the player, i.e. non-wall. The map is
     * traversed from (0,0) to (maxY,MaxX).
     * 
     * @return true if there is at least one non-wall location, false otherwise
     */
    private boolean atLeastOneNonWallLocation() {
	for (int x = 0; x < this.map.getMapWidth(); x++) {
	    for (int y = 0; y < this.map.getMapHeight(); y++) {

		if (this.map.getMapCell(new Location(x, y)).isWalkable()) {
		    // If it's not a wall then we can put them there
		    return true;
		}
	    }
	}

	return false;
    }

    /**
     * Ensures a player has been added to the map. Otherwise, an exception is
     * raised. In a multiplayer scenario, this could ensure a player by given ID
     * exists.
     * 
     * @throws RuntimeException
     */
    private void assertPlayerExists(int playerID) throws RuntimeException {
	if (player.get(playerID) == null) {
	    throw new IllegalStateException(": Player has not been added.");
	}
    }

    /**
     * Ensures a player has enough AP, otherwise a runtime error is raised,
     * since the turn should have been advanced. In a multiplayer example, this
     * is still a bug, since the server should have checked whose turn it was.
     * 
     * @throws RuntimeException
     */
    private void assertPlayerAP(int playerID) throws RuntimeException {
	if (player.get(playerID).remainingAp() == 0) {
	    throw new IllegalStateException("Player has 0 ap");
	}
    }

    /**
     * Ensure that no player has won the game. Throws a CommandException if
     * someone has one, preventing the command from executing
     * 
     * @throws CommandException
     */
    private void ensureNoWinner() throws CommandException {
	if (this.playerWon) {
	    throw new CommandException("the game is over");
	}
    }
    
    // Ensure that it is the players turn
    private void assertPlayerTurn(int playerID) throws RuntimeException {
    	if (!(playerID == playerTurn)) {
    		player.get(playerID).sendMessage("Server: It is not your turn");
    		throw new IllegalStateException("Not players turn");
    	}
    }

    /**
     * This doesn't really do anything as yet, other than reset the player AP.
     * It should do more when the game is multiplayer
     * @throws CommandException 
     */
    private void startTurn() throws CommandException {
    	ensureNoWinner();
    	int allDeadCheck = playerTurn;
    	// Check if the player is dead/disconnected
    	if (player.get(playerTurn).isDead()) {
    		playerTurn++;
    		if(playerTurn == player.size()){
    			playerTurn = 0;
    		}
    		if(playerTurn == allDeadCheck){
    			System.exit(0);
    		}
    		startTurn();
    	} else {
    	player.get(playerTurn).startTurn();
    	}
    }

    /**
     * Once a player has performed an action the game needs to move onto the
     * next turn to do this the game needs to check for a win and then test to
     * see if the current player has more AP left.
     * 
     * Note that in this implementation we currently playing this as a single
     * player game so the next turn will always be the current player so we
     * simply start their turn again.
     * @throws CommandException 
     * @throws GameWonException 
     */
    private void advanceTurn(int playerID) throws CommandException {
	// Check if the player has won
	if ((player.get(playerTurn).getGold() >= this.map.getGoal())
		&& (this.map.getMapCell(player.get(playerTurn).getLocation()).isExit())) {

	    // Player should not be able to move if they have won
	    assert (!this.playerWon);

	    this.playerWon = true;
	    player.get(playerTurn).win();
	    for(int i=0; i<player.size(); i++) {
	    	if (!(i == playerTurn)) {
	    		player.get(i).lose();
	    		disconnectPlayer(i);
	    	}
	    }
	    playerTurn = -1;

	} else {
	    // The player may die, for now, just exit. In a server client
	    // version, this might be a bit inconvenient.
	    if (player.get(playerTurn).isDead()) {
	    	disconnectPlayer(playerID);
	    }

	    if (player.get(playerTurn).remainingAp() == 0) {
	    	// Force the end of turn
	    	// clientEndTurn(playerID);
			playerTurn++;
			if(playerTurn == player.size()){
			playerTurn = 0;
			}
			startTurn();
	    }
	}
    }
    
    public void disconnectPlayer(int playerID) throws CommandException {
    	player.get(playerID).disconnectPlayer(playerID);
    	player.get(playerID).kill();
    	if(playerID == playerTurn){
    		startTurn();
    	}
    }

}
