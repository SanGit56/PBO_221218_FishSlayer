package application;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.event.EventHandler;

public class FishSlayer extends Application{
//	variables
	private static final Random RAND = new Random();
	private static final int WIDTH = 800;
	private static final int HEIGHT = 600;
	private static final int PLAYER_SIZE = 60;
	
	static final int EXPLOSION_W = 128;
	static final int EXPLOSION_H = 128;
	static final int EXPLOSION_ROWS = 3;
	static final int EXPLOSION_COL = 3;
	static final int EXPLOSION_STEPS = 15;
	

	static final Image PLAYER_IMG = new Image("file:src/application/images/player.png");
	static final Image CAUGHT_IMG = new Image("file:src/application/images/caught.png");
	static final Image BONUSFISH_IMG = new Image("file:src/application/images/bonus.png");
	static final Image EXPLOSION_IMG = new Image("file:src/application/images/explosion.png");
	
	static final Image FISHES_IMG[] = {
		new Image("file:src/application/images/1.png"),
		new Image("file:src/application/images/2.png"),
		new Image("file:src/application/images/3.png"),
		new Image("file:src/application/images/4.png"),
		new Image("file:src/application/images/5.png"),
		new Image("file:src/application/images/6.png"),
		new Image("file:src/application/images/7.png"),
		new Image("file:src/application/images/8.png"),
		new Image("file:src/application/images/9.png"),
		new Image("file:src/application/images/10.png")
	};
	
	final int MAX_FISHES = 5;
	final int MAX_SHOTS = MAX_FISHES * 2;
	boolean gameOver = false;
	private GraphicsContext gc;
	
	Ship player;
	Fish bonusFish;
	List<Net> nets;
	List<Ocean> oceans;
	List<Fish> fishes;
	
	private double mouseX;
	private int score;
	private int health;
	private int highScore;
	private int limit;
	private int scoreThen;
    private int level;
	private File highScoreFile = new File("highscore.txt");
	boolean shipDestroyed = false;

//	start canvas
	public void start(Stage stage) throws Exception {
		Canvas canvas = new Canvas(WIDTH, HEIGHT);	
		gc = canvas.getGraphicsContext2D();

		Timeline timeline = new Timeline(new KeyFrame(Duration.millis(100), e -> run(gc)));
		timeline.setCycleCount(Timeline.INDEFINITE);
		timeline.play();

		canvas.setCursor(Cursor.MOVE);
		canvas.setOnMouseMoved(e -> mouseX = e.getX());

		canvas.setOnMouseClicked(e -> {
			if(nets.size() < MAX_SHOTS) 
				nets.add(player.shoot());
			
			if(gameOver) { 
				gameOver = false;
				setup();
			}
		});

		setup();
			
		stage.setScene(new Scene(new StackPane(canvas)));
		stage.setTitle("Fish Slayer");
		stage.show();
	}
	
//	initialize variables value
	private void setup() {
		oceans = new ArrayList<>();
		nets = new ArrayList<>();
		fishes = new ArrayList<>();
		player = new Ship(WIDTH / 2, HEIGHT - PLAYER_SIZE, PLAYER_SIZE, PLAYER_IMG);
		IntStream.range(0, MAX_FISHES).mapToObj(i -> this.newFish()).forEach(fishes::add);
		
		score = 0;
		health = 100;
		highScore = 0;
		limit = 1;
		scoreThen = 0;
        level = 1;
		
//		read highscore file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(highScoreFile));
            String line = reader.readLine();
            
            while (line != null) {
                try {
                    int score = Integer.parseInt(line.trim());
                    
                    if (score > highScore)
                    	highScore = score;
                } catch (NumberFormatException e1) {
                    System.err.println("Ignore invalid score: " + line);
                }
                
                line = reader.readLine();
            }
            
            reader.close();
        } catch (IOException ex) {
            System.err.println("Error when reading from file");
        }
	}
	
