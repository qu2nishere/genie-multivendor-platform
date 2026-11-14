// Form đăng bán / sửa sản phẩm (prototype Products.jsx Editor) — plan G11.23: react-hook-form giữ toàn bộ dữ
// liệu 7 khối; "Lưu nháp" không kiểm đủ trường, "Đăng bán / Lưu & gửi duyệt" kiểm (`publishIssues` + zod các
// ô nhập tay) rồi hỏi lại khi có phân loại giá 0 / sản phẩm đang bán phải duyệt lại; chờ duyệt → chỉ xem;
// rời trang khi đang sửa dở → hỏi lại. Khóa gửi duyệt khi shop chưa VERIFIED (API 10.5 403 GNE-VND-4031).
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm, useWatch } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { kycGroup, useSellerMe } from '@/features/seller-settings';
import { errorMessage, isApiError } from '@/shared/api/errors';
import { MQ } from '@/shared/hooks/useMediaQuery';
import { useT } from '@/shared/hooks/useT';
import { Button, ConfirmDialog, EmptyState, Icon, Skeleton, toast, buttonClass } from '@/shared/ui';
import {
  useListingRules,
  useSaveProduct,
  useSellerProduct,
} from '../../application/useSellerProducts';
import {
  emptyForm,
  isReadOnly,
  publishIssues,
  requiresReview,
  zeroVariants,
  type PublishIssue,
} from '../../domain/product.rules';
import { basicsSchema } from '../../domain/product.schema';
import type { ProductDetail, ProductForm } from '../../domain/product.types';
import { BasicInfo, RichDescription, SpecsTable } from './InfoSections';
import { ImageUpload, LegalDocs } from './MediaSections';
import { ShippingInfo, VariantSection } from './SaleSections';

const SECTION_OF: Record<PublishIssue, string> = {
  images: 'sec-images',
  documents: 'sec-docs',
  name: 'sec-basic',
  category: 'sec-basic',
  specs: 'sec-specs',
  description: 'sec-desc',
  price: 'sec-variants',
  variants_limit: 'sec-variants',
  weight: 'sec-shipping',
  pre_order: 'sec-shipping',
};
const ORDER: PublishIssue[] = [
  'images',
  'documents',
  'name',
  'category',
  'specs',
  'description',
  'price',
  'variants_limit',
  'weight',
  'pre_order',
];

