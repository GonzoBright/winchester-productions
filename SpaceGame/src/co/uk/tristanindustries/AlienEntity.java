/*
 * Copyright (c) 2015 Tristan Lee - Winchester Studios
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted.
 */

package co.uk.tristanindustries;

import org.newdawn.slick.Image;
import org.newdawn.slick.SlickException;

public class AlienEntity extends Entity {
	private static final int DOWNWARD_MOVEMENT = 10;

	private static final int BOTTOM_BORDER = 570;

	private static final int RIGHT_BORDER = 750;

	private static final int LEFT_BORDER = 10;

	private float moveSpeed = 82;

	private Game game;

	private Image[] frames = new Image[4];

	private long lastFrameChange;

	private long frameDuration = 250;

	private int frameNumber;
	
	private String reference;

	public AlienEntity(Game game, int x, int y, String reference) throws SlickException {
		super(new Image("res/alien.gif"), x, y);

		frames[0] = sprite;
		frames[1] = new Image("res/alien2.gif");
		frames[2] = sprite;
		frames[3] = new Image("res/alien3.gif");

		this.game = game;
		dx = -moveSpeed;
		this.reference = reference;
	}

	public void move(long delta) {
		lastFrameChange += delta;

		if (lastFrameChange > frameDuration) {
			lastFrameChange = 0;

			frameNumber++;
			if (frameNumber >= frames.length) {
				frameNumber = 0;
			}

			sprite = frames[frameNumber];
		}

		if ((dx < 0) && (x < LEFT_BORDER)) {
			game.updateLogic();
		}
		if ((dx > 0) && (x > RIGHT_BORDER)) {
			game.updateLogic();
		}

		super.move(delta);
	}

	public void doLogic() {
		dx = -dx;
		y += DOWNWARD_MOVEMENT;

		if (y > BOTTOM_BORDER) {
			game.notifyDeath();
		}
	}

	public void collidedWith(Entity other) {
		// handled elsewhere!
	}
	
	public String toString() {
		return reference;
	}
}
