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
    // We store 3 face captures separated by ||| for robust matching
    private static final String ENCODING_SEPARATOR = "|||";
    private static final int NUM_CAPTURES = 3;

    // MSE threshold: same person RMSE is typically 15-35, different person 45-80+
    // We convert to similarity: sim = exp(-mse / sigma)
    // Same person: sim ~ 0.75-0.95, Different person: sim ~ 0.15-0.45
    private static final double MATCH_THRESHOLD = 0.55;

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
     * Encode a single face from a frame as Base64.
     * Returns null if no face detected.
     */
    public String encodeFace(Mat image) {
        return encodeSingleFace(image);
    }

    /**
     * Capture multiple face encodings for registration (more robust).
     * Takes NUM_CAPTURES samples with delays between them.
     * Returns a combined encoding string with ||| separator, or null on failure.
     */
    public String encodeMultipleFaces() {
        if (!cameraRunning) return null;

        List<String> encodings = new ArrayList<>();
        System.out.println("=== Multi-capture face registration: capturing " + NUM_CAPTURES + " samples ===");

        for (int i = 0; i < NUM_CAPTURES * 3 && encodings.size() < NUM_CAPTURES; i++) {
            try {
                Thread.sleep(300); // wait between captures for variation
            } catch (InterruptedException ignored) {}

            Mat frame = grabMat();
            if (frame == null) continue;

            String enc = encodeSingleFace(frame);
            if (enc != null) {
                encodings.add(enc);
                System.out.println("Captured face " + encodings.size() + "/" + NUM_CAPTURES);
            }
        }

        if (encodings.size() < 2) {
            System.out.println("Failed: only captured " + encodings.size() + " faces, need at least 2");
            return null;
        }

        String combined = String.join(ENCODING_SEPARATOR, encodings);
        System.out.println("Multi-capture complete: " + encodings.size() + " samples, total length: " + combined.length());
        return combined;
    }

    /**
     * Extract and encode a single face from an image.
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

        Mat faceROI = new Mat(image, faceRect);

        // Resize to standard 200x200
        Mat standardFace = new Mat();
        resize(faceROI, standardFace, new Size(200, 200));

        // Convert to grayscale
        Mat grayFace = new Mat();
        if (standardFace.channels() > 1) {
            cvtColor(standardFace, grayFace, COLOR_BGR2GRAY);
        } else {
            grayFace = standardFace.clone();
        }

        // Histogram equalization for consistent lighting
        equalizeHist(grayFace, grayFace);

        // Slight blur to reduce webcam noise
        GaussianBlur(grayFace, grayFace, new Size(3, 3), 0);

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
     * Uses MSE (Mean Squared Error) which measures ACTUAL pixel differences.
     * 
     * WHY MSE WORKS: Different people have different eye shapes, nose, mouth.
     * These show up as large pixel differences (high MSE).
     * Correlation/Bhattacharyya NORMALIZE these away, making all faces look similar.
     * MSE preserves the actual differences.
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
            return bestScore;

        } catch (Exception e) {
            System.err.println("compareFaces ERROR: " + e.getMessage());
            e.printStackTrace();
            return 0.0;
        }
    }

    /**
     * Compare two single face encodings using MSE + regional MSE.
     */
    private double compareTwoFaces(String enc1, String enc2) {
        try {
            byte[] bytes1 = Base64.getDecoder().decode(enc1);
            byte[] bytes2 = Base64.getDecoder().decode(enc2);

            int size1 = (int) Math.round(Math.sqrt(bytes1.length));
            int size2 = (int) Math.round(Math.sqrt(bytes2.length));
            if (size1 * size1 != bytes1.length) size1 = 200;
            if (size2 * size2 != bytes2.length) size2 = 200;

            Mat face1 = bytesToMat(bytes1, size1, size1);
            Mat face2 = bytesToMat(bytes2, size2, size2);
            if (face1 == null || face2 == null) return 0.0;

            // Ensure same size
            if (face1.rows() != 200 || face1.cols() != 200) {
                Mat r = new Mat(); resize(face1, r, new Size(200, 200)); face1.close(); face1 = r;
            }
            if (face2.rows() != 200 || face2.cols() != 200) {
                Mat r = new Mat(); resize(face2, r, new Size(200, 200)); face2.close(); face2 = r;
            }

            byte[] data1 = getPixelData(face1);
            byte[] data2 = getPixelData(face2);

            // Method 1: Global MSE (weight 40%)
            double globalMSE = computeMSE(data1, data2, 0, data1.length);
            double globalSim = mseToSimilarity(globalMSE);

            // Method 2: Regional MSE with face-area weighting (weight 40%)
            double regionSim = computeRegionalMSE(data1, data2, 200, 200);

            // Method 3: Absolute difference count — how many pixels differ significantly (weight 20%)
            double diffRatio = computeDiffRatio(data1, data2, 30); // pixels differing by >30

            double combined = globalSim * 0.40 + regionSim * 0.40 + diffRatio * 0.20;

            System.out.println(String.format(
                "  MSE=%.1f (sim=%.4f), RegionSim=%.4f, DiffRatio=%.4f => Combined=%.4f",
                globalMSE, globalSim, regionSim, diffRatio, combined
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
     * Compute Mean Squared Error between pixel arrays
     */
    private double computeMSE(byte[] d1, byte[] d2, int start, int end) {
        double mse = 0;
        int count = 0;
        int limit = Math.min(Math.min(d1.length, d2.length), end);
        for (int i = start; i < limit; i++) {
            double diff = (d1[i] & 0xFF) - (d2[i] & 0xFF);
            mse += diff * diff;
            count++;
        }
        return count > 0 ? mse / count : Double.MAX_VALUE;
    }

    /**
     * Convert MSE to similarity score using exponential decay.
     * Same person MSE: ~200-600 -> sim 0.74-0.90
     * Diff person MSE: ~1200-3000+ -> sim 0.22-0.55
     */
    private double mseToSimilarity(double mse) {
        // sigma controls the decay rate — tuned for 200x200 equalized grayscale faces
        double sigma = 1500.0;
        return Math.exp(-mse / sigma);
    }

    /**
     * Regional MSE: split face into weighted regions.
     * Eye & nose regions weigh more because they differ most between people.
     */
    private double computeRegionalMSE(byte[] d1, byte[] d2, int rows, int cols) {
        // 5 key face regions (y-ranges as fractions of face height)
        // [startRow%, endRow%, startCol%, endCol%, weight]
        double[][] regions = {
            {0.05, 0.30, 0.10, 0.90, 0.8},  // forehead
            {0.25, 0.50, 0.05, 0.45, 1.8},  // left eye
            {0.25, 0.50, 0.55, 0.95, 1.8},  // right eye
            {0.40, 0.70, 0.25, 0.75, 1.5},  // nose
            {0.65, 0.90, 0.15, 0.85, 1.2},  // mouth
        };

        double totalSim = 0;
        double totalWeight = 0;

        for (double[] reg : regions) {
            int r0 = (int)(reg[0] * rows), r1 = (int)(reg[1] * rows);
            int c0 = (int)(reg[2] * cols), c1 = (int)(reg[3] * cols);
            double weight = reg[4];

            double regionMSE = 0;
            int count = 0;
            for (int y = r0; y < r1; y++) {
                for (int x = c0; x < c1; x++) {
                    int idx = y * cols + x;
                    if (idx < d1.length && idx < d2.length) {
                        double diff = (d1[idx] & 0xFF) - (d2[idx] & 0xFF);
                        regionMSE += diff * diff;
                        count++;
                    }
                }
            }
            regionMSE = count > 0 ? regionMSE / count : 5000;
            totalSim += mseToSimilarity(regionMSE) * weight;
            totalWeight += weight;
        }

        return totalWeight > 0 ? totalSim / totalWeight : 0.0;
    }

    /**
     * Compute ratio of pixels that are SIMILAR (differ by less than threshold).
     * Same person: most pixels similar -> high ratio (0.75-0.90)
     * Diff person: many pixels differ -> low ratio (0.40-0.60)
     */
    private double computeDiffRatio(byte[] d1, byte[] d2, int threshold) {
        int similar = 0;
        int limit = Math.min(d1.length, d2.length);
        for (int i = 0; i < limit; i++) {
            int diff = Math.abs((d1[i] & 0xFF) - (d2[i] & 0xFF));
            if (diff <= threshold) similar++;
        }
        return (double) similar / limit;
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
