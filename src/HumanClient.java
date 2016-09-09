import java.io.*;
import java.net.*;

import javax.swing.*;

import java.awt.event.*;
import java.awt.*;
 
public class HumanClient {
	
	private boolean alreadyConnected = false;

	public static int currentHealth = 3;
	public static int currentGold = 0;
	public static String torchPickup = "No";
	
	private Socket socket;
	private PrintWriter out;
	private BufferedReader in;
	private Thread listener;
	JLabel gold;
	JLabel hearts;
	JLabel torchAcquired;
	
	static ImageIcon floorTile;
	static ImageIcon goldTile;
	static ImageIcon healthTile;
	static ImageIcon torchTile;
	static ImageIcon wallTile;
	static ImageIcon fogTile;
	static ImageIcon exitTile;
	static ImageIcon playerTile;
	static ImageIcon userTile;
	static JPanel lookReply;
	
	private static JLabel goal;
	private static JLabel userPosition;
	
	private JButton pickupButton;
	
	private static JTextArea chatBoxDisplay;
	
	public HumanClient(){
		loadGUI();
	}
	
	/*
	 * This method constructs the GUI, adding all the swing components needed
	 * and adding the actions the buttons perform. It populates the look view
	 * with a 7x7 of fog, and then will auto update upon server messages.
	 */
	private void loadGUI(){
		JFrame mainFrame = new JFrame("Dungeon of Doom");
		
		JPanel mainPanel = new JPanel();
		mainFrame.getContentPane().add(mainPanel);
		mainPanel.setLayout(null);
		
		//Images to be used in the GUI
		floorTile = new ImageIcon(getClass().getResource("floorTile.png"));		
		goldTile = new ImageIcon(getClass().getResource("goldTile.png"));
		healthTile = new ImageIcon(getClass().getResource("healthTile.png"));
		torchTile = new ImageIcon(getClass().getResource("torchTile.png"));
		wallTile = new ImageIcon(getClass().getResource("wallTile.png"));
		fogTile = new ImageIcon(getClass().getResource("fog.png"));
		exitTile = new ImageIcon(getClass().getResource("exitTile.png"));
		playerTile = new ImageIcon(getClass().getResource("playerTile.png"));
		userTile = new ImageIcon(getClass().getResource("userTile.png"));
		
		//Graphical display of the player view
		lookReply = new JPanel(new GridLayout(7,7,0,0));
		lookReply.setBounds(5, 5, 350, 350);
		mainPanel.add(lookReply);
		
		//Populating the display with fog
		JLabel tiles[] = new JLabel[49];
		for(int i=0; i<49; i++){
			tiles[i] = new JLabel(fogTile);
			lookReply.add(tiles[i]);
		}
		
		//Gold Counter Label
		ImageIcon goldBag = new ImageIcon(getClass().getResource("gold"+String.valueOf(currentGold)+".png"));
		final JLabel gold = new JLabel(goldBag);
		gold.setBounds(360, 5, 75, 75);
		mainPanel.add(gold);
		
		//Torch Possession Label
		ImageIcon torch = new ImageIcon(getClass().getResource("torchNo.png"));
		final JLabel torchAcquired = new JLabel(torch);
		torchAcquired.setBounds(360, 75, 75, 75);
		mainPanel.add(torchAcquired);
		
		//Health Counter Label
		ImageIcon health = new ImageIcon(getClass().getResource("health"+String.valueOf(currentHealth)+".png"));
		final JLabel hearts = new JLabel(health);
		hearts.setBounds(430, 5, 75, 75);
		mainPanel.add(hearts);
		
		//Gold Goal Label
		goal = new JLabel("  GOAL: 0");
		goal.setBounds(430, 75, 90, 75);
		goal.setFont(new Font("Tahoma", Font.BOLD, 15));
		goal.setForeground(new Color(50, 50, 25));
		mainPanel.add(goal);
		
		//Move North Icon
		ImageIcon moveNIcon = new ImageIcon(getClass().getResource("moveN.png"));
		JButton moveNorthButton = new JButton(moveNIcon);
		moveNorthButton.setBounds(415, 150, 50, 50);
		mainPanel.add(moveNorthButton);
		moveNorthButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("MOVE N");
					out.println("LOOK");
				}
			}
		});
		
		//Move South Icon
		ImageIcon moveSIcon = new ImageIcon(getClass().getResource("moveS.png"));
		JButton moveSouthButton = new JButton(moveSIcon);
		moveSouthButton.setBounds(415, 205, 50, 50);
		mainPanel.add(moveSouthButton);
		moveSouthButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("MOVE S");
					out.println("LOOK");
				}
			}
		});
		
		//Move West Button
		ImageIcon moveWIcon = new ImageIcon(getClass().getResource("moveW.png"));
		JButton moveWestButton = new JButton(moveWIcon);
		moveWestButton.setBounds(360, 205, 50, 50);
		mainPanel.add(moveWestButton);
		moveWestButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("MOVE W");
					out.println("LOOK");
				}
			}
		});
		
		//Move East Button
		ImageIcon moveEIcon = new ImageIcon(getClass().getResource("moveE.png"));
		JButton moveEastButton = new JButton(moveEIcon);
		moveEastButton.setBounds(470, 205, 50, 50);
		mainPanel.add(moveEastButton);
		moveEastButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("MOVE E");
					out.println("LOOK");
				}
			}
		});
		
		//End Turn Button
		ImageIcon endTurnIcon = new ImageIcon(getClass().getResource("endTurn.png"));
		JButton endTurnButton = new JButton(endTurnIcon);
		endTurnButton.setBounds(470, 150, 50, 50);
		mainPanel.add(endTurnButton);
		endTurnButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("ENDTURN");
					out.println("LOOK");
				}
			}
		});
		
		//Pick Up Button
		ImageIcon pickupIcon = new ImageIcon(getClass().getResource("pickup.png"));
		pickupButton = new JButton(pickupIcon);
		pickupButton.setBounds(360, 150, 50, 50);
		mainPanel.add(pickupButton);
		pickupButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				if(alreadyConnected){
					out.println("PICKUP");
					out.println("LOOK");
				}
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				ImageIcon goldBag = new ImageIcon(getClass().getResource("gold"+String.valueOf(currentGold)+".png"));
				gold.setIcon(goldBag);
				ImageIcon torch = new ImageIcon(getClass().getResource("torch"+torchPickup+".png"));
				torchAcquired.setIcon(torch);
				ImageIcon health = new ImageIcon(getClass().getResource("health"+String.valueOf(currentHealth)+".png"));
				hearts.setIcon(health);

			}
		});
		
		//Address Label
		JLabel ipAddress = new JLabel("IP Address");
		ipAddress.setFont(new Font("Tahoma", Font.PLAIN, 15));
		ipAddress.setForeground(new Color(50, 50, 25));
		ipAddress.setBounds(360, 250, 150, 50);
		mainPanel.add(ipAddress);
		
		//Address Input
		final JTextField ipInput = new JTextField();
		ipInput.setBounds(435, 267, 90, 20);
		mainPanel.add(ipInput);
		
		//Port Label
		JLabel portNumber = new JLabel("Port");
		portNumber.setFont(new Font("Tahoma", Font.PLAIN, 15));
		portNumber.setForeground(new Color(50, 50, 25));
		portNumber.setBounds(360, 273, 100, 50);
		mainPanel.add(portNumber);
		
		//Port Input
		final JTextField portInput = new JTextField();
		portInput.setBounds(435, 290, 90, 20);
		mainPanel.add(portInput);
		
		//Connect Button
		ImageIcon connectIcon = new ImageIcon(getClass().getResource("connect.png"));
		JButton connect = new JButton(connectIcon);
		connect.setBounds(360, 315, 78, 30);
		mainPanel.add(connect);
		
		connect.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				connectToServer(ipInput.getText(), portInput.getText());
				String name = (String)JOptionPane.showInputDialog(null, "You have connected to the server.\n" +"What is your name?", "Set name", JOptionPane.PLAIN_MESSAGE);
				name = name.replaceAll(" ", "");
				out.println("HELLO "+name);
			}
		});
		
		//Disconnect Button
		ImageIcon disconnectIcon = new ImageIcon(getClass().getResource("disconnect.png"));
		JButton disconnect = new JButton(disconnectIcon);
		disconnect.setBounds(442, 315, 80, 30);
		mainPanel.add(disconnect);
		
		disconnect.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				out.println("DISCONNECT");
				alreadyConnected = false;
				try{
					reset();
				} catch (IOException e){
					//
				}
				JOptionPane.showMessageDialog(null, "You have disconnected from the server.");
			}
		});
		
		//ChatBox Display
		chatBoxDisplay = new JTextArea();
		JScrollPane scrollChat = new JScrollPane(chatBoxDisplay);
		chatBoxDisplay.setEditable(false);
		chatBoxDisplay.setLineWrap(true);
		chatBoxDisplay.setWrapStyleWord(true);
		scrollChat.setBounds(540, 5, 235, 305);
		scrollChat.setBorder(BorderFactory.createLineBorder(Color.black));
		mainPanel.add(scrollChat);
		
		//ChatBox Input
		final JTextField chatBoxInput = new JTextField();
		chatBoxInput.setBounds(540, 315, 170, 30);
		mainPanel.add(chatBoxInput);
		
		//ChatBox SendButton
		ImageIcon sendButton = new ImageIcon(getClass().getResource("send.png"));
		JButton sendMessage = new JButton(sendButton);
		sendMessage.setBounds(715, 315, 60, 30);
		mainPanel.add(sendMessage);
		
		sendMessage.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent event){
				sendChatMessage(chatBoxInput.getText());
				chatBoxInput.setText("");
			}
		});
		
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		mainFrame.setSize(780, 360);
		mainFrame.setVisible(true);
	}
	
	/*
	 * This method first checks to see if the client is already connected
	 * to a server. If it is it will not do anything. If it isn't, it will
	 * connect to the server and start a new listener thread.
	 */
	private void connectToServer(String ip, String port){
		if(alreadyConnected){
			JOptionPane.showMessageDialog(null, "You are already connected to a server.", "Server error.", JOptionPane.ERROR_MESSAGE);
			return;
		}
		try{
			socket = new Socket(ip, Integer.parseInt(port));
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			listener = new Thread(new ListenThread(socket, in, out));
			listener.start();
			alreadyConnected = true;
		} catch (UnknownHostException e) {
            System.err.println("Don't know about host " + ip);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                ip);
            System.exit(1);
        }
	}
	
	/*
	 * Displays a pop up window declaring that it is now the players
	 * turn.
	 */
	public static void startTurn() {
		JOptionPane.showMessageDialog(null, "It is now your turn.");
	}
	
	/*
	 * Displays a pop up window declaring that the players turn is
	 * now over.
	 */
	public static void endTurn() {
		JOptionPane.showMessageDialog(null, "Your turn is over.");
	}

	/*
	 * Displays a pop up window declaring that the player has won
	 * the game.
	 */
	public static void wonGame() {
		JOptionPane.showMessageDialog(null, "You have won the game.");
	}
	
	/*
	 * Displays a pop up window declaring that the player has lost
	 * the game.
	 */
	public static void loseGame() {
		JOptionPane.showMessageDialog(null, "You have lost the game.");
	}
	
	/*
	 * When the disconnect button is pressed this method is called.
	 * It closes the connection to the server and then resets all
	 * the variables that are used by the GUI. It simulates a pick
	 * up button press to reset the images of the inventory, and it
	 * then redraws the look view with a 7x7 of fog.
	 */
	private void reset() throws IOException{
		socket.close();
		currentHealth = 3;
		currentGold = 0;
		torchPickup = "No";
		drawGoal("0");
		pickupButton.doClick();
		lookReply.removeAll();
		chatBoxDisplay.setText("");
		JLabel tiles[] = new JLabel[49];
		for(int i=0; i<49; i++){
			tiles[i] = new JLabel(fogTile);
			lookReply.add(tiles[i]);
		}
		lookReply.revalidate();		
	}
	
	/*
	 * This method removes the current 7x7 from the look reply, and
	 * then looks at each character in the string it has been sent.
	 * It then adds the relevant tile to the look reply and once it
	 * has been fully populated, it redraws the look reply, showing
	 * the new view.
	 */
	 public static void handleLookReply(String look) {
		lookReply.removeAll();
		JLabel tiles[] = new JLabel[49];
		for(int i=0; i<49; i++){
			if(String.valueOf(look.charAt(i)).equals(".")){
				tiles[i] = new JLabel(floorTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("X")){
				tiles[i] = new JLabel(fogTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("#")){
				tiles[i] = new JLabel(wallTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("L")){
				tiles[i] = new JLabel(torchTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("G")){
				tiles[i] = new JLabel(goldTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("H")){
				tiles[i] = new JLabel(healthTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("E")){
				tiles[i] = new JLabel(exitTile);
				lookReply.add(tiles[i]);
			} else if(String.valueOf(look.charAt(i)).equals("P")){
				tiles[i] = new JLabel(playerTile);
				lookReply.add(tiles[i]);
			}
		}
		lookReply.revalidate();
	 }
	 
	 /*
	  * This method sets the goal for the current map.
	  */
	 public static void drawGoal(String goalAmount){
		 goal.setText("  GOAL: "+goalAmount);
		 goal.revalidate();
	 }
	 
	 /*
	  * This method sends the message from the chat box input to
	  * the server, sending either Whisper or Shout depending on
	  * the type of message.
	  */
	 public void sendChatMessage(String messageFromClient){
		 //Message will not send if not connected.
		 if(!alreadyConnected){
			 return;
		 }
		 if(messageFromClient.startsWith("/w")){
			 out.println("WHISPER " + messageFromClient.substring(3));
		 } else {
			 out.println("SHOUT " + messageFromClient);
		 }
	 }
	 
	 /*
	  * This method adds messages sent to all players into the
	  * chat box.
	  */
	 public static void addShout(String message){
		 chatBoxDisplay.append(message + "\n");
		 chatBoxDisplay.setCaretPosition(chatBoxDisplay.getDocument().getLength());

	 }
	 
	 /*
	  * This method adds messages sent to one player into the
	  * chat box.
	  */
	 public static void addWhisper(String message){
		 chatBoxDisplay.append(message + "\n");
		 chatBoxDisplay.setCaretPosition(chatBoxDisplay.getDocument().getLength());
	 }
	 
	/*
	 * This method is the human client for the game. IT will connect to the server and the launch a new thread,
	 * ListenThread, that will continuously take messages the server is sending to the client. It then loops as
	 * long as this listenerThread is running taking input from the command line and sending it to the server to
	 * update the game. Once the listenerThread is interrupted the client will disconnect and close.
	 */
    public static void main(String[] args) throws IOException {
         @SuppressWarnings("unused")
		HumanClient client = new HumanClient();
    }
}
