import javafx.application.*;
import javafx.stage.*;
import javafx.scene.*;
import javafx.event.* ;
import javafx.scene.control.* ;
import javafx.scene.layout.* ;
import javafx.scene.Group ;
import javafx.scene.shape.*;
import javafx.scene.paint.*;
import javafx.scene.input.* ;
import javafx.scene.image.* ;
import javafx.scene.text.* ;
import javafx.animation.* ;
import javafx.util.* ;
import javafx.geometry.* ;
import javafx.beans.property.* ;
import javafx.beans.value.* ;
import javafx.beans.binding.* ;
import java.util.* ;
import java.io.* ;

// javac --module-path "C:\Program Files\Java\javafx-sdk-11.0.2\lib" --add-modules javafx.controls Game.java
// java --module-path "C:\Program Files\Java\javafx-sdk-11.0.2\lib" --add-modules javafx.controls Game

public class Game extends Application
{
	protected final double MAP_WIDTH = 505 ;
	protected final double MAP_HEIGHT = 535 ;
	protected final double IMAGE_WIDTH = 50 ;
	protected final double IMAGE_HEIGHT = 25 ;
	protected final double LASER_SPEED = 2.15 ;
	protected final double PLAYER_SPEED = 5.5 ;
	protected final double ENEMY_SPEED = 5.25 ;
	protected final int MAX_ROW = 5 ;
	protected final int MAX_COL = 8 ;

	protected double count, playerFireCount  ;		// playerFireCount keeps track of how many seconds has passed since the player last shot
	protected long movePeriod ;
	protected int score, rowToMove, enemyCount, highScore ;
	protected boolean paused, gameOver ;

	protected Stage window ;
	protected Scene scene ;
	protected Pane root ;
	protected Label scoreLabel, highScoreLabel, healthLabel ;
	protected Timeline timeline ;
	protected KeyFrame keyFrame ;
	protected ImageView playerImage, enemy1_img, enemy2_img ;

	protected Spaceship player, bonusSpaceship ;

	protected Timer moveTimer, enemyFireTimer, playerFireTimer, bonusSpawnTimer ;
	protected TimerTask moveTask, enemyFireTask, playerFireTask, bonusSpawnTask ;

	protected ArrayList<Laser> laserList = new ArrayList<Laser>() ;
    protected Spaceship enemies[][] = new Spaceship [MAX_ROW][MAX_COL] ;
	protected ConfirmationBox confirmationBox = new ConfirmationBox() ;
	private Controller controller ;

    enum Dir
    {
    	LEFT, RIGHT ;
    }
    protected Dir dir ;		// indicates the direction the enemy spaceships are to move
    protected Dir bonusDir ;	// indicates the direction the bonus spaceship is to move


	public static void main(String[] args) 
	{
		Application.launch(args) ;
	}


