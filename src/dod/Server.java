package dod;

import java.io.*;
import java.net.*;
import java.text.ParseException;
import java.util.Scanner;

import dod.game.CommandException;
import dod.game.GameLogic;

public class Server {
	private ServerSocket server;
	
	/*
	 * Firstly the main method checks to see if the correct arguments have been submitted. It will then
	 * initialise a new Server object which will hold the server. It opens the server creates a new instance
	 * of the game with the map argument. It then begins to accept clients joining the server, asking the
	 * server host after each client has joined if the game should begin yet. Each client is assigned a thread
	 * which controls connections between the server and client. Once the host deems the game ready to begin,
	 * the game starts and the first turn is taken. If the host inputs 'end' at any time after the game has begun,
	 * the server will close and the program will exit. This is designed to be used once all players have
	 * disconnected at the end of the game to make sure that the server doesn't stay open.
	 */
	public static void main(String[] args) throws ParseException, IOException {
		if (args.length != 2) {
			System.err.println("Usage: java Server <port number> <map name>");
			System.exit(1);
		}
		Server gameServer = new Server();
		
		try {
			gameServer.server = new ServerSocket(Integer.parseInt(args[0]));
			System.out.println("Server initialised.");
			GameLogic game = new GameLogic(args[1]);
			int playerID = 0;
			while(true){
				Scanner gameBegin = new Scanner(System.in);
				Socket client = gameServer.server.accept();
				System.out.println("New player connected.");
				(new Thread(new ClientThread(client, game, playerID))).start();
				System.out.println("Would you like to start the game? (y/n)");
				if(gameBegin.nextLine().equals("y")){
					break;
				}
				playerID++;
			}
			try {
				game.startNewGame();
			} catch (CommandException e) {
				//asdf
			}
			System.out.println("The game has begun.");
			Scanner gameEnd = new Scanner(System.in);
			while(true){	
				if(gameEnd.nextLine().equals("end")){
					gameServer.server.close();
					System.exit(0);
					
				}
			}
		} catch (IOException e) {
			System.out.println("ERROR");
			System.out.println(e.getMessage());
		} catch (IllegalStateException e) {
			System.out.println("Someone did something wrong!");
		}
	}
}
