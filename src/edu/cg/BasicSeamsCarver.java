package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.LinkedList;


public class BasicSeamsCarver extends ImageProcessor {

	// An enum describing the carving scheme used by the seams carver.
	// VERTICAL_HORIZONTAL means vertical seams are removed first.
	// HORIZONTAL_VERTICAL means horizontal seams are removed first.
	// INTERMITTENT means seams are removed intermittently : vertical, horizontal, vertical, horizontal etc.
	public static enum CarvingScheme {
		VERTICAL_HORIZONTAL("Vertical seams first"),
		HORIZONTAL_VERTICAL("Horizontal seams first"),
		INTERMITTENT("Intermittent carving");
		
		public final String description;
		
		private CarvingScheme(String description) {
			this.description = description;
		}
	}
	
	// A simple coordinate class which assists the implementation.
	protected static class Coordinate{
		public int X;
		public int Y;
		public int metric_X;
		public int metric_Y;
		public Coordinate(int X, int Y,int metric_X,int metric_Y) {
			this.X = X;
			this.Y = Y;
			this.metric_X = metric_X;
			this.metric_Y = metric_Y;
		}
	}
	
	// TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework 
			// instructions PDF to make an educated decision.

//	BufferedImage greyScaledImage;
	double totalDiff = 0;
	int countDiffs;
	double avgDiff = 5;
	BufferedImage seamCarvedImage;
	DPMetric DPMatrix;
	LinkedList<Coordinate[]> removedSeams;
	
