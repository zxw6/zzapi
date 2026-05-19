import * as React from "react";
import { NavLink, type NavLinkProps, useHref, useLocation } from "react-router";
import { AnimatePresence, motion } from "motion/react";
import { Skeleton } from "../ui/skeleton";
import { cn } from "../ui/utils";
import { LoadingSpinner } from "../ui/feedback";
import { RouteTransitionContext, useRouteTransition } from "./route-transition-context";

const MIN_TRANSITION_MS = 1000;

function isModifiedEvent(event: React.MouseEvent<HTMLElement>) {
  return event.metaKey || event.altKey || event.ctrlKey || event.shiftKey;
}

export function RouteTransitionProvider({ children }: { children: React.ReactNode }) {
  const location = useLocation();
  const lastPathnameRef = React.useRef(location.pathname);
  const transitionStartedAtRef = React.useRef(0);
  const timerRef = React.useRef<number | null>(null);
  const [isTransitioning, setIsTransitioning] = React.useState(false);

  const clearPendingTimer = React.useCallback(() => {
    if (timerRef.current !== null) {
      window.clearTimeout(timerRef.current);
      timerRef.current = null;
    }
  }, []);

  const beginTransition = React.useCallback(
    (nextPath: string) => {
      if (!nextPath || nextPath === location.pathname) {
        return;
      }

      clearPendingTimer();
      transitionStartedAtRef.current = Date.now();
      setIsTransitioning(true);
    },
    [clearPendingTimer, location.pathname],
  );

  React.useEffect(() => {
    const previousPath = lastPathnameRef.current;
    if (previousPath === location.pathname) {
      return;
    }

    lastPathnameRef.current = location.pathname;

    if (!isTransitioning) {
      transitionStartedAtRef.current = Date.now();
      setIsTransitioning(true);
    }

    const elapsed = Date.now() - transitionStartedAtRef.current;
    const remaining = Math.max(MIN_TRANSITION_MS - elapsed, 0);

    clearPendingTimer();
    timerRef.current = window.setTimeout(() => {
      setIsTransitioning(false);
      timerRef.current = null;
    }, remaining);

    return clearPendingTimer;
  }, [clearPendingTimer, isTransitioning, location.pathname]);

  React.useEffect(
    () => () => {
      clearPendingTimer();
    },
    [clearPendingTimer],
  );

  const value = React.useMemo(
    () => ({
      isTransitioning,
      beginTransition,
    }),
    [beginTransition, isTransitioning],
  );

  return (
    <RouteTransitionContext.Provider value={value}>{children}</RouteTransitionContext.Provider>
  );
}
type TransitionNavLinkProps = NavLinkProps;

export function TransitionNavLink({ onClick, target, to, ...props }: TransitionNavLinkProps) {
  const href = useHref(to);
  const { beginTransition } = useRouteTransition();

  const handleClick = React.useCallback(
    (event: React.MouseEvent<HTMLAnchorElement>) => {
      onClick?.(event);

      if (
        event.defaultPrevented ||
        event.button !== 0 ||
        isModifiedEvent(event) ||
        (typeof target === "string" && target !== "_self")
      ) {
        return;
      }

      beginTransition(href);
    },
    [beginTransition, href, onClick, target],
  );

  return <NavLink {...props} onClick={handleClick} target={target} to={to} />;
}

