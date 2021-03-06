/*
 * Copyright (c) 2015 Tristan Lee - Winchester Studios
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted.
 */

package co.uk.tristanindustries;

import java.util.ArrayList;

import org.lwjgl.LWJGLException;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

import static org.lwjgl.opengl.GL11.*;

public class Game {
	/** Title that appears at the top bar **/
	private final static String WINDOW_TITLE = "The Effects of Cannabis in Space (Win. Productions)";

	/** Window Width **/
	private int width = 800;

	/** Window Height **/
	private int height = 600;

	/** Contains Every Entity within the Game **/
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	/** Contains Entities to be Removed **/
	private ArrayList<Entity> removeList = new ArrayList<Entity>();

	/** Represents the Player's Ship **/
	private ShipEntity ship;

	/** Represents Each of the Player's Shots **/
	private ShotEntity[] shots;

	/** Message Image to be Displayed Currently **/
	private Image message;

	/** Instructs the Player to Press a Key **/
	private Image pressAnyKey;

	/** Informs the Player of his Success **/
	private Image youWin;

	/** Informs the Player of his Failure **/
	private Image gotYou;

	/** Our own Logo **/
	private Image logo;
	
	/** Scale of the Start Game message, meant to expand and contract **/
	private float startPromptScale = 1.0f;

	/** Current Shot **/
	private int shotIndex;

	/** Entity's Movement Speed **/
	private float moveSpeed = 300;

	/** Time of Last Shot Firing **/
	private long lastFire;

	/** Amount of Time which must Elapse to Fire Again **/
	private long firingInterval = 500;

	/** Number of Aliens Remaining **/
	private int alienCount;

	/** Whether the Game is Waiting for Input **/
	private boolean waitingForKeyPress = true;

	/** Whether the Menu is to be Shown **/
	private boolean topMenu = true;

	/** Sets a Logic Required Flag **/
	private boolean logicRequiredThisLoop;

	/** Time of Last Loop **/
	private long lastLoopTime = getTime();

	/** Whether the Fire Button has been Released (Trigger) **/
	private boolean fireHasBeenReleased;

	/** Time of Last FPS Measurement **/
	private long lastFpsTime;

	/** Current game FPS **/
	private int fps;

	/** Resolution of the System Timer **/
	private static long timerTicksPerSecond = Sys.getTimerResolution();

	/** Whether the Game is Currently Running **/
	public static boolean gameRunning = true;

	/** Whether the Window is in fullscreen mode **/
	private boolean fullscreen;

	/** Mouse's current X position **/
	private int mouseX;
	
	/** Whether the mouse is to be grabbed **/
	private boolean mouseGrabbed = false;

	/** Amount of speed that increases as more aliens are shot **/ 
	private float difficultyIncrease = DIFFICULTY_EASY;
	
	/** EASY **/
	private final static float DIFFICULTY_EASY = 1.030f;
	
	/** MEDIUM **/
	private final static float DIFFICULTY_MEDIUM = 1.035f;
	
	/** HARD **/
	private final static float DIFFICULTY_HARD = 1.040f;

	//private SoundManager soundManager;

	//private int SHOT_SOUND;

	//private int HIT_SOUND;

	/**
	 * Construct game, initialise.
	 * 
	 * @throws SlickException
	 */
	public Game() throws SlickException {
		initialise();
	}

	/**
	 * Get the system time
	 * 
	 * @return Current System time in SECONDS
	 */
	public static long getTime() {
		return (Sys.getTime() * 1000) / timerTicksPerSecond;
	}

	/**
	 * Sleep for the specified duration
	 * 
	 * @param duration
	 *            The time to sleep in SECONDS.
	 */
	public static void sleep(long duration) {
		try {
			Thread.sleep((duration * timerTicksPerSecond) / 1000);
		} catch (InterruptedException inte) {
		}
	}

	/**
	 * Initialise the Display, Textures and set up the shots.
	 * 
	 * @throws SlickException
	 */
	public void initialise() throws SlickException {
		// Init window beforehand
		try {
			setDisplayMode();
			setWindowTitle();
			Display.setFullscreen(fullscreen);
			Display.create();

			Mouse.setGrabbed(mouseGrabbed);

			glEnable(GL_TEXTURE_2D);

			glDisable(GL_DEPTH_TEST);

			glMatrixMode(GL_PROJECTION);
			glLoadIdentity();

			glOrtho(0, width, height, 0, -1, 1);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();
			glViewport(0, 0, width, height);

			// Sound initialisation
			//soundManager = new SoundManager();
			//soundManager.initialize(8);

			//SHOT_SOUND = soundManager.addSound("shot.wav");
			//HIT_SOUND = soundManager.addSound("hit.wav");
		} catch (LWJGLException le) {
			System.out.println("Game exiting - Initialisation error.");
			le.printStackTrace();
			Game.gameRunning = false;
			return;
		}

		// Create images, throw exceptions if needed
		try {
			gotYou = new Image("res/gotyou.gif");
			pressAnyKey = new Image("res/pressanykey.gif");
			youWin = new Image("res/youwin.gif");
			logo = new Image("res/title.gif");
			// startButton = new Button(350,350, logo, youWin);

			message = pressAnyKey;

			shots = new ShotEntity[5];
			for (int i = 0; i < shots.length; i++) {
				shots[i] = new ShotEntity(this, new Image("res/other_shot.gif"), 0, 0);
			}
		} catch (SlickException se) {
			se.printStackTrace();
		}

		startGame();	// begin
	}

