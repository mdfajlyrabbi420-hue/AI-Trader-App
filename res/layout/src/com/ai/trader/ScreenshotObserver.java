package com.ai.trader;

import android.os.FileObserver;
import java.io.File;

public class ScreenshotObserver extends FileObserver {
    
    public interface OnScreenshotDetectedListener {
        void onScreenshotDetected(String path);
    }

    private String folderPath;
    private OnScreenshotDetectedListener listener;

    public ScreenshotObserver(String path, OnScreenshotDetectedListener listener) {
        super(path, FileObserver.CLOSE_WRITE);
        this.folderPath = path;
        this.listener = listener;
    }

    @Override
    public void onEvent(int event, String path) {
        if (path != null && (path.toLowerCase().endsWith(".png") || 
                             path.toLowerCase().endsWith(".jpg") || 
                             path.toLowerCase().endsWith(".jpeg"))) {
            
            String fullPath = folderPath + File.separator + path;
            if (listener != null) {
                listener.onScreenshotDetected(fullPath);
            }
        }
    }
}
