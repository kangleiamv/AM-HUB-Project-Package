package com.cmaisnam.amhub;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static final String PREFS_NAME = "amhub_prefs";
    private static final String KEY_RECENTS = "recent_presets";

    private EditText inputUrl;
    private Button btnPaste;
    private Button btnInspect;
    private ProgressBar progressLoading;
    private TextView textError;

    // Recent Presets
    private LinearLayout recentStrip;
    private LinearLayout recentChipsContainer;
    private TextView btnClearRecent;

    // Results Section
    private LinearLayout resultsSection;
    private TextView heroTitle;
    private TextView heroPolicyBadge;
    private TextView heroSubtitle;
    private TextView statSize, statDownloads, statPlatform, statVersion;
    private Button btnDownloadZip, btnCopyZip, btnOpenAm;

    // Tabs
    private TextView tabPreview, tabMedia, tabXml, tabDetails;
    private LinearLayout containerPreview, containerMedia, containerXml, containerDetails;
    private String currentTab = "preview";

    // Preview Tab Views
    private LinearLayout cardVideoPreview;
    private TextView videoSpecs;
    private VideoView videoView;
    private Button btnPlayVideo;

    // Advanced Custom Video Player
    private View videoPlayerFrame;
    private ImageView videoPoster;
    private View videoCenterOverlay;
    private Button btnVideoCenterPlay;
    private ProgressBar videoBufferingSpinner;
    private LinearLayout videoControlsBar;
    private SeekBar videoSeekbar;
    private Button btnVideoPlayToggle, btnVideoRewind, btnVideoForward, btnVideoReplay;
    private TextView videoTimeDisplay;
    private Handler videoHandler = new Handler(Looper.getMainLooper());
    private boolean isVideoScrubbing = false;
    private boolean isVideoPrepared = false;

    private LinearLayout cardAudioPreview;
    private TextView audioBadge, audioFilename, audioTime;
    private Button btnAudioPlayPause, btnDownloadAudioTrack;
    private SeekBar audioSeekbar;

    private ImageView imgThumbnail;
    private Button btnDownloadThumb;

    // Media Tab Views
    private TextView filterAll, filterVideo, filterImage, filterAudio;
    private LinearLayout mediaItemsLayout;
    private String currentFilter = "all";

    // XML Tab Views
    private TextView xmlFilename, xmlContentView;
    private Button btnCopyXml, btnDownloadXml;

    // Details Tab Views
    private LinearLayout scenesLayout;
    private TextView effectsText;

    // Current State
    private ProjectData currentProject;
    private MediaPlayer mediaPlayer;
    private Handler audioHandler = new Handler(Looper.getMainLooper());
    private boolean isAudioPlaying = false;

    private MediaPlayer listAudioPlayer;
    private Button activeListAudioBtn;
    private VideoView activeListVideoView;
    private Button activeListVideoBtn;

    private void stopAnyPlayback() {
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
            } catch (Exception ignored) {}
            isAudioPlaying = false;
            if (btnAudioPlayPause != null) btnAudioPlayPause.setText("▶");
        }
        if (listAudioPlayer != null) {
            try {
                if (listAudioPlayer.isPlaying()) listAudioPlayer.stop();
                listAudioPlayer.release();
            } catch (Exception ignored) {}
            listAudioPlayer = null;
        }
        if (activeListAudioBtn != null) {
            activeListAudioBtn.setText("▶");
            activeListAudioBtn = null;
        }
        if (activeListVideoView != null) {
            try {
                if (activeListVideoView.isPlaying()) activeListVideoView.stopPlayback();
            } catch (Exception ignored) {}
            activeListVideoView = null;
        }
        if (activeListVideoBtn != null) {
            activeListVideoBtn.setText("▶ Stream Video");
            activeListVideoBtn = null;
        }
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
            if (btnPlayVideo != null) btnPlayVideo.setText("▶ Play Video Preview");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        setupListeners();
        loadRecentPresets();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action) && intent.getData() != null) {
            String url = intent.getData().toString();
            inputUrl.setText(url);
            inspectUrl(url);
        } else if (Intent.ACTION_SEND.equals(action) && intent.hasExtra(Intent.EXTRA_TEXT)) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null && text.contains("alight")) {
                inputUrl.setText(text.trim());
                inspectUrl(text.trim());
            }
        }
    }

    private void bindViews() {
        inputUrl = findViewById(R.id.input_url);
        btnPaste = findViewById(R.id.btn_paste);
        btnInspect = findViewById(R.id.btn_inspect);
        progressLoading = findViewById(R.id.progress_loading);
        textError = findViewById(R.id.text_error);

        recentStrip = findViewById(R.id.recent_strip);
        recentChipsContainer = findViewById(R.id.recent_chips_container);
        btnClearRecent = findViewById(R.id.btn_clear_recent);

        resultsSection = findViewById(R.id.results_section);
        heroTitle = findViewById(R.id.hero_title);
        heroPolicyBadge = findViewById(R.id.hero_policy_badge);
        heroSubtitle = findViewById(R.id.hero_subtitle);

        statSize = findViewById(R.id.stat_size);
        statDownloads = findViewById(R.id.stat_downloads);
        statPlatform = findViewById(R.id.stat_platform);
        statVersion = findViewById(R.id.stat_version);

        btnDownloadZip = findViewById(R.id.btn_download_zip);
        btnCopyZip = findViewById(R.id.btn_copy_zip);
        btnOpenAm = findViewById(R.id.btn_open_am);

        tabPreview = findViewById(R.id.tab_preview);
        tabMedia = findViewById(R.id.tab_media);
        tabXml = findViewById(R.id.tab_xml);
        tabDetails = findViewById(R.id.tab_details);

        containerPreview = findViewById(R.id.container_tab_preview);
        containerMedia = findViewById(R.id.container_tab_media);
        containerXml = findViewById(R.id.container_tab_xml);
        containerDetails = findViewById(R.id.container_tab_details);

        cardVideoPreview = findViewById(R.id.card_video_preview);
        videoSpecs = findViewById(R.id.video_specs);
        videoView = findViewById(R.id.video_view);
        btnPlayVideo = findViewById(R.id.btn_play_video);

        videoPlayerFrame = findViewById(R.id.video_player_frame);
        videoPoster = findViewById(R.id.video_poster);
        videoCenterOverlay = findViewById(R.id.video_center_overlay);
        btnVideoCenterPlay = findViewById(R.id.btn_video_center_play);
        videoBufferingSpinner = findViewById(R.id.video_buffering_spinner);
        videoControlsBar = findViewById(R.id.video_controls_bar);
        videoSeekbar = findViewById(R.id.video_seekbar);
        btnVideoPlayToggle = findViewById(R.id.btn_video_play_toggle);
        btnVideoRewind = findViewById(R.id.btn_video_rewind);
        btnVideoForward = findViewById(R.id.btn_video_forward);
        videoTimeDisplay = findViewById(R.id.video_time_display);
        btnVideoReplay = findViewById(R.id.btn_video_replay);

        cardAudioPreview = findViewById(R.id.card_audio_preview);
        audioBadge = findViewById(R.id.audio_badge);
        audioFilename = findViewById(R.id.audio_filename);
        audioTime = findViewById(R.id.audio_time);
        btnAudioPlayPause = findViewById(R.id.btn_audio_play_pause);
        btnDownloadAudioTrack = findViewById(R.id.btn_download_audio_track);
        audioSeekbar = findViewById(R.id.audio_seekbar);

        imgThumbnail = findViewById(R.id.img_thumbnail);
        btnDownloadThumb = findViewById(R.id.btn_download_thumb);

        filterAll = findViewById(R.id.filter_all);
        filterVideo = findViewById(R.id.filter_video);
        filterImage = findViewById(R.id.filter_image);
        filterAudio = findViewById(R.id.filter_audio);
        mediaItemsLayout = findViewById(R.id.media_items_layout);

        xmlFilename = findViewById(R.id.xml_filename);
        xmlContentView = findViewById(R.id.xml_content_view);
        btnCopyXml = findViewById(R.id.btn_copy_xml);
        btnDownloadXml = findViewById(R.id.btn_download_xml);

        scenesLayout = findViewById(R.id.scenes_layout);
        effectsText = findViewById(R.id.effects_text);
    }

    private void setupListeners() {
        btnPaste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null && text.length() > 0) {
                    inputUrl.setText(text.toString().trim());
                    inspectUrl(text.toString().trim());
                }
            }
        });

        btnInspect.setOnClickListener(v -> {
            String val = inputUrl.getText().toString().trim();
            if (!val.isEmpty()) {
                inspectUrl(val);
            }
        });

        btnClearRecent.setOnClickListener(v -> {
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit().remove(KEY_RECENTS).apply();
            loadRecentPresets();
            Toast.makeText(this, "Recent presets cleared", Toast.LENGTH_SHORT).show();
        });

        tabPreview.setOnClickListener(v -> switchTab("preview"));
        tabMedia.setOnClickListener(v -> switchTab("media"));
        tabXml.setOnClickListener(v -> switchTab("xml"));
        tabDetails.setOnClickListener(v -> switchTab("details"));

        filterAll.setOnClickListener(v -> setMediaFilter("all"));
        filterVideo.setOnClickListener(v -> setMediaFilter("video"));
        filterImage.setOnClickListener(v -> setMediaFilter("image"));
        filterAudio.setOnClickListener(v -> setMediaFilter("audio"));

        btnCopyXml.setOnClickListener(v -> {
            if (currentProject != null && currentProject.xmlContent != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("AM XML Preset", currentProject.xmlContent);
                if (cm != null) {
                    cm.setPrimaryClip(clip);
                    Toast.makeText(this, "XML copied to clipboard!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDownloadXml.setOnClickListener(v -> {
            if (currentProject != null && currentProject.xmlContent != null) {
                DownloadHelper.saveTextToDownloads(this, currentProject.xmlContent, currentProject.xmlFileName);
            }
        });

        btnDownloadZip.setOnClickListener(v -> {
            if (currentProject != null && currentProject.packageDownloadUrl != null) {
                DownloadHelper.downloadViaManager(this, currentProject.packageDownloadUrl, currentProject.pid + ".zip", currentProject.title);
            }
        });

        btnCopyZip.setOnClickListener(v -> {
            if (currentProject != null && currentProject.packageDownloadUrl != null) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("ZIP Download Link", currentProject.packageDownloadUrl);
                if (cm != null) {
                    cm.setPrimaryClip(clip);
                    Toast.makeText(this, "ZIP link copied!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnOpenAm.setOnClickListener(v -> {
            if (currentProject != null && currentProject.shareUrl != null) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(currentProject.shareUrl));
                try {
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Could not open link: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDownloadThumb.setOnClickListener(v -> {
            if (currentProject != null && currentProject.thumbUrl != null) {
                DownloadHelper.downloadViaManager(this, currentProject.thumbUrl, currentProject.pid + "_thumb.jpg", currentProject.title + " Thumbnail");
            }
        });
    }

    private void inspectUrl(String url) {
        AlightMotionApi.ShareIds ids = AlightMotionApi.parseShareIds(url);
        if (ids == null) {
            showError("Invalid Alight Motion share link format");
            return;
        }

        hideError();
        setLoading(true);
        stopAudioPlayer();

        new Thread(() -> {
            try {
                ProjectData data = AlightMotionApi.fetchProjectMetadata(ids.uid, ids.pid);
                runOnUiThread(() -> {
                    currentProject = data;
                    renderProject(data);
                    saveRecentPreset(data, url);
                    setLoading(false);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError("Inspection failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void renderProject(ProjectData data) {
        heroTitle.setText(data.title);
        heroSubtitle.setText("UID: " + (data.uid.length() > 14 ? data.uid.substring(0, 14) + "..." : data.uid) + " • " + data.pid);
        heroPolicyBadge.setText(data.policyText);

        statSize.setText(data.packageSizeFormatted);
        statDownloads.setText(String.valueOf(data.downloadsCount));
        statPlatform.setText(data.platform);
        statVersion.setText(data.appVersion.split(" ")[0]);

        // Thumbnail
        if (data.thumbUrl != null && !data.thumbUrl.isEmpty()) {
            DownloadHelper.loadBitmapFromUrl(data.thumbUrl, bmp -> runOnUiThread(() -> {
                if (bmp != null) imgThumbnail.setImageBitmap(bmp);
            }));
        }

        // Video Player Card with Custom Advanced Controls
        if (data.hasVideo) {
            cardVideoPreview.setVisibility(View.VISIBLE);
            videoSpecs.setText(data.videoSpecs);

            // Find video asset name
            String vName = null;
            for (ProjectData.MediaItem m : data.mediaFiles) {
                if ("video".equals(m.type)) {
                    vName = m.fileName;
                    break;
                }
            }
            final String videoFileName = vName;

            setupCustomVideoPlayerControls(data.packageDownloadUrl, videoFileName, data.thumbUrl);

            // Download video button
            btnPlayVideo.setText("⬇ Download Video (.MP4)");
            btnPlayVideo.setOnClickListener(v -> {
                if (videoFileName != null && data.packageDownloadUrl != null) {
                    Toast.makeText(this, "Downloading video " + videoFileName + "...", Toast.LENGTH_SHORT).show();
                    DownloadHelper.extractAndSaveAsset(this, data.packageDownloadUrl, videoFileName, new DownloadHelper.DownloadCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved: " + file.getName(), Toast.LENGTH_LONG).show());
                        }
                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
                }
            });
        } else {
            cardVideoPreview.setVisibility(View.GONE);
            if (videoView != null && videoView.isPlaying()) {
                videoView.stopPlayback();
            }
        }

        // Audio Track Card (.WAV / .M4A)
        if (data.hasAudio) {
            cardAudioPreview.setVisibility(View.VISIBLE);
            audioFilename.setText(data.audioFileName);
            String ext = data.audioFileName.substring(data.audioFileName.lastIndexOf('.') + 1).toUpperCase();
            audioBadge.setText(ext);

            btnDownloadAudioTrack.setText("⬇ Download " + data.audioFileName + " (" + data.audioSpecs + ")");
            btnDownloadAudioTrack.setOnClickListener(v -> {
                Toast.makeText(this, "Extracting audio " + data.audioFileName + "...", Toast.LENGTH_SHORT).show();
                DownloadHelper.extractAndSaveAsset(this, data.packageDownloadUrl, data.audioFileName, new DownloadHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File file) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved: " + file.getName(), Toast.LENGTH_LONG).show());
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Audio save error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            });

            setupAudioControls(data.packageDownloadUrl, data.audioFileName);
        } else {
            cardAudioPreview.setVisibility(View.GONE);
        }

        // XML Content View
        xmlFilename.setText(data.xmlFileName);
        xmlContentView.setText(data.xmlContent.isEmpty() ? "No XML preset bundled" : data.xmlContent);

        // Details / Scenes
        scenesLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ProjectData.SceneItem scene : data.scenes) {
            View row = inflater.inflate(R.layout.item_scene, scenesLayout, false);
            TextView stitle = row.findViewById(R.id.scene_title);
            TextView ssize = row.findViewById(R.id.scene_size);
            Button sbtn = row.findViewById(R.id.btn_scene_download);

            stitle.setText(scene.title);
            ssize.setText(scene.sizeFormatted);
            sbtn.setOnClickListener(v -> {
                DownloadHelper.saveTextToDownloads(this, data.xmlContent, scene.title.replaceAll("[^a-zA-Z0-9.-]", "_") + ".xml");
            });
            scenesLayout.addView(row);
        }

        // Effects
        StringBuilder sb = new StringBuilder();
        for (String eff : data.requiredEffects) {
            if (sb.length() > 0) sb.append(" • ");
            sb.append(eff);
        }
        effectsText.setText(sb.length() > 0 ? sb.toString() : "Standard Motion Plugins");

        // Render Media Assets List
        renderMediaAssets(data.mediaFiles, currentFilter);

        resultsSection.setVisibility(View.VISIBLE);
        switchTab("preview");
    }

    private void renderMediaAssets(List<ProjectData.MediaItem> items, String filter) {
        mediaItemsLayout.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        int count = 0;
        for (ProjectData.MediaItem item : items) {
            if (!filter.equals("all") && !item.type.equals(filter)) {
                continue;
            }
            count++;
            View card = inflater.inflate(R.layout.item_media_asset, mediaItemsLayout, false);
            TextView badge = card.findViewById(R.id.item_type_badge);
            TextView name = card.findViewById(R.id.item_filename);
            TextView specs = card.findViewById(R.id.item_specs);
            TextView size = card.findViewById(R.id.item_size);
            ImageView imgPreview = card.findViewById(R.id.item_image_preview);
            Button btnDownload = card.findViewById(R.id.item_btn_download);

            name.setText(item.fileName);
            size.setText(item.sizeFormatted);
            specs.setText(item.specs != null && !item.specs.isEmpty() ? item.specs : item.sizeFormatted);

            if ("image".equals(item.type)) {
                badge.setText("🖼️ IMAGE");
                btnDownload.setText("⬇ Download Image");
                imgPreview.setVisibility(View.VISIBLE);

                // Load inline preview bitmap from the zip package!
                DownloadHelper.loadBitmapFromZip(currentProject.packageDownloadUrl, item.fileName, bmp -> runOnUiThread(() -> {
                    if (bmp != null) {
                        imgPreview.setImageBitmap(bmp);
                        // Tap to open full-screen lightbox
                        imgPreview.setOnClickListener(v -> {
                            ImagePreviewDialog.show(MainActivity.this, bmp, item.fileName, () -> {
                                DownloadHelper.extractAndSaveAsset(MainActivity.this, currentProject.packageDownloadUrl, item.fileName, null);
                            });
                        });
                    }
                }));

            } else if ("audio".equals(item.type)) {
                String ext = item.fileName.substring(item.fileName.lastIndexOf('.') + 1).toUpperCase();
                badge.setText("🎵 " + ext);
                btnDownload.setText("⬇ Download Audio (." + ext + ")");

                // Inline audio streaming without saving to downloads
                LinearLayout audioControls = card.findViewById(R.id.item_audio_controls);
                Button audioPlayBtn = card.findViewById(R.id.item_audio_play);
                SeekBar audioSeekBar = card.findViewById(R.id.item_audio_seekbar);
                TextView audioTimeTxt = card.findViewById(R.id.item_audio_time);
                audioControls.setVisibility(View.VISIBLE);

                audioPlayBtn.setOnClickListener(v -> {
                    if (listAudioPlayer != null && activeListAudioBtn == audioPlayBtn) {
                        if (listAudioPlayer.isPlaying()) {
                            listAudioPlayer.pause();
                            audioPlayBtn.setText("▶");
                        } else {
                            listAudioPlayer.start();
                            audioPlayBtn.setText("⏸");
                        }
                        return;
                    }

                    stopAnyPlayback();
                    audioPlayBtn.setText("⏳");
                    activeListAudioBtn = audioPlayBtn;
                    Toast.makeText(MainActivity.this, "Streaming " + item.fileName + " (no download)...", Toast.LENGTH_SHORT).show();

                    DownloadHelper.extractToCache(MainActivity.this, currentProject.packageDownloadUrl, item.fileName, new DownloadHelper.DownloadCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> {
                                try {
                                    listAudioPlayer = new MediaPlayer();
                                    listAudioPlayer.setDataSource(file.getAbsolutePath());
                                    listAudioPlayer.prepare();
                                    listAudioPlayer.start();
                                    audioPlayBtn.setText("⏸");
                                    audioSeekBar.setMax(listAudioPlayer.getDuration());

                                    listAudioPlayer.setOnCompletionListener(mp -> {
                                        audioPlayBtn.setText("▶");
                                        audioSeekBar.setProgress(0);
                                        activeListAudioBtn = null;
                                    });

                                    final Handler h = new Handler(Looper.getMainLooper());
                                    h.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (listAudioPlayer != null && listAudioPlayer.isPlaying() && activeListAudioBtn == audioPlayBtn) {
                                                int p = listAudioPlayer.getCurrentPosition();
                                                audioSeekBar.setProgress(p);
                                                int s = p / 1000;
                                                audioTimeTxt.setText(String.format("%d:%02d", s / 60, s % 60));
                                                h.postDelayed(this, 300);
                                            }
                                        }
                                    });
                                } catch (Exception e) {
                                    audioPlayBtn.setText("▶");
                                    Toast.makeText(MainActivity.this, "Audio stream error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                audioPlayBtn.setText("▶");
                                Toast.makeText(MainActivity.this, "Stream error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                });

            } else if ("video".equals(item.type)) {
                badge.setText("🎬 VIDEO");
                btnDownload.setText("⬇ Download Video (.MP4)");

                // Same Advanced Custom Video Player Frame on Media card
                FrameLayout videoFrame = card.findViewById(R.id.item_video_frame);
                VideoView itemVideoView = card.findViewById(R.id.item_video_view);
                ImageView itemVideoPoster = card.findViewById(R.id.item_video_poster);
                Button itemBtnCenterPlay = card.findViewById(R.id.item_btn_center_play);
                ProgressBar itemVideoSpinner = card.findViewById(R.id.item_video_spinner);
                LinearLayout itemControlsBar = card.findViewById(R.id.item_video_controls_bar);
                SeekBar itemVideoSeekbar = card.findViewById(R.id.item_video_seekbar);
                Button itemBtnPlayToggle = card.findViewById(R.id.item_btn_play_toggle);
                Button itemBtnRewind = card.findViewById(R.id.item_btn_rewind);
                Button itemBtnForward = card.findViewById(R.id.item_btn_forward);
                TextView itemVideoTime = card.findViewById(R.id.item_video_time);
                Button itemBtnReplay = card.findViewById(R.id.item_btn_replay);

                videoFrame.setVisibility(View.VISIBLE);

                if (currentProject.thumbUrl != null && !currentProject.thumbUrl.isEmpty()) {
                    DownloadHelper.loadBitmapFromUrl(currentProject.thumbUrl, bmp -> runOnUiThread(() -> {
                        if (bmp != null) itemVideoPoster.setImageBitmap(bmp);
                    }));
                }

                // Tap frame to toggle controls
                videoFrame.setOnClickListener(v -> {
                    itemControlsBar.setVisibility(itemControlsBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                });

                final boolean[] isPrepared = new boolean[]{false};
                final boolean[] isScrubbing = new boolean[]{false};
                final Handler itemVideoHandler = new Handler(Looper.getMainLooper());

                final Runnable progressUpdater = new Runnable() {
                    @Override
                    public void run() {
                        if (itemVideoView.isPlaying()) {
                            if (!isScrubbing[0]) {
                                int pos = itemVideoView.getCurrentPosition();
                                int dur = itemVideoView.getDuration();
                                itemVideoSeekbar.setProgress(pos);
                                itemVideoTime.setText(formatTime(pos) + " / " + formatTime(dur));
                            }
                            itemVideoHandler.postDelayed(this, 300);
                        }
                    }
                };

                View.OnClickListener mediaPlayToggle = v -> {
                    if (itemVideoView.isPlaying()) {
                        itemVideoView.pause();
                        itemBtnCenterPlay.setVisibility(View.VISIBLE);
                        itemBtnCenterPlay.setText("▶");
                        itemBtnPlayToggle.setText("▶");
                        return;
                    }

                    if (isPrepared[0]) {
                        stopAnyPlayback();
                        itemVideoView.start();
                        itemVideoPoster.setVisibility(View.GONE);
                        itemBtnCenterPlay.setVisibility(View.GONE);
                        itemBtnPlayToggle.setText("⏸");
                        activeListVideoView = itemVideoView;
                        itemVideoHandler.postDelayed(progressUpdater, 300);
                        return;
                    }

                    stopAnyPlayback();
                    itemBtnCenterPlay.setVisibility(View.GONE);
                    itemVideoSpinner.setVisibility(View.VISIBLE);
                    Toast.makeText(MainActivity.this, "Buffering " + item.fileName + "...", Toast.LENGTH_SHORT).show();

                    DownloadHelper.extractToCache(MainActivity.this, currentProject.packageDownloadUrl, item.fileName, new DownloadHelper.DownloadCallback() {
                        @Override
                        public void onSuccess(File file) {
                            runOnUiThread(() -> {
                                itemVideoSpinner.setVisibility(View.GONE);
                                itemVideoPoster.setVisibility(View.GONE);
                                itemVideoView.setVideoPath(file.getAbsolutePath());

                                itemVideoView.setOnPreparedListener(mp -> {
                                    isPrepared[0] = true;
                                    mp.setLooping(true);
                                    itemVideoView.start();
                                    itemBtnCenterPlay.setVisibility(View.GONE);
                                    itemBtnPlayToggle.setText("⏸");
                                    activeListVideoView = itemVideoView;
                                    int dur = itemVideoView.getDuration();
                                    if (dur > 0) itemVideoSeekbar.setMax(dur);
                                    itemVideoHandler.postDelayed(progressUpdater, 300);
                                });

                                itemVideoView.setOnCompletionListener(mp -> {
                                    itemBtnCenterPlay.setVisibility(View.VISIBLE);
                                    itemBtnCenterPlay.setText("▶");
                                    itemBtnPlayToggle.setText("▶");
                                    itemVideoSeekbar.setProgress(0);
                                });

                                itemVideoView.setOnErrorListener((mp, what, extra) -> {
                                    itemVideoSpinner.setVisibility(View.GONE);
                                    itemBtnCenterPlay.setVisibility(View.VISIBLE);
                                    itemBtnCenterPlay.setText("▶");
                                    itemBtnPlayToggle.setText("▶");
                                    Toast.makeText(MainActivity.this, "Video stream error (" + what + ")", Toast.LENGTH_SHORT).show();
                                    return true;
                                });
                            });
                        }

                        @Override
                        public void onError(Exception e) {
                            runOnUiThread(() -> {
                                itemVideoSpinner.setVisibility(View.GONE);
                                itemBtnCenterPlay.setVisibility(View.VISIBLE);
                                itemBtnCenterPlay.setText("▶");
                                Toast.makeText(MainActivity.this, "Could not stream video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                };

                itemBtnCenterPlay.setOnClickListener(mediaPlayToggle);
                itemBtnPlayToggle.setOnClickListener(mediaPlayToggle);

                itemBtnRewind.setOnClickListener(v -> {
                    if (isPrepared[0]) {
                        int pos = Math.max(0, itemVideoView.getCurrentPosition() - 5000);
                        itemVideoView.seekTo(pos);
                        itemVideoSeekbar.setProgress(pos);
                    }
                });

                itemBtnForward.setOnClickListener(v -> {
                    if (isPrepared[0]) {
                        int pos = Math.min(itemVideoView.getDuration(), itemVideoView.getCurrentPosition() + 5000);
                        itemVideoView.seekTo(pos);
                        itemVideoSeekbar.setProgress(pos);
                    }
                });

                itemBtnReplay.setOnClickListener(v -> {
                    if (isPrepared[0]) {
                        itemVideoView.seekTo(0);
                        itemVideoView.start();
                        itemBtnCenterPlay.setVisibility(View.GONE);
                        itemBtnPlayToggle.setText("⏸");
                        itemVideoHandler.postDelayed(progressUpdater, 300);
                    }
                });

                itemVideoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && isPrepared[0]) {
                            itemVideoView.seekTo(progress);
                        }
                    }
                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        isScrubbing[0] = true;
                    }
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        isScrubbing[0] = false;
                    }
                });
            }

            btnDownload.setOnClickListener(v -> {
                Toast.makeText(this, "Downloading " + item.fileName + " to device...", Toast.LENGTH_SHORT).show();
                DownloadHelper.extractAndSaveAsset(this, currentProject.packageDownloadUrl, item.fileName, new DownloadHelper.DownloadCallback() {
                    @Override
                    public void onSuccess(File file) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Saved: " + file.getName(), Toast.LENGTH_LONG).show());
                    }
                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                    }
                });
            });

            mediaItemsLayout.addView(card);
        }

        if (count == 0) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No " + filter + " assets in this project");
            emptyView.setTextColor(getResources().getColor(R.color.text_muted));
            emptyView.setPadding(0, 40, 0, 40);
            emptyView.setGravity(android.view.Gravity.CENTER);
            mediaItemsLayout.addView(emptyView);
        }
    }

    private void setupCustomVideoPlayerControls(String zipUrl, String videoFileName, String thumbUrl) {
        isVideoPrepared = false;
        videoSeekbar.setProgress(0);
        videoTimeDisplay.setText("0:00 / 0:00");
        btnVideoCenterPlay.setVisibility(View.VISIBLE);
        btnVideoCenterPlay.setText("▶");
        btnVideoPlayToggle.setText("▶");
        videoBufferingSpinner.setVisibility(View.GONE);
        videoPoster.setVisibility(View.VISIBLE);

        if (thumbUrl != null && !thumbUrl.isEmpty()) {
            DownloadHelper.loadBitmapFromUrl(thumbUrl, bmp -> runOnUiThread(() -> {
                if (bmp != null) videoPoster.setImageBitmap(bmp);
            }));
        }

        videoPlayerFrame.setOnClickListener(v -> {
            if (videoControlsBar.getVisibility() == View.VISIBLE) {
                videoControlsBar.setVisibility(View.GONE);
            } else {
                videoControlsBar.setVisibility(View.VISIBLE);
            }
        });

        View.OnClickListener playToggleListener = v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnVideoCenterPlay.setVisibility(View.VISIBLE);
                btnVideoCenterPlay.setText("▶");
                btnVideoPlayToggle.setText("▶");
                return;
            }

            if (isVideoPrepared) {
                stopAnyPlayback();
                videoView.start();
                videoPoster.setVisibility(View.GONE);
                btnVideoCenterPlay.setVisibility(View.GONE);
                btnVideoPlayToggle.setText("⏸");
                startVideoProgressTracker();
                return;
            }

            // First load: stream to cache
            stopAnyPlayback();
            btnVideoCenterPlay.setVisibility(View.GONE);
            videoBufferingSpinner.setVisibility(View.VISIBLE);
            Toast.makeText(MainActivity.this, "Buffering video stream...", Toast.LENGTH_SHORT).show();

            DownloadHelper.extractToCache(MainActivity.this, zipUrl, videoFileName, new DownloadHelper.DownloadCallback() {
                @Override
                public void onSuccess(File file) {
                    runOnUiThread(() -> {
                        videoBufferingSpinner.setVisibility(View.GONE);
                        videoPoster.setVisibility(View.GONE);
                        videoView.setVideoPath(file.getAbsolutePath());

                        videoView.setOnPreparedListener(mp -> {
                            isVideoPrepared = true;
                            mp.setLooping(true);
                            videoView.start();
                            btnVideoCenterPlay.setVisibility(View.GONE);
                            btnVideoPlayToggle.setText("⏸");
                            int dur = videoView.getDuration();
                            if (dur > 0) videoSeekbar.setMax(dur);
                            startVideoProgressTracker();
                        });

                        videoView.setOnCompletionListener(mp -> {
                            btnVideoCenterPlay.setVisibility(View.VISIBLE);
                            btnVideoCenterPlay.setText("▶");
                            btnVideoPlayToggle.setText("▶");
                            videoSeekbar.setProgress(0);
                        });

                        videoView.setOnErrorListener((mp, what, extra) -> {
                            videoBufferingSpinner.setVisibility(View.GONE);
                            btnVideoCenterPlay.setVisibility(View.VISIBLE);
                            btnVideoCenterPlay.setText("▶");
                            btnVideoPlayToggle.setText("▶");
                            Toast.makeText(MainActivity.this, "Video stream error (" + what + ")", Toast.LENGTH_SHORT).show();
                            return true;
                        });
                    });
                }

                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        videoBufferingSpinner.setVisibility(View.GONE);
                        btnVideoCenterPlay.setVisibility(View.VISIBLE);
                        btnVideoCenterPlay.setText("▶");
                        Toast.makeText(MainActivity.this, "Could not stream video: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        };

        btnVideoCenterPlay.setOnClickListener(playToggleListener);
        btnVideoPlayToggle.setOnClickListener(playToggleListener);

        btnVideoRewind.setOnClickListener(v -> {
            if (isVideoPrepared && videoView != null) {
                int pos = Math.max(0, videoView.getCurrentPosition() - 5000);
                videoView.seekTo(pos);
                videoSeekbar.setProgress(pos);
            }
        });

        btnVideoForward.setOnClickListener(v -> {
            if (isVideoPrepared && videoView != null) {
                int pos = Math.min(videoView.getDuration(), videoView.getCurrentPosition() + 5000);
                videoView.seekTo(pos);
                videoSeekbar.setProgress(pos);
            }
        });

        btnVideoReplay.setOnClickListener(v -> {
            if (isVideoPrepared && videoView != null) {
                videoView.seekTo(0);
                videoView.start();
                btnVideoCenterPlay.setVisibility(View.GONE);
                btnVideoPlayToggle.setText("⏸");
                startVideoProgressTracker();
            }
        });

        videoSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isVideoPrepared && videoView != null) {
                    videoView.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isVideoScrubbing = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isVideoScrubbing = false;
            }
        });
    }

    private void startVideoProgressTracker() {
        videoHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (videoView != null && videoView.isPlaying()) {
                    if (!isVideoScrubbing) {
                        int pos = videoView.getCurrentPosition();
                        int dur = videoView.getDuration();
                        videoSeekbar.setProgress(pos);
                        videoTimeDisplay.setText(formatTime(pos) + " / " + formatTime(dur));
                    }
                    videoHandler.postDelayed(this, 300);
                }
            }
        }, 300);
    }

    private String formatTime(int ms) {
        if (ms <= 0) return "0:00";
        int s = ms / 1000;
        int m = s / 60;
        int remSec = s % 60;
        return String.format("%d:%02d", m, remSec);
    }

    private void setupAudioControls(String zipUrl, String audioFileName) {
        btnAudioPlayPause.setText("▶");
        isAudioPlaying = false;

        btnAudioPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (isAudioPlaying) {
                    mediaPlayer.pause();
                    isAudioPlaying = false;
                    btnAudioPlayPause.setText("▶");
                } else {
                    stopAnyPlayback();
                    mediaPlayer.start();
                    isAudioPlaying = true;
                    btnAudioPlayPause.setText("⏸");
                    startAudioProgressTracker();
                }
                return;
            }

            stopAnyPlayback();
            btnAudioPlayPause.setText("⏳");
            Toast.makeText(this, "Streaming audio (no download)...", Toast.LENGTH_SHORT).show();

            // Stream audio into private temp buffer / cache without saving to public downloads
            DownloadHelper.extractToCache(this, zipUrl, audioFileName, new DownloadHelper.DownloadCallback() {
                @Override
                public void onSuccess(File file) {
                    runOnUiThread(() -> {
                        try {
                            mediaPlayer = new MediaPlayer();
                            mediaPlayer.setDataSource(file.getAbsolutePath());
                            mediaPlayer.prepare();
                            mediaPlayer.start();
                            isAudioPlaying = true;
                            btnAudioPlayPause.setText("⏸");
                            audioSeekbar.setMax(mediaPlayer.getDuration());
                            startAudioProgressTracker();

                            mediaPlayer.setOnCompletionListener(mp -> {
                                isAudioPlaying = false;
                                btnAudioPlayPause.setText("▶");
                                audioSeekbar.setProgress(0);
                            });
                        } catch (Exception e) {
                            btnAudioPlayPause.setText("▶");
                            Toast.makeText(MainActivity.this, "Play error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                @Override
                public void onError(Exception e) {
                    runOnUiThread(() -> {
                        btnAudioPlayPause.setText("▶");
                        Toast.makeText(MainActivity.this, "Audio stream error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }
            });
        });

        audioSeekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void startAudioProgressTracker() {
        audioHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && isAudioPlaying) {
                    int pos = mediaPlayer.getCurrentPosition();
                    audioSeekbar.setProgress(pos);
                    int sec = pos / 1000;
                    audioTime.setText(String.format("%d:%02d", sec / 60, sec % 60));
                    audioHandler.postDelayed(this, 500);
                }
            }
        }, 500);
    }

    private void stopAudioPlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
        isAudioPlaying = false;
    }

    private void switchTab(String tab) {
        currentTab = tab;
        containerPreview.setVisibility("preview".equals(tab) ? View.VISIBLE : View.GONE);
        containerMedia.setVisibility("media".equals(tab) ? View.VISIBLE : View.GONE);
        containerXml.setVisibility("xml".equals(tab) ? View.VISIBLE : View.GONE);
        containerDetails.setVisibility("details".equals(tab) ? View.VISIBLE : View.GONE);

        tabPreview.setBackgroundResource("preview".equals(tab) ? R.drawable.bg_chip_active : 0);
        tabMedia.setBackgroundResource("media".equals(tab) ? R.drawable.bg_chip_active : 0);
        tabXml.setBackgroundResource("xml".equals(tab) ? R.drawable.bg_chip_active : 0);
        tabDetails.setBackgroundResource("details".equals(tab) ? R.drawable.bg_chip_active : 0);

        tabPreview.setTextColor(getResources().getColor("preview".equals(tab) ? R.color.accent_green : R.color.text_secondary));
        tabMedia.setTextColor(getResources().getColor("media".equals(tab) ? R.color.accent_green : R.color.text_secondary));
        tabXml.setTextColor(getResources().getColor("xml".equals(tab) ? R.color.accent_green : R.color.text_secondary));
        tabDetails.setTextColor(getResources().getColor("details".equals(tab) ? R.color.accent_green : R.color.text_secondary));
    }

    private void setMediaFilter(String filter) {
        currentFilter = filter;
        filterAll.setBackgroundResource("all".equals(filter) ? R.drawable.bg_chip_active : R.drawable.bg_chip);
        filterVideo.setBackgroundResource("video".equals(filter) ? R.drawable.bg_chip_active : R.drawable.bg_chip);
        filterImage.setBackgroundResource("image".equals(filter) ? R.drawable.bg_chip_active : R.drawable.bg_chip);
        filterAudio.setBackgroundResource("audio".equals(filter) ? R.drawable.bg_chip_active : R.drawable.bg_chip);

        filterAll.setTextColor(getResources().getColor("all".equals(filter) ? R.color.accent_green : R.color.text_secondary));
        filterVideo.setTextColor(getResources().getColor("video".equals(filter) ? R.color.accent_green : R.color.text_secondary));
        filterImage.setTextColor(getResources().getColor("image".equals(filter) ? R.color.accent_green : R.color.text_secondary));
        filterAudio.setTextColor(getResources().getColor("audio".equals(filter) ? R.color.accent_green : R.color.text_secondary));

        if (currentProject != null) {
            renderMediaAssets(currentProject.mediaFiles, currentFilter);
        }
    }

    private void saveRecentPreset(ProjectData project, String url) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String raw = prefs.getString(KEY_RECENTS, "[]");
            JSONArray arr = new JSONArray(raw);
            JSONArray newArr = new JSONArray();

            JSONObject newItem = new JSONObject();
            newItem.put("title", project.title);
            newItem.put("url", project.shareUrl != null ? project.shareUrl : url);
            newItem.put("pid", project.pid);
            newItem.put("size", project.packageSizeFormatted);
            newItem.put("hasVideo", project.hasVideo);
            newItem.put("hasAudio", project.hasAudio);
            newArr.put(newItem);

            for (int i = 0; i < arr.length() && newArr.length() < 8; i++) {
                JSONObject item = arr.getJSONObject(i);
                if (!item.optString("pid").equals(project.pid)) {
                    newArr.put(item);
                }
            }

            prefs.edit().putString(KEY_RECENTS, newArr.toString()).apply();
            loadRecentPresets();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRecentPresets() {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String raw = prefs.getString(KEY_RECENTS, "[]");
            JSONArray arr = new JSONArray(raw);

            if (arr.length() == 0) {
                recentStrip.setVisibility(View.GONE);
                return;
            }

            recentStrip.setVisibility(View.VISIBLE);
            recentChipsContainer.removeAllViews();

            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.getJSONObject(i);
                String title = item.optString("title", "Project");
                String url = item.optString("url", "");
                String size = item.optString("size", "");
                boolean hasVid = item.optBoolean("hasVideo", false);
                boolean hasAud = item.optBoolean("hasAudio", false);

                String icon = hasVid ? "🎬" : (hasAud ? "🎵" : "📦");

                Button chip = new Button(this);
                chip.setText(icon + " " + title + " (" + size + ")");
                chip.setTextSize(10f);
                chip.setTextColor(getResources().getColor(R.color.text_secondary));
                chip.setBackgroundResource(R.drawable.bg_chip);
                chip.setPadding(24, 8, 24, 8);
                chip.setAllCaps(false);

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (int) (32 * getResources().getDisplayMetrics().density)
                );
                lp.setMargins(0, 0, 16, 0);
                chip.setLayoutParams(lp);

                chip.setOnClickListener(v -> {
                    inputUrl.setText(url);
                    inspectUrl(url);
                });

                recentChipsContainer.addView(chip);
            }
        } catch (Exception e) {
            recentStrip.setVisibility(View.GONE);
        }
    }

    private void setLoading(boolean loading) {
        progressLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnInspect.setEnabled(!loading);
    }

    private void showError(String msg) {
        textError.setText(msg);
        textError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        textError.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudioPlayer();
    }
}
