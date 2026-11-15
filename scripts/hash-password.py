#!/usr/bin/env python3
"""Sinh hash Argon2id (RFC 9106) dạng PHC mà Spring Security `Argon2PasswordEncoder` đọc được.

Dùng cho V13 (BOOTSTRAP_ADMIN_PASSWORD_HASH) và dữ liệu mẫu — không cần thư viện ngoài (chỉ hashlib.blake2b).
Tham số mặc định = plan AUTH D10 (OWASP 2024): m = 19456 KiB, t = 2, p = 1, salt 16 B, hash 32 B.

    python3 scripts/hash-password.py 'Genie@123'
    python3 scripts/hash-password.py --self-test        # kiểm bằng test vector RFC 9106 mục 5.3

Python thuần nên chậm (~vài chục giây / hash) — chỉ dùng sinh hash một lần, không dùng trong ứng dụng.
"""
import base64
import hashlib
import os
import struct
import sys

MASK = (1 << 64) - 1
SYNC_POINTS = 4
VERSION = 0x13
TYPE_ID = 2


def le32(n: int) -> bytes:
    return struct.pack("<I", n)


def blake2b(data: bytes, size: int) -> bytes:
    return hashlib.blake2b(data, digest_size=size).digest()


def h_prime(data: bytes, out_len: int) -> bytes:
    """Hàm băm độ dài thay đổi H' (RFC 9106 mục 3.3)."""
    if out_len <= 64:
        return blake2b(le32(out_len) + data, out_len)
    r = (out_len + 31) // 32 - 2
    v = blake2b(le32(out_len) + data, 64)
    out = [v[:32]]
    for _ in range(1, r):
        v = blake2b(v, 64)
        out.append(v[:32])
    out.append(blake2b(v, out_len - 32 * r))
    return b"".join(out)


def rotr(x: int, n: int) -> int:
    return ((x >> n) | (x << (64 - n))) & MASK


def gb(v, a, b, c, d):
    va, vb, vc, vd = v[a], v[b], v[c], v[d]
    va = (va + vb + 2 * (va & 0xFFFFFFFF) * (vb & 0xFFFFFFFF)) & MASK
    vd = rotr(vd ^ va, 32)
    vc = (vc + vd + 2 * (vc & 0xFFFFFFFF) * (vd & 0xFFFFFFFF)) & MASK
    vb = rotr(vb ^ vc, 24)
    va = (va + vb + 2 * (va & 0xFFFFFFFF) * (vb & 0xFFFFFFFF)) & MASK
    vd = rotr(vd ^ va, 16)
    vc = (vc + vd + 2 * (vc & 0xFFFFFFFF) * (vd & 0xFFFFFFFF)) & MASK
    vb = rotr(vb ^ vc, 63)
    v[a], v[b], v[c], v[d] = va, vb, vc, vd


def permute(v, idx):
    """P trên 16 từ 64 bit (chỉ số trong khối 128 từ)."""
    w = [v[i] for i in idx]
    gb(w, 0, 4, 8, 12); gb(w, 1, 5, 9, 13); gb(w, 2, 6, 10, 14); gb(w, 3, 7, 11, 15)
    gb(w, 0, 5, 10, 15); gb(w, 1, 6, 11, 12); gb(w, 2, 7, 8, 13); gb(w, 3, 4, 9, 14)
    for k, i in enumerate(idx):
        v[i] = w[k]


ROWS = [list(range(16 * r, 16 * r + 16)) for r in range(8)]
COLS = [[16 * r + 2 * c + k for r in range(8) for k in (0, 1)] for c in range(8)]


def compress(x, y):
    """G(X, Y) trên khối 1 KiB (128 từ)."""
    r = [a ^ b for a, b in zip(x, y)]
    q = list(r)
    for idx in ROWS:
        permute(q, idx)
    for idx in COLS:
        permute(q, idx)
    return [a ^ b for a, b in zip(q, r)]


def to_words(b: bytes):
    return list(struct.unpack("<128Q", b))


def to_bytes(w) -> bytes:
    return struct.pack("<128Q", *w)


