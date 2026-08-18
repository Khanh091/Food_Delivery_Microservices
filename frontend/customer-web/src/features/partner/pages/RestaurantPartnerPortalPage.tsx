import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { FileUpload } from '../../../components/media/FileUpload'
import { refreshKeycloakToken } from '../../auth/authService'
import { useCurrentUserStore } from '../../auth/stores/currentUserStore'
import { useToastStore } from '../../toast/stores/toastStore'
import { createApplication, deleteDocument, getApplication, getDocuments, getMyApplications, submitApplication, updateApplication, uploadDocument } from '../api/partnerApi'
import type { ApplicationDocument, ApplicationDocumentType, ApplicationInput, RestaurantApplication, RestaurantApplicationSummary } from '../types/partner'
import '../partner.css'

type DocumentMetadata = { documentNumber: string; issuedAt: string; expiresAt: string }

const emptyInput = (profile: { fullName: string | null; phoneNumber: string | null; email: string | null } | null): ApplicationInput => ({
  businessName: '', businessType: '', taxCode: '', representativeName: profile?.fullName ?? '', representativePhone: profile?.phoneNumber ?? '', representativeEmail: profile?.email ?? '', description: '', city: '', district: '', businessAddress: '', expectedBranchCount: 1, estimatedDailyOrders: null, mainCuisine: '',
})
const editable = (status: RestaurantApplication['status']) => status === 'DRAFT' || status === 'NEEDS_MORE_INFORMATION'
const requiredDocuments: { type: ApplicationDocumentType; label: string }[] = [{ type: 'BUSINESS_LICENSE', label: 'Giấy phép kinh doanh' }, { type: 'OWNER_ID_CARD', label: 'Giấy tờ tùy thân người đại diện' }]
const statusCopy: Record<RestaurantApplication['status'], [string, string]> = {
  DRAFT: ['Hồ sơ nháp', 'Hoàn tất các bước còn lại để gửi hồ sơ.'], SUBMITTED: ['Đã gửi hồ sơ', 'Hồ sơ của bạn đang chờ được tiếp nhận.'], UNDER_REVIEW: ['Đang xét duyệt', 'Đội ngũ FD đang kiểm tra hồ sơ của bạn.'], NEEDS_MORE_INFORMATION: ['Yêu cầu bổ sung', 'Vui lòng cập nhật các thông tin hoặc tài liệu được yêu cầu.'], APPROVED: ['Đã được phê duyệt', 'Bạn có thể bắt đầu quản lý nhà hàng của mình.'], REJECTED: ['Không được phê duyệt', 'Bạn có thể xem lý do ở bên dưới.'], CANCELLED: ['Đã hủy', 'Hồ sơ này đã được hủy.'],
}

