


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ColorHistEq {

	// number of bins to divide the brightness pixel values
	static int bin = 256;
    //Use these labels to instantiate you timers.  You will need 8 invocations of now()
	static String[] labels = { "getRGB", "convert to HSB", "create brightness map", "probability array",
			"parallel prefix", "equalize pixels", "setRGB" };

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer times = new Timer(labels);
		
		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		
		times.now();
		// get the RGB pixel values of the original image using the standard Java getRGB method 
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		
		times.now();
		// convert RGB to HSB and store the hue, saturation, and brightness values in separate arrays
		double[] hsbpixelHueArray = Arrays.stream(sourcePixelArray).
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[0]).toArray();
		double[] hsbpixelSaturationArray = Arrays.stream(sourcePixelArray).
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[1]).toArray();
		double[] hsbpixelBrightnessArray = Arrays.stream(sourcePixelArray).
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[2]).toArray();
		
		times.now();
		// generate the histogram of brightness and count the number of brightness values in a particular bin
		// each bin is denoted by the key of the following map 
		Map<Double, Integer> brightnessBinMap = brightnessHistogramGeneration(Arrays.
				stream(hsbpixelBrightnessArray).boxed(), bin);
	    Map<Double, Integer> keySortedBrightnessMap = new LinkedHashMap<Double, Integer>();
	    // sort the map based on the keys  
	    brightnessBinMap.entrySet().stream().sorted(Map.Entry.<Double, Integer>comparingByKey()).
	    	forEach(item -> keySortedBrightnessMap.put(item.getKey(), item.getValue()));
	    
	    times.now();
	    //Integer[] keySortedBrightnessArray = Arrays.copyOf(keySortedBrightnessMap.values().toArray(), keySortedBrightnessMap.values().toArray().length, Integer[].class);
	    // compute the parallel prefix sum of the brightness counts  
	    List<Integer> intermediateBrightnessList = keySortedBrightnessMap.entrySet().stream().map(x -> x.getValue()).collect(Collectors.toList());
	    Integer[] keySortedBrightnessArray = intermediateBrightnessList.stream().toArray(Integer[]::new);
	    Arrays.parallelPrefix(keySortedBrightnessArray, (x, y) -> x+y );
	    
	    times.now();
	    // calculate the probability array of the brightness values
	    Integer highestElement = keySortedBrightnessArray[keySortedBrightnessArray.length-1];
	    double[] newHsbPixelBrightnessArray = Arrays.stream(Arrays.stream(keySortedBrightnessArray).
	    		mapToInt(Integer::intValue).toArray()).asDoubleStream().map(element -> element/highestElement).toArray();
	    
	    times.now();
	    // set the new brightness value and convert it to RGB pixel value from HSB
	    hsbpixelBrightnessArray = Arrays.stream(hsbpixelBrightnessArray).
	    		map(i -> getUpdatedPixel(i, newHsbPixelBrightnessArray, bin)).toArray();
	    double[][] hsbArray = new double[][] {hsbpixelHueArray, hsbpixelSaturationArray, hsbpixelBrightnessArray};
		int[] finalPixelArray = IntStream.range(0, hsbArray[0].length).
				map(i -> Color.HSBtoRGB((float) hsbArray[0][i], (float) hsbArray[1][i], (float) hsbArray[2][i])).toArray();
				
		times.now();
		 //create a new Buffered image and set its pixels to the new pixel array
		newImage.setRGB(0, 0, w, h, finalPixelArray, 0, w);
		
		times.now();
		return times;
	}

	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer times = new Timer(labels);

		ColorModel colorModel = ColorModel.getRGBdefault();
		int w = image.getWidth();
		int h = image.getHeight();
		
		times.now();
		// get the RGB pixel values of the original image using the standard Java getRGB method 
		int[] sourcePixelArray = image.getRGB(0, 0, w, h, new int[w * h], 0, w);
		
		times.now();
		// convert RGB to HSB and store the hue, saturation, and brightness values in separate arrays
		double[] hsbpixelHueArray = Arrays.stream(sourcePixelArray).parallel().
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[0]).toArray();
		double[] hsbpixelSaturationArray = Arrays.stream(sourcePixelArray).parallel().
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[1]).toArray();
		double[] hsbpixelBrightnessArray = Arrays.stream(sourcePixelArray).parallel().
				mapToDouble(pixel -> (double) (Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), 
						colorModel.getBlue(pixel), null))[2]).toArray();
		
		times.now();
		// generate the histogram of brightness and count the number of brightness values in a particular bin
		// each bin is denoted by the key of the following map 
		Map<Double, Integer> brightnessBinMap = brightnessHistogramGeneration(Arrays.stream(hsbpixelBrightnessArray).
	    		parallel().boxed(), bin);
	    Map<Double, Integer> keySortedBrightnessMap = new LinkedHashMap<Double, Integer>();
	    // sort the map based on the keys
	    brightnessBinMap.entrySet().stream().sorted(Map.Entry.<Double, Integer>comparingByKey()).
	    	forEach(x -> keySortedBrightnessMap.put(x.getKey(), x.getValue()));
	    
	    times.now();
	    //Integer[] keySortedBrightnessArray = Arrays.copyOf(keySortedBrightnessMap.values().toArray(), keySortedBrightnessMap.values().toArray().length, Integer[].class);
	    // compute the parallel prefix sum of the brightness counts  
	    List<Integer> intermediateBrightnessList = keySortedBrightnessMap.entrySet().stream().parallel().
	    		map(x -> x.getValue()).collect(Collectors.toList());
	    Integer[] keySortedBrightnessArray = intermediateBrightnessList.stream().parallel().toArray(Integer[]::new);
	    Arrays.parallelPrefix(keySortedBrightnessArray, (x, y) -> x+y );
	    
	    times.now();
	    // calculate the probability array of the brightness values
	    Integer dividend = keySortedBrightnessArray[keySortedBrightnessArray.length-1];
	    int[] intermediateHsbBrightnessArray = Arrays.stream(keySortedBrightnessArray).parallel().
	    		mapToInt(Integer::intValue).toArray();
	    double[] newHsbPixelBrightnessArray = Arrays.stream(intermediateHsbBrightnessArray).parallel().
	    		asDoubleStream().parallel().map(s -> s/dividend).toArray();
	    
	    times.now();
	    // set the new brightness value and convert it to RGB pixel value from HSB
	    hsbpixelBrightnessArray = Arrays.stream(hsbpixelBrightnessArray).parallel().
	    		map(s -> getUpdatedPixel(s, newHsbPixelBrightnessArray, bin)).toArray();
	    double[][] hsbArray = new double[][] {hsbpixelHueArray, hsbpixelSaturationArray, hsbpixelBrightnessArray};
		int[] finalPixelArray = IntStream.range(0, hsbArray[0].length).parallel().
				map(i -> Color.HSBtoRGB((float) hsbArray[0][i], (float) hsbArray[1][i], (float) hsbArray[2][i])).toArray();
				
		times.now();
		 //create a new Buffered image and set its pixels to the new pixel array
		newImage.setRGB(0, 0, w, h, finalPixelArray, 0, w);
		
		times.now();
		return times;
	}
	
	/** 
	 * This method creates a map of the bin and the number of brightness elements belonging to that
	 * bin. If no elements are present, then an entry of the corresponding bin with zero element 
	 * is created. 
	 * 
	 * @param stream the double stream denoting the brightness values
	 * @param bins the number of bins
	 * @return the map of bins and count of brightness values in that bin 
	 */
	private static Map<Double, Integer> brightnessHistogramGeneration(Stream<Double> stream, int bins) {
	    Map<Double, Integer> brightnessBinMap = (Map<Double, Integer>) stream.collect(
	        Collectors.groupingBy(element -> new ColorHistEq().getInterval(element, bins),
	            Collectors.mapping(i -> 1, Collectors.summingInt(s -> ((Integer) s).intValue()))));
	    
		Double interval = 1.0/bins;
		for(int j=1; j<= bins; j++) {
	    	if(!brightnessBinMap.containsKey(j*interval))
	    		brightnessBinMap.put(j*interval, 0);
	    }
		return brightnessBinMap;
	  }
	
	/**
	 * 
	 * Get the interval or bin of a brightness value
	 * 
	 * @param brightnessValue the brightness value of a pixel
	 * @param bins number of bins
	 * @return the particular interval or bin, in which the value belongs 
	 */
	private Double getInterval(Double brightnessValue, int bins) {
		Double interval = 1.0/bins;
		for(int j=1; j<= bins; j++){
			if(brightnessValue <= j*interval)
				return j*interval;
		}
		return null;
	}
	
	/**
	 * This method returns the new brightness value of the pixel based on the probability array value
	 * of the corresponding brightness value
	 * 
	 * @param brightnessValue the brightness value of a pixel
	 * @param newVal the probability array of the brightness pixel value of the bins
	 * @param bins number of bins
	 * @return the new brightness value 
	 */
	private static Double getUpdatedPixel(Double brightnessValue, double[] newVal, int bins) {
		Double interval = 1.0/bins;
		for(int j=1; j<= bins; j++){
			if(brightnessValue <= j*interval)
				return Double.valueOf(newVal[j-1]);
		}
		return null;
	  }
}
