package org.tripplanner.config;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;

import reactor.core.publisher.Mono;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "org.tripplanner.repositories")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 1000;

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String database;

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public MongoClient reactiveMongoClient() {
        if (mongoUri == null || mongoUri.isEmpty()) {
            throw new IllegalStateException("MongoDB URI is not configured");
        }
        
        logger.info("Using MongoDB URI: {}", mongoUri);
        
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings settings = MongoClientSettings.builder()
            .applyConnectionString(connectionString)
            .retryWrites(true)
            .retryReads(true)
            .build();
            
        MongoClient client = MongoClients.create(settings);
        
        // Test connection with retries
        final int[] retryCount = {0};
        while (retryCount[0] < MAX_RETRIES) {
            try {
                final int currentRetry = retryCount[0];
                logger.info("Testing MongoDB connection (attempt {}/{})", currentRetry + 1, MAX_RETRIES);
                
                // Block until connection test completes
                Mono.from(client.getDatabase(database).runCommand(new Document("ping", 1)))
                    .doOnSuccess(result -> logger.info("Successfully connected to MongoDB"))
                    .doOnError(error -> {
                        logger.error("Failed to connect to MongoDB (attempt {}/{}): {}", 
                            currentRetry + 1, MAX_RETRIES, error.getMessage());
                        if (currentRetry < MAX_RETRIES - 1) {
                            try {
                                Thread.sleep(RETRY_DELAY_MS);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    })
                    .block(); // Wait for the test to complete
                
                logger.info("MongoDB connection test completed successfully");
                return client;
            } catch (Exception e) {
                logger.error("Error testing MongoDB connection: {}", e.getMessage());
                retryCount[0]++;
                if (retryCount[0] < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }
        
        logger.error("Failed to connect to MongoDB after {} attempts", MAX_RETRIES);
        throw new RuntimeException("Failed to connect to MongoDB after " + MAX_RETRIES + " attempts");
    }
} 