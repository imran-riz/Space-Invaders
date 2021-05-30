import javafx.scene.layout.* ;
import javafx.scene.shape.*;
import javafx.scene.paint.*;

public class Laser
{
	private double x, y, width, height ;
	private String dir ;
	private Rectangle rect ;
	private Pane pane ;

	public Laser(double tempX, double tempY, double tempWidth, double tempHeight, Pane tempPane)
	{
		this.x = tempX ;
		this.y = tempY ;
		this.width = tempWidth ;
		this.height = tempHeight ;
		this.pane = tempPane ;

		rect = new Rectangle() ;
		rect.setX(this.x) ;
		rect.setY(this.y) ;
		rect.setWidth(this.width) ;
		rect.setHeight(this.height) ;
		rect.setFill(Color.YELLOW) ;

		this.pane.getChildren().add(rect) ;
	}

	public void destroy()
	{
		this.pane.getChildren().remove(this.rect) ;
	}

	public BoundingBox getBoundingBox()
	{
		return new BoundingBox(this.x, this.y, this.width, this.height) ;
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

	public String getDirection()
	{
		return this.dir ;
	}

	public void setX(double tempX)
	{
		this.x = tempX ;
		rect.setX(this.x) ;		
	}

	public void setY(double tempY)
	{		
		this.y = tempY ;
		rect.setY(this.y) ;
	}

	public void setWidth(double tempWidth)
	{
		this.width = tempWidth ;
		rect.setWidth(this.width) ;		
	}

	public void setHeight(double tempHeight)
	{
		this.height = tempHeight ;
		rect.setHeight(this.height) ;
	}

	public void setDirection(String temp)
	{
		this.dir = temp.toUpperCase() ;
	}
}