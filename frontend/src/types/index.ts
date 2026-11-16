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
export interface Attempt { id: string; testId: string; testTitle: string; status: string; score?: number; resultSummary?: string; startedAt: string; completedAt?: string; answers: AnswerView[] }
