/* -------------------------------------------------------
   KIT AI v2.1 — Application Logic Engine (SPA)
   New: Career DNA, Batch Screening, Candidate Compare,
        Toast Notifications, Recruiter Notes, Re-Screen,
        Preferred Skills, Keyboard Shortcuts
-------------------------------------------------------- */
const API_BASE = '';

let state = {
    jobs: [],
    screenings: [],
    selectedFile: null,
    activeEngine: 'offline',
    batchEngine: 'offline',
    batchFiles: [],
    compareSelectedIds: new Set(),
    dbProfile: 'h2',
    rescreenResultId: null,   // tracks which result is currently open in the modal
    charts: { distribution: null, skills: null, dnaRadar: null, dnaTrends: null }
};

document.addEventListener('DOMContentLoaded', () => {
    checkAuthentication();
    initSPA();
    loadSettings();
    loadJobs();
    loadScreenings();
    initDragAndDrop();
    initBatchDragAndDrop();
    registerEventListeners();
    registerCoachListeners();
    updateAnalytics();
    loadDnaSection();
    initKeyboardShortcuts();
});

/* -------------------------------------------------------
   Authentication
------------------------------------------------------- */
function checkAuthentication() {
    const loggedIn = sessionStorage.getItem('kit_ai_logged_in') === 'true';
    const loginScreen = document.getElementById('login-screen');
    const profileUsername = document.getElementById('profile-username');
    if (loggedIn) {
        if (loginScreen) loginScreen.classList.add('hidden');
        document.body.style.overflow = '';
        const username = sessionStorage.getItem('kit_ai_username') || 'Recruiter Admin';
        if (profileUsername) profileUsername.textContent = username;
    } else {
        if (loginScreen) loginScreen.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
    }
}

/* -------------------------------------------------------
   SPA Navigation
------------------------------------------------------- */
function initSPA() {
    const navItems = document.querySelectorAll('.nav-item');
    const sections = document.querySelectorAll('.content-section');

    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const targetId = item.getAttribute('data-target');
            navItems.forEach(n => n.classList.remove('active'));
            item.classList.add('active');
            sections.forEach(s => {
                s.classList.remove('active');
                if (s.id === targetId) s.classList.add('active');
            });
            if (targetId === 'dashboard-section') { loadScreenings(); loadDnaTopArchetype(); }
            else if (targetId === 'jobs-section') loadJobs();
            else if (targetId === 'analytics-section') updateAnalytics();
            else if (targetId === 'compare-section') refreshCompareList();
            else if (targetId === 'dna-section') loadDnaSection();
            else if (targetId === 'coach-section') initCoachSection();
        });
    });

    if (window.location.hash) {
        const hashTarget = window.location.hash.substring(1);
        const item = document.querySelector(`.nav-item[href="#${hashTarget}"]`);
        if (item) item.click();
    }
}

/* -------------------------------------------------------
   Single Resume Drag & Drop
------------------------------------------------------- */
function initDragAndDrop() {
    const dropZone = document.getElementById('resume-drag-drop-zone');
    const fileInput = document.getElementById('resume-file-input');
    const browseBtn = document.getElementById('btn-browse-file');
    const removeBtn = document.getElementById('btn-remove-selected-file');
    const filePanel = document.getElementById('selected-file-panel');
    const fileNameText = document.getElementById('selected-file-name');
    const runBtn = document.getElementById('btn-run-screening');

    browseBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', (e) => handleFileSelect(e.target.files[0]));
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
    ['dragleave','dragend'].forEach(ev => dropZone.addEventListener(ev, () => dropZone.classList.remove('dragover')));
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault(); dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) handleFileSelect(e.dataTransfer.files[0]);
    });
    removeBtn.addEventListener('click', () => {
        state.selectedFile = null; fileInput.value = '';
        filePanel.classList.add('hidden'); dropZone.classList.remove('hidden');
        runBtn.disabled = true;
    });

    function handleFileSelect(file) {
        if (!file) return;
        const validExtensions = ['pdf','docx','txt','doc','rtf'];
        const ext = file.name.split('.').pop().toLowerCase();
        if (!validExtensions.includes(ext)) { toast('Unsupported file format. Use PDF, DOCX, TXT or RTF.', 'warning'); return; }
        if (file.size > 10 * 1024 * 1024) { toast('File exceeds the 10MB limit.', 'warning'); return; }
        state.selectedFile = file;
        fileNameText.textContent = file.name;
        const icon = document.getElementById('selected-file-icon');
        icon.className = 'fa-solid ' + (ext === 'pdf' ? 'fa-file-pdf' : ext === 'docx' || ext === 'doc' ? 'fa-file-word' : 'fa-file-lines');
        dropZone.classList.add('hidden');
        filePanel.classList.remove('hidden');
        validateScreeningForm();
    }
}

function validateScreeningForm() {
    const jobSelect = document.getElementById('screen-job-select');
    const runBtn = document.getElementById('btn-run-screening');
    runBtn.disabled = !(state.selectedFile && jobSelect.value);
}

/* -------------------------------------------------------
   Batch Drag & Drop
------------------------------------------------------- */
function initBatchDragAndDrop() {
    const dropZone = document.getElementById('batch-drop-zone');
    const fileInput = document.getElementById('batch-file-input');
    const browseBtn = document.getElementById('btn-batch-browse');

    if (!dropZone) return;

    browseBtn.addEventListener('click', () => fileInput.click());
    fileInput.addEventListener('change', (e) => handleBatchFiles(Array.from(e.target.files)));
    dropZone.addEventListener('dragover', (e) => { e.preventDefault(); dropZone.classList.add('dragover'); });
    ['dragleave','dragend'].forEach(ev => dropZone.addEventListener(ev, () => dropZone.classList.remove('dragover')));
    dropZone.addEventListener('drop', (e) => {
        e.preventDefault(); dropZone.classList.remove('dragover');
        if (e.dataTransfer.files.length > 0) handleBatchFiles(Array.from(e.dataTransfer.files));
    });
}

function handleBatchFiles(files) {
    const valid = ['pdf','docx','txt','doc','rtf'];
    const filtered = files.filter(f => valid.includes(f.name.split('.').pop().toLowerCase()) && f.size <= 10*1024*1024);
    if (filtered.length === 0) { toast('No valid files found. Use PDF, DOCX, TXT or RTF under 10MB.', 'warning'); return; }
    if (filtered.length > 20) { toast('Maximum 20 files per batch. First 20 will be used.', 'warning'); state.batchFiles = filtered.slice(0, 20); }
    else state.batchFiles = filtered;
    renderBatchFileList();
    validateBatchForm();
}

function renderBatchFileList() {
    const list = document.getElementById('batch-file-list');
    list.innerHTML = '';
    state.batchFiles.forEach((f, i) => {
        const ext = f.name.split('.').pop().toLowerCase();
        const iconClass = ext === 'pdf' ? 'fa-file-pdf' : (ext === 'docx' || ext === 'doc') ? 'fa-file-word' : 'fa-file-lines';
        const div = document.createElement('div');
        div.className = 'batch-file-item';
        div.innerHTML = `<i class="fa-solid ${iconClass}"></i><span>${f.name}</span><span class="file-size">${(f.size/1024).toFixed(0)} KB</span>`;
        list.appendChild(div);
    });
    const zone = document.getElementById('batch-drop-zone');
    if (state.batchFiles.length > 0) zone.classList.add('hidden');
}

function validateBatchForm() {
    const jobSelect = document.getElementById('batch-job-select');
    const runBtn = document.getElementById('btn-run-batch');
    runBtn.disabled = !(state.batchFiles.length > 0 && jobSelect && jobSelect.value);
}

/* -------------------------------------------------------
   Job Management
------------------------------------------------------- */
async function loadJobs() {
    try {
        const response = await fetch(`${API_BASE}/api/jobs`);
        if (!response.ok) throw new Error('Failed to fetch jobs');
        state.jobs = await response.json();
        renderJobs();
        populateJobDropdown();
        populateBatchJobDropdown();
    } catch (err) { console.error('Error loading jobs:', err); }
}

