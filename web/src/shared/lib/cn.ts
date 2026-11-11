// Ghép lớp CSS có điều kiện (plan G04.01) — thay các đoạn [..].filter(Boolean).join(' ') của prototype.
export type ClassValue =
  string | number | false | null | undefined | ClassDictionary | ClassValue[];
export interface ClassDictionary {
  [cls: string]: boolean | null | undefined;
}

export function cn(...inputs: ClassValue[]): string {
  const out: string[] = [];
  const push = (v: ClassValue): void => {
    if (v === null || v === undefined || v === false || v === '') return;
    if (typeof v === 'string' || typeof v === 'number') {
      out.push(String(v));
    } else if (Array.isArray(v)) {
      v.forEach(push);
    } else {
      for (const [k, on] of Object.entries(v)) if (on) out.push(k);
    }
  };
  inputs.forEach(push);
  return out.join(' ');
}
