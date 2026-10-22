package com.cmaisnam.amhub;

import java.util.ArrayList;
import java.util.List;

public class ProjectData {
    public String uid;
    public String pid;
    public String title = "Untitled Project";
    public long packageSize = 0;
    public String packageSizeFormatted = "0 B";
    public long downloadsCount = 0;
    public String platform = "Android";
    public String appVersion = "Latest";
    public String shareUrl;
    public String packageDownloadUrl;
    public String thumbUrl;

    public boolean hasVideo = false;
    public String videoStreamUrl;
    public String videoSpecs = "";

    public boolean hasAudio = false;
    public String audioStreamUrl;
    public String audioFileName = "";
    public String audioSpecs = "";

    public String xmlContent = "";
    public String xmlFileName = "preset.xml";
    public long xmlSize = 0;

    public boolean isFreeEligible = true;
    public String policyText = "Free Download";

    public List<MediaItem> mediaFiles = new ArrayList<>();
    public List<SceneItem> scenes = new ArrayList<>();
    public List<String> requiredEffects = new ArrayList<>();

    public static class MediaItem {
        public String fileName;
        public String type; // "video", "image", "audio", "file"
        public long size;
        public String sizeFormatted;
        public String downloadUrl;
        public String previewUrl;
        public String specs;
        public String codec;
        public String duration;

        public MediaItem(String fileName, String type, long size, String sizeFormatted, String downloadUrl, String previewUrl, String specs) {
            this.fileName = fileName;
            this.type = type;
            this.size = size;
            this.sizeFormatted = sizeFormatted;
            this.downloadUrl = downloadUrl;
            this.previewUrl = previewUrl;
            this.specs = specs;
        }
    }

    public static class SceneItem {
        public String title;
        public long size;
        public String sizeFormatted;
        public int index;
        public String xmlData;

        public SceneItem(String title, long size, String sizeFormatted, int index) {
            this.title = title;
            this.size = size;
            this.sizeFormatted = sizeFormatted;
            this.index = index;
        }
    }
}
