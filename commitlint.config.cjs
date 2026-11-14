// Luật commit — TECH_STACK_VA_QUY_UOC.md mục 13.3
// <type>(<scope>)!: <subject> [#GNE-<số>]
module.exports = {
  extends: ['@commitlint/config-conventional'],
  // Commit / tiêu đề PR do Dependabot sinh ("build(deps-dev): Bump x from 1.0 to 1.1"): không đổi được
  // chữ hoa "Bump" và scope deps-dev → bỏ qua đúng mẫu này, mọi commit khác vẫn kiểm đủ luật.
  ignores: [(msg) => /^(build|ci)\(deps(-dev)?\): Bump \S+ from \S+ to \S+/.test(msg)],
  rules: {
    'type-enum': [2, 'always', [
      'feat', 'fix', 'perf', 'refactor', 'test', 'docs', 'build', 'ci', 'chore', 'revert',
    ]],
    'scope-enum': [2, 'always', [
      'auth', 'catalog', 'order', 'payment', 'vendor', 'shipping', 'promotion', 'social',
      'support', 'notification', 'admin', 'common', 'web', 'seller-ui', 'admin-ui',
      'ai-worker', 'db', 'deploy', 'deps', 'release', 'api',
    ]],
    'subject-case': [2, 'always', 'lower-case'],
    'subject-full-stop': [2, 'never', '.'],
    'header-max-length': [2, 'always', 100],
  },
};
