package com.cmaisnam.amhub;

import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class DownloadHelper {

    public interface DownloadCallback {
        void onSuccess(File file);
        void onError(Exception e);
    }

    public interface BitmapCallback {
        void onLoaded(Bitmap bitmap);
    }

    public static void downloadViaManager(Context context, String downloadUrl, String fileName, String title) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadUrl));
            request.setTitle(title != null ? title : fileName);
            request.setDescription("Downloading " + fileName);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AlightMotionHub/" + fileName);
            request.allowScanningByMediaScanner();

            DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                Toast.makeText(context, "Download started: " + fileName, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    public static void saveTextToDownloads(Context context, String content, String fileName) {
        new Thread(() -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                    values.put(MediaStore.MediaColumns.MIME_TYPE, "text/xml");
                    values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AlightMotionHub");

                    Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                    if (uri != null) {
                        try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                            out.write(content.getBytes("UTF-8"));
                            out.flush();
                        }
                        Toast.makeText(context, "Saved to Downloads/AlightMotionHub/" + fileName, Toast.LENGTH_LONG).show();
                        return;
                    }
                }

                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File hubDir = new File(downloadsDir, "AlightMotionHub");
                if (!hubDir.exists()) hubDir.mkdirs();

                File outFile = new File(hubDir, fileName);
                try (FileOutputStream fos = new FileOutputStream(outFile)) {
                    fos.write(content.getBytes("UTF-8"));
                    fos.flush();
                }
                Toast.makeText(context, "Saved to Downloads/AlightMotionHub/" + fileName, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(context, "Save error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }).start();
    }

    public static void extractToCache(Context context, String zipUrl, String targetFileName, DownloadCallback callback) {
        new Thread(() -> {
            try {
                File cacheDir = context.getCacheDir();
                File outFile = new File(cacheDir, targetFileName);

                if (outFile.exists() && outFile.length() > 0) {
                    if (callback != null) callback.onSuccess(outFile);
                    return;
                }

                URL url = new URL(zipUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(20000);
                conn.setReadTimeout(30000);

                if (conn.getResponseCode() != 200) {
                    throw new Exception("HTTP " + conn.getResponseCode());
                }

                InputStream is = conn.getInputStream();
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                byte[] buf = new byte[16384];
                boolean found = false;

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(targetFileName) || entry.getName().endsWith("/" + targetFileName)) {
                        File tempFile = new File(cacheDir, targetFileName + ".tmp");
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        int count;
                        while ((count = zis.read(buf)) != -1) {
                            fos.write(buf, 0, count);
                        }
                        fos.flush();
                        fos.close();
                        zis.closeEntry();
                        if (tempFile.renameTo(outFile)) {
                            found = true;
                        } else {
                            outFile = tempFile;
                            found = true;
                        }
                        break;
                    }
                    zis.closeEntry();
                }
                zis.close();

                if (found && outFile.exists()) {
                    if (callback != null) callback.onSuccess(outFile);
                } else {
                    throw new Exception("Asset " + targetFileName + " not found in package");
                }
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    public static void extractAndSaveAsset(Context context, String zipUrl, String targetFileName, DownloadCallback callback) {
        new Thread(() -> {
            try {
                // 1. Check if already extracted in local cache
                File cached = new File(context.getCacheDir(), targetFileName);
                if (cached.exists() && cached.length() > 0) {
                    saveFileToPublicDownloads(context, cached, targetFileName, callback);
                    return;
                }

                // 2. Otherwise extract to cache first, then export to public Downloads
                extractToCache(context, zipUrl, targetFileName, new DownloadCallback() {
                    @Override
                    public void onSuccess(File file) {
                        saveFileToPublicDownloads(context, file, targetFileName, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) callback.onError(e);
                    }
                });
            } catch (Exception e) {
                if (callback != null) callback.onError(e);
            }
        }).start();
    }

    private static void saveFileToPublicDownloads(Context context, File sourceFile, String targetFileName, DownloadCallback callback) {
        try {
            String mime = "application/octet-stream";
            String lower = targetFileName.toLowerCase();
            if (lower.endsWith(".mp4")) mime = "video/mp4";
            else if (lower.endsWith(".wav")) mime = "audio/wav";
            else if (lower.endsWith(".m4a")) mime = "audio/mp4";
            else if (lower.endsWith(".mp3")) mime = "audio/mpeg";
            else if (lower.endsWith(".png")) mime = "image/png";
            else if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) mime = "image/jpeg";
            else if (lower.endsWith(".xml")) mime = "text/xml";

            File savedFile = null;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, targetFileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AlightMotionHub");

                Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    try (InputStream in = new FileInputStream(sourceFile);
                         OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                        byte[] buf = new byte[16384];
                        int len;
                        while ((len = in.read(buf)) != -1) {
                            out.write(buf, 0, len);
                        }
                        out.flush();
                    }
                    savedFile = sourceFile;
                }
            }

            if (savedFile == null) {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File hubDir = new File(downloadsDir, "AlightMotionHub");
                if (!hubDir.exists()) hubDir.mkdirs();

                File outFile = new File(hubDir, targetFileName);
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[16384];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                    out.flush();
                }
                savedFile = outFile;
            }

            if (savedFile != null) {
                MediaScannerConnection.scanFile(context, new String[]{savedFile.getAbsolutePath()}, new String[]{mime}, null);
                if (callback != null) callback.onSuccess(savedFile);
            } else {
                throw new Exception("Failed to write file to Downloads");
            }
        } catch (Exception e) {
            if (callback != null) callback.onError(e);
        }
    }

    public static void loadBitmapFromZip(String zipUrl, String targetFileName, BitmapCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(zipUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);

                if (conn.getResponseCode() != 200) return;

                InputStream is = conn.getInputStream();
                ZipInputStream zis = new ZipInputStream(is);
                ZipEntry entry;
                byte[] buf = new byte[8192];

                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals(targetFileName) || entry.getName().endsWith("/" + targetFileName)) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        int count;
                        while ((count = zis.read(buf)) != -1) {
                            baos.write(buf, 0, count);
                        }
                        byte[] imgBytes = baos.toByteArray();
                        Bitmap bmp = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                        zis.closeEntry();
                        if (callback != null) callback.onLoaded(bmp);
                        break;
                    }
                    zis.closeEntry();
                }
                zis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static void loadBitmapFromUrl(String imgUrl, BitmapCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(imgUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                if (conn.getResponseCode() == 200) {
                    Bitmap bmp = BitmapFactory.decodeStream(conn.getInputStream());
                    if (callback != null) callback.onLoaded(bmp);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