function Editor({ product }: { product: ProductDetail | null }) {
  const { t } = useT();
  const navigate = useNavigate();
  const me = useSellerMe();
  const save = useSaveProduct();
  const initial = useMemo<ProductForm>(() => {
    if (!product) return emptyForm();
    const { id: _id, status: _s, rejectReason: _r, ...f } = product;
    return f;
  }, [product]);
  const form = useForm<ProductForm>({ defaultValues: initial });
  const path = useWatch({ control: form.control, name: 'categoryPath' });
  const rules = useListingRules(path.at(-1)?.id ?? null).data ?? null;
  const [issues, setIssues] = useState<PublishIssue[]>([]);
  const [ask, setAsk] = useState<null | 'zero' | 'review' | 'leave'>(null);
  const tried = useRef(false);
  const readOnly = isReadOnly(product?.status);
  const verified = me.data ? kycGroup(me.data.kycStatus) === 'VERIFIED' : false;
  const dirty = form.formState.isDirty;

  // Sau lần gửi đầu, cập nhật danh sách lỗi theo dữ liệu đang sửa
  const values = useWatch({ control: form.control });
  useEffect(() => {
    if (tried.current) setIssues(publishIssues(form.getValues(), rules));
  }, [values, rules, form]);

  // Rời tab / tải lại khi đang sửa dở
  useEffect(() => {
    if (!dirty || readOnly) return undefined;
    const h = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', h);
    return () => window.removeEventListener('beforeunload', h);
  }, [dirty, readOnly]);

  const back = () => navigate('/products');

  const doSave = (submit: boolean) => {
    const f = form.getValues();
    save.mutate(
      { id: product?.id ?? null, form: f, submit },
      {
        onSuccess: (r) => {
          form.reset(f);
          toast.success(
            !submit
              ? t('seller-products.editor.saved_draft')
              : r.status === 'PUBLISHED'
                ? t('seller-products.editor.saved_live')
                : t('seller-products.editor.submitted'),
          );
          back();
        },
        onError: (e) => {
          if (isApiError(e) && e.code === 'GNE-CAT-4222')
            setIssues((x) => [...new Set([...x, 'documents' as const])]);
          if (isApiError(e) && e.code === 'GNE-CAT-4223')
            setIssues((x) => [...new Set([...x, 'specs' as const])]);
          toast.error(errorMessage(e, t));
        },
      },
    );
  };

  const publish = (skip: { zero?: boolean; review?: boolean } = {}) => {
    tried.current = true;
    const f = form.getValues();
    const found = publishIssues(f, rules);
    const z = basicsSchema.safeParse({
      name: f.name,
      origin: f.origin,
      condition: f.condition,
      weightGrams: f.weightGrams,
      length: f.dims.length,
      width: f.dims.width,
      height: f.dims.height,
      shippingPayer: f.shippingPayer,
      freeShippingThreshold: f.freeShippingThreshold,
      isPreOrder: f.isPreOrder,
      preOrderDays: f.preOrderDays,
    });
    form.clearErrors();
    if (!z.success)
      z.error.issues.forEach((i) => {
        const k = String(i.path[0]);
        if (k === 'name' || k === 'origin') form.setError(k, { message: i.message });
        if (k === 'weightGrams') found.push('weight');
        if (k === 'preOrderDays') found.push('pre_order');
      });
    const all = ORDER.filter(
      (x) =>
        found.includes(x) ||
        (!z.success && x === 'name' && z.error.issues.some((i) => i.path[0] === 'name')),
    );
    setIssues(all);
    if (all.length) {
      toast.error(t('seller-products.editor.fix', { count: all.length }));
      document.getElementById(SECTION_OF[all[0]])?.scrollIntoView({
        behavior: window.matchMedia(MQ.reducedMotion).matches ? 'auto' : 'smooth',
        block: 'start',
      });
      return;
    }
    if (!skip.zero && zeroVariants(f.variants) > 0) return setAsk('zero');
    if (!skip.review && product && requiresReview(product.status, initial, f))
      return setAsk('review');
    doSave(true);
  };

  const zeroCount = zeroVariants(form.getValues('variants'));
  const g = me.data ? kycGroup(me.data.kycStatus) : null;
  const sectionProps = { form, readOnly, rules, issues };

  return (
    <div className="max-w-[1180px]">
      {readOnly && (
        <div
          className="flex gap-2.5 items-center bg-amber-50 rounded-md px-4 py-3 mb-3.5 text-sm text-body"
          role="status"
        >
          <Icon name="clock" size={18} className="text-amber-600 flex-none" />
          {t('seller-products.editor.read_only')}
        </div>
      )}
      {product?.status === 'REJECTED' && (
        <div
          className="flex gap-3 bg-red-50 border border-solid border-red-200 rounded-md px-4 py-3.5 mb-3.5"
          role="status"
        >
          <Icon name="triangle-alert" size={22} className="text-red-600 flex-none" />
          <div>
            <div className="font-bold text-sm text-red-600">
              {t('seller-products.editor.rejected')}
            </div>
            <div className="text-sm text-body mt-0.5">
              {t('seller-products.editor.rejected_reason', { reason: product.rejectReason ?? '' })}
            </div>
          </div>
        </div>
      )}
      <button
        type="button"
        onClick={() => (readOnly || !dirty ? back() : setAsk('leave'))}
        className="inline-flex items-center gap-1.5 border-0 bg-transparent text-muted text-sm cursor-pointer mb-3.5 p-0 hover:text-strong"
      >
        <Icon name="chevron-left" size={16} /> {t('seller-products.editor.back')}
      </button>
      <h2 className="m-0 mb-3.5 text-xl font-bold text-strong">
        {product
          ? t('seller-products.editor.title_edit', { name: product.name })
          : t('seller-products.editor.title_new')}
      </h2>

      <form onSubmit={(e) => e.preventDefault()} noValidate>
        <ImageUpload {...sectionProps} />
        <LegalDocs {...sectionProps} />
        <BasicInfo {...sectionProps} />
        <SpecsTable {...sectionProps} />
        <RichDescription {...sectionProps} />
        <VariantSection {...sectionProps} />
        <ShippingInfo {...sectionProps} />

        {!readOnly && (
          <div className="flex gap-3 items-center flex-wrap sticky bottom-0 bg-surface-page/95 py-3 -mx-1 px-1 max-md:static">
            <Button
              variant="secondary"
              iconLeft="file-text"
              loading={save.isPending && !tried.current}
              onClick={() => doSave(false)}
            >
              {t('seller-products.editor.save_draft')}
            </Button>
            <Button
              iconLeft="check"
              disabled={!verified}
              loading={save.isPending && tried.current}
              onClick={() => publish()}
            >
              {product
                ? t('seller-products.editor.save_submit')
                : t('seller-products.editor.publish')}
            </Button>
            <span className="ml-auto inline-flex items-center gap-1.5 text-xs">
              {g === 'VERIFIED' && (
                <span className="inline-flex items-center gap-1.5 text-mint-600">
                  <Icon name="shield-check" size={14} /> {t('seller-products.editor.kyc_ok')}
                </span>
              )}
              {g === 'WAITING' && (
                <span className="inline-flex items-center gap-1.5 text-amber-600">
                  <Icon name="clock" size={14} /> {t('seller-products.editor.kyc_wait')}
                </span>
              )}
              {g && g !== 'VERIFIED' && g !== 'WAITING' && (
                <span className="inline-flex items-center gap-1.5 text-red-600">
                  <Icon name="triangle-alert" size={14} /> {t('seller-products.editor.kyc_bad')}
                </span>
              )}
            </span>
          </div>
        )}
      </form>

      <ConfirmDialog
        open={ask === 'review'}
        onCancel={() => setAsk(null)}
        onConfirm={() => {
          setAsk(null);
          publish({ zero: true, review: true });
        }}
        title={t('seller-products.editor.review_title')}
        message={t('seller-products.editor.review_body')}
        confirmLabel={t('seller-products.editor.review_ok')}
      />
      <ConfirmDialog
        open={ask === 'zero'}
        onCancel={() => setAsk(null)}
        onConfirm={() => {
          setAsk(null);
          publish({ zero: true });
        }}
        title={t('seller-products.editor.zero_title')}
        message={t('seller-products.editor.zero_body', { count: zeroCount })}
        cancelLabel={t('seller-products.editor.zero_back')}
        confirmLabel={t('seller-products.editor.zero_ok')}
      />
      <ConfirmDialog
        open={ask === 'leave'}
        tone="danger"
        onCancel={() => setAsk(null)}
        onConfirm={back}
        title={t('seller-products.editor.leave_title')}
        message={t('seller-products.editor.leave_body')}
        cancelLabel={t('seller-products.editor.leave_stay')}
        confirmLabel={t('seller-products.editor.leave_ok')}
      />
    </div>
  );
}

export default function ProductEditor() {
  const { t } = useT();
  const { id } = useParams();
  const pid = id ? Number(id) : null;
  const q = useSellerProduct(pid && Number.isInteger(pid) ? pid : null);
  if (pid === null) return <Editor product={null} />;
  if (q.isPending) return <Skeleton lines={12} />;
  if (q.isError || !q.data)
    return (
      <EmptyState
        icon="search"
        title={t('seller-products.editor.not_found')}
        text={q.error ? errorMessage(q.error, t) : undefined}
        action={
          <Link to="/products" className={buttonClass({})}>
            {t('seller-products.editor.back')}
          </Link>
        }
      />
    );
  return <Editor key={q.data.id} product={q.data} />;
}
