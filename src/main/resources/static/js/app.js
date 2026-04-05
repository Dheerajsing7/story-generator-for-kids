// ===== State =====
let currentLang = 'en';
let currentStory = null;
let selectedTheme = null;
let selectedAgeGroup = '6-8';
let currentSceneIndex = 0;
let autoPlayInterval = null;
let isSpeaking = false;
let uploadedFile = null;

const i18n = {
    en: {
        tagline: 'Create magical stories for kids!',
        inputTitle: '📝 Create Your Story',
        tabText: 'Text Story', tabImage: 'Image Story',
        charLabel: '👤 Character Name', themeLabel: '🎭 Choose a Theme',
        generateBtn: 'Generate Story!', imgCharLabel: '👤 Character Name (optional)',
        uploadText: 'Click or drag an image here', imgGenerateBtn: 'Generate from Image!',
        loadingText: 'Creating your magical story...',
        choicesTitle: 'What should happen next?',
        completeText: 'The End! What a wonderful story!',
        newStoryBtn: 'Create Another Story',
        charPlaceholder: "Enter hero's name...",
        footer: 'Intelligent Story Generator',
        selectTheme: 'Please select a theme first! 🎭',
        uploadImage: 'Please upload an image first! 📸',
        errorGeneric: 'Something went wrong! Please try again.',
        storyCreated: 'Your magical story is ready! ✨',
        ageLabel: '👶 Age Group',
        ageToddler: 'Toddler', ageExplorer: 'Explorer', ageAdventurer: 'Adventurer'
    },
    hi: {
        tagline: 'बच्चों के लिए जादुई कहानियाँ बनाएं!',
        inputTitle: '📝 अपनी कहानी बनाएं',
        tabText: 'लिखित कहानी', tabImage: 'चित्र कहानी',
        charLabel: '👤 पात्र का नाम', themeLabel: '🎭 विषय चुनें',
        generateBtn: 'कहानी बनाओ!', imgCharLabel: '👤 पात्र का नाम (वैकल्पिक)',
        uploadText: 'यहाँ चित्र अपलोड करें', imgGenerateBtn: 'चित्र से कहानी बनाओ!',
        loadingText: 'आपकी जादुई कहानी बन रही है...',
        choicesTitle: 'आगे क्या होना चाहिए?',
        completeText: 'समाप्त! क्या शानदार कहानी थी!',
        newStoryBtn: 'एक और कहानी बनाओ',
        charPlaceholder: 'नायक का नाम लिखें...',
        footer: 'बुद्धिमान कहानी जनरेटर',
        selectTheme: 'कृपया पहले एक विषय चुनें! 🎭',
        uploadImage: 'कृपया पहले एक चित्र अपलोड करें! 📸',
        errorGeneric: 'कुछ गलत हो गया! कृपया पुनः प्रयास करें।',
        storyCreated: 'आपकी जादुई कहानी तैयार है! ✨',
        ageLabel: '👶 उम्र समूह',
        ageToddler: 'छोटे बच्चे', ageExplorer: 'खोजी', ageAdventurer: 'साहसी'
    },
    hinglish: {
        tagline: 'Bacchon ke liye magical kahaniyaan banao!',
        inputTitle: '📝 Apni Kahani Banao',
        tabText: 'Text Kahani', tabImage: 'Image Kahani',
        charLabel: '👤 Character Ka Naam', themeLabel: '🎭 Theme Choose Karo',
        generateBtn: 'Kahani Banao!', imgCharLabel: '👤 Character Ka Naam (optional)',
        uploadText: 'Yahan image upload ya drag karo', imgGenerateBtn: 'Image Se Kahani Banao!',
        loadingText: 'Aapki magical kahani ban rahi hai...',
        choicesTitle: 'Aage kya hona chahiye?',
        completeText: 'The End! Kya zabardast kahani thi!',
        newStoryBtn: 'Ek Aur Kahani Banao',
        charPlaceholder: 'Hero ka naam likho...',
        footer: 'Intelligent Story Generator',
        selectTheme: 'Pehle ek theme select karo! 🎭',
        uploadImage: 'Pehle ek image upload karo! 📸',
        errorGeneric: 'Kuch gadbad ho gayi! Please dobara try karo.',
        storyCreated: 'Aapki magical kahani ready hai! ✨',
        ageLabel: '👶 Age Group',
        ageToddler: 'Chhote Bacche', ageExplorer: 'Explorer', ageAdventurer: 'Adventurer'
    },
    bn: {
        tagline: 'শিশুদের জন্য জাদুকরী গল্প তৈরি করুন!',
        inputTitle: '📝 তোমার গল্প তৈরি করো',
        tabText: 'লিখিত গল্প', tabImage: 'ছবির গল্প',
        charLabel: '👤 চরিত্রের নাম', themeLabel: '🎭 থিম বেছে নাও',
        generateBtn: 'গল্প তৈরি করো!', imgCharLabel: '👤 চরিত্রের নাম (ঐচ্ছিক)',
        uploadText: 'এখানে ছবি আপলোড করো', imgGenerateBtn: 'ছবি থেকে গল্প তৈরি করো!',
        loadingText: 'তোমার জাদুকরী গল্প তৈরি হচ্ছে...',
        choicesTitle: 'এরপর কী হওয়া উচিত?',
        completeText: 'শেষ! কী চমৎকার গল্প!',
        newStoryBtn: 'আরেকটা গল্প তৈরি করো',
        charPlaceholder: 'নায়কের নাম লেখো...',
        footer: 'বুদ্ধিমান গল্প জেনারেটর',
        selectTheme: 'আগে একটা থিম বেছে নাও! 🎭',
        uploadImage: 'আগে একটা ছবি আপলোড করো! 📸',
        errorGeneric: 'কিছু ভুল হয়ে গেছে! আবার চেষ্টা করো।',
        storyCreated: 'তোমার জাদুকরী গল্প তৈরি হয়ে গেছে! ✨',
        ageLabel: '👶 বয়সের গ্রুপ',
        ageToddler: 'ছোট শিশু', ageExplorer: 'অনুসন্ধানী', ageAdventurer: 'অভিযাত্রী'
    }
};

