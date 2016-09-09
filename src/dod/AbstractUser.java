package dod;

import dod.game.CommandException;
import dod.game.CompassDirection;
import dod.game.GameLogic;
import dod.game.PlayerListener;

public abstract class AbstractUser implements PlayerListener, Runnable {
	private GameLogic game;

	public void setGame(GameLogic game) {
		this.game = game;
		return;
	}
	
	public abstract void run();
	
    protected abstract void outputMessage(String message);
    
    // Notifies a player of a change to the map
    public void change() {
    	outputMessage("CHANGE");
    }
    
    // Disconnects the player
    public void disconnectPlayer(int playerID) {
    	outputMessage("DISCONNECTED");
    }
    /**
     * Sends a message to the player from the game.
     * 
     * @ param message The message to be sent
     */
    public void sendMessage(String message) {
    	outputMessage("MESSAGE " + message);
    }
    
   public void sendWhisper(String message) {
	   outputMessage("WHISPER " + message);
   }
    
    /**
     * Informs the user of the beginning of a player's turn
     */
    public void startTurn() {
	outputMessage("STARTTURN");
    }

    /**
     * Informs the user of the end of a player's turn
     */
    public void endTurn() {
	outputMessage("ENDTURN");
    }

    /**
     * Informs the user that the player has won
     */
    public void win() {
	outputMessage("WIN");
    }

    // Informs the user that the player has lost
    public void lose() {
    	outputMessage("LOSE");
    }
    /**
     * Informs the user that the player's hit points have changed
     */
    public void hpChange(int value) {
	outputMessage("HITMOD " + value);
    }

    /**
     * Informs the user that the player's gold count has changed
     */
    public void treasureChange(int value) {
	outputMessage("TREASUREMOD " + value);
    }
    
    public void hasLantern() {
    	outputMessage("TORCH PICKUP");
    }

    /**
     * Processes a text command from the user.
     * 
     * @param commandString
     *            the string containing the command and any argument
     */
    protected final void processCommand(String commandString, int playerID) {
	// Because continuously pressing the shift key while testing made my
	// finger hurt...
 //   try {
  //  	commandString = commandString.toUpperCase();
   // } catch (NullPointerException e) {
   // 	Thread.currentThread().interrupt();
    //}
	// Process the command string e.g. MOVE N
	final String commandStringSplit[] = commandString.split(" ", 2);
	final String command = commandStringSplit[0];
	final String arg = ((commandStringSplit.length == 2) ? commandStringSplit[1]
		: null);

	try {
	    processCommandAndArgument(command, arg, playerID);
	} catch (final CommandException e) {
	    outputMessage("FAIL " + e.getMessage());
	}
    }

    /**
     * Processes the command and an optional argument
     * 
     * @param command
     *            the text command
     * @param arg
     *            the text argument (null if no argument)
     * @throws CommandException
     */
    private void processCommandAndArgument(String command, String arg, int playerID)
	    throws CommandException {
	if (command.equals("HELLO")) {
	    if (arg == null) {
		throw new CommandException("HELLO needs an argument");
	    }

	    final String name = sanitiseMessage(arg);
	    this.game.clientHello(name, playerID);
	    outputMessage("HELLO " + name);

	} else if (command.equals("LOOK")) {
	    if (arg != null) {
		throw new CommandException("LOOK does not take an argument");
	    }

	    outputMessage("LOOKREPLY" + System.getProperty("line.separator")
		    + this.game.clientLook(playerID));

	} else if (command.equals("PICKUP")) {
	    if (arg != null) {
		throw new CommandException("PICKUP does not take an argument");
	    }

	    this.game.clientPickup(playerID);
	    outputSuccess();


	} else if (command.equals("MOVE")) {
	    // We need to know which direction to move in.
	    if (arg == null) {
		throw new CommandException("MOVE needs a direction");
	    }

	    this.game.clientMove(getDirection(arg), playerID);

	    outputSuccess();

	} else if (command.equals("ATTACK")) {
	    // We need to know which direction to move in.
	    if (arg == null) {
		throw new CommandException("ATTACK needs a direction");
	    }

	    this.game.clientAttack(getDirection(arg), playerID);

	    outputSuccess();

	} else if (command.equals("ENDTURN")) {
	    this.game.clientEndTurn(playerID);

	} else if (command.equals("SHOUT")) {
	    // Ensure they have given us something to shout.
	    if (arg == null) {
		throw new CommandException("need something to shout");
	    }

	    this.game.clientShout(arg, playerID);
	} else if (command.equals("WHISPER")) {
		if (arg == null) {
			throw new CommandException("need something to whisper");
		}
		
		final String[] whisperSplit = arg.split(" ", 2);
		this.game.clientWhisper(whisperSplit[1], playerID, whisperSplit[0]);
		
	} else if (command.equals("SETPLAYERPOS")) {
	    if (arg == null) {
		throw new CommandException("need a position");
	    }

	    // Obtain two co-ordinates
	    final String coordinates[] = arg.split(" ");

	    if (coordinates.length != 2) {
		throw new CommandException("need two co-ordinates");
	    }

	    try {
		final int col = Integer.parseInt(coordinates[0]);
		final int row = Integer.parseInt(coordinates[1]);

		this.game.setPlayerPosition(col, row, playerID);
		outputSuccess();
	    } catch (final NumberFormatException e) {
		throw new CommandException("co-ordinates must be integers");
	    }

	} else if (command.equals("DISCONNECT")) {
		this.game.disconnectPlayer(playerID);
	} else {	
	    // If it is none of the above then it must be a bad command.
	    throw new CommandException("invalid command");
	}
    }

    /**
     * Obtains a compass direction from a string. Used to ensure the correct
     * exception type is thrown, and for consistency between MOVE and ATTACK.
     * 
     * @param string
     *            the direction string
     * 
     * @return the compass direction
     * @throws CommandException
     */
    private CompassDirection getDirection(String string)
	    throws CommandException {
	try {
	    return CompassDirection.fromString(string);
	} catch (final IllegalArgumentException e) {
	    throw new CommandException("invalid direction");
	}
    }

    /**
     * Sanitises the given message - there are some characters that we can put
     * in the messages that we don't want in other stuff that we sanitise.
     * 
     * @param s
     *            The message to be sanitised
     * @return The sanitised message
     */
    private static String sanitiseMessage(String s) {
	return sanitise(s, "[a-zA-Z0-9-_ \\.,:!\\(\\)#]");
    }

    /**
     * Strip out anything that isn't in the specified regex.
     * 
     * @param s
     *            The string to be sanitised
     * @param regex
     *            The regex to use for sanitisiation
     * @return The sanitised string
     */
    private static String sanitise(String s, String regex) {
	String rv = "";

	for (int i = 0; i < s.length(); i++) {
	    final String tmp = s.substring(i, i + 1);

	    if (tmp.matches(regex)) {
		rv += tmp;
	    }
	}

	return rv;
    }
    
    /**
     * Sends a success message in the event that a command has succeeded
     */
    private void outputSuccess() {
	outputMessage("SUCCESS");
    }
}
