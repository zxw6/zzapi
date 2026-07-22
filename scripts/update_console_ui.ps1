$path = 'E:\project-ai\Test\src\main\resources\static\console\app.js'
$content = Get-Content -Path $path -Raw

function Replace-Block {
    param(
        [string]$Source,
        [string]$Pattern,
        [string]$Replacement
    )

    return [regex]::Replace(
        $Source,
        $Pattern,
        [System.Text.RegularExpressions.MatchEvaluator]{ param($m) $Replacement },
        [System.Text.RegularExpressions.RegexOptions]::Singleline
    )
}

if ($content -notmatch 'function formatTokens\(value\)') {
    $content = $content -replace 'function formatDecimal\(value, digits = 4\) \{\r?\n    return toNumber\(value\)\.toFixed\(digits\);\r?\n\}', @'
function formatDecimal(value, digits = 4) {
    return toNumber(value).toFixed(digits);
}

function formatTokens(value) {
    return Math.round(toNumber(value)).toLocaleString("en-US");
}
'@
}

$content = Replace-Block $content 'function renderOverviewCards\(\) \{.*?\n\}\r?\n\r?\nfunction renderProfileCard\(\) \{' @'
function renderOverviewCards() {
    const overview = state.overview;
    if (!overview) {
        elements.overviewCards.innerHTML = "";
        return;
    }
    const cards = isAdmin()
        ? [
            { icon: "U", label: "Users", value: overview.userCount, subvalue: "active platform users" },
            { icon: "K", label: "API Keys", value: overview.apiKeyCount, subvalue: "all available keys" },
            { icon: "T", label: "Today Tokens", value: formatTokens(overview.totalTokensToday), subvalue: "consumed today" },
            { icon: "$", label: "Balance", value: formatMoney(overview.walletBalanceTotal), subvalue: "wallet total" }
        ]
        : [
            { icon: "$", label: "Balance", value: formatMoney(overview.walletBalanceTotal), subvalue: "current wallet balance" },
            { icon: "K", label: "My Keys", value: overview.apiKeyCount, subvalue: "available api keys" },
            { icon: "T", label: "Today Tokens", value: formatTokens(overview.totalTokensToday), subvalue: "your token usage today" },
            { icon: "L", label: "Today Requests", value: overview.requestCountToday, subvalue: "requests sent today" }
        ];
    elements.overviewCards.innerHTML = cards.map((card) => `
        <article class="metric-card">
            <div class="metric-top">
                <div class="metric-icon">${escapeHtml(card.icon)}</div>
                <div class="metric-label">${escapeHtml(card.label)}</div>
            </div>
            <div class="metric-value">${escapeHtml(card.value)}</div>
            <div class="metric-subvalue">${escapeHtml(card.subvalue)}</div>
        </article>
    `).join("");
}

