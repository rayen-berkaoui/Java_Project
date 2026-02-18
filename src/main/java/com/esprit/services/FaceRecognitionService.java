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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

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

    // ═══════ MULTI-CAPTURE FACE ENCODING ═══════
    // We store 8 face captures separated by ||| for robust matching across sessions
    private static final String ENCODING_SEPARATOR = "|||";
    private static final int NUM_CAPTURES = 8;

    // Threshold for face matching using LBP histogram correlation.
    // Same person across sessions: typically 0.55-0.85
    // Different person: typically 0.20-0.50
    private static final double MATCH_THRESHOLD = 0.75;

    // Face quality thresholds
    private static final int MIN_FACE_SIZE = 80;  // Minimum face width/height in pixels
    private static final double MIN_SHARPNESS = 15.0;  // Minimum Laplacian variance for sharpness

    // Last match confidence (populated after compareFaces)
    private double lastMatchConfidence = 0.0;
    private String lastQualityMessage = "";

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

        gray.close();
        return faces;
    }

    /**
     * Encode a single face from a frame as Base64.
     * Returns null if no face detected.
     */
    public String encodeFace(Mat image) {
        return encodeSingleFace(image);
    }

    /**
     * Get the last match confidence score (0.0-1.0)
     */
    public double getLastMatchConfidence() {
        return lastMatchConfidence;
    }

    /**
     * Get the last quality assessment message
     */
    public String getLastQualityMessage() {
        return lastQualityMessage;
    }

    /**
     * Assess the quality of a face capture.
     * Returns a quality score (0.0-1.0) and sets lastQualityMessage.
     */
    public double assessFaceQuality(Mat image) {
        if (image == null || image.empty()) {
            lastQualityMessage = "Pas d'image";
            return 0.0;
        }

        RectVector faces = detectFaces(image);
        if (faces.size() == 0) {
            lastQualityMessage = "Aucun visage detecte";
            return 0.0;
        }

        Rect faceRect = faces.get(0);
        for (int i = 1; i < faces.size(); i++) {
            if (faces.get(i).area() > faceRect.area()) faceRect = faces.get(i);
        }

        double score = 0.0;

        // Size check (larger = better) — face should be at least 80px, ideal >150px
        int faceSize = Math.min(faceRect.width(), faceRect.height());
        if (faceSize < MIN_FACE_SIZE) {
            lastQualityMessage = "Rapprochez-vous de la camera";
            return 0.1;
        }
        double sizeScore = Math.min(1.0, faceSize / 200.0);
        score += sizeScore * 0.3;

        // Centering check — face should be near center of frame
        double centerX = faceRect.x() + faceRect.width() / 2.0;
        double centerY = faceRect.y() + faceRect.height() / 2.0;
        double frameCenterX = image.cols() / 2.0;
        double frameCenterY = image.rows() / 2.0;
        double distFromCenter = Math.sqrt(
            Math.pow((centerX - frameCenterX) / frameCenterX, 2) +
            Math.pow((centerY - frameCenterY) / frameCenterY, 2)
        );
        double centerScore = Math.max(0.0, 1.0 - distFromCenter);
        score += centerScore * 0.2;

        // Sharpness check using Laplacian variance
        Mat gray = new Mat();
        if (image.channels() > 1) {
            cvtColor(image, gray, COLOR_BGR2GRAY);
        } else {
            gray = image.clone();
        }
        Mat faceROI = new Mat(gray, faceRect);
        Mat laplacian = new Mat();
        Laplacian(faceROI, laplacian, CV_64F);

        // Calculate variance of Laplacian
        Mat meanMat = new Mat();
        Mat stddevMat = new Mat();
        meanStdDev(laplacian, meanMat, stddevMat);
        double stddevVal = stddevMat.createIndexer().getDouble(0);
        double sharpness = stddevVal * stddevVal;
        double sharpnessScore = Math.min(1.0, sharpness / 50.0);
        score += sharpnessScore * 0.3;

        // Brightness check — not too dark, not too bright
        double brightness = meanMat.createIndexer().getDouble(0);
        double brightnessScore;
        if (brightness < 40) {
            brightnessScore = brightness / 40.0;
            lastQualityMessage = "Eclairage insuffisant";
        } else if (brightness > 220) {
            brightnessScore = (255 - brightness) / 35.0;
            lastQualityMessage = "Trop de lumiere";
        } else {
            brightnessScore = 1.0;
        }
        score += brightnessScore * 0.2;

        // Set quality message
        if (score >= 0.8) {
            lastQualityMessage = "Excellente qualite";
        } else if (score >= 0.6) {
            lastQualityMessage = "Bonne qualite";
        } else if (score >= 0.4) {
            lastQualityMessage = "Qualite moyenne - ameliorez l'eclairage";
        } else {
            if (lastQualityMessage.isEmpty()) lastQualityMessage = "Qualite insuffisante";
        }

        meanMat.close();
        stddevMat.close();
        gray.close();
        faceROI.close();
        laplacian.close();

        return score;
    }

    /**
     * Capture multiple face encodings for registration (more robust).
     * Takes NUM_CAPTURES samples with delays between them for lighting variation.
     * Only accepts high-quality captures.
     * Returns a combined encoding string with ||| separator, or null on failure.
     */
    public String encodeMultipleFaces() {
        return encodeMultipleFaces(null);
    }

    /**
     * Capture multiple face encodings with progress callback.
     * Callback receives (capturedCount, totalNeeded, qualityScore)
     */
    public String encodeMultipleFaces(CaptureProgressCallback callback) {
        if (!cameraRunning) return null;

        List<String> encodings = new ArrayList<>();
        System.out.println("=== Multi-capture face registration: capturing " + NUM_CAPTURES + " samples ===");

        for (int i = 0; i < NUM_CAPTURES * 5 && encodings.size() < NUM_CAPTURES; i++) {
            try {
                // Longer delay between captures for more lighting/pose variation
                Thread.sleep(400);
            } catch (InterruptedException ignored) {}

            Mat frame = grabMat();
            if (frame == null) continue;

            // Only accept good quality captures
            double quality = assessFaceQuality(frame);
            if (quality < 0.4) {
                System.out.println("Skipping low quality capture (" + String.format("%.2f", quality) + ")");
                if (callback != null) callback.onProgress(encodings.size(), NUM_CAPTURES, quality, lastQualityMessage);
                continue;
            }

            String enc = encodeSingleFace(frame);
            if (enc != null) {
                encodings.add(enc);
                System.out.println("Captured face " + encodings.size() + "/" + NUM_CAPTURES + " (quality: " + String.format("%.2f", quality) + ")");
                if (callback != null) callback.onProgress(encodings.size(), NUM_CAPTURES, quality, lastQualityMessage);
            }
        }

        if (encodings.size() < 4) {
            System.out.println("Failed: only captured " + encodings.size() + " faces, need at least 4");
            return null;
        }

        String combined = String.join(ENCODING_SEPARATOR, encodings);
        System.out.println("Multi-capture complete: " + encodings.size() + " samples, total length: " + combined.length());
        return combined;
    }

    /**
     * Callback interface for capture progress
     */
    public interface CaptureProgressCallback {
        void onProgress(int captured, int total, double quality, String qualityMessage);
    }

    /**
     * Extract and encode a single face from an image.
     * Uses preprocessing to maximize cross-session consistency:
     * - Grayscale conversion
     * - CLAHE (adaptive histogram equalization) for local contrast normalization
     * - Gaussian blur to reduce noise
     * - Standardized 150x150 resize
     */
    private String encodeSingleFace(Mat image) {
        RectVector faces = detectFaces(image);
        if (faces.size() == 0) {
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

        // Expand the face ROI slightly (20% padding) to capture more context
        int padX = (int)(faceRect.width() * 0.1);
        int padY = (int)(faceRect.height() * 0.1);
        int x = Math.max(0, faceRect.x() - padX);
        int y = Math.max(0, faceRect.y() - padY);
        int w = Math.min(image.cols() - x, faceRect.width() + 2 * padX);
        int h = Math.min(image.rows() - y, faceRect.height() + 2 * padY);
        Rect expandedRect = new Rect(x, y, w, h);

        Mat faceROI = new Mat(image, expandedRect);

        // Resize to standard 150x150
        Mat standardFace = new Mat();
        resize(faceROI, standardFace, new Size(150, 150));

        // Convert to grayscale
        Mat grayFace = new Mat();
        if (standardFace.channels() > 1) {
            cvtColor(standardFace, grayFace, COLOR_BGR2GRAY);
        } else {
            grayFace = standardFace.clone();
        }

        // CLAHE (Contrast Limited Adaptive Histogram Equalization)
        // Much better than regular equalizeHist for cross-session robustness
        org.bytedeco.opencv.opencv_imgproc.CLAHE clahe = createCLAHE(2.0, new Size(8, 8));
        clahe.apply(grayFace, grayFace);

        // Slight blur to reduce webcam noise
        GaussianBlur(grayFace, grayFace, new Size(3, 3), 0.8);

        byte[] faceBytes = matToBytes(grayFace);
        if (faceBytes == null) return null;

        String encoded = Base64.getEncoder().encodeToString(faceBytes);

        faceROI.close();
        standardFace.close();
        grayFace.close();

        return encoded;
    }

    /**
     * Compare a captured face against a stored encoding (which may contain multiple captures).
     * Uses LBP (Local Binary Pattern) histograms + Normalized Cross-Correlation.
     * These methods are INVARIANT to brightness/contrast changes between sessions.
     * 
     * Returns similarity score (0.0 to 1.0)
     */
    public double compareFaces(String storedEncoding, String capturedEncoding) {
        if (storedEncoding == null || capturedEncoding == null) return 0.0;

        try {
            // Split stored encoding into multiple captures if present
            String[] storedParts = storedEncoding.split("\\|\\|\\|");
            
            double bestScore = 0.0;

            for (String storedPart : storedParts) {
                if (storedPart.trim().isEmpty()) continue;

                double score = compareTwoFaces(storedPart.trim(), capturedEncoding);
                if (score > bestScore) {
                    bestScore = score;
                }
            }

            System.out.println("Best match score across " + storedParts.length + " stored captures: " + String.format("%.4f", bestScore));
            lastMatchConfidence = bestScore;
            return bestScore;

        } catch (Exception e) {
            System.err.println("compareFaces ERROR: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Compare two single face encodings using highly discriminative methods:
     * 1. HOG descriptors (50%) — captures gradient/shape features of face geometry
     * 2. Spatial Grid Correlation (30%) — 6x6 grid, each cell must match independently
     * 3. Edge Structure Matching (20%) — compares Sobel edge maps of facial contours
     *
     * These methods focus on STRUCTURAL differences (nose shape, jawline, brow ridges)
     * rather than texture patterns, making them far more discriminative between people.
     */
    private double compareTwoFaces(String enc1, String enc2) {
        try {
            byte[] bytes1 = Base64.getDecoder().decode(enc1);
            byte[] bytes2 = Base64.getDecoder().decode(enc2);

            int size1 = (int) Math.round(Math.sqrt(bytes1.length));
            int size2 = (int) Math.round(Math.sqrt(bytes2.length));
            if (size1 * size1 != bytes1.length) size1 = 150;
            if (size2 * size2 != bytes2.length) size2 = 150;

            Mat face1 = bytesToMat(bytes1, size1, size1);
            Mat face2 = bytesToMat(bytes2, size2, size2);
            if (face1 == null || face2 == null) return 0.0;

            // Ensure same size (150x150)
            if (face1.rows() != 150 || face1.cols() != 150) {
                Mat r = new Mat(); resize(face1, r, new Size(150, 150)); face1.close(); face1 = r;
            }
            if (face2.rows() != 150 || face2.cols() != 150) {
                Mat r = new Mat(); resize(face2, r, new Size(150, 150)); face2.close(); face2 = r;
            }

            byte[] data1 = getPixelData(face1);
            byte[] data2 = getPixelData(face2);

            // Method 1: HOG Descriptor Correlation (weight 50%)
            // HOG captures gradient orientations = face shape geometry
            double hogScore = computeHOGCorrelation(data1, data2, 150, 150);

            // Method 2: Spatial Grid Correlation (weight 30%)
            // Fine-grained 6x6 grid comparison — each cell compared independently
            double gridScore = computeSpatialGridCorrelation(data1, data2, 150, 150);

            // Method 3: Edge Structure Matching (weight 20%)
            // Compare Sobel edge maps of facial contours
            double edgeScore = computeEdgeStructureMatch(data1, data2, 150, 150);

            double combined = hogScore * 0.50 + gridScore * 0.30 + edgeScore * 0.20;

            System.out.println(String.format(
                "  HOG=%.4f, Grid=%.4f, Edge=%.4f => Combined=%.4f",
                hogScore, gridScore, edgeScore, combined
            ));

            face1.close();
            face2.close();

            return combined;

        } catch (Exception e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * HOG (Histogram of Oriented Gradients) descriptor comparison.
     * Computes gradient magnitude and orientation at each pixel, then builds
     * orientation histograms in 10x10 cells with 9 orientation bins.
     * This captures the SHAPE of facial features (nose contour, eye socket,
     * jawline) which is highly discriminative between different people.
     */
    private double computeHOGCorrelation(byte[] d1, byte[] d2, int rows, int cols) {
        double[] hog1 = computeHOGDescriptor(d1, rows, cols);
        double[] hog2 = computeHOGDescriptor(d2, rows, cols);

        // Pearson correlation between HOG descriptors
        int n = Math.min(hog1.length, hog2.length);
        if (n == 0) return 0.0;

        double mean1 = 0, mean2 = 0;
        for (int i = 0; i < n; i++) { mean1 += hog1[i]; mean2 += hog2[i]; }
        mean1 /= n; mean2 /= n;

        double cov = 0, var1 = 0, var2 = 0;
        for (int i = 0; i < n; i++) {
            double v1 = hog1[i] - mean1;
            double v2 = hog2[i] - mean2;
            cov += v1 * v2;
            var1 += v1 * v1;
            var2 += v2 * v2;
        }

        double denom = Math.sqrt(var1 * var2);
        if (denom < 1e-10) return 0.0;

        double corr = cov / denom;
        return Math.max(0.0, (corr + 1.0) / 2.0);
    }

    /**
     * Compute HOG descriptor for a grayscale image.
     * Uses 10x10 pixel cells, 9 orientation bins (0-180 degrees).
     * Each cell produces a 9-bin histogram of gradient orientations,
     * weighted by gradient magnitude.
     */
    private double[] computeHOGDescriptor(byte[] data, int rows, int cols) {
        int cellSize = 10;
        int numBins = 9;
        int cellsY = rows / cellSize;
        int cellsX = cols / cellSize;
        double[] descriptor = new double[cellsY * cellsX * numBins];

        for (int cy = 0; cy < cellsY; cy++) {
            for (int cx = 0; cx < cellsX; cx++) {
                double[] cellHist = new double[numBins];

                for (int y = cy * cellSize + 1; y < (cy + 1) * cellSize - 1 && y < rows - 1; y++) {
                    for (int x = cx * cellSize + 1; x < (cx + 1) * cellSize - 1 && x < cols - 1; x++) {
                        // Compute gradient using centered differences
                        double gx = (data[y * cols + x + 1] & 0xFF) - (data[y * cols + x - 1] & 0xFF);
                        double gy = (data[(y + 1) * cols + x] & 0xFF) - (data[(y - 1) * cols + x] & 0xFF);

                        double magnitude = Math.sqrt(gx * gx + gy * gy);
                        double angle = Math.atan2(gy, gx) * 180.0 / Math.PI;
                        if (angle < 0) angle += 180.0; // Map to [0, 180)

                        // Bilinear interpolation into bins
                        double binWidth = 180.0 / numBins;
                        double binPos = angle / binWidth;
                        int bin0 = (int) binPos % numBins;
                        int bin1 = (bin0 + 1) % numBins;
                        double frac = binPos - (int) binPos;

                        cellHist[bin0] += magnitude * (1.0 - frac);
                        cellHist[bin1] += magnitude * frac;
                    }
                }

                // L2 normalize the cell histogram
                double norm = 0;
                for (double v : cellHist) norm += v * v;
                norm = Math.sqrt(norm + 1e-6);
                for (int b = 0; b < numBins; b++) {
                    descriptor[(cy * cellsX + cx) * numBins + b] = cellHist[b] / norm;
                }
            }
        }

        return descriptor;
    }

    /**
     * Fine-grained Spatial Grid Correlation.
     * Divides face into a 6x6 grid (36 cells). Each cell is compared independently
     * using Pearson correlation. The final score requires MOST cells to match well.
     * This catches local differences that global correlation would miss.
     * Cells in the eye/nose/mouth area are weighted more heavily.
     */
    private double computeSpatialGridCorrelation(byte[] d1, byte[] d2, int rows, int cols) {
        int gridSize = 6;
        int cellH = rows / gridSize;
        int cellW = cols / gridSize;

        // Weight matrix: higher weight for discriminative face regions
        // Top-center = forehead, middle rows = eyes/nose, bottom = mouth/chin
        double[][] weights = {
            {0.5, 0.7, 0.8, 0.8, 0.7, 0.5},  // row 0: top forehead
            {0.8, 1.5, 1.8, 1.8, 1.5, 0.8},  // row 1: eyebrows/upper eyes
            {0.8, 1.8, 2.0, 2.0, 1.8, 0.8},  // row 2: eyes
            {0.7, 1.2, 1.8, 1.8, 1.2, 0.7},  // row 3: nose
            {0.6, 1.0, 1.5, 1.5, 1.0, 0.6},  // row 4: mouth
            {0.4, 0.6, 0.8, 0.8, 0.6, 0.4},  // row 5: chin
        };

        double totalScore = 0;
        double totalWeight = 0;

        for (int gy = 0; gy < gridSize; gy++) {
            for (int gx = 0; gx < gridSize; gx++) {
                int startR = gy * cellH;
                int startC = gx * cellW;
                int endR = Math.min(startR + cellH, rows);
                int endC = Math.min(startC + cellW, cols);
                int cellSize = (endR - startR) * (endC - startC);

                byte[] cell1 = new byte[cellSize];
                byte[] cell2 = new byte[cellSize];
                int idx = 0;

                for (int y = startR; y < endR; y++) {
                    for (int x = startC; x < endC; x++) {
                        int pixIdx = y * cols + x;
                        if (pixIdx < d1.length && pixIdx < d2.length) {
                            cell1[idx] = d1[pixIdx];
                            cell2[idx] = d2[pixIdx];
                            idx++;
                        }
                    }
                }

                if (idx > 10) {
                    double cellCorr = computePearsonCorrelation(cell1, cell2, idx);
                    double w = weights[gy][gx];
                    totalScore += cellCorr * w;
                    totalWeight += w;
                }
            }
        }

        return totalWeight > 0 ? totalScore / totalWeight : 0.0;
    }

    /**
     * Compute Pearson correlation between two byte arrays.
     * Maps result from [-1,1] to [0,1].
     */
    private double computePearsonCorrelation(byte[] d1, byte[] d2, int n) {
        double mean1 = 0, mean2 = 0;
        for (int i = 0; i < n; i++) {
            mean1 += (d1[i] & 0xFF);
            mean2 += (d2[i] & 0xFF);
        }
        mean1 /= n;
        mean2 /= n;

        double cov = 0, var1 = 0, var2 = 0;
        for (int i = 0; i < n; i++) {
            double v1 = (d1[i] & 0xFF) - mean1;
            double v2 = (d2[i] & 0xFF) - mean2;
            cov += v1 * v2;
            var1 += v1 * v1;
            var2 += v2 * v2;
        }

        double denom = Math.sqrt(var1 * var2);
        if (denom < 1e-10) return 0.0;

        double corr = cov / denom;
        return Math.max(0.0, (corr + 1.0) / 2.0);
    }

    /**
     * Edge Structure Matching using Sobel gradients.
     * Computes horizontal and vertical edge maps, then compares the edge
     * strength patterns. This captures the contours of facial features
     * (nose ridge, eye creases, jawline profile) which differ significantly
     * between people even when overall pixel values are similar.
     */
    private double computeEdgeStructureMatch(byte[] d1, byte[] d2, int rows, int cols) {
        // Compute Sobel magnitude for both faces
        double[] edges1 = computeSobelMagnitude(d1, rows, cols);
        double[] edges2 = computeSobelMagnitude(d2, rows, cols);

        int n = edges1.length;
        if (n == 0) return 0.0;

        // Pearson correlation on edge maps
        double mean1 = 0, mean2 = 0;
        for (int i = 0; i < n; i++) { mean1 += edges1[i]; mean2 += edges2[i]; }
        mean1 /= n; mean2 /= n;

        double cov = 0, var1 = 0, var2 = 0;
        for (int i = 0; i < n; i++) {
            double v1 = edges1[i] - mean1;
            double v2 = edges2[i] - mean2;
            cov += v1 * v2;
            var1 += v1 * v1;
            var2 += v2 * v2;
        }

        double denom = Math.sqrt(var1 * var2);
        if (denom < 1e-10) return 0.0;

        double corr = cov / denom;
        return Math.max(0.0, (corr + 1.0) / 2.0);
    }

    /**
     * Compute Sobel gradient magnitude at each pixel.
     * Uses 3x3 Sobel kernels for horizontal and vertical edges.
     */
    private double[] computeSobelMagnitude(byte[] data, int rows, int cols) {
        double[] magnitude = new double[(rows - 2) * (cols - 2)];
        int idx = 0;

        for (int y = 1; y < rows - 1; y++) {
            for (int x = 1; x < cols - 1; x++) {
                // Sobel X kernel: [[-1,0,1],[-2,0,2],[-1,0,1]]
                double gx =
                    -(data[(y-1)*cols + (x-1)] & 0xFF) + (data[(y-1)*cols + (x+1)] & 0xFF)
                    - 2*(data[y*cols + (x-1)] & 0xFF) + 2*(data[y*cols + (x+1)] & 0xFF)
                    - (data[(y+1)*cols + (x-1)] & 0xFF) + (data[(y+1)*cols + (x+1)] & 0xFF);

                // Sobel Y kernel: [[-1,-2,-1],[0,0,0],[1,2,1]]
                double gy =
                    -(data[(y-1)*cols + (x-1)] & 0xFF) - 2*(data[(y-1)*cols + x] & 0xFF) - (data[(y-1)*cols + (x+1)] & 0xFF)
                    + (data[(y+1)*cols + (x-1)] & 0xFF) + 2*(data[(y+1)*cols + x] & 0xFF) + (data[(y+1)*cols + (x+1)] & 0xFF);

                magnitude[idx++] = Math.sqrt(gx * gx + gy * gy);
            }
        }

        return magnitude;
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
        for (int i = 0; i < faces.size(); i++) {
            Rect face = faces.get(i);
            rectangle(image,
                new Point(face.x(), face.y()),
                new Point(face.x() + face.width(), face.y() + face.height()),
                new Scalar(0, 215, 255, 255),  // Gold color (BGR)
                2, LINE_AA, 0
            );
        }
        return image;
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
            boolean faceDetected = faces.size() > 0;
            int faceCount = (int) faces.size();
            int fw = 0, fh = 0;
            if (faceDetected) {
                // Get the largest face dimensions
                Rect largest = faces.get(0);
                for (int i = 1; i < faces.size(); i++) {
                    if (faces.get(i).area() > largest.area()) largest = faces.get(i);
                }
                fw = largest.width();
                fh = largest.height();
            }

            // Draw rectangles directly on mat (no clone)
            drawFaceRects(mat, faces);
            Image image = matToImage(mat);

            return new CameraResult(image, faceDetected, faceDetected ? mat : null, faceCount, fw, fh);

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
        private final int faceCount;
        private final int faceWidth;
        private final int faceHeight;

        public CameraResult(Image image, boolean faceDetected, Mat originalMat) {
            this(image, faceDetected, originalMat, faceDetected ? 1 : 0, 0, 0);
        }

        public CameraResult(Image image, boolean faceDetected, Mat originalMat, int faceCount, int faceWidth, int faceHeight) {
            this.image = image;
            this.faceDetected = faceDetected;
            this.originalMat = originalMat;
            this.faceCount = faceCount;
            this.faceWidth = faceWidth;
            this.faceHeight = faceHeight;
        }

        public Image getImage() { return image; }
        public boolean isFaceDetected() { return faceDetected; }
        public Mat getOriginalMat() { return originalMat; }
        public int getFaceCount() { return faceCount; }
        public int getFaceWidth() { return faceWidth; }
        public int getFaceHeight() { return faceHeight; }
    }
}
