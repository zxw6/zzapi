import { render, screen } from "@testing-library/react";
import { createMemoryRouter, RouterProvider } from "react-router";
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  RedirectIfAuthenticated,
  RequireAdminRole,
  RequireAuth,
  RequireUserRole,
} from "./guards";

const mockUseAuth = vi.fn();

vi.mock("../auth/auth-context", () => ({
  useAuth: () => mockUseAuth(),
}));

function createUser(role: "user" | "admin") {
  return {
    id: `u_${role}`,
    username: role,
    nickname: role === "admin" ? "管理员" : "普通用户",
    email: `${role}@apihub.io`,
    role,
    roleCode: role === "admin" ? "ADMIN" : "USER",
  };
}

describe("route guards", () => {
  beforeEach(() => {
    mockUseAuth.mockReset();
  });

  it("redirects unauthenticated users from /console to /", async () => {
    mockUseAuth.mockReturnValue({ status: "anonymous", user: null });

    const router = createMemoryRouter(
      [
        { path: "/", element: <div>login-page</div> },
        {
          path: "/console",
          Component: RequireAuth,
          children: [{ index: true, element: <div>console-page</div> }],
        },
      ],
      { initialEntries: ["/console"] },
    );

    render(<RouterProvider router={router} />);
    expect(await screen.findByText("login-page")).toBeInTheDocument();
  });

  it("allows authenticated users into user routes", async () => {
    mockUseAuth.mockReturnValue({ status: "authenticated", user: createUser("user") });

    const router = createMemoryRouter(
      [
        { path: "/", element: <div>login-page</div> },
        {
          path: "/console",
          Component: RequireUserRole,
          children: [{ index: true, element: <div>console-page</div> }],
        },
      ],
      { initialEntries: ["/console"] },
    );

    render(<RouterProvider router={router} />);
    expect(await screen.findByText("console-page")).toBeInTheDocument();
  });

  it("redirects admin users away from user routes", async () => {
    mockUseAuth.mockReturnValue({ status: "authenticated", user: createUser("admin") });

    const router = createMemoryRouter(
      [
        { path: "/admin/stats", element: <div>admin-page</div> },
        {
          path: "/console",
          Component: RequireUserRole,
          children: [{ index: true, element: <div>console-page</div> }],
        },
      ],
      { initialEntries: ["/console"] },
    );

    render(<RouterProvider router={router} />);
    expect(await screen.findByText("admin-page")).toBeInTheDocument();
  });

  it("allows authenticated admins into admin routes", async () => {
    mockUseAuth.mockReturnValue({ status: "authenticated", user: createUser("admin") });

    const router = createMemoryRouter(
      [
        { path: "/admin/login", element: <div>admin-login</div> },
        {
          path: "/admin",
          Component: RequireAdminRole,
          children: [{ index: true, element: <div>admin-page</div> }],
        },
      ],
      { initialEntries: ["/admin"] },
    );

    render(<RouterProvider router={router} />);
    expect(await screen.findByText("admin-page")).toBeInTheDocument();
  });

  it("redirects authenticated users away from login based on role", async () => {
    mockUseAuth.mockReturnValue({ status: "authenticated", user: createUser("admin") });

    const router = createMemoryRouter(
      [
        {
          path: "/",
          Component: RedirectIfAuthenticated,
          children: [{ index: true, element: <div>login-page</div> }],
        },
        {
          path: "/admin/stats",
          element: <div>admin-page</div>,
        },
      ],
      { initialEntries: ["/"] },
    );

    render(<RouterProvider router={router} />);
    expect(await screen.findByText("admin-page")).toBeInTheDocument();
  });
});
