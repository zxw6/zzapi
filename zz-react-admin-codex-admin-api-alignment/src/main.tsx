
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./app/App.tsx";
import "./styles/index.css";
import { AppQueryProvider } from "./app/providers/query-provider";
import { AuthProvider } from "./app/auth/auth-context";
import { ThemeProvider } from "./app/providers/theme-provider";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <AppQueryProvider>
      <ThemeProvider>
        <AuthProvider>
          <App />
        </AuthProvider>
      </ThemeProvider>
    </AppQueryProvider>
  </StrictMode>,
);
  
