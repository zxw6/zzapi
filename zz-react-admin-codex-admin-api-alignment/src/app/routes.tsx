import { createBrowserRouter, Navigate } from "react-router";
import { UserLoginPage } from "./pages/user/UserLogin";
import { UserLayout } from "./components/user/UserLayout";
import { OverviewPage } from "./pages/user/Overview";
import { UserKeysPage } from "./pages/user/UserKeys";
import { ModelsPage } from "./pages/user/Models";
import { BillingPage } from "./pages/user/Billing";
import { DocsPage } from "./pages/user/Docs";
import { AccountPage } from "./pages/user/Account";
import { RedirectIfAuthenticated, RequireAdminRole, RequireUserRole } from "./routes/guards";
import { Layout } from "./components/layout/Layout";
import { LoginPage } from "./pages/Login";
import { StatsPage } from "./pages/Stats";
import { LogsPage } from "./pages/Logs";
import { ChannelsPage } from "./pages/Channels";
import { ApiKeysPage } from "./pages/ApiKeys";
import { SettingsPage } from "./pages/Settings";
import { UsersPage } from "./pages/Users";
import { PackagePurchasesPage } from "./pages/PackagePurchases";
import { AmountFlowsPage } from "./pages/AmountFlows";
import { PackageCenterPage } from "./pages/PackageCenter";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: RedirectIfAuthenticated,
    children: [{ index: true, Component: UserLoginPage }],
  },
  {
    path: "/admin/login",
    Component: RedirectIfAuthenticated,
    children: [{ index: true, Component: LoginPage }],
  },
  {
    path: "/console",
    children: [
      {
        Component: RequireUserRole,
        children: [
          {
            Component: UserLayout,
            children: [
              { index: true, element: <Navigate to="/console/overview" replace /> },
              { path: "overview", Component: OverviewPage },
              { path: "keys", Component: UserKeysPage },
              { path: "models", Component: ModelsPage },
              { path: "billing", Component: BillingPage },
              { path: "docs", Component: DocsPage },
              { path: "account", Component: AccountPage },
            ],
          },
        ],
      },
    ],
  },
  {
    path: "/admin",
    children: [
      {
        Component: RequireAdminRole,
        children: [
          {
            Component: Layout,
            children: [
              { index: true, element: <Navigate to="/admin/stats" replace /> },
              { path: "stats", Component: StatsPage },
              { path: "logs", Component: LogsPage },
              { path: "channels", Component: ChannelsPage },
              { path: "package-center", Component: PackageCenterPage },
              { path: "package-purchases", Component: PackagePurchasesPage },
              { path: "amount-flows", Component: AmountFlowsPage },
              { path: "users", Component: UsersPage },
              { path: "keys", Component: ApiKeysPage },
              { path: "settings", Component: SettingsPage },
            ],
          },
        ],
      },
    ],
  },
  {
    path: "*",
    element: <Navigate to="/" replace />,
  },
]);
