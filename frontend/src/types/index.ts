export type Role = 'PATIENT' | 'PSYCHOLOGIST' | 'ADMIN'
export interface User { id: string; name: string; email: string; role: Role }
export interface AuthResponse { token: string; user: User }
export interface JournalAttachment { id: string; type: string; originalName?: string; mimeType?: string; url: string; createdAt: string }
export interface JournalComment { id: string; authorName: string; authorRole: string; content: string; createdAt: string }
export interface JournalEntry { id: string; title?: string; content: string; mood?: string; entryDate: string; createdAt: string; attachments?: JournalAttachment[]; comments?: JournalComment[] }
export interface PatientSummary { id: string; name: string; email: string; linkedAt?: string }
export interface TestSummary { id: string; type: string; title: string; description: string; questionCount: number }
export interface TestOption { id: string; label: string; value: number }
export interface TestQuestion { id: string; position: number; prompt: string; kind: string; options: TestOption[] }
export interface TestDetail { id: string; type: string; title: string; description: string; questions: TestQuestion[] }
export interface AnswerView { questionId: string; optionId?: string; value?: number }
export interface Attempt { id: string; testId: string; testTitle: string; status: string; score?: number; maxScore?: number; resultSummary?: string; psychologistFeedback?: string; startedAt: string; completedAt?: string; answers: AnswerView[] }

export type PatientAttempt = Attempt

export interface Consent { id: string; kind: string; version: string; accepted: boolean; acceptedAt: string; createdAt: string }
export interface PrivacyRequestView { id: string; userId: string; type: string; status: string; details?: string; requestedAt: string; resolvedAt?: string }
export interface PrivacyOverview { consents: Consent[]; requests: PrivacyRequestView[] }

export interface AdminUser { id: string; name: string; email: string; role: Role; enabled: boolean; createdAt: string }
export interface Paged<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export interface AdminTest { id: string; type: string; title: string; description?: string; active: boolean; questionCount: number; createdAt: string }
export interface AdminQuestion { id: string; position: number; prompt: string; kind: string; options: { id: string; label: string; value: number }[] }
export interface AuditEvent { id: string; actorEmail?: string; actorRole?: string; action: string; resourceType?: string; resourceId?: string; targetUserId?: string; details?: string; ipAddress?: string; createdAt: string }
export interface AdminOverview { users: number; patients: number; psychologists: number; admins: number; activeTests: number; pendingPrivacyRequests: number; auditEventsLast7Days: number }

export interface Appointment { id: string; patientId: string; patientName: string; psychologistId: string; psychologistName: string; startsAt: string; endsAt: string; mode: string; status: string; title?: string; notes?: string; createdAt: string }
export interface AppointmentRequest { patientId?: string; psychologistId?: string; startsAt: string; endsAt?: string; mode?: string; status?: string; title?: string; notes?: string }
export interface PsychologistDirectoryEntry { id: string; name: string }
export interface TransferResponse { patientId: string; fromPsychologistId: string; toPsychologistId: string; rescheduledAppointments: number }
export interface TransferHistoryEntry { id: string; details: string; createdAt: string }

export interface ChatStatus { psychologistOnline: boolean; psychologistName?: string; message: string; consentRequired: boolean }
export interface ChatMessageView { id: string; sender: 'USER' | 'BOT'; content: string; crisis: boolean; createdAt: string; userEmail?: string | null }
export interface ChatReply { sessionId: string; userMessage: ChatMessageView; botMessage: ChatMessageView; crisis: boolean }
export interface Availability { psychologistId: string; timezone: string; startTime: string; endTime: string; weekendEnabled: boolean }
export interface AvailabilityRequest { timezone: string; startTime: string; endTime: string; weekendEnabled: boolean }
export interface CrisisAlert { id: string; patientId: string; patientName: string; createdAt: string }