function renderJobs() {
    const container = document.getElementById('jobs-grid-container');
    container.innerHTML = '';
    if (state.jobs.length === 0) {
        container.innerHTML = `<div class="empty-state-card"><i class="fa-solid fa-briefcase"></i><p>No job requisitions created yet.</p></div>`;
        return;
    }
    state.jobs.forEach(job => {
        const skills = job.requiredSkills ? job.requiredSkills.split(',').map(s => s.trim()) : [];
        const skillsHtml = skills.map(s => `<span class="job-skill-tag">${s}</span>`).join('');
        const card = document.createElement('div');
        card.className = 'job-card';
        card.innerHTML = `
            <div>
                <div class="job-card-header">
                    <span class="job-dept">${job.department || 'General'}</span>
                    <button class="btn-icon-only danger btn-delete-job" data-id="${job.id}"><i class="fa-solid fa-trash-can"></i></button>
                </div>
                <h4 class="job-title">${job.title}</h4>
                <div class="job-meta-row">
                    <span class="job-meta-pill"><i class="fa-solid fa-clock"></i> Min ${job.minExperience || 0} Yrs</span>
                    <span class="job-meta-pill"><i class="fa-solid fa-graduation-cap"></i> ${job.minEducation || 'Any'}</span>
                </div>
                <div class="job-skills-container">${skillsHtml}</div>
            </div>
            <div class="job-card-footer">
                <span class="job-stats"><i class="fa-solid fa-clock-rotate-left"></i> ${formatDate(job.createdAt)}</span>
                <button class="btn btn-secondary btn-sm btn-quick-screen" data-id="${job.id}"><i class="fa-solid fa-microchip"></i> Screen</button>
            </div>`;
        container.appendChild(card);
    });
    document.querySelectorAll('.btn-delete-job').forEach(btn => {
        btn.addEventListener('click', async (e) => {
            e.stopPropagation();
            if (confirm('Delete this Job Requisition? Existing screenings will lose their job link.')) await deleteJob(btn.getAttribute('data-id'));
        });
    });
    document.querySelectorAll('.btn-quick-screen').forEach(btn => {
        btn.addEventListener('click', () => {
            document.getElementById('screen-job-select').value = btn.getAttribute('data-id');
            validateScreeningForm();
            document.querySelector('.nav-item[href="#screen"]').click();
        });
    });
}

async function deleteJob(id) {
    try {
        const r = await fetch(`${API_BASE}/api/jobs/${id}`, { method: 'DELETE' });
        if (r.ok) { loadJobs(); toast('Job requisition deleted.', 'info'); }
        else toast('Failed to delete job.', 'error');
    } catch (err) { console.error(err); toast('Network error deleting job.', 'error'); }
}

function populateJobDropdown() {
    const select = document.getElementById('screen-job-select');
    const cur = select.value;
    select.innerHTML = '<option value="" disabled selected>Select Job Opening...</option>';
    state.jobs.forEach(job => {
        const o = document.createElement('option');
        o.value = job.id;
        o.textContent = `${job.title} (${job.department || 'N/A'})`;
        select.appendChild(o);
    });
    if (cur && state.jobs.some(j => j.id == cur)) select.value = cur;
}

function populateBatchJobDropdown() {
    const select = document.getElementById('batch-job-select');
    if (!select) return;
    const cur = select.value;
    select.innerHTML = '<option value="" disabled selected>Select Job Opening...</option>';
    state.jobs.forEach(job => {
        const o = document.createElement('option');
        o.value = job.id;
        o.textContent = `${job.title} (${job.department || 'N/A'})`;
        select.appendChild(o);
    });
    if (cur && state.jobs.some(j => j.id == cur)) select.value = cur;
}

/* -------------------------------------------------------
   Single Resume Screening Pipeline
------------------------------------------------------- */
async function runScreening(e) {
    e.preventDefault();
    const jobId = document.getElementById('screen-job-select').value;
    const consoleBox = document.getElementById('screening-console');
    const terminalBadge = document.getElementById('terminal-status-badge');
    const resultCard = document.getElementById('live-screening-result-card');
    const runBtn = document.getElementById('btn-run-screening');
    if (!state.selectedFile || !jobId) return;

    resultCard.classList.add('hidden');
    consoleBox.innerHTML = '';
    terminalBadge.textContent = 'Processing';
    terminalBadge.className = 'status-indicator processing';
    runBtn.disabled = true;

    function log(msg, type = '') {
        const line = document.createElement('div');
        line.className = `console-line ${type}`;
        line.textContent = `> [${new Date().toLocaleTimeString()}] ${msg}`;
        consoleBox.appendChild(line);
        consoleBox.scrollTop = consoleBox.scrollHeight;
    }

    log('Initializing screening pipeline...', 'text-muted');
    let apiKey = '';
    if (state.activeEngine === 'gemini') {
        apiKey = document.getElementById('screen-api-key').value.trim();
        if (!apiKey) { log('Error: Gemini API Key is missing.', 'error-line'); terminalBadge.textContent = 'Failed'; terminalBadge.className = 'status-indicator idle'; runBtn.disabled = false; return; }
        log('Generative AI screening active: Gemini 2.5 Flash selected.', 'success-line');    } else {
        log('Offline screening active: Pure Java NLP matching enabled.', 'text-muted');
    }
    log(`Target Job ID: ${jobId}`, 'text-muted');
    log(`Uploading: ${state.selectedFile.name} (${Math.round(state.selectedFile.size/1024)} KB)...`, 'text-muted');

    const logSteps = ['Executing Tika engine to extract content...', 'Tika extraction successful.', 'Identifying candidate identifiers...', 'Running skill taxonomy indexing...', 'Matching experience vectors...', 'Computing Career DNA fingerprint...'];
    if (state.activeEngine === 'gemini') logSteps.push('Calling Gemini 2.5 Flash model...');
    else logSteps.push('Calculating TF-IDF cosine similarities...');

    let logIdx = 0;
    const logInterval = setInterval(() => { if (logIdx < logSteps.length) log(logSteps[logIdx++], 'text-muted'); else clearInterval(logInterval); }, 1200);

    const formData = new FormData();
    formData.append('file', state.selectedFile);
    formData.append('jobId', jobId);
    const headers = {};
    if (apiKey) headers['X-Gemini-Key'] = apiKey;

    try {
        const response = await fetch(`${API_BASE}/api/screen`, { method: 'POST', headers, body: formData });
        clearInterval(logInterval);
        if (!response.ok) throw new Error(await response.text() || 'Screening API error');
        const result = await response.json();

        log('Screening complete!', 'success-line');
        log(`Score: ${result.matchScore}% | Status: ${result.matchStatus}`, 'success-line');
        terminalBadge.textContent = 'Success';
        terminalBadge.className = 'status-indicator success';

        document.getElementById('live-result-name').textContent = result.candidate.name;
        document.getElementById('live-result-email').innerHTML = `<i class="fa-regular fa-envelope"></i> ${result.candidate.email}`;
        document.getElementById('live-result-score-text').textContent = `${Math.round(result.matchScore)}%`;
        document.getElementById('live-result-exp').textContent = `${result.candidate.experienceYears || 0} Yrs`;
        document.getElementById('live-result-edu').textContent = result.candidate.education || 'N/A';
        document.getElementById('live-result-status').textContent = result.matchStatus;

        const statusBadge = document.getElementById('live-result-status');
        statusBadge.className = 'badge ' + (result.matchStatus === 'Shortlisted' ? 'badge-success' : result.matchStatus === 'Rejected' ? 'badge-danger' : 'badge-warning');

        const scoreCircle = document.getElementById('live-result-score-circle');
        if (result.matchScore >= 75) { scoreCircle.style.background = 'var(--grad-emerald)'; scoreCircle.style.boxShadow = 'var(--shadow-glow-emerald)'; }
        else if (result.matchScore >= 50) { scoreCircle.style.background = 'var(--accent-orange)'; scoreCircle.style.boxShadow = 'var(--shadow-glow-orange)'; }
        else { scoreCircle.style.background = 'var(--accent-red)'; scoreCircle.style.boxShadow = 'none'; }

        resultCard.classList.remove('hidden');
        document.getElementById('btn-view-live-result-details').onclick = () => showCandidateReport(result);
        loadScreenings(); updateAnalytics(); loadDnaTopArchetype();
        toast(`${result.candidate.name} — ${Math.round(result.matchScore)}% (${result.matchStatus})`,
              result.matchStatus === 'Shortlisted' ? 'success' : result.matchStatus === 'Rejected' ? 'error' : 'info');
    } catch (err) {
        clearInterval(logInterval);
        log(`Critical error: ${err.message}`, 'error-line');
        terminalBadge.textContent = 'Failed';
        terminalBadge.className = 'status-indicator idle';
    } finally { runBtn.disabled = false; }
}