	/**
	 * Set the display mode for the display.
	 * 
	 * @return True if the Display mode was set correctly. False if not.
	 */
	private boolean setDisplayMode() {
		try {
			DisplayMode[] dm = org.lwjgl.util.Display.getAvailableDisplayModes(
					width, height, -1, -1, -1, -1, 60, 60);

			org.lwjgl.util.Display.setDisplayMode(dm, new String[] {
					"width=" + width,
					"height=" + height,
					"freq=" + 60,
					"bpp="
							+ org.lwjgl.opengl.Display.getDisplayMode()
									.getBitsPerPixel() });
			return true;
		} catch (Exception e) {
			System.out.println("Error!");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * Start a fresh game: clears entities, and initialises a new set.
	 * 
	 * @throws SlickException
	 */
	private void startGame() throws SlickException {
		entities.clear();
		initEntities();
	}

	/**
	 * Initialise the Ship and a set of 5x12 aliens
	 * 
	 * @throws SlickException
	 */
	private void initEntities() throws SlickException {
		ship = new ShipEntity(this, new Image("res/other_ship.gif"), 370, 550);
		entities.add(ship);

		// generate each Alien
		alienCount = 0;
		for (int row = 0; row < 5; row++) {
			for (int x = 0; x < 12; x++) {
				Entity alien;
				alien = new AlienEntity(this, 100 + (x * 50), (50) + row * 30,
						"Alien " + row + "," + x);
				entities.add(alien);
				alienCount++;
			}
		}
	}

	/**
	 * Informs the game that logic is needed, results in a game event of 
	 * some type.
	 */
	public void updateLogic() {
		logicRequiredThisLoop = true;
	}

	/**
	 * Removes an entity from the list of active entities.
	 * 
	 * @param entity
	 * 			The entity to remove from the entity list.
	 */
	public void removeEntity(Entity entity) {
		removeList.add(entity);
	}

	/**
	 * Notify the game if the player has died. Show the gotYou message and wait
	 * for a keypress.
	 */
	public void notifyDeath() {
		message = gotYou;
		waitingForKeyPress = true;
	}

	/**
	 * Notify the game if the player has won! Show win message, wait for
	 * keypress.
	 */
	public void notifyWin() {
		message = youWin;
		waitingForKeyPress = true;
	}
	
	/**
	 * Set the window's title
	 */
	public void setWindowTitle() {
		if(difficultyIncrease == DIFFICULTY_EASY) {
			Display.setTitle(WINDOW_TITLE + " (" + fps + " FPS) (Easy)");
		} else if(difficultyIncrease == DIFFICULTY_MEDIUM) {
			Display.setTitle(WINDOW_TITLE + " (" + fps + " FPS) (Medium)");
		} else if(difficultyIncrease == DIFFICULTY_HARD) {
			Display.setTitle(WINDOW_TITLE + " (" + fps + " FPS) (Hard)");
		} else {
			Display.setTitle(WINDOW_TITLE + " (" + fps + " FPS) (----)");
		}
	}

	/**
	 * Notify the game if an alien was killed.
	 */
	public void notifyAlienKilled() {
		alienCount--;

		if (alienCount == 0) {
			notifyWin();
		}

		for (Entity entity : entities) {
			if (entity instanceof AlienEntity) {
				entity.setHorizontalMovement(entity.getHorizontalMovement() * difficultyIncrease);
			}
		}

		//soundManager.playEffect(HIT_SOUND);
	}

	/**
	 * ATTEMPT to fire a shot. Will only fire if the last shot was 1 second ago.
	 */
	public void tryToFire() {
		if (System.currentTimeMillis() - lastFire < firingInterval) {
			return;
		}

		lastFire = System.currentTimeMillis();
		ShotEntity shot = shots[shotIndex++ % shots.length];
		shot.reinitialise(ship.getX() + 10, ship.getY() - 30);
		entities.add(shot);

		//soundManager.playEffect(SHOT_SOUND);
	}

	/**
	 * The main loop for the game. Clears display and calls the frameRendering
	 * method. Also updates the display.
	 * 
	 * @throws SlickException
	 */
	private void gameLoop() throws SlickException {
		while (Game.gameRunning) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
			glMatrixMode(GL_MODELVIEW);
			glLoadIdentity();

			if (topMenu) {
				menuRendering();
			} else {
				frameRendering();
			}

			Display.update();
		}

		//soundManager.destroy();
		Display.destroy();
	}

	/**
	 * Responsible for logic updates, rendering entities and handling input The
	 * main backbone for the game.
	 * 
	 * @throws SlickException
	 */
	public void frameRendering() throws SlickException {
		Display.sync(60);

		// Quickly render the logo
		// logo.draw(2, -3);

		// Work out the time from last update
		long delta = getTime() - lastLoopTime;
		lastLoopTime = getTime();
		lastFpsTime += delta;
		fps++;

		// If it has been 1 second since last measurement,
		// set the fps counter at the top of the screen.
		// Also, update the score label
		if (lastFpsTime >= 1000) {
			setWindowTitle();
			lastFpsTime = 0;
			fps = 0;
		}

		// Move all entities based upon if we are not waiting
		// for a "press any key!" message.
		if (!waitingForKeyPress) {
			for (Entity entity : entities) {
				entity.move(delta);
			}
		}

		// Draw each entity
		for (Entity entity : entities) {
			entity.draw();
		}

		// Check if any of the entities have collided with each-other.
		// If they have collided, notify each one.
		for (int p = 0; p < entities.size(); p++) {
			for (int s = p + 1; s < entities.size(); s++) {
				Entity me = entities.get(p);
				Entity him = entities.get(s);

				if (me.collidesWith(him)) {
					me.collidedWith(him);
					him.collidedWith(me);
				}
			}
		}

		// Get rid of all entities that have been marked for cleanup.
		entities.removeAll(removeList);
		removeList.clear();

		// If logic is required, usually as a response to a game event,
		// update the logic for each entity.
		if (logicRequiredThisLoop) {
			for (Entity entity : entities) {
				entity.doLogic();
			}

			logicRequiredThisLoop = false;
		}

		// If were waiting for a "press any key" event, then draw the
		// message
		if (waitingForKeyPress) {
			message.draw(325, 250);
		}

		// First assume that the horizontal movement is 0
		ship.setHorizontalMovement(0);

		// Get the x position for the mouse
		mouseX = Mouse.getDX();

		// Get the inputs for each of the keys, set the booleans
		// as the result
		boolean leftPressed = hasInput(Keyboard.KEY_LEFT);
		boolean rightPressed = hasInput(Keyboard.KEY_RIGHT);
		boolean firePressed = hasInput(Keyboard.KEY_SPACE);

		// If we aren't waiting for a keypress (game is running)
		// if one of the two keys is pressed, set the ship's movement
		// based upon which key it is.
		// Otherwise, if the spacebar ("fire button") has been pressed
		// don't do anything, but remember that it has. When it has been
		// released, then start the game.
		if (!waitingForKeyPress) {
			if ((leftPressed) && (!rightPressed)) {
				ship.setHorizontalMovement(-moveSpeed);
			} else if ((rightPressed) && (!leftPressed)) {
				ship.setHorizontalMovement(moveSpeed);
			}

			// if player is pressing the fire button, attempt to fire!
			if (firePressed) {
				tryToFire();
			}
		} else {
			if (!firePressed) {
				fireHasBeenReleased = true;
			}
			if ((firePressed) && (fireHasBeenReleased)) {
				waitingForKeyPress = false;
				fireHasBeenReleased = false;
				startGame();
			}
		}

		if ((Display.isCloseRequested() || Keyboard
				.isKeyDown(Keyboard.KEY_ESCAPE))) {
			Game.gameRunning = false;
		}
	} // frameRendering

	/**
	 * Responsible for Rendering the Menu, and handling options
	 * for the Menu.
	 */
	private void menuRendering() {
		setWindowTitle();
		logo.draw(width/2 - logo.getWidth()/2, height/2 - logo.getHeight()/2, startPromptScale);
		
		if (Keyboard.isKeyDown(Keyboard.KEY_S)) {
			topMenu = false;
			mouseGrabbed = true;
			Mouse.setGrabbed(mouseGrabbed);
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_1)) {
			difficultyIncrease = DIFFICULTY_EASY;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_2)) {
			difficultyIncrease = DIFFICULTY_MEDIUM;
		}
		if(Keyboard.isKeyDown(Keyboard.KEY_3)) {
			difficultyIncrease = DIFFICULTY_HARD;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)
				|| Display.isCloseRequested()) {
			Game.gameRunning = false;
			return;
		}
		
		//startButton.update(Mouse.getX(), Mouse.getY());
		//startButton.draw();
	}	// menuRendering

	/**
	 * Returns any input that has been received by the keyboard or the mouse.
	 * 
	 * @param direction
	 *            The key that one wants to check.
	 * @return Returns if the key is true or false.
	 */
	private boolean hasInput(int direction) {
		switch (direction) {
		case (Keyboard.KEY_LEFT):
			return Keyboard.isKeyDown(Keyboard.KEY_LEFT) || mouseX < 0;
		case Keyboard.KEY_RIGHT:
			return Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || mouseX > 0;
		case Keyboard.KEY_SPACE:
			return Keyboard.isKeyDown(Keyboard.KEY_SPACE)
					|| Mouse.isButtonDown(0);
		}

		return false;
	}

	/**
	 * The main method for the game, executes game.
	 * 
	 * @param argv
	 * @throws SlickException
	 */
	public static void main(String argv[]) throws SlickException {
		new Game().execute();
		System.exit(0);
	}

	/**
	 * Starts the game
	 * 
	 * @throws SlickException
	 */
	public void execute() throws SlickException {
		gameLoop();
	}
}
