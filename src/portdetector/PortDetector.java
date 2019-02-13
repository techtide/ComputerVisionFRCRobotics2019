package portdetector;

import java.util.ArrayList;
import java.util.List;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.*;

public class PortDetector {

    //Outputs
    private Mat resizeImageOutput = new Mat();
    private Mat blurOutput = new Mat();
    private Mat rgbThresholdOutput = new Mat();
    private ArrayList<MatOfPoint> findContoursOutput = new ArrayList<MatOfPoint>();
    private ArrayList<MatOfPoint> filterContoursOutput = new ArrayList<MatOfPoint>();

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    public ArrayList<MatOfPoint> process() {
        Mat source0 = Imgcodecs.imread(getClass().getResource("1.JPG").getPath());
        
        // Step Resize_Image0:
        Mat resizeImageInput = source0;
        double resizeImageWidth = 320.0;
        double resizeImageHeight = 240.0;
        int resizeImageInterpolation = Imgproc.INTER_CUBIC;
        resizeImage(resizeImageInput, resizeImageWidth, resizeImageHeight, resizeImageInterpolation, resizeImageOutput);

        Mat blurInput = resizeImageOutput;
        BlurType blurType = BlurType.get("Box Blur");
        double blurRadius = 0.0;
        blur(blurInput, blurType, blurRadius, blurOutput);
        
        Mat rgbThresholdInput = blurOutput;
        double[] rgbThresholdRed = {119.56849774417753, 133.7775891341256};
        double[] rgbThresholdGreen = {107.56284802666342, 148.93039049235995};
        double[] rgbThresholdBlue = {80.80061374629109, 116.74795903623163};
        rgbThreshold(rgbThresholdInput, rgbThresholdRed, rgbThresholdGreen, rgbThresholdBlue, rgbThresholdOutput);

        Mat findContoursInput = rgbThresholdOutput;
        boolean findContoursExternalOnly = false;
        findContours(findContoursInput, findContoursExternalOnly, findContoursOutput);

        ArrayList<MatOfPoint> filterContoursContours = findContoursOutput;
        double filterContoursMinArea = 20.0;
        double filterContoursMinPerimeter = 0.0;
        double filterContoursMinWidth = 8.0;
        double filterContoursMaxWidth = 1000.0;
        double filterContoursMinHeight = 0.0;
        double filterContoursMaxHeight = 1000.0;
        double[] filterContoursSolidity = {0, 100};
        double filterContoursMaxVertices = 1000000.0;
        double filterContoursMinVertices = 0.0;
        double filterContoursMinRatio = 0.0;
        double filterContoursMaxRatio = 1000.0;
        filterContours(filterContoursContours, filterContoursMinArea, filterContoursMinPerimeter, filterContoursMinWidth, filterContoursMaxWidth, filterContoursMinHeight, filterContoursMaxHeight, filterContoursSolidity, filterContoursMaxVertices, filterContoursMinVertices, filterContoursMinRatio, filterContoursMaxRatio, filterContoursOutput);

        return filterContoursOutput;
    }
    
    public Mat resizeImageOutput() {
        return resizeImageOutput;
    }

    public Mat blurOutput() {
        return blurOutput;
    }
    
    public Mat rgbThresholdOutput() {
        return rgbThresholdOutput;
    }

    public ArrayList<MatOfPoint> findContoursOutput() {
        return findContoursOutput;
    }

    public ArrayList<MatOfPoint> filterContoursOutput() {
        return filterContoursOutput;
    }

    private void resizeImage(Mat input, double width, double height,
            int interpolation, Mat output) {
        Imgproc.resize(input, output, new Size(width, height), 0.0, 0.0, interpolation);
    }

    enum BlurType {
        BOX("Box Blur"), GAUSSIAN("Gaussian Blur"), MEDIAN("Median Filter"),
        BILATERAL("Bilateral Filter");

        private final String label;