/* -------------------------------------------------------
   Batch Screening Pipeline
------------------------------------------------------- */
async function runBatchScreening(e) {
    e.preventDefault();
    const jobId = document.getElementById('batch-job-select').value;
    if (!jobId || state.batchFiles.length === 0) return;

    const runBtn = document.getElementById('btn-run-batch');
    const statusBadge = document.getElementById('batch-status-badge');
    const progressContainer = document.getElementById('batch-progress-container');
    const progressBar = document.getElementById('batch-progress-bar');
    const progressLabel = document.getElementById('batch-progress-label');
    const resultsList = document.getElementById('batch-results-list');

    runBtn.disabled = true;
    statusBadge.textContent = 'Processing';
    statusBadge.className = 'status-indicator processing';
    progressContainer.classList.remove('hidden');
    progressBar.style.width = '0%';
    progressLabel.textContent = `Processing 0 / ${state.batchFiles.length}...`;
    resultsList.innerHTML = '';

    let apiKey = '';
    if (state.batchEngine === 'gemini') {
        apiKey = document.getElementById('batch-api-key').value.trim();
        if (!apiKey) { toast('Gemini API Key is required for AI screening.', 'warning'); runBtn.disabled = false; return; }
    }

    const formData = new FormData();
    state.batchFiles.forEach(f => formData.append('files', f));
    formData.append('jobId', jobId);
    const headers = {};
    if (apiKey) headers['X-Gemini-Key'] = apiKey;

    // Animate progress while waiting (indeterminate)
    let fakeProgress = 0;
    const fakeInterval = setInterval(() => {
        fakeProgress = Math.min(fakeProgress + (100 / state.batchFiles.length / 8), 90);
        progressBar.style.width = fakeProgress + '%';
    }, 400);

    try {
        const response = await fetch(`${API_BASE}/api/screen/batch`, { method: 'POST', headers, body: formData });
        clearInterval(fakeInterval);
        if (!response.ok) throw new Error(await response.text());
        const data = await response.json();

        progressBar.style.width = '100%';
        progressLabel.textContent = `Done — ${data.totalSucceeded} succeeded, ${data.totalFailed} failed.`;
        statusBadge.textContent = 'Complete';
        statusBadge.className = 'status-indicator success';

        // Sort successful results by score desc
        const successful = data.results.filter(r => r.success).sort((a,b) => b.result.matchScore - a.result.matchScore);
        const failed = data.results.filter(r => !r.success);

        if (successful.length === 0 && failed.length === 0) {
            resultsList.innerHTML = '<div class="empty-state"><i class="fa-solid fa-layer-group"></i><p>No results returned.</p></div>';
        }

        successful.forEach((entry, i) => {
            const r = entry.result;
            const score = Math.round(r.matchScore);
            const scoreClass = score >= 75 ? 'score-high' : score >= 50 ? 'score-mid' : 'score-low';
            const rankClass = i === 0 ? 'rank-1' : i === 1 ? 'rank-2' : i === 2 ? 'rank-3' : '';
            const statusClass = r.matchStatus === 'Shortlisted' ? 'badge-success' : r.matchStatus === 'Rejected' ? 'badge-danger' : 'badge-warning';
            const item = document.createElement('div');
            item.className = 'batch-result-item';
            item.style.cursor = 'pointer';
            item.innerHTML = `
                <div class="batch-result-rank ${rankClass}">${i+1}</div>
                <div class="batch-result-info">
                    <h5>${r.candidate.name}</h5>
                    <span>${entry.filename} &bull; ${r.candidate.experienceYears || 0} yrs exp</span>
                </div>
                <span class="badge ${statusClass}">${r.matchStatus}</span>
                <span class="batch-result-score ${scoreClass}">${score}%</span>`;
            item.addEventListener('click', () => showCandidateReport(r));
            resultsList.appendChild(item);
        });

        failed.forEach(entry => {
            const item = document.createElement('div');
            item.className = 'batch-error-item';
            item.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i><span>${entry.filename}: ${entry.error}</span>`;
            resultsList.appendChild(item);
        });

        loadScreenings(); updateAnalytics(); loadDnaTopArchetype();
        toast(`Batch complete — ${data.totalSucceeded} screened, ${data.totalFailed} failed.`,
              data.totalFailed > 0 ? 'warning' : 'success');
    } catch (err) {
        clearInterval(fakeInterval);
        progressBar.style.width = '100%';
        progressBar.style.background = 'var(--accent-red)';
        progressLabel.textContent = `Error: ${err.message}`;
        statusBadge.textContent = 'Failed';
        statusBadge.className = 'status-indicator idle';
    } finally { runBtn.disabled = false; }
}

/* -------------------------------------------------------
   Screenings List & Reports
------------------------------------------------------- */
async function loadScreenings() {
    try {
        const r = await fetch(`${API_BASE}/api/screen/results`);
        if (!r.ok) throw new Error('Failed to fetch screenings');
        state.screenings = await r.json();
        renderScreenings();
        // Refresh coach dropdown whenever screenings update
        const coachSection = document.getElementById('coach-section');
        if (coachSection && coachSection.classList.contains('active')) {
            initCoachSection();
        }
    } catch (err) { console.error('Error loading screenings:', err); }
}

function renderScreenings() {
    const tableBody = document.querySelector('#screenings-table tbody');
    tableBody.innerHTML = '';
    const filterVal = document.getElementById('global-search').value.toLowerCase();

    const filtered = state.screenings.filter(item => {
        if (!filterVal) return true;
        return item.candidate.name.toLowerCase().includes(filterVal)
            || item.candidate.email.toLowerCase().includes(filterVal)
            || (item.candidate.extractedSkills && item.candidate.extractedSkills.toLowerCase().includes(filterVal))
            || item.jobDescription.title.toLowerCase().includes(filterVal);
    }).sort((a, b) => new Date(b.screenedAt) - new Date(a.screenedAt));

    if (filtered.length === 0) {
        tableBody.innerHTML = `<tr class="empty-state-row"><td colspan="7" class="text-center"><div class="empty-state"><i class="fa-regular fa-folder-open"></i><p>${filterVal ? 'No results match your search.' : 'No candidates screened yet.'}</p></div></td></tr>`;
        return;
    }

    filtered.forEach(item => {
        const initials = item.candidate.name.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();
        const scoreClass = item.matchScore >= 75 ? 'score-high' : item.matchScore < 50 ? 'score-low' : 'score-mid';
        const statusClass = item.matchStatus === 'Shortlisted' ? 'badge-success' : item.matchStatus === 'Rejected' ? 'badge-danger' : 'badge-review';
        const engineBadge = item.screeningMode === 'GEMINI_AI'
            ? `<span class="badge badge-engine-ai"><i class="fa-solid fa-brain"></i> Gemini</span>`
            : `<span class="badge badge-engine-offline"><i class="fa-solid fa-database"></i> Offline</span>`;

        const row = document.createElement('tr');
        row.innerHTML = `
            <td><div class="candidate-info-cell"><div class="candidate-initials">${initials}</div><div class="candidate-meta"><h5>${item.candidate.name}</h5><span>${item.candidate.email}</span></div></div></td>
            <td><div class="candidate-meta"><h5>${item.jobDescription.title}</h5><span>${item.jobDescription.department || 'N/A'}</span></div></td>
            <td><span class="score-cell ${scoreClass}">${Math.round(item.matchScore)}%</span></td>
            <td><span class="badge ${statusClass}">${item.matchStatus}</span></td>
            <td>${engineBadge}</td>
            <td class="text-muted">${formatDate(item.screenedAt)}</td>
            <td><div class="btn-actions">
                <button class="btn-icon-only btn-view-report" data-id="${item.id}" title="View Report"><i class="fa-solid fa-folder-open"></i></button>
                <button class="btn-icon-only danger btn-delete-screening" data-id="${item.id}" title="Delete"><i class="fa-solid fa-trash-can"></i></button>
            </div></td>`;
        tableBody.appendChild(row);
    });

    document.querySelectorAll('.btn-view-report').forEach(btn => {
        btn.addEventListener('click', () => {
            const screening = state.screenings.find(s => s.id == btn.getAttribute('data-id'));
            if (screening) showCandidateReport(screening);
        });
    });
    document.querySelectorAll('.btn-delete-screening').forEach(btn => {
        btn.addEventListener('click', async () => {
            if (confirm('Delete this screening result?')) await deleteScreening(btn.getAttribute('data-id'));
        });
    });
    updateCounters(state.screenings);
}

async function deleteScreening(id) {
    try {
        const r = await fetch(`${API_BASE}/api/screen/results/${id}`, { method: 'DELETE' });
        if (r.ok) { loadScreenings(); updateAnalytics(); toast('Screening result deleted.', 'info'); }
        else toast('Failed to delete screening.', 'error');
    } catch (err) { console.error(err); }
}

function updateCounters(list) {
    document.getElementById('stat-total-screened').textContent = list.length;
    document.getElementById('stat-shortlisted').textContent = list.filter(s => s.matchStatus === 'Shortlisted').length;
    document.getElementById('stat-rejected').textContent = list.filter(s => s.matchStatus === 'Rejected').length;
    const avg = list.length > 0 ? list.reduce((sum, i) => sum + i.matchScore, 0) / list.length : 0;
    document.getElementById('stat-avg-score').textContent = `${Math.round(avg * 10) / 10}%`;
}

/* -------------------------------------------------------
   Candidate Detail Modal + DNA Radar
------------------------------------------------------- */
function showCandidateReport(item) {
    const modal = document.getElementById('candidate-detail-modal');
    const initials = item.candidate.name.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();

    document.getElementById('detail-candidate-avatar').textContent = initials;
    document.getElementById('detail-candidate-name').textContent = item.candidate.name;
    document.getElementById('detail-candidate-email').textContent = item.candidate.email;
    document.getElementById('detail-candidate-phone').textContent = item.candidate.phone || 'Not Provided';
    document.getElementById('detail-candidate-exp').textContent = `${item.candidate.experienceYears || 0} Years`;
    document.getElementById('detail-candidate-edu').textContent = item.candidate.education || 'Not Specified';
    document.getElementById('detail-candidate-engine').textContent = item.screeningMode === 'GEMINI_AI' ? 'Google Gemini AI' : 'Offline Heuristics';
    document.getElementById('detail-candidate-job-target').textContent = item.jobDescription.title;

    const score = Math.round(item.matchScore);
    document.getElementById('detail-candidate-score-text').textContent = `${score}%`;
    const radial = document.getElementById('detail-candidate-score-radial');
    radial.style.setProperty('--radial-percent', `${score}%`);
    const color = score >= 75 ? 'var(--accent-emerald)' : score >= 50 ? 'var(--accent-orange)' : 'var(--accent-red)';
    radial.style.background = `conic-gradient(${color} ${score}%, rgba(255,255,255,0.05) 0)`;

    const statusBadge = document.getElementById('detail-candidate-badge-status');
    statusBadge.textContent = item.matchStatus;
    statusBadge.className = 'badge ' + (item.matchStatus === 'Shortlisted' ? 'badge-success' : item.matchStatus === 'Rejected' ? 'badge-danger' : 'badge-warning');
    document.getElementById('detail-status-select').value = item.matchStatus;

    document.getElementById('detail-candidate-summary').textContent = item.aiSummary || 'No evaluation summary available.';

    const skillsContainer = document.getElementById('detail-candidate-skills-tags');
    skillsContainer.innerHTML = '';
    if (item.candidate.extractedSkills) {
        item.candidate.extractedSkills.split(',').map(s => s.trim()).filter(Boolean).forEach(tag => {
            const span = document.createElement('span');
            span.className = 'skill-tag'; span.textContent = tag;
            skillsContainer.appendChild(span);
        });
    } else { skillsContainer.innerHTML = '<span class="text-muted">No skills indexed.</span>'; }

    const strengthsUl = document.getElementById('detail-candidate-strengths-ul');
    strengthsUl.innerHTML = '';
    (item.aiStrengths ? item.aiStrengths.split('\n').filter(Boolean) : ['Meets basic criteria.']).forEach(s => {
        const li = document.createElement('li'); li.textContent = s.replace(/^[-*\d.\s]+/, ''); strengthsUl.appendChild(li);
    });

    const weaknessesUl = document.getElementById('detail-candidate-weaknesses-ul');
    weaknessesUl.innerHTML = '';
    (item.aiWeaknesses ? item.aiWeaknesses.split('\n').filter(Boolean) : ['No significant gaps found.']).forEach(w => {
        const li = document.createElement('li'); li.textContent = w.replace(/^[-*\d.\s]+/, ''); weaknessesUl.appendChild(li);
    });

    const questionsContainer = document.getElementById('detail-candidate-questions-container');
    questionsContainer.innerHTML = '';
    (item.suggestedQuestions ? item.suggestedQuestions.split('\n').filter(Boolean) : ['Explain your technical background.']).forEach(q => {
        const p = document.createElement('p'); p.textContent = q; questionsContainer.appendChild(p);
    });

    document.getElementById('detail-status-select').onchange = async (e) => {
        await updateCandidateStatus(item.id, e.target.value);
    };

    // Career DNA radar
    renderDetailDnaRadar(item);

    modal.classList.remove('hidden');
}

function renderDetailDnaRadar(item) {
    const dnaJson = item.careerDnaProfile;
    if (!dnaJson) {
        document.getElementById('detail-dna-block').style.opacity = '0.4';
        document.getElementById('detail-dna-dominant').textContent = 'Not computed';
        document.getElementById('detail-dna-secondary').textContent = '—';
        return;
    }
    document.getElementById('detail-dna-block').style.opacity = '1';

    let dna;
    try { dna = typeof dnaJson === 'string' ? JSON.parse(dnaJson) : dnaJson; } catch { return; }

    document.getElementById('detail-dna-dominant').textContent = dna.dominantArchetype || '—';
    document.getElementById('detail-dna-secondary').textContent = dna.secondaryArchetype || '—';
    document.getElementById('detail-dna-desc').textContent = dna.archetypeDescription || '—';

    const strength = dna.dnaStrength || 0;
    document.getElementById('detail-dna-strength-fill').style.width = strength + '%';
    document.getElementById('detail-dna-strength-pct').textContent = strength + '%';

    const scores = dna.archetypeScores || {};
    const labels = Object.keys(scores);
    const values = Object.values(scores);

    const canvas = document.getElementById('detail-dna-radar-chart');
    if (state.charts.dnaRadar) { state.charts.dnaRadar.destroy(); state.charts.dnaRadar = null; }

    state.charts.dnaRadar = new Chart(canvas, {
        type: 'radar',
        data: {
            labels,
            datasets: [{
                label: item.candidate.name,
                data: values,
                backgroundColor: 'rgba(20,184,166,0.15)',
                borderColor: 'rgba(20,184,166,0.8)',
                pointBackgroundColor: 'rgba(20,184,166,1)',
                pointRadius: 3,
                borderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            scales: {
                r: {
                    min: 0, max: 100, ticks: { display: false },
                    grid: { color: 'rgba(255,255,255,0.07)' },
                    angleLines: { color: 'rgba(255,255,255,0.07)' },
                    pointLabels: { color: '#94a3b8', font: { size: 9, family: 'Inter' } }
                }
            },
            plugins: { legend: { display: false } }
        }
    });
}

async function updateCandidateStatus(resultId, newStatus) {
    try {
        const r = await fetch(`${API_BASE}/api/screen/results/${resultId}/status?status=${encodeURIComponent(newStatus)}`, { method: 'PUT' });
        if (r.ok) {
            const updated = await r.json();
            const idx = state.screenings.findIndex(s => s.id == resultId);
            if (idx !== -1) state.screenings[idx] = updated;
            const badge = document.getElementById('detail-candidate-badge-status');
            badge.textContent = updated.matchStatus;
            badge.className = 'badge ' + (updated.matchStatus === 'Shortlisted' ? 'badge-success' : updated.matchStatus === 'Rejected' ? 'badge-danger' : 'badge-warning');
            renderScreenings(); updateAnalytics();
            toast(`Status updated to ${updated.matchStatus}`, 'success');
        } else { toast('Failed to update status.', 'error'); }
    } catch (err) { console.error(err); }
}

/* -------------------------------------------------------
   Compare Candidates
------------------------------------------------------- */
function refreshCompareList() {
    const container = document.getElementById('compare-checkbox-list');
    container.innerHTML = '';
    state.compareSelectedIds.clear();
    document.getElementById('btn-run-compare').disabled = true;
    document.getElementById('compare-results-container').classList.add('hidden');

    if (state.screenings.length === 0) {
        container.innerHTML = '<p class="text-muted" style="font-size:12px;">No screened results yet.</p>';
        return;
    }

    const sorted = [...state.screenings].sort((a,b) => new Date(b.screenedAt) - new Date(a.screenedAt));
    sorted.forEach(item => {
        const label = document.createElement('label');
        label.className = 'compare-checkbox-item';
        label.innerHTML = `<input type="checkbox" value="${item.id}">
            <span>${item.candidate.name}</span>
            <span class="text-muted" style="font-size:10px;">&nbsp;${Math.round(item.matchScore)}%</span>`;
        label.querySelector('input').addEventListener('change', (e) => {
            if (e.target.checked) {
                if (state.compareSelectedIds.size >= 5) { e.target.checked = false; toast('Maximum 5 candidates can be compared.', 'warning'); return; }
                state.compareSelectedIds.add(item.id);
                label.classList.add('selected');
            } else {
                state.compareSelectedIds.delete(item.id);
                label.classList.remove('selected');
            }
            document.getElementById('btn-run-compare').disabled = state.compareSelectedIds.size < 2;
        });
        container.appendChild(label);
    });
}

async function runCompare() {
    if (state.compareSelectedIds.size < 2) return;
    const ids = Array.from(state.compareSelectedIds).join(',');
    const container = document.getElementById('compare-results-container');
    container.innerHTML = '<div class="text-muted" style="padding:20px;text-align:center;">Loading comparison...</div>';
    container.classList.remove('hidden');

    try {
        const r = await fetch(`${API_BASE}/api/screen/compare?ids=${ids}`);
        if (!r.ok) throw new Error(await r.text());
        const candidates = await r.json();
        renderCompareTable(candidates, container);
    } catch (err) {
        container.innerHTML = `<div class="error-msg">Failed to compare: ${err.message}</div>`;
    }
}

function renderCompareTable(candidates, container) {
    const bestScore = Math.max(...candidates.map(c => c.matchScore));

    const headers = candidates.map(c =>
        `<th class="compare-header-candidate">
            <div>${c.candidateName}</div>
            <div style="font-size:10px;color:var(--text-secondary);font-weight:400;margin-top:4px;">${c.candidateEmail}</div>
        </th>`
    ).join('');

    const rows = [
        { label: 'Match Score', key: 'matchScore', render: (v, c) => {
            const cls = v >= 75 ? 'score-high' : v < 50 ? 'score-low' : 'score-mid';
            const best = v === bestScore ? 'compare-best' : '';
            return `<td class="compare-score-cell ${cls} ${best}">${Math.round(v)}%</td>`;
        }},
        { label: 'Status', key: 'matchStatus', render: (v) => {
            const cls = v === 'Shortlisted' ? 'badge-success' : v === 'Rejected' ? 'badge-danger' : 'badge-warning';
            return `<td><span class="badge ${cls}">${v}</span></td>`;
        }},
        { label: 'Experience', key: 'experienceYears', render: (v) => `<td>${v || 0} yrs</td>` },
        { label: 'Education', key: 'education', render: (v) => `<td>${v || 'N/A'}</td>` },
        { label: 'Engine', key: 'screeningMode', render: (v) => `<td>${v === 'GEMINI_AI' ? '🤖 Gemini AI' : '⚙️ Offline'}</td>` },
        { label: 'DNA Archetype', key: 'careerDnaProfile', render: (v) => {
            if (!v) return '<td class="text-muted">—</td>';
            try { const d = typeof v === 'string' ? JSON.parse(v) : v; return `<td><span class="dna-archetype-pill">${d.dominantArchetype || '—'}</span></td>`; } catch { return '<td>—</td>'; }
        }},
        { label: 'Screened', key: 'screenedAt', render: (v) => `<td class="text-muted">${formatDate(v)}</td>` }
    ];

    const tableRows = rows.map(row => {
        const cells = candidates.map(c => row.render(c[row.key], c)).join('');
        return `<tr><td class="compare-row-label">${row.label}</td>${cells}</tr>`;
    }).join('');

    container.innerHTML = `
        <div class="glass-panel">
            <h4 style="margin-bottom:16px;">Side-by-Side Comparison (${candidates.length} candidates)</h4>
            <div class="compare-results-container">
                <table class="compare-table">
                    <thead><tr><th></th>${headers}</tr></thead>
                    <tbody>${tableRows}</tbody>
                </table>
            </div>
        </div>`;
}

/* -------------------------------------------------------
   Career DNA Section
------------------------------------------------------- */
async function loadDnaSection() {
    await loadScreenings();
    renderDnaCards();
    loadDnaTrendsChart();
    loadDnaTopArchetype();
}

function renderDnaCards() {
    const grid = document.getElementById('dna-cards-grid');
    if (!grid) return;
    grid.innerHTML = '';

    const withDna = state.screenings.filter(s => s.careerDnaProfile);
    if (withDna.length === 0) {
        grid.innerHTML = `<div class="empty-state-card"><i class="fa-solid fa-dna"></i><p>No DNA profiles yet. Screen candidates to generate fingerprints.</p></div>`;
        return;
    }

    withDna.forEach(item => {
        let dna;
        try { dna = typeof item.careerDnaProfile === 'string' ? JSON.parse(item.careerDnaProfile) : item.careerDnaProfile; } catch { return; }

        const scores = dna.archetypeScores || {};
        const sortedScores = Object.entries(scores).sort((a,b) => b[1] - a[1]);
        const barsHtml = sortedScores.map(([label, val]) =>
            `<div class="dna-bar-row">
                <span class="dna-bar-label">${label}</span>
                <div class="dna-bar-track"><div class="dna-bar-fill" style="width:${val}%"></div></div>
                <span class="dna-bar-pct">${val}%</span>
            </div>`
        ).join('');

        const card = document.createElement('div');
        card.className = 'dna-card';
        card.innerHTML = `
            <div class="dna-card-header">
                <div>
                    <div class="dna-card-name">${item.candidate.name}</div>
                    <div style="font-size:11px;color:var(--text-muted);margin-top:2px;">${item.jobDescription.title}</div>
                </div>
                <span class="dna-archetype-pill">${dna.dominantArchetype || '—'}</span>
            </div>
            <div class="dna-card-bars">${barsHtml}</div>
            <div class="dna-card-footer">${dna.archetypeDescription || ''}</div>`;
        card.style.cursor = 'pointer';
        card.addEventListener('click', () => showCandidateReport(item));
        grid.appendChild(card);
    });
}

async function loadDnaTrendsChart() {
    const canvas = document.getElementById('dna-trends-chart');
    if (!canvas) return;
    try {
        const r = await fetch(`${API_BASE}/api/dna/trends`);
        if (!r.ok) return;
        const trends = await r.json();
        if (state.charts.dnaTrends) { state.charts.dnaTrends.destroy(); state.charts.dnaTrends = null; }

        const labels = Object.keys(trends);
        const values = Object.values(trends);
        const colors = ['#14b8a6','#a855f7','#3b82f6','#f97316','#10b981','#ef4444','#f59e0b','#6366f1'];

        state.charts.dnaTrends = new Chart(canvas, {
            type: 'bar',
            data: {
                labels,
                datasets: [{ label: 'Candidates', data: values, backgroundColor: colors.slice(0, labels.length), borderRadius: 4, borderSkipped: false }]
            },
            options: {
                responsive: true, maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 10 } } },
                    y: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8', stepSize: 1, precision: 0 } }
                }
            }
        });
    } catch (err) { console.error('DNA trends chart error:', err); }
}

async function loadDnaTopArchetype() {
    const el = document.getElementById('stat-top-archetype');
    if (!el) return;
    try {
        const r = await fetch(`${API_BASE}/api/dna/trends`);
        if (!r.ok) return;
        const trends = await r.json();
        const top = Object.keys(trends)[0];
        el.textContent = top || '—';
    } catch { el.textContent = '—'; }
}

/* -------------------------------------------------------
   Analytics Charts
------------------------------------------------------- */
async function updateAnalytics() {
    try {
        const [sumResp, skillResp] = await Promise.all([
            fetch(`${API_BASE}/api/analytics/summary`),
            fetch(`${API_BASE}/api/analytics/skills`)
        ]);
        if (!sumResp.ok || !skillResp.ok) throw new Error('Analytics fetch failed');
        const summary = await sumResp.json();
        const skillsData = await skillResp.json();
        renderAnalyticsCharts(summary, skillsData);
    } catch (err) { console.error('Analytics error:', err); }
}

function renderAnalyticsCharts(summary, skillsData) {
    const distCanvas = document.getElementById('selection-distribution-chart');
    const skillsCanvas = document.getElementById('skills-frequency-chart');
    if (!distCanvas || !skillsCanvas) return;

    if (state.charts.distribution) { state.charts.distribution.destroy(); }
    if (state.charts.skills) { state.charts.skills.destroy(); }

    const distData = [summary.shortlisted || 0, summary.underReview || 0, summary.rejected || 0];
    const isEmpty = distData.reduce((a,b) => a+b, 0) === 0;

    state.charts.distribution = new Chart(distCanvas, {
        type: 'doughnut',
        data: {
            labels: isEmpty ? ['No Data'] : ['Shortlisted', 'Under Review', 'Rejected'],
            datasets: [{ data: isEmpty ? [1] : distData, backgroundColor: isEmpty ? ['#1e293b'] : ['#10b981','#f97316','#ef4444'], borderWidth: 1, borderColor: '#0f1420' }]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { position: 'bottom', labels: { color: '#94a3b8', font: { family: 'Inter', size: 11 } } } }, cutout: '65%' }
    });

    const sortedSkills = Object.entries(skillsData).sort((a,b) => b[1]-a[1]).slice(0, 8);
    const skillLabels = sortedSkills.map(e => e[0]);
    const skillCounts = sortedSkills.map(e => e[1]);
    const isSkillsEmpty = skillLabels.length === 0;

    state.charts.skills = new Chart(skillsCanvas, {
        type: 'bar',
        data: {
            labels: isSkillsEmpty ? ['No Skills Indexed'] : skillLabels,
            datasets: [{ label: 'Occurrences', data: isSkillsEmpty ? [0] : skillCounts, backgroundColor: 'rgba(168,85,247,0.65)', borderColor: '#a855f7', borderWidth: 1, borderRadius: 4 }]
        },
        options: {
            indexAxis: 'y', responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: {
                x: { grid: { color: 'rgba(255,255,255,0.05)' }, ticks: { color: '#94a3b8', stepSize: 1, precision: 0 } },
                y: { grid: { display: false }, ticks: { color: '#94a3b8', font: { family: 'Inter', size: 11 } } }
            }
        }
    });
}

/* -------------------------------------------------------
   Settings & Local Storage
------------------------------------------------------- */
function loadSettings() {
    const savedKey = localStorage.getItem('kit_ai_gemini_key') || localStorage.getItem('talentpulse_gemini_key');
    if (savedKey) {
        document.getElementById('screen-api-key').value = savedKey;
        document.getElementById('settings-api-key').value = savedKey;
        const batchKey = document.getElementById('batch-api-key');
        if (batchKey) batchKey.value = savedKey;
    }
    const savedDb = localStorage.getItem('kit_ai_db_mode') || 'h2';
    state.dbProfile = savedDb;
    toggleDbBadge(savedDb);
}

function toggleDbBadge(db) {
    const badge = document.getElementById('current-db-status');
    const badgeSpan = badge.querySelector('span');
    document.querySelectorAll('.db-toggle-item').forEach(i => i.classList.remove('active'));
    if (db === 'mysql') {
        document.getElementById('db-toggle-mysql').classList.add('active');
        badgeSpan.textContent = 'MySQL Relational Active';
    } else {
        document.getElementById('db-toggle-h2').classList.add('active');
        badgeSpan.textContent = 'SQLite/H2 Active';
    }
}

/* -------------------------------------------------------
   Event Listeners (all wired once on DOMContentLoaded)
------------------------------------------------------- */
function registerEventListeners() {
    // Single engine toggles
    document.getElementById('engine-offline').addEventListener('click', () => {
        state.activeEngine = 'offline';
        document.getElementById('engine-offline').classList.add('active');
        document.getElementById('engine-gemini').classList.remove('active');
        document.getElementById('gemini-key-input-container').classList.add('hidden');
    });
    document.getElementById('engine-gemini').addEventListener('click', () => {
        state.activeEngine = 'gemini';
        document.getElementById('engine-gemini').classList.add('active');
        document.getElementById('engine-offline').classList.remove('active');
        document.getElementById('gemini-key-input-container').classList.remove('hidden');
    });

    // Batch engine toggles
    const batchOffline = document.getElementById('batch-engine-offline');
    const batchGemini = document.getElementById('batch-engine-gemini');
    const batchKeyContainer = document.getElementById('batch-gemini-key-container');
    if (batchOffline) batchOffline.addEventListener('click', () => {
        state.batchEngine = 'offline';
        batchOffline.classList.add('active'); batchGemini.classList.remove('active');
        batchKeyContainer.classList.add('hidden');
    });
    if (batchGemini) batchGemini.addEventListener('click', () => {
        state.batchEngine = 'gemini';
        batchGemini.classList.add('active'); batchOffline.classList.remove('active');
        batchKeyContainer.classList.remove('hidden');
    });

    // API key visibility toggles
    [['btn-toggle-key-visibility','screen-api-key'], ['btn-toggle-settings-key-visibility','settings-api-key'], ['btn-toggle-batch-key','batch-api-key']].forEach(([btnId, inputId]) => {
        const btn = document.getElementById(btnId);
        const inp = document.getElementById(inputId);
        if (btn && inp) btn.addEventListener('click', () => {
            inp.type = inp.type === 'password' ? 'text' : 'password';
            btn.querySelector('i').className = inp.type === 'password' ? 'fa-solid fa-eye' : 'fa-solid fa-eye-slash';
        });
    });

    // Job modal
    document.getElementById('btn-open-create-job-modal').onclick = () => document.getElementById('create-job-modal').classList.remove('hidden');
    ['btn-close-job-modal','btn-cancel-job-modal'].forEach(id => {
        document.getElementById(id).onclick = () => {
            document.getElementById('create-job-modal').classList.add('hidden');
            document.getElementById('create-job-form').reset();
        };
    });

    // Candidate modal close
    ['btn-close-candidate-modal','btn-close-candidate-modal-footer'].forEach(id => {
        document.getElementById(id).onclick = () => document.getElementById('candidate-detail-modal').classList.add('hidden');
    });

    // Settings save
    document.getElementById('btn-save-settings').onclick = () => {
        const key = document.getElementById('settings-api-key').value.trim();
        localStorage.setItem('kit_ai_gemini_key', key);
        localStorage.setItem('talentpulse_gemini_key', key);
        document.getElementById('screen-api-key').value = key;
        const batchKey = document.getElementById('batch-api-key');
        if (batchKey) batchKey.value = key;
        localStorage.setItem('kit_ai_db_mode', state.dbProfile);
        toggleDbBadge(state.dbProfile);
        toast('Settings saved successfully!', 'success');
    };

    document.getElementById('db-toggle-h2').onclick = () => { state.dbProfile = 'h2'; toggleDbBadge('h2'); };
    document.getElementById('db-toggle-mysql').onclick = () => { state.dbProfile = 'mysql'; toggleDbBadge('mysql'); };

    // Job create form
    document.getElementById('create-job-form').onsubmit = async (e) => {
        e.preventDefault();
        const payload = {
            title: document.getElementById('job-title').value,
            department: document.getElementById('job-dept').value,
            requiredSkills: document.getElementById('job-skills').value,
            minExperience: parseInt(document.getElementById('job-experience').value),
            minEducation: document.getElementById('job-education').value,
            description: document.getElementById('job-desc').value
        };
        try {
            const r = await fetch(`${API_BASE}/api/jobs`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
            if (r.ok) { document.getElementById('create-job-modal').classList.add('hidden'); document.getElementById('create-job-form').reset(); loadJobs(); toast('Job requisition created!', 'success'); }
            else toast('Failed to create job requisition.', 'error');
        } catch (err) { console.error(err); toast('Network error. Please try again.', 'error'); }
    };

    // Screening form
    document.getElementById('screening-form').onsubmit = runScreening;
    document.getElementById('screen-job-select').onchange = validateScreeningForm;

    // Batch form
    const batchForm = document.getElementById('batch-form');
    if (batchForm) batchForm.onsubmit = runBatchScreening;
    const batchJobSelect = document.getElementById('batch-job-select');
    if (batchJobSelect) batchJobSelect.onchange = validateBatchForm;

    // Compare button
    const compareBtn = document.getElementById('btn-run-compare');
    if (compareBtn) compareBtn.addEventListener('click', runCompare);

    // Refresh & search
    document.getElementById('btn-refresh-screenings').onclick = loadScreenings;
    document.getElementById('global-search').oninput = renderScreenings;

    // Auth
    const loginForm = document.getElementById('login-form');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const username = document.getElementById('login-username').value.trim();
            const password = document.getElementById('login-password').value;
            const errorMsg = document.getElementById('login-error-message');
            const btnLogin = document.getElementById('btn-login');
            btnLogin.disabled = true;
            errorMsg.classList.add('hidden');
            try {
                const r = await fetch(`${API_BASE}/api/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ username, password }) });
                if (r.ok) {
                    const data = await r.json();
                    sessionStorage.setItem('kit_ai_logged_in', 'true');
                    sessionStorage.setItem('kit_ai_username', data.username || username);
                    document.getElementById('login-screen').classList.add('hidden');
                    document.body.style.overflow = '';
                    document.getElementById('profile-username').textContent = data.username || username;
                    document.getElementById('login-username').value = '';
                    document.getElementById('login-password').value = '';
                } else {
                    const data = await r.json().catch(() => ({}));
                    errorMsg.textContent = data.message || 'Invalid credentials.';
                    errorMsg.classList.remove('hidden');
                }
            } catch { errorMsg.textContent = 'Server error. Try again.'; errorMsg.classList.remove('hidden'); }
            finally { btnLogin.disabled = false; }
        });
    }

    document.getElementById('btn-logout').addEventListener('click', (e) => {
        e.preventDefault();
        sessionStorage.removeItem('kit_ai_logged_in');
        sessionStorage.removeItem('kit_ai_username');
        window.location.reload();
    });
}

