// Sổ địa chỉ + đơn vị hành chính (TanStack Query) — dùng ở Tài khoản › Địa chỉ và trang Thanh toán.
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import type { AddressInput } from '../domain/address.types';
import {
  createAddress,
  deleteAddress,
  listAddresses,
  listDistricts,
  listProvinces,
  listWards,
  reverseGeocode,
  setDefaultAddress,
  updateAddress,
} from '../infrastructure/address.api';

export const ADDRESSES_KEY = ['me', 'addresses'] as const;
const DAY = 24 * 60 * 60_000;

export function useAddresses(enabled = true) {
  return useQuery({ queryKey: ADDRESSES_KEY, queryFn: listAddresses, enabled });
}

/** Thêm (id null) hoặc sửa địa chỉ. */
export function useSaveAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: number | null; input: AddressInput }) =>
      id == null ? createAddress(input) : updateAddress(id, input),
    onSuccess: () => qc.invalidateQueries({ queryKey: ADDRESSES_KEY }),
  });
}

export function useSetDefaultAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: setDefaultAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: ADDRESSES_KEY }),
  });
}

export function useDeleteAddress() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: deleteAddress,
    onSuccess: () => qc.invalidateQueries({ queryKey: ADDRESSES_KEY }),
  });
}

// Danh sách hành chính nhỏ, cache 24 giờ (API_SPEC 3.9)
export const useProvinces = (enabled = true) =>
  useQuery({ queryKey: ['regions', 'p'], queryFn: listProvinces, staleTime: DAY, enabled });
export const useDistricts = (provinceId: number | null) =>
  useQuery({
    queryKey: ['regions', 'd', provinceId],
    queryFn: () => listDistricts(provinceId ?? 0),
    staleTime: DAY,
    enabled: provinceId != null,
  });
export const useWards = (districtId: number | null) =>
  useQuery({
    queryKey: ['regions', 'w', districtId],
    queryFn: () => listWards(districtId ?? 0),
    staleTime: DAY,
    enabled: districtId != null,
  });

export function useReverseGeocode() {
  return useMutation({
    mutationFn: ({ lat, lng }: { lat: number; lng: number }) => reverseGeocode(lat, lng),
  });
}
