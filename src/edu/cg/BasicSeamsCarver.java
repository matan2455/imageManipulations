package edu.cg;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.LinkedList;


public class BasicSeamsCarver extends ImageProcessor {

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


    public BasicSeamsCarver(Logger logger, BufferedImage workingImage,
                            int outWidth, int outHeight, RGBWeights rgbWeights) {
        super((s) -> logger.log("Seam carving: " + s), workingImage, rgbWeights, outWidth, outHeight);

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

        if(carvingScheme == CarvingScheme.VERTICAL_HORIZONTAL) {
            removeVerticalSeams(numberOfVerticalSeamsToCarve);
            removeHorizontalSeams(numberOfHorizontalSeamsToCarve);
        } else if(carvingScheme == CarvingScheme.HORIZONTAL_VERTICAL) {
            removeHorizontalSeams(numberOfHorizontalSeamsToCarve);
            removeVerticalSeams(numberOfVerticalSeamsToCarve);
        } else {
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
                    updateCostEdgePixel(width, height, edgeCostUp, height - 1, width - 1, width, height - 1);
                }
                else {
                    updateVerticalCostMiddlePixel(height, width);
                }
            }
        });
    }

    private void updateVerticalCostMiddlePixel(Integer height, Integer width) {
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
                    updateCostEdgePixel(width, height, CostLeft, height + 1, width - 1, width-1, height);
                }else if(height >= currentHeight - 1) {
                    updateCostEdgePixel(width, height, CostLeft, height - 1, width - 1, width - 1, height);
                }
                else {
                    updateHorizontalCostMiddlePixel(width, height);
                }
            }
        });
    }

    private void updateCostEdgePixel(Integer width, Integer height, double costLeft, int i, int i2, int i3, Integer height2) {
        double costTop = costMatrix[i][i2];
        double costTopLeft = costTop + Math.abs(greyScaleMatrix[i][width] - greyScaleMatrix[height][i2]);
        if (costTopLeft < costLeft) {
            parentMatrix[height][width] = new Coordinate(i2, i);
            costMatrix[height][width] += costTopLeft;
        } else {
            parentMatrix[height][width] = new Coordinate(i3, height2);
            costMatrix[height][width] += costLeft;
        }
    }

    private void updateHorizontalCostMiddlePixel(Integer width, Integer height) {
        int diffUpDown = Math.abs(greyScaleMatrix[height -1][width] - greyScaleMatrix[height +1][width]);
        int calcDiffTop = Math.abs(greyScaleMatrix[height -1][width] - greyScaleMatrix[height][width -1]) + diffUpDown;
        int calcDiffDown = Math.abs(greyScaleMatrix[height + 1][width] - greyScaleMatrix[height][width - 1]) + diffUpDown;

        double costTop = costMatrix[height - 1][width - 1] + calcDiffTop;
        double costDown = costMatrix[height + 1][width - 1] + calcDiffDown;
        double costLeft = costMatrix[height][width -1] + diffUpDown;
        boolean leftIsMin = costLeft < costDown && costLeft < costTop;

        if(leftIsMin){
            parentMatrix[height][width] = new Coordinate(width -1, height);
            costMatrix[height][width] += costLeft;
        }else if(costDown < costTop){
            parentMatrix[height][width] = new Coordinate(width - 1, height + 1);
            costMatrix[height][width] += costDown;
        }else{
            parentMatrix[height][width] = new Coordinate(width - 1, height - 1);
            costMatrix[height][width] += costTop;
        }
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

        int seamEndPoint = getSeamEndPointVertical();
        Coordinate[] seam = getSeamForStorageVertical(seamEndPoint);
        this.currentWidth--;
        BufferedImage newimage = createNewCarvedImageVertical(seam);
        seamCarvedImage = newimage;
        verticalRemovedSeams.add(seam);
    }

    private BufferedImage createNewCarvedImageVertical(Coordinate[] seam) {
        BufferedImage newimage = newEmptyImage(currentWidth, currentHeight);
        for (int height = 0; height < currentHeight; height++) {
            for (int width = 0; width < currentWidth; width++) {
                if (width < seam[height].X) {
                    newimage.setRGB(width, height, seamCarvedImage.getRGB(width, height));
                } else {
                    newimage.setRGB(width, height, seamCarvedImage.getRGB(width + 1, height));
                    currentCoordinates[height][width] = currentCoordinates[height][width + 1];
                }
            }
        }
        return newimage;
    }


    private Coordinate[] getSeamForStorageVertical(int seamEndPoint) {
        Coordinate[] seam = new Coordinate[currentHeight];
        int width = seamEndPoint;
        seam[currentHeight - 1] = new Coordinate(width, currentHeight - 1);
        for (int height = currentHeight - 1; height > 0; height--) {
            width = parentMatrix[height][width].X;
            seam[height - 1] = new Coordinate(width, height - 1);
        }
        return seam;
    }

    private int getSeamEndPointVertical() {
        double minSeamValue = costMatrix[currentHeight-1][0];
        int seamEndPoint = 0;
        for (int width = 0; width < currentWidth; width++) {
            if (costMatrix[currentHeight-1][width] <= minSeamValue) {
                seamEndPoint = width;
                minSeamValue = costMatrix[currentHeight-1][width];
            }
        }
        return seamEndPoint;
    }


    private void removeOneHorizontalSeam() {

        int seamEndPoint = getSeamEndPointHorizontal();
        this.currentHeight--;
        BufferedImage newimage = newEmptyImage(currentWidth, currentHeight);
        Coordinate[] seam = getSeamHorizontal(seamEndPoint);
        updateImageHorizontal(newimage, seam);

        horizontalRemovedSeams.add(seam);

    }

    private void updateImageHorizontal(BufferedImage newimage, Coordinate[] seam) {
        for (int width = 0; width < currentWidth; width++) {
            for (int height = 0; height < currentHeight; height++) {
                if (height < seam[width].Y) {
                    newimage.setRGB(width, height, seamCarvedImage.getRGB(width, height));
                } else {
                    currentCoordinates[height][width] = currentCoordinates[height+1][width];
                    newimage.setRGB(width, height, seamCarvedImage.getRGB(width, height+1));
                }
            }
        }
        this.seamCarvedImage = newimage;
    }

    private int getSeamEndPointHorizontal() {
        double minSeamValue = costMatrix[0][currentWidth-1];
        int seamEndPoint = 0;
        for (int height = 0; height < currentHeight; height++) {
            if (costMatrix[height][currentWidth-1] < minSeamValue) {
                minSeamValue = costMatrix[height][currentWidth-1];
                seamEndPoint = height;
            }
        }
        return seamEndPoint;
    }

    private Coordinate[] getSeamHorizontal(int seamEndPoint) {
        Coordinate[] seam = new Coordinate[currentWidth];
        int height = seamEndPoint;
        seam[currentWidth - 1] = new Coordinate(currentWidth-1, height);
        for (int width = currentWidth - 1; width > 0; width--) {
            height = parentMatrix[height][width].Y;
            seam[width - 1] = new Coordinate(width-1, height);
        }
        return seam;
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

        LinkedList<Coordinate[]> removedSeams;

        if(showVerticalSeams){
            carveImage(CarvingScheme.VERTICAL_HORIZONTAL);
            removedSeams = verticalRemovedSeams;
        }else{
            carveImage(CarvingScheme.HORIZONTAL_VERTICAL);
            removedSeams = horizontalRemovedSeams;
        }

        setForEachParameters(inWidth,inHeight);
        BufferedImage ans = duplicateWorkingImage();

        for(Coordinate[] coordinateArray:removedSeams){
            for(Coordinate coordinate: coordinateArray){
                ans.setRGB(coordinate.X,coordinate.Y,seamColorRGB);
            }
        }
        return ans;
    }
}