export function RouteTransitionOutlet({
  children,
  className,
}: {
  children: React.ReactNode;
  className?: string;
}) {
  const location = useLocation();
  const { isTransitioning } = useRouteTransition();

  return (
    <div className={cn("relative min-h-full", className)}>
      <div
        aria-hidden={isTransitioning}
        className={cn("min-h-full", isTransitioning && "invisible")}
      >
        <AnimatePresence initial={false} mode="wait">
          <motion.div
            key={location.pathname}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            className="min-h-full"
            exit={{ opacity: 0, y: 10, filter: "blur(6px)" }}
            initial={{ opacity: 0, y: 18, filter: "blur(10px)" }}
            transition={{ duration: 0.26, ease: [0.22, 1, 0.36, 1] }}
          >
            {children}
          </motion.div>
        </AnimatePresence>
      </div>

      <AnimatePresence>
        {isTransitioning ? (
          <motion.div
            animate={{ opacity: 1, scale: 1, y: 0 }}
            className="pointer-events-none absolute inset-x-0 top-0 z-20 flex justify-center px-6 pt-8"
            exit={{ opacity: 0, scale: 0.96, y: 10 }}
            initial={{ opacity: 0, scale: 0.92, y: 18 }}
            transition={{ duration: 0.24, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="flex items-center gap-2 rounded-xl border border-sky-200/90 bg-white/96 px-4 py-2.5 text-sm font-semibold text-sky-700 shadow-[0_16px_40px_rgba(14,116,144,0.14)] ring-1 ring-white/80 backdrop-blur-md dark:border-sky-500/30 dark:bg-slate-900/94 dark:text-sky-200 dark:ring-slate-800/80 dark:shadow-[0_20px_50px_rgba(2,8,23,0.6)] md:text-base">
              <LoadingSpinner className="h-4 w-4 md:h-5 md:w-5" />
              <span>加载中 🔥🔥🔥🔥</span>
            </div>
          </motion.div>
        ) : null}
      </AnimatePresence>

      <AnimatePresence>
        {isTransitioning ? (
          <motion.div
            animate={{ opacity: 1, scaleX: 1 }}
            className="pointer-events-none absolute inset-x-0 top-0 z-10 h-1 origin-left overflow-hidden rounded-full"
            exit={{ opacity: 0 }}
            initial={{ opacity: 0, scaleX: 0.12 }}
            transition={{ duration: 0.5, ease: [0.22, 1, 0.36, 1] }}
          >
            <div className="h-full w-full bg-gradient-to-r from-sky-400 via-blue-500 to-cyan-400 shadow-[0_0_24px_rgba(59,130,246,0.35)]" />
          </motion.div>
        ) : null}
      </AnimatePresence>

      <AnimatePresence>
        {isTransitioning ? (
          <motion.div
            animate={{ opacity: 1 }}
            className="pointer-events-none absolute inset-0 z-[5] overflow-hidden"
            exit={{ opacity: 0 }}
            initial={{ opacity: 0 }}
            transition={{ duration: 0.18, ease: "easeOut" }}
          >
            <div className="absolute inset-0 bg-gradient-to-b from-white via-white to-slate-50 dark:from-slate-950 dark:via-slate-950 dark:to-slate-900" />
            <div className="absolute inset-0 p-6">
              <div className="mx-auto flex h-full max-w-6xl flex-col gap-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="space-y-2">
                    <Skeleton className="h-7 w-36 rounded-xl" />
                    <Skeleton className="h-4 w-64 rounded-lg" />
                  </div>
                  <Skeleton className="h-10 w-28 rounded-xl" />
                </div>
                <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
                  {Array.from({ length: 4 }).map((_, index) => (
                    <div
                      key={index}
                      className="rounded-2xl border border-slate-200/80 bg-white p-4 shadow-sm shadow-slate-200/30 dark:border-slate-800/80 dark:bg-slate-900 dark:shadow-slate-950/40"
                    >
                      <div className="space-y-3">
                        <Skeleton className="h-4 w-20 rounded-lg" />
                        <Skeleton className="h-8 w-24 rounded-xl" />
                        <Skeleton className="h-3 w-28 rounded-lg" />
                      </div>
                    </div>
                  ))}
                </div>
                <div className="grid flex-1 grid-cols-1 gap-4 xl:grid-cols-[1.4fr_1fr]">
                  <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-200/30 dark:border-slate-800/80 dark:bg-slate-900 dark:shadow-slate-950/40">
                    <div className="mb-4 space-y-2">
                      <Skeleton className="h-5 w-28 rounded-lg" />
                      <Skeleton className="h-3 w-48 rounded-lg" />
                    </div>
                    <div className="grid h-[240px] grid-cols-7 items-end gap-3">
                      {Array.from({ length: 7 }).map((_, index) => (
                        <Skeleton
                          key={index}
                          className="w-full rounded-t-xl rounded-b-md"
                          style={{ height: `${108 + (index % 4) * 24}px` }}
                        />
                      ))}
                    </div>
                  </div>
                  <div className="rounded-2xl border border-slate-200/80 bg-white p-5 shadow-sm shadow-slate-200/30 dark:border-slate-800/80 dark:bg-slate-900 dark:shadow-slate-950/40">
                    <div className="mb-4 space-y-2">
                      <Skeleton className="h-5 w-24 rounded-lg" />
                      <Skeleton className="h-3 w-44 rounded-lg" />
                    </div>
                    <div className="space-y-3">
                      {Array.from({ length: 5 }).map((_, index) => (
                        <Skeleton key={index} className="h-4 w-full rounded-lg" />
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  );
}
