// Sản phẩm của shop (TanStack Query). Ghi xong làm mới mọi truy vấn ['seller', …] (Tổng quan đếm sản phẩm,
// tìm nhanh, danh sách).
import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect, useState } from 'react';
import type { ProductForm, ProductTab } from '../domain/product.types';
import {
  createBrand,
  createProposal,
  deleteProduct,
  getCategoryTree,
  getListingRules,
  getSellerProduct,
  listCatalog,
  listProposals,
  listSellerProducts,
  saveProduct,
  searchBrands,
  setProductHidden,
} from '../infrastructure/product.api';

const SELLER = ['seller'] as const;
const KEY = [...SELLER, 'products'] as const;

function useSellerMutation<V, R>(fn: (v: V) => Promise<R>) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: fn,
    onSuccess: () => qc.invalidateQueries({ queryKey: SELLER }),
  });
}

export const useProductList = (q: { tab: ProductTab; q?: string; page?: number }) =>
  useQuery({
    queryKey: [...KEY, 'list', q],
    queryFn: () => listSellerProducts({ ...q, size: 20 }),
    placeholderData: keepPreviousData,
  });

export const useSellerProduct = (id: number | null) =>
  useQuery({
    queryKey: [...KEY, 'detail', id],
    queryFn: () => getSellerProduct(id ?? 0),
    enabled: id != null,
    staleTime: Infinity,
  });

export const useSaveProduct = () =>
  useSellerMutation((v: { id: number | null; form: ProductForm; submit: boolean }) =>
    saveProduct(v.id, v.form, v.submit),
  );
export const useSetHidden = () =>
  useSellerMutation((v: { id: number; hidden: boolean }) => setProductHidden(v.id, v.hidden));
export const useDeleteProduct = () => useSellerMutation(deleteProduct);

export const useCategoryTree = () =>
  useQuery({ queryKey: ['catalog', 'seller-tree'], queryFn: getCategoryTree, staleTime: Infinity });

export const useListingRules = (categoryId: number | null) =>
  useQuery({
    queryKey: [...KEY, 'rules', categoryId],
    queryFn: () => getListingRules(categoryId ?? 0),
    enabled: !!categoryId,
    staleTime: Infinity,
  });

/** Tìm thương hiệu có trễ 200 ms. */
export function useBrandSearch(q: string, enabled: boolean) {
  const [term, setTerm] = useState(q);
  useEffect(() => {
    const id = setTimeout(() => setTerm(q), 200);
    return () => clearTimeout(id);
  }, [q]);
  return useQuery({
    queryKey: [...KEY, 'brands', term],
    queryFn: () => searchBrands(term),
    enabled,
    placeholderData: keepPreviousData,
  });
}
export const useCreateBrand = () => useSellerMutation(createBrand);

export const useCatalog = () =>
  useQuery({ queryKey: [...SELLER, 'catalog'], queryFn: listCatalog });
export const useProposals = () =>
  useQuery({ queryKey: [...SELLER, 'proposals'], queryFn: listProposals });
export const useCreateProposal = () => useSellerMutation(createProposal);