	public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
			int outWidth, int outHeight, RGBWeights rgbWeights) {
		super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
		// TODO : Include some additional initialization procedures.

//		this.greyScaledImage = this.greyscale();
		this.seamCarvedImage = new BufferedImage(workingImage.getWidth(),workingImage.getHeight(),workingImage.getType());
		pushForEachParameters();
		setForEachParameters(workingImage.getWidth(),workingImage.getHeight());
		this.removedSeams = new LinkedList<>();
		try {
			forEach((y, x) -> seamCarvedImage.setRGB(x, y, workingImage.getRGB(x, y)));
		}catch(Exception exception){
			logger.log("ok");
		}
		popForEachParameters();

		this.DPMatrix = new DPMetric(workingImage.getWidth(),workingImage.getHeight());

	}


	public BufferedImage carveImage(CarvingScheme carvingScheme) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

		if(carvingScheme.equals(CarvingScheme.HORIZONTAL_VERTICAL)){
			CarveMultipleVerticalSeams(numberOfVerticalSeamsToCarve);
//			CarveMultipleHorizontalSeams(numberOfHorizontalSeamsToCarve);
		}
		else if(carvingScheme.equals(CarvingScheme.VERTICAL_HORIZONTAL)){

			CarveMultipleHorizontalSeams(numberOfHorizontalSeamsToCarve);
			CarveMultipleVerticalSeams(numberOfVerticalSeamsToCarve);
		}
		else if(carvingScheme.equals(CarvingScheme.INTERMITTENT)){

			CarveIntermittentSeams(numberOfVerticalSeamsToCarve,numberOfHorizontalSeamsToCarve);
		}else{
			throw new EnumConstantNotPresentException(CarvingScheme.class,carvingScheme.toString());
		}

		return this.seamCarvedImage;
	}

	private void CarveMultipleVerticalSeams(int numberOfVerticalSeamsToCarve){

		while(numberOfVerticalSeamsToCarve-- > 0){
			CarveImageVerticalSeam();
		}
	}

	private void CarveMultipleHorizontalSeams(int numberOfHorizontalSeamsToCarve){

		while(numberOfHorizontalSeamsToCarve-- > 0){
			CarveImageHorizontalSeam();
		}
	}

	public void CarveImageVerticalSeam(){
		DPMatrix.updateCostVertical();
		Coordinate[] seamCoordinates = DPMatrix.getVerticalSeam();
		removedSeams.add(seamCoordinates);
		BufferedImage tempEditedImage = new BufferedImage(this.seamCarvedImage.getWidth()-1,this.seamCarvedImage.getHeight(),this.seamCarvedImage.getType());

		pushForEachParameters();
		setForEachParameters(tempEditedImage.getWidth(),tempEditedImage.getHeight());
		try {
			forEach((y, x) -> {
				int destLocationX = x;
				//			TODO - outX instead
				if (x > seamCoordinates[y].metric_X) {
					destLocationX = destLocationX - 1;
				}
				if (seamCoordinates[y].metric_X != x) {
					tempEditedImage.setRGB(destLocationX, y, this.seamCarvedImage.getRGB(x, y));
				}
			});
		}catch(Exception exception){
			logger.log("ok");
		}
		popForEachParameters();

		this.seamCarvedImage = tempEditedImage;
		DPMatrix.removeVerticalSeam(seamCoordinates);
		DPMatrix.updateCells(seamCoordinates);
		setForEachWidth(seamCarvedImage.getWidth());
	}

	public void CarveIntermittentSeams(int numberOfVerticalSeamsToCarve, int numberOfHorizontalSeamsToCarve){
		while(numberOfHorizontalSeamsToCarve-- > 0 && numberOfVerticalSeamsToCarve-- > 0){
			CarveImageVerticalSeam();
			CarveImageHorizontalSeam();
		}

		while (numberOfHorizontalSeamsToCarve-- > 0 || numberOfVerticalSeamsToCarve-- > 0){
			if (numberOfHorizontalSeamsToCarve > 0) {
				CarveImageHorizontalSeam();
			} else {
				CarveImageVerticalSeam();
			}
		}
	}

	public BufferedImage CarveImageHorizontalSeam(){
		return this.workingImage;
	}

	public BufferedImage CarveImageIntermittent(int numberOfVerticalSeamsToCarve, int numberOfHorizontalSeamsToCarve){

		return this.workingImage;
	}


	public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
		int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
		int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
		// TODO :  Present either vertical or horizontal seams on the input image.
				// If showVerticalSeams = true, carve 'numberOfVerticalSeamsToCarve' vertical seams from the image.
				// Then, generate a new image from the input image in which you mark all of the vertical seams that
				// were chosen in the Seam Carving process. 
				// This is done by painting each pixel in each seam with 'seamColorRGB' (overriding its' previous value). 
				// Similarly, if showVerticalSeams = false, carve 'numberOfHorizontalSeamsToCarve' horizontal seams
				// from the image.
				// Then, generate a new image from the input image in which you mark all of the horizontal seams that
				// were chosen in the Seam Carving process.
		BufferedImage ans = new BufferedImage(this.inWidth,this.inHeight,workingImageType);
		pushForEachParameters();
		setForEachParameters(this.inWidth,this.inHeight);
		HashMap<Integer,Coordinate> testDups = new HashMap<>();
		try {
			forEach((y,x) -> ans.setRGB(x,y,workingImage.getRGB(x,y)));
			popForEachParameters();

				if (showVerticalSeams) {
					CarveMultipleVerticalSeams(numberOfVerticalSeamsToCarve);
					int count = 0;
					for (Coordinate[] carvedSeam : removedSeams) {
						for (Coordinate coordinate : carvedSeam) {
							ans.setRGB(coordinate.X, coordinate.Y, seamColorRGB);
							count++;
							if(testDups.containsKey(coordinate.X * coordinate.Y + coordinate.Y)){
								System.out.println("X: " + coordinate.X + " Y: " + coordinate.Y);
							}
							testDups.put(coordinate.X * coordinate.Y + coordinate.Y,coordinate);
						}
//						System.out.println("count: " + count);
						count = 0;
					}
				} else {
					CarveMultipleHorizontalSeams(numberOfHorizontalSeamsToCarve);
					for (Coordinate[] carvedSeam : removedSeams) {
						for (Coordinate coordinate : carvedSeam) {
							ans.setRGB(coordinate.X, coordinate.Y, seamColorRGB);
						}
					}
				}
		} catch(Exception exception){
			logger.log("ok");
		}
		return ans;
	}


	private class DPMetric {

		int height;
		int width;
		DPCell[][] matrix;

		public DPMetric(int width , int height) {

			this.matrix = new DPCell[width][height];
			this.height = height;
			this.width = width;

			pushForEachParameters();
			setForEachParameters(width,height);
			forEach((y, x) -> {
				int greyColorCurrent = CalculateGreyColor(seamCarvedImage,x,y);
				int energy = calculateEnergy(x,y);
				this.matrix[x][y] = new DPCell(x, y,energy,greyColorCurrent);
			});
			popForEachParameters();
		}

		public void updateCells(Coordinate[] seamCoordinates){
			try {
				for (Coordinate coordinate : seamCoordinates) {
					if (coordinate.metric_X != 0) {
						this.matrix[coordinate.metric_X - 1][coordinate.Y].energy = calculateEnergy(coordinate.metric_X - 1, coordinate.Y);
					}
					if (coordinate.metric_X < seamCarvedImage.getWidth()) {
						this.matrix[coordinate.metric_X - 1][coordinate.Y].energy = calculateEnergy(coordinate.metric_X, coordinate.Y);
					}
				}

			}
			catch(Exception exception){
				logger.log("ok");
			}
		}

		public int calculateEnergy(int x,int y) {


			int greyColorCurrent = CalculateGreyColor(seamCarvedImage,x,y);
			int greyColorHorizontal;
			int greyColorVertical;

			if (x < seamCarvedImage.getWidth() - 1) {
				greyColorHorizontal = CalculateGreyColor(seamCarvedImage,x+1,y);
			} else {
				greyColorHorizontal = CalculateGreyColor(seamCarvedImage,x-1,y);
			}

			if (y < seamCarvedImage.getHeight() - 1) {
				greyColorVertical = CalculateGreyColor(seamCarvedImage,x,y+1);
			} else {
				greyColorVertical = CalculateGreyColor(seamCarvedImage,x,y-1);
			}

			int diffVertical = greyColorCurrent - greyColorVertical;
			int diffHorizontal = greyColorCurrent - greyColorHorizontal;
			int squaredSum = diffVertical * diffVertical + diffHorizontal * diffHorizontal;
			return (int) Math.sqrt(squaredSum);
		}

		public void updateCostVertical(){
			pushForEachParameters();
			setForEachParameters(matrix.length,matrix[0].length);

			try {
				forEach((y, x) -> {

					//first raw cost is simply the cell energy
					if (y > 0) {

						DPCell topLeft = x > 0 ? matrix[x - 1][y - 1] : null;
						DPCell topRight = x < matrix.length - 1 ? matrix[x + 1][y - 1] : null;
						DPCell topVertical = matrix[x][y - 1];
						DPCell left = x > 0 ? matrix[x - 1][y] : null;
						DPCell right = x < matrix.length - 1 ? matrix[x + 1][y] : null;


						// If x is a left or right bound we only allow to continue a seam the inner pixels
						// this is to avoid seam always chosen from the bounds (that are positively biased by not creating new pixel matches)
						if (x == 0) {
							double verticalCost = topVertical.totalCost;
							double rightCost = right.totalCost + Math.abs(topVertical.intensity - right.intensity);
							matrix[x][y].updateTotalCost(Math.min(verticalCost, rightCost)+avgDiff);
							matrix[x][y].prevCell = verticalCost < rightCost? topVertical: topRight;
						} else if (x == matrix.length - 1) {
							double verticalCost = topVertical.totalCost;
							double leftCost = topLeft.totalCost + Math.abs(topVertical.intensity - left.intensity);
							matrix[x][y].updateTotalCost(Math.min(verticalCost,leftCost)+avgDiff);
							matrix[x][y].prevCell = verticalCost < leftCost? topVertical: topLeft;;
						} else {


							int leftRightDiff = Math.abs(right.intensity - left.intensity);
							int topLeftDiff = Math.abs(topVertical.intensity - left.intensity);
							int topRightDiff = Math.abs(topVertical.intensity - right.intensity);
							totalDiff += leftRightDiff;
							countDiffs++;
							double verticalCost = topVertical.totalCost + leftRightDiff;
							double topLeftCost = topLeft.totalCost + leftRightDiff + topLeftDiff;
							double topRightCost = topRight.totalCost + leftRightDiff + topRightDiff;

							boolean verticalIsMin = verticalCost < topLeftCost && verticalCost < topRightCost;
							boolean topLeftIsMin = !verticalIsMin && topLeftCost < topRightCost;

							if (verticalIsMin) {
								matrix[x][y].updateTotalCost(verticalCost);
								matrix[x][y].prevCell = topVertical;
							} else if (topLeftIsMin) {
								matrix[x][y].updateTotalCost(topLeftCost);
								matrix[x][y].prevCell = topLeft;
							} else {
								matrix[x][y].updateTotalCost(topRightCost);
								matrix[x][y].prevCell = topRight;
							}
						}
					}
				});
			}catch(Exception exception){
				logger.log("ok");
			}
			System.out.println("avg diff is :" + totalDiff/countDiffs);
			popForEachParameters();
		}

		public void removeVerticalSeam(Coordinate[] seamCoordinates){


			DPCell[][] tempMatrix = new DPCell[this.width-1][this.height];

			pushForEachParameters();
			setForEachParameters(width,height);
			try {
				forEach((y, x) -> {
					int destLocationX = x;

					if (x > seamCoordinates[y].metric_X) {
						destLocationX = destLocationX - 1;
					}

					if (x != seamCoordinates[y].metric_X) {
						tempMatrix[destLocationX][y] = this.matrix[x][y];
						tempMatrix[destLocationX][y].outLocationX = destLocationX;
					}
				});
			} catch(Exception exception){
				logger.log("ok");
			}

			popForEachParameters();
			this.width--;
			this.matrix = tempMatrix;

		}

//		return an array of the minimum seam coordinates
		public Coordinate[] getVerticalSeam() {
			int count = 0;
			Coordinate[] seamCoordinates = new Coordinate[this.height];
			DPCell currentDPCell = matrix[this.width -1][this.height -1];
			Coordinate currentCoordinate;

//			iterate over all columns to find the one with min total cost at it's bottom cell.
			for (DPCell[] dpCells : matrix) {
				if (dpCells[this.height - 1].totalCost < currentDPCell.totalCost) {
					currentDPCell = dpCells[this.height - 1];
				}
			}

//			iterate back using the pointers to retrieve the seam.
			currentCoordinate = new Coordinate(currentDPCell.inLocationX, currentDPCell.inLocationY, currentDPCell.outLocationX, currentDPCell.outLocationY);
			seamCoordinates[count++] = currentCoordinate;

			while(currentDPCell.prevCell != null){
				currentDPCell = currentDPCell.prevCell;

				currentCoordinate = new Coordinate(currentDPCell.inLocationX, currentDPCell.inLocationY, currentDPCell.outLocationX, currentDPCell.outLocationY);
				seamCoordinates[count++] = currentCoordinate;
			}

			return seamCoordinates;
		}

		 class DPCell {

			int outLocationX;
			int outLocationY;
			int inLocationX;
			int inLocationY;
			int intensity;
			int energy;
			double totalCost;

			DPCell prevCell;

			public DPCell(int x, int y, int energy,int greyScaleIntensity){
				this.outLocationX = x;
				this.outLocationY = y;
				this.inLocationX = x;
				this.inLocationY = y;
				this.intensity = greyScaleIntensity;
				this.energy = energy;
				this.totalCost = energy;
				this.prevCell = null;
			}

			public void updateTotalCost(double addedCost){
				this.totalCost = this.energy + addedCost;

			}

		}

	}

}
