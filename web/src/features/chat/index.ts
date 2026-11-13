// API công khai của feature "chat" — nơi DUY NHẤT feature khác được import.
export type { Conversation, ChatMessage, MessageType } from './domain/chat.types';
export { totalUnread } from './domain/chat.rules';
export { useConversations, useOpenConversation } from './application/useChat';
export { default as ChatWidget } from './ui/ChatWidget';
export { default as ChatPage, type ChatPageProps } from './ui/ChatPage';
