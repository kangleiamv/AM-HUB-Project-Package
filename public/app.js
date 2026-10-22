document.addEventListener('DOMContentLoaded', () => {
  // Elements
  const form = document.getElementById('inspect-form');
  const input = document.getElementById('share-input');
  const submitBtn = document.getElementById('submit-btn');
  const btnText = submitBtn.querySelector('.btn-text');
  const btnSpinner = submitBtn.querySelector('.btn-spinner');
  const btnPaste = document.getElementById('btn-paste-clipboard');
  
  const errorBanner = document.getElementById('error-banner');
  const errorMessage = document.getElementById('error-message');
  const resultsArea = document.getElementById('results-area');
  const toast = document.getElementById('toast');

  // Hero section
  const resTitle = document.getElementById('res-title');
  const resPolicyBadge = document.getElementById('res-policy-badge');
  const resDesc = document.getElementById('res-desc');
  const resSize = document.getElementById('res-size');
  const resDownloads = document.getElementById('res-downloads');
  const resPlatform = document.getElementById('res-platform');
  const resVersion = document.getElementById('res-version');

  // Hero Actions
  const btnDownloadPackage = document.getElementById('btn-download-package');
  const btnCopyZipLink = document.getElementById('btn-copy-zip-link');
  const btnImportDirect = document.getElementById('btn-import-direct');
  const btnCopyLink = document.getElementById('btn-copy-link');
  const btnDownloadJson = document.getElementById('btn-download-json');

  // Tab Nav
  const navTabs = document.querySelectorAll('.nav-tab');
  const tabContents = document.querySelectorAll('.tab-content');
  const tabMediaCount = document.getElementById('tab-media-count');

  // Video, Audio & Thumbnails
  const videoCard = document.getElementById('video-card');
  const playerVideo = document.getElementById('player-video');
  const videoSpecsBadge = document.getElementById('video-specs-badge');
  const btnDownloadVideo = document.getElementById('btn-download-video');
  const audioCard = document.getElementById('audio-card');
  const playerAudio = document.getElementById('player-audio');
  const audioSpecsBadge = document.getElementById('audio-specs-badge');
  const audioTitleText = document.getElementById('audio-title-text');
  const btnDownloadAudio = document.getElementById('btn-download-audio');
  const previewImg = document.getElementById('preview-img');
  const btnDownloadThumb = document.getElementById('btn-download-thumb');
  const thumbTierLabel = document.getElementById('thumb-tier-label');
  const thumbTiers = document.querySelectorAll('#thumb-tiers .seg-btn');

  // Media
  const btnDownloadAllMedia = document.getElementById('btn-download-all-media');
  const btnCopyMediaZipLink = document.getElementById('btn-copy-media-zip-link');
  const mediaCardsContainer = document.getElementById('media-cards-container');
  const mediaFilterBar = document.getElementById('media-filter-bar');
  const countAll = document.getElementById('count-all');
  const countVideo = document.getElementById('count-video');
  const countImage = document.getElementById('count-image');
  const countAudio = document.getElementById('count-audio');
  let currentMediaFilter = 'all';

  // Full-Screen Image Lightbox Modal
  const imageModal = document.getElementById('image-modal');
  const modalImg = document.getElementById('modal-img');
  const modalImgTitle = document.getElementById('modal-img-title');
  const modalCloseBtn = document.getElementById('modal-close-btn');
  const modalImgDownload = document.getElementById('modal-img-download');

  // XML
  const xmlFilename = document.getElementById('xml-filename');
  const xmlSizeBadge = document.getElementById('xml-size-badge');
  const xmlCodeDisplay = document.getElementById('xml-code-display');
  const btnCopyXml = document.getElementById('btn-copy-xml');
  const btnDownloadXml = document.getElementById('btn-download-xml');

  // Details
  const scenesCountPill = document.getElementById('scenes-count-pill');
  const scenesContainer = document.getElementById('scenes-container');
  const effectsCountPill = document.getElementById('effects-count-pill');
  const effectsPills = document.getElementById('effects-pills');

  // Raw API
  const rawJsonDisplay = document.getElementById('raw-json-display');
  const btnCopyRawJson = document.getElementById('btn-copy-raw-json');

  // Sticky Bottom Bar
  const stickyBottomBar = document.getElementById('sticky-bottom-bar');
  const stickyTitle = document.getElementById('sticky-title');
  const stickySize = document.getElementById('sticky-size');
  const btnStickyCopyZip = document.getElementById('btn-sticky-copy-zip');
  const btnStickyDownload = document.getElementById('btn-sticky-download');

  let currentProject = null;
  let currentXmlString = '';

  // Paste from clipboard
  btnPaste.addEventListener('click', async () => {
    try {
      const text = await navigator.clipboard.readText();
      if (text) {
        input.value = text.trim();
        showToast('Link pasted!');
        inspectUrl(input.value);
      }
    } catch (err) {
      input.focus();
    }
  });

  // Recent Presets Logic (Dynamic link history saved in localStorage)
  const RECENT_STORAGE_KEY = 'alight_recent_presets_v1';
  const recentPresetsStrip = document.getElementById('recent-presets-strip');
  const recentChipsContainer = document.getElementById('recent-chips-container');
  const btnClearRecent = document.getElementById('btn-clear-recent');

  function getRecentPresets() {
    try {
      const raw = localStorage.getItem(RECENT_STORAGE_KEY);
      return raw ? JSON.parse(raw) : [];
    } catch (e) {
      return [];
    }
  }

  function saveRecentPreset(data, inputUrl) {
    if (!data || !data.uid || !data.pid) return;
    try {
      let recents = getRecentPresets();
      const projectUrl = data.shareUrl || inputUrl || `https://alightcreative.com/am/share/u/${data.uid}/p/${data.pid}`;

      // Filter out duplicate by pid or url
      recents = recents.filter(r => r.pid !== data.pid && r.url !== projectUrl);

      // Prepend newest inspected preset
      recents.unshift({
        uid: data.uid,
        pid: data.pid,
        url: projectUrl,
        title: data.title || 'Untitled Project',
        sizeFormatted: data.packageSizeFormatted || '',
        hasVideo: !!data.hasVideo,
        hasAudio: !!data.hasAudio,
        timestamp: Date.now()
      });

      // Keep up to 8 recent presets
      if (recents.length > 8) recents = recents.slice(0, 8);
      localStorage.setItem(RECENT_STORAGE_KEY, JSON.stringify(recents));
      renderRecentPresets();
    } catch (e) {
      console.warn('Recent presets storage error:', e);
    }
  }

  function renderRecentPresets() {
    if (!recentChipsContainer || !recentPresetsStrip) return;
    const recents = getRecentPresets();
    if (recents.length === 0) {
      recentPresetsStrip.style.display = 'none';
      recentChipsContainer.innerHTML = '';
      return;
    }

    recentPresetsStrip.style.display = 'flex';
    recentChipsContainer.innerHTML = '';

    recents.forEach(item => {
      const chip = document.createElement('button');
      chip.type = 'button';
      chip.className = 'preset-chip';
      if (currentProject && (currentProject.pid === item.pid || currentProject.shareUrl === item.url)) {
        chip.classList.add('active');
      }

      let icon = '📦';
      if (item.hasVideo) icon = '🎬';
      else if (item.hasAudio) icon = '🎵';

      chip.innerHTML = `<span>${icon} ${escapeHtml(item.title)}</span> <small class="chip-size">${escapeHtml(item.sizeFormatted)}</small>`;
      chip.title = `${item.title} (${item.url})`;

      chip.addEventListener('click', () => {
        document.querySelectorAll('.preset-chip').forEach(c => c.classList.remove('active'));
        chip.classList.add('active');
        input.value = item.url;
        inspectUrl(item.url);
      });

      recentChipsContainer.appendChild(chip);
    });
  }

  if (btnClearRecent) {
    btnClearRecent.addEventListener('click', () => {
      try {
        localStorage.removeItem(RECENT_STORAGE_KEY);
      } catch (e) {}
      renderRecentPresets();
      showToast('Recent presets cleared');
    });
  }

  // Initial render of recent presets from localStorage
  renderRecentPresets();

  // Tab switching
  navTabs.forEach(tab => {
    tab.addEventListener('click', () => {
      const target = tab.getAttribute('data-tab');
      navTabs.forEach(t => t.classList.remove('active'));
      tabContents.forEach(c => c.classList.remove('active'));

      tab.classList.add('active');
      const activeContent = document.getElementById(`tab-content-${target}`);
      if (activeContent) {
        activeContent.classList.add('active');
      }

      // If switching to XML tab and not yet loaded, fetch XML
      if (target === 'xml' && currentProject && !currentXmlString) {
        loadXmlPreset();
      }
    });
  });

  // Media category filter pills (All, Video, Image, Audio)
  if (mediaFilterBar) {
    mediaFilterBar.querySelectorAll('.filter-pill').forEach(pill => {
      pill.addEventListener('click', () => {
        mediaFilterBar.querySelectorAll('.filter-pill').forEach(p => p.classList.remove('active'));
        pill.classList.add('active');
        currentMediaFilter = pill.getAttribute('data-filter') || 'all';
        if (currentProject && currentProject.mediaFiles) {
          renderMediaCards(currentProject.mediaFiles, currentMediaFilter);
        }
      });
    });
  }

  // Form submit
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const val = input.value.trim();
    if (val) {
      inspectUrl(val);
    }
  });

  async function inspectUrl(targetUrl) {
    setLoading(true);
    hideError();

    try {
      const res = await fetch('/api/inspect', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: targetUrl })
      });

      const data = await res.json();
      if (!res.ok || !data.ok) {
        throw new Error(data.error || 'Failed to inspect link');
      }

      currentProject = data;
      currentXmlString = '';
      saveRecentPreset(data, targetUrl);
      renderProject(data);
    } catch (err) {
      showError(err.message);
    } finally {
      setLoading(false);
    }
  }

  function renderProject(data) {
    // Top summary
    resTitle.textContent = data.title;
    resDesc.textContent = `Project by UID: ${data.uid.substring(0, 14)}... • Package ID: ${data.pid}`;
    resSize.textContent = data.packageSizeFormatted;
    resDownloads.textContent = data.downloadsCount.toLocaleString();
    resPlatform.textContent = data.platform;
    resVersion.textContent = data.appVersion.split(' ')[0];

    // Download policy badge
    if (data.downloadPolicy.isFreeEligible) {
      resPolicyBadge.className = 'hero-badge badge-free';
      resPolicyBadge.textContent = 'Free Download';
    } else {
      resPolicyBadge.className = 'hero-badge badge-sub';
      resPolicyBadge.textContent = `PRO (> ${data.downloadPolicy.maxFreeSizeFormatted})`;
    }

    // Hero actions
    btnImportDirect.href = data.shareUrl;
    if (data.packageDownloadUrl) {
      btnDownloadPackage.href = data.packageDownloadUrl;
      btnDownloadAllMedia.href = data.packageDownloadUrl;
      btnStickyDownload.href = data.packageDownloadUrl;
      btnDownloadPackage.style.display = 'inline-flex';
      btnDownloadAllMedia.style.display = 'inline-flex';
      btnCopyZipLink.style.display = 'inline-flex';
      if (btnCopyMediaZipLink) btnCopyMediaZipLink.style.display = 'inline-flex';
      if (btnStickyCopyZip) btnStickyCopyZip.style.display = 'inline-flex';
    } else {
      btnDownloadPackage.style.display = 'none';
      btnDownloadAllMedia.style.display = 'none';
      btnCopyZipLink.style.display = 'none';
      if (btnCopyMediaZipLink) btnCopyMediaZipLink.style.display = 'none';
      if (btnStickyCopyZip) btnStickyCopyZip.style.display = 'none';
    }

    // Video Player Tab
    if (data.hasVideo && data.videoStreamUrl) {
      videoCard.style.display = 'block';
      playerVideo.pause();
      playerVideo.src = data.videoStreamUrl;
      playerVideo.load();
      if (data.videoDetails) {
        videoSpecsBadge.textContent = `${data.videoDetails.width}x${data.videoDetails.height} @ ${data.videoDetails.fps}fps (${data.videoDetails.duration})`;
      }
      const downloadUrl = data.videoDownloadUrl || data.videoStreamUrl.replace('/api/stream-media', '/api/download-media');
      const cleanVidName = (data.title ? data.title.replace(/[^a-zA-Z0-9._-]/g, '_') : data.pid) + '.mp4';
      btnDownloadVideo.href = downloadUrl;
      btnDownloadVideo.setAttribute('download', cleanVidName);
    } else {
      videoCard.style.display = 'none';
      playerVideo.pause();
      playerVideo.removeAttribute('src');
      playerVideo.load();
    }

    // Audio Player in Preview Tab (for .wav / .m4a / .mp3)
    if (data.hasAudio && data.audioStreamUrl) {
      audioCard.style.display = 'block';
      playerAudio.src = data.audioStreamUrl;
      const audioName = data.audioDetails?.fileName || 'Audio Track';
      if (audioTitleText) audioTitleText.textContent = audioName;
      const extUpper = (audioName.split('.').pop() || 'AUDIO').toUpperCase();
      if (audioSpecsBadge) audioSpecsBadge.textContent = `${extUpper} • ${data.audioDetails?.sizeFormatted || ''}`;
      if (btnDownloadAudio) {
        btnDownloadAudio.href = data.audioStreamUrl;
        btnDownloadAudio.setAttribute('download', audioName);
        const span = btnDownloadAudio.querySelector('span');
        if (span) span.textContent = `Download Audio (.${extUpper})`;
      }
    } else {
      if (audioCard) audioCard.style.display = 'none';
      if (playerAudio) {
        playerAudio.pause();
        playerAudio.removeAttribute('src');
      }
    }

    // Thumbnails setup
    setThumbnailTier('large');

    // Calculate Media Assets counts for filter pills
    const allMedia = data.mediaFiles || [];
    const vidCount = allMedia.filter(m => m.type === 'video').length;
    const imgCount = allMedia.filter(m => m.type === 'image').length;
    const audCount = allMedia.filter(m => m.type === 'audio').length;

    tabMediaCount.textContent = allMedia.length;
    if (countAll) countAll.textContent = allMedia.length;
    if (countVideo) countVideo.textContent = vidCount;
    if (countImage) countImage.textContent = imgCount;
    if (countAudio) countAudio.textContent = audCount;

    // Render Media cards with current filter
    renderMediaCards(allMedia, currentMediaFilter);

    // Scenes list
    scenesCountPill.textContent = data.scenes.length;
    scenesContainer.innerHTML = '';
    data.scenes.forEach((scene, idx) => {
      const row = document.createElement('div');
      row.className = 'scene-item-row';
      row.innerHTML = `
        <span class="scene-title-text">${escapeHtml(scene.title || `Scene ${idx + 1}`)}</span>
        <div class="scene-right-meta">
          <span class="pill-badge">${scene.sizeFormatted}</span>
          <a href="${scene.downloadUrl}" class="btn btn-small btn-secondary" download>XML</a>
        </div>
      `;
      scenesContainer.appendChild(row);
    });

    // Required Effects
    effectsCountPill.textContent = data.requiredEffects.length;
    effectsPills.innerHTML = '';
    data.requiredEffects.forEach(effect => {
      const chip = document.createElement('span');
      chip.className = 'effect-chip';
      chip.textContent = effect.name;
      chip.title = effect.raw;
      effectsPills.appendChild(chip);
    });

    // API JSON Display
    rawJsonDisplay.textContent = JSON.stringify(data, null, 2);

    // Sticky Bottom Bar
    stickyTitle.textContent = data.title;
    stickySize.textContent = data.packageSizeFormatted;
    stickyBottomBar.style.display = 'block';

    resultsArea.style.display = 'flex';

    // Auto-fetch XML in the background
    loadXmlPreset();
  }

  function renderMediaCards(files, filter = 'all') {
    mediaCardsContainer.innerHTML = '';
    
    let filteredFiles = files || [];
    if (filter !== 'all') {
      filteredFiles = filteredFiles.filter(f => f.type === filter);
    }

    if (filteredFiles.length === 0) {
      const emptyMsg = filter === 'all' 
        ? 'No external media files in this project' 
        : `No ${filter} files found in this project`;
      mediaCardsContainer.innerHTML = `<div style="text-align:center; padding: 24px; color: var(--text-muted);">${emptyMsg}</div>`;
      return;
    }

    filteredFiles.forEach(file => {
      const card = document.createElement('div');
      card.className = 'media-asset-card';
      
      let typeClass = 'type-file';
      let typeLabel = (file.type || 'FILE').toUpperCase();
      let btnLabel = 'Download Asset';

      if (file.type === 'video') {
        typeClass = 'type-video';
        typeLabel = '🎬 Video';
        btnLabel = 'Download Video';
      } else if (file.type === 'image') {
        typeClass = 'type-image';
        typeLabel = '🖼️ Image';
        btnLabel = 'Download Image';
      } else if (file.type === 'audio') {
        typeClass = 'type-audio';
        const ext = (file.fileName.split('.').pop() || 'AUDIO').toUpperCase();
        typeLabel = `🎵 ${ext}`;
        btnLabel = `Download Audio (.${ext})`;
      }

      let specsText = file.sizeFormatted;
      if (file.details) {
        if (file.type === 'video') {
          specsText = `${file.details.width}x${file.details.height} • ${file.details.fps} FPS • ${file.details.codec}`;
        } else if (file.type === 'image') {
          specsText = `${file.details.width}x${file.details.height} • ${file.sizeFormatted}`;
        } else if (file.type === 'audio') {
          const parts = [];
          if (file.details.duration) parts.push(file.details.duration);
          if (file.details.bitrate) parts.push(file.details.bitrate);
          if (file.details.codec) parts.push(file.details.codec);
          specsText = parts.length ? parts.join(' • ') : file.sizeFormatted;
        }
      }

      // Inline image preview thumbnail for image assets
      let imagePreviewHtml = '';
      if (file.type === 'image' && (file.previewUrl || file.downloadUrl)) {
        const imgSrc = file.previewUrl || file.downloadUrl;
        imagePreviewHtml = `
          <div class="card-image-preview" data-src="${imgSrc}" data-name="${escapeHtml(file.fileName)}" title="Click to view full image">
            <img src="${imgSrc}" alt="${escapeHtml(file.fileName)}" loading="lazy" />
            <div class="preview-overlay-zoom">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                <line x1="11" y1="8" x2="11" y2="14"></line>
                <line x1="8" y1="11" x2="14" y2="11"></line>
              </svg>
              <span>Preview</span>
            </div>
          </div>
        `;
      }

      // Advanced Custom Video Player for video assets (.mp4, .webm, .mov)
      let videoPlayerHtml = '';
      if (file.type === 'video' && (file.previewUrl || file.downloadUrl)) {
        const vUrl = file.previewUrl || file.downloadUrl;
        videoPlayerHtml = createCustomVideoPlayerHtml(vUrl);
      }

      // Inline audio player for audio assets (.wav, .m4a, .mp3)
      let playerHtml = '';
      if (file.type === 'audio' && (file.previewUrl || file.downloadUrl)) {
        const aUrl = file.previewUrl || file.downloadUrl;
        playerHtml = `
          <div class="card-audio-player" style="margin: 10px 0;">
            <audio controls preload="metadata" style="width: 100%; border-radius: 6px; outline: none;" src="${aUrl}"></audio>
          </div>
        `;
      }

      card.innerHTML = `
        <div class="media-card-top">
          <span class="type-pill ${typeClass}">${typeLabel}</span>
          <strong class="media-name" title="${escapeHtml(file.fileName)}">${escapeHtml(file.fileName)}</strong>
        </div>
        <div class="media-card-specs">
          <span class="specs-tag">${specsText}</span>
          <span class="specs-tag">${file.sizeFormatted}</span>
        </div>
        ${imagePreviewHtml}
        ${videoPlayerHtml}
        ${playerHtml}
        <div class="media-card-action" style="margin-top: 10px;">
          <a href="${file.downloadUrl}" class="btn btn-primary" download="${encodeURIComponent(file.fileName)}" style="width: 100%;">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
              <polyline points="7 10 12 15 17 10"></polyline>
              <line x1="12" y1="15" x2="12" y2="3"></line>
            </svg>
            <span>${btnLabel}</span>
          </a>
        </div>
      `;

      // Initialize the custom video player on this media card if present
      const mediaVideoWrapper = card.querySelector('.custom-video-wrapper');
      if (mediaVideoWrapper) {
        setupCustomVideoPlayer(mediaVideoWrapper);
      }

      // Attach image preview click to open full-screen lightbox
      const previewBox = card.querySelector('.card-image-preview');
      if (previewBox) {
        previewBox.addEventListener('click', () => {
          openImageModal(previewBox.getAttribute('data-src'), previewBox.getAttribute('data-name'));
        });
      }

      mediaCardsContainer.appendChild(card);
    });
  }

  // Full-Screen Image Lightbox Modal logic
  function openImageModal(src, title) {
    if (!imageModal || !src) return;
    modalImg.src = src;
    if (modalImgTitle) modalImgTitle.textContent = title || 'Image Preview';
    if (modalImgDownload) {
      modalImgDownload.href = src.replace('/api/stream-media', '/api/download-media');
      modalImgDownload.setAttribute('download', title || 'image.png');
    }
    imageModal.style.display = 'flex';
    document.body.style.overflow = 'hidden';
  }

  function closeImageModal() {
    if (!imageModal) return;
    imageModal.style.display = 'none';
    if (modalImg) modalImg.removeAttribute('src');
    document.body.style.overflow = '';
  }

  if (modalCloseBtn) {
    modalCloseBtn.addEventListener('click', closeImageModal);
  }
  if (imageModal) {
    imageModal.addEventListener('click', (e) => {
      if (e.target === imageModal) {
        closeImageModal();
      }
    });
  }
  window.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') closeImageModal();
  });

  async function loadXmlPreset() {
    if (!currentProject) return;
    xmlCodeDisplay.textContent = 'Extracting XML preset from package...';
    try {
      const res = await fetch(`/api/get-xml?uid=${encodeURIComponent(currentProject.uid)}&pid=${encodeURIComponent(currentProject.pid)}`);
      const data = await res.json();
      if (res.ok && data.ok) {
        currentXmlString = data.xml;
        xmlFilename.textContent = data.fileName;
        xmlSizeBadge.textContent = `${Math.round(data.size / 1024)} KB`;
        xmlCodeDisplay.textContent = data.xml;
        btnDownloadXml.href = `/api/download-media?uid=${encodeURIComponent(currentProject.uid)}&pid=${encodeURIComponent(currentProject.pid)}&file=${encodeURIComponent(data.fileName)}`;
      } else {
        xmlCodeDisplay.textContent = 'XML file could not be extracted: ' + (data.error || 'Unknown error');
      }
    } catch (err) {
      xmlCodeDisplay.textContent = 'Error loading XML: ' + err.message;
    }
  }

  function setThumbnailTier(tier) {
    if (!currentProject) return;
    thumbTiers.forEach(btn => {
      btn.classList.toggle('active', btn.getAttribute('data-tier') === tier);
    });
    const url = currentProject.thumbnails[tier] || currentProject.thumbnails.large;
    if (url) {
      previewImg.src = url;
      btnDownloadThumb.href = url;
      btnDownloadThumb.setAttribute('download', `${currentProject.pid}-${tier}.jpg`);
      thumbTierLabel.textContent = `${tier.toUpperCase()} Resolution`;
    }
  }

  thumbTiers.forEach(btn => {
    btn.addEventListener('click', () => {
      setThumbnailTier(btn.getAttribute('data-tier'));
    });
  });

  // Bulletproof clipboard copy with fallback for mobile HTTP/LAN
  function copyToClipboard(text, successMsg, btnElement) {
    if (!text) return;

    const onCopied = () => {
      showToast(successMsg || 'Copied to clipboard!');
      if (btnElement) {
        const originalHtml = btnElement.innerHTML;
        btnElement.innerHTML = '<span>Copied! ✓</span>';
        setTimeout(() => {
          btnElement.innerHTML = originalHtml;
        }, 2000);
      }
    };

    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(onCopied).catch(() => {
        fallbackCopyText(text, onCopied);
      });
    } else {
      fallbackCopyText(text, onCopied);
    }
  }

  function fallbackCopyText(text, callback) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.top = '-9999px';
    textarea.style.left = '-9999px';
    textarea.setAttribute('readonly', '');
    document.body.appendChild(textarea);
    textarea.focus();
    textarea.select();
    try {
      const ok = document.execCommand('copy');
      if (ok && callback) callback();
    } catch (e) {
      prompt('Copy download link:', text);
    }
    document.body.removeChild(textarea);
  }

  // Copy direct ZIP download link: e.g. http://localhost:3000/api/download-package?uid=...&pid=...
  function copyZipDownloadLink(e) {
    if (!currentProject) return;
    const uid = encodeURIComponent(currentProject.uid);
    const pid = encodeURIComponent(currentProject.pid);
    const origin = window.location.origin || `${window.location.protocol}//${window.location.host}`;
    const fullZipUrl = `${origin}/api/download-package?uid=${uid}&pid=${pid}`;

    const triggerBtn = e ? e.currentTarget : btnCopyZipLink;
    copyToClipboard(fullZipUrl, `Copied: ${fullZipUrl}`, triggerBtn);
  }

  if (btnCopyZipLink) btnCopyZipLink.addEventListener('click', copyZipDownloadLink);
  if (btnCopyMediaZipLink) btnCopyMediaZipLink.addEventListener('click', copyZipDownloadLink);
  if (btnStickyCopyZip) btnStickyCopyZip.addEventListener('click', copyZipDownloadLink);

  // Copy share link
  btnCopyLink.addEventListener('click', (e) => {
    if (!currentProject) return;
    copyToClipboard(currentProject.shareUrl, 'Share link copied!', e.currentTarget);
  });

  // Export JSON
  btnDownloadJson.addEventListener('click', () => {
    if (!currentProject) return;
    const blob = new Blob([JSON.stringify(currentProject, null, 2)], { type: 'application/json' });
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = `alight-motion-${currentProject.pid}.json`;
    a.click();
    URL.revokeObjectURL(a.href);
    showToast('Project manifest exported!');
  });

  // Copy XML
  btnCopyXml.addEventListener('click', (e) => {
    if (!currentXmlString) return;
    copyToClipboard(currentXmlString, 'XML Preset copied to clipboard!', e.currentTarget);
  });

  // Copy Raw JSON
  btnCopyRawJson.addEventListener('click', (e) => {
    if (!currentProject) return;
    copyToClipboard(JSON.stringify(currentProject, null, 2), 'API JSON copied!', e.currentTarget);
  });

  function showToast(msg) {
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => {
      toast.classList.remove('show');
    }, 2400);
  }

  function setLoading(loading) {
    submitBtn.disabled = loading;
    btnText.style.display = loading ? 'none' : 'inline-block';
    btnSpinner.style.display = loading ? 'inline-block' : 'none';
  }

  function showError(msg) {
    errorMessage.textContent = msg;
    errorBanner.style.display = 'flex';
  }

  function hideError() {
    errorBanner.style.display = 'none';
  }

  function escapeHtml(text) {
    if (!text) return '';
    return text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  }

  // --- Advanced Custom Video Player Controller ---
  setupCustomVideoPlayer();

  function createCustomVideoPlayerHtml(vUrl) {
    return `
      <div class="custom-video-wrapper" style="margin: 10px 0;">
        <video playsinline loop preload="metadata" src="${vUrl}">
          Your browser does not support the video tag.
        </video>

        <!-- Center Big Play Button & Buffer Spinner -->
        <div class="video-center-overlay">
          <button type="button" class="video-big-play-btn" aria-label="Play/Pause">
            <svg class="icon-play" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
              <polygon points="5 3 19 12 5 21 5 3"></polygon>
            </svg>
            <svg class="icon-pause" style="display:none;" width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="4" width="4" height="16"></rect>
              <rect x="14" y="4" width="4" height="16"></rect>
            </svg>
          </button>
          <div class="video-spinner" style="display:none;"></div>
        </div>

        <!-- Custom Bottom Controls Bar -->
        <div class="video-controls-bar">
          <!-- Scrubber Track -->
          <div class="video-timeline">
            <div class="video-timeline-buffer"></div>
            <div class="video-timeline-progress"></div>
            <div class="video-timeline-thumb"></div>
          </div>

          <!-- Controls Row -->
          <div class="video-controls-row">
            <div class="video-controls-left">
              <button type="button" class="v-btn v-btn-play" title="Play/Pause">
                <svg class="icon-play" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <polygon points="5 3 19 12 5 21 5 3"></polygon>
                </svg>
                <svg class="icon-pause" style="display:none;" width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
                  <rect x="6" y="4" width="4" height="16"></rect>
                  <rect x="14" y="4" width="4" height="16"></rect>
                </svg>
              </button>
              <button type="button" class="v-btn v-btn-rewind" title="Rewind 5s">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="11 19 2 12 11 5 11 19"></polygon>
                  <polygon points="22 19 13 12 22 5 22 19"></polygon>
                </svg>
              </button>
              <button type="button" class="v-btn v-btn-forward" title="Forward 5s">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="13 19 22 12 13 5 13 19"></polygon>
                  <polygon points="2 19 11 12 2 5 2 19"></polygon>
                </svg>
              </button>
              <div class="video-time-display">
                <span class="v-time-current">0:00</span> / <span class="v-time-duration">0:00</span>
              </div>
            </div>

            <div class="video-controls-right">
              <div class="v-volume-group">
                <button type="button" class="v-btn v-btn-volume" title="Mute/Unmute">
                  <svg class="icon-vol-high" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                    <path d="M19.07 4.93a10 10 0 0 1 0 14.14M15.54 8.46a5 5 0 0 1 0 7.07"></path>
                  </svg>
                  <svg class="icon-vol-mute" style="display:none;" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polygon points="11 5 6 9 2 9 2 15 6 15 11 19 11 5"></polygon>
                    <line x1="23" y1="9" x2="17" y2="15"></line>
                    <line x1="17" y1="9" x2="23" y2="15"></line>
                  </svg>
                </button>
                <input type="range" class="v-volume-slider" min="0" max="1" step="0.05" value="1">
              </div>
              <button type="button" class="v-btn v-btn-speed" title="Speed">1x</button>
              <button type="button" class="v-btn v-btn-loop active" title="Toggle Loop">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="17 1 21 5 17 9"></polyline>
                  <path d="M3 11V9a4 4 0 0 1 4-4h14"></path>
                  <polyline points="7 23 3 19 7 15"></polyline>
                  <path d="M21 13v2a4 4 0 0 1-4 4H3"></path>
                </svg>
              </button>
              <button type="button" class="v-btn v-btn-fullscreen" title="Fullscreen">
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3"></path>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>
    `;
  }

  function setupCustomVideoPlayer(wrapperTarget) {
    const wrapper = wrapperTarget || document.getElementById('custom-video-wrapper');
    if (!wrapper || wrapper.dataset.customPlayerInit) return;
    wrapper.dataset.customPlayerInit = 'true';

    const video = wrapper.querySelector('video');
    const centerOverlay = wrapper.querySelector('.video-center-overlay');
    const bigPlayBtn = wrapper.querySelector('.video-big-play-btn');
    const spinner = wrapper.querySelector('.video-spinner');

    const timeline = wrapper.querySelector('.video-timeline');
    const progress = wrapper.querySelector('.video-timeline-progress');
    const bufferBar = wrapper.querySelector('.video-timeline-buffer');
    const thumb = wrapper.querySelector('.video-timeline-thumb');

    const btnPlay = wrapper.querySelector('.v-btn-play') || wrapper.querySelector('#v-btn-play');
    const btnRewind = wrapper.querySelector('.v-btn-rewind') || wrapper.querySelector('#v-btn-rewind');
    const btnForward = wrapper.querySelector('.v-btn-forward') || wrapper.querySelector('#v-btn-forward');
    const timeCurrent = wrapper.querySelector('.v-time-current') || wrapper.querySelector('#v-time-current');
    const timeDuration = wrapper.querySelector('.v-time-duration') || wrapper.querySelector('#v-time-duration');

    const btnVolume = wrapper.querySelector('.v-btn-volume') || wrapper.querySelector('#v-btn-volume');
    const volumeSlider = wrapper.querySelector('.v-volume-slider') || wrapper.querySelector('#v-volume-slider');
    const btnSpeed = wrapper.querySelector('.v-btn-speed') || wrapper.querySelector('#v-btn-speed');
    const btnLoop = wrapper.querySelector('.v-btn-loop') || wrapper.querySelector('#v-btn-loop');
    const btnFullscreen = wrapper.querySelector('.v-btn-fullscreen') || wrapper.querySelector('#v-btn-fullscreen');

    if (!video) return;

    let isScrubbing = false;
    let controlsTimeout = null;
    const speeds = [1, 1.25, 1.5, 2, 0.5];
    let speedIndex = 0;

    function formatTime(sec) {
      if (!sec || isNaN(sec)) return '0:00';
      const m = Math.floor(sec / 60);
      const s = Math.floor(sec % 60);
      return `${m}:${s < 10 ? '0' : ''}${s}`;
    }

    function updatePlayState(isPlaying) {
      const playIcons = wrapper.querySelectorAll('.icon-play');
      const pauseIcons = wrapper.querySelectorAll('.icon-pause');
      playIcons.forEach(el => el.style.display = isPlaying ? 'none' : 'block');
      pauseIcons.forEach(el => el.style.display = isPlaying ? 'block' : 'none');

      if (isPlaying) {
        centerOverlay.style.opacity = '0';
        centerOverlay.style.pointerEvents = 'none';
        resetControlsTimer();
      } else {
        centerOverlay.style.opacity = '1';
        centerOverlay.style.pointerEvents = 'auto';
        wrapper.classList.remove('hide-controls');
      }
    }

    function togglePlay() {
      if (video.paused || video.ended) {
        video.play().catch(e => console.log('Autoplay prevent:', e));
      } else {
        video.pause();
      }
    }

    // Playback events
    video.addEventListener('play', () => {
      document.querySelectorAll('video, audio').forEach(el => {
        if (el !== video && !el.paused) el.pause();
      });
      updatePlayState(true);
    });
    video.addEventListener('pause', () => updatePlayState(false));
    video.addEventListener('ended', () => updatePlayState(false));

    video.addEventListener('waiting', () => {
      spinner.style.display = 'block';
      bigPlayBtn.style.display = 'none';
    });

    video.addEventListener('playing', () => {
      spinner.style.display = 'none';
      bigPlayBtn.style.display = 'flex';
    });

    video.addEventListener('canplay', () => {
      spinner.style.display = 'none';
      bigPlayBtn.style.display = 'flex';
    });

    video.addEventListener('timeupdate', () => {
      if (isScrubbing) return;
      const cur = video.currentTime;
      const dur = video.duration || 0;
      timeCurrent.textContent = formatTime(cur);
      if (dur > 0) {
        const pct = (cur / dur) * 100;
        progress.style.width = `${pct}%`;
        thumb.style.left = `${pct}%`;
      }

      // Update buffer
      if (video.buffered.length > 0 && dur > 0) {
        const bufferedEnd = video.buffered.end(video.buffered.length - 1);
        const bufPct = Math.min(100, (bufferedEnd / dur) * 100);
        bufferBar.style.width = `${bufPct}%`;
      }
    });

    video.addEventListener('loadedmetadata', () => {
      timeDuration.textContent = formatTime(video.duration);
      timeCurrent.textContent = '0:00';
    });

    // Big Center & Bar play buttons
    if (bigPlayBtn) bigPlayBtn.addEventListener('click', togglePlay);
    if (btnPlay) btnPlay.addEventListener('click', togglePlay);
    video.addEventListener('click', togglePlay);

    // Rewind / Forward 5s
    if (btnRewind) btnRewind.addEventListener('click', () => {
      video.currentTime = Math.max(0, video.currentTime - 5);
      showControlsBriefly();
    });

    if (btnForward) btnForward.addEventListener('click', () => {
      video.currentTime = Math.min(video.duration || 0, video.currentTime + 5);
      showControlsBriefly();
    });

    // Timeline Scrubbing
    function seekToEvent(e) {
      const rect = timeline.getBoundingClientRect();
      const clientX = e.touches ? e.touches[0].clientX : e.clientX;
      const clickX = Math.max(0, Math.min(clientX - rect.left, rect.width));
      const pct = (clickX / rect.width);
      if (video.duration) {
        video.currentTime = pct * video.duration;
      }
      progress.style.width = `${pct * 100}%`;
      thumb.style.left = `${pct * 100}%`;
    }

    timeline.addEventListener('mousedown', (e) => {
      isScrubbing = true;
      seekToEvent(e);
      const onMouseMove = (ev) => seekToEvent(ev);
      const onMouseUp = () => {
        isScrubbing = false;
        window.removeEventListener('mousemove', onMouseMove);
        window.removeEventListener('mouseup', onMouseUp);
      };
      window.addEventListener('mousemove', onMouseMove);
      window.addEventListener('mouseup', onMouseUp);
    });

    timeline.addEventListener('touchstart', (e) => {
      isScrubbing = true;
      seekToEvent(e);
      const onTouchMove = (ev) => seekToEvent(ev);
      const onTouchEnd = () => {
        isScrubbing = false;
        window.removeEventListener('touchmove', onTouchMove);
        window.removeEventListener('touchend', onTouchEnd);
      };
      window.addEventListener('touchmove', onTouchMove);
      window.addEventListener('touchend', onTouchEnd);
    }, { passive: true });

    // Volume & Mute
    let lastVolume = 1;
    if (volumeSlider) {
      volumeSlider.addEventListener('input', (e) => {
        const val = parseFloat(e.target.value);
        video.volume = val;
        video.muted = (val === 0);
        updateVolumeIcons(val > 0 && !video.muted);
      });
    }

    function updateVolumeIcons(isAudible) {
      const volHigh = wrapper.querySelector('.icon-vol-high');
      const volMute = wrapper.querySelector('.icon-vol-mute');
      if (volHigh && volMute) {
        volHigh.style.display = isAudible ? 'block' : 'none';
        volMute.style.display = isAudible ? 'none' : 'block';
      }
    }

    if (btnVolume) {
      btnVolume.addEventListener('click', () => {
        if (video.muted || video.volume === 0) {
          video.muted = false;
          video.volume = lastVolume > 0 ? lastVolume : 1;
          volumeSlider.value = video.volume;
          updateVolumeIcons(true);
        } else {
          lastVolume = video.volume;
          video.muted = true;
          volumeSlider.value = 0;
          updateVolumeIcons(false);
        }
        showControlsBriefly();
      });
    }

    // Playback Speed
    if (btnSpeed) {
      btnSpeed.addEventListener('click', () => {
        speedIndex = (speedIndex + 1) % speeds.length;
        const spd = speeds[speedIndex];
        video.playbackRate = spd;
        btnSpeed.textContent = `${spd}x`;
        showControlsBriefly();
      });
    }

    // Loop
    if (btnLoop) {
      btnLoop.addEventListener('click', () => {
        video.loop = !video.loop;
        btnLoop.classList.toggle('active', video.loop);
        showToast(video.loop ? 'Video loop enabled' : 'Video loop disabled');
        showControlsBriefly();
      });
    }

    // Fullscreen
    if (btnFullscreen) {
      btnFullscreen.addEventListener('click', () => {
        if (!document.fullscreenElement) {
          if (wrapper.requestFullscreen) {
            wrapper.requestFullscreen();
          } else if (wrapper.webkitRequestFullscreen) {
            wrapper.webkitRequestFullscreen();
          }
        } else {
          if (document.exitFullscreen) {
            document.exitFullscreen();
          }
        }
      });
    }

    // Auto-hide controls during playback
    function resetControlsTimer() {
      clearTimeout(controlsTimeout);
      wrapper.classList.remove('hide-controls');
      if (!video.paused) {
        controlsTimeout = setTimeout(() => {
          if (!video.paused && !isScrubbing) {
            wrapper.classList.add('hide-controls');
          }
        }, 2600);
      }
    }

    function showControlsBriefly() {
      resetControlsTimer();
    }

    wrapper.addEventListener('mousemove', resetControlsTimer);
    wrapper.addEventListener('touchstart', resetControlsTimer, { passive: true });
    wrapper.addEventListener('mouseleave', () => {
      if (!video.paused) {
        wrapper.classList.add('hide-controls');
      }
    });

    // Keyboard Shortcuts (when video in view or active)
    window.addEventListener('keydown', (e) => {
      if (document.activeElement && (document.activeElement.tagName === 'INPUT' || document.activeElement.tagName === 'TEXTAREA')) {
        return;
      }
      if (wrapper.offsetParent === null) return; // not visible

      if (e.code === 'Space') {
        e.preventDefault();
        togglePlay();
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        video.currentTime = Math.max(0, video.currentTime - 5);
        showControlsBriefly();
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        video.currentTime = Math.min(video.duration || 0, video.currentTime + 5);
        showControlsBriefly();
      } else if (e.code === 'KeyF') {
        e.preventDefault();
        btnFullscreen?.click();
      } else if (e.code === 'KeyM') {
        e.preventDefault();
        btnVolume?.click();
      }
    });
  }
});
