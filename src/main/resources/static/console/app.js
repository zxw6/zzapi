const state = {
    token: localStorage.getItem("zxw-console-token") || "",
    me: null,
    overview: null,
    accessSummary: null,
    trend: [],
    users: [],
    keys: [],
    providers: [],
    models: [],
    logs: [],
    modelStats: [],
    packagePurchaseRecords: [],
    walletTransactions: [],
    upstreamModels: [],
    modelProviderFilter: "ALL",
    selectedPanel: "overview-panel",
    selectedModelId: null,
    selectedPackageGroupId: null,
    selectedPackageId: null,
    editingUserId: null,
    userDetailMode: "view",
    editingModelId: null,
    editingModelBindingId: null
};

let dashboardRefreshTimer = null;

const panelMeta = {
    "overview-panel": {
        title: "仪表盘",
        subtitle: "查看账户概览、余额和请求趋势。"
    },
    "docs-panel": {
        title: "使用教程",
        subtitle: "查看 Codex 的安装、配置、启动方式和常见问题。"
    },
    "billing-panel": {
        title: "用量账单",
        subtitle: "查看用量统计、费用趋势和模型分布。"
    },
    "users-panel": {
        title: "用户管理",
        subtitle: "查看、编辑和管理用户资料与余额。"
    },
    "keys-panel": {
        title: "API 密钥",
        subtitle: "创建并管理当前账户的 API 密钥。"
    },
    "providers-panel": {
        title: "渠道管理",
        subtitle: "配置 OpenAI、Claude 及兼容上游渠道。"
    },
    "models-panel": {
        title: "套餐中心",
        subtitle: "购买分组套餐，并查看各分组对应的模型能力。"
    },
    "logs-panel": {
        title: "请求日志",
        subtitle: "查看最近请求、Token 消耗与计费记录。"
    }
};

const elements = {
    loginPanel: document.getElementById("login-panel"),
    appPanel: document.getElementById("app-panel"),
    loginForm: document.getElementById("login-form"),
    registerModal: document.getElementById("register-modal"),
    openRegisterButton: document.getElementById("open-register-button"),
    closeRegisterButton: document.getElementById("close-register-button"),
    registerForm: document.getElementById("register-form"),
    sendRegisterCodeButton: document.getElementById("send-register-code-button"),
    registerCodeHint: document.getElementById("register-code-hint"),
    userDetailModal: document.getElementById("user-detail-modal"),
    closeUserDetailButton: document.getElementById("close-user-detail-button"),
    userDetailForm: document.getElementById("user-detail-form"),
    userDetailTitle: document.getElementById("user-detail-title"),
    userDetailCopy: document.getElementById("user-detail-copy"),
    saveUserDetailButton: document.getElementById("save-user-detail-button"),
    refreshAllButton: document.getElementById("refresh-all-button"),
    logoutButton: document.getElementById("logout-button"),
    pageTitle: document.getElementById("page-title"),
    pageSubtitle: document.getElementById("page-subtitle"),
    currentUser: document.getElementById("current-user"),
    currentUserEmail: document.getElementById("current-user-email"),
    currentBalance: document.getElementById("current-balance"),
    accountAvatar: document.getElementById("account-avatar"),
    overviewCards: document.getElementById("overview-cards"),
    profileCardBody: document.getElementById("profile-card-body"),
    dashboardPackageSelect: document.getElementById("dashboard-package-select"),
    quotaCardTitle: document.getElementById("quota-card-title"),
    quotaStatusBadge: document.getElementById("quota-status-badge"),
    quotaCardBody: document.getElementById("quota-card-body"),
    dashboardGreeting: document.getElementById("dashboard-greeting"),
    dashboardGreetingCopy: document.getElementById("dashboard-greeting-copy"),
    trendSummary: document.getElementById("trend-summary"),
    trendChart: document.getElementById("trend-chart"),
    overviewEndpointUrl: document.getElementById("overview-endpoint-url"),
    billingSummaryCards: document.getElementById("billing-summary-cards"),
    billingTrendSummary: document.getElementById("billing-trend-summary"),
    billingTrendChart: document.getElementById("billing-trend-chart"),
    billingModelUsage: document.getElementById("billing-model-usage"),
    billingModelStatsTable: document.getElementById("billing-model-stats-table"),
    usersMenuItem: document.getElementById("users-menu-item"),
    usersPanelTitle: document.getElementById("users-panel-title"),
    userForm: document.getElementById("user-form"),
    rechargeForm: document.getElementById("recharge-form"),
    usersTable: document.getElementById("users-table"),
    keysMenuLabel: document.getElementById("keys-menu-label"),
    keysPanelTitle: document.getElementById("keys-panel-title"),
    keysPanelSubtitle: document.getElementById("keys-panel-subtitle"),
    toggleKeyCreateButton: document.getElementById("toggle-key-create-button"),
    closeKeyCreateButton: document.getElementById("close-key-create-button"),
    keyCreateBox: document.getElementById("key-create-box"),
    keyForm: document.getElementById("key-form"),
    keyUserIdRow: document.getElementById("key-user-id-row"),
    keyGroupHint: document.getElementById("key-group-hint"),
    keyGroupOptions: document.getElementById("key-group-options"),
    modelPackageSummary: document.getElementById("model-package-summary"),
    packageCardGrid: document.getElementById("package-card-grid"),
    packageBalancePill: document.getElementById("package-balance-pill"),
    togglePackageCreateButton: document.getElementById("toggle-package-create-button"),
    closePackageCreateButton: document.getElementById("close-package-create-button"),
    packageCreateBox: document.getElementById("package-create-box"),
    packageForm: document.getElementById("package-form"),
    packageSubmitButton: document.getElementById("package-submit-button"),
    plainKeyBox: document.getElementById("plain-key-box"),
    keysList: document.getElementById("keys-list"),
    keysEmpty: document.getElementById("keys-empty"),
    keysTable: document.getElementById("keys-table"),
    providerForm: document.getElementById("provider-form"),
    providersTable: document.getElementById("providers-table"),
    modelsMenuLabel: document.getElementById("models-menu-label"),
    modelsPanelTitle: document.getElementById("models-panel-title"),
    modelProviderFilters: document.getElementById("model-provider-filters"),
    modelsCardGrid: document.getElementById("models-card-grid"),
    modelsEmpty: document.getElementById("models-empty"),
    modelsMarketMeta: document.getElementById("models-market-meta"),
    modelDetailBadge: document.getElementById("model-detail-badge"),
    modelDetailTitle: document.getElementById("model-detail-title"),
    modelDetailDesc: document.getElementById("model-detail-desc"),
    modelDetailSpecs: document.getElementById("model-detail-specs"),
    modelDetailPricing: document.getElementById("model-detail-pricing"),
    modelDetailCode: document.getElementById("model-detail-code"),
    fetchUpstreamModelsButton: document.getElementById("fetch-upstream-models-button"),
    importGroupSelect: document.getElementById("import-group-select"),
    importProviderSelect: document.getElementById("import-provider-select"),
    importPromptPrice: document.getElementById("import-prompt-price"),
    importCachedPromptPrice: document.getElementById("import-cached-prompt-price"),
    importCompletionPrice: document.getElementById("import-completion-price"),
    importMultiplier: document.getElementById("import-multiplier"),
    importIsPublic: document.getElementById("import-is-public"),
    toggleAllUpstreamModelsButton: document.getElementById("toggle-all-upstream-models-button"),
    importUpstreamModelsButton: document.getElementById("import-upstream-models-button"),
    upstreamModelsBox: document.getElementById("upstream-models-box"),
    importResultBox: document.getElementById("import-result-box"),
    modelForm: document.getElementById("model-form"),
    modelFormTitle: document.getElementById("model-form-title"),
    modelFormCaption: document.getElementById("model-form-caption"),
    modelSubmitButton: document.getElementById("model-submit-button"),
    modelCancelEditButton: document.getElementById("model-cancel-edit-button"),
    modelsTable: document.getElementById("models-table"),
    packagePurchasesTable: document.getElementById("package-purchases-table"),
    walletTransactionsTable: document.getElementById("wallet-transactions-table"),
    logsTable: document.getElementById("logs-table"),
    agentDebugModel: document.getElementById("agent-debug-model"),
    agentDebugWorkspace: document.getElementById("agent-debug-workspace"),
    agentDebugPrompt: document.getElementById("agent-debug-prompt"),
    agentDebugEndpoint: document.getElementById("agent-debug-endpoint"),
    agentDebugPayload: document.getElementById("agent-debug-payload"),
    agentDebugResponse: document.getElementById("agent-debug-response"),
    agentDebugCopyPayloadButton: document.getElementById("agent-debug-copy-payload"),
    agentDebugCopyCurlButton: document.getElementById("agent-debug-copy-curl"),
    agentDebugSendButton: document.getElementById("agent-debug-send"),
    toast: document.getElementById("toast"),
    menuItems: Array.from(document.querySelectorAll(".menu-item")),
    panels: Array.from(document.querySelectorAll(".view-panel")),
    refreshButtons: Array.from(document.querySelectorAll("[data-refresh]")),
    adminOnlyBlocks: Array.from(document.querySelectorAll(".admin-only")),
    adminOnlyUserDetail: Array.from(document.querySelectorAll(".admin-only-user-detail"))
};

