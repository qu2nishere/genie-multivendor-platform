// Danh mục toàn sàn (TanStack Query). Mỗi thao tác làm mới mọi truy vấn ['admin', …].
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { CatalogProductTab, CategoryAttribute, CategoryInput } from '../domain/catalog.types';
import {
  approveProposal,
  createCategory,
  deleteCategory,
  getCategoryTree,
  listAttributes,
  listCatalogProducts,
  listProposals,
  rejectProposal,
  runProductCommand,
  saveAttribute,
  setAttributeActive,
  updateCategory,
  type ProductCommand,
} from '../infrastructure/catalog.api';

const KEY = ['admin', 'catalog'] as const;

function useAdminMutation<A, R>(fn: (a: A) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin'] }),
  });
}

export const useCategoryTree = () =>
  useQuery({ queryKey: [...KEY, 'tree'], queryFn: getCategoryTree });

export const useProposals = () =>
  useQuery({ queryKey: [...KEY, 'proposals'], queryFn: listProposals });

export const useAttributes = (categoryId: number) =>
  useQuery({ queryKey: [...KEY, 'attrs', categoryId], queryFn: () => listAttributes(categoryId) });

export const useCatalogProducts = (categoryId: number, tab: CatalogProductTab) =>
  useQuery({
    queryKey: [...KEY, 'products', categoryId, tab],
    queryFn: () => listCatalogProducts(categoryId, tab),
    placeholderData: keepPreviousData,
  });

export const useCreateCategory = () => useAdminMutation((c: CategoryInput) => createCategory(c));
export const useUpdateCategory = () =>
  useAdminMutation((a: { id: number; patch: Partial<CategoryInput> }) =>
    updateCategory(a.id, a.patch),
  );
export const useDeleteCategory = () => useAdminMutation((id: number) => deleteCategory(id));

export const useSaveAttribute = () =>
  useAdminMutation((a: { categoryId: number; attr: CategoryAttribute; editing: string | null }) =>
    saveAttribute(a.categoryId, a.attr, a.editing),
  );
export const useToggleAttribute = () =>
  useAdminMutation((a: { categoryId: number; code: string; active: boolean }) =>
    setAttributeActive(a.categoryId, a.code, a.active),
  );

export const useApproveProposal = () =>
  useAdminMutation((a: { id: number; commissionRate: number | null }) =>
    approveProposal(a.id, a.commissionRate),
  );
export const useRejectProposal = () =>
  useAdminMutation((a: { id: number; reason: string }) => rejectProposal(a.id, a.reason));

export const useProductCommand = () =>
  useAdminMutation((c: ProductCommand) => runProductCommand(c));