/* -------------------------------------------------------
   Helpers
------------------------------------------------------- */
function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    return new Date(dateStr).toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
}

/* -------------------------------------------------------
   Toast Notification System
   Usage: toast('Message here', 'success' | 'error' | 'info' | 'warning')
------------------------------------------------------- */
function toast(message, type = 'info', duration = 3500) {
    const container = document.getElementById('toast-container');
    if (!container) return;

    const icons = { success: 'fa-circle-check', error: 'fa-circle-xmark', info: 'fa-circle-info', warning: 'fa-triangle-exclamation' };
    const icon = icons[type] || icons.info;

    const el = document.createElement('div');
    el.className = `toast toast-${type}`;
    el.innerHTML = `<i class="fa-solid ${icon}"></i><span>${message}</span><button class="toast-close" onclick="this.parentElement.remove()"><i class="fa-solid fa-xmark"></i></button>`;
    container.appendChild(el);

    // Auto-dismiss
    setTimeout(() => {
        el.classList.add('toast-hiding');
        setTimeout(() => el.remove(), 320);
    }, duration);
}

/* -------------------------------------------------------
   Recruiter Notes — save/load inside candidate modal
------------------------------------------------------- */
function loadRecruiterNotes(item) {
    const textarea = document.getElementById('detail-recruiter-notes');
    const saveBtn  = document.getElementById('btn-save-notes');
    const indicator = document.getElementById('notes-saved-indicator');
    if (!textarea) return;

    textarea.value = item.recruiterNotes || '';

    saveBtn.onclick = async () => {
        const notes = textarea.value;
        try {
            const r = await fetch(`${API_BASE}/api/screen/results/${item.id}/notes`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ notes })
            });
            if (r.ok) {
                const updated = await r.json();
                const idx = state.screenings.findIndex(s => s.id == item.id);
                if (idx !== -1) state.screenings[idx].recruiterNotes = updated.recruiterNotes;
                indicator.classList.add('visible');
                setTimeout(() => indicator.classList.remove('visible'), 2000);
                toast('Notes saved.', 'success');
            } else { toast('Failed to save notes.', 'error'); }
        } catch (err) { console.error(err); toast('Network error saving notes.', 'error'); }
    };
}

