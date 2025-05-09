package org.tripplanner.modules.connections;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.tripplanner.modules.dialog.TelegramBotController;

public class TripPlannerBot extends TelegramLongPollingBot {

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

            controller.handleCommand(chatId, text)
                    .subscribe(response -> {
                        SendMessage reply = new SendMessage();
                        reply.setChatId(chatId.toString());
                        reply.setText(response);

                        try {
                            execute(reply);
                        } catch (TelegramApiException e) {
                            e.printStackTrace();
                        }
                    });
        }
    }
}