	@Override
	public void start(Stage primaryStage)
	{
		window = primaryStage ;
		window.setTitle("S P A C E   I N V A D E R S") ;
		window.setResizable(false) ;

		root = new Pane() ;
		root.setPrefWidth(MAP_WIDTH) ;
		root.setPrefHeight(MAP_HEIGHT) ;
		root.setStyle("-fx-background-color: black") ;		

		playerImage = new ImageView(new Image("file:Player.png")) ;
		playerImage.setFitWidth(IMAGE_WIDTH) ;
		playerImage.setFitHeight(IMAGE_HEIGHT) ;
		playerImage.setCache(true) ;

		score = 0 ;
		highScore = getHighScore() ;

		playerFireCount = 0 ;

		player = new Spaceship(playerImage, 250, (MAP_HEIGHT - IMAGE_HEIGHT), root) ;
		player.setName("PLAYER") ;
		player.setHealth(5) ;
		
		scoreLabel = new Label("Your Score: " + Integer.toString(score)) ;
		scoreLabel.setPrefHeight(20) ;
		scoreLabel.setStyle("-fx-text-fill : white ;" +
							"-fx-font-size : 11pt ;") ;

		highScoreLabel = new Label("High Score: " + Integer.toString(highScore)) ;
		highScoreLabel.setPrefHeight(20) ;
		highScoreLabel.setStyle("-fx-text-fill : white ;" +
							"-fx-font-size : 11pt ;") ;

		HBox hbox = new HBox(250) ;
		hbox.setStyle("-fx-background-color : black") ;
		hbox.getChildren().addAll(scoreLabel, highScoreLabel) ;

		healthLabel = new Label("Health: " + Integer.toString(player.getHealth())) ;
		healthLabel.setStyle("-fx-background-color : black ;" +
							 "-fx-border-color : darkgray ;" +
							 "-fx-text-fill : white ;" +
							 "-fx-font-size : 11pt ;") ;
		healthLabel.setPrefWidth(MAP_WIDTH) ;

		VBox vbox = new VBox() ;
		vbox.getChildren().addAll(hbox, root, healthLabel) ;

		spawnEnemy() ;

		initializeTimerTasks() ;

		movePeriod = 240 ;

		moveTimer = new Timer() ;
		moveTimer.scheduleAtFixedRate(moveTask, (long)(0), movePeriod) ;	// executes task after a delay of 0 seconds and repeats every "movePeriod" milliseconds
		
		enemyFireTimer = new Timer() ;
		enemyFireTimer.scheduleAtFixedRate(enemyFireTask, 0, (long)(1000*1.5)) ;		// executes every 1.5 seconds - enemies fire every 2 seconds
		
		playerFireTimer = new Timer() ;
		playerFireTimer.scheduleAtFixedRate(playerFireTask, 0, (long)(1000*0.5)) ; 		// executes every half second

		bonusSpawnTimer = new Timer() ;
		bonusSpawnTimer.scheduleAtFixedRate(bonusSpawnTask, (long)(1000*5), (long)(1000*15)) ;

		scene = new Scene(vbox) ;
		scene.setOnKeyPressed(event ->
		{
			switch(event.getCode()) 
			{
				case LEFT : if(gameOver == false && !paused)
							{
								player.setX(player.getX() - PLAYER_SPEED) ;
							
								if(player.getX()+(IMAGE_WIDTH*0.5) < 0 )
									player.setX(MAP_WIDTH-(IMAGE_WIDTH*0.5)) ;
							}

						  	break ;

				case RIGHT : if(gameOver == false && !paused)
							 {
							 	player.setX(player.getX() + PLAYER_SPEED) ;
							
							 	if(player.getX()+(IMAGE_WIDTH*0.5) > MAP_WIDTH)
							 		player.setX(-(IMAGE_WIDTH*0.5)) ;
							 }

							 break ;


				case ESCAPE : 	pauseOrPlay() ;
							 	break ;

			}
		}) ;
		scene.setOnKeyReleased(event ->
		{
			if(event.getCode() == KeyCode.SPACE)
			{	
				// only after a second has passed since the player last shot is the player allowed to shoot again
				if(gameOver == false &&  !paused && playerFireCount >= 1)
				{
					laserList.add(player.fire()) ;
					playerFireCount = 0 ;
				}
			}
		}) ;

		play() ;

		window.setScene(scene) ;
		window.show() ;
	}


