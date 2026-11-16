export type Role = 'PATIENT' | 'PSYCHOLOGIST' | 'ADMIN'
export interface User { id: string; name: string; email: string; role: Role }
export interface AuthResponse { token: string; user: User }
export interface JournalAttachment { id: string; type: string; originalName?: string; mimeType?: string; url: string; createdAt: string }
export interface JournalComment { id: string; authorName: string; authorRole: string; content: string; createdAt: string }
export interface JournalEntry { id: string; title?: string; content: string; mood?: string; entryDate: string; createdAt: string; attachments?: JournalAttachment[]; comments?: JournalComment[] }
export interface PatientSummary { id: string; name: string; email: string; linkedAt?: string }