function isAdmin() {
    return state.me?.roleCode === "ADMIN";
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function toNumber(value) {
    const num = Number(value ?? 0);
    return Number.isFinite(num) ? num : 0;
}

const API_KEY_STORE_KEY = "zxw-plain-api-keys";

function readStoredPlainKeys() {
    try {
        const raw = localStorage.getItem(API_KEY_STORE_KEY);
        const parsed = raw ? JSON.parse(raw) : {};
        return parsed && typeof parsed === "object" ? parsed : {};
    } catch {
        return {};
    }
}

function writeStoredPlainKeys(store) {
    localStorage.setItem(API_KEY_STORE_KEY, JSON.stringify(store || {}));
}

function rememberPlainApiKey(plainTextKey) {
    const key = String(plainTextKey || "").trim();
    if (!key) {
        return;
    }
    const store = readStoredPlainKeys();
    store[key.slice(0, 20)] = key;
    writeStoredPlainKeys(store);
}

function forgetPlainApiKey(accessKey) {
    const prefix = String(accessKey || "").trim();
    if (!prefix) {
        return;
    }
    const store = readStoredPlainKeys();
    if (!store[prefix]) {
        return;
    }
    delete store[prefix];
    writeStoredPlainKeys(store);
}

function hydrateApiKeys(keys) {
    const store = readStoredPlainKeys();
    return (keys || []).map((key) => ({
        ...key,
        plainTextKey: store[key.accessKey] || ""
    }));
}/*
            showToast("API Key 已复制");
        }
    }));
}

*/
function getUsableApiKey(keys) {
    const activeKey = (keys || []).find((key) => key.status === "ACTIVE" && key.plainTextKey);
    if (activeKey) {
        return activeKey.plainTextKey;
    }
    if ((keys || []).some((key) => key.status === "ACTIVE")) {
        throw new Error("当前浏览器没有可用的完整 API Key。请复制创建时返回的完整密钥，或重新创建一个新的 Key 后再试。");
    }
    throw new Error("当前账号还没有可用的 API Key，请先创建一个。");
}

function getGatewayBaseUrl() {
    const endpoint = String(elements.overviewEndpointUrl?.textContent || "").trim();
    if (endpoint) {
        return endpoint.replace(/\/+$/, "");
    }
    return `${window.location.origin}/v1`;
}

function buildAgentDebugPayload() {
    const model = String(elements.agentDebugModel?.value || "").trim() || "gpt-5.3-codex";
    const workspaceRoot = String(elements.agentDebugWorkspace?.value || "").trim();
    const prompt = String(elements.agentDebugPrompt?.value || "").trim();
    if (!workspaceRoot) {
        throw new Error("请先填写工作区根目录。");
    }
    if (!prompt) {
        throw new Error("请先填写任务指令。");
    }
    return {
        model,
        stream: false,
        metadata: {
            gateway_agent: true,
            workspace_root: workspaceRoot
        },
        input: [
            {
                role: "user",
                content: [
                    {
                        type: "input_text",
                        text: prompt
                    }
                ]
            }
        ]
    };
}

function buildAgentDebugCurl(payload, apiKey) {
    const endpoint = `${getGatewayBaseUrl()}/responses`;
    const body = JSON.stringify(payload, null, 2).replace(/\r?\n/g, "\n");
    const escapedBody = body.replaceAll('"', '\\"');
    return `curl ${endpoint} ^\n  -H "Authorization: Bearer ${apiKey}" ^\n  -H "Content-Type: application/json" ^\n  -d "${escapedBody}"`;
}

function renderAgentDebugPreview() {
    if (!elements.agentDebugPayload || !elements.agentDebugEndpoint) {
        return;
    }
    elements.agentDebugEndpoint.textContent = `${getGatewayBaseUrl()}/responses`;
    try {
        const payload = buildAgentDebugPayload();
        elements.agentDebugPayload.textContent = JSON.stringify(payload, null, 2);
    } catch (error) {
        elements.agentDebugPayload.textContent = error instanceof Error ? error.message : "请补全调试参数。";
    }
}

async function copyAgentDebugPayload() {
    const payload = buildAgentDebugPayload();
    await navigator.clipboard.writeText(JSON.stringify(payload, null, 2));
    renderAgentDebugPreview();
    showToast("Payload 已复制");
}

async function copyAgentDebugCurl() {
    const payload = buildAgentDebugPayload();
    const apiKey = getUsableApiKey(state.keys);
    await navigator.clipboard.writeText(buildAgentDebugCurl(payload, apiKey));
    renderAgentDebugPreview();
    showToast("Curl 已复制");
}

async function sendAgentDebugRequest() {
    const payload = buildAgentDebugPayload();
    const apiKey = getUsableApiKey(state.keys);
    const endpoint = `${getGatewayBaseUrl()}/responses`;
    if (elements.agentDebugResponse) {
        elements.agentDebugResponse.textContent = "请求已发出，正在等待网关响应...";
    }
    renderAgentDebugPreview();

    const response = await fetch(endpoint, {
        method: "POST",
        headers: {
            "Authorization": `Bearer ${apiKey}`,
            "Content-Type": "application/json"
        },
        body: JSON.stringify(payload)
    });
    const text = await response.text();
    let formatted = text;
    try {
        formatted = JSON.stringify(JSON.parse(text), null, 2);
    } catch {
        // Keep raw text when response is not JSON.
    }
    if (elements.agentDebugResponse) {
        elements.agentDebugResponse.textContent = formatted || "(empty response)";
    }
    if (!response.ok) {
        throw new Error(`请求失败: HTTP ${response.status}`);
    }
    showToast("服务端 Agent 测试成功");
}

function formatMoney(value) {
    const amount = toNumber(value);
    return `$${amount.toFixed(2)}`;
}

function formatDecimal(value, digits = 4) {
    return toNumber(value).toFixed(digits);
}

function formatModelRate(value) {
    const num = toNumber(value);
    if (num >= 1) {
        return num.toFixed(2);
    }
    if (num >= 0.1) {
        return num.toFixed(3);
    }
    if (num >= 0.01) {
        return num.toFixed(4);
    }
    return num.toFixed(6);
}

function formatTokens(value) {
    return Math.round(toNumber(value)).toLocaleString("en-US");
}

function formatTokenBreakdown(promptTokens, completionTokens, totalTokens) {
    const prompt = formatTokens(promptTokens || 0);
    const completion = formatTokens(completionTokens || 0);
    const total = formatTokens(totalTokens || 0);
    return `${prompt} / ${completion} · ${total}`;
}

function parseServerDate(value) {
    if (!value) {
        return null;
    }
    if (value instanceof Date) {
        return Number.isNaN(value.getTime()) ? null : value;
    }
    const text = String(value).trim();
    if (!text) {
        return null;
    }
    const normalized = text.includes("T") ? text : text.replace(" ", "T");
    const hasTimeZone = /(?:Z|[+-]\d{2}:?\d{2})$/i.test(normalized);
    const date = new Date(hasTimeZone ? normalized : normalized);
    return Number.isNaN(date.getTime()) ? null : date;
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }
    const date = parseServerDate(value);
    if (!date) {
        return escapeHtml(value);
    }
    const pad = (num) => String(num).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function formatQuotaText(used, quota) {
    return toNumber(quota) > 0 ? `${formatMoney(used)} / ${formatMoney(quota)}` : `${formatMoney(used)} / 未设置`;
}

function getAccessGroups() {
    const summary = state.accessSummary || {};
    return Array.isArray(summary.groups) ? summary.groups : [];
}

function getActiveAccessGroup() {
    const summary = state.accessSummary || {};
    const groups = getAccessGroups();
    return groups.find((item) => item.active) || groups.find((item) => String(item.id) === String(summary.activeGroupId)) || null;
}

function getSelectedPackageGroup() {
    const groups = getAccessGroups();
    if (!groups.length) {
        return null;
    }
    const explicit = groups.find((item) => String(item.id) === String(state.selectedPackageGroupId));
    return explicit || null;
}

function syncSelectedPackageGroup() {
    if (!state.selectedPackageGroupId) {
        return;
    }
    const groups = getAccessGroups();
    if (!groups.some((item) => String(item.id) === String(state.selectedPackageGroupId))) {
        state.selectedPackageGroupId = null;
    }
}

function getPurchasedGroups() {
    return getAccessGroups().filter((item) => item.purchased);
}

function getPurchasedPackages() {
    const groups = getAccessGroups();
    return (state.packagePurchaseRecords || [])
        .map((item) => {
            const group = groups.find((groupItem) => String(groupItem.id) === String(item.groupId)) || {};
            return {
                ...group,
                ...item,
                modelCount: item.modelCount ?? group.modelCount ?? 0,
                packageDays: item.packageDays ?? group.packageDays ?? 30,
                remark: item.remark ?? group.remark ?? ""
            };
        })
        .sort((a, b) => toNumber(a.id) - toNumber(b.id));
}

function sumPurchasedPackageField(field) {
    return getPurchasedPackages().reduce((sum, item) => sum + toNumber(item?.[field]), 0);
}

function isPackageUsable(item) {
    if (!item) {
        return false;
    }
    if (item.active === true) {
        return true;
    }
    const status = String(item.status || item.packageStatus || "").toUpperCase();
    if (status && status !== "ACTIVE") {
        return false;
    }
    if (!item.expiresAt) {
        return status === "ACTIVE";
    }
    const expiresAt = parseServerDate(item.expiresAt);
    return !!expiresAt && expiresAt.getTime() > Date.now();
}

function packageAvailabilityLabel(item) {
    return isPackageUsable(item) ? "可用" : "不可用";
}

function getSelectedPurchasedPackage() {
    const packages = getPurchasedPackages();
    if (!packages.length) {
        return null;
    }
    const explicit = packages.find((item) => String(item.id) === String(state.selectedPackageId));
    if (explicit) {
        return explicit;
    }
    if (state.selectedPackageGroupId) {
        const latestByGroup = getLatestPurchasedPackageByGroup(state.selectedPackageGroupId);
        if (latestByGroup) {
            return latestByGroup;
        }
    }
    return null;
}

function syncSelectedPurchasedPackage() {
    if (!state.selectedPackageId) {
        return;
    }
    const packages = getPurchasedPackages();
    if (!packages.some((item) => String(item.id) === String(state.selectedPackageId))) {
        state.selectedPackageId = null;
    }
}

function getFirstPurchasedPackageByGroup(groupId) {
    return getPurchasedPackages()
        .filter((item) => String(item.groupId) === String(groupId))
        .sort((a, b) => toNumber(b.id) - toNumber(a.id))[0] || null;
}

function getLatestPurchasedPackageByGroup(groupId) {
    return getPurchasedPackages()
        .filter((item) => String(item.groupId) === String(groupId))
        .sort((a, b) => toNumber(b.id) - toNumber(a.id))[0] || null;
}

function buildPurchasedPackageLabel(item) {
    if (!item) {
        return "未购买套餐";
    }
    return `${item.groupName || item.groupCode || "套餐"} #${item.id}`;
}

function getModelRowKey(model) {
    return `${model?.bindingId || "model"}:${model?.id || ""}:${model?.groupId || ""}`;
}

function getModelPreviewGroup() {
    const filter = String(state.modelProviderFilter || "");
    if (filter.startsWith("GROUP:")) {
        const groupId = filter.slice("GROUP:".length);
        return getAccessGroups().find((item) => String(item.id) === String(groupId)) || null;
    }
    return null;
}

function getManageableGroups() {
    return getAccessGroups();
}

function populatePackageGroupSelects() {
    const groups = getManageableGroups();
    const options = groups.length
        ? groups.map((group) => `<option value="${escapeHtml(group.id)}">${escapeHtml(group.groupName)} (${escapeHtml(group.groupCode)})</option>`).join("")
        : '<option value="">暂无套餐</option>';

    if (elements.importGroupSelect) {
        elements.importGroupSelect.innerHTML = groups.length
            ? `<option value="">请选择套餐</option>${options}`
            : options;
        if (state.selectedPackageGroupId && groups.some((item) => String(item.id) === String(state.selectedPackageGroupId))) {
            elements.importGroupSelect.value = String(state.selectedPackageGroupId);
        }
    }

    const modelGroupSelect = elements.modelForm?.elements?.groupId;
    if (modelGroupSelect) {
        modelGroupSelect.innerHTML = groups.length
            ? `<option value="">请选择套餐</option>${options}`
            : options;
        if (!state.editingModelId && state.selectedPackageGroupId && groups.some((item) => String(item.id) === String(state.selectedPackageGroupId))) {
            modelGroupSelect.value = String(state.selectedPackageGroupId);
        }
    }
}

function getPackageTotalQuota(group) {
    if (!group) {
        return 0;
    }
    const total = toNumber(group.totalQuota);
    if (total > 0) {
        return total;
    }
    const monthly = toNumber(group.monthlyQuota);
    if (monthly > 0) {
        return monthly;
    }
    return toNumber(group.dailyQuota) * Math.max(toNumber(group.packageDays), 0);
}

function getLast7DaysSpend() {
    return state.trend.reduce((sum, item) => sum + toNumber(item.userAmount), 0);
}

function sumValues(list, field) {
    return (list || []).reduce((total, item) => total + toNumber(item?.[field]), 0);
}

function percentage(part, total) {
    if (!total) {
        return 0;
    }
    return Math.max(0, Math.min(100, (toNumber(part) / toNumber(total)) * 100));
}

function roleLabel(roleCode) {
    return String(roleCode || "").toUpperCase() === "ADMIN" ? "管理员" : "普通用户";
}

function statusLabel(status) {
    const normalized = String(status || "").toUpperCase();
    if (normalized === "ACTIVE") {
        return "启用";
    }
    if (normalized === "DISABLED") {
        return "禁用";
    }
    if (normalized === "SUCCESS") {
        return "成功";
    }
    return normalized || "-";
}

function statusChip(status) {
    const normalized = String(status || "").toUpperCase();
    const className = normalized === "ACTIVE" || normalized === "SUCCESS" ? "active" : "disabled";
    return `<span class="status-chip ${className}">${escapeHtml(statusLabel(normalized))}</span>`;
}

function accessStatusBadge(status) {
    const normalized = String(status || "").toUpperCase();
    if (normalized === "ACTIVE") {
        return '<span class="status-chip active">使用中</span>';
    }
    if (normalized === "EXPIRED") {
        return '<span class="status-chip disabled">已过期</span>';
    }
    if (normalized === "NOT_PURCHASED") {
        return '<span class="soft-badge">未购买</span>';
    }
    return '<span class="soft-badge">待处理</span>';
}

async function copyApiKey(id) {
    const key = state.keys.find((item) => String(item.id) === String(id));
    if (!key) {
        throw new Error("未找到对应的 API Key");
    }
    if (!key.plainTextKey) {
        throw new Error("当前浏览器没有保存这个 Key 的完整明文，请使用创建成功时返回的密钥，或重新创建一个新的 Key。");
    }
    await navigator.clipboard.writeText(key.plainTextKey);
    showToast("完整 API Key 已复制");
}

let toastTimer = null;
let registerCodeCooldownTimer = null;
let registerCodeSentToEmail = "";
const REGISTER_CODE_HINT_DEFAULT = "验证码将发送到你的 QQ 邮箱，5 分钟内有效。";

function showToast(message, isError = false) {
    elements.toast.textContent = message || (isError ? "操作失败" : "操作成功");
    elements.toast.style.background = isError ? "rgba(140, 42, 27, 0.94)" : "rgba(51, 31, 18, 0.92)";
    elements.toast.classList.remove("hidden");
    window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(() => elements.toast.classList.add("hidden"), 2600);
}

async function fetchJson(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (state.token) {
        headers.set("Authorization", `Bearer ${state.token}`);
    }
    if (options.body && !headers.has("Content-Type")) {
        headers.set("Content-Type", "application/json");
    }
    const response = await fetch(url, {
        ...options,
        headers,
        body: options.body ? JSON.stringify(options.body) : undefined
    });
    const data = await response.json().catch(() => null);
    if (!response.ok || !data?.success) {
        throw new Error(data?.message || `请求失败: HTTP ${response.status}`);
    }
    return data.data;
}

function updateRegisterCodeUi(secondsLeft = 0) {
    if (!elements.sendRegisterCodeButton || !elements.registerCodeHint) {
        return;
    }
    const coolingDown = secondsLeft > 0;
    elements.sendRegisterCodeButton.disabled = coolingDown;
    elements.sendRegisterCodeButton.textContent = coolingDown
        ? `重新发送 (${secondsLeft}s)`
        : "发送验证码";
}

function resetRegisterCodeState({ resetForm = false } = {}) {
    window.clearInterval(registerCodeCooldownTimer);
    registerCodeCooldownTimer = null;
    registerCodeSentToEmail = "";
    updateRegisterCodeUi(0);
    if (elements.registerCodeHint) {
        elements.registerCodeHint.textContent = REGISTER_CODE_HINT_DEFAULT;
    }
    if (resetForm) {
        elements.registerForm?.reset();
    }
}

function startRegisterCodeCooldown(durationSeconds = 60) {
    window.clearInterval(registerCodeCooldownTimer);
    let secondsLeft = Math.max(0, Math.floor(durationSeconds));
    updateRegisterCodeUi(secondsLeft);
    if (secondsLeft <= 0) {
        return;
    }
    registerCodeCooldownTimer = window.setInterval(() => {
        secondsLeft -= 1;
        updateRegisterCodeUi(secondsLeft);
        if (secondsLeft <= 0) {
            window.clearInterval(registerCodeCooldownTimer);
            registerCodeCooldownTimer = null;
        }
    }, 1000);
}

function setMenuLabel() {
    const userText = isAdmin() ? "用户管理" : "个人中心";
    elements.usersMenuItem.lastElementChild.textContent = userText;
    elements.usersPanelTitle.textContent = userText;
}

function toggleAuth(isLoggedIn) {
    elements.loginPanel.classList.toggle("hidden", isLoggedIn);
    elements.appPanel.classList.toggle("hidden", !isLoggedIn);
}

function applyRoleView() {
    const admin = isAdmin();
    elements.adminOnlyBlocks.forEach((node) => node.classList.toggle("hidden", !admin));
    elements.adminOnlyUserDetail.forEach((node) => node.classList.toggle("hidden", !admin));
    elements.keysPanelTitle.textContent = admin ? "API 密钥管理" : "我的 API 密钥";
    elements.modelsPanelTitle.textContent = "套餐中心";
    elements.modelsMenuLabel.textContent = "套餐中心";
    elements.keyUserIdRow.classList.toggle("hidden", !admin);
    const userIdInput = elements.keyForm.elements.userId;
    if (admin) {
        userIdInput.readOnly = false;
    } else {
        userIdInput.readOnly = true;
        userIdInput.value = state.me?.userId || "";
    }
    setMenuLabel();
}

function selectPanel(panelId) {
    state.selectedPanel = panelId;
    elements.panels.forEach((panel) => panel.classList.toggle("active", panel.id === panelId));
    elements.menuItems.forEach((item) => item.classList.toggle("active", item.dataset.panel === panelId));
    elements.pageTitle.textContent = panelMeta[panelId]?.title || "AI getaway controller";
    elements.pageSubtitle.textContent = panelMeta[panelId]?.subtitle || "";
}

function updateAccountHeader() {
    const user = state.me || {};
    const detail = state.users.find((item) => item.id === user.userId) || state.users[0] || {};
    const displayName = detail.nickname || user.nickname || user.username || "zxw";
    elements.currentUser.textContent = displayName;
    elements.currentUserEmail.textContent = detail.email || roleLabel(user.roleCode || "USER");
    elements.currentBalance.textContent = `余额: ${formatMoney(detail.balance ?? user.balance)}`;
    elements.accountAvatar.textContent = (displayName || "Z").slice(0, 1).toUpperCase();
}

function populateKeyGroupOptions() {
    if (!elements.keyForm) {
        return;
    }
    const selectableGroups = getPurchasedPackages().filter((item) => isPackageUsable(item));
    const currentValue = elements.keyForm.querySelector('input[name="modelPackageId"]:checked')?.value || "";
    let selectedValue = currentValue;

    if (!elements.keyGroupOptions) {
        return;
    }

    if (!selectableGroups.length) {
        elements.keyGroupHint.textContent = "当前账号还没有已购买且生效中的套餐，请先到套餐中心购买。";
        elements.keyGroupOptions.innerHTML = `
            <div class="key-group-empty">
                <div class="empty-state">暂无可用分组，购买套餐后才可以创建 API Key。</div>
                <button type="button" class="secondary-button" data-open-panel="models-panel">前往套餐中心</button>
            </div>
        `;
        return;
    }

    if (!selectedValue) {
        const selectedGroupPackage = state.selectedPackageGroupId
            ? getLatestPurchasedPackageByGroup(state.selectedPackageGroupId)
            : null;
        selectedValue = selectedGroupPackage?.id ? String(selectedGroupPackage.id) : (state.selectedPackageId ? String(state.selectedPackageId) : "");
    }

    if (!selectedValue) {
        const latestActivePackage = selectableGroups
            .slice()
            .sort((a, b) => toNumber(b.id) - toNumber(a.id))[0];
        selectedValue = latestActivePackage?.id ? String(latestActivePackage.id) : "";
    }

    if (selectedValue && !selectableGroups.some((item) => String(item.id) === String(selectedValue))) {
        const latestActivePackage = selectableGroups
            .slice()
            .sort((a, b) => toNumber(b.id) - toNumber(a.id))[0];
        selectedValue = latestActivePackage?.id ? String(latestActivePackage.id) : "";
    }

    elements.keyGroupHint.textContent = "请选择一个已购买且未过期的具体套餐来创建 Key。";
    state.selectedPackageId = selectedValue || null;

    const options = selectableGroups.map((group) => `
        <label class="key-group-card ${String(selectedValue) === String(group.id) ? "selected" : ""}">
            <input type="radio" name="modelPackageId" value="${escapeHtml(group.id)}" ${String(selectedValue) === String(group.id) ? "checked" : ""}>
            <span class="key-group-card-check"></span>
            <span class="key-group-card-main">
                <strong>${escapeHtml(buildPurchasedPackageLabel(group))}</strong>
                <small>${escapeHtml(group.remark || `${group.modelCount || 0} 个模型 · ${group.packageDays || 30} 天有效期`)}</small>
            </span>
            <span class="key-group-card-pill">${escapeHtml(formatMoney(group.purchasePrice || 0))}</span>
        </label>
    `).join("");

    elements.keyGroupOptions.innerHTML = options;
}

function renderModelAccessSummary() {
    const summary = state.accessSummary || {};
    const groups = getAccessGroups();
    const purchasedPackages = getPurchasedPackages();
    const selectedGroup = getSelectedPackageGroup();
    const selectedPackage = getSelectedPurchasedPackage();
    const detail = state.users.find((item) => item.id === state.me?.userId) || state.users[0] || {};

    if (!elements.modelPackageSummary || !elements.packageCardGrid) {
        return;
    }

    if (elements.packageBalancePill) {
        elements.packageBalancePill.textContent = `当前余额: ${formatMoney(detail.balance ?? state.me?.balance ?? 0)}`;
    }

    elements.packageCardGrid.innerHTML = groups.length
        ? groups.map((group, index) => {
            const isSelected = String(selectedPackage?.groupId || selectedGroup?.id) === String(group.id);
            return `
            <article class="package-plan-card ${group.active ? "active" : ""} ${isSelected ? "selected" : ""}" data-select-package="${escapeHtml(group.id)}">
                <div class="package-plan-badge-row">
                    <span class="package-plan-badge">${index === 0 ? "推荐" : index === 1 ? "热门" : "分组"}</span>
                    ${group.active ? '<span class="status-chip active">使用中</span>' : group.purchased ? '<span class="status-chip disabled">已购买</span>' : '<span class="soft-badge">未购买</span>'}
                </div>
                <div class="package-plan-name">${escapeHtml(group.groupName)}</div>
                <div class="package-plan-price">${escapeHtml(formatMoney(group.salePrice || 0))}<small> / ${escapeHtml(group.packageDays || 30)}天</small></div>
                <ul class="package-plan-features">
                    <li>每日额度 ${escapeHtml(formatMoney(group.dailyQuota || 0))}</li>
                    <li>有效期 ${escapeHtml(group.packageDays || 30)} 天</li>
                    <li>${escapeHtml(group.modelCount || 0)} 个模型可用</li>
                    <li>${escapeHtml(group.packageStatusText || (group.purchased ? "已购买" : "未购买"))}</li>
                    <li>${escapeHtml(group.remark || "支持对应分组下的全部模型调用")}</li>
                </ul>
                <div class="package-plan-actions">
                    <button type="button" class="secondary-button wide-button" data-show-package-models="${escapeHtml(group.id)}">查看详情</button>
                    <button
                        type="button"
                        class="primary-button wide-button package-buy-button"
                        data-purchase-group="${escapeHtml(group.id)}"
                    >
                        ${group.purchased ? "再次购买" : "立即购买"}
                    </button>
                    ${isAdmin() ? `<button type="button" class="mini-button danger-button package-delete-button" data-delete-group="${escapeHtml(group.id)}">删除套餐</button>` : ""}
                </div>
            </article>
        `;
        }).join("")
        : '<div class="empty-state">当前还没有可购买的套餐分组。</div>';

    if (purchasedPackages.length) {
        elements.modelPackageSummary.innerHTML = purchasedPackages.map((item) => `
            <article class="package-summary-card">
                <div class="package-summary-top">
                    <div>
                        <div class="package-summary-name">${escapeHtml(buildPurchasedPackageLabel(item))}</div>
                        <div class="card-caption">${escapeHtml(item.active ? "可用中" : (item.status || "已购买"))}</div>
                    </div>
                    ${accessStatusBadge(item.active ? "ACTIVE" : (item.status || "EXPIRED"))}
                </div>
                <div class="package-summary-meta">
                    <span>到期 ${escapeHtml(formatDateTime(item.expiresAt))}</span>
                    <span>今日 ${escapeHtml(formatQuotaText(item.dailyUsed, item.dailyQuota))}</span>
                    <span>本周 ${escapeHtml(formatQuotaText(item.weeklyUsed, item.weeklyQuota))}</span>
                    <span>总额 ${escapeHtml(formatQuotaText(item.totalUsed, getPackageTotalQuota(item)))}</span>
                </div>
                <div class="package-plan-actions">
                    <button class="mini-button" type="button" data-show-package-models="${escapeHtml(item.groupId)}">详情</button>
                    <button class="mini-button danger-button" type="button" data-delete-package-purchase="${escapeHtml(item.id)}" data-package-name="${escapeHtml(buildPurchasedPackageLabel(item))}">删除套餐</button>
                </div>
            </article>
        `).join("");
    } else {
        elements.modelPackageSummary.innerHTML = `
            <article class="package-summary-card">
                <div class="package-summary-top">
                    <div>
                        <div class="package-summary-name">还没有已购套餐</div>
                        <div class="card-caption">购买一个分组套餐后，你才能在对应分组下创建 API Key。</div>
                    </div>
                    ${accessStatusBadge(summary.packageStatus)}
                </div>
            </article>
        `;
    }

    if (elements.dashboardPackageSelect) {
        elements.dashboardPackageSelect.value = selectedPackage ? String(selectedPackage.id) : "";
    }
    populateKeyGroupOptions();
    populatePackageGroupSelects();
}

function renderAllSections() {
    updateAccountHeader();
    renderOverviewCards();
    renderProfileCard();
    renderQuotaCard();
    renderModelAccessSummary();
    renderTrend();
    renderUsersTable();
    renderKeysTable();
    renderProvidersTable();
    populateImportProviderOptions();
    populatePackageGroupSelects();
    renderModelProviderFilters();
    renderModelCards();
    renderModelsTable();
    renderPackagePurchaseRecords();
    renderWalletTransactions();
    renderLogsTable();
    renderBillingModelStats();
}

function renderOverviewCards() {
    const overview = state.overview || {};
    const displayName = getCurrentDisplayName();
    const detail = state.users.find((item) => item.id === state.me?.userId) || state.users[0] || {};
    const activeKeys = state.keys.filter((item) => String(item.status || "").toUpperCase() === "ACTIVE").length;
    const todayPackageSpend = sumPurchasedPackageField("dailyUsed");
    const weekPackageSpend = sumPurchasedPackageField("weeklyUsed");
    const todaySpend = todayPackageSpend > 0 ? todayPackageSpend : toNumber(overview.consumeAmountToday);
    const weekSpend = weekPackageSpend > 0 ? weekPackageSpend : getLast7DaysSpend();
    const balance = toNumber(detail.balance ?? overview.walletBalanceTotal);
    const dailyQuotaTotal = sumPurchasedPackageField("dailyQuota");
    const limitLabel = dailyQuotaTotal > 0 ? formatMoney(dailyQuotaTotal) : "未开通";

    if (elements.dashboardGreeting) {
        elements.dashboardGreeting.textContent = isAdmin() ? `你好，${displayName} 管理员` : `你好，${displayName}`;
    }
    if (elements.dashboardGreetingCopy) {
        elements.dashboardGreetingCopy.textContent = isAdmin()
            ? "欢迎回来，以下是平台今天的 API 运行概况。"
            : "欢迎回来，以下是您今日的 API 使用概况。";
    }

    if (!elements.overviewCards) {
        return;
    }

    const cards = isAdmin()
        ? [
            {
                tone: "blue soft",
                icon: "余",
                value: formatMoney(balance),
                label: "平台余额",
                subvalue: "用户钱包余额汇总",
                action: "查看账户",
                panel: "users-panel"
            },
            {
                tone: "orange warm",
                icon: "今",
                value: formatMoney(todaySpend),
                label: "今日消费",
                subvalue: `全部套餐日额度 ${limitLabel}`,
                action: "查看套餐",
                panel: "models-panel"
            },
            {
                tone: "orange warm",
                icon: "周",
                value: formatMoney(weekSpend),
                label: "本周消费",
                subvalue: "按全部套餐统计",
                action: "用量详情",
                panel: "billing-panel"
            },
            {
                tone: "cream",
                icon: "钥",
                value: `${activeKeys}/${state.keys.length || 0}`,
                label: "API Keys",
                subvalue: "当前账号已创建的密钥数量",
                action: "管理 Key",
                panel: "keys-panel"
            }
        ]
        : [
            {
                tone: "blue soft",
                icon: "余",
                value: formatMoney(balance),
                label: "账户余额",
                subvalue: "余额用于购买套餐，不参与套餐内调用扣费",
                action: "去买套餐",
                panel: "models-panel"
            },
            {
                tone: "orange warm",
                icon: "今",
                value: formatMoney(todaySpend),
                label: "今日消费",
                subvalue: `全部套餐日额度 ${limitLabel}`,
                action: "查看套餐",
                panel: "models-panel"
            },
            {
                tone: "orange warm",
                icon: "周",
                value: formatMoney(weekSpend),
                label: "本周消费",
                subvalue: "按全部套餐统计",
                action: "用量详情",
                panel: "billing-panel"
            },
            {
                tone: "cream",
                icon: "钥",
                value: `${activeKeys}/${state.keys.length || 0}`,
                label: "API Keys",
                subvalue: "创建前请先购买套餐并选择对应分组",
                action: "管理 Key",
                panel: "keys-panel"
            }
        ];

    elements.overviewCards.innerHTML = cards.map((card) => `
        <article class="metric-card metric-card-home metric-${escapeHtml(card.tone)}">
            <div class="metric-card-topline">
                <div class="metric-icon">${escapeHtml(card.icon)}</div>
            </div>
            <div class="metric-value">${escapeHtml(card.value)}</div>
            <div class="metric-label">${escapeHtml(card.label)}</div>
            <div class="metric-subvalue">${escapeHtml(card.subvalue)}</div>
            <button class="metric-link-button" type="button" data-open-panel="${escapeHtml(card.panel)}">${escapeHtml(card.action)}</button>
        </article>
    `).join("");
}

function renderProfileCard() {
    if (!elements.profileCardBody) {
        return;
    }
    const detail = state.users.find((item) => item.id === state.me?.userId) || state.users[0] || {};
    const selectedPackage = getSelectedPurchasedPackage();
    const displayName = detail.nickname || detail.username || state.me?.nickname || state.me?.username || "开发者";
    const email = detail.email || "未设置邮箱";
    const lastLogin = detail.lastLoginAt ? relativeTimeFromNow(detail.lastLoginAt) : "尚未登录";
    const createdAt = detail.createdAt ? formatDateTime(detail.createdAt) : "-";
    const packageLabel = selectedPackage ? buildPurchasedPackageLabel(selectedPackage) : "未购买套餐";

    elements.profileCardBody.innerHTML = `
        <div class="dashboard-user-top">
            <div class="dashboard-user-avatar">${escapeHtml((displayName || "Z").slice(0, 1).toUpperCase())}</div>
            <div>
                <div class="dashboard-user-name">${escapeHtml(displayName)}</div>
                <div class="dashboard-user-email">${escapeHtml(email)}</div>
                <div class="chip-row">
                    ${statusChip(detail.status || "ACTIVE")}
                    <span class="soft-badge">${escapeHtml(roleLabel(detail.roleCode || state.me?.roleCode || "USER"))}</span>
                </div>
            </div>
        </div>
        <div class="dashboard-user-grid">
            <div class="meta-block">
                <span>计费优先级</span>
                <strong>套餐扣费</strong>
            </div>
            <div class="meta-block">
                <span>当前套餐</span>
                <strong>${escapeHtml(packageLabel)}</strong>
            </div>
            <div class="meta-block">
                <span>注册时间</span>
                <strong>${escapeHtml(createdAt)}</strong>
            </div>
            <div class="meta-block">
                <span>最后登录</span>
                <strong>${escapeHtml(lastLogin)}</strong>
            </div>
            <div class="meta-block">
                <span>钱包余额</span>
                <strong>${escapeHtml(formatMoney(detail.balance ?? state.me?.balance ?? 0))}</strong>
            </div>
            <div class="meta-block">
                <span>接入地址</span>
                <strong>${escapeHtml(`${window.location.origin}/v1`)}</strong>
            </div>
        </div>
    `;
}

function renderQuotaCard() {
    const summary = state.accessSummary || {};
    const purchasedPackages = getPurchasedPackages();
    const selectedPackage = getSelectedPurchasedPackage();
    if (!elements.quotaCardBody || !elements.dashboardPackageSelect) {
        return;
    }

    elements.quotaCardTitle.textContent = selectedPackage ? buildPurchasedPackageLabel(selectedPackage) : "套餐额度";
    elements.quotaStatusBadge.innerHTML = accessStatusBadge(
        selectedPackage ? (isPackageUsable(selectedPackage) ? "ACTIVE" : "EXPIRED") : summary.packageStatus
    );
    elements.dashboardPackageSelect.innerHTML = purchasedPackages.length
        ? purchasedPackages.map((item) => `
            <option value="${escapeHtml(item.id)}" ${String(item.id) === String(selectedPackage?.id) ? "selected" : ""}>
                ${escapeHtml(buildPurchasedPackageLabel(item))}${item.active ? "（可用）" : ""}
            </option>
        `).join("")
        : '<option value="">未购买套餐</option>';

    if (!purchasedPackages.length) {
        elements.quotaCardBody.innerHTML = `
            <div class="dashboard-package-empty">
                <div class="quota-plan">还没有已购套餐</div>
                <div class="quota-plan-subtitle">先去套餐中心购买一个分组套餐，之后才能在对应分组下创建 API Key。</div>
                <button type="button" class="primary-button" data-open-panel="models-panel">前往套餐中心</button>
            </div>
        `;
        return;
    }

    const dailyQuota = toNumber(selectedPackage?.dailyQuota);
    const weeklyQuota = toNumber(selectedPackage?.weeklyQuota);
    const monthlyQuota = toNumber(selectedPackage?.monthlyQuota);
    const totalQuota = getPackageTotalQuota(selectedPackage);
    const dailyUsed = toNumber(selectedPackage?.dailyUsed);
    const weeklyUsed = toNumber(selectedPackage?.weeklyUsed);
    const monthlyUsed = toNumber(selectedPackage?.monthlyUsed);
    const totalUsed = toNumber(selectedPackage?.totalUsed);

    elements.quotaCardBody.innerHTML = `
        <div class="quota-headline">
            <div>
                <div class="quota-plan">${escapeHtml(selectedPackage ? buildPurchasedPackageLabel(selectedPackage) : "套餐")}</div>
                <div class="quota-plan-subtitle">
                    ${escapeHtml(selectedPackage?.active ? "可用中" : (selectedPackage?.status || summary.packageStatusText || "未购买套餐"))}
                    ${selectedPackage?.remainingDays != null ? ` · 剩余 ${escapeHtml(selectedPackage.remainingDays)} 天` : ""}
                </div>
            </div>
            <div class="quota-meta">
                <span>到期时间</span>
                <strong>${escapeHtml(formatDateTime(selectedPackage?.expiresAt))}</strong>
            </div>
        </div>
        <div class="quota-bars">
            <div>
                <div class="bar-label"><span>今日额度</span><strong>${escapeHtml(formatQuotaText(dailyUsed, dailyQuota))}</strong></div>
                <div class="progress-track"><div class="progress-fill" style="width:${percentage(dailyUsed, dailyQuota)}%"></div></div>
            </div>
            <div>
                <div class="bar-label"><span>每周额度</span><strong>${escapeHtml(formatQuotaText(weeklyUsed, weeklyQuota))}</strong></div>
                <div class="progress-track"><div class="progress-fill" style="width:${percentage(weeklyUsed, weeklyQuota)}%"></div></div>
            </div>
            <div>
                <div class="bar-label"><span>每月额度</span><strong>${escapeHtml(formatQuotaText(monthlyUsed, monthlyQuota))}</strong></div>
                <div class="progress-track"><div class="progress-fill" style="width:${percentage(monthlyUsed, monthlyQuota)}%"></div></div>
            </div>
            <div>
                <div class="bar-label"><span>总额度</span><strong>${escapeHtml(formatQuotaText(totalUsed, totalQuota))}</strong></div>
                <div class="progress-track"><div class="progress-fill" style="width:${percentage(totalUsed, totalQuota)}%"></div></div>
            </div>
        </div>
        <div class="quota-footer">
            <div class="quota-meta">
                <span>购买价格</span>
                <strong>${escapeHtml(formatMoney(selectedPackage?.purchasePrice || 0))}</strong>
            </div>
            <div class="quota-meta">
                <span>分组模型</span>
                <strong>${escapeHtml(selectedPackage?.modelCount || 0)} 个</strong>
            </div>
        </div>
    `;
}

function renderPackageSelectionViews() {
    renderOverviewCards();
    renderProfileCard();
    renderQuotaCard();
    refreshPackageAvailabilityUi();
    renderModelAccessSummary();
}

function refreshPackageAvailabilityUi() {
    if (!elements.dashboardPackageSelect) {
        return;
    }
    const purchasedPackages = getPurchasedPackages();
    const selectedPackage = getSelectedPurchasedPackage();
    const summary = state.accessSummary || {};
    elements.dashboardPackageSelect.innerHTML = purchasedPackages.length
        ? purchasedPackages.map((item) => `
            <option value="${escapeHtml(item.id)}" ${String(item.id) === String(selectedPackage?.id) ? "selected" : ""}>
                ${escapeHtml(`${item.groupName || item.groupCode || "套餐"} #${item.id}（${packageAvailabilityLabel(item)}）`)}
            </option>
        `).join("")
        : '<option value="">未购买套餐</option>';

    if (elements.quotaStatusBadge) {
        elements.quotaStatusBadge.innerHTML = accessStatusBadge(
            selectedPackage ? (isPackageUsable(selectedPackage) ? "ACTIVE" : "EXPIRED") : summary.packageStatus
        );
    }
}

function renderTrend() {
    renderOverviewTrendChart();
    renderBillingTrendChart();
    renderBillingModelUsage();
    renderBillingModelStats();
}

function resolveBillingModelMeta(modelCode, upstreamModels) {
    const current = state.models.find((item) => item.modelCode === modelCode);
    return {
        modelCode,
        modelName: current?.modelName || "",
        upstreamModels: current?.upstreamModel || upstreamModels || ""
    };
}

function renderBillingModelStats() {
    if (!elements.billingModelStatsTable) {
        return;
    }
    if (!state.modelStats.length) {
        elements.billingModelStatsTable.innerHTML = '<tr><td colspan="6" class="empty-state">本月还没有可统计的模型调用数据。</td></tr>';
        return;
    }
    elements.billingModelStatsTable.innerHTML = state.modelStats.map((item) => {
        const meta = resolveBillingModelMeta(item.modelCode, item.upstreamModels);
        const nameLine = meta.modelName && meta.modelName !== meta.modelCode
            ? `<span class="billing-model-name">${escapeHtml(meta.modelName)}</span>`
            : "";
        const upstreamLine = meta.upstreamModels
            ? `<span class="billing-model-upstream">上游: ${escapeHtml(meta.upstreamModels)}</span>`
            : "";
        return `
            <tr>
                <td>
                    <div class="billing-model-primary">
                        <span class="billing-model-code">${escapeHtml(meta.modelCode || "-")}</span>
                        ${nameLine}
                        ${upstreamLine}
                    </div>
                </td>
                <td>${escapeHtml(formatCompactNumber(item.requestCount || 0))}</td>
                <td>${escapeHtml(formatCompactNumber(item.totalTokens || 0))}</td>
                <td>${escapeHtml(formatLatency(item.avgLatencyMs || 0))}</td>
                <td>${escapeHtml(formatLatency(item.totalLatencyMs || 0))}</td>
                <td>${escapeHtml(formatPercent(item.successRate || 0))}</td>
            </tr>
        `;
    }).join("");
}

function formatCompactNumber(value) {
    const num = toNumber(value);
    if (Math.abs(num) >= 1000000) {
        return `${(num / 1000000).toFixed(num >= 10000000 ? 0 : 1)}M`;
    }
    if (Math.abs(num) >= 1000) {
        return `${(num / 1000).toFixed(num >= 10000 ? 0 : 1)}K`;
    }
    return Math.round(num).toLocaleString("en-US");
}

function formatPercent(value, digits = 1) {
    return `${toNumber(value).toFixed(digits)}%`;
}

function formatLatency(value) {
    return `${(toNumber(value) / 1000).toFixed(1)}s`;
}

function getCurrentDisplayName() {
    const user = state.me || {};
    const detail = state.users.find((item) => item.id === user.userId) || state.users[0] || {};
    return detail.nickname || user.nickname || user.username || "开发者";
}

function getSuccessRate(logs) {
    if (!logs?.length) {
        return 100;
    }
    const successCount = logs.filter((item) => item.success).length;
    return (successCount / logs.length) * 100;
}

function getAverageLatency(logs) {
    if (!logs?.length) {
        return 0;
    }
    return logs.reduce((sum, item) => sum + toNumber(item.latencyMs), 0) / logs.length;
}

function getAverageDailyRequests() {
    if (!state.trend.length) {
        return 0;
    }
    const history = state.trend.slice(0, -1);
    const list = history.length ? history : state.trend;
    return list.reduce((sum, item) => sum + toNumber(item.requestCount), 0) / Math.max(list.length, 1);
}

function getAverageDailyTokens() {
    if (!state.trend.length) {
        return 0;
    }
    const history = state.trend.slice(0, -1);
    const list = history.length ? history : state.trend;
    return list.reduce((sum, item) => sum + toNumber(item.totalTokens), 0) / Math.max(list.length, 1);
}

function getPercentDeltaText(current, baseline) {
    if (!baseline) {
        return "稳定";
    }
    const delta = ((toNumber(current) - toNumber(baseline)) / Math.max(Math.abs(toNumber(baseline)), 1)) * 100;
    const sign = delta >= 0 ? "+" : "";
    return `${sign}${delta.toFixed(1)}%`;
}

function getRequestDeltaText(current, baseline) {
    if (!baseline) {
        return "稳定";
    }
    const delta = ((toNumber(current) - toNumber(baseline)) / Math.max(Math.abs(toNumber(baseline)), 1)) * 100;
    const sign = delta >= 0 ? "+" : "";
    return `${sign}${delta.toFixed(1)}%`;
}

function isTodayValue(value) {
    if (!value) {
        return false;
    }
    const date = parseServerDate(value);
    if (!date) {
        return false;
    }
    const now = new Date();
    return date.getFullYear() === now.getFullYear()
        && date.getMonth() === now.getMonth()
        && date.getDate() === now.getDate();
}

function getLogsForToday() {
    return state.logs.filter((item) => isTodayValue(item.createdAt));
}

function getLogsForCurrentMonth() {
    const now = new Date();
    return state.logs.filter((item) => {
        const date = parseServerDate(item.createdAt);
        return !!date && date.getFullYear() === now.getFullYear() && date.getMonth() === now.getMonth();
    });
}

function relativeTimeFromNow(value) {
    if (!value) {
        return "刚刚";
    }
    const date = parseServerDate(value);
    if (!date) {
        return "鍒氬垰";
    }
    const diff = Date.now() - date.getTime();
    const minute = 60 * 1000;
    const hour = 60 * minute;
    if (diff < minute) {
        return "刚刚";
    }
    if (diff < hour) {
        return `${Math.max(1, Math.floor(diff / minute))}分钟前`;
    }
    return `${Math.max(1, Math.floor(diff / hour))}小时前`;
}

function createSmoothPath(points) {
    if (!points.length) {
        return "";
    }
    let path = `M ${points[0].x} ${points[0].y}`;
    for (let index = 0; index < points.length - 1; index += 1) {
        const current = points[index];
        const next = points[index + 1];
        const midX = (current.x + next.x) / 2;
        path += ` C ${midX} ${current.y}, ${midX} ${next.y}, ${next.x} ${next.y}`;
    }
    return path;
}

function renderLineChart(series, labels, options = {}) {
    if (!series.length || !labels.length) {
        return '<div class="empty-state">暂无趋势数据。</div>';
    }
    const width = options.width || 860;
    const height = options.height || 280;
    const left = 26;
    const right = 18;
    const top = 16;
    const bottom = 38;
    const plotWidth = width - left - right;
    const plotHeight = height - top - bottom;
    const values = series.flatMap((item) => item.values.map((value) => toNumber(value)));
    const maxValue = Math.max(...values, 1);
    const minValue = options.minZero === false ? Math.min(...values, maxValue) : 0;
    const range = Math.max(maxValue - minValue, 1);

    const gridLines = Array.from({ length: 5 }).map((_, index) => {
        const y = top + (plotHeight / 4) * index;
        return `<line x1="${left}" y1="${y}" x2="${width - right}" y2="${y}" class="chart-grid-line"></line>`;
    }).join("");

    const renderedSeries = series.map((item, seriesIndex) => {
        const points = item.values.map((value, valueIndex) => {
            const x = left + (plotWidth / Math.max(labels.length - 1, 1)) * valueIndex;
            const y = top + plotHeight - ((toNumber(value) - minValue) / range) * plotHeight;
            return { x, y, value: toNumber(value) };
        });
        const path = createSmoothPath(points);
        const circles = points.map((point) => `<circle cx="${point.x}" cy="${point.y}" r="${seriesIndex === 0 ? 4 : 3}" fill="${item.color}" class="chart-dot"></circle>`).join("");
        const areaPath = item.fill
            ? `${path} L ${points[points.length - 1].x} ${top + plotHeight} L ${points[0].x} ${top + plotHeight} Z`
            : "";
        return `
            ${areaPath ? `<path d="${areaPath}" fill="${item.fill}" class="chart-area"></path>` : ""}
            <path d="${path}" fill="none" stroke="${item.color}" stroke-width="${item.strokeWidth || 4}" stroke-linecap="round" stroke-linejoin="round"></path>
            ${circles}
        `;
    }).join("");

    const labelsHtml = labels.map((label) => `<span>${escapeHtml(label)}</span>`).join("");
    return `
        <div class="line-chart">
            <svg viewBox="0 0 ${width} ${height}" preserveAspectRatio="none" class="chart-svg">
                ${gridLines}
                ${renderedSeries}
            </svg>
            <div class="chart-x-labels">${labelsHtml}</div>
        </div>
    `;
}

function renderOverviewTrendChart() {
    if (!elements.trendChart || !elements.trendSummary) {
        return;
    }
    if (!state.trend.length) {
        elements.trendSummary.textContent = "暂无数据";
        elements.trendChart.innerHTML = '<div class="empty-state">最近 7 天还没有统计数据。</div>';
        return;
    }
    const labels = state.trend.map((item) => String(item.statDate || "").slice(5) || "-");
    const requestValues = state.trend.map((item) => toNumber(item.requestCount));
    const totalRequests = requestValues.reduce((sum, item) => sum + item, 0);
    elements.trendSummary.textContent = `近 7 天共 ${formatCompactNumber(totalRequests)} 次请求`;
    elements.trendChart.innerHTML = renderLineChart([
        {
            values: requestValues,
            color: "#5b63f6",
            fill: "rgba(91, 99, 246, 0.14)"
        }
    ], labels);
}

function renderBillingTrendChart() {
    if (!elements.billingTrendChart || !elements.billingTrendSummary) {
        return;
    }
    const days = 7;
    const labels = [];
    const values = [];
    for (let offset = days - 1; offset >= 0; offset -= 1) {
        const date = new Date();
        date.setDate(date.getDate() - offset);
        const month = `${date.getMonth() + 1}`;
        const day = `${date.getDate()}`;
        labels.push(`${month}/${day}`);
        const total = state.logs
            .filter((item) => {
                const itemDate = parseServerDate(item.createdAt);
                return !!itemDate && itemDate.getFullYear() === date.getFullYear()
                    && itemDate.getMonth() === date.getMonth()
                    && itemDate.getDate() === date.getDate();
            })
            .reduce((sum, item) => sum + toNumber(item.userAmount), 0);
        values.push(total);
    }
    const totalAmount = values.reduce((sum, item) => sum + item, 0);
    elements.billingTrendSummary.textContent = `近 7 天消耗 ${formatMoney(totalAmount)}`;
    elements.billingTrendChart.innerHTML = totalAmount > 0
        ? renderLineChart([{ values, color: "#5b63f6", fill: "rgba(91, 99, 246, 0.12)" }], labels)
        : '<div class="empty-state">最近 7 天还没有费用数据。</div>';
}

function renderBillingModelUsage() {
    if (!elements.billingModelUsage) {
        return;
    }
    const groups = new Map();
    getLogsForCurrentMonth().forEach((item) => {
        const key = item.modelCode || "未命名模型";
        const current = groups.get(key) || { model: key, amount: 0, tokens: 0, requests: 0 };
        current.amount += toNumber(item.userAmount);
        current.tokens += toNumber(item.totalTokens);
        current.requests += 1;
        groups.set(key, current);
    });
    const list = Array.from(groups.values()).sort((a, b) => b.amount - a.amount).slice(0, 6);
    const totalAmount = list.reduce((sum, item) => sum + item.amount, 0);
    if (!list.length) {
        elements.billingModelUsage.innerHTML = '<div class="empty-state">暂时没有可统计的模型消费数据。</div>';
        return;
    }
    elements.billingModelUsage.innerHTML = list.map((item) => {
        const percent = totalAmount ? (item.amount / totalAmount) * 100 : 0;
        return `
            <div class="model-usage-item">
                <div class="model-usage-row">
                    <strong>${escapeHtml(item.model)}</strong>
                    <span>${escapeHtml(formatMoney(item.amount))}</span>
                </div>
                <div class="model-usage-bar"><div class="model-usage-fill" style="width:${percent}%"></div></div>
                <div class="model-usage-meta">${escapeHtml(formatCompactNumber(item.tokens))} tokens · ${escapeHtml(formatCompactNumber(item.requests))} 次请求</div>
            </div>
        `;
    }).join("");
}

function renderUsersTable() {
    if (!state.users.length) {
        elements.usersTable.innerHTML = '<tr><td colspan="7" class="empty-state">暂无用户数据。</td></tr>';
        return;
    }
    elements.usersTable.innerHTML = state.users.map((user) => {
        const actions = [
            `<button class="mini-button" type="button" data-user-view="${user.id}">查看</button>`,
            `<button class="mini-button" type="button" data-user-edit="${user.id}">编辑</button>`
        ];
        if (isAdmin()) {
            const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
            actions.push(`<button class="mini-button" type="button" data-user-toggle="${user.id}" data-next-status="${nextStatus}">${user.status === "ACTIVE" ? "禁用" : "启用"}</button>`);
            actions.push(`<button class="mini-button danger-button" type="button" data-user-delete="${user.id}" data-username="${escapeHtml(user.username)}">删除</button>`);
        }
        return `
            <tr>
                <td>${escapeHtml(user.id)}</td>
                <td>${escapeHtml(user.username)}</td>
                <td>${escapeHtml(roleLabel(user.roleCode))}</td>
                <td>${statusChip(user.status)}</td>
                <td>${escapeHtml(formatMoney(user.balance))}</td>
                <td>${escapeHtml(user.email || "-")}</td>
                <td><div class="action-row">${actions.join("")}</div></td>
            </tr>
        `;
    }).join("");
}

function toggleUserDetailReadonly(readonly) {
    ["nickname", "email", "phone", "password", "roleCode", "status"].forEach((name) => {
        const field = elements.userDetailForm.elements[name];
        if (!field) {
            return;
        }
        if (name === "roleCode" || name === "status") {
            field.disabled = readonly || !isAdmin();
        } else {
            field.readOnly = readonly;
        }
    });
    elements.saveUserDetailButton.classList.toggle("hidden", readonly);
}

function openUserDetailModal(user, mode) {
    state.editingUserId = user.id;
    state.userDetailMode = mode;
    const form = elements.userDetailForm;
    form.elements.id.value = user.id || "";
    form.elements.username.value = user.username || "";
    form.elements.balance.value = formatMoney(user.balance);
    form.elements.nickname.value = user.nickname || "";
    form.elements.email.value = user.email || "";
    form.elements.phone.value = user.phone || "";
    form.elements.password.value = "";
    form.elements.roleCode.value = user.roleCode || "USER";
    form.elements.status.value = user.status || "ACTIVE";
    elements.userDetailTitle.textContent = mode === "edit" ? "编辑用户资料" : "查看用户资料";
    elements.userDetailCopy.textContent = mode === "edit"
        ? "修改后点击保存即可生效。普通用户只能修改自己的昵称、邮箱、手机号和密码。"
        : "这里展示当前用户的详细资料。";
    toggleUserDetailReadonly(mode !== "edit");
    elements.userDetailModal.classList.remove("hidden");
}

function closeUserDetailModal() {
    elements.userDetailModal.classList.add("hidden");
    elements.userDetailForm.reset();
    state.editingUserId = null;
    state.userDetailMode = "view";
}

function renderKeysTable() {
    elements.keysPanelSubtitle.textContent = `已创建 ${state.keys.length} 个接口密钥`;
    renderModelAccessSummary();
    elements.keysEmpty.classList.toggle("hidden", state.keys.length > 0);
    if (!state.keys.length) {
        elements.keysList.innerHTML = "";
        elements.keysTable.innerHTML = "";
        return;
    }
    elements.keysList.innerHTML = state.keys.map((key) => `
        <article class="key-card">
            <div class="key-card-head">
                <div>
                    <div class="key-card-name">${escapeHtml(key.name)}</div>
                    <div class="key-card-subtitle">${escapeHtml(isAdmin() ? `${key.username} / 用户 ID ${key.userId}` : "当前账号密钥")}</div>
                    <div class="key-card-prefix"><code>${escapeHtml(key.accessKey)}</code></div>
                </div>
                ${statusChip(key.status)}
            </div>
            <div class="key-card-grid">
                <div class="key-card-meta"><div class="key-card-meta-icon">T</div><div><div class="card-caption">创建时间</div><div class="key-card-meta-value">${escapeHtml(formatDateTime(key.createdAt))}</div></div></div>
                <div class="key-card-meta"><div class="key-card-meta-icon">E</div><div><div class="card-caption">过期时间</div><div class="key-card-meta-value">${escapeHtml(formatDateTime(key.expiresAt))}</div></div></div>
            </div>
            <div class="key-card-tags">
                ${key.modelPackageName ? `<span class="key-tag">套餐 ${escapeHtml(key.modelPackageName)} #${escapeHtml(key.modelPackageId)}</span>` : ""}
                ${key.modelGroupName ? `<span class="key-tag">分组 ${escapeHtml(key.modelGroupName)}</span>` : ""}
                <span class="key-tag">最近使用 ${escapeHtml(formatDateTime(key.lastUsedAt))}</span>
            </div>
            <div class="key-card-actions">
                <button class="mini-button" type="button" data-key-copy="${key.id}">复制</button>
                <button class="mini-button" type="button" data-key-toggle="${key.id}" data-next-status="${key.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${key.status === "ACTIVE" ? "禁用" : "启用"}</button>
                <button class="mini-button danger-button" type="button" data-key-delete="${key.id}" data-key-name="${escapeHtml(key.name)}" data-key-access="${escapeHtml(key.accessKey)}">删除</button>
            </div>
        </article>
    `).join("");
    elements.keysTable.innerHTML = state.keys.map((key) => `
        <tr>
            <td>${escapeHtml(key.id)}</td>
            <td>${escapeHtml(key.username)}</td>
            <td>${escapeHtml(key.name)}</td>
            <td><code>${escapeHtml(key.accessKey)}</code></td>
            <td>${statusChip(key.status)}</td>
            <td><button class="mini-button" type="button" data-key-toggle="${key.id}" data-next-status="${key.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${key.status === "ACTIVE" ? "禁用" : "启用"}</button></td>
        </tr>
    `).join("");
}

function renderProvidersTable() {
    if (!isAdmin()) {
        elements.providersTable.innerHTML = "";
        return;
    }
    if (!state.providers.length) {
        elements.providersTable.innerHTML = '<tr><td colspan="8" class="empty-state">暂无渠道配置。</td></tr>';
        return;
    }
    elements.providersTable.innerHTML = state.providers.map((provider) => `
        <tr>
            <td>${escapeHtml(provider.id)}</td>
            <td>${escapeHtml(provider.providerCode)}</td>
            <td>${escapeHtml(provider.providerName)}</td>
            <td>${escapeHtml(provider.baseUrl)}</td>
            <td>${escapeHtml(provider.providerType || "-")}</td>
            <td>${escapeHtml(provider.tokenCount)}</td>
            <td>${statusChip(provider.status)}</td>
            <td><button class="mini-button" type="button" data-provider-toggle="${provider.id}" data-next-status="${provider.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${provider.status === "ACTIVE" ? "禁用" : "启用"}</button></td>
        </tr>
    `).join("");
}

function normalizeModelProviderLabel(item) {
    const type = String(item?.providerType || "").toUpperCase();
    if (type === "ANTHROPIC") {
        return "Claude";
    }
    if (type === "OPENAI_COMPATIBLE") {
        return item?.providerName || "OpenAI 兼容";
    }
    return item?.providerName || item?.providerType || "未分组";
}

function renderModelProviderFilters() {
    const groups = getAccessGroups();
    const validFilters = new Set(["ALL", "ALL_GROUPS", ...groups.map((group) => `GROUP:${group.id}`)]);
    if (!validFilters.has(state.modelProviderFilter)) {
        state.modelProviderFilter = "ALL_GROUPS";
    }
    const filterButtons = ['<button type="button" class="filter-pill' + (state.modelProviderFilter === "ALL_GROUPS" || state.modelProviderFilter === "ALL" ? ' active' : '') + '" data-provider-filter="ALL_GROUPS">全部</button>']
        .concat(groups.map((group) => {
            const code = `GROUP:${group.id}`;
            return `<button type="button" class="filter-pill${state.modelProviderFilter === code ? " active" : ""}" data-provider-filter="${escapeHtml(code)}">${escapeHtml(group.groupName || group.groupCode)}</button>`;
        }));
    elements.modelProviderFilters.innerHTML = filterButtons.join("");
}

function showPackageModelDetails(groupId) {
    const groups = getAccessGroups();
    const targetGroup = groups.find((item) => String(item.id) === String(groupId));
    state.modelProviderFilter = `GROUP:${groupId}`;

    const groupModels = state.models.filter((item) => String(item.groupId) === String(groupId));
    state.selectedModelId = groupModels.length ? getModelRowKey(groupModels[0]) : null;

    renderPackageSelectionViews();
    renderModelProviderFilters();
    renderModelCards();

    document.querySelector(".package-model-head")?.scrollIntoView({ behavior: "smooth", block: "start" });
    showToast(groupModels.length
        ? `${targetGroup?.groupName || "套餐"} 已展示 ${groupModels.length} 个模型`
        : `${targetGroup?.groupName || "套餐"} 暂无可展示模型`);
}

function inferModelBadges(model) {
    const tags = [];
    const code = String(model.modelCode || "").toLowerCase();
    if (code.includes("mini") || code.includes("flash") || code.includes("haiku")) {
        tags.push("快速");
    }
    if (code.includes("gpt") || code.includes("claude") || code.includes("qwen") || code.includes("deepseek")) {
        tags.push("对话");
    }
    if (code.includes("embed")) {
        tags.push("嵌入");
    }
    if (code.includes("vision") || code.includes("vl")) {
        tags.push("多模态");
    }
    if (!tags.length) {
        tags.push(model.modelType === "IMAGE" ? "图片" : "通用");
    }
    return tags.slice(0, 3);
}

function inferGatewayRouteMode(model) {
    const code = String(model?.modelCode || "").toLowerCase();
    const upstream = String(model?.upstreamModel || "").toLowerCase();
    const providerType = String(model?.providerType || "").toUpperCase();
    if (providerType === "ANTHROPIC") {
        return "原生 Messages";
    }
    if (isAgentCapableModel(code) || isAgentCapableModel(upstream)) {
        return "原生 Responses";
    }
    return "兼容转发";
}

function inferIdeCapability(model) {
    const code = String(model?.modelCode || "").toLowerCase();
    const upstream = String(model?.upstreamModel || "").toLowerCase();
    const providerType = String(model?.providerType || "").toUpperCase();
    if (providerType === "ANTHROPIC") {
        return "对话为主";
    }
    if (isAgentCapableModel(code) || isAgentCapableModel(upstream)) {
        return "完整代理";
    }
    return "工具受限";
}

function isAgentCapableModel(value) {
    const normalized = String(value || "").toLowerCase();
    return normalized.includes("codex") || normalized.includes("gpt-5");
}

function buildIdeClientHint(model) {
    const capability = inferIdeCapability(model);
    if (capability === "完整代理") {
        return "客户端若支持 Codex/Responses 工具流，可持续分析、调工具、改代码直到完成。";
    }
    if (capability === "对话为主") {
        return "更适合问答与内容生成，通常不承担持续改码代理。";
    }
    return "可用于普通问答或轻量工具调用，但不建议期待完整的自驱改码闭环。";
}

function buildModelSummary(model) {
    const provider = normalizeModelProviderLabel(model);
    const tags = inferModelBadges(model);
    return `${provider} ${tags.join(" / ")} 模型，适合 ${model.modelType === "IMAGE" ? "图像生成" : "文本与智能体"} 场景。`;
}

function renderSelectedModelDetail(model) {
    if (!elements.modelDetailTitle || !elements.modelDetailPricing || !elements.modelDetailCode) {
        return;
    }
    if (!model) {
        elements.modelDetailTitle.textContent = "暂未选择模型";
        elements.modelDetailDesc.textContent = "点击左侧任一模型卡片，这里会显示模型详情与价格。";
        elements.modelDetailSpecs.innerHTML = "";
        elements.modelDetailPricing.innerHTML = "";
        elements.modelDetailCode.textContent = "-";
        if (elements.modelDetailBadge) {
            elements.modelDetailBadge.textContent = "模型";
        }
        return;
    }
    const provider = normalizeModelProviderLabel(model);
    const routeMode = inferGatewayRouteMode(model);
    const ideCapability = inferIdeCapability(model);
    if (elements.modelDetailBadge) {
        elements.modelDetailBadge.textContent = provider;
    }
    elements.modelDetailTitle.textContent = model.modelName || model.modelCode;
    elements.modelDetailDesc.textContent = `${buildModelSummary(model)} 当前网关路由为 ${routeMode}，IDE 能力等级为 ${ideCapability}。`;
    elements.modelDetailSpecs.innerHTML = `
        <div class="detail-spec-row"><span>模型编码</span><strong>${escapeHtml(model.modelCode)}</strong></div>
        <div class="detail-spec-row"><span>所属套餐</span><strong>${escapeHtml(model.groupName || "-")}</strong></div>
        <div class="detail-spec-row"><span>模型类型</span><strong>${escapeHtml(model.modelType || "CHAT")}</strong></div>
        <div class="detail-spec-row"><span>上游模型</span><strong>${escapeHtml(model.upstreamModel || "-")}</strong></div>
        <div class="detail-spec-row"><span>网关路由</span><strong>${escapeHtml(routeMode)}</strong></div>
        <div class="detail-spec-row"><span>IDE 能力</span><strong>${escapeHtml(ideCapability)}</strong></div>
        <div class="detail-spec-row"><span>当前状态</span><strong>${escapeHtml(statusLabel(model.status))}</strong></div>
    `;
    elements.modelDetailPricing.innerHTML = `
        <div class="detail-price-box">
            <span>输入 Token</span>
            <strong>$${escapeHtml(formatModelRate(model.promptPrice))} / 1M</strong>
        </div>
        <div class="detail-price-box">
            <span>缓存读取 Token</span>
            <strong>$${escapeHtml(formatModelRate(model.cachedPromptPrice))} / 1M</strong>
        </div>
        <div class="detail-price-box">
            <span>输出 Token</span>
            <strong>$${escapeHtml(formatModelRate(model.completionPrice))} / 1M</strong>
        </div>
        ${isAdmin() ? `
        <div class="detail-price-box">
            <span>&#21333;&#27425;&#26368;&#20302;&#25187;&#36153;</span>
            <strong>$${escapeHtml(formatModelRate(model.requestPrice || 0.07))}</strong>
        </div>
        ` : ""}
        <div class="detail-price-box">
            <span>倍率</span>
            <strong>${escapeHtml(formatDecimal(model.multiplier, 2))}x</strong>
        </div>
        <div class="detail-price-box detail-price-box-wide">
            <span>IDE 提示</span>
            <strong>${escapeHtml(buildIdeClientHint(model))}</strong>
        </div>
    `;
    elements.modelDetailCode.textContent = model.modelCode;
}

function renderModelCards() {
    const selectedGroup = getModelPreviewGroup();
    const showAllGroups = state.modelProviderFilter === "ALL" || state.modelProviderFilter === "ALL_GROUPS";
    const list = state.models.filter((item) => {
        const matchesGroup = showAllGroups || !selectedGroup || String(item.groupId) === String(selectedGroup.id);
        return matchesGroup;
    });
    elements.modelsEmpty.classList.toggle("hidden", list.length > 0);
    if (!list.length) {
        elements.modelsCardGrid.innerHTML = "";
        renderSelectedModelDetail(null);
        if (elements.modelsMarketMeta) {
            elements.modelsMarketMeta.textContent = selectedGroup ? `${selectedGroup.groupName} 暂无可用模型` : "0 个模型可用";
        }
        return;
    }
    if (!state.selectedModelId || !list.some((item) => getModelRowKey(item) === String(state.selectedModelId))) {
        state.selectedModelId = getModelRowKey(list[0]);
    }
    if (elements.modelsMarketMeta) {
        elements.modelsMarketMeta.textContent = selectedGroup
            ? `${selectedGroup.groupName} 可用 ${list.length} 个模型`
            : `${list.length} 个模型可用`;
    }
    elements.modelsCardGrid.innerHTML = list.map((model) => {
        const providerLabel = normalizeModelProviderLabel(model);
        const badges = inferModelBadges(model);
        const routeMode = inferGatewayRouteMode(model);
        const ideCapability = inferIdeCapability(model);
        const modelKey = getModelRowKey(model);
        const selected = modelKey === String(state.selectedModelId);
        return `
            <article class="model-price-card plaza-model-card ${selected ? "active" : ""}" data-model-select="${escapeHtml(modelKey)}">
                <div class="model-price-head">
                    <div class="model-brand-icon">${escapeHtml(providerLabel.slice(0, 1).toUpperCase())}</div>
                    <div>
                        <div class="model-badge-row">
                            <span class="provider-pill">${escapeHtml(providerLabel)}</span>
                            <span class="model-tag-pill route-pill">${escapeHtml(routeMode)}</span>
                            <span class="model-tag-pill capability-pill">${escapeHtml(ideCapability)}</span>
                            ${badges.map((badge) => `<span class="model-tag-pill">${escapeHtml(badge)}</span>`).join("")}
                        </div>
                        <div class="model-title">${escapeHtml(model.modelName || model.modelCode)}</div>
                        <div class="model-provider-line">${escapeHtml(buildModelSummary(model))}</div>
                    </div>
                </div>
                <div class="model-price-grid">
                    <div class="model-price-box"><span>输入</span><strong>$${escapeHtml(formatModelRate(model.promptPrice))}/M</strong></div>
                    <div class="model-price-box"><span>缓存读取</span><strong>$${escapeHtml(formatModelRate(model.cachedPromptPrice))}/M</strong></div>
                    <div class="model-price-box"><span>输出</span><strong>$${escapeHtml(formatModelRate(model.completionPrice))}/M</strong></div>
                    ${isAdmin() ? `<div class="model-price-box"><span>&#26368;&#20302;&#25187;&#36153;</span><strong>$${escapeHtml(formatModelRate(model.requestPrice || 0.07))}</strong></div>` : ""}
                </div>
                <div class="model-price-foot">
                    <span>${escapeHtml(model.groupName || "未分组")} · 倍率 ${escapeHtml(formatDecimal(model.multiplier, 2))}x</span>
                    ${statusChip(model.status)}
                </div>
                ${isAdmin() ? `
                    <div class="model-card-actions">
                        <button class="mini-button" type="button" data-model-edit="${model.id}" data-model-binding="${escapeHtml(model.bindingId || "")}">编辑</button>
                        <button class="mini-button danger-button" type="button" data-model-delete="${model.id}" data-model-code="${escapeHtml(model.modelCode)}">删除</button>
                    </div>
                ` : ""}
            </article>
        `;
    }).join("");
    renderSelectedModelDetail(list.find((item) => getModelRowKey(item) === String(state.selectedModelId)) || list[0]);
}

function renderModelsTable() {
    if (!isAdmin()) {
        elements.modelsTable.innerHTML = "";
        return;
    }
    if (!state.models.length) {
        elements.modelsTable.innerHTML = '<tr><td colspan="13" class="empty-state">暂无模型配置。</td></tr>';
        return;
    }
    elements.modelsTable.innerHTML = state.models.map((model) => `
        <tr>
            <td>${escapeHtml(model.id)}</td>
            <td>${escapeHtml(model.modelCode)}</td>
            <td>${escapeHtml(model.groupName || "-")}</td>
            <td>${escapeHtml(formatModelRate(model.promptPrice))}</td>
            <td>${escapeHtml(formatModelRate(model.cachedPromptPrice))}</td>
            <span>&#21333;&#27425;&#26368;&#20302;&#25187;&#36153;</span>
            <td>${escapeHtml(formatModelRate(model.requestPrice || 0.07))}</td>
            <td>${escapeHtml(formatDecimal(model.multiplier, 4))}</td>
            <td>${escapeHtml(model.modelType || "-")}</td>
            <td>${escapeHtml(model.providerName || "-")}</td>
            <td>${escapeHtml(model.upstreamModel || "-")}</td>
            <td>${statusChip(model.status)}</td>
            <td class="action-row">
                <button class="mini-button" type="button" data-model-edit="${model.id}" data-model-binding="${escapeHtml(model.bindingId || "")}">编辑价格</button>
                <button class="mini-button" type="button" data-model-toggle="${model.id}" data-next-status="${model.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${model.status === "ACTIVE" ? "禁用" : "启用"}</button>
                <button class="mini-button danger-button" type="button" data-model-delete="${model.id}" data-model-code="${escapeHtml(model.modelCode)}">删除</button>
            </td>
        </tr>
    `).join("");
}

function renderPackagePurchaseRecords() {
    if (!elements.packagePurchasesTable) {
        return;
    }
    if (!state.packagePurchaseRecords.length) {
        elements.packagePurchasesTable.innerHTML = '<tr><td colspan="8" class="empty-state">暂无套餐购买记录。</td></tr>';
        return;
    }
    elements.packagePurchasesTable.innerHTML = state.packagePurchaseRecords.map((item) => `
        <tr>
            <td>${escapeHtml(item.username || "-")}</td>
            <td>${escapeHtml(item.groupName || item.groupCode || "-")}</td>
            <td>${escapeHtml(formatMoney(item.purchasePrice || 0))}</td>
            <td>${escapeHtml(formatDateTime(item.startAt))}</td>
            <td>${escapeHtml(formatDateTime(item.expiresAt))}</td>
            <td>${statusChip(item.status || "-")}</td>
            <td>${escapeHtml(formatDateTime(item.createdAt))}</td>
            <td class="action-row">
                <button class="mini-button" type="button" data-show-package-models="${escapeHtml(item.groupId)}">详情</button>
                <button class="mini-button danger-button" type="button" data-delete-package-purchase="${escapeHtml(item.id)}" data-package-name="${escapeHtml(buildPurchasedPackageLabel(item))}">删除</button>
            </td>
        </tr>
    `).join("");
}

function renderWalletTransactions() {
    if (!elements.walletTransactionsTable) {
        return;
    }
    if (!state.walletTransactions.length) {
        elements.walletTransactionsTable.innerHTML = '<tr><td colspan="8" class="empty-state">暂时还没有余额流水。</td></tr>';
        return;
    }
    elements.walletTransactionsTable.innerHTML = state.walletTransactions.map((item) => `
        <tr>
            <td>${escapeHtml(item.username || "-")}</td>
            <td>${escapeHtml(item.transactionType || "-")}</td>
            <td>${escapeHtml(item.direction || "-")}</td>
            <td>${escapeHtml(formatMoney(item.amount || 0))}</td>
            <td>${escapeHtml(formatMoney(item.balanceBefore || 0))}</td>
            <td>${escapeHtml(formatMoney(item.balanceAfter || 0))}</td>
            <td>${escapeHtml(item.descriptionText || "-")}</td>
            <td>${escapeHtml(formatDateTime(item.createdAt))}</td>
        </tr>
    `).join("");
}

function populateImportProviderOptions() {
    if (!isAdmin()) {
        elements.importProviderSelect.innerHTML = '<option value="">请先登录管理员</option>';
        return;
    }
    const activeProviders = state.providers.filter((item) => item.status === "ACTIVE");
    elements.importProviderSelect.innerHTML = ['<option value="">请选择渠道</option>']
        .concat(activeProviders.map((item) => `<option value="${item.id}">${escapeHtml(item.providerName)} 路 ${escapeHtml(item.providerType || "-")}</option>`))
        .join("");
}

function renderUpstreamModels() {
    if (!state.upstreamModels.length) {
        elements.upstreamModelsBox.innerHTML = '<div class="empty-state">先选择渠道，再点击“读取上游模型”。</div>';
        return;
    }
    elements.upstreamModelsBox.innerHTML = `
        <div class="upstream-models-grid">
            ${state.upstreamModels.map((item) => `
                <label class="upstream-model-option">
                    <input type="checkbox" data-upstream-model="${escapeHtml(item.id)}" ${item.selected ? "checked" : ""}>
                    <span>
                        <span class="upstream-model-name">${escapeHtml(item.displayName || item.id)}</span>
                        <span class="upstream-model-meta">${escapeHtml(item.id)} 路 ${escapeHtml(item.ownedBy || item.providerType || "-")}</span>
                    </span>
                </label>
            `).join("")}
        </div>
    `;
}

function renderLogsTable() {
    if (!state.logs.length) {
        elements.logsTable.innerHTML = '<tr><td colspan="10" class="empty-state">暂无请求日志。</td></tr>';
        return;
    }
    elements.logsTable.innerHTML = state.logs.map((log) => `
        <tr>
            <td>${escapeHtml(log.username || "-")}</td>
            <td>${escapeHtml(log.modelCode || "-")}</td>
            <td>${escapeHtml(log.packageName || "-")}</td>
            <td>${escapeHtml(`${formatDecimal(log.multiplier || 1, 1)}x`)}</td>
            <td>${escapeHtml(formatLatency(log.latencyMs || 0))}</td>
            <td>${escapeHtml(`${formatCompactNumber(log.promptTokens || 0)} / ${formatCompactNumber(log.completionTokens || 0)}`)}</td>
            <td>${escapeHtml(formatCompactNumber(log.cachedPromptTokens || 0))}</td>
            <td>${escapeHtml(formatMoney(log.userAmount || 0))}</td>
            <td>${statusChip(log.success ? "SUCCESS" : `HTTP ${log.statusCode || 0}`)}</td>
            <td>${escapeHtml(relativeTimeFromNow(log.createdAt))}<br><small>${escapeHtml(formatDateTime(log.createdAt))}</small></td>
        </tr>
    `).join("");
}

async function loadSession() {
    const me = await fetchJson("/admin/auth/me");
    state.me = { ...state.me, ...me, token: state.token };
    applyRoleView();
}

async function loadOverview(render = true) {
    const [overview, trend, modelStats, accessSummary, packagePurchaseRecords] = await Promise.all([
        fetchJson("/admin/dashboard/overview"),
        fetchJson("/admin/dashboard/trend?days=7"),
        fetchJson("/admin/dashboard/model-stats"),
        fetchJson("/admin/model-access/summary"),
        fetchJson("/admin/model-access/purchases")
    ]);
    state.overview = overview;
    state.trend = trend || [];
    state.modelStats = modelStats || [];
    state.accessSummary = accessSummary;
    state.packagePurchaseRecords = packagePurchaseRecords || [];
    syncSelectedPackageGroup();
    syncSelectedPurchasedPackage();
    if (render) {
        renderOverviewCards();
        renderQuotaCard();
        renderTrend();
        renderBillingModelStats();
        renderModelAccessSummary();
        renderPackagePurchaseRecords();
    }
}

async function loadAccessSummary(render = true) {
    state.accessSummary = await fetchJson("/admin/model-access/summary");
    syncSelectedPackageGroup();
    if (render) {
        renderPackageSelectionViews();
    }
}

async function loadUsers(render = true) {
    state.users = await fetchJson("/admin/users");
    if (render) {
        renderUsersTable();
        renderOverviewCards();
        renderProfileCard();
        updateAccountHeader();
    }
}

async function loadKeys(render = true) {
    state.keys = hydrateApiKeys(await fetchJson("/admin/api-keys"));
    if (render) {
        renderKeysTable();
        renderOverviewCards();
        populateKeyGroupOptions();
    }
}

async function loadProviders(render = true) {
    if (!isAdmin()) {
        state.providers = [];
        if (render) {
            renderProvidersTable();
            populateImportProviderOptions();
        }
        return;
    }
    state.providers = await fetchJson("/admin/providers");
    if (render) {
        renderProvidersTable();
        populateImportProviderOptions();
    }
}

async function loadModels(render = true) {
    state.models = await fetchJson("/admin/models");
    if (render) {
        renderModelAccessSummary();
        renderModelProviderFilters();
        populatePackageGroupSelects();
        renderModelCards();
        renderModelsTable();
        renderProfileCard();
    }
}

async function loadPackagePurchaseRecords(render = true) {
    state.packagePurchaseRecords = await fetchJson("/admin/model-access/purchases");
    syncSelectedPurchasedPackage();
    if (render) {
        renderPackagePurchaseRecords();
        renderPackageSelectionViews();
    }
}

async function loadWalletTransactions(render = true) {
    state.walletTransactions = await fetchJson("/admin/model-access/wallet-transactions");
    if (render) {
        renderWalletTransactions();
    }
}

async function loadLogs(render = true) {
    const [logs] = await Promise.all([
        fetchJson("/admin/request-logs?limit=50"),
        loadAccessSummary(false),
        loadPackagePurchaseRecords(false),
        loadWalletTransactions(false)
    ]);
    state.logs = logs;
    if (render) {
        renderLogsTable();
        renderOverviewCards();
        renderProfileCard();
        renderTrend();
        renderQuotaCard();
        renderPackagePurchaseRecords();
        renderWalletTransactions();
    }
}

async function loadAllData() {
    const loaders = [
        loadOverview(false),
        loadAccessSummary(false),
        loadUsers(false),
        loadKeys(false),
        loadModels(false),
        loadLogs(false),
        loadPackagePurchaseRecords(false),
        loadWalletTransactions(false)
    ];
    if (isAdmin()) {
        loaders.push(loadProviders(false));
    } else {
        await loadProviders(false);
    }
    const results = await Promise.allSettled(loaders);
    const failed = results.find((item) => item.status === "rejected");
    if (failed) {
        throw failed.reason;
    }
    syncSelectedPackageGroup();
    renderAllSections();
}

function resetRuntimeState() {
    state.me = null;
    state.overview = null;
    state.accessSummary = null;
    state.trend = [];
    state.users = [];
    state.keys = [];
    state.providers = [];
    state.models = [];
    state.logs = [];
    state.modelStats = [];
    state.packagePurchaseRecords = [];
    state.walletTransactions = [];
    state.upstreamModels = [];
    state.modelProviderFilter = "ALL";
    state.selectedPanel = "overview-panel";
    state.selectedModelId = null;
    state.selectedPackageGroupId = null;
    state.editingUserId = null;
    state.editingModelId = null;
}

function stopDashboardAutoRefresh() {
    if (dashboardRefreshTimer) {
        window.clearInterval(dashboardRefreshTimer);
        dashboardRefreshTimer = null;
    }
}

function startDashboardAutoRefresh() {
    stopDashboardAutoRefresh();
    dashboardRefreshTimer = window.setInterval(handleAction(async () => {
        if (!state.token || document.hidden) {
            return;
        }
        await Promise.all([loadOverview(false), loadLogs(false)]);
        renderOverviewCards();
        renderQuotaCard();
        renderTrend();
        renderBillingModelStats();
        renderLogsTable();
        renderPackagePurchaseRecords();
        renderModelAccessSummary();
    }), 10000);
}

function logout() {
    localStorage.removeItem("zxw-console-token");
    state.token = "";
    stopDashboardAutoRefresh();
    resetRuntimeState();
    toggleAuth(false);
    elements.keyCreateBox.classList.add("hidden");
    elements.plainKeyBox.classList.add("hidden");
    elements.importResultBox.classList.add("hidden");
}

async function viewUserDetail(id) {
    const user = await fetchJson(`/admin/users/${id}`);
    openUserDetailModal(user, "view");
}

async function editUserDetail(id) {
    const user = await fetchJson(`/admin/users/${id}`);
    openUserDetailModal(user, "edit");
}

async function onLogin(event) {
    event.preventDefault();
    const formData = new FormData(elements.loginForm);
    const data = await fetchJson("/admin/auth/login", {
        method: "POST",
        body: {
            username: String(formData.get("username") || "").trim(),
            password: String(formData.get("password") || "")
        }
    });
    state.token = data.token;
    state.me = data;
    localStorage.setItem("zxw-console-token", data.token);
    toggleAuth(true);
    applyRoleView();
    selectPanel("overview-panel");
    await loadAllData();
    startDashboardAutoRefresh();
    showToast("登录成功");
}

async function onRegister(event) {
    event.preventDefault();
    const formData = new FormData(elements.registerForm);
    const email = String(formData.get("email") || "").trim().toLowerCase();
    if (registerCodeSentToEmail && registerCodeSentToEmail !== email) {
        throw new Error(`验证码发送到的是 ${registerCodeSentToEmail}，请保持邮箱一致或重新发送验证码`);
    }
    const data = await fetchJson("/admin/auth/register", {
        method: "POST",
        body: {
            username: String(formData.get("username") || "").trim(),
            password: String(formData.get("password") || ""),
            nickname: String(formData.get("nickname") || "").trim(),
            email,
            phone: String(formData.get("phone") || "").trim(),
            verificationCode: String(formData.get("verificationCode") || "").trim()
        }
    });
    state.token = data.token;
    state.me = data;
    localStorage.setItem("zxw-console-token", data.token);
    resetRegisterCodeState({ resetForm: true });
    elements.registerModal.classList.add("hidden");
    toggleAuth(true);
    applyRoleView();
    selectPanel("overview-panel");
    await loadAllData();
    showToast("注册成功，已自动登录");
}

async function sendRegisterCode() {
    const email = String(elements.registerForm?.elements?.email?.value || "").trim();
    if (!email) {
        throw new Error("请先填写 QQ 邮箱");
    }
    if (!/^[^@\s]+@qq\.com$/i.test(email)) {
        throw new Error("请输入正确的 QQ 邮箱");
    }
    const data = await fetchJson("/admin/auth/register/code", {
        method: "POST",
        body: { email }
    });
    registerCodeSentToEmail = String(data?.email || email).trim().toLowerCase();
    startRegisterCodeCooldown(60);
    if (elements.registerCodeHint) {
        const debugSuffix = data?.code ? ` 当前验证码：${data.code}` : "";
        elements.registerCodeHint.textContent = `验证码已发送到 ${registerCodeSentToEmail}，${data?.expireSeconds || 300} 秒内有效。${debugSuffix}`;
    }
    showToast(data?.code
        ? `验证码已发送，当前验证码：${data.code}`
        : "验证码已发送，请检查邮箱");
}

function onRegisterEmailChange(event) {
    const email = String(event?.target?.value || "").trim().toLowerCase();
    if (!registerCodeSentToEmail || registerCodeSentToEmail === email) {
        return;
    }
    resetRegisterCodeState();
    if (elements.registerCodeHint) {
        elements.registerCodeHint.textContent = "邮箱已修改，请重新发送验证码。";
    }
}

async function onCreateUser(event) {
    event.preventDefault();
    const formData = new FormData(elements.userForm);
    await fetchJson("/admin/users", {
        method: "POST",
        body: {
            username: String(formData.get("username") || "").trim(),
            password: String(formData.get("password") || ""),
            nickname: String(formData.get("nickname") || "").trim(),
            email: String(formData.get("email") || "").trim(),
            phone: String(formData.get("phone") || "").trim(),
            roleCode: String(formData.get("roleCode") || "USER"),
            initialBalance: toNumber(formData.get("initialBalance"))
        }
    });
    elements.userForm.reset();
    elements.userForm.elements.roleCode.value = "USER";
    elements.userForm.elements.initialBalance.value = "0";
    await Promise.all([loadUsers(), loadOverview(), loadWalletTransactions()]);
    showToast("用户创建成功");
}

async function onRecharge(event) {
    event.preventDefault();
    const formData = new FormData(elements.rechargeForm);
    await fetchJson("/admin/users/recharge", {
        method: "POST",
        body: {
            userId: toNumber(formData.get("userId")),
            amount: toNumber(formData.get("amount")),
            remark: String(formData.get("remark") || "").trim()
        }
    });
    elements.rechargeForm.reset();
    await Promise.all([loadUsers(), loadOverview(), loadWalletTransactions()]);
    showToast("充值成功");
}

async function onUpdateUser(event) {
    event.preventDefault();
    const form = elements.userDetailForm;
    const payload = {
        nickname: String(form.elements.nickname.value || "").trim(),
        email: String(form.elements.email.value || "").trim(),
        phone: String(form.elements.phone.value || "").trim(),
        password: String(form.elements.password.value || "").trim()
    };
    if (isAdmin()) {
        payload.roleCode = String(form.elements.roleCode.value || "").trim();
        payload.status = String(form.elements.status.value || "").trim();
    }
    await fetchJson(`/admin/users/${state.editingUserId || state.me.userId}`, {
        method: "PUT",
        body: payload
    });
    closeUserDetailModal();
    await Promise.all([loadSession(), loadUsers(), loadOverview()]);
    showToast("用户信息已更新");
}

async function toggleUserStatus(id, nextStatus) {
    await fetchJson(`/admin/users/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await Promise.all([loadUsers(), loadOverview()]);
    showToast("用户状态已更新");
}

async function deleteUser(id, username) {
    const confirmed = window.confirm(`确认删除用户 ${username || id} 吗？该用户及其 API 密钥、钱包、会话记录会被直接删除。`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/users/${id}`, {
        method: "DELETE"
    });
    if (String(state.editingUserId) === String(id)) {
        closeUserDetailModal();
    }
    await Promise.all([loadUsers(), loadKeys(), loadOverview()]);
    showToast("用户已删除");
}

function toggleKeyCreateBox(forceVisible) {
    const shouldShow = typeof forceVisible === "boolean"
        ? forceVisible
        : elements.keyCreateBox.classList.contains("hidden");
    elements.keyCreateBox.classList.toggle("hidden", !shouldShow);
    if (!shouldShow) {
        elements.keyForm.reset();
        elements.plainKeyBox.classList.add("hidden");
        elements.plainKeyBox.textContent = "";
    }
    if (!isAdmin()) {
        elements.keyForm.elements.userId.value = state.me?.userId || "";
    }
    populateKeyGroupOptions();
    if (shouldShow) {
        elements.keyCreateBox.scrollIntoView({ behavior: "auto", block: "start" });
    }
}

function togglePackageCreateBox(forceVisible) {
    if (!elements.packageCreateBox) {
        return;
    }
    const shouldShow = typeof forceVisible === "boolean"
        ? forceVisible
        : elements.packageCreateBox.classList.contains("hidden");
    elements.packageCreateBox.classList.toggle("hidden", !shouldShow);
    if (!shouldShow) {
        elements.packageForm?.reset();
        return;
    }
    elements.packageCreateBox.scrollIntoView({ behavior: "auto", block: "start" });
}

async function onCreatePackage(event) {
    event.preventDefault();
    const formData = new FormData(elements.packageForm);
    await fetchJson("/admin/model-access/groups", {
        method: "POST",
        body: {
            groupCode: String(formData.get("groupCode") || "").trim(),
            groupName: String(formData.get("groupName") || "").trim(),
            salePrice: toNumber(formData.get("salePrice")),
            packageDays: toNumber(formData.get("packageDays")),
            dailyQuota: toNumber(formData.get("dailyQuota")),
            weeklyQuota: toNumber(formData.get("weeklyQuota")),
            monthlyQuota: toNumber(formData.get("monthlyQuota")),
            remark: String(formData.get("remark") || "").trim()
        }
    });
    togglePackageCreateBox(false);
    await Promise.all([loadAccessSummary(), loadModels(), loadPackagePurchaseRecords()]);
    showToast("套餐创建成功");
}

async function purchaseModelGroup(groupId) {
    const groups = getAccessGroups();
    const targetGroup = groups.find((item) => String(item.id) === String(groupId));
    if (!targetGroup) {
        throw new Error("暂未找到对应套餐分组");
    }
    await fetchJson("/admin/model-access/purchase", {
        method: "POST",
        body: { groupId: targetGroup.id }
    });
    state.selectedPackageGroupId = targetGroup.id;
    state.selectedPackageId = null;
    await Promise.all([loadAccessSummary(), loadUsers(), loadOverview(), loadPackagePurchaseRecords(), loadWalletTransactions()]);
    const newestPackage = getLatestPurchasedPackageByGroup(targetGroup.id);
    if (newestPackage) {
        state.selectedPackageId = newestPackage.id;
        state.selectedPackageGroupId = newestPackage.groupId;
    }
    renderPackageSelectionViews();
    showToast(`${targetGroup.groupName} 购买成功`);
}

async function deleteModelGroup(groupId) {
    const groups = getAccessGroups();
    const targetGroup = groups.find((item) => String(item.id) === String(groupId));
    if (!targetGroup) {
        throw new Error("暂未找到对应套餐分组");
    }
    const confirmed = window.confirm(`确认删除套餐 ${targetGroup.groupName} 吗？删除后该套餐将不再展示和售卖。`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/model-access/${targetGroup.id}`, {
        method: "DELETE"
    });
    if (String(state.selectedPackageGroupId) === String(targetGroup.id)) {
        state.selectedPackageGroupId = null;
    }
    await Promise.all([loadAccessSummary(), loadModels(), loadOverview(), loadPackagePurchaseRecords()]);
    showToast(`${targetGroup.groupName} 已删除`);
}

async function deletePurchasedPackage(packageId, packageName) {
    const confirmed = window.confirm(`确认删除已购套餐 ${packageName || packageId} 吗？绑定该套餐的 API Key 会同步禁用。`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/model-access/purchases/${packageId}`, {
        method: "DELETE"
    });
    if (String(state.selectedPackageId) === String(packageId)) {
        state.selectedPackageId = null;
    }
    const deletedPackage = getPurchasedPackages().find((item) => String(item.id) === String(packageId));
    if (deletedPackage && String(state.selectedPackageGroupId) === String(deletedPackage.groupId)) {
        state.selectedPackageGroupId = null;
    }
    await Promise.all([loadAccessSummary(), loadKeys(), loadOverview(), loadPackagePurchaseRecords(), loadWalletTransactions()]);
    renderPackageSelectionViews();
    showToast("已删除已购套餐");
}
async function onCreateKey(event) {
    event.preventDefault();
    const formData = new FormData(elements.keyForm);
    const checkedPackageInput = elements.keyForm.querySelector('input[name="modelPackageId"]:checked');
    const fallbackActivePackage = getPurchasedPackages()
        .filter((item) => isPackageUsable(item))
        .sort((a, b) => toNumber(b.id) - toNumber(a.id))[0] || null;
    const rawModelPackageId = String(
        checkedPackageInput?.value
        || state.selectedPackageId
        || fallbackActivePackage?.id
        || formData.get("modelPackageId")
        || ""
    ).trim();
    const modelPackageId = rawModelPackageId ? toNumber(rawModelPackageId) : null;
    if (!modelPackageId) {
        throw new Error("请先选择已购买且有效的具体套餐");
    }
    state.selectedPackageId = modelPackageId;
    const data = await fetchJson("/admin/api-keys", {
        method: "POST",
        body: {
            userId: isAdmin() ? toNumber(formData.get("userId")) : state.me.userId,
            name: String(formData.get("name") || "").trim(),
            expiresAt: String(formData.get("expiresAt") || "").trim(),
            modelPackageId,
            remark: String(formData.get("remark") || "").trim()
        }
    });
    rememberPlainApiKey(data.plainTextKey);
    elements.plainKeyBox.innerHTML = `新密钥已创建：<code>${escapeHtml(data.plainTextKey)}</code>`;
    elements.plainKeyBox.classList.remove("hidden");
    elements.plainKeyBox.innerHTML = `
        <div>新密钥已创建：<code>${escapeHtml(data.plainTextKey)}</code></div>
        <div class="inline-actions">
            <button type="button" class="mini-button" data-copy-key-value="${escapeHtml(data.plainTextKey)}">一键复制</button>
        </div>
    `;
    elements.keyForm.reset();
    if (!isAdmin()) {
        elements.keyForm.elements.userId.value = state.me.userId;
    }
    await Promise.all([loadKeys(), loadOverview(), loadAccessSummary()]);
    populateKeyGroupOptions();
    showToast("API 密钥创建成功");
}

async function toggleKeyStatus(id, nextStatus) {
    await fetchJson(`/admin/api-keys/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await loadKeys();
    showToast("密钥状态已更新");
}

async function deleteKey(id, keyName, accessKey) {
    const confirmed = window.confirm(`确认删除 API Key ${keyName || id} 吗？删除后这个 Key 会立即不可用。`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/api-keys/${id}`, {
        method: "DELETE"
    });
    forgetPlainApiKey(accessKey);
    await loadKeys();
    showToast("API Key 已删除");
}

async function onCreateProvider(event) {
    event.preventDefault();
    const formData = new FormData(elements.providerForm);
    await fetchJson("/admin/providers", {
        method: "POST",
        body: {
            providerCode: String(formData.get("providerCode") || "").trim(),
            providerName: String(formData.get("providerName") || "").trim(),
            baseUrl: String(formData.get("baseUrl") || "").trim(),
            providerType: String(formData.get("providerType") || "").trim(),
            priorityNo: toNumber(formData.get("priorityNo")),
            timeoutMs: toNumber(formData.get("timeoutMs")),
            tokenName: String(formData.get("tokenName") || "").trim(),
            tokenValue: String(formData.get("tokenValue") || "").trim()
        }
    });
    elements.providerForm.reset();
    elements.providerForm.elements.providerType.value = "OPENAI_COMPATIBLE";
    elements.providerForm.elements.priorityNo.value = "100";
    elements.providerForm.elements.timeoutMs.value = "60000";
    await loadProviders();
    showToast("渠道创建成功");
}

async function toggleProviderStatus(id, nextStatus) {
    await fetchJson(`/admin/providers/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await loadProviders();
    showToast("渠道状态已更新");
}

async function onCreateModel(event) {
    event.preventDefault();
    const formData = new FormData(elements.modelForm);
    const payload = {
        modelName: String(formData.get("modelName") || "").trim(),
        modelType: String(formData.get("modelType") || "CHAT").trim(),
        billingType: String(formData.get("billingType") || "TOKEN").trim(),
        promptPrice: toNumber(formData.get("promptPrice")),
        cachedPromptPrice: toNumber(formData.get("cachedPromptPrice")),
        completionPrice: toNumber(formData.get("completionPrice")),
        requestPrice: toNumber(formData.get("requestPrice")) || 0.07,
        multiplier: toNumber(formData.get("multiplier")) || 1,
        isPublic: String(formData.get("isPublic")) === "true",
        bindingId: state.editingModelBindingId,
        groupId: toNumber(formData.get("groupId")),
        providerId: toNumber(formData.get("providerId")),
        upstreamModel: String(formData.get("upstreamModel") || "").trim()
    };
    if (!payload.groupId) {
        throw new Error("请先选择套餐分组");
    }
    if (state.editingModelId) {
        await fetchJson(`/admin/models/${state.editingModelId}`, {
            method: "PUT",
            body: payload
        });
        resetModelForm();
        await Promise.all([loadModels(), loadOverview(), loadAccessSummary()]);
        showToast("模型已更新");
        return;
    }
    await fetchJson("/admin/models", {
        method: "POST",
        body: {
            modelCode: String(formData.get("modelCode") || "").trim(),
            ...payload,
            imagePrice: 0,
        }
    });
    resetModelForm();
    await Promise.all([loadModels(), loadOverview(), loadAccessSummary()]);
    showToast("模型创建成功");
}

function resetModelForm() {
    state.editingModelId = null;
    state.editingModelBindingId = null;
    elements.modelForm.reset();
    elements.modelForm.elements.modelCode.value = "";
    elements.modelForm.elements.modelCode.readOnly = false;
    elements.modelForm.elements.modelCode.required = true;
    elements.modelForm.elements.modelType.value = "CHAT";
    elements.modelForm.elements.billingType.value = "TOKEN";
    elements.modelForm.elements.promptPrice.value = "0";
    elements.modelForm.elements.cachedPromptPrice.value = "0";
    elements.modelForm.elements.completionPrice.value = "0";
    elements.modelForm.elements.requestPrice.value = "0.07";
    elements.modelForm.elements.multiplier.value = "1";
    elements.modelForm.elements.isPublic.value = "true";
    if (state.selectedPackageGroupId) {
        elements.modelForm.elements.groupId.value = String(state.selectedPackageGroupId);
    }
    elements.modelFormTitle.textContent = "手动创建模型";
    elements.modelFormCaption.textContent = "普通用户不能创建模型，只能使用管理员已创建的公开模型。";
    elements.modelSubmitButton.textContent = "创建模型";
    elements.modelCancelEditButton.classList.add("hidden");
}

function startModelEdit(id, bindingId = null) {
    const model = state.models.find((item) => bindingId && String(item.bindingId) === String(bindingId))
        || state.models.find((item) => String(item.id) === String(id));
    if (!model) {
        throw new Error("未找到模型配置");
    }
    state.editingModelId = model.id;
    state.editingModelBindingId = model.bindingId || null;
    elements.modelForm.elements.modelCode.value = model.modelCode || "";
    elements.modelForm.elements.modelCode.readOnly = true;
    elements.modelForm.elements.modelCode.required = false;
    elements.modelForm.elements.modelName.value = model.modelName || "";
    elements.modelForm.elements.modelType.value = model.modelType || "CHAT";
    elements.modelForm.elements.billingType.value = model.billingType || "TOKEN";
    elements.modelForm.elements.promptPrice.value = formatDecimal(model.promptPrice, 6);
    elements.modelForm.elements.cachedPromptPrice.value = formatDecimal(model.cachedPromptPrice, 6);
    elements.modelForm.elements.completionPrice.value = formatDecimal(model.completionPrice, 6);
    elements.modelForm.elements.requestPrice.value = formatDecimal(model.requestPrice || 0.07, 6);
    elements.modelForm.elements.multiplier.value = formatDecimal(model.multiplier, 4);
    elements.modelForm.elements.isPublic.value = String(model.isPublic) === "1" ? "true" : "false";
    elements.modelForm.elements.groupId.value = model.groupId || "";
    elements.modelForm.elements.providerId.value = model.providerId || "";
    elements.modelForm.elements.upstreamModel.value = model.upstreamModel || "";
    elements.modelFormTitle.textContent = `编辑价格 / ${model.modelCode}`;
    elements.modelFormCaption.textContent = "管理员可以修改模型价格、倍率、公开状态和上游映射。";
    elements.modelSubmitButton.textContent = "保存修改";
    elements.modelCancelEditButton.classList.remove("hidden");
    elements.modelForm.scrollIntoView({ behavior: "auto", block: "start" });
}

async function toggleModelStatus(id, nextStatus) {
    await fetchJson(`/admin/models/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await Promise.all([loadModels(), loadOverview(), loadAccessSummary()]);
    showToast("模型状态已更新");
}

async function deleteModel(id, modelCode) {
    const confirmed = window.confirm(`确认删除模型 ${modelCode || id} 吗？会直接删除该模型和它的路由配置。`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/models/${id}`, {
        method: "DELETE"
    });
    if (String(state.editingModelId) === String(id)) {
        resetModelForm();
    }
    await Promise.all([loadModels(), loadOverview(), loadAccessSummary()]);
    showToast("模型已删除");
}

async function fetchUpstreamModels() {
    const groupId = toNumber(elements.importGroupSelect.value);
    const providerId = toNumber(elements.importProviderSelect.value);
    if (!groupId) {
        throw new Error("请先选择套餐");
    }
    if (!providerId) {
        throw new Error("请先选择渠道");
    }
    const list = await fetchJson(`/admin/models/upstream?providerId=${providerId}`);
    state.upstreamModels = (list || []).map((item) => ({ ...item, selected: false }));
    renderUpstreamModels();
    showToast("已读取上游模型");
}

function toggleAllUpstreamModels() {
    if (!state.upstreamModels.length) {
        return;
    }
    const shouldSelectAll = state.upstreamModels.some((item) => !item.selected);
    state.upstreamModels = state.upstreamModels.map((item) => ({ ...item, selected: shouldSelectAll }));
    renderUpstreamModels();
}

async function importUpstreamModels() {
    const groupId = toNumber(elements.importGroupSelect.value);
    const providerId = toNumber(elements.importProviderSelect.value);
    const selected = state.upstreamModels.filter((item) => item.selected).map((item) => item.id);
    if (!groupId) {
        throw new Error("请先选择套餐");
    }
    if (!providerId) {
        throw new Error("请先选择渠道");
    }
    if (!selected.length) {
        throw new Error("请至少勾选一个上游模型");
    }
    const result = await fetchJson("/admin/models/import", {
        method: "POST",
        body: {
            groupId,
            providerId,
            upstreamModels: selected,
            promptPrice: toNumber(elements.importPromptPrice.value),
            cachedPromptPrice: toNumber(elements.importCachedPromptPrice.value),
            completionPrice: toNumber(elements.importCompletionPrice.value),
            multiplier: toNumber(elements.importMultiplier.value) || 1,
            isPublic: elements.importIsPublic.value === "true"
        }
    });
    elements.importResultBox.classList.remove("hidden");
    elements.importResultBox.innerHTML = `导入完成：成功 ${escapeHtml(result.importedCount)} 个，跳过 ${escapeHtml(result.skippedCount)} 个。`;
    await Promise.all([loadModels(), loadOverview(), loadAccessSummary()]);
    showToast("批量导入完成");
}

async function refreshSection(section) {
    if (section === "users") {
        await loadUsers();
    } else if (section === "keys") {
        await Promise.all([loadKeys(), loadAccessSummary()]);
    } else if (section === "providers") {
        await loadProviders();
    } else if (section === "models") {
        await Promise.all([loadModels(), loadAccessSummary(), loadPackagePurchaseRecords(), loadWalletTransactions()]);
    } else if (section === "logs") {
        await loadLogs();
    }
}

function handleAction(action) {
    return async (event) => {
        try {
            await action(event);
        } catch (error) {
            const message = error instanceof Error ? error.message : "操作失败";
            if (message.includes("请先登录")) {
                logout();
            }
            showToast(message, true);
        }
    };
}

function bindEvents() {
    elements.loginForm.addEventListener("submit", handleAction(onLogin));
    elements.registerForm.addEventListener("submit", handleAction(onRegister));
    elements.sendRegisterCodeButton?.addEventListener("click", handleAction(sendRegisterCode));
    elements.registerForm?.elements?.email?.addEventListener("input", onRegisterEmailChange);
    elements.userForm.addEventListener("submit", handleAction(onCreateUser));
    elements.rechargeForm.addEventListener("submit", handleAction(onRecharge));
    elements.userDetailForm.addEventListener("submit", handleAction(onUpdateUser));
    elements.keyForm.addEventListener("submit", handleAction(onCreateKey));
    elements.providerForm.addEventListener("submit", handleAction(onCreateProvider));
    elements.packageForm?.addEventListener("submit", handleAction(onCreatePackage));
    elements.modelForm.addEventListener("submit", handleAction(onCreateModel));
    elements.modelCancelEditButton.addEventListener("click", resetModelForm);
    elements.togglePackageCreateButton?.addEventListener("click", () => togglePackageCreateBox());
    elements.closePackageCreateButton?.addEventListener("click", () => togglePackageCreateBox(false));
    elements.refreshAllButton.addEventListener("click", handleAction(async () => {
        await loadSession();
        await loadAllData();
        showToast("数据已刷新");
    }));
    elements.logoutButton.addEventListener("click", () => {
        logout();
        showToast("已退出登录");
    });
    elements.openRegisterButton.addEventListener("click", () => elements.registerModal.classList.remove("hidden"));
    elements.closeRegisterButton.addEventListener("click", () => elements.registerModal.classList.add("hidden"));
    document.querySelector('[data-close-register="true"]').addEventListener("click", () => elements.registerModal.classList.add("hidden"));
    elements.closeUserDetailButton.addEventListener("click", closeUserDetailModal);
    document.querySelector('[data-close-user-detail="true"]').addEventListener("click", closeUserDetailModal);
    elements.toggleKeyCreateButton.addEventListener("click", toggleKeyCreateBox);
    elements.closeKeyCreateButton?.addEventListener("click", () => toggleKeyCreateBox(false));
    elements.fetchUpstreamModelsButton.addEventListener("click", handleAction(fetchUpstreamModels));
    elements.toggleAllUpstreamModelsButton.addEventListener("click", toggleAllUpstreamModels);
    elements.importUpstreamModelsButton.addEventListener("click", handleAction(importUpstreamModels));
    elements.menuItems.forEach((item) => item.addEventListener("click", () => selectPanel(item.dataset.panel)));
    elements.refreshButtons.forEach((button) => button.addEventListener("click", handleAction(() => refreshSection(button.dataset.refresh))));

    elements.dashboardPackageSelect?.addEventListener("change", () => {
        state.selectedPackageId = elements.dashboardPackageSelect.value || null;
        const selectedPackage = getSelectedPurchasedPackage();
        state.selectedPackageGroupId = selectedPackage?.groupId || null;
        renderPackageSelectionViews();
    });

    elements.importGroupSelect?.addEventListener("change", () => {
        if (elements.importGroupSelect.value) {
            state.selectedPackageGroupId = elements.importGroupSelect.value;
            renderPackageSelectionViews();
        }
    });

    elements.keyGroupOptions?.addEventListener("change", (event) => {
        const target = event.target.closest('input[name="modelPackageId"]');
        if (!target) {
            return;
        }
        state.selectedPackageId = target.value || null;
        const selectedPackage = getSelectedPurchasedPackage();
        state.selectedPackageGroupId = selectedPackage?.groupId || null;
        populateKeyGroupOptions();
    });

    elements.packageCardGrid?.addEventListener("click", handleAction(async (event) => {
        const showButton = event.target.closest("button[data-show-package-models]");
        if (showButton) {
            event.stopPropagation();
            showPackageModelDetails(showButton.dataset.showPackageModels);
            return;
        }
        const purchaseButton = event.target.closest("button[data-purchase-group]");
        if (purchaseButton) {
            await purchaseModelGroup(purchaseButton.dataset.purchaseGroup);
            return;
        }
        const deleteButton = event.target.closest("button[data-delete-group]");
        if (deleteButton) {
            await deleteModelGroup(deleteButton.dataset.deleteGroup);
            return;
        }
        const card = event.target.closest("[data-select-package]");
        if (card) {
            state.selectedPackageGroupId = card.dataset.selectPackage;
            const selectedPackage = getFirstPurchasedPackageByGroup(card.dataset.selectPackage);
            if (selectedPackage) {
                state.selectedPackageId = selectedPackage.id;
            }
            renderPackageSelectionViews();
            populatePackageGroupSelects();
        }
    }));

    elements.usersTable.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }
        if (button.dataset.userView) {
            await viewUserDetail(button.dataset.userView);
        }
        if (button.dataset.userEdit) {
            await editUserDetail(button.dataset.userEdit);
        }
        if (button.dataset.userToggle) {
            await toggleUserStatus(button.dataset.userToggle, button.dataset.nextStatus);
        }
        if (button.dataset.userDelete) {
            await deleteUser(button.dataset.userDelete, button.dataset.username);
        }
    }));

    if (elements.profileCardBody) {
        elements.profileCardBody.addEventListener("click", handleAction(async (event) => {
            const button = event.target.closest("[data-profile-edit]");
            if (button) {
                await editUserDetail(button.dataset.profileEdit);
            }
        }));
    }

    elements.keysList.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }
        if (button.dataset.keyCopy) {
            await copyApiKey(button.dataset.keyCopy);
            return;
        }
        if (button.dataset.keyDelete) {
            await deleteKey(button.dataset.keyDelete, button.dataset.keyName, button.dataset.keyAccess);
            return;
        }
        if (button.dataset.keyToggle) {
            await toggleKeyStatus(button.dataset.keyToggle, button.dataset.nextStatus);
        }
    }));

    elements.keysTable.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (!button) {
            return;
        }
        if (button.dataset.keyCopy) {
            await copyApiKey(button.dataset.keyCopy);
            return;
        }
        if (button.dataset.keyDelete) {
            await deleteKey(button.dataset.keyDelete, button.dataset.keyName, button.dataset.keyAccess);
            return;
        }
        if (button.dataset.keyToggle) {
            await toggleKeyStatus(button.dataset.keyToggle, button.dataset.nextStatus);
        }
    }));

    elements.providersTable.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button[data-provider-toggle]");
        if (button) {
            await toggleProviderStatus(button.dataset.providerToggle, button.dataset.nextStatus);
        }
    }));

    elements.modelsTable.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (button) {
            if (button.dataset.modelEdit) {
                startModelEdit(button.dataset.modelEdit, button.dataset.modelBinding);
                return;
            }
            if (button.dataset.modelDelete) {
                await deleteModel(button.dataset.modelDelete, button.dataset.modelCode);
                return;
            }
            if (button.dataset.modelToggle) {
                await toggleModelStatus(button.dataset.modelToggle, button.dataset.nextStatus);
            }
        }
    }));

    elements.modelsCardGrid.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (button) {
            if (button.dataset.modelEdit) {
                startModelEdit(button.dataset.modelEdit, button.dataset.modelBinding);
                return;
            }
            if (button.dataset.modelDelete) {
                await deleteModel(button.dataset.modelDelete, button.dataset.modelCode);
                return;
            }
        }
        const card = event.target.closest("[data-model-select]");
        if (card) {
            state.selectedModelId = card.dataset.modelSelect;
            renderModelCards();
        }
    }));

    elements.modelProviderFilters.addEventListener("click", (event) => {
        const button = event.target.closest("button[data-provider-filter]");
        if (!button) {
            return;
        }
        state.modelProviderFilter = button.dataset.providerFilter;
        if (state.modelProviderFilter.startsWith("GROUP:")) {
            state.selectedPackageGroupId = state.modelProviderFilter.slice("GROUP:".length);
        }
        renderModelProviderFilters();
        renderModelCards();
    });

    document.addEventListener("click", handleAction(async (event) => {
        const showPackageButton = event.target.closest("[data-show-package-models]");
        if (showPackageButton) {
            showPackageModelDetails(showPackageButton.dataset.showPackageModels);
            return;
        }
        const deletePackageButton = event.target.closest("[data-delete-package-purchase]");
        if (deletePackageButton) {
            await deletePurchasedPackage(deletePackageButton.dataset.deletePackagePurchase, deletePackageButton.dataset.packageName);
            return;
        }
        const panelButton = event.target.closest("[data-open-panel]");
        if (panelButton) {
            selectPanel(panelButton.dataset.openPanel);
            return;
        }
        const copyButton = event.target.closest("[data-copy-endpoint]");
        if (copyButton && elements.overviewEndpointUrl) {
            await navigator.clipboard.writeText(elements.overviewEndpointUrl.textContent || "");
            showToast("接入地址已复制");
        }
    }));

    document.addEventListener("click", handleAction(async (event) => {
        const copyKeyButton = event.target.closest("[data-copy-key-value]");
        if (!copyKeyButton) {
            return;
        }
        await navigator.clipboard.writeText(copyKeyButton.dataset.copyKeyValue || "");
        showToast("API Key 已复制");
    }));

    elements.agentDebugCopyPayloadButton?.addEventListener("click", handleAction(copyAgentDebugPayload));
    elements.agentDebugCopyCurlButton?.addEventListener("click", handleAction(copyAgentDebugCurl));
    elements.agentDebugSendButton?.addEventListener("click", handleAction(sendAgentDebugRequest));
    elements.agentDebugModel?.addEventListener("input", renderAgentDebugPreview);
    elements.agentDebugWorkspace?.addEventListener("input", renderAgentDebugPreview);
    elements.agentDebugPrompt?.addEventListener("input", renderAgentDebugPreview);

    elements.upstreamModelsBox.addEventListener("change", (event) => {
        const checkbox = event.target.closest("input[data-upstream-model]");
        if (!checkbox) {
            return;
        }
        state.upstreamModels = state.upstreamModels.map((item) => item.id === checkbox.dataset.upstreamModel
            ? { ...item, selected: checkbox.checked }
            : item);
    });
}

async function bootstrap() {
    bindEvents();
    resetModelForm();
    updateRegisterCodeUi(0);
    renderAgentDebugPreview();
    if (!state.token) {
        toggleAuth(false);
        return;
    }
    try {
        toggleAuth(true);
        await loadSession();
        selectPanel("overview-panel");
        await loadAllData();
        startDashboardAutoRefresh();
    } catch (error) {
        logout();
        showToast(error instanceof Error ? error.message : "会话已失效，请重新登录", true);
    }
}

bootstrap();





