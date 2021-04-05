package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
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
     LinkedList<Coordinate[]> verticalRemovedSeams;
     LinkedList<Coordinate[]> horizontalRemovedSeams;
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
        this.verticalRemovedSeams = new LinkedList<Coordinate[]>();
        this.horizontalRemovedSeams = new LinkedList<Coordinate[]>();


        for(int x = 0; x < inHeight; x++){
            for(int y = 0 ; y < inWidth; y++){
                this.currentCoordinates[x][y] = new Coordinate(x,y);
                this.parentMatrix[x][y] = new Coordinate(x,y);
            }
        }
    }

    public BufferedImage carveImage(CarvingScheme carvingScheme) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

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
                while(numberOfVerticalSeamsToCarve > 0 || numberOfHorizontalSeamsToCarve > 0) {
                    if(numberOfVerticalSeamsToCarve > 0){
                        this.greyScaleMatrix = getGreyScaleMatrix();
                        this.energyMatrix = calculateEnergy();
                        updateCostMatrixVertical();
                        removeOneVerticalSeam();
                        numberOfVerticalSeamsToCarve--;
                    }
                    if(numberOfHorizontalSeamsToCarve > 0){
                        this.greyScaleMatrix = getGreyScaleMatrix();
                        this.energyMatrix = calculateEnergy();
                        updateCostMatrixHorizontal();
                        removeOneHorizontalSeam();
                        numberOfHorizontalSeamsToCarve--;
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

    private void removeHorizontalSeams(int numberOfHorizontalSeamsToCarve){
        while(numberOfHorizontalSeamsToCarve-- > 0){
            this.greyScaleMatrix = getGreyScaleMatrix();
            this.energyMatrix = calculateEnergy();
            updateCostMatrixHorizontal();
            removeOneHorizontalSeam();
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
                double edgeCostUp = EDGE_PENALTY + costMatrix[height - 1][width];
                if (width == 0) {
                    double costTopRight = costMatrix[height - 1][width + 1];
                    double costRight = costTopRight + Math.abs(greyScaleMatrix[height - 1][width] - greyScaleMatrix[height][width + 1]);
                    if (costRight < edgeCostUp) {
                        parentMatrix[height][width] = new Coordinate(width + 1, height - 1);
                        costMatrix[height][width] += costRight;
                    } else {
                        parentMatrix[height][width] = new Coordinate(width, height - 1);
                        costMatrix[height][width] += edgeCostUp;
                    }
                }else if(width >= currentWidth - 1) {
                    double costTopLeft = costMatrix[height - 1][width - 1];
                    double costLeft = costTopLeft + Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]);
                    if (costLeft < edgeCostUp) {
                        parentMatrix[height][width] = new Coordinate(width - 1, height - 1);
                        costMatrix[height][width] += costLeft;
                    } else {
                        parentMatrix[height][width] = new Coordinate(width, height - 1);
                        costMatrix[height][width] += edgeCostUp;
                    }
                }
                else {
                    int diffRightLeft = Math.abs(greyScaleMatrix[height][width-1] - greyScaleMatrix[height][width+1]);
                    int calcDiffLeft = Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]) + diffRightLeft;
                    int calcDiffRight= Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width+1]) + diffRightLeft;

                    double costLeft = costMatrix[height - 1][width - 1] + calcDiffLeft;
                    double costRight = costMatrix[height - 1][width + 1] + calcDiffRight;
                    double costUp = costMatrix[height - 1][width] + diffRightLeft;
                    boolean upIsMin = costUp < costRight && costUp < costLeft;

                    if(upIsMin){
                        parentMatrix[height][width] = new Coordinate(width, height - 1);
                        costMatrix[height][width] += costUp;
                    }else if(costRight < costLeft){
                        parentMatrix[height][width] = new Coordinate(width + 1, height - 1);
                        costMatrix[height][width] += costRight;
                    }else{
                        parentMatrix[height][width] = new Coordinate(width - 1, height - 1);
                        costMatrix[height][width] += costLeft;
                    }
                }
            }
        });
    }

    public void updateCostMatrixHorizontal(){

        setForEachParameters(currentHeight,currentWidth);
        pushForEachParameters();
        this.costMatrix = new double[currentHeight][currentWidth];

        forEach((width,height) -> {
            if(width == 0){
                costMatrix[height][width] = calcEnergy(height,width);
            }else {
                double CostLeft = EDGE_PENALTY + costMatrix[height][width-1];
                if (height == 0) {
                    double costDown = costMatrix[height + 1][width - 1];
                    double costDownLeft = costDown + Math.abs(greyScaleMatrix[height + 1][width] - greyScaleMatrix[height][width - 1]);
                    if (costDownLeft < CostLeft) {
                        parentMatrix[height][width] = new Coordinate(width - 1, height + 1);
                        costMatrix[height][width] += costDownLeft;
                    } else {
                        parentMatrix[height][width] = new Coordinate(width-1, height);
                        costMatrix[height][width] += CostLeft;
                    }
                }else if(height >= currentHeight - 1) {
                    double costTop = costMatrix[height - 1][width - 1];
                    double costTopLeft = costTop + Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]);
                    if (costTopLeft < CostLeft) {
                        parentMatrix[height][width] = new Coordinate(width - 1, height - 1);
                        costMatrix[height][width] += costTopLeft;
                    } else {
                        parentMatrix[height][width] = new Coordinate(width-1, height);
                        costMatrix[height][width] += CostLeft;
                    }
                }
                else {
                    int diffUpDown = Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height+1][width]);
                    int calcDiffTop = Math.abs(greyScaleMatrix[height-1][width] - greyScaleMatrix[height][width-1]) + diffUpDown;
                    int calcDiffDown = Math.abs(greyScaleMatrix[height + 1][width] - greyScaleMatrix[height][width - 1]) + diffUpDown;

                    double costTop = costMatrix[height - 1][width - 1] + calcDiffTop;
                    double costDown = costMatrix[height + 1][width - 1] + calcDiffDown;
                    double costLeft = costMatrix[height][width-1] + diffUpDown;
                    boolean leftIsMin = costLeft < costDown && costLeft < costTop;

                    if(leftIsMin){
                        parentMatrix[height][width] = new Coordinate(width-1, height);
                        costMatrix[height][width] += costLeft;
                    }else if(costDown < costTop){
                        parentMatrix[height][width] = new Coordinate(width - 1, height + 1);
                        costMatrix[height][width] += costDown;
                    }else{
                        parentMatrix[height][width] = new Coordinate(width - 1, height - 1);
                        costMatrix[height][width] += costTop;
                    }
                }
            }
        });
    }

    public int[][] getGreyScaleMatrix(){

        setForEachParameters(currentWidth,currentHeight);
        int[][] tempMatrix = new int[currentHeight][currentWidth];
        setForEachParameters(currentWidth,currentHeight);
        pushForEachParameters();
        forEach((height, width) -> {
            Color c = new Color(this.seamCarvedImage.getRGB(width,height));
            tempMatrix[height][width]= CalculateGreyColor(c.getRed(),c.getGreen(),c.getBlue());
        });
        popForEachParameters();
        return tempMatrix;
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

        BufferedImage carvedImage = newEmptyImage(currentWidth, currentHeight);
        double minSeamValue = costMatrix[currentHeight-1][0];
        int seamEndPoint = currentWidth-1;

        for (int i = 0; i < currentWidth; i++) {
            if (costMatrix[currentHeight-1][i] <= minSeamValue) {
                seamEndPoint = i;
                minSeamValue = costMatrix[currentHeight-1][i];
            }
        }

        Coordinate[] seamToRemove = new Coordinate[currentHeight];
        int x = seamEndPoint;
        seamToRemove[currentHeight - 1] = new Coordinate(x, currentHeight - 1);
        for (int i = currentHeight - 1; i > 0; i--) {
            seamToRemove[i - 1] = new Coordinate(parentMatrix[i][x].X, i - 1);
        }

        this.currentWidth--;

        for (int i = 0; i < currentHeight; i++) {
            for (int j = 0; j < currentWidth; j++) {
                if (j < seamToRemove[i].X) {
                    carvedImage.setRGB(j, i, seamCarvedImage.getRGB(j, i));
                } else {
                    carvedImage.setRGB(j, i, seamCarvedImage.getRGB(j + 1, i));
                    currentCoordinates[i][j] = currentCoordinates[i][j + 1];
                }
            }
        }

        verticalRemovedSeams.add(seamToRemove);
        this.seamCarvedImage = carvedImage;
    }


    private void removeOneHorizontalSeam() {

        BufferedImage carvedImage = newEmptyImage(currentWidth, currentHeight);
        double minSeamValue = costMatrix[0][currentWidth-1];
        int seamEndPoint = currentWidth-1;

        for (int i = 0; i < currentHeight; i++) {
            if (costMatrix[i][currentWidth-1] <= minSeamValue) {
                seamEndPoint = i;
                minSeamValue = costMatrix[i][currentWidth-1];
            }
        }

        Coordinate[] seamToRemove = new Coordinate[currentWidth];
        seamToRemove[currentWidth - 1] = new Coordinate(currentWidth-1, seamEndPoint);
        for (int i = currentWidth - 1; i > 0; i--) {
            seamToRemove[i - 1] = new Coordinate(i-1, parentMatrix[seamEndPoint][i].Y);
        }

        this.currentHeight--;

        for (int i = 0; i < currentWidth; i++) {
            for (int j = 0; j < currentHeight; j++) {
                if (j < seamToRemove[i].Y) {
                    carvedImage.setRGB(i, j, seamCarvedImage.getRGB(i, j));
                } else {
                    carvedImage.setRGB(i, j, seamCarvedImage.getRGB(i, j+1));
                    currentCoordinates[j][i] = currentCoordinates[j+1][i];
                }
            }
        }

        horizontalRemovedSeams.add(seamToRemove);
        this.seamCarvedImage = carvedImage;
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

        double squaredSum = diffVertical * diffVertical + diffHorizontal * diffHorizontal;
        return (int) Math.sqrt(squaredSum/2.0);

    }

    public BufferedImage showSeams(boolean showVerticalSeams, int seamColorRGB) {
        int numberOfVerticalSeamsToCarve = Math.abs(this.outWidth - this.inWidth);
        int numberOfHorizontalSeamsToCarve = Math.abs(this.outHeight - this.inHeight);

        carveImage(CarvingScheme.VERTICAL_HORIZONTAL);
        setForEachParameters(inWidth,inHeight);
        BufferedImage ans = duplicateWorkingImage();

        LinkedList<Coordinate[]> removedSeams;

        if(showVerticalSeams){
            removedSeams = verticalRemovedSeams;
        }else{
            removedSeams = horizontalRemovedSeams;
        }

        for(Coordinate[] coordinateArray:removedSeams){
            for(Coordinate coordinate: coordinateArray){
                ans.setRGB(coordinate.X,coordinate.Y,seamColorRGB);
            }
        }
        return ans;
    }
}