/* -------------------------------------------------------
   Re-Screen stored candidate against a different job
------------------------------------------------------- */
function initRescreenUI(item) {
    // Populate job dropdown in modal footer
    const select = document.getElementById('rescreen-job-select');
    if (!select) return;
    select.innerHTML = '<option value="" disabled selected>Re-screen against job...</option>';
    state.jobs.forEach(job => {
        const o = document.createElement('option');
        o.value = job.id;
        o.textContent = `${job.title} (${job.department || 'N/A'})`;
        select.appendChild(o);
    });

    const rescreenBtn = document.getElementById('btn-rescreen-candidate');
    if (!rescreenBtn) return;

    rescreenBtn.onclick = async () => {
        const jobId = select.value;
        if (!jobId) { toast('Please select a job to re-screen against.', 'warning'); return; }

        rescreenBtn.disabled = true;
        rescreenBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Screening...';

        const apiKey = localStorage.getItem('kit_ai_gemini_key') || '';
        const headers = {};
        if (apiKey) headers['X-Gemini-Key'] = apiKey;

        try {
            const r = await fetch(`${API_BASE}/api/screen/rescreen?candidateId=${item.candidate.id}&jobId=${jobId}`, {
                method: 'POST', headers
            });
            if (!r.ok) throw new Error(await r.text());
            const result = await r.json();

            // Close current modal, open new result
            document.getElementById('candidate-detail-modal').classList.add('hidden');
            await loadScreenings();
            showCandidateReport(result);
            toast(`Re-screened ${result.candidate.name} — ${Math.round(result.matchScore)}% (${result.matchStatus})`,
                  result.matchStatus === 'Shortlisted' ? 'success' : result.matchStatus === 'Rejected' ? 'error' : 'info');
            updateAnalytics(); loadDnaTopArchetype();
        } catch (err) {
            toast(`Re-screen failed: ${err.message}`, 'error');
        } finally {
            rescreenBtn.disabled = false;
            rescreenBtn.innerHTML = '<i class="fa-solid fa-rotate"></i> Re-Screen';
        }
    };
}