//	run graphics (frame)
  	private void run(GraphicsContext gc) {
		gc.setFill(Color.ROYALBLUE);
		gc.fillRect(0, 0, WIDTH, HEIGHT);
		gc.setTextAlign(TextAlignment.LEFT);
		gc.setFont(Font.font(20));
		gc.setFill(Color.WHITE);
		gc.fillText("Score: " + score, 5,  20);
		gc.fillText("Level: " + level, 5,  40);
		gc.fillText("High Score: " + highScore, 5,  60);
		gc.fillText("Health: " + health + " %", 670,  20);

  		if(gameOver) {
  			gc.setTextAlign(TextAlignment.CENTER);
  			gc.setFont(Font.font(35));
  			gc.setFill(Color.YELLOW);
  			gc.fillText("Game Over\nYour Score is: " + score + "\nYour Level is: "+ level + "\nClick to play again", WIDTH/2, HEIGHT/2.5);
  			
//  		write score to file every game over
            if (limit == 1) {
                try {
                    BufferedWriter output = new BufferedWriter(new FileWriter(highScoreFile, true));
                    output.newLine();
                    output.append("" + score);
                    output.close();

                } catch (IOException ex1) {
                    System.out.printf("Error when writing to file: %s\n", ex1);
                }
                
                limit--;
            }
  		}
  		
  		oceans.forEach(Ocean::draw);
		
  		player.update();
		player.draw();
		player.posX = (int) mouseX - 35;

		fishes.stream().peek(Ship::update).peek(Ship::draw).forEach(e ->{
			for (Fish fish : fishes) {
				if(player.collide(fish) && !fish.exploding && !player.exploding) {
					fish.explode();
					gameOver = false;
					if(health>0){
						Math.max(0, health-=20);
	                		}
				}
			}
		});
			
		for(int i = nets.size() - 1; i >= 0 ; i--) {
			Net net = nets.get(i);
			
			if(net.posY < 0 || net.toRemove) {
				nets.remove(i);
				continue;
			}
			
			net.update();
			net.draw();
			
			for(Fish fish : fishes) {
				if(net.collide(fish) && !fish.exploding) {
					score++;
					fish.explode();
					net.toRemove = true;
					if(score % 20 == 0){
						level++;
					}
				}
			}
			
			if (bonusFish != null) {
				if(net.collide(bonusFish) && !bonusFish.exploding) {
					score += 3;
					bonusFish.explode();
					net.toRemove = true;
				}
			}
		}
			
		for(int i = fishes.size() - 1; i>=0; i--) {
			if(fishes.get(i).destroyed) {
				fishes.set(i,newFish());
			}
		}
		
		if(RAND.nextInt(10) > 2) {
			oceans.add(new Ocean());
		}
		
		for(int i = 0; i< oceans.size(); i++) {
			if(oceans.get(i).posY > HEIGHT)
				oceans.remove(i);
		}
		
		if (score > 0 && score % 10 == 0) {
			if (!(scoreThen == score)) {
				bonusFish = newBonusFish();
				scoreThen = score;
			}
		}
		
		if (bonusFish != null) {
			if(player.collide(bonusFish) && !player.exploding && !bonusFish.exploding) {
                		bonusFish.explode();
				if(health>0){
					Math.max(0, health-=60);
                		}
			}
			bonusFish.update();
			bonusFish.draw();
		}
		if(health <= 0) {
			shipDestroyed = true;
			player.explode();
        		player.destroyed = true;
        		gameOver = player.destroyed;
        	}
	}
	
