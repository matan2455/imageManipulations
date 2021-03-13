package edu.cg;

import java.awt.Color;
import java.awt.image.BufferedImage;

public class ImageProcessor extends FunctioalForEachLoops {
	
	//MARK: Fields
	public final Logger logger;
	public final BufferedImage workingImage;
	public final RGBWeights rgbWeights;
	public final int inWidth;
	public final int inHeight;
	public final int workingImageType;
	public final int outWidth;
	public final int outHeight;
	public final int MAX_COLOR_VALUE = 255;
	
	//MARK: Constructors
	public ImageProcessor(Logger logger, BufferedImage workingImage,
			RGBWeights rgbWeights, int outWidth, int outHeight) {
		super(); //Initializing for each loops...
		
		this.logger = logger;
		this.workingImage = workingImage;
		this.rgbWeights = rgbWeights;
		inWidth = workingImage.getWidth();
		inHeight = workingImage.getHeight();
		workingImageType = workingImage.getType();
		this.outWidth = outWidth;
		this.outHeight = outHeight;
		setForEachInputParameters();
	}
	
	public ImageProcessor(Logger logger,
			BufferedImage workingImage,
			RGBWeights rgbWeights) {
		this(logger, workingImage, rgbWeights,
				workingImage.getWidth(), workingImage.getHeight());
	}
	
	//MARK: Change picture hue - example
	public BufferedImage changeHue() {
		logger.log("Prepareing for hue changing...");
		
		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int max = rgbWeights.maxWeight;
		
		BufferedImage ans = newEmptyInputSizedImage();
		
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			int red = r*c.getRed() / max;
			int green = g*c.getGreen() / max;
			int blue = b*c.getBlue() / max;
			Color color = new Color(red, green, blue);
			ans.setRGB(x, y, color.getRGB());
		});
		
		logger.log("Changing hue done!");
		
		return ans;
	}
	
	//MARK: Nearest neighbor - example
	public BufferedImage nearestNeighbor() {
		logger.log("applies nearest neighbor interpolation.");
		BufferedImage ans = newEmptyOutputSizedImage();
		
		pushForEachParameters();
		setForEachOutputParameters();
		
		forEach((y, x) -> {
			int imgX = (int)Math.round((x*inWidth) / ((float)outWidth));
			int imgY = (int)Math.round((y*inHeight) / ((float)outHeight));
			imgX = Math.min(imgX,  inWidth-1);
			imgY = Math.min(imgY, inHeight-1);
			ans.setRGB(x, y, workingImage.getRGB(imgX, imgY));
		});
		
		popForEachParameters();
		
		return ans;
	}
	
	//MARK: Unimplemented methods
	public BufferedImage greyscale() {
		//TODO: Implement this method, remove the exception.
		logger.log("applies greyscale interpolation");
		BufferedImage ans = newEmptyOutputSizedImage();

		int r = rgbWeights.redWeight;
		int g = rgbWeights.greenWeight;
		int b = rgbWeights.blueWeight;
		int sumWeights = rgbWeights.weightsSum;

		pushForEachParameters();
		setForEachOutputParameters();
		forEach((y, x) -> {
			Color c = new Color(workingImage.getRGB(x, y));
			float red = r*c.getRed();
			float green = g*c.getGreen();
			float blue = b*c.getBlue();
			int greyColor = (int)((red + green + blue)/sumWeights);
			ans.setRGB(x, y,new Color(greyColor,greyColor,greyColor).getRGB());
		});

		logger.log("grey scaling completed");
		return ans;
	}

	public BufferedImage gradientMagnitude() {
		//TODO: Implement this method, remove the exception.
		logger.log("applies gradient interpolation");
		BufferedImage greyScaledImage = this.greyscale();
		BufferedImage ans = newEmptyOutputSizedImage();
		pushForEachParameters();
		setForEachOutputParameters();

		forEach((y, x) -> {
			int diffVertical;
			int diffHorizontal;

			int greyColor = new Color(greyScaledImage.getRGB(x,y)).getRed();

			if(x <  greyScaledImage.getWidth() - 1) {
				diffHorizontal = greyColor - new Color(greyScaledImage.getRGB(x+1,y)).getRed();
			}else{
				diffHorizontal = greyColor - new Color(greyScaledImage.getRGB(x-1,y)).getRed();;
			}

			if(y <  greyScaledImage.getHeight() - 1) {
				diffVertical = greyColor - new Color(greyScaledImage.getRGB(x,y+1)).getRed();
			}else{
				diffVertical = greyColor - new Color(greyScaledImage.getRGB(x,y-1)).getRed();
			}

			int color = MAX_COLOR_VALUE - (int)Math.sqrt((diffVertical*diffVertical + diffHorizontal*diffHorizontal)/2.0);
			Color c = new Color(color,color,color);
			ans.setRGB(x, y,c.getRGB());
		});

		logger.log("Gradient interpolation completed:");
		return ans;
	}

	public BufferedImage bilinear() {
		//TODO: Implement this method, remove the exception.
		throw new UnimplementedMethodException("bilinear");
	}
	
	//MARK: Utilities
	public final void setForEachInputParameters() {
		setForEachParameters(inWidth, inHeight);
	}
	
	public final void setForEachOutputParameters() {
		setForEachParameters(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyInputSizedImage() {
		return newEmptyImage(inWidth, inHeight);
	}
	
	public final BufferedImage newEmptyOutputSizedImage() {
		return newEmptyImage(outWidth, outHeight);
	}
	
	public final BufferedImage newEmptyImage(int width, int height) {
		return new BufferedImage(width, height, workingImageType);
	}
	
	public final BufferedImage duplicateWorkingImage() {
		BufferedImage output = newEmptyInputSizedImage();
		
		forEach((y, x) -> 
			output.setRGB(x, y, workingImage.getRGB(x, y))
		);
		
		return output;
	}
}
