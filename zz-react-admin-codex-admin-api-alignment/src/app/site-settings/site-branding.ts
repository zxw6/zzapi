import { useEffect, useMemo, useState } from "react";
import { useSiteSettingsQuery } from "../api/queries";
import type { SiteSettingsResponse } from "../api/types";

const STORAGE_KEY = "apihub.site.branding.v1";
const UPDATE_EVENT = "apihub:site-branding-updated";

export type SiteBranding = {
  siteName: string;
  siteDescription: string;
  footerText: string;
  themeMode: "LIGHT" | "DARK";
};

const defaultBranding: SiteBranding = {
  siteName: "API Hub 中转站",
  siteDescription: "企业级 AI API 中转管理平台",
  footerText: "Powered by API Hub",
  themeMode: "LIGHT",
};

function canUseStorage() {
  return typeof window !== "undefined" && typeof window.localStorage !== "undefined";
}

function normalizeBranding(input?: Partial<SiteBranding> | null): SiteBranding {
  return {
    siteName: input?.siteName?.trim() || defaultBranding.siteName,
    siteDescription: input?.siteDescription?.trim() || defaultBranding.siteDescription,
    footerText: input?.footerText?.trim() || defaultBranding.footerText,
    themeMode: input?.themeMode === "DARK" ? "DARK" : "LIGHT",
  };
}

export function readSiteBranding(): SiteBranding {
  if (!canUseStorage()) {
    return defaultBranding;
  }

  const raw = window.localStorage.getItem(STORAGE_KEY);
  if (!raw) {
    return defaultBranding;
  }

  try {
    return normalizeBranding(JSON.parse(raw) as Partial<SiteBranding>);
  } catch {
    window.localStorage.removeItem(STORAGE_KEY);
    return defaultBranding;
  }
}

export function saveSiteBranding(input: Partial<SiteBranding>) {
  const normalized = normalizeBranding(input);
  if (canUseStorage()) {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(normalized));
    window.dispatchEvent(new CustomEvent(UPDATE_EVENT, { detail: normalized }));
  }
  return normalized;
}

export function mapSiteSettingsToBranding(siteSettings: Pick<SiteSettingsResponse, "siteName" | "siteDescription" | "footerText" | "themeMode">): SiteBranding {
  return normalizeBranding({
    siteName: siteSettings.siteName,
    siteDescription: siteSettings.siteDescription ?? "",
    footerText: siteSettings.footerText ?? "",
    themeMode: siteSettings.themeMode,
  });
}

export function useSiteBranding(fetchRemote = false) {
  const [branding, setBranding] = useState<SiteBranding>(() => readSiteBranding());
  const siteSettingsQuery = useSiteSettingsQuery(fetchRemote);

  useEffect(() => {
    const handleUpdate = (event: Event) => {
      const customEvent = event as CustomEvent<SiteBranding>;
      setBranding(normalizeBranding(customEvent.detail));
    };

    window.addEventListener(UPDATE_EVENT, handleUpdate);
    return () => window.removeEventListener(UPDATE_EVENT, handleUpdate);
  }, []);

  useEffect(() => {
    if (!siteSettingsQuery.data) {
      return;
    }

    setBranding(saveSiteBranding(mapSiteSettingsToBranding(siteSettingsQuery.data)));
  }, [siteSettingsQuery.data]);

  return useMemo(
    () => ({
      branding,
      isLoading: fetchRemote ? siteSettingsQuery.isLoading : false,
      query: siteSettingsQuery,
    }),
    [branding, fetchRemote, siteSettingsQuery],
  );
}