	private void play()
	{
		rowToMove = 3 ;
		dir = Dir.RIGHT ;
		gameOver = false ;

		count = 0 ;
		keyFrame = new KeyFrame(Duration.millis(10), event ->
		{
			Laser tempLaser ;

			// move the bonus spaceship(if it's there) every 10 milliseconds
			if(count % 10 == 0 && bonusSpaceship != null)
			{
				if(bonusDir == Dir.RIGHT)
					bonusSpaceship.setX(bonusSpaceship.getX() + ENEMY_SPEED) ;
				else
					bonusSpaceship.setX(bonusSpaceship.getX() - ENEMY_SPEED) ;

				if((bonusSpaceship.getX()+IMAGE_WIDTH < 0 && bonusDir == Dir.LEFT) || (bonusSpaceship.getX() > MAP_WIDTH && bonusDir == Dir.RIGHT))
				{
					bonusSpaceship.destruct() ;
					bonusSpaceship = null ;
				}
			}

			// remove the lasers that are off the map
			for(int index = 0 ; index < laserList.size() ; index++)
			{
				tempLaser = laserList.get(index) ;

				if(tempLaser.getY() < 0 || tempLaser.getY() > MAP_HEIGHT)
				{					
					laserList.remove(tempLaser) ;
					tempLaser.destroy() ;
				}
			}

			// move the lasers
			for(int index = 0 ; index < laserList.size() ; index++)
			{
				tempLaser = laserList.get(index) ;

				if(tempLaser.getDirection().equals("UP"))
				{
					tempLaser.setY(tempLaser.getY() - LASER_SPEED) ;		// move the player's laser up the map

					// check if the laser hits any of the enemies
					int row = 0 ;
					boolean hit = false ;
					while(row < MAX_ROW && !hit)
					{
						for(int col = 0 ; col < MAX_COL ; col++)
						{
							if(enemies[row][col] != null)
							{
								Spaceship enemy = enemies[row][col] ;

								if(enemy.hit(tempLaser) == true)
								{
									tempLaser.destroy() ;
									laserList.remove(tempLaser) ;

									enemy.destruct() ;									
									enemies[row][col] = null ;
									enemyCount-- ;

									score+=10 ;

									scoreLabel.setText("Your Score: " + Integer.toString(score)) ;
									hit = true ;

									if(enemyCount == 0)
									{
										controller = new Controller("WON") ;
										controller.run() ;
									}

									// decrease the enemies' move period
									if(enemyCount <= 35 && enemyCount > 0)
									{
										movePeriod-=8 ;

										if(enemyCount <= 7 && enemyCount >= 5)
											movePeriod = 8 ;
										else if(enemyCount <= 4 && enemyCount >= 2)
											movePeriod = 5 ;
										else if(enemyCount == 1)
											movePeriod = 2 ;
									}
									
									moveTimer.cancel() ;
									moveTimer = new Timer() ;
									moveTask = new TimerTask()
									{
										@Override
										public void run()
										{
											move() ;
										}
									} ;
									moveTimer.scheduleAtFixedRate(moveTask, (long)(0), movePeriod) ;

									// decrease the enemies' fire period when there are 10 enemies. This will be the new fire period for the enemies 
									if(enemyCount == 10)
									{
										enemyFireTimer.cancel() ;

										enemyFireTimer = new Timer() ;
										enemyFireTask = new TimerTask()
										{
											@Override
											public void run()
											{
												enemyFire() ;
											}
										} ;
										enemyFireTimer.scheduleAtFixedRate(enemyFireTask, 0, (long)(1000*1)) ;	// will fire every 1 second now
									}

									break ;
								}
							}
						}

						row++ ;
					}

					// check if the player's laser hit the bonus spaceship(if it's there that is)
					if(bonusSpaceship != null && bonusSpaceship.hit(tempLaser) == true)
					{
						tempLaser.destroy() ;
						bonusSpaceship.destruct() ;
						bonusSpaceship = null ;

						if(enemyCount <= 15)	// when there less that 15 enemies remaining
							score+=25 ;
						else
							score+=15 ;

						scoreLabel.setText("Your Score: " + Integer.toString(score)) ;
					}
				}
				else
				{
					tempLaser.setY(tempLaser.getY() + LASER_SPEED) ;		// move the enemy's laser down the map

					// check if the laser hits the player
					if(player.hit(tempLaser) == true)
					{
						tempLaser.destroy() ;
						laserList.remove(tempLaser) ;
						player.setHealth(player.getHealth() - 1) ;
						healthLabel.setText("Health: " + Integer.toString(player.getHealth())) ;

						if(player.getHealth() == 0)
						{
							player.destruct() ;

							Controller controller = new Controller("LOST") ;
							controller.run() ;
						}
					}
				}
			}

			count++ ; 
		}) ;

		timeline = new Timeline(keyFrame) ;
		timeline.setCycleCount(Timeline.INDEFINITE) ;
		timeline.play() ;

		paused = false ;
	}


