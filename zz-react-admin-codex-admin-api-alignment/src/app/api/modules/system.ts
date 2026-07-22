import { request } from "../../../lib/http/client";
import { SYSTEM_API_ENDPOINTS } from "../endpoints";
import type { SiteSettingsResponse, SiteSettingsUpdateRequest } from "../types";

export const systemApi = {
  getSiteSettings: () => request<SiteSettingsResponse>(SYSTEM_API_ENDPOINTS.siteSettings),
  updateSiteSettings: (input: SiteSettingsUpdateRequest) =>
    request<null>(SYSTEM_API_ENDPOINTS.siteSettings, {
      method: "PUT",
      body: input,
    }),
};
