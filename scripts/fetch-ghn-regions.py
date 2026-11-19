#!/usr/bin/env python3
"""Tạo danh mục hành chính 2 cấp (Tỉnh / thành → Phường / xã / đặc khu, địa giới từ 01/07/2025) thành
server-spring/src/main/resources/regions/regions.json — nguồn dữ liệu khi backend chạy KHÔNG có GHN_TOKEN /
GHN_SHOP_ID (plan AUTH A12.01, D15, D25). Định dạng: [{id, name, wards: [{code, name}]}].

Hai nguồn:

1) GHN master-data v3 (mã khớp GHN — dùng khi đã có tài khoản GHN):
    GHN_TOKEN=<token> GHN_SHOP_ID=<shop id> python3 scripts/fetch-ghn-regions.py
    # môi trường thử của GHN:
    GHN_TOKEN=... GHN_SHOP_ID=... GHN_API_URL=https://dev-online-gateway.ghn.vn/shiip/public-api \\
        python3 scripts/fetch-ghn-regions.py

2) Danh mục chính thức (mã ĐVHC theo Quyết định 19/2025/QĐ-TTg) — tệp trong repo được tạo theo cách này, từ bộ dữ
   liệu MIT github.com/ThangLeQuoc/vietnamese-provinces-database (json/vn_only_simplified_json_generated_data_vn_units.json):
    python3 scripts/fetch-ghn-regions.py --official <đường dẫn tệp json đó>

Chỉ dùng thư viện chuẩn Python 3. Tỉnh và phường sắp theo tên (bỏ tiền tố "Tỉnh", "Thành phố", "Phường", "Xã", ...).
"""
import json
import os
import sys
import time
import unicodedata
import urllib.request

BASE = os.environ.get("GHN_API_URL", "https://online-gateway.ghn.vn/shiip/public-api").rstrip("/")
TOKEN = os.environ.get("GHN_TOKEN", "").strip()
SHOP_ID = os.environ.get("GHN_SHOP_ID", "").strip()
OUT = os.path.join(os.path.dirname(__file__), "..", "server-spring", "src", "main", "resources", "regions",
                   "regions.json")
PREFIXES = ("thanh pho ", "tinh ", "phuong ", "xa ", "dac khu ")


def sort_key(name):
    s = unicodedata.normalize("NFD", name.lower().replace("đ", "d"))
    s = "".join(c for c in s if not unicodedata.combining(c))
    for p in PREFIXES:
        if s.startswith(p):
            return s[len(p):]
    return s


def capitalize(name):
    return name[:1].upper() + name[1:]


def ghn(path):
    req = urllib.request.Request(BASE + path, headers={"Token": TOKEN, "ShopId": SHOP_ID,
                                                      "Accept": "application/json"})
    with urllib.request.urlopen(req, timeout=15) as res:
        body = json.load(res)
    return [row for row in (body.get("data") or []) if row.get("status", 1) == 1]


def from_ghn():
    if not TOKEN or not SHOP_ID:
        sys.exit("Thiếu GHN_TOKEN hoặc GHN_SHOP_ID (hoặc dùng --official <tệp>).")
    provinces = []
    for p in ghn("/v3/master-data/province/all?limit=200"):
        time.sleep(0.1)
        wards = [{"code": str(w["_id"]), "name": w["name"]}
                 for w in ghn(f"/v3/master-data/ward/all-by-province-id?limit=200&province_id={p['_id']}")]
        provinces.append({"id": p["_id"], "name": p["name"], "wards": wards})
        print(f"{p['name']}: {len(wards)} phường / xã", file=sys.stderr)
    return provinces


def from_official(path):
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    return [{"id": int(p["Code"]), "name": p["FullName"],
             "wards": [{"code": w["Code"], "name": capitalize(w["FullName"])} for w in p["Wards"]]}
            for p in data]


def main():
    if len(sys.argv) == 3 and sys.argv[1] == "--official":
        provinces = from_official(sys.argv[2])
    elif len(sys.argv) == 1:
        provinces = from_ghn()
    else:
        sys.exit(__doc__)
    provinces.sort(key=lambda p: sort_key(p["name"]))
    for p in provinces:
        p["wards"].sort(key=lambda w: sort_key(w["name"]))
    with open(OUT, "w", encoding="utf-8") as f:
        # mỗi tỉnh một dòng — diff git dễ đọc
        f.write("[\n" + ",\n".join(json.dumps(p, ensure_ascii=False, separators=(",", ":")) for p in provinces)
                + "\n]\n")
    wards = sum(len(p["wards"]) for p in provinces)
    print(f"Đã ghi {os.path.normpath(OUT)} ({len(provinces)} tỉnh / thành, {wards} phường / xã).")


if __name__ == "__main__":
    main()