	private void pauseOrPlay()
	{
		if(!paused) 
		{
			timeline.pause() ;

			moveTimer.cancel() ;
			playerFireTimer.cancel() ;
			enemyFireTimer.cancel() ;
			bonusSpawnTimer.cancel() ;
		}
		else
		{
			timeline.play() ;
			
			initializeTimerTasks() ;

			moveTimer = new Timer() ;
			moveTimer.scheduleAtFixedRate(moveTask, (long)(0), movePeriod) ;
								
			enemyFireTimer = new Timer() ;
			enemyFireTimer.scheduleAtFixedRate(enemyFireTask, (long)(0), (long)(1000*1.5)) ;
								
			playerFireTimer = new Timer() ;
			playerFireTimer.scheduleAtFixedRate(playerFireTask, (long)(0), (long)(1000*0.5)) ;

			bonusSpawnTimer = new Timer() ;
			bonusSpawnTimer.scheduleAtFixedRate(bonusSpawnTask, (long)(1000*5), (long)(1000*15)) ;
		}

		paused = !paused ;
	}


	private void initializeTimerTasks()
	{		
		moveTask = new TimerTask()
		{
			@Override
			public void run()
			{				
				move() ;
			}
		} ;		

		enemyFireTask = new TimerTask()
		{
			@Override
			public void run()
			{
				enemyFire() ;
			}
		} ;

		playerFireTask = new TimerTask()
		{
			@Override
			public void run()
			{
				playerFireCount+=0.5 ;
			}
		} ;

		bonusSpawnTask = new TimerTask()
		{
			@Override
			public void run()
			{
				Platform.runLater(() ->
				{
					if(bonusSpaceship == null)
					{
						double x ;

						ImageView image_view = new ImageView(new Image("file:Enemy6.png")) ;
						image_view.setFitWidth(IMAGE_HEIGHT+5) ;
						image_view.setFitHeight(IMAGE_HEIGHT+5) ;
						image_view.setCache(true) ;

						// by random determine which direction the bonus spaceship must move in
						int num = (int)(Math.random() * 11) ;

						if(num % 2 == 0)
						{
							bonusDir = Dir.LEFT ;
							x = MAP_WIDTH ;
						}
						else
						{ 
							bonusDir = Dir.RIGHT ;
							x = -IMAGE_WIDTH ;
						}

						bonusSpaceship = new Spaceship(image_view, x, (IMAGE_HEIGHT * 0.5) + 5, root) ;
						bonusSpaceship.setName("ENEMY") ;
					}
				}) ;
			}
		} ;
	}


