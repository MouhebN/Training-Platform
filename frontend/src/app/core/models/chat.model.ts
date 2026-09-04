import { Role } from './user.model';

export type ChatMessageType = 'TEXT' | 'SYSTEM';

export interface ChatMessage {
  id: number;
  sessionId: number;
  senderId: number;
  senderFullName: string;
  senderRole: Role;
  content: string;
  messageType: ChatMessageType;
  createdAt: string;
  mine: boolean;
  readByCount: number;
}

export interface ChatMessageRequest {
  content: string;
}

export interface UnreadCount {
  sessionId: number;
  unreadCount: number;
}

export interface TypingEvent {
  sessionId: number;
  userId: number;
  fullName: string;
  typing: boolean;
}
