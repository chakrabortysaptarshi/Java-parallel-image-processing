
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;

import java.util.concurrent.RecursiveAction;

/**
 * @author ssapt
 *
 */
public class FJBufferedImage extends BufferedImage {
	
   /**Constructors*/
	// threshold till which the image will be split further 
	private static int threshold = 2;
	
	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
			Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}
	

	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source){
	       Hashtable<String,Object> properties=null; 
	      
	       String[] propertyNames = source.getPropertyNames();
	       if (propertyNames != null) {
	    	   properties = new Hashtable<String,Object>();
	    	   for (String name: propertyNames){properties.put(name, source.getProperty(name));}
	    	   }
	 	   return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(), properties);		
	}
	
	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
        /****THIS SETRGB METHOD USES PARALLEL DIVIDE AND CONQUER*****/
		if(h-yStart <= threshold)
			super.setRGB(xStart, yStart, w, h, rgbArray, yStart*scansize, scansize);
		else {
			FJSetRgb fjUp = new FJSetRgb(xStart, yStart, w, h/2, rgbArray, offset, scansize, this);
			fjUp.fork();
			FJSetRgb fjDown = new FJSetRgb(xStart, h/2, w, h-h/2, rgbArray, (h/2)*scansize, scansize, this);
			fjDown.fork();
			fjUp.join();
			fjDown.join();
		}
	}
	

	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
	       /****THIS GETRGB METHOD USES PARALLEL DIVIDE AND CONQUER*****/		
		if(h-yStart <= threshold)
			super.getRGB(xStart, yStart, w, h, rgbArray, yStart*scansize, scansize);
		else {
			FJGetRgb fjUp = new FJGetRgb(xStart, yStart, w, h/2, rgbArray, offset, scansize, this);
			fjUp.invoke();
			FJGetRgb fjDown = new FJGetRgb(xStart, h/2, w, h-h/2, rgbArray, (h/2)*scansize, scansize, this);
			fjDown.invoke();
			fjUp.join();
			fjDown.join();
		}
		return rgbArray;
	}
}

//This class extends the fork-join framework and calls the setRGB method in parallel  
class FJSetRgb extends RecursiveAction {
	private static final long serialVersionUID = 1L;
	int xStart, yStart, w, h, offset, scansize;
	int[] rgbArray;
	FJBufferedImage fjb;
	
	public FJSetRgb(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize, FJBufferedImage fbj) {
		this.xStart = xStart;
		this.yStart = yStart;
		this.w = w;
		this.h = h;
		this.rgbArray = rgbArray;
		this.offset = offset;
		this.scansize = scansize;
		this.fjb = fbj;
	}
	
	@Override
	protected void compute() {
		fjb.setRGB(xStart, yStart, w, h, rgbArray, offset, scansize);
	}
}


// This class extends the fork-join framework and calls the getRGB method in parallel  
class FJGetRgb extends RecursiveAction {
	private static final long serialVersionUID = 1L;
	int xStart, yStart, w, h, offset, scansize;
	int[] rgbArray;
	FJBufferedImage fjb;
	
	public FJGetRgb(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize, FJBufferedImage fbj) {
		this.xStart = xStart;
		this.yStart = yStart;
		this.w = w;
		this.h = h;
		this.rgbArray = rgbArray;
		this.offset = offset;
		this.scansize = scansize;
		this.fjb = fbj;
	}
	
	@Override
	protected void compute() {
		fjb.getRGB(xStart, yStart, w, h, rgbArray, offset, scansize);
	}
}