export function RestaurantPartnerPortalPage() {
  const navigate = useNavigate()
  const { applicationId } = useParams()
  const profile = useCurrentUserStore((state) => state.profile)
  const pushToast = useToastStore((state) => state.push)
  const [applications, setApplications] = useState<RestaurantApplicationSummary[] | null>(null)
  const [application, setApplication] = useState<RestaurantApplication | null>(null)
  const [documents, setDocuments] = useState<ApplicationDocument[]>([])
  const [documentMetadata, setDocumentMetadata] = useState<Record<string, DocumentMetadata>>({})
  const [form, setForm] = useState<ApplicationInput>(() => emptyInput(profile))
  const [step, setStep] = useState(1)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    const summaries = await getMyApplications()
    setApplications(summaries)
    if (!applicationId || applicationId === 'new') return
    const [detail, uploaded] = await Promise.all([getApplication(applicationId), getDocuments(applicationId)])
    setApplication(detail); setDocuments(uploaded); setForm({
      businessName: detail.businessName, businessType: detail.businessType ?? '', taxCode: detail.taxCode ?? '', representativeName: detail.representativeName, representativePhone: detail.representativePhone, representativeEmail: detail.representativeEmail ?? '', description: detail.description ?? '', city: detail.city, district: detail.district ?? '', businessAddress: detail.businessAddress, expectedBranchCount: detail.expectedBranchCount, estimatedDailyOrders: detail.estimatedDailyOrders, mainCuisine: detail.mainCuisine ?? '',
    })
  }, [applicationId])

  useEffect(() => { setLoading(true); setError(null); void refresh().catch(() => setError('Chưa thể tải hồ sơ đối tác lúc này.')).finally(() => setLoading(false)) }, [refresh])
  const canEdit = application ? editable(application.status) : true
  const hasRequiredDocuments = requiredDocuments.every(({ type }) => documents.some((document) => document.documentType === type))
  const selectedSummary = useMemo(() => applications?.find((item) => item.id === applicationId), [applicationId, applications])

  const updateForm = <K extends keyof ApplicationInput>(key: K, value: ApplicationInput[K]) => setForm((current) => ({ ...current, [key]: value }))
  const saveDraft = async (nextStep: number) => {
    setSaving(true); setError(null)
    try {
      if (application) await updateApplication(application.id, form)
      else {
        const created = await createApplication(form)
        pushToast('success', 'Đã tạo hồ sơ nháp.')
        navigate(`/partner/restaurant/applications/${created.id}`, { replace: true })
        return
      }
      await refresh(); pushToast('success', 'Đã lưu hồ sơ.'); setStep(nextStep)
    } catch { setError('Không thể lưu hồ sơ. Hãy kiểm tra các thông tin bắt buộc.') } finally { setSaving(false) }
  }
  const upload = async (type: ApplicationDocumentType, file: File) => {
    if (!application) return
    setSaving(true); setError(null)
    try {
      const metadata = documentMetadata[type]
      await uploadDocument(application.id, file, {
        documentType: type,
        documentNumber: metadata?.documentNumber || undefined,
        issuedAt: metadata?.issuedAt || undefined,
        expiresAt: metadata?.expiresAt || undefined,
      })
      setDocuments(await getDocuments(application.id)); pushToast('success', 'Đã tải tài liệu.')
    } catch { setError('Không thể tải tài liệu lúc này.') } finally { setSaving(false) }
  }
  const removeDocument = async (document: ApplicationDocument) => {
    if (!application) return
    setSaving(true)
    try { await deleteDocument(application.id, document.id); setDocuments(await getDocuments(application.id)); pushToast('success', 'Đã xóa tài liệu.') } catch { setError('Không thể xóa tài liệu lúc này.') } finally { setSaving(false) }
  }
  const submit = async () => {
    if (!application || !hasRequiredDocuments) { setError('Vui lòng tải đủ các tài liệu bắt buộc trước khi gửi.'); return }
    setSaving(true); setError(null)
    try { const submitted = await submitApplication(application.id); setApplication(submitted); pushToast('success', 'Đã gửi hồ sơ đăng ký.') } catch { setError('Chưa thể gửi hồ sơ. Vui lòng kiểm tra lại thông tin và tài liệu.') } finally { setSaving(false) }
  }

  if (loading) return <main className="partner-page"><p>Đang tải hồ sơ đối tác…</p></main>
  if (error && !applicationId) return <main className="partner-page"><div className="empty-state"><h1>Không thể tải hồ sơ</h1><p>{error}</p></div></main>
  if (!applicationId) return <main className="partner-page"><section className="partner-hero"><p className="eyebrow">FD Partner</p><h1>Trở thành đối tác nhà hàng FD</h1><p>Mở rộng việc kinh doanh của bạn với nền tảng giao đồ ăn, quản lý hồ sơ minh bạch và đội ngũ hỗ trợ chuyên biệt.</p><button type="button" className="button primary" onClick={() => { setApplication(null); setForm(emptyInput(profile)); setStep(1); navigate('/partner/restaurant/applications/new') }}>Bắt đầu đăng ký</button></section>{applications?.length ? <section className="partner-history"><h2>Hồ sơ của bạn</h2>{applications.map((item) => <button type="button" key={item.id} onClick={() => navigate(`/partner/restaurant/applications/${item.id}`)}><span><strong>{item.businessName}</strong><small>{item.city}</small></span><em>{statusCopy[item.status][0]}</em></button>)}</section> : null}</main>
  if (applicationId === 'new') return <Wizard form={form} updateForm={updateForm} step={step} setStep={setStep} saving={saving} error={error} onNext={() => { if (step === 1) setStep(2); else void saveDraft(3) }} />
  if (!application) return <main className="partner-page"><div className="empty-state"><h1>Không tìm thấy hồ sơ</h1><p>{selectedSummary ? 'Hồ sơ không còn khả dụng.' : 'Hãy chọn một hồ sơ khác.'}</p></div></main>
  if (!canEdit) return <StatusPage application={application} onOwner={() => { void refreshKeycloakToken(-1).finally(() => navigate('/restaurant')) }} />
  return <Wizard form={form} updateForm={updateForm} step={step} setStep={setStep} saving={saving} error={error} documents={documents} application={application} hasRequiredDocuments={hasRequiredDocuments} documentMetadata={documentMetadata} setDocumentMetadata={setDocumentMetadata} onNext={() => void saveDraft(Math.min(step + 1, 4))} onUpload={upload} onDelete={removeDocument} onSubmit={() => void submit()} />
}