def argon2id(password: bytes, salt: bytes, t: int, m: int, p: int, tag_len: int,
             secret: bytes = b"", ad: bytes = b"") -> bytes:
    h0 = blake2b(
        le32(p) + le32(tag_len) + le32(m) + le32(t) + le32(VERSION) + le32(TYPE_ID)
        + le32(len(password)) + password + le32(len(salt)) + salt
        + le32(len(secret)) + secret + le32(len(ad)) + ad, 64)
    m_prime = 4 * p * (m // (4 * p))
    q = m_prime // p
    seg = q // SYNC_POINTS
    zero = [0] * 128
    B = [[None] * q for _ in range(p)]
    for lane in range(p):
        B[lane][0] = to_words(h_prime(h0 + le32(0) + le32(lane), 1024))
        B[lane][1] = to_words(h_prime(h0 + le32(1) + le32(lane), 1024))

    for rnd in range(t):
        for sl in range(SYNC_POINTS):
            for lane in range(p):
                data_independent = rnd == 0 and sl < 2
                addresses = None
                counter = 0
                start = 2 if (rnd == 0 and sl == 0) else 0
                if data_independent:
                    def next_addresses(cnt):
                        z = [rnd, lane, sl, m_prime, t, TYPE_ID, cnt] + [0] * 121
                        return compress(zero, compress(zero, z))
                    counter = 1
                    addresses = next_addresses(counter)
                for i in range(start, seg):
                    j = sl * seg + i
                    prev = j - 1 if j > 0 else q - 1
                    if data_independent:
                        if i > 0 and i % 128 == 0:
                            counter += 1
                            addresses = next_addresses(counter)
                        rand = addresses[i % 128]
                    else:
                        rand = B[lane][prev][0]
                    j1 = rand & 0xFFFFFFFF
                    j2 = rand >> 32
                    ref_lane = lane if (rnd == 0 and sl == 0) else j2 % p
                    same = ref_lane == lane
                    if rnd == 0:
                        if sl == 0:
                            area = i - 1
                        elif same:
                            area = sl * seg + i - 1
                        else:
                            area = sl * seg + (-1 if i == 0 else 0)
                    else:
                        if same:
                            area = q - seg + i - 1
                        else:
                            area = q - seg + (-1 if i == 0 else 0)
                    rel = (j1 * j1) >> 32
                    rel = area - 1 - ((area * rel) >> 32)
                    start_pos = 0 if (rnd == 0 or sl == SYNC_POINTS - 1) else (sl + 1) * seg
                    ref_index = (start_pos + rel) % q
                    nb = compress(B[lane][prev], B[ref_lane][ref_index])
                    if rnd > 0:
                        nb = [a ^ b for a, b in zip(nb, B[lane][j])]
                    B[lane][j] = nb

    c = B[0][q - 1]
    for lane in range(1, p):
        c = [a ^ b for a, b in zip(c, B[lane][q - 1])]
    return h_prime(to_bytes(c), tag_len)


def b64(b: bytes) -> str:
    return base64.b64encode(b).decode().rstrip("=")


def encode(password: str, m: int = 19456, t: int = 2, p: int = 1, salt: bytes | None = None) -> str:
    salt = salt or os.urandom(16)
    tag = argon2id(password.encode("utf-8"), salt, t, m, p, 32)
    return f"$argon2id$v=19$m={m},t={t},p={p}${b64(salt)}${b64(tag)}"


def self_test() -> None:
    tag = argon2id(b"\x01" * 32, b"\x02" * 16, 3, 32, 4, 32, secret=b"\x03" * 8, ad=b"\x04" * 12)
    expected = bytes.fromhex("0d640df58d78766c08c037a34a8b53c9d01ef0452d75b65eb52520e96b01e659")
    if tag != expected:
        sys.exit(f"SELF-TEST FAILED: {tag.hex()}")
    print("RFC 9106 argon2id test vector: OK")


if __name__ == "__main__":
    if len(sys.argv) == 2 and sys.argv[1] == "--self-test":
        self_test()
    elif len(sys.argv) == 2:
        print(encode(sys.argv[1]))
    else:
        sys.exit(__doc__)
