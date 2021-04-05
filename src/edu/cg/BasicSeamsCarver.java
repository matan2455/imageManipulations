package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;


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
    protected class Coordinate{
        public int X;
        public int Y;
        public Coordinate(int X, int Y) {
            this.X = X;
            this.Y = Y;
        }
    }

     final int EDGE_PENALTY = 255;
     final int inWidth;
     final int inHeight;
     int currentHeight;
     int currentWidth;
     Coordinate[][] currentCoordinates;
     BufferedImage seamCarvedImage;
     int[][] greyScaleMatrix;
     int[][] energyMatrix;
     Coordinate[][] parentMatrix;
    double[][] costMatrix;

    // TODO :  Decide on the fields your BasicSeamsCarver should include. Refer to the recitation and homework
    // instructions PDF to make an educated decision.

    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);
        // TODO : Include some additional initialization procedures.
        this.inWidth = workingImage.getWidth();
        this.inHeight = workingImage.getHeight();
        this.currentWidth = workingImage.getWidth();
        this.currentHeight = workingImage.getHeight();
        this.currentCoordinates = new Coordinate[inHeight][inWidth];
        this.parentMatrix = new Coordinate[inHeight][inWidth];
        this.seamCarvedImage = duplicateWorkingImage();


        for(int x = 0 ; x < currentCoordinates.length;x++){
            for(int y = 0 ; y < currentCoordinates[0].length; y++){
                this.currentCoordinates[x][y] = new Coordinate(x,y);
                this.parentMatrix[x][y] = new Coordinate(x,y);
            }
        }
        System.out.println("ok");
    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);
        // TODO :  Perform Seam Carving. Overall you need to remove 'numberOfVerticalSeamsToCarve' vertical seams
        // and 'numberOfHorizontalSeamsToCarve' horizontal seams from the image.
        // Note you must consider the 'carvingScheme' parameter in your procedure.
        // Return the resulting image.

        switch(carvingScheme){

            case VERTICAL_HORIZONTAL:
                removeVerticalSeams(numberOfVerticalSeamsToCarve);
                removeHorizontalSeams(numberOfHorizontalSeamsToCarve);
                break;
            case HORIZONTAL_VERTICAL:
                removeHorizontalSeams(numberOfHorizontalSeamsToCarve);
                removeVerticalSeams(numberOfVerticalSeamsToCarve);
                break;
            case INTERMITTENT:
                while(numberOfVerticalSeamsToCarve-- + numberOfHorizontalSeamsToCarve-- > 0) {
                    removeHorizontalSeams(1);
                    removeVerticalSeams(1);
                    if(numberOfVerticalSeamsToCarve == 0){
                        removeHorizontalSeams(numberOfHorizontalSeamsToCarve);
                    }else if(numberOfHorizontalSeamsToCarve == 0){
                        removeVerticalSeams(numberOfHorizontalSeamsToCarve);
                    }
                }
                break;
        }

        return this.seamCarvedImage;
    }




    private void removeVerticalSeams(int numberOfVerticalSeamsToCarve){
        while(numberOfVerticalSeamsToCarve-- > 0){
            this.greyScaleMatrix = getGreyScaleMatrix();
            this.energyMatrix = calculateEnergy();
            updateCostMatrixVertical();
            removeOneVerticalSeam();
        }

    }

    private void removeHorizontalSeams(int numberOfVerticalSeamsToCarve){
        while(numberOfVerticalSeamsToCarve-- > 0){

        }

    }


    public void updateCostMatrixVertical(){

        setForEachParameters(currentWidth,currentHeight);
        pushForEachParameters();
        this.costMatrix = new double[currentHeight][currentWidth];

        forEach((height,width) -> {
            if(height == 0){
                costMatrix[height][width] = calcEnergy(height,width);
            }else {
                double edgeCostUp = EDGE_PENALTY + costMatrix[height][width - 1];
                if (width == 0) {
                    double costTopRight = costMatrix[height + 1][width - 1];
                    double costRight = costTopRight + Math.abs(greyScaleMatrix[height + 1][width] - greyScaleMatrix[height][width - 1]);
                    if (costRight < edgeCostUp) {
                        parentMatrix[height][width] = new Coordinate(height + 1, width - 1);
                        costMatrix[height][width] += costRight;
                    } else {
                        parentMatrix[height][width] = new Coordinate(height, width - 1);
                        costMatrix[height][width] += edgeCostUp;
                    }
                }else if(width >= currentWidth - 1) {
                    double costTopLeft = costMatrix[height - 1][width - 1];
                    double costLeft = costTopLeft + Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]);
                    if (costLeft < edgeCostUp) {
                        parentMatrix[height][width] = new Coordinate(height - 1, width - 1);
                        costMatrix[height][width] += costLeft;
                    } else {
                        parentMatrix[height][width] = new Coordinate(height, width - 1);
                        costMatrix[height][width] += edgeCostUp;
                    }
                }
                else {
                    int diffRightLeft = Math.abs(greyScaleMatrix[height+1][width] - greyScaleMatrix[height-1][width]);
                    int calcDiffLeft = Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]) + diffRightLeft;
                    int calcDiffRight= Math.abs(greyScaleMatrix[height+1][width] - greyScaleMatrix[height][width-1]) + diffRightLeft;

                    double costLeft = costMatrix[height - 1][width - 1] + calcDiffLeft;
                    double costRight = costMatrix[height + 1][width - 1] + calcDiffRight;
                    double costUp = costMatrix[height][width - 1] + diffRightLeft;
                    boolean upIsMin = costUp < costRight && costUp < costLeft;

                    if(upIsMin){
                        parentMatrix[height][width] = new Coordinate(height,width-1);
                        costMatrix[height][width] += costUp;
                    }else if(costRight < costLeft){
                        parentMatrix[height][width] = new Coordinate(height+1,width-1);
                        costMatrix[height][width] += costRight;
                    }else{
                        parentMatrix[height][width] = new Coordinate(height-1,width-1);
                        costMatrix[height][width] += costLeft;
                    }
                }
            }
        });
    }

    public int[][] getGreyScaleMatrix(){

        BufferedImage ans = newEmptyImage(currentWidth,currentHeight);
        setForEachParameters(currentWidth,currentHeight);
        int[][] tempMatrix = new int[currentHeight][currentWidth];
        setForEachParameters(currentWidth,currentHeight);
        pushForEachParameters();
        forEach((height, width) -> {
            Color c = new Color(this.seamCarvedImage.getRGB(width,height));
            tempMatrix[height][width]= CalculateGreyColor(c.getRed(),c.getGreen(),c.getBlue());
        });
        popForEachParameters();
        return  tempMatrix;
    }

    public int[][] calculateEnergy(){
        boolean isSeamCarved = true;
        setForEachParameters(currentWidth,currentHeight);
        pushForEachParameters();
        int[][] tempMatrix = new int[currentHeight][currentWidth];
        forEach((height,width) ->{
            tempMatrix[height][width] = calcEnergy(height,width);
        });
        popForEachParameters();
        return tempMatrix;
    }



    private void removeOneVerticalSeam() {
        //find the minimum value in the bottom of the cost matrix
        double minSeamValue = costMatrix[0][currentHeight-1];
        int minSeamIndex = 0;
        for (int i = 0; i < currentWidth; i++) {
            if (costMatrix[i][currentHeight-1] <= minSeamValue) {
                minSeamIndex = i;
                minSeamValue = costMatrix[i][currentHeight-1];
            }
        }


        Coordinate[] seam = new Coordinate[currentHeight];
        int x = minSeamIndex;
        seam[currentHeight - 1] = new Coordinate(x, currentHeight - 1);
        for (int i = currentHeight - 1; i > 0; i--) {
            x = parentMatrix[x][i].X;
            seam[i - 1] = new Coordinate(x, i - 1);
        }
        //update coordinateMatrix, imagetoCarve
        this.currentWidth--;
        BufferedImage newimage = newEmptyImage(currentWidth, currentHeight);
        for (int i = 0; i < currentHeight; i++) {
            for (int j = 0; j < currentWidth; j++) {
                if (j < seam[i].X) {
                    newimage.setRGB(j, i, seamCarvedImage.getRGB(j, i));
                } else {
                    newimage.setRGB(j, i, seamCarvedImage.getRGB(j + 1, i));
                    currentCoordinates[i][j] = currentCoordinates[i][j + 1];
                }
            }

        }
        this.seamCarvedImage = newimage;
    }

        private int calcEnergy(int height,int width){

            int diffVertical;
            int diffHorizontal;
            int greyColor = greyScaleMatrix[height][width];
            if (width < greyScaleMatrix[0].length - 1) {
                diffHorizontal = greyColor - greyScaleMatrix[height][width+1];
            } else {
                diffHorizontal = greyColor - greyScaleMatrix[height][width-1];
            }

            if (height < greyScaleMatrix.length - 1) {
                diffVertical = greyColor - greyScaleMatrix[height+1][width];
            } else {
                diffVertical = greyColor - greyScaleMatrix[height-1][width];
            }

            int squaredSum = diffVertical * diffVertical + diffHorizontal * diffHorizontal;
            return (int) Math.sqrt(squaredSum);

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
        throw new UnimplementedMethodException("showSeams");
    }
}
