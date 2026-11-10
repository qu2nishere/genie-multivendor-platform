// output: 'server', adapter @astrojs/node, integrations: react, tailwind (TECH_STACK mục 2.4)
import { defineConfig } from 'astro/config';
import node from '@astrojs/node';
import react from '@astrojs/react';
import tailwind from '@astrojs/tailwind';

export default defineConfig({
  site: process.env.PUBLIC_WEB_URL ?? 'http://localhost:4321',
  output: 'server',
  adapter: node({ mode: 'standalone' }),
  integrations: [react(), tailwind({ applyBaseStyles: false })],
  server: { host: true, port: 4321 },
});
