import pino from "pino";

const transport =
  process.env.NODE_ENV === "development"
    ? pino.transport({
      target: "pino-pretty",
      options: {
        colorize: true,
      },
    })
    : undefined;

const logger = pino(
  {
    level: process.env.NODE_ENV === "development" ? "debug" : "info",
  },
  transport,
);

export default logger;