/* -------------------------------------------------------
   Wire notes + rescreen into showCandidateReport
------------------------------------------------------- */
// Patch showCandidateReport to also call the new helpers
const _origShowCandidateReport = showCandidateReport;
window.showCandidateReport = function(item) {
    _origShowCandidateReport(item);
    loadRecruiterNotes(item);
    initRescreenUI(item);
};

/* -------------------------------------------------------
   Preferred Skills — wire into job creation form
------------------------------------------------------- */
// Patch job create form to include preferredSkills
const _origJobFormSubmit = document.getElementById('create-job-form');
if (_origJobFormSubmit) {
    _origJobFormSubmit.onsubmit = async (e) => {
        e.preventDefault();
        const payload = {
            title: document.getElementById('job-title').value,
            department: document.getElementById('job-dept').value,
            requiredSkills: document.getElementById('job-skills').value,
            preferredSkills: (document.getElementById('job-preferred-skills') || {}).value || '',
            minExperience: parseInt(document.getElementById('job-experience').value) || 0,
            minEducation: document.getElementById('job-education').value,
            description: document.getElementById('job-desc').value
        };
        try {
            const r = await fetch(`${API_BASE}/api/jobs`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
            if (r.ok) {
                document.getElementById('create-job-modal').classList.add('hidden');
                document.getElementById('create-job-form').reset();
                loadJobs();
                toast('Job requisition created!', 'success');
            } else { toast('Failed to create job requisition.', 'error'); }
        } catch (err) { console.error(err); toast('Network error. Please try again.', 'error'); }
    };
}

/* -------------------------------------------------------
   Keyboard Shortcuts
   - Ctrl+Enter  → submit the active screening form
   - Escape      → close any open modal
------------------------------------------------------- */
function initKeyboardShortcuts() {
    document.addEventListener('keydown', (e) => {
        // Ctrl+Enter → run screening if on screen section and form is ready
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
            const screenSection = document.getElementById('screen-section');
            if (screenSection && screenSection.classList.contains('active')) {
                const runBtn = document.getElementById('btn-run-screening');
                if (runBtn && !runBtn.disabled) {
                    e.preventDefault();
                    runBtn.click();
                }
            }
        }

        // Escape → close topmost visible modal
        if (e.key === 'Escape') {
            const modals = [
                document.getElementById('candidate-detail-modal'),
                document.getElementById('create-job-modal')
            ];
            for (const modal of modals) {
                if (modal && !modal.classList.contains('hidden')) {
                    modal.classList.add('hidden');
                    break;
                }
            }
        }
    });
}

