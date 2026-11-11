import { useEffect } from 'react';

let locks = 0;
let saved = { overflow: '', paddingRight: '' };

/** Khóa cuộn trang khi mở modal / ngăn kéo; đếm số khóa để nhiều lớp phủ lồng nhau không mở khóa sớm. */
export function useScrollLock(active: boolean): void {
  useEffect(() => {
    if (!active) return undefined;
    const body = document.body;
    if (locks === 0) {
      saved = { overflow: body.style.overflow, paddingRight: body.style.paddingRight };
      const gap = window.innerWidth - document.documentElement.clientWidth;
      body.style.overflow = 'hidden';
      if (gap > 0) body.style.paddingRight = `${gap}px`;
    }
    locks += 1;
    return () => {
      locks -= 1;
      if (locks === 0) {
        body.style.overflow = saved.overflow;
        body.style.paddingRight = saved.paddingRight;
      }
    };
  }, [active]);
}
