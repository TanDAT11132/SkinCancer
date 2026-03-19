const API_BASE_URL = window.APP_CONFIG?.API_BASE_URL || "http://localhost:8001";
const GOOGLE_CLIENT_ID = window.APP_CONFIG?.GOOGLE_CLIENT_ID || "";

class ApiHttpError extends Error {
  constructor(message, status, code, payload) {
    super(message);
    this.name = "ApiHttpError";
    this.status = status;
    this.code = code;
    this.payload = payload;
  }
}

const state = {
  token: localStorage.getItem("jwt") || "",
  me: null,
  latestPredictionId: "",
  guestMode: false,
  activePage: "user",
  adminDashboard: null,
  adminUsers: [],
  selectedAdminUserId: "",
  selectedAdminPredictionId: "",
  adminUserPredictions: []
};

const loginView = document.getElementById("loginView");
const profileView = document.getElementById("profileView");
const mainView = document.getElementById("mainView");
const meInfo = document.getElementById("meInfo");
const latestResult = document.getElementById("latestResult");
const historyList = document.getElementById("historyList");
const predictionIdInput = document.getElementById("predictionId");
const errorBanner = document.getElementById("errorBanner");
const googleBtnContainer = document.getElementById("googleBtnContainer");
const historyPanel = document.getElementById("historyPanel");
const feedbackPanel = document.getElementById("feedbackPanel");
const mainNav = document.getElementById("mainNav");
const userPage = document.getElementById("userPage");
const adminPage = document.getElementById("adminPage");
const userPageBtn = document.getElementById("userPageBtn");
const adminPageBtn = document.getElementById("adminPageBtn");
const refreshAdminBtn = document.getElementById("refreshAdminBtn");

const statTotalUsers = document.getElementById("statTotalUsers");
const statNewUsersMonth = document.getElementById("statNewUsersMonth");
const statTotalPredictions = document.getElementById("statTotalPredictions");
const statPredictionsMonth = document.getElementById("statPredictionsMonth");
const statTotalFeedbacks = document.getElementById("statTotalFeedbacks");
const statFeedbackAccuracy = document.getElementById("statFeedbackAccuracy");
const statRetrainReady = document.getElementById("statRetrainReady");
const statAnsweredFeedbacks = document.getElementById("statAnsweredFeedbacks");
const monthlyUsersChart = document.getElementById("monthlyUsersChart");
const genderBreakdown = document.getElementById("genderBreakdown");
const diagnosisBreakdown = document.getElementById("diagnosisBreakdown");
const feedbackBreakdown = document.getElementById("feedbackBreakdown");
const adminUsersList = document.getElementById("adminUsersList");
const adminPredictionList = document.getElementById("adminPredictionList");
const adminPredictionDetail = document.getElementById("adminPredictionDetail");
const adminSelectedUserLabel = document.getElementById("adminSelectedUserLabel");
const refreshAdminUsersBtn = document.getElementById("refreshAdminUsersBtn");
const adminStatDetails = document.getElementById("adminStatDetails");
const adminCorrectFilter = document.getElementById("adminCorrectFilter");
const adminRetrainFilter = document.getElementById("adminRetrainFilter");

let googleInitialized = false;

function setView(viewId) {
  [loginView, profileView, mainView].forEach(v => v.classList.add("hidden"));
  document.getElementById(viewId).classList.remove("hidden");
}

function showError(message) {
  errorBanner.textContent = message;
  errorBanner.classList.remove("hidden");
}

function clearError() {
  errorBanner.textContent = "";
  errorBanner.classList.add("hidden");
}

function formatError(err, fallbackPrefix) {
  if (err instanceof ApiHttpError) {
    const codePart = err.code ? `[${err.code}] ` : "";
    return `${fallbackPrefix}: ${codePart}${err.message}`;
  }
  return `${fallbackPrefix}: ${err.message || "Unknown error"}`;
}

function saveToken(token) {
  state.token = token;
  localStorage.setItem("jwt", token);
}

function clearToken() {
  state.token = "";
  localStorage.removeItem("jwt");
}

function enableGuestMode() {
  state.guestMode = true;
  state.me = null;
  state.latestPredictionId = "";
  state.adminDashboard = null;
  state.activePage = "user";
  state.adminUsers = [];
  state.selectedAdminUserId = "";
  state.selectedAdminPredictionId = "";
  state.adminUserPredictions = [];
  predictionIdInput.value = "";
  meInfo.textContent = "Guest mode | Upload and predict only (no DB save)";
  historyPanel.classList.add("hidden");
  feedbackPanel.classList.add("hidden");
  syncAdminVisibility();
  userPage.classList.remove("hidden");
  adminPage.classList.add("hidden");
  latestResult.textContent = "No result yet.";
  setView("mainView");
}

