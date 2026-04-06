export function getRetryAfterSeconds(errorLike: unknown): number | undefined {
  const details = (errorLike as any)?.details;
  if (!Array.isArray(details)) return undefined;

  const retryInfo = details.find((d: any) => d?.["@type"]?.includes("RetryInfo"));
  const retryDelay = retryInfo?.retryDelay;
  if (typeof retryDelay !== "string") return undefined;

  const seconds = Number.parseInt(retryDelay.replace("s", ""), 10);
  return Number.isFinite(seconds) ? seconds : undefined;
}

export function normalizeChatError(error: unknown) {
  const errorObj = (error as any)?.error ?? (error as any)?.cause ?? error;
  const code = (errorObj as any)?.code;
  const statusText = (errorObj as any)?.status;
  const message =
    (errorObj as any)?.message ||
    (error as any)?.message ||
    "AI advisor request failed unexpectedly.";

  const isQuota =
    code === 429 ||
    statusText === "RESOURCE_EXHAUSTED" ||
    /quota|rate.?limit|resource_exhausted/i.test(message);

  const retryAfterSeconds = getRetryAfterSeconds(errorObj);

  return {
    status: isQuota ? 429 : 500,
    message: isQuota
      ? `Gemini quota exceeded. ${retryAfterSeconds ? `Please retry in ${retryAfterSeconds}s.` : "Please retry shortly."}`
      : message,
    providerMessage: message,
    retryAfterSeconds,
  };
}
