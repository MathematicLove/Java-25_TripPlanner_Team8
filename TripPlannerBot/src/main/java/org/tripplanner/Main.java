package org.tripplanner;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;
import org.tripplanner.config.MongoConfig;
import org.tripplanner.config.SecurityConfig;
import org.tripplanner.config.WebConfig;

import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

public class Main {
    public static void main(String[] args) {
        System.out.println("🚀 Запуск TripPlanner...");

        // Инициализация Spring контекста
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.scan("org.tripplanner");
        context.register(WebConfig.class, SecurityConfig.class, MongoConfig.class);
        context.refresh();

        // Запуск WebFlux сервера
        DisposableServer server = HttpServer.create()
                .port(8080)
                .handle(new ReactorHttpHandlerAdapter(WebHttpHandlerBuilder.applicationContext(context).build()))
                .bindNow();

        // Добавляем обработчик завершения работы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("🛑 Завершение работы...");
            try {
                // Даем время на корректное завершение работы бота
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            server.dispose();
            context.close();
            System.out.println("✅ Приложение остановлено");
        }));

        // Блокируем главный поток до завершения работы сервера
        server.onDispose().block();
    }
}
