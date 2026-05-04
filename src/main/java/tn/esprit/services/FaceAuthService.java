package tn.esprit.services;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de reconnaissance faciale basé sur OpenCV.
 * Compare le visage capturé par la webcam avec la photo de profil stockée en BDD (champ `photo`).
 *
 * Algorithme :
 *  1. Détection du visage (Haar Cascade) dans les deux images
 *  2. Redimensionnement en 100×100 niveaux de gris
 *  3. Calcul de corrélation de Pearson normalisée entre les deux vecteurs de pixels
 *  4. Seuil de similarité configurable (défaut 0.75)
 */
public class FaceAuthService {

    private static final double SIMILARITY_THRESHOLD = 0.75;
    private static final int    FACE_SIZE            = 100;

    private CascadeClassifier faceDetector;
    private VideoCapture      camera;
    private boolean           opencvLoaded = false;
    private final Object      detectorLock = new Object();

    // ── Singleton ────────────────────────────────────────────────────────────
    private static FaceAuthService instance;
    public static FaceAuthService getInstance() {
        if (instance == null) instance = new FaceAuthService();
        return instance;
    }

    private FaceAuthService() { loadOpenCV(); }

    // ── Chargement OpenCV ────────────────────────────────────────────────────
    private void loadOpenCV() {
        try {
            nu.pattern.OpenCV.loadLocally();
            initCascade();
            opencvLoaded = true;
            System.out.println("[FaceAuth] OpenCV chargé : " + Core.VERSION);
        } catch (Exception e) {
            System.err.println("[FaceAuth] Impossible de charger OpenCV : " + e.getMessage());
        }
    }

    private void initCascade() throws Exception {
        InputStream is = getClass().getResourceAsStream("/haarcascade_frontalface_default.xml");
        if (is == null)
            is = getClass().getResourceAsStream("/org/opencv/haarcascade_frontalface_default.xml");
        if (is == null) throw new Exception("Haar cascade XML introuvable dans les ressources.");

        File tmp = File.createTempFile("haarcascade", ".xml");
        tmp.deleteOnExit();
        Files.copy(is, tmp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        is.close();

        faceDetector = new CascadeClassifier(tmp.getAbsolutePath());
        if (faceDetector.empty()) throw new Exception("CascadeClassifier vide après chargement.");
    }

    public boolean isAvailable() { return opencvLoaded && faceDetector != null && !faceDetector.empty(); }

    // ── Caméra ───────────────────────────────────────────────────────────────
    public boolean openCamera() {
        if (!isAvailable()) return false;
        camera = new VideoCapture(0);
        return camera.isOpened();
    }

    public void closeCamera() {
        if (camera != null && camera.isOpened()) { camera.release(); camera = null; }
    }

    public Mat captureFrame() {
        if (camera == null || !camera.isOpened()) return null;
        Mat frame = new Mat();
        camera.read(frame);
        return frame.empty() ? null : frame;
    }

    public javafx.scene.image.Image frameToFxImage(Mat frame) {
        if (frame == null || frame.empty()) return null;
        Mat rgb = new Mat();
        Imgproc.cvtColor(frame, rgb, Imgproc.COLOR_BGR2RGB);
        int w = rgb.cols(), h = rgb.rows();
        byte[] pixels = new byte[w * h * 3];
        rgb.get(0, 0, pixels);
        javafx.scene.image.WritableImage wi = new javafx.scene.image.WritableImage(w, h);
        javafx.scene.image.PixelWriter pw = wi.getPixelWriter();
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++) {
                int idx = (y * w + x) * 3;
                pw.setArgb(x, y, 0xFF000000
                        | ((pixels[idx]     & 0xFF) << 16)
                        | ((pixels[idx + 1] & 0xFF) <<  8)
                        |  (pixels[idx + 2] & 0xFF));
            }
        return wi;
    }

    // ── Détection & reconnaissance ────────────────────────────────────────────

    public List<Rect> detectFaces(Mat image) {
        List<Rect> result = new ArrayList<>();
        if (!isAvailable() || image == null || image.empty()) return result;
        Mat gray = new Mat();
        if (image.channels() > 1) Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        else image.copyTo(gray);
        Imgproc.equalizeHist(gray, gray);
        MatOfRect faces = new MatOfRect();
        try {
            synchronized (detectorLock) {
                faceDetector.detectMultiScale(gray, faces, 1.1, 4, 0, new Size(60, 60), new Size());
            }
        } catch (Exception e) {
            System.err.println("[FaceAuth] Erreur detectFaces : " + e.getMessage());
        }
        for (Rect r : faces.toArray()) result.add(r);
        return result;
    }