function renderProfileCard() {
'@

$content = Replace-Block $content 'function renderQuotaCard\(\) \{.*?\n\}\r?\n\r?\nfunction renderUsersTable\(\) \{' @'
function renderQuotaCard() {
    const overview = state.overview || {};
    const totalQuota = sumValues(state.keys, "totalQuota");
    const usedQuota = sumValues(state.keys, "usedQuota");
    const usedPercent = percentage(usedQuota, totalQuota);
    const todayTokens = toNumber(overview.totalTokensToday);
    const weekTokens = toNumber(overview.totalTokens7d);
    elements.quotaCardTitle.textContent = isAdmin() ? "Platform Overview" : "My Usage";
    elements.quotaStatusBadge.textContent = isAdmin() ? "ADMIN" : "USER";
    elements.quotaCardBody.innerHTML = `
        <div class="quota-headline">
            <div>
                <div class="quota-plan">${escapeHtml(isAdmin() ? "Admin Control View" : "Personal Usage View")}</div>
                <div class="quota-plan-subtitle">${escapeHtml(isAdmin() ? "Track balance, token usage, keys, and model availability in one place." : "Track your balance, token usage, and key consumption in one place.")}</div>
            </div>
            <span class="soft-badge">${escapeHtml(formatMoney(overview.walletBalanceTotal))}</span>
        </div>
        <div class="quota-bars">
            <div>
                <div class="bar-label">
                    <span>Key Quota Used</span>
                    <strong>${escapeHtml(formatDecimal(usedQuota, 2))} / ${escapeHtml(formatDecimal(totalQuota, 2))}</strong>
                </div>
                <div class="progress-track"><div class="progress-fill" style="width:${usedPercent}%"></div></div>
            </div>
            <div>
                <div class="bar-label">
                    <span>Today Tokens</span>
                    <strong>${escapeHtml(formatTokens(todayTokens))}</strong>
                </div>
                <div class="progress-track"><div class="progress-fill" style="width:${Math.min(100, weekTokens > 0 ? (todayTokens / weekTokens) * 100 : 0)}%"></div></div>
            </div>
        </div>
        <div class="quota-footer">
            <div class="quota-meta"><span>Today Recharge</span><strong>${escapeHtml(formatMoney(overview.rechargeAmountToday || 0))}</strong></div>
            <div class="quota-meta"><span>Current Keys</span><strong>${escapeHtml(state.keys.length)}</strong></div>
            <div class="quota-meta"><span>7d Tokens</span><strong>${escapeHtml(formatTokens(weekTokens))}</strong></div>
            <div class="quota-meta"><span>Today Cost</span><strong>${escapeHtml(formatMoney(overview.consumeAmountToday || 0))}</strong></div>
        </div>
    `;
}

function renderTrend() {
    if (!state.trend.length) {
        elements.trendSummary.textContent = "No data";
        elements.trendChart.innerHTML = '<div class="empty-state">No statistics for the last 7 days yet.</div>';
        return;
    }
    const maxValue = Math.max(...state.trend.map((item) => toNumber(item.requestCount)), 1);
    const totalRequests = state.trend.reduce((sum, item) => sum + toNumber(item.requestCount), 0);
    const totalTokens = state.trend.reduce((sum, item) => sum + toNumber(item.totalTokens), 0);
    elements.trendSummary.textContent = `7d requests ${totalRequests} | tokens ${formatTokens(totalTokens)}`;
    elements.trendChart.innerHTML = state.trend.map((item) => {
        const height = Math.max(12, (toNumber(item.requestCount) / maxValue) * 180);
        const label = item.statDate ? String(item.statDate).slice(5) : "-";
        return `
            <div class="trend-bar">
                <div class="trend-value">${escapeHtml(item.requestCount)} / ${escapeHtml(formatTokens(item.totalTokens || 0))}</div>
                <div class="trend-column"><div class="trend-fill" style="height:${height}px"></div></div>
                <div class="trend-label">${escapeHtml(label)}</div>
            </div>
        `;
    }).join("");
}

function renderUsersTable() {
'@

$content = Replace-Block $content 'function renderUsersTable\(\) \{.*?\n\}\r?\n\r?\nfunction toggleUserDetailReadonly\(readonly\) \{' @'
function renderUsersTable() {
    if (!state.users.length) {
        elements.usersTable.innerHTML = '<tr><td colspan="7" class="empty-state">No user data yet.</td></tr>';
        return;
    }
    elements.usersTable.innerHTML = state.users.map((user) => {
        const actions = [
            `<button class="mini-button" type="button" data-user-view="${user.id}">View</button>`,
            `<button class="mini-button" type="button" data-user-edit="${user.id}">Edit</button>`
        ];
        if (isAdmin()) {
            const nextStatus = user.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
            actions.push(`<button class="mini-button" type="button" data-user-toggle="${user.id}" data-next-status="${nextStatus}">${user.status === "ACTIVE" ? "Disable" : "Enable"}</button>`);
            actions.push(`<button class="mini-button danger-button" type="button" data-user-delete="${user.id}" data-username="${escapeHtml(user.username)}">Delete</button>`);
        }
        return `
            <tr>
                <td>${escapeHtml(user.id)}</td>
                <td>${escapeHtml(user.username)}</td>
                <td>${escapeHtml(user.roleCode)}</td>
                <td>${statusChip(user.status)}</td>
                <td>${escapeHtml(formatMoney(user.balance))}</td>
                <td>${escapeHtml(user.email || "-")}</td>
                <td><div class="action-row">${actions.join("")}</div></td>
            </tr>
        `;
    }).join("");
}

function toggleUserDetailReadonly(readonly) {
'@

$content = Replace-Block $content 'function renderModelCards\(\) \{.*?\n\}\r?\n\r?\nfunction populateImportProviderOptions\(\) \{' @'
function renderModelCards() {
    const list = state.models.filter((item) => state.modelProviderFilter === "ALL" || (item.providerType || item.providerName || "UNKNOWN") === state.modelProviderFilter);
    elements.modelsEmpty.classList.toggle("hidden", list.length > 0);
    if (!list.length) {
        elements.modelsCardGrid.innerHTML = "";
        return;
    }
    elements.modelsCardGrid.innerHTML = list.map((model) => {
        const providerLabel = normalizeModelProviderLabel(model);
        return `
            <article class="model-price-card">
                <div class="model-price-head">
                    <div class="model-brand-icon">${escapeHtml(providerLabel.slice(0, 1).toUpperCase())}</div>
                    <div>
                        <div class="model-title">${escapeHtml(model.modelName || model.modelCode)}</div>
                        <div class="model-code">${escapeHtml(model.modelCode)}</div>
                        <div class="model-provider-line">${escapeHtml(providerLabel)} / upstream ${escapeHtml(model.upstreamModel || "-")}</div>
                    </div>
                </div>
                <div class="model-price-grid">
                    <div class="model-price-box"><span>Prompt</span><strong>${escapeHtml(formatDecimal(model.promptPrice, 6))}</strong></div>
                    <div class="model-price-box"><span>Completion</span><strong>${escapeHtml(formatDecimal(model.completionPrice, 6))}</strong></div>
                </div>
                <div class="model-price-foot">
                    <span>Multiplier ${escapeHtml(formatDecimal(model.multiplier, 2))}</span>
                    ${statusChip(model.status)}
                </div>
                ${isAdmin() ? `
                    <div class="model-card-actions">
                        <button class="mini-button" type="button" data-model-edit="${model.id}">Edit</button>
                        <button class="mini-button danger-button" type="button" data-model-delete="${model.id}" data-model-code="${escapeHtml(model.modelCode)}">Delete</button>
                    </div>
                ` : ""}
            </article>
        `;
    }).join("");
}

function renderModelsTable() {
    if (!isAdmin()) {
        elements.modelsTable.innerHTML = "";
        return;
    }
    if (!state.models.length) {
        elements.modelsTable.innerHTML = '<tr><td colspan="10" class="empty-state">No model configuration yet.</td></tr>';
        return;
    }
    elements.modelsTable.innerHTML = state.models.map((model) => `
        <tr>
            <td>${escapeHtml(model.id)}</td>
            <td>${escapeHtml(model.modelCode)}</td>
            <td>${escapeHtml(formatDecimal(model.promptPrice, 6))}</td>
            <td>${escapeHtml(formatDecimal(model.completionPrice, 6))}</td>
            <td>${escapeHtml(formatDecimal(model.multiplier, 4))}</td>
            <td>${escapeHtml(model.modelType || "-")}</td>
            <td>${escapeHtml(model.providerName || "-")}</td>
            <td>${escapeHtml(model.upstreamModel || "-")}</td>
            <td>${statusChip(model.status)}</td>
            <td class="action-row">
                <button class="mini-button" type="button" data-model-edit="${model.id}">Edit Price</button>
                <button class="mini-button" type="button" data-model-toggle="${model.id}" data-next-status="${model.status === "ACTIVE" ? "DISABLED" : "ACTIVE"}">${model.status === "ACTIVE" ? "Disable" : "Enable"}</button>
                <button class="mini-button danger-button" type="button" data-model-delete="${model.id}" data-model-code="${escapeHtml(model.modelCode)}">Delete</button>
            </td>
        </tr>
    `).join("");
}

function populateImportProviderOptions() {
'@

$content = Replace-Block $content 'async function onCreateModel\(event\) \{.*?\n\}\r?\n\r?\nfunction resetModelForm\(\) \{' @'
async function onCreateModel(event) {
    event.preventDefault();
    const formData = new FormData(elements.modelForm);
    const payload = {
        modelName: String(formData.get("modelName") || "").trim(),
        modelType: String(formData.get("modelType") || "CHAT").trim(),
        billingType: String(formData.get("billingType") || "TOKEN").trim(),
        promptPrice: toNumber(formData.get("promptPrice")),
        completionPrice: toNumber(formData.get("completionPrice")),
        multiplier: toNumber(formData.get("multiplier")) || 1,
        isPublic: String(formData.get("isPublic")) === "true",
        providerId: toNumber(formData.get("providerId")),
        upstreamModel: String(formData.get("upstreamModel") || "").trim()
    };
    if (state.editingModelId) {
        await fetchJson(`/admin/models/${state.editingModelId}`, {
            method: "PUT",
            body: payload
        });
        resetModelForm();
        await Promise.all([loadModels(), loadOverview()]);
        showToast("Model updated");
        return;
    }
    await fetchJson("/admin/models", {
        method: "POST",
        body: {
            modelCode: String(formData.get("modelCode") || "").trim(),
            ...payload,
            requestPrice: 0,
            imagePrice: 0,
        }
    });
    resetModelForm();
    await Promise.all([loadModels(), loadOverview()]);
    showToast("Model created");
}

function resetModelForm() {
'@

$content = Replace-Block $content 'async function toggleUserStatus\(id, nextStatus\) \{.*?\n\}\r?\n\r?\nasync function viewUserDetail\(id\) \{' @'
async function toggleUserStatus(id, nextStatus) {
    await fetchJson(`/admin/users/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await Promise.all([loadUsers(), loadOverview()]);
    showToast("User status updated");
}

async function deleteUser(id, username) {
    const confirmed = window.confirm(`Delete user ${username || id}? Their API keys will also be disabled.`);
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
    showToast("User deleted");
}

async function viewUserDetail(id) {
'@

$content = Replace-Block $content 'async function toggleModelStatus\(id, nextStatus\) \{.*?\n\}\r?\n\r?\nasync function fetchUpstreamModels\(\) \{' @'
async function toggleModelStatus(id, nextStatus) {
    await fetchJson(`/admin/models/${id}/status`, {
        method: "PUT",
        body: { status: nextStatus }
    });
    await Promise.all([loadModels(), loadOverview()]);
    showToast("Model status updated");
}

async function deleteModel(id, modelCode) {
    const confirmed = window.confirm(`Delete model ${modelCode || id}? This will remove its price and route configuration.`);
    if (!confirmed) {
        return;
    }
    await fetchJson(`/admin/models/${id}`, {
        method: "DELETE"
    });
    if (String(state.editingModelId) === String(id)) {
        resetModelForm();
    }
    await Promise.all([loadModels(), loadOverview()]);
    showToast("Model deleted");
}

async function fetchUpstreamModels() {
'@

$content = Replace-Block $content '    elements\.usersTable\.addEventListener\("click", handleAction\(async \(event\) => \{.*?\n    \}\)\);\r?\n\r?\n    elements\.profileCardBody\.addEventListener\("click", handleAction\(async \(event\) => \{' @'
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

    elements.profileCardBody.addEventListener("click", handleAction(async (event) => {
'@

$content = Replace-Block $content '    elements\.modelsTable\.addEventListener\("click", handleAction\(async \(event\) => \{.*?\n    \}\)\);\r?\n\r?\n    elements\.modelProviderFilters\.addEventListener\("click", \(event\) => \{' @'
    elements.modelsTable.addEventListener("click", handleAction(async (event) => {
        const button = event.target.closest("button");
        if (button) {
            if (button.dataset.modelEdit) {
                startModelEdit(button.dataset.modelEdit);
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
        if (!button) {
            return;
        }
        if (button.dataset.modelEdit) {
            startModelEdit(button.dataset.modelEdit);
            return;
        }
        if (button.dataset.modelDelete) {
            await deleteModel(button.dataset.modelDelete, button.dataset.modelCode);
        }
    }));

    elements.modelProviderFilters.addEventListener("click", (event) => {
'@

Set-Content -Path $path -Value $content -Encoding UTF8
