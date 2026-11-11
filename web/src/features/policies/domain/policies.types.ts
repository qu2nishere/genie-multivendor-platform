// Trang chính sách tĩnh (prototype Pages.jsx + pages-data.js) — plan G08.41.
export interface PolicyPage {
  slug: string;
  name: string;
  title: string;
  html: string;
  groupSlug: string;
}

export interface PolicyGroup {
  slug: string;
  title: string;
  pages: Pick<PolicyPage, 'slug' | 'name'>[];
}

export const policyHref = (slug: string): string => `/policies/${slug}`;
