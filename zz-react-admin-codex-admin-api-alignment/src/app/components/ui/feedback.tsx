import * as React from "react";
import { AlertCircle, Inbox, LoaderCircle } from "lucide-react";
import { Button } from "./button";
import { Skeleton } from "./skeleton";
import { cn } from "./utils";

type LoadingSpinnerProps = {
  className?: string;
};

export function LoadingSpinner({ className }: LoadingSpinnerProps) {
  return <LoaderCircle className={cn("h-4 w-4 animate-spin", className)} />;
}

type ButtonLoadingContentProps = {
  loading: boolean;
  loadingText?: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  spinnerClassName?: string;
};

export function ButtonLoadingContent({
  loading,
  loadingText,
  children,
  className,
  spinnerClassName,
}: ButtonLoadingContentProps) {
  return (
    <span className={cn("inline-flex items-center gap-2", className)}>
      {loading ? <LoadingSpinner className={cn("h-4 w-4", spinnerClassName)} /> : null}
      <span>{loading && loadingText ? loadingText : children}</span>
    </span>
  );
}

type PageErrorStateProps = {
  message: string;
  onRetry?: () => void;
  className?: string;
  retryLabel?: string;
};

export function PageErrorState({
  message,
  onRetry,
  className,
  retryLabel = "重试",
}: PageErrorStateProps) {
  return (
    <div className={cn("space-y-4 p-6", className)}>
      <div className="rounded-2xl border border-red-200 bg-red-50/90 p-4 text-sm text-red-600 shadow-sm shadow-red-100/60 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-300 dark:shadow-red-950/30">
        <div className="flex items-start gap-3">
          <div className="rounded-xl bg-white/80 p-2 text-red-500 dark:bg-slate-900/70 dark:text-red-300">
            <AlertCircle className="h-4 w-4" />
          </div>
          <div className="space-y-1">
            <div className="font-medium text-red-700 dark:text-red-200">数据加载失败</div>
            <div>{message}</div>
          </div>
        </div>
      </div>
      {onRetry ? (
        <Button onClick={onRetry} size="sm">
          {retryLabel}
        </Button>
      ) : null}
    </div>
  );
}

type PageEmptyStateProps = {
  title?: string;
  description?: string;
  className?: string;
};

export function PageEmptyState({
  title = "暂无数据",
  description = "当前还没有可展示的数据。",
  className,
}: PageEmptyStateProps) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-slate-200 bg-white px-6 py-10 text-center shadow-sm dark:border-slate-800 dark:bg-slate-900",
        className,
      )}
    >
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-slate-100 dark:bg-slate-800">
        <Inbox className="h-5 w-5 text-slate-400 dark:text-slate-500" />
      </div>
      <div className="mt-4 text-base font-medium text-slate-900 dark:text-slate-100">{title}</div>
      <div className="mt-1 text-sm text-slate-500 dark:text-slate-400">{description}</div>
    </div>
  );
}

type PageHeaderSkeletonProps = {
  showAction?: boolean;
  className?: string;
};

export function PageHeaderSkeleton({ showAction = true, className }: PageHeaderSkeletonProps) {
  return (
    <div className={cn("flex items-start justify-between gap-4", className)}>
      <div className="space-y-2">
        <Skeleton className="h-7 w-40 rounded-xl" />
        <Skeleton className="h-4 w-72 max-w-[70vw] rounded-lg" />
      </div>
      {showAction ? <Skeleton className="h-10 w-28 rounded-xl" /> : null}
    </div>
  );
}

type PageCardGridSkeletonProps = {
  cards?: number;
  className?: string;
  gridClassName?: string;
};

export function PageCardGridSkeleton({
  cards = 4,
  className,
  gridClassName,
}: PageCardGridSkeletonProps) {
  return (
    <div
      className={cn(
        "grid gap-4",
        cards >= 4
          ? "grid-cols-1 md:grid-cols-2 xl:grid-cols-4"
          : "grid-cols-1 md:grid-cols-2 xl:grid-cols-3",
        gridClassName,
        className,
      )}
    >
      {Array.from({ length: cards }).map((_, index) => (
        <div
          key={index}
          className="rounded-2xl border border-slate-200/80 bg-white/90 p-4 shadow-sm shadow-slate-200/40 dark:border-slate-800/80 dark:bg-slate-900/90 dark:shadow-slate-950/40"
        >
          <div className="space-y-3">
            <Skeleton className="h-4 w-20 rounded-lg" />
            <Skeleton className="h-8 w-24 rounded-xl" />
            <Skeleton className="h-3 w-28 rounded-lg" />
          </div>
        </div>
      ))}
    </div>
  );
}

