// eslint-plugin-boundaries: luật phụ thuộc tầng & feature — TECH_STACK mục 5.2
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
    'boundaries/elements': [
      { type: 'pages', pattern: 'src/pages/**' },
      { type: 'layouts', pattern: 'src/layouts/**' },
      { type: 'app', pattern: 'src/app/**' },
      { type: 'features-public', pattern: 'src/features/*/index.ts', mode: 'file' },
      { type: 'feature-domain', pattern: 'src/features/*/domain/**', capture: ['feature'] },
      { type: 'feature-application', pattern: 'src/features/*/application/**', capture: ['feature'] },
      { type: 'feature-infrastructure', pattern: 'src/features/*/infrastructure/**', capture: ['feature'] },
      { type: 'feature-ui', pattern: 'src/features/*/ui/**', capture: ['feature'] },
      { type: 'shared-lib', pattern: 'src/shared/lib/**' },
      { type: 'shared', pattern: 'src/shared/**' },
    ],
  },
  rules: {
    '@typescript-eslint/no-explicit-any': 'error',
    'react-hooks/rules-of-hooks': 'error',
    'react-hooks/exhaustive-deps': 'warn',
    'boundaries/element-types': ['error', {
      default: 'disallow',
      rules: [
        { from: 'pages', allow: ['features-public', 'layouts', 'shared', 'shared-lib', 'app', 'feature-infrastructure'] },
        { from: 'layouts', allow: ['features-public', 'shared', 'shared-lib'] },
        { from: 'app', allow: ['features-public', 'shared', 'shared-lib', 'app'] },
        { from: 'features-public', allow: ['feature-domain', 'feature-application', 'feature-infrastructure', 'feature-ui'] },
        { from: 'feature-ui', allow: ['feature-application', 'feature-domain', 'feature-ui', 'shared', 'shared-lib', 'features-public'] },
        { from: 'feature-application', allow: ['feature-domain', 'feature-infrastructure', 'feature-application', 'shared', 'shared-lib'] },
        { from: 'feature-infrastructure', allow: ['feature-domain', 'feature-infrastructure', 'shared', 'shared-lib'] },
        { from: 'feature-domain', allow: ['feature-domain', 'shared-lib'] },
        { from: 'shared', allow: ['shared', 'shared-lib'] },
        { from: 'shared-lib', allow: ['shared-lib'] },
      ],
    }],
  },
  overrides: [
    {
      files: ['*.astro'],
      parser: 'astro-eslint-parser',
      parserOptions: { parser: '@typescript-eslint/parser', extraFileExtensions: ['.astro'] },
    },
  ],
  ignorePatterns: ['dist/', 'node_modules/', '.astro/'],
};
