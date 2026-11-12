// eslint-plugin-boundaries: luật phụ thuộc tầng & feature — TECH_STACK mục 5.2
// ui → application → domain ← infrastructure; feature khác chỉ qua features/<x>/index.ts
const sameFeature = (type) => [type, { feature: '${from.feature}' }];

const DEEP_FEATURE_IMPORT = {
  group: [
    '@/features/*/domain',
    '@/features/*/domain/**',
    '@/features/*/application',
    '@/features/*/application/**',
    '@/features/*/infrastructure',
    '@/features/*/infrastructure/**',
    '@/features/*/ui',
    '@/features/*/ui/**',
  ],
  message:
    'Chỉ import feature qua "@/features/<x>" (index.ts). Trong cùng feature dùng đường dẫn tương đối (TECH_STACK 5.2).',
};

module.exports = {
  root: true,
  parser: '@typescript-eslint/parser',
  parserOptions: { ecmaVersion: 'latest', sourceType: 'module', ecmaFeatures: { jsx: true } },
  plugins: ['@typescript-eslint', 'boundaries', 'react-hooks'],
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:astro/recommended',
    'plugin:boundaries/recommended',
  ],
  settings: {
    'import/resolver': { typescript: { project: './tsconfig.json' } },
    // Thứ tự quan trọng: mẫu khớp trước thắng
    'boundaries/elements': [
      { type: 'env', pattern: 'src/env.d.ts', mode: 'file' },
      { type: 'middleware', pattern: 'src/middleware.ts', mode: 'file' },
      { type: 'pages', pattern: 'src/pages/**' },
      { type: 'layouts', pattern: 'src/layouts/**' },
      { type: 'app', pattern: 'src/app/**' },
      { type: 'i18n', pattern: 'src/i18n/**' },
      { type: 'styles', pattern: 'src/styles/**' },
      { type: 'assets', pattern: 'src/assets/**' },
      // index.ts: API công khai chung; server.ts: component .astro (chỉ trang / layout / .astro dùng)
      { type: 'features-public', pattern: 'src/features/*/{index,server}.ts', mode: 'file', capture: ['feature'] },
      { type: 'feature-domain', pattern: 'src/features/*/domain/**', capture: ['feature'] },
      { type: 'feature-application', pattern: 'src/features/*/application/**', capture: ['feature'] },
      { type: 'feature-infrastructure', pattern: 'src/features/*/infrastructure/**', capture: ['feature'] },
      // .astro trong ui: frontmatter SSR được lấy dữ liệu qua infrastructure của chính feature (TS 5.2)
      { type: 'feature-ui-astro', pattern: 'src/features/*/ui/**/*.astro', mode: 'file', capture: ['feature'] },
      { type: 'feature-ui', pattern: 'src/features/*/ui/**', capture: ['feature'] },
      { type: 'shared-lib', pattern: 'src/shared/lib/**' },
      { type: 'shared-hooks', pattern: 'src/shared/hooks/**' },
      { type: 'shared', pattern: 'src/shared/**' },
    ],
    'boundaries/ignore': ['**/*.test.ts', '**/*.test.tsx'],
  },
  rules: {
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_', caughtErrorsIgnorePattern: '^_' }],
    'react-hooks/rules-of-hooks': 'error',
    'react-hooks/exhaustive-deps': 'warn',
    'no-restricted-imports': ['error', { patterns: [DEEP_FEATURE_IMPORT] }],
    'boundaries/element-types': [
      'error',
      {
        default: 'disallow',
        message: '${file.type} không được import ${dependency.type} (TECH_STACK 5.2)',
        rules: [
          {
            from: 'pages',
            allow: ['features-public', 'layouts', 'shared', 'shared-lib', 'shared-hooks', 'app', 'i18n', 'assets'],
          },
          { from: 'middleware', allow: ['shared', 'shared-lib'] },
          {
            from: 'layouts',
            // app: island lớn (Header) cần Providers (QueryClient, locale, phiên)
            allow: ['features-public', 'layouts', 'shared', 'shared-lib', 'shared-hooks', 'styles', 'assets', 'app'],
          },
          { from: 'app', allow: ['features-public', 'shared', 'shared-lib', 'shared-hooks', 'app'] },
          {
            from: 'features-public',
            allow: [
              sameFeature('feature-domain'),
              sameFeature('feature-application'),
              sameFeature('feature-infrastructure'),
              sameFeature('feature-ui'),
              sameFeature('feature-ui-astro'),
            ],
          },
          {
            from: 'feature-ui',
            allow: [
              sameFeature('feature-application'),
              sameFeature('feature-domain'),
              sameFeature('feature-ui'),
              'shared',
              'shared-lib',
              'shared-hooks',
              'features-public',
              'assets',
            ],
          },
          {
            from: 'feature-ui-astro',
            allow: [
              sameFeature('feature-application'),
              sameFeature('feature-domain'),
              sameFeature('feature-ui'),
              sameFeature('feature-ui-astro'),
              sameFeature('feature-infrastructure'),
              'shared',
              'shared-lib',
              'shared-hooks',
              'features-public',
              'assets',
            ],
          },
          {
            from: 'feature-application',
            allow: [
              sameFeature('feature-domain'),
              sameFeature('feature-infrastructure'),
              sameFeature('feature-application'),
              'shared',
              'shared-lib',
              'shared-hooks',
            ],
          },
          {
            from: 'feature-infrastructure',
            allow: [sameFeature('feature-domain'), sameFeature('feature-infrastructure'), 'shared', 'shared-lib'],
          },
          { from: 'feature-domain', allow: [sameFeature('feature-domain'), 'shared-lib'] },
          { from: 'shared', allow: ['shared', 'shared-lib', 'shared-hooks', 'assets'] },
          { from: 'shared-hooks', allow: ['shared-hooks', 'shared-lib', 'shared'] },
          { from: 'shared-lib', allow: ['shared-lib', 'i18n'] },
        ],
      },
    ],
  },
  overrides: [
    {
      files: ['*.astro'],
      parser: 'astro-eslint-parser',
      parserOptions: { parser: '@typescript-eslint/parser', extraFileExtensions: ['.astro'] },
    },
    {
      // Mọi file trong src phải thuộc một element; import tới file không thuộc element nào bị chặn
      files: ['src/**/*.{ts,tsx,astro}'],
      excludedFiles: ['**/*.test.ts', '**/*.test.tsx'],
      rules: {
        'boundaries/no-unknown-files': 'error',
        'boundaries/no-unknown': 'error',
      },
    },
    {
      // Island React không được kéo component .astro (server.ts) vào bundle trình duyệt
      files: ['src/**/*.tsx'],
      rules: {
        'no-restricted-imports': [
          'error',
          {
            patterns: [
              DEEP_FEATURE_IMPORT,
              { group: ['@/features/*/server'], message: 'server.ts chỉ dành cho .astro (SSR).' },
            ],
          },
        ],
      },
    },
    {
      files: ['src/env.d.ts'],
      rules: { '@typescript-eslint/triple-slash-reference': 'off' },
    },
    {
      // Test & cấu hình: nới luật boundaries, giữ no-explicit-any
      files: ['**/*.test.ts', '**/*.test.tsx', 'tests/**', '*.config.*', '*.cjs', '*.mjs'],
      rules: {
        'boundaries/element-types': 'off',
        'boundaries/no-unknown': 'off',
        'boundaries/no-unknown-files': 'off',
        'boundaries/no-private': 'off',
        'boundaries/entry-point': 'off',
        'no-restricted-imports': 'off',
        '@typescript-eslint/no-explicit-any': 'error',
      },
    },
    {
      files: ['*.cjs'],
      env: { node: true },
      rules: { '@typescript-eslint/no-require-imports': 'off', 'no-undef': 'off' },
    },
  ],
  ignorePatterns: ['dist/', 'node_modules/', '.astro/', 'playwright-report/', 'test-results/', 'public/'],
};
