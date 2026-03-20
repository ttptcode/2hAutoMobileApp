package com.example.a2hauto.model;

public class SendMessageRequest {
    private final String conversationId;
    private final String senderId;
    private final String content;

    public SendMessageRequest(String conversationId, String senderId, String content) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
    }

    public String getConversationId() {
        return conversationId;
    }

    public String getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }
}