    /**
     * Compare le visage présent dans la frame webcam avec la photo de profil.
     *
     * @param liveFrame  frame OpenCV capturée par la webcam
     * @param photoPath  chemin local OU URL http/https vers la photo de profil
     */
    public boolean matchFace(Mat liveFrame, String photoPath) {
        if (!isAvailable() || liveFrame == null || photoPath == null || photoPath.isBlank())
            return false;
        try {
            // ── Visage live ──
            Mat liveGray = extractFaceGray(liveFrame);
            if (liveGray == null) {
                System.out.println("[FaceAuth] Aucun visage détecté sur webcam.");
                return false;
            }

            // ── Photo de référence : en mémoire si URL, fichier local sinon ──
            Mat refMat = photoPath.startsWith("http://") || photoPath.startsWith("https://")
                    ? readMatFromUrl(photoPath)       // ← bytes directs, zéro fichier temp
                    : Imgcodecs.imread(photoPath);    // ← chemin local classique

            if (refMat == null || refMat.empty()) {
                System.out.println("[FaceAuth] Photo de référence illisible : " + photoPath);
                return false;
            }

            Mat refGray = extractFaceGray(refMat);
            if (refGray == null) {
                System.out.println("[FaceAuth] Aucun visage dans la photo de référence.");
                return false;
            }

            double score = pearsonSimilarity(liveGray, refGray);
            System.out.printf("[FaceAuth] Similarité : %.3f  (seuil %.2f)%n", score, SIMILARITY_THRESHOLD);
            return score >= SIMILARITY_THRESHOLD;

        } catch (Exception e) {
            System.err.println("[FaceAuth] Erreur matchFace : " + e.getMessage());
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /**
     * Télécharge les bytes d'une image HTTP/HTTPS directement en mémoire
     * et les décode en Mat via Imgcodecs.imdecode() — aucun fichier temporaire créé.
     *
     * Pourquoi imdecode() et pas imread() ?
     *   imread()   → lit depuis le disque uniquement (fopen en C++)
     *   imdecode() → décode depuis un tableau de bytes en RAM
     */
    private Mat readMatFromUrl(String imageUrl) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(imageUrl).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8_000);
            conn.setReadTimeout(15_000);
            conn.setRequestProperty("User-Agent", "SmartPath-FaceAuth/1.0");

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                System.err.println("[FaceAuth] HTTP " + conn.getResponseCode() + " : " + imageUrl);
                conn.disconnect();
                return null;
            }

            // Lire tous les bytes du flux HTTP en mémoire
            byte[] imageBytes;
            try (InputStream in = conn.getInputStream();
                 ByteArrayOutputStream buf = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = in.read(chunk)) != -1) buf.write(chunk, 0, read);
                imageBytes = buf.toByteArray();
            }
            conn.disconnect();

            // Mettre les bytes dans un MatOfByte (vecteur OpenCV)
            // puis décoder en Mat BGR — exactement comme imread() mais depuis la RAM
            MatOfByte mob = new MatOfByte(imageBytes);
            Mat decoded = Imgcodecs.imdecode(mob, Imgcodecs.IMREAD_COLOR);
            mob.release();

            if (decoded.empty()) {
                System.err.println("[FaceAuth] imdecode() a retourné une Mat vide pour : " + imageUrl);
                return null;
            }

            System.out.println("[FaceAuth] Image lue en mémoire (" + imageBytes.length + " bytes) : " + imageUrl);
            return decoded;

        } catch (Exception e) {
            System.err.println("[FaceAuth] Erreur readMatFromUrl : " + e.getMessage());
            return null;
        }
    }

    /** Extrait le premier visage détecté et le retourne en niveaux de gris 100×100. */
    private Mat extractFaceGray(Mat src) {
        List<Rect> faces = detectFaces(src);
        if (faces.isEmpty()) return null;
        Rect r = faces.get(0);
        Mat face = new Mat(src, r);
        Mat gray = new Mat();
        if (face.channels() > 1) Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        else face.copyTo(gray);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(FACE_SIZE, FACE_SIZE));
        return resized;
    }

    /** Corrélation de Pearson normalisée entre deux images 100×100 niveaux de gris → [0,1]. */
    private double pearsonSimilarity(Mat a, Mat b) {
        int n = FACE_SIZE * FACE_SIZE;
        byte[] pa = new byte[n], pb = new byte[n];
        a.get(0, 0, pa); b.get(0, 0, pb);

        double sumA = 0, sumB = 0;
        for (int i = 0; i < n; i++) { sumA += pa[i] & 0xFF; sumB += pb[i] & 0xFF; }
        double meanA = sumA / n, meanB = sumB / n;

        double cov = 0, varA = 0, varB = 0;
        for (int i = 0; i < n; i++) {
            double da = (pa[i] & 0xFF) - meanA, db = (pb[i] & 0xFF) - meanB;
            cov  += da * db;
            varA += da * da;
            varB += db * db;
        }
        if (varA == 0 || varB == 0) return 0;
        double r = cov / Math.sqrt(varA * varB);
        return (r + 1.0) / 2.0;
    }

    public Mat drawFaceBoxes(Mat frame, List<Rect> faces) {
        Mat out = frame.clone();
        for (Rect r : faces)
            Imgproc.rectangle(out, r.tl(), r.br(), new Scalar(37, 99, 235), 2);
        return out;
    }
}
