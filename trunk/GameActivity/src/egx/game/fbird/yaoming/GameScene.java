package egx.game.fbird.yaoming;
/**
 * Copyright (C) 2013 Martin Varga <android@kul.is>
 */
import egx.game.fbird.yaoming.R;

import java.util.Iterator;
import java.util.LinkedList;

import org.andengine.engine.camera.hud.HUD;
import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.Entity;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.ParallaxBackground;
import org.andengine.entity.scene.background.ParallaxBackground.ParallaxEntity;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.sprite.TiledSprite;
import org.andengine.entity.text.Text;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.constants.PhysicsConstants;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.debug.Debug;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Manifold;

public class GameScene extends Scene implements IOnSceneTouchListener,
		ContactListener, ITouchArea {

	private static final long TIME_TO_RESSURECTION = 200;
	PhysicsWorld physics;

	enum State {
		NEW, PAUSED, PLAY, DEAD, AFTERLIFE;
	}

	Text infoText;
	Text scoreText;
	Text finishScore;
	Text finishBest;

	Sprite tapBackGround;

	Sprite dandelion;
	Body dandelionBody;
	ParallaxBackground pb;
	TiledSprite blood;
	State state = State.NEW;
	State lastState = state;
	long timestamp = 0;

	private int score = 0;
	private boolean scored;

	LinkedList<Pillar> pillars = new LinkedList<Pillar>();

	protected ResourceManager res = ResourceManager.getInstance();
	protected VertexBufferObjectManager vbom = ResourceManager.getInstance().vbom;

	/**
	 * variable for score
	 */
	private Sprite box;
	private Sprite play;
	private Sprite btnScore;
	private Sprite faceShare;
	private Text gameOver;

	private boolean reset = false;

	private boolean isBlood = false;
	private int countBlood = 0;

	private boolean isTap = false;
	private int countTimeTap = 0;

	/**
	 * ham khoi tao game
	 */
	public GameScene() {
		physics = new PhysicsWorld(new Vector2(0, 0), true);
		physics.setContactListener(this);
		PillarFactory.getInstance().create(physics);

		createBackground();
		createActor();
		createBounds();

		createText();
		res.camera.setChaseEntity(dandelion);
		showOrHideScore(false);
		sortChildren();
		setOnSceneTouchListener(this);
		blood.setVisible(false);
		registerUpdateHandler(physics);
	}

	/**
	 * create text score and tap
	 */
	private void createText() {
		HUD hud = new HUD();
		res.camera.setHUD(hud);
		infoText = new Text(Constants.CW / 2, Constants.CH / 2 - 200, res.font,
				"12345678901234567890", vbom);
		hud.attachChild(infoText);

		scoreText = new Text(Constants.CW / 2, Constants.CH / 2 + 200,
				res.font, "12345678901234567890", vbom);
		hud.attachChild(scoreText);

		tapBackGround = new Sprite(Constants.CW / 2
				- res.tapBackGround.getWidth() / 2, Constants.CH / 2
				+ res.tapBackGround.getHeight() / 2, res.tapBackGround, vbom);
		tapBackGround.setAnchorCenter(0, 1);
		hud.attachChild(tapBackGround);
		createScore(hud);
	}

	// show score when finish game
	private void createScore(HUD hud) {
		gameOver = new Text(Constants.CW / 2, Constants.CH / 2 + 50
				+ res.box.getHeight() / 2 + res.btnFbShare.getHeight(),
				res.fontGameOver, "Game Over", vbom);
		hud.attachChild(gameOver);

		box = new Sprite(Constants.CW / 2, Constants.CH / 2
				+ res.btnFbShare.getHeight(), res.box, vbom);
		hud.attachChild(box);

		play = new Sprite(Constants.CW / 2 - res.btnPlay.getWidth() / 2 - 30,
				Constants.CH / 2 - res.box.getHeight() / 2, res.btnPlay, vbom) {
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				if (pSceneTouchEvent.isActionUp() && play.isVisible()) {
					play.setAlpha(1f);
				} else if (pSceneTouchEvent.isActionDown() && play.isVisible()) {
					play.setAlpha(0.8f);
					if (state == State.AFTERLIFE) {
						reset = true;
					}
				}
				return false;
			}
		};
		hud.attachChild(play);
		hud.registerTouchArea(play);

		btnScore = new Sprite(Constants.CW / 2 + res.btnPlay.getWidth() / 2
				+ 30, Constants.CH / 2 - res.box.getHeight() / 2, res.btnScore,
				vbom) {
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				// TODO Auto-generated method stub
				if (pSceneTouchEvent.isActionUp() && btnScore.isVisible()) {
					btnScore.setAlpha(1f);
				} else if (pSceneTouchEvent.isActionDown()
						&& btnScore.isVisible()) {
					btnScore.setAlpha(0.8f);
					res.activity.gotoPlayStore();
				}
				return false;
			}
		};
		hud.registerTouchArea(btnScore);
		hud.attachChild(btnScore);

		faceShare = new Sprite(Constants.CW / 2, Constants.CH / 2
				- res.box.getHeight() / 2 - res.btnScore.getHeight(),
				res.btnFbShare, vbom) {
			@Override
			public boolean onAreaTouched(TouchEvent pSceneTouchEvent,
					float pTouchAreaLocalX, float pTouchAreaLocalY) {
				// TODO Auto-generated method stub
				if (pSceneTouchEvent.isActionUp() && faceShare.isVisible()) {
					faceShare.setAlpha(1f);
				} else if (pSceneTouchEvent.isActionDown()
						&& faceShare.isVisible()) {
					faceShare.setAlpha(0.9f);
					boolean isFace = res.activity.isVisibleFacebook();
					if (isFace)
						res.activity.faceShare();

				}
				return false;
			}
		};
		hud.registerTouchArea(faceShare);
		hud.attachChild(faceShare);

		float positionX = box.getX() + box.getWidth() / 2;
		float positionY = box.getY() + box.getHeight() / 2;
		Debug.e(positionX + ":" + positionY);
		finishBest = new Text(positionX - 40, positionY - 80, res.fontScore,
				"12345678901234567890", vbom);
		finishBest.setText(String.valueOf(score));
		finishBest.setPosition(finishBest.getX() - finishBest.getWidth(),
				finishBest.getY());
		hud.attachChild(finishBest);

		finishScore = new Text(positionX - 40, positionY - 160, res.fontScore,
				"12345678901234567890", vbom);
		finishScore.setText(String.valueOf(res.activity.getHighScore()));
		finishScore.setPosition(finishScore.getX() - finishScore.getWidth(),
				finishScore.getY());
		hud.attachChild(finishScore);
	}

	public void showOrHideScore(boolean show) {

		gameOver.setVisible(show);
		box.setVisible(show);
		play.setVisible(show);
		btnScore.setVisible(show);
		faceShare.setVisible(show);

		if (show) {
			play.setAlpha(1f);
			btnScore.setAlpha(1f);
			faceShare.setAlpha(1f);
			scoreText.setVisible(!show);
			finishBest.setText(String.valueOf(score));
			finishBest.setPosition(box.getX() + box.getWidth() / 2 - 40
					- finishBest.getWidth(), finishBest.getY());
			finishScore.setText(String.valueOf(res.activity.getHighScore()));
			finishScore.setPosition(box.getX() + box.getWidth() / 2 - 40
					- finishScore.getWidth(), finishScore.getY());
			finishScore.setVisible(true);
			finishBest.setVisible(true);
		} else {
			scoreText.setVisible(true);
			finishBest.setVisible(false);
			finishScore.setVisible(false);
		}
	}

	/**
	 * creat bound
	 */
	private void createBounds() {
		float bigNumber = 999999; // i dunno, just a big number
		res.repeatingGroundRegion.setTextureWidth(bigNumber);
		Sprite ground = new Sprite(0, -100, res.repeatingGroundRegion, vbom);
		ground.setAnchorCenter(0, 0);
		ground.setZIndex(10);
		attachChild(ground);

		Body groundBody = PhysicsFactory.createBoxBody(physics, ground,
				BodyType.StaticBody, Constants.WALL_FIXTURE);
		groundBody.setUserData(Constants.BODY_WALL);

		// just to limit the movement at the top
		@SuppressWarnings("unused")
		Body ceillingBody = PhysicsFactory.createBoxBody(physics,
				bigNumber / 2, 820, bigNumber, 20, BodyType.StaticBody,
				Constants.CEILLING_FIXTURE);
	}

	/**
	 * create yaoming
	 */
	private void createActor() {
		dandelion = new Sprite(200, 400, res.dandelionRegion, vbom);
		dandelion.setZIndex(999);
		dandelion.registerUpdateHandler(new IUpdateHandler() {

			@Override
			public void onUpdate(float pSecondsElapsed) {
				if (dandelionBody.getLinearVelocity().y > -0.01) {
					// dandelion.setRotation(-45);
				} else {

				}
			}

			@Override
			public void reset() {
			}
		});
		dandelionBody = PhysicsFactory.createCircleBody(physics, dandelion,
				BodyType.DynamicBody, Constants.DANDELION_FIXTURE);
		dandelionBody.setFixedRotation(true);
		dandelionBody.setUserData(Constants.BODY_ACTOR);
		physics.registerPhysicsConnector(new PhysicsConnector(dandelion,
				dandelionBody));
		attachChild(dandelion);
		blood = new TiledSprite(200, 400, res.blood_sprite, vbom);
		blood.setZIndex(999);
		blood.setCurrentTileIndex(0);
		attachChild(blood);
		blood.setVisible(false);
	}

	/**
	 * create background
	 */
	private void createBackground() {
		pb = new ParallaxBackground(0.75f, 0.83f, 0.95f);
		Entity clouds = new Rectangle(0, 0, 480, 800, vbom);
		clouds.setAnchorCenter(0, 0);
		clouds.setAlpha(0f);
		clouds.attachChild(new Sprite(240, 400, res.backGround, vbom));
		ParallaxEntity pe = new ParallaxEntity(1f, clouds);
		pb.attachParallaxEntity(pe);
		setBackground(pb);
	}

	@Override
	public void reset() {
		super.reset();
		physics.setGravity(new Vector2(0, 0));

		Iterator<Pillar> pi = pillars.iterator();
		while (pi.hasNext()) {
			Pillar p = pi.next();
			PillarFactory.getInstance().recycle(p);
			pi.remove();
		}

		PillarFactory.getInstance().reset();

		dandelionBody.setTransform(
				200 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT,
				400 / PhysicsConstants.PIXEL_TO_METER_RATIO_DEFAULT, 0);

		addPillar();
		addPillar();
		addPillar();

		score = 0;

		infoText.setText(res.activity.getString(R.string.tap_to_play));
		infoText.setVisible(true);

		scoreText.setText(res.activity.getString(R.string.hiscore)
				+ res.activity.getHighScore());
		infoText.setVisible(true);

		tapBackGround.setVisible(true);
		sortChildren();

		unregisterUpdateHandler(physics);
		physics.onUpdate(0);
		state = State.NEW;
	}

	private void addPillar() {
		Pillar p = PillarFactory.getInstance().next();
		pillars.add(p);
		attachIfNotAttached(p);
	}

	@Override
	public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
		if (pSceneTouchEvent.isActionDown()) {
			if (state == State.PAUSED) {
				if (lastState != State.NEW) {
					registerUpdateHandler(physics);
				}
				state = lastState;
			} else if (state == State.NEW) {
				registerUpdateHandler(physics);
				state = State.PLAY;
				physics.setGravity(new Vector2(0, Constants.GRAVITY));
				dandelionBody.setLinearVelocity(new Vector2(Constants.SPEED_X,
						0));
				scoreText.setText("0");
				infoText.setVisible(false);
				tapBackGround.setVisible(false);
			} else if (state == State.DEAD) {
				// don't touch the dead!
			} else if (state == State.AFTERLIFE) {

			} else {
				Vector2 v = dandelionBody.getLinearVelocity();
				v.x = Constants.SPEED_X;
				v.y = Constants.SPEED_Y;
				dandelionBody.setLinearVelocity(v);
				res.sndFly.play();
				isTap = true;
			}
		}
		return false;
	}

	public void resume() {
	}

	public void pause() {
		unregisterUpdateHandler(physics);
		lastState = state;
		state = State.PAUSED;
	}

	int temp = 0;

	private boolean isTapDown = false;
	private int countTapDown = 30;

	@Override
	protected void onManagedUpdate(float pSecondsElapsed) {
		super.onManagedUpdate(pSecondsElapsed);
		if (isTap) {
			countTimeTap++;
			dandelion.setRotation(countTimeTap / 2 * -3f);
			isTapDown = false;
			countTapDown = 30;
			if (countTimeTap / 2 == 15) {
				isTap = false;
				countTimeTap = 0;
				dandelion.setRotation(-45f);
				isTapDown = true;
			}
		}

		if (isTapDown) {
			countTapDown--;
			dandelion.setRotation(countTapDown / 2 * -3f);
			if (countTapDown < 0) {
				countTapDown = 30;
				isTapDown = false;
				dandelion.setRotation(0f);
			}
		}

		if (reset) {
			temp++;
			if (temp == 20) {
				temp = 0;
				reset = false;
				blood.setVisible(false);
				blood.setCurrentTileIndex(0);
				reset();
				state = State.NEW;
				showOrHideScore(false);
			}
			return;
		}

		if (isBlood && countBlood / 2 < 7) {
			countBlood++;
			blood.setCurrentTileIndex(countBlood / 2);
			if (countBlood / 2 == 7) {
				countBlood = 0;
				isBlood = false;
			}
		}

		if (scored) {
			addPillar();
			sortChildren();
			scored = false;
			score++;
			scoreText.setText(String.valueOf(score));
		}

		// if first pillar is out of the screen, delete it
		if (!pillars.isEmpty()) {
			Pillar fp = pillars.getFirst();
			if (fp.getX() + fp.getWidth() < res.camera.getXMin()) {
				PillarFactory.getInstance().recycle(fp);
				pillars.remove();
			}
		}

		if (state == State.DEAD
				&& timestamp + TIME_TO_RESSURECTION < System
						.currentTimeMillis()) {
			state = State.AFTERLIFE;
			showOrHideScore(true);
		}
	}

	private void attachIfNotAttached(Pillar p) {
		if (!p.hasParent()) {
			attachChild(p);
		}
	}

	@Override
	public void beginContact(Contact contact) {
		if (Constants.BODY_WALL.equals(contact.getFixtureA().getBody()
				.getUserData())
				|| Constants.BODY_WALL.equals(contact.getFixtureB().getBody()
						.getUserData())) {
			state = State.DEAD;
			if (this.dandelion.getY() < 200) {
				// this.dandelion.setVisible(false);
				isBlood = true;
				blood.setCurrentTileIndex(0);
				blood.setPosition(this.dandelion.getX(),
						this.dandelion.getY() - 32);
			}
			res.sndFail.play();
			if (score > res.activity.getHighScore()) {
				res.activity.setHighScore(score);
			}
			timestamp = System.currentTimeMillis();
			dandelionBody.setLinearVelocity(0, 0);
			for (Pillar p : pillars) {
				p.getPillarUpBody().setActive(false);
				p.getPillarDownBody().setActive(false);
			}
		}

	}

	@Override
	public void endContact(Contact contact) {
		if (Constants.BODY_SENSOR.equals(contact.getFixtureA().getBody()
				.getUserData())
				|| Constants.BODY_SENSOR.equals(contact.getFixtureB().getBody()
						.getUserData())) {
			scored = true;
		}

	}

	@Override
	public void preSolve(Contact contact, Manifold oldManifold) {
	}

	@Override
	public void postSolve(Contact contact, ContactImpulse impulse) {
	}

	public void captureScrenAndShareFace() {

	}
}
