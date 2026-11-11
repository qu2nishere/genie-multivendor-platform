import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Đếm ngược theo giây (prototype auth/index.html useCountdown; Home / Checkout / UntilMidnight).
 * - `seconds`: số giây ban đầu; `running`: tạm dừng khi false.
 * - Tính theo mốc thời gian thật (Date.now) nên không trôi khi tab ngủ.
 */
export function useCountdown(seconds: number, running = true) {
  const [left, setLeft] = useState(seconds);
  const deadline = useRef<number>(Date.now() + seconds * 1000);
  const leftRef = useRef(left);
  leftRef.current = left;
  // tăng mỗi lần reset để khởi động lại bộ đếm kể cả khi đã về 0 (vd. "Gửi lại OTP")
  const [round, setRound] = useState(0);

  const reset = useCallback(
    (next: number = seconds) => {
      leftRef.current = next;
      setLeft(next);
      setRound((r) => r + 1);
    },
    [seconds],
  );

  useEffect(() => {
    if (!running) return undefined;
    deadline.current = Date.now() + leftRef.current * 1000;
    const tick = () => {
      const remain = Math.max(0, Math.round((deadline.current - Date.now()) / 1000));
      setLeft(remain);
      if (remain === 0) window.clearInterval(id);
    };
    const id = window.setInterval(tick, 1000);
    return () => window.clearInterval(id);
  }, [running, round]);

  return { left, done: left === 0, reset };
}

/** Số giây từ bây giờ đến nửa đêm giờ Việt Nam (Flash Sale "Kết thúc sau"). */
export function secondsUntilMidnightVN(now: number = Date.now()): number {
  const VN_OFFSET = 7 * 3600 * 1000;
  const local = now + VN_OFFSET;
  const nextMidnight = Math.floor(local / 86_400_000 + 1) * 86_400_000;
  return Math.max(0, Math.round((nextMidnight - local) / 1000));
}
