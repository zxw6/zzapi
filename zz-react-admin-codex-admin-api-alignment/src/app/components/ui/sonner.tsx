import type { CSSProperties } from "react";
import { Toaster as Sonner, ToasterProps } from "sonner";
import { useThemeMode } from "../../providers/theme-context";

const Toaster = ({ ...props }: ToasterProps) => {
  const { isDark } = useThemeMode();

  return (
    <Sonner
      theme={isDark ? "dark" : "light"}
      richColors
      closeButton
      position="top-right"
      className="toaster group"
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
        } as CSSProperties
      }
      {...props}
    />
  );
};

export { Toaster };