//	player
	public class Ship {
		int posX, posY, size;
		Image img;
		boolean exploding, destroyed;
		int explosionStep = 0;
		
		public Ship(int posX, int posY, int size, Image image) {
			this.posX = posX;
			this.posY = posY;
			this.size = size;
			img = image;
		}
		
		public Net shoot() {
			return new Net(posX+size / 2 - Net.size / 2, posY - Net.size);
		}
		
		public void update() {
			if (exploding) explosionStep++;
			destroyed = explosionStep > EXPLOSION_STEPS;
		}
		
		public void draw() {
			if (exploding) {
				if (shipDestroyed) {
					gc.drawImage(EXPLOSION_IMG, explosionStep % EXPLOSION_COL * EXPLOSION_W, 
							(explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1, EXPLOSION_W, 
							EXPLOSION_H, posX, posY, size, size);
				} else {
					gc.drawImage(CAUGHT_IMG, explosionStep % EXPLOSION_COL * EXPLOSION_W, 
							(explosionStep / EXPLOSION_ROWS) * EXPLOSION_H + 1, EXPLOSION_W, 
							EXPLOSION_H, posX, posY, size, size);
				}
			} else gc.drawImage(img, posX, posY, size, size);
		}
		
		public boolean collide(Ship other) {
			int d = distance(this.posX + size / 2, this.posY + size / 2, 
					other.posX + other.size / 2, other.posY + other.size / 2);
			return d < other.size / 2 + this.size / 2;
		}
		
		public void explode() {
			exploding = true;
			explosionStep = -1;
		}
	}
	
	public class Fish extends Ship {
		int fishSpeed;
		
		public Fish (int posX, int posY, int size, Image image, int speed) {
			super(posX, posY, size, image);
			fishSpeed = speed;
		}
		
		public void update() {
			super.update();
			if(!exploding && !destroyed) posY += fishSpeed;
			if(posY > HEIGHT) destroyed = true;
		}
	}
	
	public class Net {
		public boolean toRemove;
		Image netImg = new Image("file:src/application/images/net.png");
		Image hookImg = new Image("file:src/application/images/hook.png");
		
		int posX = 10;
		int posY = 10;
		int speed = 30;
		
		static final int size = 6;
		
		public Net (int posX, int posY) {
			this.posX = posX;
			this.posY = posY;
		}
		
		public void update() {
			posY -= speed;
		}
		
		public void draw() {
			if((level % 2)==0 ){
				if(score >= 40 && score <= 80){
					speed = 50;
					gc.drawImage(netImg, posX, posY,size+40,size+40);
				}
				gc.drawImage(netImg, posX, posY,size+40,size+40);
			} 
			else {
				gc.drawImage(hookImg, posX, posY,size+25,size+25);
			}
		}
		
		public boolean collide(Ship other) {
			int distance = distance(this.posX + size / 2, this.posY + size / 2,
					       other.posX + other.size / 2, other.posY + other.size / 2);
			return distance < other.size / 2 + size / 2;
		}
	}
	
	public class Ocean {
		int posX;
		int posY;
		private int w;
		private int h;
		private int r;
		private int g;
		private int b;
		private double opacity;
		
		public Ocean() {
			posX = RAND.nextInt(WIDTH);
			posY = 0;
			
			w = RAND.nextInt(5) + 1;
			h =  RAND.nextInt(5) + 1;
			r = RAND.nextInt(100) + 150;
			g = RAND.nextInt(100) + 150;
			b = RAND.nextInt(100) + 150;
			
			opacity = RAND.nextFloat();
			if(opacity < 0) opacity *= -1;
			if(opacity > 0.5) opacity = 0.5;
		}
		
		public void draw() {
			if(opacity > 0.8) opacity -= 0.01;
			if(opacity < 0.1) opacity += 0.01;
			gc.setFill(Color.rgb(r, g, b, opacity));
			gc.fillOval(posX, posY, w, h);
			posY += 20;
		}
	}
	
	Fish newFish() {
		return new Fish(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE, 
				FISHES_IMG[RAND.nextInt(FISHES_IMG.length)], (score/5)+2);
		
	}
    
    Fish newBonusFish() {
		return new Fish(50 + RAND.nextInt(WIDTH - 100), 0, PLAYER_SIZE+75, BONUSFISH_IMG, 20);
	}
	
	int distance (int x1, int y1, int x2, int y2) {
		return (int) Math.sqrt(Math.pow(x1 - x2, 2) + Math.pow((y1 - y2), 2));
	}
	
	public static void main(String[] args) {
		launch();
	}
}