	private void move()
	{
		Platform.runLater(() ->
		{
			Spaceship spaceship = null ;
			boolean moveDown = false ;
			Controller controller ;

			// first find out if the spaceships have got to move a row down
			for(int col = 0 ; col < MAX_COL ; col++)
			{
				for(int row = 0 ; row < MAX_ROW ; row++) 
				{
					spaceship = enemies[row][col] ;

					if(spaceship != null)
					{
						if(dir == Dir.LEFT)
						{
							if(spaceship.getX()-ENEMY_SPEED < 0) moveDown = true ;
						}
						else if(dir == Dir.RIGHT)
						{
							if((spaceship.getX()+IMAGE_WIDTH)+ENEMY_SPEED > MAP_WIDTH)	moveDown = true ;
						}
					}
				}
			}

			
			if(moveDown)
			{
				for(int row = 0 ; row < MAX_ROW  ; row++)
				{
					for(int col = 0 ; col < MAX_COL ; col++)
					{
						spaceship = enemies[row][col] ;

						if(spaceship != null)
							spaceship.setY(spaceship.getY() + IMAGE_HEIGHT + 5) ;
					}
				}

				// reverse the direction of the ships' movement
				if(dir == Dir.LEFT)
					dir = Dir.RIGHT ;
				else 
					dir = Dir.LEFT ;

				rowToMove = MAX_ROW - 1 ;
			}
			else
			{
				// move them in the x-direction
				for(int col = 0 ; col < MAX_COL ; col++)
				{
					spaceship = enemies[rowToMove][col] ;

					if(spaceship != null)
					{
						if(dir == Dir.LEFT)
							spaceship.setX(spaceship.getX()-ENEMY_SPEED) ;
						else if(dir == Dir.RIGHT)
							spaceship.setX(spaceship.getX()+ENEMY_SPEED) ;

						// check if the enemy spaceship has reached the player
						if(spaceship.getY()+IMAGE_HEIGHT >= player.getY())
						{
							controller = new Controller("LOST") ;
							controller.run() ;
						}
					}
				}

				if(rowToMove == 0)
					rowToMove = MAX_ROW -1 ;
				else
					rowToMove-- ;
			}
		}) ;		
	}


	// enemies fire
	private void enemyFire()
	{
		Platform.runLater(() ->
		{
			Spaceship spaceship ;
			boolean canFire ;
			double playerX ;
			int fireCount = 0 ;		// counts the number of spaceships that fired

			for(int row = MAX_ROW-1 ; row > -1 ; row--)
			{
				for(int col = 0 ; col < MAX_COL ; col++)
				{
					spaceship = enemies[row][col] ;

					canFire = false ;
					if(spaceship != null)
					{
						playerX = player.getX() ;

						// check if the player is range to shoot
						if((playerX >= spaceship.getX() && playerX <= spaceship.getX()+IMAGE_WIDTH) || (playerX+IMAGE_WIDTH <= spaceship.getX()+IMAGE_WIDTH && playerX+IMAGE_WIDTH >= spaceship.getX()))
						{
							canFire = true ;							
						}
						else if(fireCount < 2)
						{
							// decide at random whether to fire or not
							int num = (int)(Math.random() * 101) ;

							if(num % 2 == 0) canFire = true ;
						}

						// check if there are no enemy spacechips in the way
						if(canFire)
						{
							if(row == MAX_ROW-1)
							{
								laserList.add(spaceship.fire()) ;
							}
							else
							{
								if(enemies[row+1][col] == null || (enemies[row+1][col].getX() >= spaceship.getX()+IMAGE_WIDTH || enemies[row+1][col].getX()+IMAGE_WIDTH <= spaceship.getX()))
								{
									if(row+2 < MAX_ROW)
									{
										if(enemies[row+2][col] == null || (enemies[row+2][col].getX() >= spaceship.getX()+IMAGE_WIDTH || enemies[row+2][col].getX()+IMAGE_WIDTH <= spaceship.getX()))
										{
											if(row+3 < MAX_ROW)
											{
												if(enemies[row+3][col] == null || (enemies[row+3][col].getX() >= spaceship.getX()+IMAGE_WIDTH || enemies[row+3][col].getX()+IMAGE_WIDTH <= spaceship.getX()))
												{
													if(row+4 < MAX_ROW)
													{
														if(enemies[row+4][col] == null || (enemies[row+4][col].getX() >= spaceship.getX()+IMAGE_WIDTH || enemies[row+4][col].getX()+IMAGE_WIDTH <= spaceship.getX()))
															laserList.add(spaceship.fire()) ;
													}
													else
													{
														laserList.add(spaceship.fire()) ;
													}
												}												
											}
											else
											{
												laserList.add(spaceship.fire()) ;
											}
										}
									}
									else
									{
										laserList.add(spaceship.fire()) ;
									}
								}
							}

							fireCount++ ;
						}
					}
				}
			}
		}) ;
	}