/* -------------------------------------------------------
   AI RESUME COACH — Novelty Feature
   Multi-turn Gemini AI conversation grounded in a
   specific candidate's resume + job description.
   No other ATS ships this feature.
------------------------------------------------------- */

let coachState = {
    selectedResultId: null,
    chatHistory: [],        // [{role:'user'|'model', text:'...'}]
    isTyping: false
};

function initCoachSection() {
    // Populate the result dropdown
    const select = document.getElementById('coach-result-select');
    if (!select) return;

    // If screenings not yet loaded, fetch them first then retry
    if (state.screenings.length === 0) {
        fetch(`${API_BASE}/api/screen/results`)
            .then(r => r.json())
            .then(data => {
                state.screenings = data;
                renderScreenings();
                initCoachSection(); // retry now that data is loaded
            })
            .catch(err => console.error('Coach: failed to load screenings', err));
        return;
    }

    select.innerHTML = '<option value="" disabled selected>Select a screening result...</option>';

    const sorted = [...state.screenings].sort((a,b) => new Date(b.screenedAt) - new Date(a.screenedAt));
    sorted.forEach(item => {
        const o = document.createElement('option');
        o.value = item.id;
        o.textContent = `${item.candidate.name} → ${item.jobDescription.title} (${Math.round(item.matchScore)}%)`;
        select.appendChild(o);
    });

    // Restore selection if still valid
    if (coachState.selectedResultId && sorted.some(s => s.id == coachState.selectedResultId)) {
        select.value = coachState.selectedResultId;
        updateCoachCandidateCard(coachState.selectedResultId);
        document.getElementById('coach-input').disabled = false;
        document.getElementById('btn-coach-send').disabled = false;
    }
}

