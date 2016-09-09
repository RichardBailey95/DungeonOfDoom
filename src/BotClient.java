import java.io.*;
import java.net.*;
import java.util.Random;

/*
 * The BotClient class is the client which will run an AI player of the game. After connecting to the server
 * it will call the run() method which cycles through various methods to move the bot around the map.
 */
public class BotClient {
	private boolean hasLantern = false;
    private boolean hasSword = false;
    private boolean hasArmour = false;
    
	private char[][] currentLookReply;
	
	private BufferedReader in;
	private PrintWriter out;
	private int botsTurn = -1;

	/*
	 * This method processes any commands the bot needs to send to the server. Once the message is sent
	 * it will take any message the server has sent back and send it to outputMessage to be printed to
	 * the bot console.
	 */
	private void processCommand(String message){
		if(message!=null){
			out.println(message);
		}
		try{
			Thread.sleep(1000);
			while(in.ready() == true){
				outputMessage(in.readLine());
				Thread.sleep(500);
			}
		} catch (IOException e) {
			//asd
		} catch (final InterruptedException e) {
			//asdh
		}
	}
	
     /**
     * Controls the playing logic of the bot
     */
    public void run() throws IOException {
	
	String waiting = "";
	while(!(waiting.equals("STARTTURN"))){
		if(in.ready()){
			waiting = in.readLine();
			outputMessage(waiting);
		}
	}
	botsTurn = 1;
	try {
		while (botsTurn == 1) {
			lookAtMap();
			Thread.sleep(1000);
			if(botsTurn != 1){
				return;
			}
			pickupIfAvailable();
			while (in.ready() == true) {
				outputMessage(in.readLine());
				Thread.sleep(500);
			}
			if(botsTurn != 1){
				return;
			}
			makeRandomMove();
			while (in.ready() == true) {
				outputMessage(in.readLine());
				Thread.sleep(500);
			}
			Thread.sleep(2000);
		} return;
	} catch (InterruptedException e) {
		//asd
	}
    }


    /**
     * Allows the bot to receive and act on messages send from the game. For
     * now, we just handle the LOOKREPLY and WIN.
     * 
     * @param the
     *            message sent by the game
     */
    protected void outputMessage(String message) {
	if (!message.equals("")) {
	    // Print the message for the benefit of a human observer
	    System.out.println(message);

	    final String[] lines = message.split(System.getProperty("line.separator"));
	    final String firstLine = lines[0];

	    if (firstLine.equals("LOOKREPLY")) {
		handleLookReply(lines);

	    } else if (firstLine.equals("WIN")) {
		System.out.println("SHOUT I won the game");
		processCommand("SHOUT I won the game");

		System.exit(0);

	    } else if (firstLine.startsWith("FAIL")) {
	    	System.out.println("Bot enterred invalid command");
	    
	    } else if (firstLine.equals("ENDTURN")) {
	    	botsTurn = -1;
	    } else if (firstLine.equals("DISCONNECTED")) {
	    	System.exit(0);
	    }
	}
    }

    /**
     * Issues a LOOK to update what the bot can see. Returns when it is updated.
     **/
    private void lookAtMap() {
	this.currentLookReply = null;

	// Have a look at the map
	System.out.println("LOOK");
	out.println("LOOK");
	
	String lookReply = "";
	try {
		Thread.sleep(1000);
		while (in.ready() == true) {
			String look = in.readLine();
		//	System.out.println(look);
			lookReply = lookReply + look + "\n";
		}
		//System.out.println(lookReply);
		outputMessage(lookReply);

	} catch (IOException e) {
		System.out.println("I SHOULDNT BE HERE");
	} catch (InterruptedException e) {
		//asd
	}
    }

    /**
     * Handles the LOOKREPLY from the game, updating the bot's array.
     * 
     * @param lines
     *            the lines returned as part of the LOOKREPLY
     */
    private void handleLookReply(String[] lines) {
	// Work out what the bot can sees

	final int lookReplySize = lines[1].length();
	if (lines.length != lookReplySize + 1) {
	    throw new RuntimeException("FAIL: Invalid LOOKREPLY dimensions");
	}

	this.currentLookReply = new char[lookReplySize][lookReplySize];

	for (int row = 0; row < lookReplySize; row++) {
	    for (int col = 0; col < lookReplySize; col++) {
	    	this.currentLookReply[row][col] = lines[row + 1].charAt(col);
	    }
	}
    }

