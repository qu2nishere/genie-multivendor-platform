import type { Config } from 'tailwindcss';

// Theme lấy từ token prototype (màu, chữ, bo góc, đổ bóng) — genie/tokens/*.css, DESIGN.md
// Breakpoint sm/md/lg, hỗ trợ 360–1440 px
export default {
  content: ['./src/**/*.{astro,html,ts,tsx,md}'],
  theme: {
    extend: {
      colors: {
        // TODO: map từ src/styles/tokens.css, ví dụ primary: 'var(--genie-color-primary)'
      },
      borderRadius: {},
      boxShadow: {},
      fontFamily: {},
    },
  },
  plugins: [],
} satisfies Config;
