import * as React from "react";

export type RouteTransitionContextValue = {
  isTransitioning: boolean;
  beginTransition: (nextPath: string) => void;
};

export const RouteTransitionContext = React.createContext<RouteTransitionContextValue | null>(null);

export function useRouteTransition() {
  const context = React.useContext(RouteTransitionContext);

  if (!context) {
    throw new Error("useRouteTransition must be used inside RouteTransitionProvider.");
  }

  return context;
}
