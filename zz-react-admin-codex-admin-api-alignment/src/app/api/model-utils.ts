import type { ModelItem, ModelListItemResponse } from "./types";

function normalizeModelCategories(modelType: string | null, providerType: string | null): string[] {
  const raw = `${modelType ?? ""} ${providerType ?? ""}`.toLowerCase();
  const categories = new Set<string>();

  if (raw.includes("chat") || raw.includes("text")) {
    categories.add("对话");
  }
  if (raw.includes("reason")) {
    categories.add("推理");
  }
  if (raw.includes("code")) {
    categories.add("代码");
  }
  if (raw.includes("vision") || raw.includes("image") || raw.includes("multimodal")) {
    categories.add("多模态");
  }
  if (raw.includes("embed")) {
    categories.add("嵌入");
  }

  if (categories.size === 0) {
    categories.add("对话");
  }

  return [...categories];
}

function normalizeProviderName(item: ModelListItemResponse): string {
  return item.providerName?.trim() || item.providerType?.trim() || "未知厂商";
}

function normalizeBooleanFlag(value: ModelListItemResponse["isPublic"]): boolean {
  return value === true || value === 1;
}

function normalizeStatus(status: string | null | undefined): string {
  return (status ?? "").trim().toUpperCase();
}

export function mapModelListItemToCatalog(item: ModelListItemResponse): ModelItem {
  const provider = normalizeProviderName(item);
  const categories = normalizeModelCategories(item.modelType, item.providerType);
  const promptPrice = item.promptPrice ?? item.requestPrice ?? 0;
  const cachedPromptPrice = item.cachedPromptPrice ?? 0;
  const completionPrice = item.completionPrice ?? item.requestPrice ?? 0;
  const status = normalizeStatus(item.status);
  const tags: string[] = [];

  if (normalizeBooleanFlag(item.isPublic)) {
    tags.push("公开");
  }
  if (item.billingType) {
    tags.push(item.billingType);
  }
  if (item.multiplier && item.multiplier !== 1) {
    tags.push(`倍率 x${item.multiplier}`);
  }

  return {
    id: item.id,
    name: item.modelName || item.modelCode,
    provider,
    category: categories,
    contextLength: 0,
    inputPrice: promptPrice,
    cachedInputPrice: cachedPromptPrice,
    outputPrice: completionPrice,
    latency: "—",
    available: status === "ACTIVE" || status === "ENABLED",
    tags,
    desc: item.upstreamModel || item.modelCode,
    hot: normalizeBooleanFlag(item.isPublic),
    isNew: false,
  };
}

export function isUserVisibleModel(item: ModelListItemResponse): boolean {
  const status = normalizeStatus(item.status);
  return normalizeBooleanFlag(item.isPublic) && (status === "ACTIVE" || status === "ENABLED");
}
