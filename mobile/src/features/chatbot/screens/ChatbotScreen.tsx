import React, { useState, useRef, useCallback } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  FlatList,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { useMutation } from '@tanstack/react-query';
import api from '../../../shared/api/axiosInstance';

interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  intent?: string;
  timestamp: Date;
}

interface ChatResponse {
  message: string;
  intent?: string;
  actionTaken?: string;
  data?: unknown;
}

const INTENT_LABELS: Record<string, { label: string; color: string; bg: string }> = {
  LEAVE_QUERY: { label: 'Nghi phep', color: '#1677ff', bg: '#e6f7ff' },
  ATTENDANCE_QUERY: { label: 'Cham cong', color: '#52c41a', bg: '#f6ffed' },
  SCHEDULE_QUERY: { label: 'Lich lam', color: '#722ed1', bg: '#f9f0ff' },
  LEAVE_CREATE: { label: 'Tao don phep', color: '#fa8c16', bg: '#fff7e6' },
  SHIFT_SWAP: { label: 'Doi ca', color: '#13c2c2', bg: '#e6fffb' },
  POLICY_QUERY: { label: 'Quy dinh', color: '#eb2f96', bg: '#fff0f6' },
  UNKNOWN: { label: 'Khac', color: '#999', bg: '#f5f5f5' },
};

let messageIdCounter = 0;
function generateId(): string {
  messageIdCounter += 1;
  return `msg-${Date.now()}-${messageIdCounter}`;
}

export default function ChatbotScreen() {
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: generateId(),
      role: 'assistant',
      content:
        'Xin chao! Toi la tro ly AI cua Smart Attendance. Ban co the hoi toi ve nghi phep, cham cong, lich lam viec, hoac quy dinh noi bo.',
      timestamp: new Date(),
    },
  ]);
  const [inputText, setInputText] = useState('');
  const flatListRef = useRef<FlatList>(null);

  const sendMutation = useMutation<ChatResponse, unknown, string>({
    mutationFn: (text) =>
      api
        .post('/ai/chat/message', { message: text })
        .then((r) => r.data.data),
    onSuccess: (data) => {
      const assistantMsg: ChatMessage = {
        id: generateId(),
        role: 'assistant',
        content: data.message,
        intent: data.intent,
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    },
    onError: () => {
      const errorMsg: ChatMessage = {
        id: generateId(),
        role: 'assistant',
        content:
          'Xin loi, toi khong the xu ly yeu cau cua ban luc nay. Vui long thu lai sau hoac lien he HR.',
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, errorMsg]);
    },
  });

  const handleSend = useCallback(() => {
    const text = inputText.trim();
    if (!text || sendMutation.isPending) return;

    const userMsg: ChatMessage = {
      id: generateId(),
      role: 'user',
      content: text,
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMsg]);
    setInputText('');
    sendMutation.mutate(text);
  }, [inputText, sendMutation]);

  const renderMessage = ({ item }: { item: ChatMessage }) => {
    const isUser = item.role === 'user';
    const intentConfig = item.intent
      ? INTENT_LABELS[item.intent] ?? INTENT_LABELS.UNKNOWN
      : null;

    return (
      <View
        style={[
          styles.messageBubble,
          isUser ? styles.userBubble : styles.assistantBubble,
        ]}
      >
        {intentConfig && (
          <View
            style={[styles.intentBadge, { backgroundColor: intentConfig.bg }]}
          >
            <Text style={[styles.intentText, { color: intentConfig.color }]}>
              {intentConfig.label}
            </Text>
          </View>
        )}
        <Text
          style={[
            styles.messageText,
            isUser ? styles.userText : styles.assistantText,
          ]}
        >
          {item.content}
        </Text>
        <Text style={styles.timeText}>
          {item.timestamp.toLocaleTimeString('vi-VN', {
            hour: '2-digit',
            minute: '2-digit',
          })}
        </Text>
      </View>
    );
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      <FlatList
        ref={flatListRef}
        data={messages}
        keyExtractor={(item) => item.id}
        renderItem={renderMessage}
        contentContainerStyle={styles.messageList}
        onContentSizeChange={() =>
          flatListRef.current?.scrollToEnd({ animated: true })
        }
      />

      {sendMutation.isPending && (
        <View style={styles.typingIndicator}>
          <ActivityIndicator size="small" color="#1677ff" />
          <Text style={styles.typingText}>Dang suy nghi...</Text>
        </View>
      )}

      <View style={styles.inputContainer}>
        <TextInput
          style={styles.textInput}
          value={inputText}
          onChangeText={setInputText}
          placeholder="Hoi toi ve nghi phep, cham cong, lich lam..."
          placeholderTextColor="#999"
          multiline
          maxLength={500}
          onSubmitEditing={handleSend}
          returnKeyType="send"
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            (!inputText.trim() || sendMutation.isPending) &&
              styles.sendButtonDisabled,
          ]}
          onPress={handleSend}
          disabled={!inputText.trim() || sendMutation.isPending}
        >
          <Text style={styles.sendButtonText}>Gui</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#f5f5f5' },
  messageList: { padding: 16, paddingBottom: 8 },
  messageBubble: {
    maxWidth: '80%',
    borderRadius: 16,
    padding: 12,
    marginBottom: 8,
  },
  userBubble: {
    backgroundColor: '#1677ff',
    alignSelf: 'flex-end',
    borderBottomRightRadius: 4,
  },
  assistantBubble: {
    backgroundColor: '#fff',
    alignSelf: 'flex-start',
    borderBottomLeftRadius: 4,
    elevation: 1,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.08,
    shadowRadius: 2,
  },
  messageText: { fontSize: 15, lineHeight: 22 },
  userText: { color: '#fff' },
  assistantText: { color: '#333' },
  timeText: {
    fontSize: 10,
    color: '#999',
    marginTop: 4,
    alignSelf: 'flex-end',
  },
  intentBadge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: 8,
    marginBottom: 6,
  },
  intentText: { fontSize: 10, fontWeight: '600' },
  typingIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingBottom: 4,
    gap: 8,
  },
  typingText: { fontSize: 12, color: '#999' },
  inputContainer: {
    flexDirection: 'row',
    padding: 12,
    paddingBottom: Platform.OS === 'ios' ? 28 : 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#eee',
    alignItems: 'flex-end',
    gap: 8,
  },
  textInput: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 15,
    maxHeight: 100,
  },
  sendButton: {
    backgroundColor: '#1677ff',
    borderRadius: 20,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  sendButtonDisabled: { backgroundColor: '#a0c4ff' },
  sendButtonText: { color: '#fff', fontWeight: '600', fontSize: 14 },
});
