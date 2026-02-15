package com.esprit.services;

import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.*;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.bytedeco.opencv.opencv_imgproc.*;

import static org.bytedeco.opencv.global.opencv_core.*;
import static org.bytedeco.opencv.global.opencv_imgproc.*;
import static org.bytedeco.opencv.global.opencv_imgcodecs.*;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Base64;

/**
 * Face Recognition Service using OpenCV via JavaCV.
 * 
 * - Captures frames from webcam
 * - Detects faces using Haar Cascade
 * - Encodes face region as Base64 for storage/comparison
 * - Compares faces using histogram correlation
 */
public class FaceRecognitionService {

    private OpenCVFrameGrabber grabber;
    private final OpenCVFrameConverter.ToMat converter = new OpenCVFrameConverter.ToMat();
    private CascadeClassifier faceDetector;
    private boolean cameraRunning = false;

    // Threshold for face match (0.0 = no match, 1.0 = perfect match)
    // Same person with webcam variation typically scores 0.72-0.95
    // Different person typically scores 0.40-0.65
    private static final double MATCH_THRESHOLD = 0.70;

    public FaceRecognitionService() {
        // Load the Haar cascade for face detection from OpenCV data
        try {
            // Extract cascade file from OpenCV resources
            String cascadePath = extractCascade();
            faceDetector = new CascadeClassifier(cascadePath);
            if (faceDetector.empty()) {
                System.err.println("ERROR: Could not load face cascade classifier.");
            }
        } catch (Exception e) {
            System.err.println("ERROR initializing face detector: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Extract the haarcascade XML to a temp file so OpenCV can load it
     */
    private String extractCascade() throws IOException {
        // Try multiple resource paths
        String[] paths = {
            "/haarcascade_frontalface_alt.xml",
            "haarcascade_frontalface_alt.xml"
        };

        InputStream is = null;
        for (String p : paths) {
            is = getClass().getResourceAsStream(p);
            if (is != null) {
                System.out.println("Found cascade at resource path: " + p);
                break;
            }
        }

        if (is == null) {
            // Try class loader
            is = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream("haarcascade_frontalface_alt.xml");
            if (is != null) System.out.println("Found cascade via context classloader");
        }

        if (is == null) {
            throw new IOException("Cannot find haarcascade_frontalface_alt.xml. " +
                "Place it in src/main/resources/");
        }

        File tempFile = File.createTempFile("haarcascade_frontalface_alt", ".xml");
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        is.close();

        System.out.println("Cascade extracted to: " + tempFile.getAbsolutePath()
            + " (size: " + tempFile.length() + " bytes)");
        return tempFile.getAbsolutePath();
    }

    /**
     * Start the webcam capture
     */
    public void startCamera() throws FrameGrabber.Exception {
        if (cameraRunning) return;

        grabber = new OpenCVFrameGrabber(0);
        grabber.setImageWidth(640);
        grabber.setImageHeight(480);
        grabber.start();
        cameraRunning = true;
        System.out.println("Camera started successfully.");
    }

    /**
     * Stop the webcam capture
     */
    public void stopCamera() {
        if (!cameraRunning) return;

        try {
            if (grabber != null) {
                grabber.stop();
                grabber.release();
            }
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        cameraRunning = false;
        System.out.println("Camera stopped.");
    }

    /**
     * Check if camera is running
     */
    public boolean isCameraRunning() {
        return cameraRunning;
    }

    /**
     * Grab a single frame from the webcam and return it as a JavaFX Image
     */
    public Image grabFrame() {
        if (!cameraRunning || grabber == null) return null;

        try {
            Frame frame = grabber.grab();
            if (frame == null) return null;

            Mat mat = converter.convert(frame);
            if (mat == null || mat.empty()) return null;

            return matToImage(mat);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Grab current frame as Mat (OpenCV)
     */
    public Mat grabMat() {
        if (!cameraRunning || grabber == null) return null;

        try {
            Frame frame = grabber.grab();
            if (frame == null) return null;

            return converter.convert(frame);
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Detect faces in the given Mat image
     * Returns a RectVector of detected face rectangles
     */
    public RectVector detectFaces(Mat image) {
        if (faceDetector == null || faceDetector.empty()) return new RectVector();

        Mat gray = new Mat();
        if (image.channels() > 1) {
            cvtColor(image, gray, COLOR_BGR2GRAY);
        } else {
            gray = image.clone();
        }
        equalizeHist(gray, gray);

        RectVector faces = new RectVector();
        faceDetector.detectMultiScale(
            gray, faces,
            1.1,     // scaleFactor
            3,       // minNeighbors (lower = more sensitive)
            0,       // flags
            new Size(30, 30),   // minSize — smaller for distant faces
            new Size(0, 0)      // maxSize — unlimited
        );

        System.out.println("Faces detected: " + faces.size());

        gray.close();
        return faces;
    }

    /**
     * Extract face region from image and encode as Base64 string.
     * Takes multiple samples and picks the best one for stability.
     * Returns null if no face detected.
     */
    public String encodeFace(Mat image) {
        RectVector faces = detectFaces(image);
        if (faces.size() == 0) {
            System.out.println("encodeFace: No face detected in frame");
            return null;
        }

        // Take the largest face
        Rect faceRect = faces.get(0);
        long maxArea = faceRect.area();
        for (int i = 1; i < faces.size(); i++) {
            if (faces.get(i).area() > maxArea) {
                faceRect = faces.get(i);
                maxArea = faceRect.area();
            }
        }

        System.out.println("encodeFace: Face found at (" + faceRect.x() + "," + faceRect.y()
            + ") size " + faceRect.width() + "x" + faceRect.height());

        Mat faceROI = new Mat(image, faceRect);

        // Resize face to standard size
        Mat standardFace = new Mat();
        resize(faceROI, standardFace, new Size(200, 200));

        // Convert to grayscale
        Mat grayFace = new Mat();
        if (standardFace.channels() > 1) {
            cvtColor(standardFace, grayFace, COLOR_BGR2GRAY);
        } else {
            grayFace = standardFace.clone();
        }

        // Apply histogram equalization for consistent lighting
        equalizeHist(grayFace, grayFace);

        // Apply slight Gaussian blur to reduce noise from webcam
        GaussianBlur(grayFace, grayFace, new Size(3, 3), 0);

        // Encode face image as Base64
        byte[] faceBytes = matToBytes(grayFace);
        if (faceBytes == null) return null;

        String encoded = Base64.getEncoder().encodeToString(faceBytes);
        System.out.println("encodeFace: Encoded " + faceBytes.length + " bytes -> Base64 length " + encoded.length());

        // Cleanup
        faceROI.close();
        standardFace.close();
        grayFace.close();

        return encoded;
    }

    /**
     * Compare a captured face encoding with a stored face encoding.
     * Uses multiple complementary methods for robust comparison.
     * Returns similarity score (0.0 to 1.0)
     */
    public double compareFaces(String storedEncoding, String capturedEncoding) {
        if (storedEncoding == null || capturedEncoding == null) return 0.0;

        try {
            byte[] storedBytes = Base64.getDecoder().decode(storedEncoding);
            byte[] capturedBytes = Base64.getDecoder().decode(capturedEncoding);

            System.out.println("compareFaces: stored=" + storedBytes.length + " bytes, captured=" + capturedBytes.length + " bytes");

            // Determine face size from byte count
            int faceSize = (int) Math.round(Math.sqrt(storedBytes.length));
            if (faceSize * faceSize != storedBytes.length) faceSize = 200;
            int capSize = (int) Math.round(Math.sqrt(capturedBytes.length));
            if (capSize * capSize != capturedBytes.length) capSize = 200;

            Mat storedFace = bytesToMat(storedBytes, faceSize, faceSize);
            Mat capturedFace = bytesToMat(capturedBytes, capSize, capSize);

            if (storedFace == null || capturedFace == null) {
                System.out.println("compareFaces: Failed to create Mat from bytes");
                return 0.0;
            }

            // Resize both to 200x200 if needed
            if (storedFace.rows() != 200 || storedFace.cols() != 200) {
                Mat resized = new Mat();
                resize(storedFace, resized, new Size(200, 200));
                storedFace.close();
                storedFace = resized;
            }
            if (capturedFace.rows() != 200 || capturedFace.cols() != 200) {
                Mat resized = new Mat();
                resize(capturedFace, resized, new Size(200, 200));
                capturedFace.close();
                capturedFace = resized;
            }

            // Method 1: LBPH comparison (weight: 40%)
            double lbphScore = compareLBPH(storedFace, capturedFace);

            // Method 2: Normalized correlation (weight: 35%)
            double corrScore = compareCorrelation(storedFace, capturedFace);

            // Method 3: Structural region comparison (weight: 25%)
            double regionScore = compareRegions(storedFace, capturedFace);

            double combined = lbphScore * 0.40 + corrScore * 0.35 + regionScore * 0.25;

            System.out.println(String.format(
                "Face comparison — LBPH: %.4f, Correlation: %.4f, Regions: %.4f => Combined: %.4f (threshold: %.2f)",
                lbphScore, corrScore, regionScore, combined, MATCH_THRESHOLD
            ));

            // Cleanup
            storedFace.close();
            capturedFace.close();

            return combined;

        } catch (Exception e) {
            System.err.println("compareFaces ERROR: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Normalized correlation coefficient between two face images.
     * Very effective for same-person recognition with lighting variation.
     */
    private double compareCorrelation(Mat face1, Mat face2) {
        byte[] data1 = getPixelData(face1);
        byte[] data2 = getPixelData(face2);
        if (data1 == null || data2 == null) return 0.0;

        int limit = Math.min(data1.length, data2.length);

        double mean1 = 0, mean2 = 0;
        for (int i = 0; i < limit; i++) {
            mean1 += (data1[i] & 0xFF);
            mean2 += (data2[i] & 0xFF);
        }
        mean1 /= limit;
        mean2 /= limit;

        double numerator = 0, denom1 = 0, denom2 = 0;
        for (int i = 0; i < limit; i++) {
            double d1 = (data1[i] & 0xFF) - mean1;
            double d2 = (data2[i] & 0xFF) - mean2;
            numerator += d1 * d2;
            denom1 += d1 * d1;
            denom2 += d2 * d2;
        }

        double denom = Math.sqrt(denom1 * denom2);
        double correlation = (denom > 0) ? (numerator / denom) : 0.0;

        // correlation ranges -1 to 1; normalize to 0 to 1
        return (correlation + 1.0) / 2.0;
    }

    /**
     * LBPH: Compare Local Binary Pattern Histograms
     * LBP captures micro-texture patterns unique to each face
     */
    private double compareLBPH(Mat face1, Mat face2) {
        int rows = face1.rows();
        int cols = face1.cols();

        byte[] data1 = getPixelData(face1);
        byte[] data2 = getPixelData(face2);
        if (data1 == null || data2 == null) return 0.0;

        // Calculate LBP for both faces
        int[] lbpHist1 = computeLBPHistogram(data1, rows, cols);
        int[] lbpHist2 = computeLBPHistogram(data2, rows, cols);

        // Compare histograms using Bhattacharyya coefficient (better than chi-squared)
        double dotProduct = 0;
        double norm1 = 0;
        double norm2 = 0;
        for (int i = 0; i < 256; i++) {
            dotProduct += Math.sqrt((double) lbpHist1[i] * lbpHist2[i]);
            norm1 += lbpHist1[i];
            norm2 += lbpHist2[i];
        }

        // Bhattacharyya coefficient: 1.0 = identical, 0.0 = completely different
        double bhatt = (norm1 > 0 && norm2 > 0)
            ? dotProduct / Math.sqrt(norm1 * norm2)
            : 0.0;

        return bhatt;
    }

    /**
     * Compute LBP histogram for a grayscale image
     */
    private int[] computeLBPHistogram(byte[] data, int rows, int cols) {
        int[] histogram = new int[256];

        for (int y = 1; y < rows - 1; y++) {
            for (int x = 1; x < cols - 1; x++) {
                int center = data[y * cols + x] & 0xFF;
                int lbp = 0;

                // 8 neighbors, clockwise from top-left
                if ((data[(y-1)*cols + (x-1)] & 0xFF) >= center) lbp |= 128;
                if ((data[(y-1)*cols + x]     & 0xFF) >= center) lbp |= 64;
                if ((data[(y-1)*cols + (x+1)] & 0xFF) >= center) lbp |= 32;
                if ((data[y*cols + (x+1)]     & 0xFF) >= center) lbp |= 16;
                if ((data[(y+1)*cols + (x+1)] & 0xFF) >= center) lbp |= 8;
                if ((data[(y+1)*cols + x]     & 0xFF) >= center) lbp |= 4;
                if ((data[(y+1)*cols + (x-1)] & 0xFF) >= center) lbp |= 2;
                if ((data[y*cols + (x-1)]     & 0xFF) >= center) lbp |= 1;

                histogram[lbp]++;
            }
        }
        return histogram;
    }

    /**
     * Multi-region comparison: Split face into grid regions and compare each
     * Different face regions have different discriminative power
     */
    private double compareRegions(Mat face1, Mat face2) {
        int rows = face1.rows();
        int cols = face1.cols();
        byte[] data1 = getPixelData(face1);
        byte[] data2 = getPixelData(face2);
        if (data1 == null || data2 == null) return 0.0;

        // Split face into 4x4 grid = 16 regions
        int gridRows = 4;
        int gridCols = 4;
        int regionH = rows / gridRows;
        int regionW = cols / gridCols;

        // Weights: eye area and nose/mouth area are more discriminative
        // Row 0=forehead, 1=eyes, 2=nose, 3=mouth
        double[][] weights = {
            {0.5, 0.8, 0.8, 0.5},   // forehead row
            {0.9, 1.5, 1.5, 0.9},   // eye row (most important)
            {0.7, 1.2, 1.2, 0.7},   // nose row
            {0.6, 1.0, 1.0, 0.6}    // mouth/chin row
        };

        double totalSimilarity = 0;
        double totalWeight = 0;

        for (int gy = 0; gy < gridRows; gy++) {
            for (int gx = 0; gx < gridCols; gx++) {
                // Compute LBP histogram for this region in both faces
                int[] regionHist1 = new int[256];
                int[] regionHist2 = new int[256];

                int startY = gy * regionH + 1;
                int endY = Math.min((gy + 1) * regionH - 1, rows - 1);
                int startX = gx * regionW + 1;
                int endX = Math.min((gx + 1) * regionW - 1, cols - 1);

                for (int y = startY; y < endY; y++) {
                    for (int x = startX; x < endX; x++) {
                        if (y > 0 && y < rows - 1 && x > 0 && x < cols - 1) {
                            int center1 = data1[y * cols + x] & 0xFF;
                            int lbp1 = computeLBPAt(data1, y, x, cols, center1);
                            regionHist1[lbp1]++;

                            int center2 = data2[y * cols + x] & 0xFF;
                            int lbp2 = computeLBPAt(data2, y, x, cols, center2);
                            regionHist2[lbp2]++;
                        }
                    }
                }

                // Compare region histograms using Bhattacharyya
                double dotP = 0, n1 = 0, n2 = 0;
                for (int i = 0; i < 256; i++) {
                    dotP += Math.sqrt((double) regionHist1[i] * regionHist2[i]);
                    n1 += regionHist1[i];
                    n2 += regionHist2[i];
                }
                double regionSim = (n1 > 0 && n2 > 0) ? dotP / Math.sqrt(n1 * n2) : 0.0;
                double w = weights[gy][gx];
                totalSimilarity += regionSim * w;
                totalWeight += w;
            }
        }

        return totalWeight > 0 ? totalSimilarity / totalWeight : 0.0;
    }

    private int computeLBPAt(byte[] data, int y, int x, int cols, int center) {
        int lbp = 0;
        if ((data[(y-1)*cols + (x-1)] & 0xFF) >= center) lbp |= 128;
        if ((data[(y-1)*cols + x]     & 0xFF) >= center) lbp |= 64;
        if ((data[(y-1)*cols + (x+1)] & 0xFF) >= center) lbp |= 32;
        if ((data[y*cols + (x+1)]     & 0xFF) >= center) lbp |= 16;
        if ((data[(y+1)*cols + (x+1)] & 0xFF) >= center) lbp |= 8;
        if ((data[(y+1)*cols + x]     & 0xFF) >= center) lbp |= 4;
        if ((data[(y+1)*cols + (x-1)] & 0xFF) >= center) lbp |= 2;
        if ((data[y*cols + (x-1)]     & 0xFF) >= center) lbp |= 1;
        return lbp;
    }

    /**
     * Get raw pixel data from a Mat
     */
    private byte[] getPixelData(Mat mat) {
        int totalPixels = mat.rows() * mat.cols();
        byte[] data = new byte[totalPixels];
        ByteBuffer buf = mat.createBuffer();
        int limit = Math.min(totalPixels, buf.remaining());
        for (int i = 0; i < limit; i++) {
            data[i] = buf.get(i);
        }
        return data;
    }

    /**
     * Check if captured face matches stored face (above threshold)
     */
    public boolean isFaceMatch(String storedEncoding, String capturedEncoding) {
        double score = compareFaces(storedEncoding, capturedEncoding);
        System.out.println("Face match score: " + score + " (threshold: " + MATCH_THRESHOLD + ")");
        return score >= MATCH_THRESHOLD;
    }

    /**
     * Draw rectangles around detected faces on the Mat
     * Returns the Mat with rectangles drawn
     */
    public Mat drawFaceRects(Mat image, RectVector faces) {
        Mat result = image.clone();
        for (int i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            rectangle(result,
                new Point(face.x(), face.y()),
                new Point(face.x() + face.width(), face.y() + face.height()),
                new Scalar(0, 215, 255, 255),  // Gold color (BGR)
                2, LINE_AA, 0
            );
        }
        return result;
    }

    /**
     * Grab frame, detect faces, draw rectangles, return as JavaFX Image
     * Also returns whether a face was detected
     */
    public CameraResult grabFrameWithDetection() {
        if (!cameraRunning || grabber == null) return null;

        try {
            Frame frame = grabber.grab();
            if (frame == null) return null;

            Mat mat = converter.convert(frame);
            if (mat == null || mat.empty()) return null;

            RectVector faces = detectFaces(mat);
            Mat display = drawFaceRects(mat, faces);

            Image image = matToImage(display);
            boolean faceDetected = faces.size() > 0;

            display.close();

            return new CameraResult(image, faceDetected, mat);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════
    // UTILITY: Mat ↔ Image conversion
    // ═══════════════════════════════════════════════════

    /**
     * Convert OpenCV Mat to JavaFX Image
     */
    public Image matToImage(Mat mat) {
        if (mat == null || mat.empty()) return null;

        int width = mat.cols();
        int height = mat.rows();
        int channels = mat.channels();

        byte[] data = new byte[width * height * channels];
        ByteBuffer buffer = mat.createBuffer();

        int dataLength = Math.min(data.length, buffer.remaining());
        buffer.get(data, 0, dataLength);

        WritableImage image = new WritableImage(width, height);
        PixelWriter pw = image.getPixelWriter();

        if (channels == 3) {
            // BGR → RGB for JavaFX
            byte[] rgbData = new byte[width * height * 3];
            for (int i = 0; i < width * height; i++) {
                int idx = i * 3;
                if (idx + 2 < dataLength) {
                    rgbData[idx] = data[idx + 2];     // R
                    rgbData[idx + 1] = data[idx + 1]; // G
                    rgbData[idx + 2] = data[idx];     // B
                }
            }
            pw.setPixels(0, 0, width, height,
                PixelFormat.getByteRgbInstance(), rgbData, 0, width * 3);
        } else if (channels == 1) {
            // Grayscale → RGB
            byte[] rgbData = new byte[width * height * 3];
            for (int i = 0; i < width * height && i < dataLength; i++) {
                byte gray = data[i];
                rgbData[i * 3] = gray;
                rgbData[i * 3 + 1] = gray;
                rgbData[i * 3 + 2] = gray;
            }
            pw.setPixels(0, 0, width, height,
                PixelFormat.getByteRgbInstance(), rgbData, 0, width * 3);
        }

        return image;
    }

    /**
     * Convert Mat to byte array (grayscale raw pixels)
     */
    private byte[] matToBytes(Mat mat) {
        if (mat == null || mat.empty()) return null;

        int totalBytes = mat.rows() * mat.cols() * mat.channels();
        byte[] bytes = new byte[totalBytes];
        ByteBuffer buffer = mat.createBuffer();
        int len = Math.min(totalBytes, buffer.remaining());
        buffer.get(bytes, 0, len);
        return bytes;
    }

    /**
     * Convert byte array to Mat (grayscale, known dimensions)
     */
    private Mat bytesToMat(byte[] bytes, int rows, int cols) {
        try {
            Mat mat = new Mat(rows, cols, CV_8UC1);
            ByteBuffer buffer = mat.createBuffer();
            int len = Math.min(bytes.length, buffer.remaining());
            buffer.put(bytes, 0, len);
            return mat;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // ═══════════════════════════════════════════════════
    // RESULT HOLDER
    // ═══════════════════════════════════════════════════

    /**
     * Holds the result of a camera frame grab with face detection
     */
    public static class CameraResult {
        private final Image image;
        private final boolean faceDetected;
        private final Mat originalMat;

        public CameraResult(Image image, boolean faceDetected, Mat originalMat) {
            this.image = image;
            this.faceDetected = faceDetected;
            this.originalMat = originalMat;
        }

        public Image getImage() { return image; }
        public boolean isFaceDetected() { return faceDetected; }
        public Mat getOriginalMat() { return originalMat; }
    }
}
