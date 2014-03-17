/**
 * Copyright (C) 2013 Martin Varga <android@kul.is>
 */
package com.android.tapyaoming;

import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.Engine;
import org.andengine.engine.camera.Camera;
import org.andengine.opengl.font.BitmapFont;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.ITexture;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.atlas.bitmap.BuildableBitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource;
import org.andengine.opengl.texture.atlas.buildable.builder.BlackPawnTextureAtlasBuilder;
import org.andengine.opengl.texture.atlas.buildable.builder.ITextureAtlasBuilder.TextureAtlasBuilderException;
import org.andengine.opengl.texture.bitmap.BitmapTextureFormat;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.GooglePlayUtils;
import org.andengine.util.adt.color.Color;

import android.graphics.Typeface;

public class ResourceManager {
	private static final ResourceManager INSTANCE = new ResourceManager();

	// font
	public Font font;
	public Font fontGameOver;
	public Font fontScore;
	public Font fontBest;

	// common objects
	public GameActivity activity;
	public Engine engine;
	public Camera camera;
	public VertexBufferObjectManager vbom;

	// gfx
	private BitmapTextureAtlas repeatingGroundAtlas;

	public TextureRegion repeatingGroundRegion;

	private BuildableBitmapTextureAtlas gameObjectsAtlas;

	public TextureRegion cloudRegion;
	public TextureRegion dandelionRegion;
	public TextureRegion pillarRegion;

	public TextureRegion bannerRegion;
	public TextureRegion backGround;
	public TextureRegion tapBackGround;
	public TextureRegion box;
	public TextureRegion btnPlay;
	public TextureRegion btnScore;
	public TextureRegion btnFbShare;
	// sfx
	public Sound sndFly;
	public Sound sndFail;

	private ResourceManager() {
	}

	public static ResourceManager getInstance() {
		return INSTANCE;
	}

	public void create(GameActivity activity, Engine engine, Camera camera,
			VertexBufferObjectManager vbom) {
		this.activity = activity;
		this.engine = engine;
		this.camera = camera;
		this.vbom = vbom;
	}

	public void loadFont() {
		FontFactory.setAssetBasePath("font/");
		font = FontFactory.createStrokeFromAsset(activity.getFontManager(),
				activity.getTextureManager(), 256, 256, activity.getAssets(),
				"font.ttf", 50f, true, Color.WHITE_ABGR_PACKED_INT, 2,
				Color.BLACK_ABGR_PACKED_INT);
		font.load();
		fontScore = FontFactory.createStrokeFromAsset(
				activity.getFontManager(), activity.getTextureManager(), 256,
				256, activity.getAssets(), "font.ttf", 40f, true,
				Color.WHITE_ABGR_PACKED_INT, 2, Color.BLACK_ABGR_PACKED_INT);
		fontScore.load();
		fontGameOver = FontFactory.createStrokeFromAsset(
				activity.getFontManager(), activity.getTextureManager(), 256,
				256, activity.getAssets(), "font.ttf", 60f, true,
				Color.YELLOW_ARGB_PACKED_INT, 2, Color.BLACK_ARGB_PACKED_INT);
		fontGameOver.load();
	}

	public void unloadFont() {
		font.unload();
		fontGameOver.unload();
		fontScore.unload();
	}

	// splash
	public void loadGameResources() {
		BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

		repeatingGroundAtlas = new BitmapTextureAtlas(
				activity.getTextureManager(), 256, 256,
				TextureOptions.REPEATING_BILINEAR_PREMULTIPLYALPHA);
		repeatingGroundRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(repeatingGroundAtlas, activity, "ground.png",
						0, 0);
		repeatingGroundAtlas.load();

		gameObjectsAtlas = new BuildableBitmapTextureAtlas(
				activity.getTextureManager(), 1024, 1024,
				BitmapTextureFormat.RGBA_8888, TextureOptions.BILINEAR);

		cloudRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "cloud.png");

		pillarRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "pillar.png");

		dandelionRegion = BitmapTextureAtlasTextureRegionFactory
				.createFromAsset(gameObjectsAtlas, activity.getAssets(),
						"dandelion.png");
		backGround = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "background.png");

		tapBackGround = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "tap.png");

		box = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "box.png");

		btnFbShare = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "btnfbshare.png");

		btnPlay = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "btnplay.png");

		btnScore = BitmapTextureAtlasTextureRegionFactory.createFromAsset(
				gameObjectsAtlas, activity.getAssets(), "btnscore.png");
		try {
			gameObjectsAtlas
					.build(new BlackPawnTextureAtlasBuilder<IBitmapTextureAtlasSource, BitmapTextureAtlas>(
							2, 0, 2));
			gameObjectsAtlas.load();

		} catch (final TextureAtlasBuilderException e) {
			throw new RuntimeException("Error while loading Splash textures", e);
		}

		try {
			sndFly = SoundFactory.createSoundFromAsset(activity.getEngine()
					.getSoundManager(), activity, "sfx/fly.wav");
			sndFail = SoundFactory.createSoundFromAsset(activity.getEngine()
					.getSoundManager(), activity, "sfx/fail.wav");
		} catch (Exception e) {
			throw new RuntimeException("Error while loading sounds", e);
		}
	}
}
