package org.tripplanner.modules.connections;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.tripplanner.modules.dialog.TelegramBotController;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class TelegramBotStarter {

    private final TelegramBotController controller;
    private TelegramBotsApi botsApi;
    private TripPlannerBot bot;
    private DefaultBotSession session;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${project.authors}")
    private String authors;

    public TelegramBotStarter(TelegramBotController controller) {
        this.controller = controller;
    }

    @PostConstruct
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            try {
                // Create bot instance
                bot = new TripPlannerBot(botToken, "tripplanner_bot", controller);
                
                // Clear any existing webhook and wait to ensure it's cleared
                bot.clearWebhook();
                TimeUnit.SECONDS.sleep(1);
                
                // Create new session and register bot
                botsApi = new TelegramBotsApi(DefaultBotSession.class);
                session = (DefaultBotSession) botsApi.registerBot(bot);
                
                System.out.println("✅ Bot started by: " + authors);
            } catch (TelegramApiException | InterruptedException e) {
                isRunning.set(false);
                System.err.println("❌ Error starting bot: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (isRunning.compareAndSet(true, false)) {
            try {
                System.out.println("🛑 Stopping bot...");
                
                // Stop the session first
                if (session != null) {
                    session.stop();
                    TimeUnit.SECONDS.sleep(1);
                }
                
                // Clear webhook and wait a bit to ensure it's cleared
                if (bot != null) {
                    bot.clearWebhook();
                    TimeUnit.SECONDS.sleep(1);
                }
                
                System.out.println("✅ Bot stopped successfully");
            } catch (Exception e) {
                System.err.println("❌ Error stopping bot: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