	private void spawnEnemy()
	{
		ImageView image_view ;
		Spaceship enemy ;
		int num, row, col = 0;
		double x, y ;

		row = 0 ;
		enemyCount = 0 ;
		for(y = (IMAGE_HEIGHT*1.5) + 10 ; row < MAX_ROW ; y+=(IMAGE_HEIGHT + 10))
		{
			col = 0 ;
			for(x = IMAGE_WIDTH*0.5 ; (x <= MAP_WIDTH - IMAGE_WIDTH) && (col < MAX_COL) ; x+=(IMAGE_WIDTH + 4))
			{
				num = (int)(Math.random() * 55) ;	// get a random integer from 0 to 55 inclusive
				
				if(num % 4 == 0)
					image_view = new ImageView(new Image("file:Enemy4.png")) ;
				else if(num % 3 == 0)
					image_view = new ImageView(new Image("file:Enemy3.png")) ;
				else if(num % 2 == 0)
					image_view = new ImageView(new Image("file:Enemy2.png")) ;
				else
					image_view = new ImageView(new Image("file:Enemy1.png")) ;

				image_view.setFitWidth(IMAGE_WIDTH) ;
				image_view.setFitHeight(IMAGE_HEIGHT) ;
				image_view.setCache(true) ;

				enemy = new Spaceship(image_view, x, y, root) ;
				enemy.setName("ENEMY") ;

				enemies[row][col] = enemy ;
				enemyCount++ ;

				col++ ;
			}

			row++ ;
		}
	}


	private Integer getHighScore()
	{
		Scanner scan = null ;

		try
		{
			File file = new File("C:\\Users\\...\\SpaceInvaders\\HighScore.txt") ;	// the path to the text file
			scan = new Scanner(file) ;	
		}
		catch(Exception e)
		{
			System.out.println("\nException in getHighScore() -> \n" + e) ;
		}

		return Integer.parseInt(scan.nextLine()) ;	// read and return the first line
	} 


	private void setHighScore(String high_score)
	{
		try
		{
			FileWriter writer = new FileWriter("HighScore.txt") ;
			writer.write(high_score) ;
			writer.close() ;
		}
		catch(IOException ex) 
		{
			ex.printStackTrace() ;
		}
	}



	public class Controller implements Runnable
	{
		private String status ;

		public Controller(String s)
		{
			this.status = s ;
		}