function renderDiseaseResult(item) {
  const disease = item?.predictedClass || "unknown";
  latestResult.textContent = disease;
}

function formatDateTime(value) {
  if (!value) return "N/A";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function formatFeedbackLabel(value) {
  if (value === true) return "Correct";
  if (value === false) return "Incorrect";
  return "Unknown";
}

function isAdmin() {
  return state.me?.role === "ADMIN";
}

function syncAdminVisibility() {
  const adminEnabled = isAdmin() && !state.guestMode;
  mainNav.classList.toggle("hidden", !adminEnabled);
  adminPageBtn.classList.toggle("hidden", !adminEnabled);
  if (!adminEnabled) {
    userPage.classList.remove("hidden");
    adminPage.classList.add("hidden");
  }
}

function setActivePage(page) {
  state.activePage = page;
  const showAdmin = page === "admin" && isAdmin() && !state.guestMode;
  userPage.classList.toggle("hidden", showAdmin);
  adminPage.classList.toggle("hidden", !showAdmin);
  userPageBtn.classList.toggle("active", !showAdmin);
  adminPageBtn.classList.toggle("active", showAdmin);
}

function renderBarChart(container, items) {
  if (!items?.length) {
    container.innerHTML = "<p class=\"emptyState\">No data yet.</p>";
    return;
  }

  const maxCount = Math.max(...items.map(item => item.count), 1);
  container.innerHTML = items.map(item => {
    const height = Math.max(12, Math.round((item.count / maxCount) * 180));
    const label = item.date ? item.date.slice(-2) : item.label;
    return `
      <div class="barItem">
        <span class="barValue">${item.count}</span>
        <div class="barTrack">
          <div class="barFill" style="height:${height}px"></div>
        </div>
        <span class="barLabel">${label}</span>
      </div>
    `;
  }).join("");
}

function renderMetricList(container, items, emptyMessage = "No data yet.") {
  if (!items?.length) {
    container.innerHTML = `<p class="emptyState">${emptyMessage}</p>`;
    return;
  }

  const maxCount = Math.max(...items.map(item => item.count), 1);
  container.innerHTML = items.map(item => `
    <div class="metricRow">
      <div>
        <strong>${item.label}</strong>
      </div>
      <div class="metricBarWrap">
        <div class="metricBar" style="width:${Math.max(8, Math.round((item.count / maxCount) * 100))}%"></div>
      </div>
      <span>${item.count}</span>
    </div>
  `).join("");
}

function renderFeedbackEffectiveness(effectiveness) {
  if (!effectiveness) {
    feedbackBreakdown.innerHTML = "<p class=\"emptyState\">No feedback data yet.</p>";
    return;
  }

  const items = [
    { label: "Correct", count: effectiveness.correctFeedbacks },
    { label: "Incorrect", count: effectiveness.incorrectFeedbacks },
    { label: "Unanswered", count: effectiveness.unansweredFeedbacks },
    { label: "Retrain Ready", count: effectiveness.retrainReadyFeedbacks }
  ];
  renderMetricList(feedbackBreakdown, items, "No feedback data yet.");
}

function renderAdminStatDetails(data) {
  const topGender = data.genderBreakdown[0];
  const topDiagnosis = data.diagnosisBreakdown[0];
  const answered = data.feedbackEffectiveness.correctFeedbacks + data.feedbackEffectiveness.incorrectFeedbacks;
  const unanswered = data.feedbackEffectiveness.unansweredFeedbacks;
  const retrainRate = data.feedbackEffectiveness.totalFeedbacks
    ? ((data.feedbackEffectiveness.retrainReadyFeedbacks / data.feedbackEffectiveness.totalFeedbacks) * 100).toFixed(1)
    : "0.0";

  adminStatDetails.innerHTML = `
    <div class="detailCard">
      <strong>User Details</strong>
      <span>Total users: ${data.summary.totalUsers}</span>
      <span>New users this month: ${data.summary.newUsersThisMonth}</span>
      <span>Largest gender group: ${topGender ? `${topGender.label} (${topGender.count})` : "N/A"}</span>
    </div>
    <div class="detailCard">
      <strong>Prediction Details</strong>
      <span>Total predictions: ${data.summary.totalPredictions}</span>
      <span>Predictions this month: ${data.summary.predictionsThisMonth}</span>
      <span>Top diagnosis: ${topDiagnosis ? `${topDiagnosis.label} (${topDiagnosis.count})` : "N/A"}</span>
    </div>
    <div class="detailCard">
      <strong>Feedback Details</strong>
      <span>Total feedbacks: ${data.feedbackEffectiveness.totalFeedbacks}</span>
      <span>Answered feedbacks: ${answered}</span>
      <span>Unanswered feedbacks: ${unanswered}</span>
    </div>
    <div class="detailCard">
      <strong>Retrain Details</strong>
      <span>Retrain ready: ${data.feedbackEffectiveness.retrainReadyFeedbacks}</span>
      <span>Retrain rate: ${retrainRate}%</span>
      <span>Accuracy from feedback: ${data.feedbackEffectiveness.accuracyRate}%</span>
    </div>
  `;
}

function renderAdminDashboard(data) {
  state.adminDashboard = data;
  statTotalUsers.textContent = data.summary.totalUsers;
  statNewUsersMonth.textContent = `${data.summary.newUsersThisMonth} new this month`;
  statTotalPredictions.textContent = data.summary.totalPredictions;
  statPredictionsMonth.textContent = `${data.summary.predictionsThisMonth} this month`;
  statTotalFeedbacks.textContent = data.summary.totalFeedbacks;
  statFeedbackAccuracy.textContent = `${data.summary.feedbackAccuracyRate}% accuracy`;
  statRetrainReady.textContent = data.feedbackEffectiveness.retrainReadyFeedbacks;

  const answered = data.feedbackEffectiveness.correctFeedbacks + data.feedbackEffectiveness.incorrectFeedbacks;
  statAnsweredFeedbacks.textContent = `${answered} answered feedbacks`;

  renderBarChart(monthlyUsersChart, data.monthlyUsers);
  renderMetricList(genderBreakdown, data.genderBreakdown);
  renderMetricList(diagnosisBreakdown, data.diagnosisBreakdown);
  renderFeedbackEffectiveness(data.feedbackEffectiveness);
  renderAdminStatDetails(data);
}

async function loadAdminDashboard() {
  if (!isAdmin()) return;
  const data = await apiFetch("/api/admin/dashboard");
  renderAdminDashboard(data);
}

function renderAdminPredictionDetail(item) {
  if (!item) {
    adminPredictionDetail.className = "adminPredictionDetail emptyState";
    adminPredictionDetail.textContent = "Select a prediction to preview image and result.";
    return;
  }

  adminPredictionDetail.className = "adminPredictionDetail";
  adminPredictionDetail.innerHTML = `
    <img id="adminPreviewImage" class="predictionPreviewImage" alt="Prediction image" />
    <div class="detailGrid">
      <div><strong>Result</strong><span>${item.predictedClass || "unknown"}</span></div>
      <div><strong>Probability</strong><span>${item.probability ?? "0"}</span></div>
      <div><strong>Requested At</strong><span>${formatDateTime(item.requestedAt)}</span></div>
      <div><strong>Model</strong><span>${item.modelName || "N/A"} ${item.modelVersion || ""}</span></div>
      <div><strong>Feedback</strong><span>${formatFeedbackLabel(item.isCorrect)}</span></div>
      <div><strong>User Label</strong><span>${item.userLabel || "N/A"}</span></div>
      <div><strong>Comment</strong><span>${item.comment || "N/A"}</span></div>
      <div>
        <strong>Allow For Retrain</strong>
        <label class="switchRow">
          <input id="adminAllowForRetrainToggle" type="checkbox" ${item.allowForRetrain ? "checked" : ""} ${item.feedbackCreatedAt ? "" : "disabled"} />
          <span>${item.feedbackCreatedAt ? "Update database value" : "No feedback yet"}</span>
        </label>
      </div>
    </div>
  `;

  loadAdminPredictionImage(item.imageUrl);

  const retrainToggle = document.getElementById("adminAllowForRetrainToggle");
  if (retrainToggle && item.feedbackCreatedAt) {
    retrainToggle.addEventListener("change", async () => {
      try {
        const response = await apiFetch(`/api/admin/predictions/${item.predictionId}/feedback`, {
          method: "PATCH",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ allowForRetrain: retrainToggle.checked })
        });

        const index = state.adminUserPredictions.findIndex(p => p.predictionId === item.predictionId);
        if (index >= 0) {
          state.adminUserPredictions[index].allowForRetrain = response.allowForRetrain;
          renderAdminPredictionList(applyAdminPredictionFilters(state.adminUserPredictions));
        }
      } catch (err) {
        retrainToggle.checked = !retrainToggle.checked;
        showError(formatError(err, "Update allowForRetrain failed"));
      }
    });
  }
}

