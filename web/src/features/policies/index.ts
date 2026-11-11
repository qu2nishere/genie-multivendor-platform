// API công khai của feature "policies" (trang chính sách tĩnh).
export type { PolicyGroup, PolicyPage } from './domain/policies.types';
export { policyHref } from './domain/policies.types';
export { neighbours, isKnownPolicy } from './domain/policies.rules';
export { listPolicyGroups, getPolicy, allPolicySlugs } from './infrastructure/policies.api';
