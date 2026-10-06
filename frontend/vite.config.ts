import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, each API path goes straight to its service. In Docker, the nginx gateway does this.
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      "/api/users": "http://localhost:8081",
      "/api/events": "http://localhost:8082",
      "/api/bookings": "http://localhost:8083",
    },
  },
});
