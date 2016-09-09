import java.io.*;
import java.net.*;

public class ListenThread implements Runnable {
	Socket socket;
	BufferedReader in;
	PrintWriter out;
	
	public ListenThread(Socket socket, BufferedReader in, PrintWriter out) {
		this.socket = socket;
		this.in = in;
		this.out = out;
	}
	
	/*
	 * This thread constantly listens to the server for the HumanClient, allowing it to receive messages
	 * whenever they are sent and update the GUI accordingly. If the server 'removes' the player from the game
	 * it will detect it here and close the client accordingly.
	 */
	public void run() {
		try {
			while(true) {
				String input = in.readLine();
				if(input.equals("LOSE")){
					HumanClient.loseGame();
				} else if(input.equals("WIN")){
					HumanClient.wonGame();
				} else if (input.equals("DISCONNECTED")) {
					HumanClient.loseGame();
				} else if (input.startsWith("TREASUREMOD")) {
					String[] goldAmount = input.split(" ");
					HumanClient.currentGold = HumanClient.currentGold + Integer.parseInt(goldAmount[1]);
				} else if (input.startsWith("HITMOD")) {
					String[] healthGain = input.split(" ");
					HumanClient.currentHealth = HumanClient.currentHealth + Integer.parseInt(healthGain[1]);
				} else if (input.startsWith("TORCH")) {
					HumanClient.torchPickup = "";
				} else if (input.equals("LOOKREPLY")){
					String lookResponse = "";
					while(in.ready() == true){
						String lookReply = in.readLine();
						if(lookReply.length() == 7){
							lookResponse = lookResponse + lookReply;
						} else {
							lookResponse = lookResponse + "X" + lookReply + "X";
						}
					}
					if(lookResponse.length() == 37){
						lookResponse = "XXXXXXX" + lookResponse + "XXXXXXX";
					}
					HumanClient.handleLookReply(lookResponse);
				} else if(input.equals("CHANGE")){
					out.println("LOOK");
				} else if(input.equals("MESSAGE SERVER GAME")){
					out.println("LOOK");
				} else if(input.equals("STARTTURN")){
					HumanClient.startTurn();
				} else if(input.equals("ENDTURN")){
					HumanClient.endTurn();
				} else if(input.startsWith("GOAL")){
					String[] goal = input.split(" ");
					HumanClient.drawGoal(goal[1]);
				} else if(input.startsWith("WHISPER")){
					String[] whisperSplit = input.split(" ", 2);
					HumanClient.addWhisper(whisperSplit[1]);
				} else if(input.startsWith("MESSAGE")){
					String[] shoutSplit = input.split(" ", 2);
					HumanClient.addShout(shoutSplit[1]);
				}
			}
		} catch (IOException e) {
			//do nothing
		}
		
	}
	
}
