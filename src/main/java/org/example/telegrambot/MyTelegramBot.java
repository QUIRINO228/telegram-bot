package org.example.telegrambot;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class MyTelegramBot extends TelegramLongPollingBot {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    private final Set<String> processedMessages = new HashSet<>();

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onRegister() {
        log.info("Bot registered with Telegram API, username: {}", botUsername);
    }

    @Override
    public void onUpdateReceived(Update update) {
        LocalDate localDate = LocalDate.now();
        try {
            if (update.hasMessage()) {
                MDC.put("chatId", String.valueOf(update.getMessage().getChatId()));
            }
            log.info("Received update, date: {}, updateId: {}", localDate, update.getUpdateId());

            if (update.hasMessage() && update.getMessage().hasText()) {
                String messageText = update.getMessage().getText();
                long chatId = update.getMessage().getChatId();
                int messageId = update.getMessage().getMessageId();
                String messageKey = chatId + ":" + messageId;

                if (processedMessages.contains(messageKey)) {
                    log.info("Duplicate message detected, date: {}, messageKey: {}", localDate, messageKey);
                    return;
                }
                processedMessages.add(messageKey);
                log.info("Message received, date: {}, text: {}, messageId: {}", localDate, messageText, messageId);

                if (messageText.equals("/start")) {
                    String username = update.getMessage().getFrom().getUserName();
                    String response = "hello @" + (username != null ? username : "user");
                    SendMessage message = new SendMessage();
                    message.setChatId(String.valueOf(chatId));
                    message.setText(response);
                    try {
                        execute(message);
                        log.info("Sent response, date: {}, response: {}, username: {}", localDate, response, username);
                    } catch (TelegramApiException e) {
                        log.error("Failed to send message, date: {}, error: {}", localDate, e.getMessage(), e);
                    }
                }
            }
        } finally {
            MDC.remove("chatId");
        }
    }
}