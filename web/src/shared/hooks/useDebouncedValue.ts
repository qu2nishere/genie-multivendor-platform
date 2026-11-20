import { useEffect, useState } from 'react';

/** Trả về `value` sau khi nó đứng yên `delay` ms — dùng cho kiểm tra tức thời khi gõ (email, tìm kiếm…). */
export function useDebouncedValue<T>(value: T, delay = 400): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const id = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(id);
  }, [value, delay]);
  return debounced;
}