function updateCoachCandidateCard(resultId) {
    const item = state.screenings.find(s => s.id == resultId);
    if (!item) return;

    const card = document.getElementById('coach-candidate-card');
    const initials = item.candidate.name.split(' ').map(n => n[0]).join('').substring(0,2).toUpperCase();
    document.getElementById('coach-card-avatar').textContent = initials;
    document.getElementById('coach-card-name').textContent = item.candidate.name;
    document.getElementById('coach-card-job').textContent = item.jobDescription.title;

    const score = Math.round(item.matchScore);
    const scoreEl = document.getElementById('coach-card-score');
    scoreEl.textContent = `${score}% Match · ${item.matchStatus}`;
    scoreEl.style.color = score >= 75 ? 'var(--accent-emerald)' : score >= 50 ? 'var(--accent-orange)' : 'var(--accent-red)';

    card.classList.remove('hidden');

    // Update chat title
    const titleEl = document.getElementById('coach-chat-title-text');
    if (titleEl) titleEl.textContent = `AI Coach — ${item.candidate.name}`;
}

function registerCoachListeners() {
    const select = document.getElementById('coach-result-select');
    const input  = document.getElementById('coach-input');
    const sendBtn = document.getElementById('btn-coach-send');
    const clearBtn = document.getElementById('btn-clear-coach-chat');

    if (!select) return;

    // Select a candidate
    select.addEventListener('change', () => {
        coachState.selectedResultId = parseInt(select.value);
        coachState.chatHistory = [];
        updateCoachCandidateCard(coachState.selectedResultId);
        input.disabled = false;
        sendBtn.disabled = false;
        input.focus();

        // Show greeting specific to this candidate
        const item = state.screenings.find(s => s.id == coachState.selectedResultId);
        if (item) {
            clearCoachMessages();
            appendCoachBotMessage(
                `Great! I'm now your AI coach for **${item.candidate.name}** applying to **${item.jobDescription.title}**.\n\n` +
                `They scored **${Math.round(item.matchScore)}%** — status: **${item.matchStatus}**.\n\n` +
                `I have full context of their resume, skills, and job requirements. Ask me anything! 💬\n\n` +
                `Or use a quick prompt on the left to get started instantly.`
            );
        }
    });

    // Send on Enter (Shift+Enter for newline)
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            if (!sendBtn.disabled) sendCoachMessage();
        }
    });

    sendBtn.addEventListener('click', sendCoachMessage);

    clearBtn.addEventListener('click', () => {
        coachState.chatHistory = [];
        clearCoachMessages();
        appendCoachBotMessage('Chat cleared! Ask me anything about this candidate. 🗑️');
        toast('Coach chat cleared.', 'info');
    });

    // Quick prompt buttons
    document.getElementById('coach-quick-prompts').addEventListener('click', (e) => {
        const btn = e.target.closest('.coach-prompt-btn');
        if (!btn) return;
        if (!coachState.selectedResultId) {
            toast('Please select a candidate first.', 'warning');
            return;
        }
        const prompt = btn.getAttribute('data-prompt');
        input.value = prompt;
        input.focus();
        sendCoachMessage();
    });
}

async function sendCoachMessage() {
    const input   = document.getElementById('coach-input');
    const sendBtn = document.getElementById('btn-coach-send');
    const message = input.value.trim();

    if (!message || !coachState.selectedResultId || coachState.isTyping) return;

    // Append user message to UI
    appendCoachUserMessage(message);
    coachState.chatHistory.push({ role: 'user', text: message });
    input.value = '';
    input.style.height = 'auto';

    // Show typing indicator
    coachState.isTyping = true;
    sendBtn.disabled = true;
    const typingId = showCoachTyping();

    const apiKey = localStorage.getItem('kit_ai_gemini_key') || '';
    const headers = { 'Content-Type': 'application/json' };
    if (apiKey) headers['X-Gemini-Key'] = apiKey;

    try {
        const response = await fetch(`${API_BASE}/api/coach/chat`, {
            method: 'POST',
            headers,
            body: JSON.stringify({
                resultId: coachState.selectedResultId,
                message: message,
                history: coachState.chatHistory.slice(0, -1) // exclude current message (already sent)
            })
        });

        removeCoachTyping(typingId);

        if (!response.ok) {
            const err = await response.text();
            appendCoachBotMessage(`❌ Error: ${err}`);
            return;
        }

        const data = await response.json();
        const reply = data.reply || 'No response received.';

        // Add model reply to history
        coachState.chatHistory.push({ role: 'model', text: reply });

        // Render with basic markdown
        appendCoachBotMessage(reply);

    } catch (err) {
        removeCoachTyping(typingId);
        appendCoachBotMessage(`❌ Network error: ${err.message}. Please check your connection.`);
    } finally {
        coachState.isTyping = false;
        sendBtn.disabled = false;
        input.focus();
    }
}

/* ── UI Helpers ── */

function clearCoachMessages() {
    const container = document.getElementById('coach-messages');
    container.innerHTML = '';
}

function appendCoachUserMessage(text) {
    const container = document.getElementById('coach-messages');
    const row = document.createElement('div');
    row.className = 'coach-msg-row user-row';
    row.innerHTML = `
        <div class="coach-user-avatar">You</div>
        <div class="coach-bubble coach-bubble-user">${escapeHtml(text)}</div>`;
    container.appendChild(row);
    scrollCoachToBottom();
}

function appendCoachBotMessage(text) {
    const container = document.getElementById('coach-messages');
    const row = document.createElement('div');
    row.className = 'coach-msg-row';
    row.innerHTML = `
        <div class="coach-bot-avatar"><i class="fa-solid fa-robot"></i></div>
        <div class="coach-bubble coach-bubble-bot">${renderCoachMarkdown(text)}</div>`;
    container.appendChild(row);
    scrollCoachToBottom();
}

function showCoachTyping() {
    const container = document.getElementById('coach-messages');
    const row = document.createElement('div');
    const id = 'typing-' + Date.now();
    row.id = id;
    row.className = 'coach-msg-row';
    row.innerHTML = `
        <div class="coach-bot-avatar"><i class="fa-solid fa-robot"></i></div>
        <div class="coach-typing">
            <span>AI is thinking</span>
            <div class="coach-typing-dots">
                <span></span><span></span><span></span>
            </div>
        </div>`;
    container.appendChild(row);
    scrollCoachToBottom();
    return id;
}

function removeCoachTyping(id) {
    const el = document.getElementById(id);
    if (el) el.remove();
}

function scrollCoachToBottom() {
    const container = document.getElementById('coach-messages');
    if (container) container.scrollTop = container.scrollHeight;
}

function escapeHtml(text) {
    return text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

/**
 * Very lightweight markdown renderer for chat bubbles.
 * Handles: **bold**, *italic*, `code`, bullet lists, numbered lists, line breaks.
 */
function renderCoachMarkdown(text) {
    let html = escapeHtml(text);
    // Bold **text**
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    // Italic *text*
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    // Inline code `code`
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    // Bullet list lines starting with - or •
    html = html.replace(/^[\-•]\s+(.+)$/gm, '<li>$1</li>');
    // Numbered list
    html = html.replace(/^\d+\.\s+(.+)$/gm, '<li>$1</li>');
    // Wrap consecutive <li> in <ul>
    html = html.replace(/(<li>.*<\/li>)/gs, (match) => '<ul>' + match + '</ul>');
    // Double newline = paragraph break
    html = html.replace(/\n\n/g, '</p><p>');
    // Single newline = <br>
    html = html.replace(/\n/g, '<br>');
    // Wrap in paragraph
    html = '<p>' + html + '</p>';
    return html;
}

// Register coach event listeners once on load
document.addEventListener('DOMContentLoaded', () => {
    registerCoachListeners();
});