// ===== Toast Notification System =====
function createToastContainer() {
    if (document.getElementById('toastContainer')) return;
    const container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    document.body.appendChild(container);
}

function showToast(message, type = 'info', duration = 3500) {
    createToastContainer();
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    const icons = { success: '✅', error: '⚠️', info: 'ℹ️' };
    toast.innerHTML = `<span class="toast-icon">${icons[type] || '✨'}</span><span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('toast-exit');
        setTimeout(() => toast.remove(), 300);
    }, duration);
}

// ===== Initialization =====
document.addEventListener('DOMContentLoaded', () => {
    createToastContainer();
    loadThemes();
    setupDragDrop();
});

async function loadThemes() {
    try {
        const res = await fetch(`/api/themes?language=${currentLang}`);
        const themes = await res.json();
        const grid = document.getElementById('themeGrid');
        grid.innerHTML = themes.map(t => `
            <div class="theme-card" data-theme="${t.id}" onclick="selectTheme('${t.id}', this)">
                <span class="theme-emoji">${t.emoji}</span>
                <span class="theme-name">${t.name}</span>
            </div>
        `).join('');
        if (selectedTheme) {
            const card = grid.querySelector(`[data-theme="${selectedTheme}"]`);
            if (card) card.classList.add('selected');
        }
    } catch (e) {
        console.error('Failed to load themes:', e);
        showToast('Failed to load themes', 'error');
    }
}

function selectTheme(theme, el) {
    document.querySelectorAll('.theme-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    selectedTheme = theme;
}

// ===== Age Group Selection =====
function selectAgeGroup(age, el) {
    // Update all age group grids (both forms)
    document.querySelectorAll('.age-card').forEach(c => c.classList.remove('selected'));
    document.querySelectorAll(`.age-card[data-age="${age}"]`).forEach(c => c.classList.add('selected'));
    selectedAgeGroup = age;
}

// ===== Language Switching =====
function switchLanguage(lang) {
    currentLang = lang;
    document.getElementById('btnEn').classList.toggle('active', lang === 'en');
    document.getElementById('btnHi').classList.toggle('active', lang === 'hi');
    document.getElementById('btnHinglish').classList.toggle('active', lang === 'hinglish');
    document.getElementById('btnBn').classList.toggle('active', lang === 'bn');
    const t = i18n[lang] || i18n.en;
    document.getElementById('tagline').textContent = t.tagline;
    document.getElementById('inputTitle').textContent = t.inputTitle;
    document.getElementById('tabTextLabel').textContent = t.tabText;
    document.getElementById('tabImageLabel').textContent = t.tabImage;
    document.getElementById('charLabel').textContent = t.charLabel;
    document.getElementById('themeLabel').textContent = t.themeLabel;
    document.getElementById('btnGenerateText').textContent = t.generateBtn;
    document.getElementById('imgCharLabel').textContent = t.imgCharLabel;
    document.getElementById('uploadText').textContent = t.uploadText;
    document.getElementById('btnImageText').textContent = t.imgGenerateBtn;
    document.getElementById('loadingText').textContent = t.loadingText;
    document.getElementById('choicesTitle').textContent = t.choicesTitle;
    document.getElementById('completeText').textContent = t.completeText;
    document.getElementById('btnNewStoryText').textContent = t.newStoryBtn;
    document.getElementById('characterName').placeholder = t.charPlaceholder;
    document.getElementById('imgCharacterName').placeholder = t.charPlaceholder;
    document.getElementById('footerText').textContent = t.footer;
    // Age group labels
    document.getElementById('ageLabel').textContent = t.ageLabel;
    const el = document.getElementById('imgAgeLabel');
    if (el) el.textContent = t.ageLabel;
    document.getElementById('ageLabel1').textContent = t.ageToddler;
    document.getElementById('ageLabel2').textContent = t.ageExplorer;
    document.getElementById('ageLabel3').textContent = t.ageAdventurer;
    // Also update img form age labels
    const imgAgeLabels = document.querySelectorAll('#imgAgeGroupGrid .age-label');
    if (imgAgeLabels.length >= 3) {
        imgAgeLabels[0].textContent = t.ageToddler;
        imgAgeLabels[1].textContent = t.ageExplorer;
        imgAgeLabels[2].textContent = t.ageAdventurer;
    }
    loadThemes();
    const langToasts = {en:'Language switched to English', hi:'भाषा हिन्दी में बदली गई', hinglish:'Language Hinglish mein badal gayi', bn:'ভাষা বাংলায় পরিবর্তন হয়েছে'};
    showToast(langToasts[lang] || langToasts.en, 'info', 2000);
}

// ===== Tab Switching =====
function switchTab(tab) {
    document.getElementById('tabText').classList.toggle('active', tab === 'text');
    document.getElementById('tabImage').classList.toggle('active', tab === 'image');
    document.getElementById('textForm').classList.toggle('hidden', tab !== 'text');
    document.getElementById('imageForm').classList.toggle('hidden', tab !== 'image');
}

// ===== Story Generation =====
async function generateStory(e) {
    e.preventDefault();
    if (!selectedTheme) {
        shakeElement(document.getElementById('themeGrid'));
        showToast(i18n[currentLang].selectTheme, 'error');
        return;
    }
    const charName = document.getElementById('characterName').value.trim();
    showLoading();
    disableButtons(true);
    try {
        const res = await fetch('/api/story/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                characterName: charName,
                theme: selectedTheme,
                language: currentLang,
                ageGroup: selectedAgeGroup
            })
        });
        const data = await res.json();
        if (data.error) {
            showToast(data.error, 'error');
            hideLoading();
            disableButtons(false);
            return;
        }
        displayStory(data);
        showToast(i18n[currentLang].storyCreated, 'success');
    } catch (e) {
        console.error(e);
        showToast(i18n[currentLang].errorGeneric, 'error');
    }
    hideLoading();
    disableButtons(false);
}

async function generateImageStory(e) {
    e.preventDefault();
    if (!uploadedFile) {
        shakeElement(document.getElementById('uploadZone'));
        showToast(i18n[currentLang].uploadImage, 'error');
        return;
    }
    const charName = document.getElementById('imgCharacterName').value.trim();
    const formData = new FormData();
    formData.append('image', uploadedFile);
    formData.append('language', currentLang);
    formData.append('characterName', charName);
    formData.append('ageGroup', selectedAgeGroup);
    showLoading();
    disableButtons(true);
    try {
        const res = await fetch('/api/story/image', { method: 'POST', body: formData });
        const data = await res.json();
        if (data.error) {
            showToast(data.error, 'error');
            hideLoading();
            disableButtons(false);
            return;
        }
        displayStory(data);
        showToast(i18n[currentLang].storyCreated, 'success');
    } catch (e) {
        console.error(e);
        showToast(i18n[currentLang].errorGeneric, 'error');
    }
    hideLoading();
    disableButtons(false);
}

async function makeChoice(storyId, choiceId) {
    showLoading();
    disableButtons(true);
    try {
        const res = await fetch('/api/story/continue', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ storyId, choiceId })
        });
        const data = await res.json();
        displayStory(data);
    } catch (e) {
        console.error(e);
        showToast(i18n[currentLang].errorGeneric, 'error');
    }
    hideLoading();
    disableButtons(false);
}

// ===== Display =====
function displayStory(story) {
    currentStory = story;
    document.getElementById('inputSection').classList.add('hidden');
    document.getElementById('storySection').classList.remove('hidden');
    document.getElementById('storyTitle').textContent = story.title || 'Your Story';
    document.getElementById('storyContent').textContent = story.content;

    const choicesSection = document.getElementById('choicesSection');
    const completeSection = document.getElementById('storyComplete');

    if (story.complete || !story.choices || story.choices.length === 0) {
        choicesSection.classList.add('hidden');
        completeSection.classList.remove('hidden');
    } else {
        completeSection.classList.add('hidden');
        choicesSection.classList.remove('hidden');
        document.getElementById('choicesGrid').innerHTML = story.choices.map(c => `
            <button class="choice-btn" onclick="makeChoice('${story.storyId}', '${c.choiceId}')">
                <span class="choice-emoji">${c.emoji || '✨'}</span>
                <span>${c.text}</span>
            </button>
        `).join('');
    }

    document.getElementById('storySection').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function createNewStory() {
    currentStory = null;
    uploadedFile = null;
    stopNarration();
    document.getElementById('storySection').classList.add('hidden');
    document.getElementById('storyComplete').classList.add('hidden');
    document.getElementById('inputSection').classList.remove('hidden');
    // Reset image upload
    const preview = document.getElementById('imagePreview');
    preview.classList.add('hidden');
    preview.src = '';
    document.getElementById('uploadIcon').textContent = '📸';
    document.getElementById('uploadZone').classList.remove('has-file');
    document.getElementById('imageUpload').value = '';
    document.getElementById('inputSection').scrollIntoView({ behavior: 'smooth' });
}

// ===== Image Upload =====
function handleImageUpload(e) {
    const file = e.target.files[0];
    if (file) {
        uploadedFile = file;
        const reader = new FileReader();
        reader.onload = (ev) => {
            const preview = document.getElementById('imagePreview');
            preview.src = ev.target.result;
            preview.classList.remove('hidden');
            document.getElementById('uploadIcon').textContent = '✅';
            document.getElementById('uploadZone').classList.add('has-file');
        };
        reader.readAsDataURL(file);
        showToast(`Image "${file.name}" uploaded! 📸`, 'success', 2500);
    }
}

function setupDragDrop() {
    const zone = document.getElementById('uploadZone');
    zone.addEventListener('dragover', e => { e.preventDefault(); zone.classList.add('drag-over'); });
    zone.addEventListener('dragleave', () => zone.classList.remove('drag-over'));
    zone.addEventListener('drop', e => {
        e.preventDefault(); zone.classList.remove('drag-over');
        if (e.dataTransfer.files.length) {
            document.getElementById('imageUpload').files = e.dataTransfer.files;
            handleImageUpload({ target: { files: e.dataTransfer.files } });
        }
    });
}

// ===== Text-to-Speech =====
function toggleNarration() {
    if (isSpeaking) { stopNarration(); return; }
    if (!currentStory) return;
    const utterance = new SpeechSynthesisUtterance(currentStory.content);
    utterance.lang = ({en:'en-US', hi:'hi-IN', hinglish:'hi-IN', bn:'bn-IN'})[currentStory.language] || 'en-US';
    utterance.rate = 0.85;
    utterance.pitch = 1.1;
    utterance.onend = () => { isSpeaking = false; document.getElementById('btnNarrate').classList.remove('active'); };
    utterance.onerror = () => { isSpeaking = false; document.getElementById('btnNarrate').classList.remove('active'); };
    speechSynthesis.speak(utterance);
    isSpeaking = true;
    document.getElementById('btnNarrate').classList.add('active');
    showToast(({en:'🔊 Listening...', hi:'🔊 सुनिए...', hinglish:'🔊 Suniye...', bn:'🔊 শুনুন...'})[currentLang] || '🔊 Listening...', 'info', 2000);
}

function stopNarration() {
    speechSynthesis.cancel();
    isSpeaking = false;
    const btn = document.getElementById('btnNarrate');
    if (btn) btn.classList.remove('active');
}

// ===== Slideshow =====
function openSlideshow() {
    if (!currentStory || !currentStory.scenes || currentStory.scenes.length === 0) {
        showToast('No scenes available for slideshow', 'info');
        return;
    }
    currentSceneIndex = 0;
    document.getElementById('slideshowModal').classList.remove('hidden');
    showScene(0);
    startAutoPlay();
}

function closeSlideshow() {
    document.getElementById('slideshowModal').classList.add('hidden');
    stopAutoPlay();
    speechSynthesis.cancel();
}

function showScene(idx) {
    const scenes = currentStory.scenes;
    if (idx < 0 || idx >= scenes.length) return;
    currentSceneIndex = idx;
    const scene = scenes[idx];
    document.getElementById('sceneEmoji').textContent = scene.emoji || '✨';
    document.getElementById('sceneText').textContent = scene.text;
    document.getElementById('sceneCounter').textContent = `${idx + 1} / ${scenes.length}`;
    document.getElementById('slideshowScreen').style.backgroundColor = scene.backgroundColor || '#FFF';
    document.getElementById('progressFill').style.width = `${((idx + 1) / scenes.length) * 100}%`;
}

function nextScene() {
    if (currentStory && currentSceneIndex < currentStory.scenes.length - 1) showScene(currentSceneIndex + 1);
    else stopAutoPlay();
}

function prevScene() { if (currentSceneIndex > 0) showScene(currentSceneIndex - 1); }

function startAutoPlay() {
    stopAutoPlay();
    autoPlayInterval = setInterval(nextScene, 4000);
    document.getElementById('btnPlayPause').textContent = '⏸️';
}

function stopAutoPlay() {
    if (autoPlayInterval) { clearInterval(autoPlayInterval); autoPlayInterval = null; }
    document.getElementById('btnPlayPause').textContent = '▶️';
}

function toggleAutoPlay() { autoPlayInterval ? stopAutoPlay() : startAutoPlay(); }

function toggleSceneNarration() {
    if (!currentStory || !currentStory.scenes) return;
    const scene = currentStory.scenes[currentSceneIndex];
    if (speechSynthesis.speaking) { speechSynthesis.cancel(); return; }
    const u = new SpeechSynthesisUtterance(scene.text);
    u.lang = ({en:'en-US', hi:'hi-IN', hinglish:'hi-IN', bn:'bn-IN'})[currentStory.language] || 'en-US';
    u.rate = 0.85; u.pitch = 1.1;
    speechSynthesis.speak(u);
}

// ===== Helpers =====
function showLoading() { document.getElementById('loading').classList.remove('hidden'); }
function hideLoading() { document.getElementById('loading').classList.add('hidden'); }

function disableButtons(disabled) {
    document.querySelectorAll('.btn-generate, .choice-btn').forEach(btn => {
        btn.disabled = disabled;
    });
}

function shakeElement(el) {
    el.style.animation = 'none';
    el.offsetHeight; // trigger reflow
    el.style.animation = 'shake 0.5s ease-in-out';
    setTimeout(() => el.style.animation = '', 500);
}

// Shake animation
const style = document.createElement('style');
style.textContent = '@keyframes shake{0%,100%{transform:translateX(0)}25%{transform:translateX(-10px)}75%{transform:translateX(10px)}}';
document.head.appendChild(style);

// Keyboard shortcut: Escape to close slideshow
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') {
        const modal = document.getElementById('slideshowModal');
        if (modal && !modal.classList.contains('hidden')) {
            closeSlideshow();
        }
    }
    if (e.key === 'ArrowRight') {
        const modal = document.getElementById('slideshowModal');
        if (modal && !modal.classList.contains('hidden')) nextScene();
    }
    if (e.key === 'ArrowLeft') {
        const modal = document.getElementById('slideshowModal');
        if (modal && !modal.classList.contains('hidden')) prevScene();
    }
});
