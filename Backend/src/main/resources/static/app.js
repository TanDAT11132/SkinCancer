const API_BASE_URL = window.APP_CONFIG?.API_BASE_URL || "http://localhost:8000";
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
  latestPredictionId: ""
};

const loginView = document.getElementById("loginView");
const profileView = document.getElementById("profileView");
const mainView = document.getElementById("mainView");
const meInfo = document.getElementById("meInfo");
const latestResult = document.getElementById("latestResult");
const historyList = document.getElementById("historyList");
const predictionIdInput = document.getElementById("predictionId");
const errorBanner = document.getElementById("errorBanner");
const googleLoginBtn = document.getElementById("googleLoginBtn");
const googleBtnContainer = document.getElementById("googleBtnContainer");

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
  const headers = new Headers(options.headers || {});
  if (auth && state.token) headers.set("Authorization", `Bearer ${state.token}`);

  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers });
  if (!response.ok) {
    throw await parseResponseError(response);
  }

  const contentType = response.headers.get("content-type") || "";
  if (contentType.includes("application/json")) return response.json();
  return response.text();
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
  const me = await apiFetch("/api/me");
  state.me = me;

  if (!profileCompletedFromAuth && !me.profileCompleted) {
    setView("profileView");
    return;
  }

  meInfo.textContent = `${me.fullName || "Unknown"} | ${me.email} | role: ${me.role}`;
  setView("mainView");
  await loadHistory();
}

async function loadHistory() {
  const items = await apiFetch("/api/predictions/history?page=0&size=20");
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
      latestResult.textContent = JSON.stringify(item, null, 2);
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

googleLoginBtn.addEventListener("click", () => {
  clearError();

  if (!initGoogleButton()) {
    showError("Google login is not ready. Please set a valid GOOGLE_CLIENT_ID in config.js");
    return;
  }

  google.accounts.id.prompt((notification) => {
    if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
      const reason = notification.getNotDisplayedReason?.() || notification.getSkippedReason?.() || "unknown_reason";
      showError(`Cannot open Google login prompt (${reason}). Check Google Cloud origin and browser cookie settings.`);
    }
  });
});

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

  const topK = Number(document.getElementById("topK").value || 3);
  const clientApp = document.getElementById("clientApp").value || "web";

  const formData = new FormData();
  Array.from(files).forEach(file => formData.append("files", file));

  try {
    const data = await apiFetch(`/api/predictions/check?top_k=${topK}&client_app=${encodeURIComponent(clientApp)}`, {
      method: "POST",
      body: formData
    });

    const first = data.results?.[0];
    latestResult.textContent = JSON.stringify(data, null, 2);
    if (first?.predictionId) {
      state.latestPredictionId = first.predictionId;
      predictionIdInput.value = first.predictionId;
    }
    await loadHistory();
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

  const predictionId = predictionIdInput.value || state.latestPredictionId;
  if (!predictionId) return showError("Feedback failed: [PREDICTION_REQUIRED] No prediction selected.");

  const rawIsCorrect = document.getElementById("isCorrect").value;

  try {
    await apiFetch(`/api/predictions/${predictionId}/feedback`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        isCorrect: rawIsCorrect === "" ? null : rawIsCorrect === "true",
        userLabel: document.getElementById("userLabel").value || null,
        comment: document.getElementById("comment").value || null,
        allowForRetrain: document.getElementById("allowForRetrain").checked
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
  try {
    await loadHistory();
  } catch (err) {
    showError(formatError(err, "Load history failed"));
  }
});

document.getElementById("logoutBtn").addEventListener("click", () => {
  clearToken();
  state.me = null;
  state.latestPredictionId = "";
  clearError();
  setView("loginView");
});

(async function init() {
  initGoogleButton();

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

