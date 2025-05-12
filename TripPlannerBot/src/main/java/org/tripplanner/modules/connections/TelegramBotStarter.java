package org.tripplanner.modules.connections;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.updates.DeleteWebhook;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.tripplanner.modules.dialog.TelegramBotController;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

@Component
public class TelegramBotStarter {

    private static final Logger logger = LoggerFactory.getLogger(TelegramBotStarter.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_SECONDS = 2;
    private static final int SHUTDOWN_DELAY_SECONDS = 3;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    @Value("${project.authors}")
    private String authors;

    private final TelegramBotController controller;
    private TelegramBotsApi botsApi;
    private TripPlannerBot bot;
    private DefaultBotSession session;
    private final AtomicBoolean isRunning = new AtomicBoolean(false);

    public TelegramBotStarter(TelegramBotController controller) {
        this.controller = controller;
    }

    @PostConstruct
    public void start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Bot is already running");
            return;
        }

        logger.info("Starting Telegram bot...");
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                if (retryCount > 0) {
                    logger.info("Retry attempt {} of {}", retryCount + 1, MAX_RETRIES);
                    Thread.sleep(RETRY_DELAY_SECONDS * 1000L);
                }

                bot = new TripPlannerBot(botToken, botUsername, controller);
                botsApi = new TelegramBotsApi(DefaultBotSession.class);

                try {
                    DeleteWebhook deleteWebhook = new DeleteWebhook();
                    deleteWebhook.setDropPendingUpdates(true);
                    bot.execute(deleteWebhook);
                    logger.info("Deleted webhook with dropPendingUpdates=true");
                    Thread.sleep(1000); // Wait a bit after deleting webhook
                } catch (Exception e) {
                    logger.warn("Failed to delete webhook: {}", e.getMessage());
                }

                session = (DefaultBotSession) botsApi.registerBot(bot);
                
                logger.info("Bot started by: {}", authors);
                return;
            } catch (Exception e) {
                retryCount++;
                logger.error("Failed to start bot (attempt {}/{}): {}", retryCount, MAX_RETRIES, e.getMessage());
                if (retryCount == MAX_RETRIES) {
                    logger.error("Failed to start bot after {} attempts", MAX_RETRIES);
                    isRunning.set(false);
                    throw new RuntimeException("Failed to start Telegram bot", e);
                }
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (!isRunning.get()) {
            return;
        }

        logger.info("Stopping Telegram bot...");
        try {
            if (session != null) {
                session.stop();
                logger.info("Stopped bot session");
                Thread.sleep(SHUTDOWN_DELAY_SECONDS * 1000L);
            }

            if (bot != null) {
                try {
                    DeleteWebhook deleteWebhook = new DeleteWebhook();
                    deleteWebhook.setDropPendingUpdates(true);
                    bot.execute(deleteWebhook);
                    logger.info("Final webhook cleanup completed");
                } catch (Exception e) {
                    logger.warn("Final webhook cleanup failed: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error stopping bot: {}", e.getMessage());
        } finally {
            isRunning.set(false);
            logger.info("Bot stopped successfully");
        }
    }
}