function Wizard({ form, updateForm, step, setStep, saving, error, documents = [], application, hasRequiredDocuments, documentMetadata = {}, setDocumentMetadata, onNext, onUpload, onDelete, onSubmit }: { form: ApplicationInput; updateForm: <K extends keyof ApplicationInput>(key: K, value: ApplicationInput[K]) => void; step: number; setStep: (step: number) => void; saving: boolean; error: string | null; documents?: ApplicationDocument[]; application?: RestaurantApplication; hasRequiredDocuments?: boolean; documentMetadata?: Record<string, DocumentMetadata>; setDocumentMetadata?: (value: Record<string, DocumentMetadata> | ((current: Record<string, DocumentMetadata>) => Record<string, DocumentMetadata>)) => void; onNext: () => void; onUpload?: (type: ApplicationDocumentType, file: File) => Promise<void>; onDelete?: (document: ApplicationDocument) => Promise<void>; onSubmit?: () => void }) {
  const nextLabel = application ? 'Lưu và tiếp tục' : 'Tiếp tục tới người đại diện'
  return <main className="partner-page"><section className="partner-wizard"><ol className="partner-steps" aria-label="Tiến độ hồ sơ"><li className={step >= 1 ? 'active' : ''}>1. Doanh nghiệp</li><li className={step >= 2 ? 'active' : ''}>2. Đại diện</li><li className={step >= 3 ? 'active' : ''}>3. Hồ sơ pháp lý</li><li className={step >= 4 ? 'active' : ''}>4. Kiểm tra & gửi</li></ol>{application?.status === 'NEEDS_MORE_INFORMATION' && application.rejectionReason ? <div className="review-card"><strong>FD yêu cầu bổ sung</strong><p>{application.rejectionReason}</p></div> : null}{error ? <p className="form-error" role="alert">{error}</p> : null}
    {step === 1 ? <section><p className="eyebrow">Bước 1</p><h1>Thông tin doanh nghiệp</h1><div className="partner-form"><label>Tên doanh nghiệp / nhà hàng<input value={form.businessName} onChange={(event) => updateForm('businessName', event.target.value)} required /></label><label>Loại hình kinh doanh<select value={form.businessType} onChange={(event) => updateForm('businessType', event.target.value as ApplicationInput['businessType'])}><option value="">Chọn loại hình</option><option value="HOUSEHOLD_BUSINESS">Hộ kinh doanh</option><option value="COMPANY">Công ty</option><option value="INDIVIDUAL">Cá nhân</option><option value="FRANCHISE">Nhượng quyền</option><option value="OTHER">Khác</option></select></label><label>Mã số thuế<input value={form.taxCode} onChange={(event) => updateForm('taxCode', event.target.value)} /></label><label>Tỉnh / thành phố<input value={form.city} onChange={(event) => updateForm('city', event.target.value)} required /></label><label>Quận / huyện<input value={form.district} onChange={(event) => updateForm('district', event.target.value)} /></label><label>Địa chỉ kinh doanh<input value={form.businessAddress} onChange={(event) => updateForm('businessAddress', event.target.value)} required /></label><label>Số chi nhánh dự kiến<input type="number" min="1" value={form.expectedBranchCount} onChange={(event) => updateForm('expectedBranchCount', Number(event.target.value))} required /></label><label>Đơn hàng/ngày ước tính<input type="number" min="0" value={form.estimatedDailyOrders ?? ''} onChange={(event) => updateForm('estimatedDailyOrders', event.target.value ? Number(event.target.value) : null)} /></label><label>Món ăn chủ đạo<input value={form.mainCuisine} onChange={(event) => updateForm('mainCuisine', event.target.value)} /></label><label className="full">Mô tả<textarea value={form.description} onChange={(event) => updateForm('description', event.target.value)} /></label></div><div className="form-actions"><button type="button" className="button primary" disabled={saving} onClick={onNext}>{saving ? 'Đang lưu…' : nextLabel}</button></div></section> : null}
    {step === 2 ? <section><p className="eyebrow">Bước 2</p><h1>Người đại diện</h1><div className="partner-form"><label>Họ tên người đại diện<input value={form.representativeName} onChange={(event) => updateForm('representativeName', event.target.value)} required /></label><label>Số điện thoại<input value={form.representativePhone} onChange={(event) => updateForm('representativePhone', event.target.value)} required /></label><label>Email<input type="email" value={form.representativeEmail} onChange={(event) => updateForm('representativeEmail', event.target.value)} /></label></div><div className="form-actions"><button type="button" className="button secondary" onClick={() => setStep(1)}>Quay lại</button><button type="button" className="button primary" disabled={saving} onClick={onNext}>{saving ? 'Đang lưu…' : nextLabel}</button></div></section> : null}
    {step === 3 ? <section><p className="eyebrow">Bước 3</p><h1>Hồ sơ pháp lý</h1><p className="partner-description">Tài liệu được tải lên an toàn. Bạn không cần dán đường dẫn tệp.</p><div className="document-list">{requiredDocuments.map(({ type, label }) => { const document = documents.find((item) => item.documentType === type); const metadata = documentMetadata[type] ?? { documentNumber: document?.documentNumber ?? '', issuedAt: document?.issuedAt ?? '', expiresAt: document?.expiresAt ?? '' }; const updateMetadata = (key: keyof DocumentMetadata, value: string) => setDocumentMetadata?.((current) => ({ ...current, [type]: { ...metadata, [key]: value } })); return <article key={type}><div><strong>{label}</strong><small>{document ? `${document.fileName} · ${document.verificationStatus}` : 'Bắt buộc'}</small><div className="document-metadata"><label>Số hồ sơ<input value={metadata.documentNumber} maxLength={100} onChange={(event) => updateMetadata('documentNumber', event.target.value)} disabled={saving} /></label><label>Ngày cấp<input type="date" value={metadata.issuedAt} onChange={(event) => updateMetadata('issuedAt', event.target.value)} disabled={saving} /></label><label>Ngày hết hạn<input type="date" value={metadata.expiresAt} onChange={(event) => updateMetadata('expiresAt', event.target.value)} disabled={saving} /></label></div></div>{document ? <div className="document-actions"><a href={document.fileUrl} target="_blank" rel="noreferrer">Xem</a><FileUpload label="Thay thế" loading={saving} onUpload={(file) => onUpload!(type, file)} /><button type="button" onClick={() => void onDelete?.(document)} disabled={saving}>Xóa</button></div> : <FileUpload label="Tải tài liệu" loading={saving} onUpload={(file) => onUpload!(type, file)} />}</article> })}</div><div className="form-actions"><button type="button" className="button secondary" onClick={() => setStep(2)}>Quay lại</button><button type="button" className="button primary" disabled={saving || !hasRequiredDocuments} onClick={() => setStep(4)}>Tiếp tục</button></div></section> : null}
    {step === 4 ? <section><p className="eyebrow">Bước 4</p><h1>Kiểm tra & gửi</h1><div className="review-card"><h2>{form.businessName || 'Thông tin doanh nghiệp'}</h2><p>{form.businessAddress}, {form.district ? `${form.district}, ` : ''}{form.city}</p><p>{form.representativeName} · {form.representativePhone}</p><p>{hasRequiredDocuments ? 'Đã đủ tài liệu bắt buộc.' : 'Chưa đủ tài liệu bắt buộc.'}</p></div><div className="form-actions"><button type="button" className="button secondary" onClick={() => setStep(3)}>Quay lại</button><button type="button" className="button primary" disabled={saving || !hasRequiredDocuments} onClick={() => void onSubmit?.()}>{saving ? 'Đang gửi…' : 'Gửi hồ sơ đăng ký'}</button></div></section> : null}
  </section></main>
}

function StatusPage({ application, onOwner }: { application: RestaurantApplication; onOwner: () => void }) { const [title, description] = statusCopy[application.status]; return <main className="partner-page"><section className="partner-status"><p className="eyebrow">FD Partner</p><h1>{title}</h1><p>{description}</p>{application.rejectionReason ? <div className="review-card"><strong>Lý do / yêu cầu từ FD</strong><p>{application.rejectionReason}</p></div> : null}{application.status === 'APPROVED' ? <button type="button" className="button primary" onClick={onOwner}>Đi tới trang quản lý nhà hàng</button> : null}</section></main> }