        BlurType(String label) {
            this.label = label;
        }

        public static BlurType get(String type) {
            if (BILATERAL.label.equals(type)) {
                return BILATERAL;
            } else if (GAUSSIAN.label.equals(type)) {
                return GAUSSIAN;
            } else if (MEDIAN.label.equals(type)) {
                return MEDIAN;
            } else {
                return BOX;
            }
        }

        @Override
        public String toString() {
            return this.label;
        }
    }
    
    private void blur(Mat input, BlurType type, double doubleRadius,
            Mat output) {
        int radius = (int) (doubleRadius + 0.5);
        int kernelSize;
        switch (type) {
            case BOX:
                kernelSize = 2 * radius + 1;
                Imgproc.blur(input, output, new Size(kernelSize, kernelSize));
                break;
            case GAUSSIAN:
                kernelSize = 6 * radius + 1;
                Imgproc.GaussianBlur(input, output, new Size(kernelSize, kernelSize), radius);
                break;
            case MEDIAN:
                kernelSize = 2 * radius + 1;
                Imgproc.medianBlur(input, output, kernelSize);
                break;
            case BILATERAL:
                Imgproc.bilateralFilter(input, output, -1, radius, radius);
                break;
        }
    }

    private void rgbThreshold(Mat input, double[] red, double[] green, double[] blue,
            Mat out) {
        Imgproc.cvtColor(input, out, Imgproc.COLOR_BGR2RGB);
        Core.inRange(out, new Scalar(red[0], green[0], blue[0]),
                new Scalar(red[1], green[1], blue[1]), out);
    }
    
    private void findContours(Mat input, boolean externalOnly,
            List<MatOfPoint> contours) {
        Mat hierarchy = new Mat();
        contours.clear();
        int mode;
        if (externalOnly) {
            mode = Imgproc.RETR_EXTERNAL;
        } else {
            mode = Imgproc.RETR_LIST;
        }
        int method = Imgproc.CHAIN_APPROX_SIMPLE;
        Imgproc.findContours(input, contours, hierarchy, mode, method);
    }

    private void filterContours(List<MatOfPoint> inputContours, double minArea,
            double minPerimeter, double minWidth, double maxWidth, double minHeight, double maxHeight, double[] solidity, double maxVertexCount, double minVertexCount, double minRatio, double maxRatio, List<MatOfPoint> output) {
        final MatOfInt hull = new MatOfInt();
        output.clear();
        //operation
        for (int i = 0; i < inputContours.size(); i++) {
            final MatOfPoint contour = inputContours.get(i);
            final Rect bb = Imgproc.boundingRect(contour);
            if (bb.width < minWidth || bb.width > maxWidth) {
                continue;
            }
            if (bb.height < minHeight || bb.height > maxHeight) {
                continue;
            }
            final double area = Imgproc.contourArea(contour);
            if (area < minArea) {
                continue;
            }
            if (Imgproc.arcLength(new MatOfPoint2f(contour.toArray()), true) < minPerimeter) {
                continue;
            }
            Imgproc.convexHull(contour, hull);
            MatOfPoint mopHull = new MatOfPoint();
            mopHull.create((int) hull.size().height, 1, CvType.CV_32SC2);
            for (int j = 0; j < hull.size().height; j++) {
                int index = (int) hull.get(j, 0)[0];
                double[] point = new double[]{contour.get(index, 0)[0], contour.get(index, 0)[1]};
                mopHull.put(j, 0, point);
            }
            final double solid = 100 * area / Imgproc.contourArea(mopHull);
            if (solid < solidity[0] || solid > solidity[1]) {
                continue;
            }
            if (contour.rows() < minVertexCount || contour.rows() > maxVertexCount) {
                continue;
            }
            final double ratio = bb.width / (double) bb.height;
            if (ratio < minRatio || ratio > maxRatio) {
                continue;
            }
            output.add(contour);
        }
    }

}
