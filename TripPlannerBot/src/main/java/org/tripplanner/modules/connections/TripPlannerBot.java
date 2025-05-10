package org.tripplanner.modules.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.tripplanner.modules.dialog.TelegramBotController;

public class TripPlannerBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(TripPlannerBot.class);
    private final TelegramBotController controller;
    private final String botToken;
    private final String botUsername;

    public TripPlannerBot(String botToken, String botUsername, TelegramBotController controller) {
        this.botToken = botToken;
        this.botUsername = botUsername;
        this.controller = controller;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            var message = update.getMessage();
            Long chatId = message.getChatId();
            String text = message.getText();

            logger.debug("Received message from chat {}: {}", chatId, text);

            controller.handleCommand(chatId, text)
                    .doOnError(error -> {
                        logger.error("Error handling command for chat {}: {}", chatId, error.getMessage());
                        sendErrorMessage(chatId, "Произошла ошибка при обработке команды. Пожалуйста, попробуйте позже.");
                    })
                    .subscribe(response -> {
                        SendMessage reply = new SendMessage();
                        reply.setChatId(chatId.toString());
                        reply.setText(response);

                        try {
                            execute(reply);
                            logger.debug("Sent response to chat {}: {}", chatId, response);
                        } catch (TelegramApiException e) {
                            logger.error("Error sending message to chat {}: {}", chatId, e.getMessage());
                            sendErrorMessage(chatId, "Произошла ошибка при отправке сообщения. Пожалуйста, попробуйте позже.");
                        }
                    });
        }
    }

    private void sendErrorMessage(Long chatId, String errorMessage) {
        try {
            SendMessage errorReply = new SendMessage();
            errorReply.setChatId(chatId.toString());
            errorReply.setText(errorMessage);
            execute(errorReply);
        } catch (TelegramApiException e) {
            logger.error("Failed to send error message to chat {}: {}", chatId, e.getMessage());
        }
    }
}