		@Override
		public void run()
		{
			try
			{
				Platform.runLater(() ->
				{
					gameOver = true ;

					timeline.stop() ;

					// cancel all the TimerTasks
					moveTimer.cancel() ;
					enemyFireTimer.cancel() ;
					playerFireTimer.cancel() ;
					bonusSpawnTimer.cancel() ;

					// set the new high score if the player has passed the previous high score
					if(score > highScore)
					{
						highScore = score ;
						setHighScore(Integer.toString(highScore)) ;
					}

					Label lbl_1 = new Label() ;
					lbl_1.setText("G A M E   O V E R") ;
					lbl_1.setStyle("-fx-text-fill : red ;" +
								   "-fx-font-size : 14pt ;" +
								   "-fx-font-weight : bold ;") ;

					Label lbl_2 = new Label() ;

					if(status.toUpperCase().equals("LOST"))
					{
						lbl_2.setText("YOU LOST") ;
						lbl_2.setStyle("-fx-text-fill : white ;" +
									   "-fx-font-size : 12pt ;") ;									   
					}
					else
					{
						lbl_2.setText("WELL DONE!!") ;
						lbl_2.setStyle("-fx-text-fill : lime ;" +
									   "-fx-font-size : 12pt ;" +
									   "-fx-font-weight : bold ;") ;
					}

					Button restartBtn = new Button("PLAY AGAIN") ;
					restartBtn.setStyle("-fx-background-color : black ;" +
										"-fx-border-color : yellow ;" +
										"-fx-text-fill : yellow ;" +
										"-fx-font-size : 11pt ;") ;
					restartBtn.setPrefWidth(120) ;
					restartBtn.setPrefHeight(25) ;
					restartBtn.setPadding(new Insets(15)) ;

					VBox vbox = new VBox(15) ;
					vbox.getChildren().addAll(lbl_1, lbl_2, restartBtn) ;
					vbox.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE) ;
					vbox.setAlignment(Pos.CENTER) ;
					vbox.setPadding(new Insets(15, 15, 15, 15)) ; 		// top, right, bottom, left
					vbox.setLayoutX((MAP_WIDTH/2) - 85) ;
					vbox.setLayoutY(MAP_HEIGHT/2 - 90) ;
					vbox.setStyle("-fx-background-color : black ;") ;

					root.getChildren().addAll(vbox) ;

					restartBtn.setOnAction(event ->
					{
						// get rid of all the enemy spaceships remaining on screen
						for(int row = 0 ; row < MAX_ROW ; row++)
						{
							for(int col = 0 ; col < MAX_COL ; col++)
							{
								if(enemies[row][col] != null)
									enemies[row][col].destruct() ;
							}
						}
						if(bonusSpaceship != null)
						{
							bonusSpaceship.destruct() ;
							bonusSpaceship = null ;
						}

						// get rid of all the lasers on screen
						for(int index = 0 ; index < laserList.size() ; index++)
						{
							laserList.get(index).destroy() ;
						}
						laserList.clear() ;

						player.destruct() ;

						playerFireCount = 0 ;

						playerImage = new ImageView(new Image("file:Player.png")) ;
						playerImage.setFitWidth(IMAGE_WIDTH) ;
						playerImage.setFitHeight(IMAGE_HEIGHT) ;
						playerImage.setCache(true) ;

						player = new Spaceship(playerImage, 250, (MAP_HEIGHT - IMAGE_HEIGHT), root) ;
						player.setName("PLAYER") ;
						player.setHealth(5) ;

						healthLabel.setText("Health: " + Integer.toString(player.getHealth())) ;

						score = 0 ;
						scoreLabel.setText("Your Score: " + score) ;

						highScoreLabel.setText("High Score: " + highScore) ;

						spawnEnemy() ;

						initializeTimerTasks() ;

						movePeriod = 240 ;

						moveTimer = new Timer() ;
						moveTimer.scheduleAtFixedRate(moveTask, (long)(0), movePeriod) ;	// executes task after a delay of 0 seconds and repeats every "movePeriod" milliseconds
			
						enemyFireTimer = new Timer() ;
						enemyFireTimer.scheduleAtFixedRate(enemyFireTask, (long)(0), (long)(1000*1.5)) ;		// executes every 1.5 seconds - enemies fire every 2 seconds
		
						playerFireTimer = new Timer() ;
						playerFireTimer.scheduleAtFixedRate(playerFireTask, (long)(0), (long)(1000*0.5)) ; 	// executes every half second

						bonusSpawnTimer = new Timer() ;
						bonusSpawnTimer.scheduleAtFixedRate(bonusSpawnTask, (long)(1000*15), (long)(1000*12)) ;

						root.getChildren().remove(vbox) ;

						try
						{
							Thread.sleep(1000*1) ;	
						}
						catch(Exception e)
						{
							System.out.println("\nException when trying to sleep -> \n" + e) ;
						}						

						play() ;
					}) ;					
				}) ;
			}
			catch(Exception e)
			{
				System.out.println("\nException in run() -> \n" + e) ;
			}
		}
	}
}