    /**
     * Picks up anything the bot is standing on, if possible
     */
    private void pickupIfAvailable() {
	switch (getCentralSquare()) {
	// We can't pick these up if we already have them, so don't even try
	case 'A':
	    if (!this.hasArmour) {
		System.out.println("PICKUP");
		processCommand("PICKUP");
		// We assume that this will be successful, but we could check
		// the reply from the game.
		this.hasArmour = true;
	    }
	    break;

	case 'L':
	    if (!this.hasLantern) {
		System.out.println("PICKUP");
		processCommand("PICKUP");
		this.hasLantern = true;
	    }
	    break;

	case 'S':
	    if (!this.hasSword) {
		System.out.println("PICKUP");
		processCommand("PICKUP");
		this.hasSword = true;
	    }
	    break;

	case 'G':
	    System.out.println("PICKUP");
	    processCommand("PICKUP");

	    System.out.println("SHOUT I got some gold");
	    processCommand("SHOUT I got some gold");
	    break;

	case 'H':
	    System.out.println("PICKUP");
	    processCommand("PICKUP");
	    processCommand("ENDTURN");
	    break;

	default:
	    break;
	}
    }

    /**
     * Makes a random move, not into a wall
     */
    private void makeRandomMove() {
	try {
	    final char dir = generateRandomMove();
	    final String moveString = "MOVE " + dir;
	    System.out.println(moveString);
	    processCommand(moveString);

	} catch (final IllegalStateException e) {
	    System.err.println(e.getMessage());
	    System.exit(1);
	}
    }

    /**
     * Return a direction to move in. Note that we do checks to see what it in
     * the square before sending the request to move to the game logic.
     * 
     * @return direction in which to move
     */
    private char generateRandomMove() {
	// First, ensure there is a move
	if (!isMovePossible()) {
	    throw new IllegalStateException(
		    "The bot is stuck in a dead end and cannot move anymore!");
	}

	final Random random = new Random();
	while (true) {
	    final int dir = (int) (random.nextFloat() * 4F);

	    switch (dir) {
	    case 0: // N
		if (getSquareWithOffset(0, -1) != '#') {
		    return 'N';
		}
		break;

	    case 1: // E
		if (getSquareWithOffset(1, 0) != '#') {
		    return 'E';
		}
		break;

	    case 2: // S
		if (getSquareWithOffset(0, 1) != '#') {
		    return 'S';
		}
		break;

	    case 3: // W
		if (getSquareWithOffset(-1, 0) != '#') {
		    return 'W';
		}
		break;
	    }
	}
    }

    /**
     * Obtains the square in the centre of the LOOKREPLY, i.e. that over which
     * the bot is standing
     * 
     * @return the square under the bot
     */
    private char getCentralSquare() {
	// Return the square with 0 offset
	return getSquareWithOffset(0, 0);
    }

    /**
     * Obtains a square in of the LOOKREPLY with an offset to the bot
     * 
     * @return the square corresponding to the bot and offset
     */
    private char getSquareWithOffset(int xOffset, int yOffset) {
	final int lookReplySize = this.currentLookReply.length;
	final int lookReplyCentreIndex = lookReplySize / 2; // We rely on
							    // truncation

	return this.currentLookReply[lookReplyCentreIndex + yOffset][lookReplyCentreIndex
		+ xOffset];
    }

    /**
     * Check if the there is a possible move from the centre of the vision field
     * to another tile
     * 
     * @return true if the bot is not encircled with walls, false otherwise
     */
    private boolean isMovePossible() {
	if ((getSquareWithOffset(-1, 0) != '#')
		|| (getSquareWithOffset(0, 1) != '#')
		|| (getSquareWithOffset(1, 0) != '#')
		|| (getSquareWithOffset(0, -1) != '#')) {
	    return true;
	}

	return false;
    }

    /*
     * The main method connects the client to the server, and then calls the run method. Once the thread is interupted
     * by the game ending the client will disconnect and close.
     */
    public static void main(String[] args) throws IOException {
        BotClient bot = new BotClient();
        if (args.length != 2) {
            System.err.println(
                "Usage: java ThinClient <host name> <port number>");
            System.exit(1);
        }
 
        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
 
        try {
            Socket socket = new Socket(hostName, portNumber);
            bot.out = new PrintWriter(socket.getOutputStream(), true);
            bot.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
       
            bot.processCommand("HELLO R2D2");
            while(!Thread.currentThread().isInterrupted()){
            		bot.run();
            }
            System.out.println("You have been disconnected from the server.");
            System.exit(0);
            
            
        } catch (UnknownHostException e) {
            System.err.println("Don't know about host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                hostName);
            System.exit(1);
        }
    }
}
