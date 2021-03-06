package com.a7m5.chess;

import java.util.ArrayList;

import javax.swing.JOptionPane;

import com.a7m5.chess.chesspieces.ChessOwner;
import com.a7m5.chess.chesspieces.ChessPieceSet;
import com.a7m5.networking.Client;
import com.a7m5.networking.Server;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.FPSLogger;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;

public class ChessGame3D implements ApplicationListener {

	public static int width;
	public static int height;
	private static String cacheDirectory = null;

	//3D stuff
	public static PerspectiveCamera cam;
	private Environment environment;
	public ModelBatch modelBatch;
	public Model model;
	private AssetManager assets;
	private Array<ModelInstance> modelInstances;
	private boolean loadingAssets;
	
	private ModelInstance skyDomeModelInstance = null;
	
	private ModelInstance opponentCameraModelInstance = null;
	private float[][] opponentCamera = new float[0][0];
	
	//End 3D Stuff
	private SpriteBatch batch;
	private Sprite yourTurnSprite;
	private Sprite waitSprite;
	private int port;
	private String address;
	private Texture yourTurnTexture;
	private Texture waitTexture;
	private ChessInputProcessor inputProcessor;
	private static ChessGame3D self;

	private static Server server = null;
	private static Client client = null;
	private static Thread clientThread = null;
	private static Thread serverThread = null;
	private static ChessOwner owner;
	
	//Opimization Testing
	private FPSLogger fpsLogger;

	public ChessGame3D(ChessOwner chessOwner, String address, int port) {
		self = this;
		setOwner(chessOwner);
		setAddress(address);
		setPort(port);
	}

	@Override
	public void create() {
		fpsLogger = new FPSLogger();
		//3D
		modelBatch = new ModelBatch();

		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(256f, 256f, 128f);
		cam.lookAt(256f,0, -256f);
		cam.near = 0.1f;
		cam.far = 1536f;
		cam.update();

		/*
		 * coordinate system
		 * x: left (negative) to right (positive)
		 * y: up (positive) and down (negative)
		 * z: forward (negative) and backward (positive)
		 */
		
		ChessBoard.loadTextures();

		inputProcessor = new ChessInputProcessor(cam);
		Gdx.input.setInputProcessor(inputProcessor);

		width = Gdx.graphics.getWidth();
		height = Gdx.graphics.getHeight();

		batch = new SpriteBatch();
		batch.getTransformMatrix().rotate(1, 0, 0, -90);

		//Your Turn
		yourTurnTexture = new Texture(Gdx.files.internal("data/your_turn.png"));
		yourTurnTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);

		TextureRegion yourTurnRegion = new TextureRegion(yourTurnTexture, 0, 0, 128, 128);

		yourTurnSprite = new Sprite(yourTurnRegion);
		yourTurnSprite.setOrigin(yourTurnSprite.getWidth()/2, yourTurnSprite.getHeight()/2);
		yourTurnSprite.setPosition(612-64, 412-64-128-32);

