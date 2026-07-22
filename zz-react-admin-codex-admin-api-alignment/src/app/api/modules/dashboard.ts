import { request } from "../../../lib/http/client";
import { DASHBOARD_API_ENDPOINTS } from "../endpoints";
import type {
  DashboardModelStatResponse,
  DashboardOverviewResponse,
  DashboardTrendPointResponse,
} from "../types";

export const dashboardApi = {
  overview: () => request<DashboardOverviewResponse>(DASHBOARD_API_ENDPOINTS.overview),
  trend: (days = 7) =>
    request<DashboardTrendPointResponse[]>(DASHBOARD_API_ENDPOINTS.trend(days)),
  modelStats: () => request<DashboardModelStatResponse[]>(DASHBOARD_API_ENDPOINTS.modelStats),
};