type PagePanelSkeletonProps = {
  lines?: number;
  className?: string;
  header?: boolean;
};

export function PagePanelSkeleton({ lines = 4, className, header = true }: PagePanelSkeletonProps) {
  return (
    <div
      className={cn(
        "rounded-2xl border border-slate-200/80 bg-white/90 p-5 shadow-sm shadow-slate-200/40 dark:border-slate-800/80 dark:bg-slate-900/90 dark:shadow-slate-950/40",
        className,
      )}
    >
      {header ? (
        <div className="mb-4 space-y-2">
          <Skeleton className="h-5 w-32 rounded-lg" />
          <Skeleton className="h-3 w-56 rounded-lg" />
        </div>
      ) : null}
      <div className="space-y-3">
        {Array.from({ length: lines }).map((_, index) => (
          <Skeleton key={index} className="h-4 w-full rounded-lg" />
        ))}
      </div>
    </div>
  );
}

type PageTableSkeletonProps = {
  rows?: number;
  columns?: number;
  className?: string;
};

export function PageTableSkeleton({ rows = 5, columns = 5, className }: PageTableSkeletonProps) {
  return (
    <div
      className={cn(
        "overflow-hidden rounded-2xl border border-slate-200/80 bg-white/90 shadow-sm shadow-slate-200/40 dark:border-slate-800/80 dark:bg-slate-900/90 dark:shadow-slate-950/40",
        className,
      )}
    >
      <div className="border-b border-slate-100 bg-slate-50/90 px-5 py-3 dark:border-slate-800 dark:bg-slate-950/70">
        <div
          className="grid gap-4"
          style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}
        >
          {Array.from({ length: columns }).map((_, index) => (
            <Skeleton key={index} className="h-3 w-16 rounded-lg" />
          ))}
        </div>
      </div>
      <div className="divide-y divide-slate-100 dark:divide-slate-800">
        {Array.from({ length: rows }).map((_, rowIndex) => (
          <div
            key={rowIndex}
            className="grid gap-4 px-5 py-4"
            style={{ gridTemplateColumns: `repeat(${columns}, minmax(0, 1fr))` }}
          >
            {Array.from({ length: columns }).map((_, columnIndex) => (
              <Skeleton
                key={columnIndex}
                className={cn(
                  "h-4 rounded-lg",
                  columnIndex === 0 ? "w-24" : columnIndex === columns - 1 ? "w-16" : "w-full",
                )}
              />
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}

type PageListSkeletonProps = {
  items?: number;
  className?: string;
};

export function PageListSkeleton({ items = 4, className }: PageListSkeletonProps) {
  return (
    <div className={cn("space-y-3", className)}>
      {Array.from({ length: items }).map((_, index) => (
        <div
          key={index}
          className="rounded-2xl border border-slate-200/80 bg-white/90 p-4 shadow-sm shadow-slate-200/40 dark:border-slate-800/80 dark:bg-slate-900/90 dark:shadow-slate-950/40"
        >
          <div className="flex items-start gap-3">
            <Skeleton className="h-10 w-10 rounded-xl" />
            <div className="flex-1 space-y-3">
              <div className="flex items-center gap-2">
                <Skeleton className="h-4 w-32 rounded-lg" />
                <Skeleton className="h-5 w-14 rounded-full" />
              </div>
              <Skeleton className="h-3 w-48 rounded-lg" />
              <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
                {Array.from({ length: 4 }).map((__, statIndex) => (
                  <Skeleton key={statIndex} className="h-4 w-full rounded-lg" />
                ))}
              </div>
            </div>
            <Skeleton className="h-8 w-16 rounded-lg" />
          </div>
        </div>
      ))}
    </div>
  );
}
