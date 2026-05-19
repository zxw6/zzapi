export type ApiKeyStatusTone = {
  label: string;
  className: string;
};

export function normalizeApiKeyStatus(status: string | null | undefined): string {
  return (status ?? "").trim().toUpperCase();
}

export function getApiKeyStatusTone(status: string | null | undefined): ApiKeyStatusTone {
  const normalized = normalizeApiKeyStatus(status);

  if (normalized === "ACTIVE" || normalized === "ENABLED") {
    return {
      label: "正常",
      className: "border-emerald-200 bg-emerald-50 text-emerald-600",
    };
  }

  if (normalized === "EXPIRED") {
    return {
      label: "已过期",
      className: "border-red-200 bg-red-50 text-red-600",
    };
  }

  return {
    label: "已禁用",
    className: "border-slate-200 bg-slate-100 text-slate-600",
  };
}

export function getNextApiKeyStatus(status: string | null | undefined): string {
  const normalized = normalizeApiKeyStatus(status);
  return normalized === "ACTIVE" || normalized === "ENABLED" ? "DISABLED" : "ACTIVE";
}

export function formatApiKeyDate(value: string | null | undefined): string {
  if (!value) {
    return "未记录";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString("zh-CN", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

export function formatQuota(value: number | null | undefined): string {
  if (value === null || value === undefined) {
    return "不限";
  }

  return String(value);
}

export function normalizeApiKeyExpiresAt(value: string): string | undefined {
  if (!value) {
    return undefined;
  }

  const normalized = value.trim();
  if (!normalized) {
    return undefined;
  }

  const [datePart, timePart = "00:00"] = normalized.split("T");
  if (!datePart) {
    return normalized;
  }

  const [hour = "00", minute = "00", second = "00"] = timePart.split(":");
  return `${datePart} ${hour}:${minute}:${second}`;
}