		//Wait
		waitTexture = new Texture(Gdx.files.internal("data/wait.png"));
		waitTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);


		TextureRegion waitRegion = new TextureRegion(waitTexture, 0, 0, 128, 128);

		waitSprite = new Sprite(waitRegion);
		waitSprite.setOrigin(waitSprite.getWidth()/2, waitSprite.getHeight()/2);
		waitSprite.setPosition(612-64, 412-64-128-32);

		/*
		 * 3D Assets
		 */
		modelInstances = new Array<ModelInstance>();
		assets = new AssetManager();

		/* Enemy Camera
		 * Model: models/bishop.g3dj
		 * Note: Currently, the bishop model is loaded for testing purposes.
		 */
		assets.load("models/camera.g3db", Model.class);
		assets.load("models/skydome.g3db", Model.class);
		loadingAssets = true;


		client = new Client(address, port);
		clientThread = new Thread(client);
		clientThread.start();
		
	}

	@Override
	public void dispose() {
		batch.dispose();
		yourTurnTexture.dispose();
		waitTexture.dispose();

		modelBatch.dispose();
	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClearColor(0.5f, 0.75f, 1f, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT | (Gdx.graphics.getBufferFormat().coverageSampling?GL20.GL_COVERAGE_BUFFER_BIT_NV:0));

		if (loadingAssets && assets.update()) {
			onAssetsLoaded();
		}

		if(client != null && clientThread.isAlive()) {
			inputProcessor.update(Gdx.graphics.getRawDeltaTime());

			modelBatch.begin(cam);
			if(!loadingAssets && opponentCamera.length != 0) {
				com.badlogic.gdx.math.Vector3 position = new com.badlogic.gdx.math.Vector3(
						opponentCamera[0][0],
						opponentCamera[0][1],
						opponentCamera[0][2]);
				com.badlogic.gdx.math.Vector3 direction = new com.badlogic.gdx.math.Vector3(
						-opponentCamera[1][0],
						opponentCamera[1][1],
						opponentCamera[1][2]);
				com.badlogic.gdx.math.Vector3 up = new com.badlogic.gdx.math.Vector3(
						opponentCamera[2][0],
						opponentCamera[2][1],
						opponentCamera[2][2]);
				opponentCameraModelInstance.transform.setToWorld(
						position,
						direction,
						up).scale(8f, 8f, 8f);
				modelBatch.render(opponentCameraModelInstance, environment);
			}
			
			/*
			 * Draws Board Tiles in 3D
			 * Note: This is more efficient and allows drawing the rectangles in 3d,
			 * 		 which resolves conflicts with 3D models.
			 */
			
			client.board.drawBoard(modelBatch, environment);

			client.board.drawCursors(modelBatch, environment, cam);
			if(!loadingAssets) {
				modelBatch.render(modelInstances, environment);
			}
			modelBatch.end();
			batch.setProjectionMatrix(cam.combined);
			batch.begin();

			if(getOwner() == getClient().board.getTurnOwner()) {
				yourTurnSprite.draw(batch);
			} else {
				waitSprite.draw(batch);
			}
			client.board.drawPieces(batch);
			batch.end();
		}
		
		// output the current FPS
        fpsLogger.log();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}

	public void onAssetsLoaded() {
		Model opponentCameraModel = assets.get("models/camera.g3db", Model.class);
		opponentCameraModelInstance = new ModelInstance(opponentCameraModel);
		Model skyDomeModel = assets.get("models/skydome.g3db", Model.class);
		skyDomeModelInstance = new ModelInstance(skyDomeModel);
		skyDomeModelInstance.transform.translate(ChessBoard.actualBoardWidth/2, 0, -ChessBoard.actualBoardWidth/2).scl(4f);
		modelInstances.add(skyDomeModelInstance);
		loadingAssets = false;
	}

	public static void startServer(int port) {

		// Grab the set of chess pieces before starting the server.
		ResourceGrabber myGrab;
		myGrab = new ResourceGrabber();
		ChessPieceSet gamePieceSet = myGrab.getChessPieceSet();

		Object[] possibilities = new Object[myGrab.getBoards().size()];
		for( int i = myGrab.getBoards().size() - 1; i >= 0; i--){
			possibilities[i] = (myGrab.getBoards().get(i).getName());
		}
		
		String selection = (String)JOptionPane.showInputDialog(null, "Which board do you want to play.", "Board Selection", JOptionPane.PLAIN_MESSAGE,
		                    null,
		                    possibilities,
		                    myGrab.getBoards().get(0).getName());
		
		int boardToPlay = 0;
		for(int i = 0; i < possibilities.length; i++){
			if(selection == possibilities[i]){
				boardToPlay = i;
			}
		}
		
		// Make the new board
			ChessBoard board = myGrab.getBoards().get(boardToPlay);	// Starting pieces from grabbed chess board.
			board.setTurnOwner(ChessOwner.WHITE);

			server = new Server(port, board);
			serverThread = new Thread(server);
			serverThread.start();
		}

	

	public static void killServer() throws InterruptedException {
		if(server != null && serverThread != null) {
			if(serverThread.isAlive()) {
				server.kill();
			}
		}
		server = null;
		serverThread = null;
	}


	public static void onClickListener(int x, int y, int pointer, int button) {
		if(client != null) {
			client.onClickListener(x, y, pointer, button);
		}
	}

	public static void clearBoard() {
		if(client != null) {
			client.board.clear();
		}
	}

	public static void restartBoard() {
		client.board.restart();
	}

	public static Client getClient() {
		return client;
	}

	public static ChessOwner getOwner() {
		return owner;
	}

	public static void setOwner(ChessOwner arg) {
		owner = arg;
	}

	private void setPort(int port) {
		this.port = port;
	}

	private void setAddress(String address) {
		this.address = address;
	}

	public static PerspectiveCamera getCamera() {
		return cam;
	}

	public static void setOpponentCamera(float[][] oc) {
		getInstance().opponentCamera = oc;
	}

	private static ChessGame3D getInstance() {
		return self;
	}

	public static String getCacheDirectory() {
		return cacheDirectory;
	}

	public static void setCacheDirectory(String cacheDirectory) {
		ChessGame3D.cacheDirectory = cacheDirectory;
	}
}
