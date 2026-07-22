import * as React from "react";

export type ThemeMode = "LIGHT" | "DARK";

export type ThemeContextValue = {
  themeMode: ThemeMode;
  isDark: boolean;
  setThemeMode: (mode: ThemeMode) => void;
  toggleThemeMode: () => void;
};

export const ThemeContext = React.createContext<ThemeContextValue | null>(null);

export function useThemeMode() {
  const context = React.useContext(ThemeContext);

  if (!context) {
    throw new Error("useThemeMode must be used within ThemeProvider");
  }

  return context;
}
