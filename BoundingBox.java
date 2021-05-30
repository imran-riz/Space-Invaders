public class BoundingBox
{
	public double minX, maxX, minY, maxY ;
	private double width, height ;

	public BoundingBox(double x, double y, double w, double h)
	{
		this.minX = x ;
		this.minY = y ;
		this.width = w ;
		this.height = h ;
		this.maxX = x + w ;
		this.maxY = y + h ;
	}


	// checks if this Bounding Box makes contact with the Bounding Box received in the parameter
	public Boolean isColliding(BoundingBox other)
	{
		if(this.maxX >= other.minX && this.minX <= other.maxX && this.maxY >= other.minY && this.minY <= other.maxY)
			return true ;

		return false ;
	}

	public Double getMinX()
	{
		return this.minX ;
	}

	public Double getMinY()
	{
		return this.minY ;
	}

	public Double getMaxX()
	{
		return this.maxX ;
	}

	public Double getMaxY()
	{
		return this.maxY ;
	}
}