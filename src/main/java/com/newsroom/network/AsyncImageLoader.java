package com.newsroom.network;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Asynchronous Remote Image Loader with Memory LRU Caching and Automatic Aspect Scaling.
 * Decouples image networking from the Swing EDT to maintain fluid UI responsiveness.
 * Demonstrates Concurrency Management, Image Processing, and Caching Strategy.
 */
public class AsyncImageLoader {
    private static final int MAX_CACHE_SIZE = 150;
    private static volatile AsyncImageLoader instance;

    private final Map<String, ImageIcon> memoryCache;
    private final Map<String, List<ImageLoadCallback>> pendingRequests = new HashMap<>();
    private final ExecutorService executor;

    public interface ImageLoadCallback {
        void onImageLoaded(ImageIcon image);
    }

    private AsyncImageLoader() {
        this.memoryCache = Collections.synchronizedMap(new LinkedHashMap<String, ImageIcon>(MAX_CACHE_SIZE, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ImageIcon> eldest) {
                return size() > MAX_CACHE_SIZE;
            }
        });

        this.executor = Executors.newFixedThreadPool(6, r -> {
            Thread t = new Thread(r, "ImageLoader-Worker");
            t.setDaemon(true);
            return t;
        });
    }

    public static AsyncImageLoader getInstance() {
        if (instance == null) {
            synchronized (AsyncImageLoader.class) {
                if (instance == null) {
                    instance = new AsyncImageLoader();
                }
            }
        }
        return instance;
    }

    public void loadImage(String imageUrl, ImageLoadCallback callback) {
        loadImage(imageUrl, 0, 0, callback);
    }

    /**
     * Loads remote image asynchronously and delivers concrete BufferedImage ImageIcon to callback on Swing EDT.
     */
    public void loadImage(String imageUrl, int targetWidth, int targetHeight, ImageLoadCallback callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty() || !imageUrl.startsWith("http")) {
            if (callback != null) {
                if (SwingUtilities.isEventDispatchThread()) callback.onImageLoaded(null);
                else SwingUtilities.invokeLater(() -> callback.onImageLoaded(null));
            }
            return;
        }

        final String cleanUrl = imageUrl.trim();

        // Check in-memory cache first
        ImageIcon cached = memoryCache.get(cleanUrl);
        if (cached != null) {
            if (callback != null) {
                if (SwingUtilities.isEventDispatchThread()) {
                    callback.onImageLoaded(cached);
                } else {
                    SwingUtilities.invokeLater(() -> callback.onImageLoaded(cached));
                }
            }
            return;
        }

        // Deduplicate in-flight requests for the same URL
        synchronized (pendingRequests) {
            if (pendingRequests.containsKey(cleanUrl)) {
                if (callback != null) {
                    pendingRequests.get(cleanUrl).add(callback);
                }
                return;
            }

            List<ImageLoadCallback> list = new ArrayList<>();
            if (callback != null) {
                list.add(callback);
            }
            pendingRequests.put(cleanUrl, list);
        }

        executor.submit(() -> {
            ImageIcon resultIcon = null;
            try {
                byte[] bytes = HttpClientManager.getInstance().fetchBytes(cleanUrl);
                if (bytes != null && bytes.length > 0) {
                    ImageIcon rawIcon = new ImageIcon(bytes);
                    Image rawImg = rawIcon.getImage();

                    int srcW = rawIcon.getIconWidth();
                    int srcH = rawIcon.getIconHeight();

                    if (srcW > 0 && srcH > 0) {
                        int maxDim = 1200;
                        int outW = srcW;
                        int outH = srcH;
                        if (outW > maxDim || outH > maxDim) {
                            double factor = Math.min((double) maxDim / outW, (double) maxDim / outH);
                            outW = Math.max(1, (int) Math.round(outW * factor));
                            outH = Math.max(1, (int) Math.round(outH * factor));
                        }

                        BufferedImage decoded = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
                        Graphics2D g2 = decoded.createGraphics();
                        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.drawImage(rawImg, 0, 0, outW, outH, null);
                        g2.dispose();

                        resultIcon = new ImageIcon(decoded);
                        memoryCache.put(cleanUrl, resultIcon);
                    }
                }
            } catch (Exception ignored) {
                // Network failure or invalid image format: fallback to null
            }

            final ImageIcon finalIcon = resultIcon;
            final List<ImageLoadCallback> callbacksToNotify;
            synchronized (pendingRequests) {
                callbacksToNotify = pendingRequests.remove(cleanUrl);
            }

            if (callbacksToNotify != null && !callbacksToNotify.isEmpty()) {
                SwingUtilities.invokeLater(() -> {
                    for (ImageLoadCallback cb : callbacksToNotify) {
                        try {
                            cb.onImageLoaded(finalIcon);
                        } catch (Exception e) {
                            System.err.println("Notice: Exception in ImageLoadCallback: " + e.getMessage());
                        }
                    }
                });
            }
        });
    }

    public void clearCache() {
        memoryCache.clear();
    }
}
