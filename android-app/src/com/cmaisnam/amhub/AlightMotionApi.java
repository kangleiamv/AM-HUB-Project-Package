package com.cmaisnam.amhub;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class AlightMotionApi {

    public static class ShareIds {
        public String uid;
        public String pid;

        public ShareIds(String uid, String pid) {
            this.uid = uid;
            this.pid = pid;
        }
    }

    public static ShareIds parseShareIds(String input) {
        if (input == null) return null;
        String trimmed = input.trim();

        // Pattern: /u/{uid}/p/{pid}
        Pattern pattern = Pattern.compile("/u/([^/?#]+)/p/([^/?#]+)");
        Matcher matcher = pattern.matcher(trimmed);
        if (matcher.find()) {
            return new ShareIds(matcher.group(1), matcher.group(2));
        }

        // Direct uid:pid format
        if (trimmed.contains(":") && !trimmed.startsWith("http")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 2) {
                return new ShareIds(parts[0].trim(), parts[1].trim());
            }
        }

        return null;
    }

    public static ProjectData fetchProjectMetadata(String uid, String pid) throws Exception {
        ProjectData project = new ProjectData();
        project.uid = uid;
        project.pid = pid;
        project.shareUrl = "https://alightcreative.com/am/share/u/" + uid + "/p/" + pid;

        // 1. Call Firebase Cloud Function getProjectMetadata
        URL apiUrl = new URL("https://us-central1-alight-creative.cloudfunctions.net/getProjectMetadata");
        HttpURLConnection conn = (HttpURLConnection) apiUrl.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(20000);

        JSONObject reqData = new JSONObject();
        JSONObject params = new JSONObject();
        params.put("uid", uid);
        params.put("pid", pid);
        reqData.put("data", params);

        OutputStream os = conn.getOutputStream();
        os.write(reqData.toString().getBytes("UTF-8"));
        os.flush();
        os.close();

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            throw new Exception("Cloud Function error (HTTP " + responseCode + ")");
        }

        String jsonStr = readStreamToString(conn.getInputStream());
        JSONObject rootObj = new JSONObject(jsonStr);
        JSONObject resultObj = rootObj.optJSONObject("result");
        JSONObject infoObj = resultObj != null ? resultObj.optJSONObject("info") : null;

        if (infoObj != null) {
            project.title = infoObj.optString("title", "Untitled Project");
            project.packageSize = infoObj.optLong("size", 0);
            project.packageSizeFormatted = formatBytes(project.packageSize);
            project.downloadsCount = infoObj.optLong("downloads", 0);
            project.platform = infoObj.optString("amPlatform", "Android");
            project.appVersion = infoObj.optString("amVersionString", "Latest");
            project.thumbUrl = infoObj.optString("largeThumbUrl",
                    infoObj.optString("medThumbUrl", infoObj.optString("smallThumbUrl", null)));

            long freeMax = resultObj.optLong("freeUserMaxDownloadSize", 5242880);
            project.isFreeEligible = project.packageSize <= freeMax;
            project.policyText = project.isFreeEligible ? "Free Download" : "PRO (Package > " + formatBytes(freeMax) + ")";

            // Parse required effects
            JSONArray effectsArr = infoObj.optJSONArray("requiredEffects");
            if (effectsArr != null) {
                for (int i = 0; i < effectsArr.length(); i++) {
                    String eff = effectsArr.optString(i);
                    String clean = eff.replace("com.alightcreative.effects.", "").replace("com.alightcreative.internal.", "");
                    project.requiredEffects.add(clean);
                }
            }

            // Parse scenes
            JSONArray scenesArr = infoObj.optJSONArray("projects");
            if (scenesArr != null) {
                for (int i = 0; i < scenesArr.length(); i++) {
                    JSONObject sc = scenesArr.optJSONObject(i);
                    if (sc != null) {
                        String stitle = sc.optString("title", "Scene " + (i + 1));
                        long ssize = sc.optLong("size", 0);
                        project.scenes.add(new ProjectData.SceneItem(stitle, ssize, formatBytes(ssize), i));
                    }
                }
            }
        }

        // 2. Fetch Firebase Storage Token for projectfiles.zip
        String storagePath = "share/u/" + uid + "/p/" + pid + "/projectfiles.zip";
        String metaUrlStr = "https://firebasestorage.googleapis.com/v0/b/alight-creative.appspot.com/o/" + URLEncoder.encode(storagePath, "UTF-8");
        URL metaUrl = new URL(metaUrlStr);
        HttpURLConnection metaConn = (HttpURLConnection) metaUrl.openConnection();
        metaConn.setConnectTimeout(15000);
        metaConn.setReadTimeout(15000);

        String downloadToken = null;
        if (metaConn.getResponseCode() == 200) {
            String metaResStr = readStreamToString(metaConn.getInputStream());
            JSONObject storageJson = new JSONObject(metaResStr);
            downloadToken = storageJson.optString("downloadTokens", null);
        }

        if (downloadToken != null && !downloadToken.isEmpty()) {
            project.packageDownloadUrl = metaUrlStr + "?alt=media&token=" + downloadToken;

            // 3. Inspect ZIP entries and extract XML scenes & media assets
            inspectZipArchive(project, project.packageDownloadUrl);
        }

        return project;
    }

    private static void inspectZipArchive(ProjectData project, String zipDownloadUrl) {
        try {
            URL zipUrl = new URL(zipDownloadUrl);
            HttpURLConnection zipConn = (HttpURLConnection) zipUrl.openConnection();
            zipConn.setConnectTimeout(20000);
            zipConn.setReadTimeout(30000);

            if (zipConn.getResponseCode() != 200) return;

            InputStream is = zipConn.getInputStream();
            ZipInputStream zis = new ZipInputStream(is);
            ZipEntry entry;

            Map<String, ProjectData.MediaItem> foundMedia = new HashMap<>();
            String firstXmlContent = null;
            String firstXmlName = null;

            byte[] buffer = new byte[8192];

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                String lower = name.toLowerCase();

                if (lower.endsWith(".xml")) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int count;
                    while ((count = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, count);
                    }
                    String xmlString = baos.toString("UTF-8");

                    if (firstXmlContent == null) {
                        firstXmlContent = xmlString;
                        firstXmlName = name;
                    }

                    // Parse <media> elements from XML for duration & metadata
                    parseXmlMediaElements(xmlString, foundMedia);

                } else if (!lower.endsWith("manifest.txt")) {
                    // Check media assets
                    String ext = "";
                    int dotIdx = lower.lastIndexOf('.');
                    if (dotIdx > 0) ext = lower.substring(dotIdx);

                    boolean isVid = ext.equals(".mp4") || ext.equals(".webm") || ext.equals(".mov");
                    boolean isImg = ext.equals(".png") || ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".webp");
                    boolean isAud = ext.equals(".wav") || ext.equals(".m4a") || ext.equals(".mp3") || ext.equals(".ogg") || ext.equals(".aac");

                    if (isVid || isImg || isAud) {
                        long size = entry.getSize();
                        String type = isVid ? "video" : (isImg ? "image" : "audio");
                        
                        ProjectData.MediaItem item = foundMedia.get(name);
                        if (item == null) {
                            String specs = formatBytes(size > 0 ? size : 0);
                            item = new ProjectData.MediaItem(name, type, size, specs, "", "", specs);
                            foundMedia.put(name, item);
                        } else {
                            if (size > 0 && item.size <= 0) {
                                item.size = size;
                                item.sizeFormatted = formatBytes(size);
                            }
                        }
                    }
                }
                zis.closeEntry();
            }
            zis.close();

            if (firstXmlContent != null) {
                project.xmlContent = firstXmlContent;
                project.xmlFileName = firstXmlName != null ? firstXmlName : "preset.xml";
                project.xmlSize = firstXmlContent.getBytes("UTF-8").length;
            }

            // Add all discovered media items to project
            for (ProjectData.MediaItem item : foundMedia.values()) {
                project.mediaFiles.add(item);

                if ("video".equals(item.type) && !project.hasVideo) {
                    project.hasVideo = true;
                    project.videoSpecs = item.specs != null ? item.specs : item.sizeFormatted;
                } else if ("audio".equals(item.type) && !project.hasAudio) {
                    project.hasAudio = true;
                    project.audioFileName = item.fileName;
                    project.audioSpecs = item.specs != null ? item.specs : item.sizeFormatted;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void parseXmlMediaElements(String xmlStr, Map<String, ProjectData.MediaItem> map) {
        try {
            Pattern p = Pattern.compile("<media\\s+([^>]+)/?>", Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(xmlStr);
            while (m.find()) {
                String attrs = m.group(1);
                String fn = extractAttr(attrs, "filename");
                if (fn == null) fn = extractAttr(attrs, "uri");
                if (fn != null && fn.startsWith("amproj:")) fn = fn.substring(7);

                if (fn != null && !fn.isEmpty()) {
                    String typeStr = extractAttr(attrs, "type");
                    String durStr = extractAttr(attrs, "duration");
                    String sizeStr = extractAttr(attrs, "size");

                    String lower = fn.toLowerCase();
                    boolean isVid = lower.endsWith(".mp4") || (typeStr != null && typeStr.contains("video"));
                    boolean isAud = lower.endsWith(".wav") || lower.endsWith(".m4a") || lower.endsWith(".mp3") || (typeStr != null && typeStr.contains("audio"));
                    boolean isImg = lower.endsWith(".png") || lower.endsWith(".jpg") || (typeStr != null && typeStr.contains("image"));

                    long size = sizeStr != null ? parseLongSafe(sizeStr) : 0;
                    String durationSec = "";
                    if (durStr != null) {
                        long us = parseLongSafe(durStr);
                        if (us > 0) durationSec = String.format("%.1fs", us / 1000000.0);
                    }

                    String itemType = isVid ? "video" : (isImg ? "image" : (isAud ? "audio" : "file"));
                    String specs = durationSec.isEmpty() ? formatBytes(size) : durationSec + " • " + formatBytes(size);

                    ProjectData.MediaItem item = map.get(fn);
                    if (item == null) {
                        item = new ProjectData.MediaItem(fn, itemType, size, formatBytes(size), "", "", specs);
                        item.duration = durationSec;
                        map.put(fn, item);
                    } else {
                        if (!durationSec.isEmpty()) item.duration = durationSec;
                        if (size > 0) {
                            item.size = size;
                            item.sizeFormatted = formatBytes(size);
                        }
                        item.specs = specs;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractAttr(String text, String attrName) {
        Pattern p = Pattern.compile(attrName + "=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        return m.find() ? m.group(1) : null;
    }

    private static long parseLongSafe(String str) {
        try {
            return Long.parseLong(str.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private static String readStreamToString(InputStream is) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = is.read(buf)) != -1) {
            baos.write(buf, 0, len);
        }
        return baos.toString("UTF-8");
    }
}
