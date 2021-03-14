package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.Buffer;

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
		logger.log("applies resize with bilinear interpolation");

		//create padded image - inspired by stack overflow - https://stackoverflow.com/questions/5836203/java-padding-image
		BufferedImage paddedImage = new BufferedImage(workingImage.getWidth() + 2, workingImage.getHeight(), workingImage.getType());
		Graphics graphics = paddedImage.getGraphics();
		graphics.setColor(Color.white);
		graphics.fillRect(0, 0, workingImage.getWidth() + 2 , workingImage.getHeight() +2);
		graphics.drawImage(workingImage, 2, 2, null);
		graphics.dispose();

		BufferedImage ans = newEmptyOutputSizedImage();
		pushForEachParameters();
		setForEachOutputParameters();
		double scaleX = (double)this.inWidth/(double)this.outWidth;
		double scaleY = (double)this.inHeight/(double)this.outHeight;

		forEach((y, x) -> {
			NearestCells cells = new NearestCells(x*scaleX,y*scaleY,paddedImage);
			NearestCells.Cell topInterpolation =  cells.interpolate(cells.q_11,cells.q_12,true);
			NearestCells.Cell bottomInterpolation =  cells.interpolate(cells.q_21,cells.q_22,true);
			NearestCells.Cell interpolationResult = cells.interpolate(topInterpolation,bottomInterpolation,false);
			ans.setRGB(x,y,interpolationResult.color.getRGB());
		});

		return ans;
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

	private class NearestCells {

		Cell q_11;
		Cell q_12;
		Cell q_21;
		Cell q_22;
		double x_scaled;
		double y_scaeld;
		BufferedImage paddedImage;

		public NearestCells(double x_scaled, double y_scaled,BufferedImage paddedImage){
			this.paddedImage = paddedImage;
			this.x_scaled = x_scaled;
			this.y_scaeld = y_scaled;

			int x_left = (int) Math.floor(this.x_scaled);
			int x_right = (int) Math.ceil(this.x_scaled);
			int y_bottom = (int)Math.floor(this.y_scaeld);
			int y_top = (int)Math.ceil(this.y_scaeld);

			this.q_11 = new Cell(x_left,y_top, new Color(paddedImage.getRGB(x_left,y_top)));
			this.q_12 = new Cell(x_right,y_top, new Color(paddedImage.getRGB(x_right,y_top)));
			this.q_21 = new Cell(x_left,y_bottom, new Color(paddedImage.getRGB(x_left,y_bottom)));
			this.q_22 = new Cell(x_right,y_bottom, new Color(paddedImage.getRGB(x_right,y_bottom)));
		}

		public Cell interpolate(Cell cellOne,Cell cellTwo,boolean isHorizontal){

			Cell approximatedCell = new Cell(x_scaled,y_scaeld,new Color(0));
			double distFromCellOne = calcDistFromPoint(cellOne,approximatedCell,isHorizontal);
			double distBetweenCells = calcDistFromPoint(cellOne,cellTwo,isHorizontal);
			double t = distBetweenCells > 0? distFromCellOne/distBetweenCells:1.0;

			int red = (int)(t*cellOne.color.getRed() + (1-t)*cellTwo.color.getRed());
			int green = (int)(t*cellOne.color.getGreen() + (1-t)*cellTwo.color.getGreen());
			int blue = (int)(t*cellOne.color.getBlue() + (1-t)*cellTwo.color.getBlue());

			double weightedX = (cellOne.x_pos + cellTwo.x_pos)/2;
			double weightedY = (cellOne.y_pos + cellTwo.y_pos)/2;

			return new Cell(weightedX,weightedY,new Color(red,green,blue));
		}
		//convert to vertical or horizontal distance.
		private double calcDistFromPoint(Cell CellOne,Cell cellTwo,boolean isHorizontal){
			return isHorizontal? CellOne.x_pos - cellTwo.x_pos :  CellOne.y_pos- cellTwo.y_pos;
		}

		private class Cell{

			double x_pos;
			double y_pos;
			Color color;

			public Cell(double x_pos,double y_pos,Color color) {

				this.x_pos = x_pos;
				this.y_pos = y_pos;
				this.color = color;
			}
		}


	}
}
