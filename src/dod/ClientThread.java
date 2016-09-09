package dod;

import java.io.*;
import java.net.*;

import dod.game.GameLogic;

public class ClientThread extends AbstractUser {
	Socket socket;
	BufferedReader in;
	PrintWriter out;
	int playerID;
	
	/*
	 * This method creates the thread to handle the connection between server and client. It sets the
	 * necessary variables and then waits to be started, which the main Server class does.
	 */
	public ClientThread(Socket socket, GameLogic game, int playerID) throws IOException {
		setGame(game);
		game.addPlayer(this, playerID);
		this.playerID = playerID;
		this.socket = socket;
		out = new PrintWriter(socket.getOutputStream(), true);                   
	    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    outputMessage("GOAL: " + game.getGoal());
	}
	
	/*
	 * Once the thread is started, this is the method that it circulates through. It takes the message
	 * that is sent over the network from the client and processes it accordingly. It also prints the
	 * message to the server console to allow the server host to know what is happening.
	 */
	public void run() {
		
		try {
			while(true) {
				String command = in.readLine();
				System.out.println("FROM ID " + playerID + ": " + command);
				processCommand(command, playerID);
			}
		} catch (IOException e) {
			//ERRORSHIT
		} catch (RuntimeException e) {
			//error
		}
	}

	/*
	 * This method is called from processCommand and sends messages back to the client, and to the server
	 * console.
	 */
	protected void outputMessage(String message) {
		out.println(message);
		System.out.println("TO ID " + playerID + ": " + message);
	}
	
}