async function loadAdminPredictionImage(imageUrl) {
  const image = document.getElementById("adminPreviewImage");
  if (!image) return;

  try {
    const response = await fetch(`${API_BASE_URL}${imageUrl}`, {
      headers: { Authorization: `Bearer ${state.token}` }
    });
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`);
    }
    const blob = await response.blob();
    image.src = URL.createObjectURL(blob);
  } catch (_) {
    image.alt = "Cannot load image";
  }
}

function applyAdminPredictionFilters(items) {
  return items.filter(item => {
    const correctFilter = adminCorrectFilter.value;
    const retrainFilter = adminRetrainFilter.value;

    const correctMatches =
      correctFilter === "all" ||
      (correctFilter === "true" && item.isCorrect === true) ||
      (correctFilter === "false" && item.isCorrect === false) ||
      (correctFilter === "unknown" && item.isCorrect == null);

    const retrainMatches =
      retrainFilter === "all" ||
      (retrainFilter === "true" && item.allowForRetrain === true) ||
      (retrainFilter === "false" && item.allowForRetrain === false);

    return correctMatches && retrainMatches;
  });
}

function renderAdminPredictionList(items) {
  adminPredictionList.innerHTML = "";
  if (!items.length) {
    adminPredictionList.innerHTML = "<p class=\"emptyState\">No prediction matches current filters.</p>";
    renderAdminPredictionDetail(null);
    return;
  }

  items.forEach(item => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `listCard ${state.selectedAdminPredictionId === item.predictionId ? "active" : ""}`;
    button.innerHTML = `
      <strong>${item.predictedClass || "unknown"}</strong>
      <small>${formatDateTime(item.requestedAt)}</small>
      <small>Feedback: ${formatFeedbackLabel(item.isCorrect)}</small>
    `;
    button.addEventListener("click", () => {
      state.selectedAdminPredictionId = item.predictionId;
      renderAdminPredictionList(items);
      renderAdminPredictionDetail(item);
    });
    adminPredictionList.appendChild(button);
  });

  const selected = items.find(item => item.predictionId === state.selectedAdminPredictionId) || items[0];
  state.selectedAdminPredictionId = selected.predictionId;
  renderAdminPredictionDetail(selected);
}

async function loadAdminUserPredictions(userId) {
  const items = await apiFetch(`/api/admin/users/${userId}/predictions`);
  state.adminUserPredictions = items;
  renderAdminPredictionList(applyAdminPredictionFilters(items));
}

function renderAdminUsers(items) {
  adminUsersList.innerHTML = "";
  if (!items.length) {
    adminUsersList.innerHTML = "<p class=\"emptyState\">No users found.</p>";
    adminSelectedUserLabel.textContent = "Select a user to view predictions";
    adminPredictionList.innerHTML = "";
    renderAdminPredictionDetail(null);
    return;
  }

  items.forEach(user => {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `listCard ${state.selectedAdminUserId === user.userId ? "active" : ""}`;
    button.innerHTML = `
      <strong>${user.fullName || user.email}</strong>
      <small>${user.email}</small>
      <small>${user.gender || "unknown"} | ${user.predictionCount} predictions</small>
    `;
    button.addEventListener("click", async () => {
      state.selectedAdminUserId = user.userId;
      state.selectedAdminPredictionId = "";
      renderAdminUsers(items);
      adminSelectedUserLabel.textContent = `${user.fullName || user.email} | ${user.email}`;
      try {
        await loadAdminUserPredictions(user.userId);
      } catch (err) {
        showError(formatError(err, "Load user prediction history failed"));
      }
    });
    adminUsersList.appendChild(button);
  });
}

async function loadAdminUsers() {
  if (!isAdmin()) return;
  const items = await apiFetch("/api/admin/users");
  state.adminUsers = items;
  renderAdminUsers(items);

  const selected = items.find(item => item.userId === state.selectedAdminUserId) || items[0];
  if (selected) {
    state.selectedAdminUserId = selected.userId;
    renderAdminUsers(items);
    adminSelectedUserLabel.textContent = `${selected.fullName || selected.email} | ${selected.email}`;
    await loadAdminUserPredictions(selected.userId);
  }
}

async function parseResponseError(response) {
  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    try {
      const payload = await response.json();
      const message = payload.message || `HTTP ${response.status}`;
      const code = payload.code || null;
      return new ApiHttpError(message, response.status, code, payload);
    } catch (_) {
      return new ApiHttpError(`HTTP ${response.status}`, response.status, null, null);
    }
  }

  const text = await response.text();
  return new ApiHttpError(text || `HTTP ${response.status}`, response.status, null, null);
}

async function apiFetch(path, options = {}, auth = true) {
  const reqId = Math.random().toString(36).slice(2, 8);
  const method = options.method || "GET";
  const headers = new Headers(options.headers || {});
  if (auth && state.token) headers.set("Authorization", `Bearer ${state.token}`);
  const hasJwtHeader = headers.has("Authorization");

  const requestHeaders = Object.fromEntries(headers.entries());
  if (requestHeaders.authorization) {
    requestHeaders.authorization = "Bearer ***";
  }
  console.log(`[API][REQ ${reqId}] ${method} ${path}`, {
    authRequired: auth,
    hasJwtHeader,
    headers: requestHeaders
  });

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    const err = await parseResponseError(response);
    console.error(`[API][RES ${reqId}] ${method} ${path}`, {
      ok: false,
      status: response.status,
      code: err.code || null,
      message: err.message
    });
    throw err;
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const data = await response.json();
    console.log(`[API][RES ${reqId}] ${method} ${path}`, {
      ok: true,
      status: response.status,
      data
    });
    return data;
  }

  const text = await response.text();
  console.log(`[API][RES ${reqId}] ${method} ${path}`, {
    ok: true,
    status: response.status,
    data: text
  });
  return text;
}

async function loginWithGoogleIdToken(idToken) {
  clearError();
  const data = await apiFetch("/api/auth/google", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken })
  }, false);
  saveToken(data.accessToken);
  await bootstrapAfterLogin(data.profileCompleted);
}

async function bootstrapAfterLogin(profileCompletedFromAuth) {
  state.guestMode = false;
  state.activePage = "user";
  historyPanel.classList.remove("hidden");
  feedbackPanel.classList.remove("hidden");
  const me = await apiFetch("/api/me");
  state.me = me;

  if (!profileCompletedFromAuth && !me.profileCompleted) {
    setView("profileView");
    return;
  }

  meInfo.textContent = `${me.fullName || "Unknown"} | ${me.email} | role: ${me.role}`;
  syncAdminVisibility();
  setActivePage("user");
  setView("mainView");
  await loadHistory();
  if (isAdmin()) {
    await loadAdminDashboard();
    await loadAdminUsers();
  }
}

async function loadHistory() {
  const items = await apiFetch("/api/predictions?page=0&size=20");
  historyList.innerHTML = "";

  if (!items.length) {
    historyList.innerHTML = "<li>No history yet.</li>";
    return;
  }

  items.forEach(item => {
    const li = document.createElement("li");
    li.innerHTML = `<strong>${item.predictedClass}</strong> (${item.probability})<br/><small>${item.requestedAt}</small>`;
    li.onclick = () => {
      clearError();
      renderDiseaseResult(item);
      state.latestPredictionId = item.predictionId;
      predictionIdInput.value = item.predictionId;
    };
    historyList.appendChild(li);
  });
}

function initGoogleButton() {
  if (googleInitialized) return true;
  if (!window.google || !window.google.accounts || !window.google.accounts.id) return false;
  if (!GOOGLE_CLIENT_ID || GOOGLE_CLIENT_ID.includes("PUT_YOUR")) return false;

  google.accounts.id.initialize({
    client_id: GOOGLE_CLIENT_ID,
    callback: async (response) => {
      try {
        await loginWithGoogleIdToken(response.credential);
      } catch (e) {
        showError(formatError(e, "Login failed"));
      }
    }
  });

  google.accounts.id.renderButton(
    googleBtnContainer,
    { theme: "outline", size: "large", shape: "pill" }
  );
  googleInitialized = true;
  return true;
}

function waitForGoogleSdkAndRenderButton() {
  clearError();

  let attempts = 0;
  const maxAttempts = 40;
  const timer = setInterval(() => {
    if (initGoogleButton()) {
      clearInterval(timer);
      return;
    }

    attempts += 1;
    if (attempts >= maxAttempts) {
      clearInterval(timer);
      showError("Google login is not ready. Please check GOOGLE_CLIENT_ID in config.js and reload.");
    }
  }, 250);
}

document.getElementById("profileForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  clearError();
  try {
    await apiFetch("/api/me/profile", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        fullName: document.getElementById("fullName").value.trim(),
        gender: document.getElementById("gender").value,
        age: Number(document.getElementById("age").value)
      })
    });
    await bootstrapAfterLogin(true);
  } catch (err) {
    showError(formatError(err, "Update profile failed"));
  }
});

document.getElementById("predictForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  clearError();

  const files = document.getElementById("filesInput").files;
  if (!files.length) return showError("Predict failed: [FILES_REQUIRED] Please choose image files.");

  const formData = new FormData();
  Array.from(files).forEach(file => formData.append("files", file));

  try {
    const endpoint = state.guestMode ? "/api/predictions/quick-check" : "/api/predictions";
    const data = await apiFetch(endpoint, {
      method: "POST",
      body: formData
    }, !state.guestMode);

    const first = data.results?.[0];
    renderDiseaseResult(first);
    if (first?.predictionId) {
      state.latestPredictionId = first.predictionId;
      predictionIdInput.value = first.predictionId;
    } else {
      state.latestPredictionId = "";
      predictionIdInput.value = "";
    }
    if (!state.guestMode) {
      await loadHistory();
    }
  } catch (err) {
    if (err instanceof ApiHttpError && err.status === 401) {
      clearToken();
      setView("loginView");
    }
    showError(formatError(err, "Predict failed"));
  }
});

document.getElementById("feedbackForm").addEventListener("submit", async (e) => {
  e.preventDefault();
  clearError();
  if (state.guestMode) return showError("Feedback is disabled in guest mode.");

  const predictionId = predictionIdInput.value || state.latestPredictionId;
  if (!predictionId) return showError("Feedback failed: [PREDICTION_REQUIRED] No prediction selected.");

  const rawIsCorrect = document.getElementById("isCorrect").value;

  try {
    await apiFetch(`/api/predictions/${predictionId}/feedbacks`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        isCorrect: rawIsCorrect === "" ? null : rawIsCorrect === "true",
        userLabel: document.getElementById("userLabel").value || null,
        comment: document.getElementById("comment").value || null
      })
    });
    alert("Feedback sent.");
  } catch (err) {
    if (err instanceof ApiHttpError && err.status === 401) {
      clearToken();
      setView("loginView");
    }
    showError(formatError(err, "Feedback failed"));
  }
});

document.getElementById("refreshHistoryBtn").addEventListener("click", async () => {
  clearError();
  if (state.guestMode) return;
  try {
    await loadHistory();
  } catch (err) {
    showError(formatError(err, "Load history failed"));
  }
});

userPageBtn.addEventListener("click", () => {
  clearError();
  setActivePage("user");
});

adminPageBtn.addEventListener("click", async () => {
  if (!isAdmin()) return;
  clearError();
  setActivePage("admin");
  try {
    await loadAdminDashboard();
    await loadAdminUsers();
  } catch (err) {
    showError(formatError(err, "Load admin dashboard failed"));
  }
});

refreshAdminBtn.addEventListener("click", async () => {
  clearError();
  try {
    await loadAdminDashboard();
  } catch (err) {
    showError(formatError(err, "Refresh admin dashboard failed"));
  }
});

refreshAdminUsersBtn.addEventListener("click", async () => {
  clearError();
  try {
    await loadAdminUsers();
  } catch (err) {
    showError(formatError(err, "Refresh admin users failed"));
  }
});

document.getElementById("logoutBtn").addEventListener("click", () => {
  clearToken();
  state.guestMode = false;
  state.me = null;
  state.latestPredictionId = "";
  state.adminDashboard = null;
  state.activePage = "user";
  state.adminUsers = [];
  state.selectedAdminUserId = "";
  state.selectedAdminPredictionId = "";
  state.adminUserPredictions = [];
  clearError();
  setView("loginView");
});

adminCorrectFilter.addEventListener("change", () => {
  renderAdminPredictionList(applyAdminPredictionFilters(state.adminUserPredictions));
});

adminRetrainFilter.addEventListener("change", () => {
  renderAdminPredictionList(applyAdminPredictionFilters(state.adminUserPredictions));
});

(async function init() {
  waitForGoogleSdkAndRenderButton();

  if (!state.token) {
    setView("loginView");
    return;
  }

  try {
    await bootstrapAfterLogin(false);
  } catch (err) {
    clearToken();
    setView("loginView");
    showError(formatError(err, "Session restore failed"));
  }
})();

