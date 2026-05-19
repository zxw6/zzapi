import * as React from "react";
import { readSiteBranding, saveSiteBranding, useSiteBranding } from "../site-settings/site-branding";
import { ThemeContext, type ThemeContextValue, type ThemeMode } from "./theme-context";

function applyTheme(mode: ThemeMode) {
  if (typeof document === "undefined") {
    return;
  }

  const root = document.documentElement;
  root.classList.toggle("dark", mode === "DARK");
  root.style.colorScheme = mode === "DARK" ? "dark" : "light";
}

function applyDocumentTitle(siteName: string) {
  if (typeof document === "undefined") {
    return;
  }

  document.title = siteName.trim() || "API Hub 中转站";
}

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const { branding } = useSiteBranding();
  const [themeMode, setThemeModeState] = React.useState<ThemeMode>(() => readSiteBranding().themeMode);

  React.useEffect(() => {
    setThemeModeState(branding.themeMode);
  }, [branding.themeMode]);

  React.useEffect(() => {
    applyTheme(themeMode);
  }, [themeMode]);

  React.useEffect(() => {
    applyDocumentTitle(branding.siteName);
  }, [branding.siteName]);

  const setThemeMode = React.useCallback(
    (mode: ThemeMode) => {
      setThemeModeState(mode);
      saveSiteBranding({
        ...branding,
        themeMode: mode,
      });
    },
    [branding],
  );

  const toggleThemeMode = React.useCallback(() => {
    setThemeMode(themeMode === "DARK" ? "LIGHT" : "DARK");
  }, [setThemeMode, themeMode]);

  const value = React.useMemo<ThemeContextValue>(
    () => ({
      themeMode,
      isDark: themeMode === "DARK",
      setThemeMode,
      toggleThemeMode,
    }),
    [setThemeMode, themeMode, toggleThemeMode],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}
