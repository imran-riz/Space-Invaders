import javafx.scene.image.* ;
import javafx.scene.layout.* ;

public class Spaceship
{
	private int health ;
	private double x, y, width, height ;
	private ImageView imageView ;
	private Image image ;
	private Pane pane ;
	private String name ;
	private Boolean destroyed ;

	public Spaceship(ImageView tempImageView, double tempX, double tempY, Pane tempPane)
	{
		this.imageView = tempImageView ;

		this.width = this.imageView.getFitWidth() ;
		this.height = this.imageView.getFitHeight() ;

		this.x = tempX - (this.width/2) ;		// sets the x coordinate of the center of the entity
		this.y = tempY - (this.height/2) ;		// sets the y coordinate of the center of the entity

		this.pane = tempPane ;

		this.imageView.setLayoutX(this.x) ;
		this.imageView.setLayoutY(this.y) ;

		this.pane.getChildren().add(this.imageView) ;

		this.destroyed = false ;
	}

	public Laser fire()
	{
		double tempX = this.x + (this.width/2) ;
		double tempY = this.y ;

		Laser laser = new Laser(tempX, tempY, 3, 15, this.pane) ;

		if(this.name.equals("PLAYER"))
			laser.setDirection("UP") ;
		else
			laser.setDirection("DOWN") ;

		return laser ;
	}

	// determines if it's been hit by a laser 
	public Boolean hit(Laser laser)
	{
		// create a new BoundingBox for this entity
		BoundingBox boundingBox = getBoundingBox() ;		

		return boundingBox.isColliding(laser.getBoundingBox()) ;
	}

	public void destruct()
	{
		this.pane.getChildren().remove(this.imageView) ;
		this.destroyed = true ;
	}

	public Boolean destroyed()
	{
		return this.destroyed ;
	}

	public BoundingBox getBoundingBox()
	{
		return new BoundingBox(this.x, this.y, this.width, this.height) ;
	}

	public String getName()
	{
		return this.name ;
	}

	public Double getX()
	{
		return this.x ;
	}

	public Double getY()
	{
		return this.y ;
	}

	public Double getWidth()
	{
		return this.width ;
	}

	public Double getHeight()
	{
		return this.height ;
	}

	public Pane getPane()
	{
		return this.pane ;
	}

	public Integer getHealth()
	{
		return this.health ;
	}

	public void setName(String temp)
	{
		this.name = temp ;
	}

	public void setX(double tempX)
	{
		this.x = tempX ;
		this.imageView.setLayoutX(this.x) ;
	}

	public void setY(double tempY)
	{		
		this.y = tempY ;
		this.imageView.setLayoutY(this.y) ;
	}

	public void setWidth(double tempWidth)
	{
		this.width = tempWidth ;
		this.imageView.setFitWidth(this.width) ;
	}

	public void setHeight(double tempHeight)
	{
		this.height = tempHeight ;
		this.imageView.setFitHeight(this.height) ;
	}

	public void setHealth(int h)
	{
		this.health = h ;
	}

	public void setPane(Pane tempPane)
	{
		this.pane = tempPane ;
	}

	public void setImage(Image tempImg)
	{
		this.image = tempImg ;
		this.imageView = new ImageView(this.image) ;
		this.pane.getChildren().add(imageView) ;
	}
}