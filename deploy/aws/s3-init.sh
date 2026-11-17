#!/usr/bin/env bash
# Tạo kho tệp production trên AWS S3 — tương đương deploy/silo/init.sh của máy dev (ADR 0010, LUU_TRU_DU_LIEU.md mục 5).
# Chạy MỘT lần từ máy có AWS CLI v2 đã đăng nhập bằng tài khoản quản trị (không phải IAM user của ứng dụng):
#   ./deploy/aws/s3-init.sh genie-prod- ap-southeast-1 https://genie.vn,https://www.genie.vn
# Chạy lại an toàn: bucket đã có thì bỏ qua bước tạo, các cấu hình còn lại được ghi đè bằng cùng giá trị.
set -euo pipefail

PREFIX=${1:?Cần tiền tố bucket, vd. genie-prod- (trùng STORAGE_BUCKET_PREFIX trong deploy/.env)}
REGION=${2:-ap-southeast-1}
ORIGINS=${3:?Cần origin web được tải tệp lên, vd. https://genie.vn,https://www.genie.vn}

# Bucket công khai: ai cũng xem được tệp (ảnh sản phẩm, đại diện, đánh giá, banner) — trừ thư mục tmp/
PUBLIC_BUCKETS="product-media avatar-assets review-media content-assets"
# Bucket riêng tư: chỉ xem qua link có chữ ký 5 phút
PRIVATE_BUCKETS="chat-media product-docs refund-evidence kyc-dossiers ml-models"

origins_json=$(printf '%s' "$ORIGINS" | awk -F, '{for(i=1;i<=NF;i++){printf "%s\"%s\"", (i>1?",":""), $i}}')

for b in $PUBLIC_BUCKETS $PRIVATE_BUCKETS; do
  bucket="${PREFIX}${b}"
  if aws s3api head-bucket --bucket "$bucket" 2>/dev/null; then
    echo "Đã có $bucket"
  else
    aws s3api create-bucket --bucket "$bucket" --region "$REGION" \
      --create-bucket-configuration LocationConstraint="$REGION" >/dev/null
    echo "Tạo $bucket"
  fi

  # Mã hóa phía máy chủ mặc định (SSE-S3)
  aws s3api put-bucket-encryption --bucket "$bucket" --server-side-encryption-configuration \
    '{"Rules":[{"ApplyServerSideEncryptionByDefault":{"SSEAlgorithm":"AES256"}}]}'

  # Xóa tệp tạm tmp/ sau 1 ngày (như Silo)
  aws s3api put-bucket-lifecycle-configuration --bucket "$bucket" --lifecycle-configuration \
    '{"Rules":[{"ID":"expire-tmp","Status":"Enabled","Filter":{"Prefix":"tmp/"},"Expiration":{"Days":1}}]}'

  # CORS: trình duyệt PUT thẳng lên S3 bằng Presigned URL (Silo cho phép sẵn, S3 thì phải khai)
  aws s3api put-bucket-cors --bucket "$bucket" --cors-configuration \
    "{\"CORSRules\":[{\"AllowedOrigins\":[${origins_json}],\"AllowedMethods\":[\"PUT\",\"GET\",\"HEAD\"],\"AllowedHeaders\":[\"*\"],\"ExposeHeaders\":[\"ETag\"],\"MaxAgeSeconds\":3000}]}"
done

for b in $PRIVATE_BUCKETS; do
  # Chặn hoàn toàn truy cập công khai
  aws s3api put-public-access-block --bucket "${PREFIX}${b}" --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
done
# Hồ sơ KYC: bật lưu phiên bản để không mất tệp khi ghi đè / xóa nhầm
aws s3api put-bucket-versioning --bucket "${PREFIX}kyc-dossiers" --versioning-configuration Status=Enabled

for b in $PUBLIC_BUCKETS; do
  bucket="${PREFIX}${b}"
  # Cho phép chính sách bucket công khai (vẫn chặn ACL công khai)
  aws s3api put-public-access-block --bucket "$bucket" --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=false,RestrictPublicBuckets=false
  # Ai cũng được ĐỌC, trừ tmp/ (tệp chưa xác nhận)
  aws s3api put-bucket-policy --bucket "$bucket" --policy "{
    \"Version\": \"2012-10-17\",
    \"Statement\": [{
      \"Sid\": \"PublicReadExceptTmp\",
      \"Effect\": \"Allow\",
      \"Principal\": \"*\",
      \"Action\": \"s3:GetObject\",
      \"NotResource\": \"arn:aws:s3:::${bucket}/tmp/*\"
    }]
  }"
done

echo "Xong. Gắn deploy/aws/iam-policy.json (thay ${PREFIX}) cho IAM user của ứng dụng, điền khóa vào deploy/.env."
