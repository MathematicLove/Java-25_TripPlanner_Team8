package org.tripplanner.modules.triphelper;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.PointDAO;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class TripHelperService {

    private static final Logger logger = LoggerFactory.getLogger(TripHelperService.class);

    private static final double NOTIFICATION_RADIUS_KM = 0.1; // 100 meters radius

    private final UserDAO userDAO;
    private final TripDAO tripDAO;
    private final PointDAO pointDAO;

    public TripHelperService(UserDAO userDAO, TripDAO tripDAO, PointDAO pointDAO) {
        this.userDAO = userDAO;
        this.tripDAO = tripDAO;
        this.pointDAO = pointDAO;
    }

    public Flux<Trip> getOngoingTrips(Long chatId) {
        LocalDate today = LocalDate.now();
        logger.info("Получение активных поездок для пользователя {} на дату {}", chatId, today);

        return userDAO.getUserByChatId(chatId)
                .flatMapMany(user -> {
                    logger.info("Найдены поездки пользователя {}: planned={}, current={}", 
                        chatId, user.getPlannedTrips(), user.getCurrentTrips());

                    return Flux.fromIterable(user.getPlannedTrips())
                            .flatMap(tripId -> tripDAO.getTrip(tripId.toString())
                                    .doOnNext(trip -> logger.info("Проверка поездки {}: startDate={}, endDate={}", 
                                        trip.getName(), trip.getStartDate(), trip.getEndDate()))
                                    .onErrorResume(e -> {
                                        logger.error("Ошибка при получении поездки {}: {}", 
                                            tripId, e.getMessage());
                                        return Mono.empty();
                                    }))
                            .filter(trip -> {
                                boolean isOngoing = !trip.getStartDate().isAfter(today) && 
                                                  !trip.getEndDate().isBefore(today);
                                logger.info("Поездка {} активна: {}", trip.getName(), isOngoing);
                                return isOngoing;
                            });
                })
                .onErrorResume(e -> {
                    logger.error("Ошибка при получении активных поездок для пользователя {}: {}", 
                        chatId, e.getMessage());
                    return Flux.empty();
                });
    }

    public Flux<Point> listAllTripPoints(String tripId) {
        return tripDAO.getAllPoints(tripId);
    }

    public Mono<Point> markPointVisited(String tripName, String pointName) {
        logger.info("Marking point {} as visited in trip {}", pointName, tripName);
        return userDAO.getAllUsers()
                .flatMap(user -> {
                    logger.info("Checking user's trips for trip: {}", tripName);
                    return Flux.concat(
                            Flux.fromIterable(user.getPlannedTrips()),
                            Flux.fromIterable(user.getCurrentTrips()),
                            Flux.fromIterable(user.getTripHistory())
                        )
                        .flatMap(trip -> tripDAO.getTrip(trip.getId()))
                        .filter(trip -> trip.getName().equals(tripName))
                        .next()
                        .switchIfEmpty(Mono.error(new RuntimeException("Поездка с названием '" + tripName + "' не найдена")))
                        .flatMap(trip -> {
                            logger.info("Found trip, checking points");
                            return tripDAO.getAllPoints(trip.getId())
                                    .filter(point -> point.getName().equals(pointName))
                                    .next()
                                    .switchIfEmpty(Mono.error(new RuntimeException("Точка с названием '" + pointName + "' не найдена")))
                                    .flatMap(point -> {
                                        logger.info("Found point, marking as visited");
                                        return pointDAO.markPointVisited(point.getId());
                                    });
                        });
                })
                .next();
    }

    public Mono<Point> addNoteToPoint(String pointId, String note) {
        return pointDAO.addNoteToPoint(pointId, note);
    }

    // Check for nearby points every minute
    @Scheduled(fixedRate = 60000)
    public void checkNearbyPoints() {
        userDAO.getAllUsers()
            .flatMap(user -> {
                LocalDate today = LocalDate.now();
                return Flux.fromIterable(user.getCurrentTrips())
                    .filter(trip -> !trip.getStartDate().isAfter(today) && !trip.getEndDate().isBefore(today))
                    .flatMap(trip -> tripDAO.getAllPoints(trip.getId())
                        .filter(point -> !point.isVisited())
                        .collectList()
                        .flatMap(points -> {
                            if (!points.isEmpty()) {
                                return notifyNearbyPoints(user.getChatId(), points);
                            }
                            return Mono.empty();
                        }));
            })
            .subscribe();
    }

    private Mono<Void> notifyNearbyPoints(Long chatId, List<Point> points) {
        // Here you would implement the actual notification logic
        // For example, sending a message to the user via Telegram
        return Mono.empty();
    }

    public Mono<Void> updateUserLocation(Long chatId, double latitude, double longitude) {
        logger.info("Получено обновление геопозиции для пользователя {}: широта={}, долгота={}", 
            chatId, latitude, longitude);

        return userDAO.getUserByChatId(chatId)
                .flatMap(user -> {
                    if (user.getOngoingTrip() == null) {
                        logger.info("У пользователя {} нет активной поездки", chatId);
                        return Mono.empty();
                    }

                    String tripId = user.getOngoingTrip().getId();
                    logger.info("Проверка точек для активной поездки {} пользователя {}", tripId, chatId);
                    
                    return tripDAO.getAllPoints(tripId)
                            .filter(point -> !point.isVisited())
                            .filter(point -> {
                                double distance = calculateDistance(latitude, longitude, point.getLatitude(), point.getLongitude());
                                logger.info("Расстояние до точки {}: {} км", point.getName(), distance);
                                return distance <= NOTIFICATION_RADIUS_KM;
                            })
                            .collectList()
                            .flatMap(points -> {
                                if (!points.isEmpty()) {
                                    Point nearestPoint = points.get(0);
                                    logger.info("Пользователь {} достиг точки {}", chatId, nearestPoint.getName());
                                    return tripDAO.markPointVisited(tripId, nearestPoint.getId())
                                            .then(Mono.just(nearestPoint))
                                            .doOnNext(point -> notifyUser(chatId, "Вы достигли точки: " + point.getName()));
                                }
                                return Mono.empty();
                            })
                            .then(userDAO.updateUserLocation(chatId, latitude, longitude))
                            .onErrorResume(e -> {
                                logger.error("Ошибка при обновлении геопозиции для пользователя {}: {}", chatId, e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c;

        return distance;
    }

    // Check for upcoming trips and send notifications
    @Scheduled(cron = "0 0 9 * * *") // Run at 9 AM every day
    public void checkUpcomingTrips() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        userDAO.getAllUsers()
            .flatMap(user -> {
                return Flux.fromIterable(user.getCurrentTrips())
                    .filter(trip -> trip.getStartDate().equals(tomorrow))
                    .flatMap(trip -> {
                        // Here you would implement the actual notification logic
                        // For example, sending a message to the user via Telegram
                        return Mono.empty();
                    });
            })
            .subscribe();
    }

    private void notifyUser(Long chatId, String message) {
        // This method will be called by the TelegramBotController to send the message
        // The actual message sending is handled in the controller
        logger.info("Отправка уведомления пользователю {}: {}", chatId, message);
    }

    public Flux<Trip> getAllPlannedTrips(Long chatId) {
        return userDAO.getUserByChatId(chatId)
                .flatMapMany(user -> Flux.fromIterable(user.getPlannedTrips())
                        .flatMap(tripId -> tripDAO.getTrip(tripId.toString())));
    }

    public Mono<String> addNote(Long chatId, String tripName, String note) {
        logger.info("Adding note to trip {} for user {}: {}", tripName, chatId, note);
        if (note == null || note.trim().isEmpty()) {
            return Mono.just("Заметка не может быть пустой");
        }
        return userDAO.getUserByChatId(chatId)
                .flatMap(user -> {
                    logger.info("Found user with planned trips: {}", user.getPlannedTrips());
                    return Flux.concat(
                            Flux.fromIterable(user.getPlannedTrips()),
                            Flux.fromIterable(user.getCurrentTrips()),
                            Flux.fromIterable(user.getTripHistory())
                        )
                        .flatMap(trip -> {
                            logger.info("Checking trip: {} with ID: {}", trip.getName(), trip.getId());
                            return tripDAO.getTrip(trip.getId());
                        })
                        .filter(trip -> trip.getName().equals(tripName))
                        .next()
                        .switchIfEmpty(Mono.error(new RuntimeException("Поездка с названием '" + tripName + "' не найдена")))
                        .flatMap(trip -> {
                            logger.info("Found trip with ID: {}", trip.getId());
                            return tripDAO.addNoteToTrip(trip.getId(), note)
                                    .thenReturn("Заметка успешно добавлена к поездке '" + tripName + "'");
                        });
                })
                .onErrorResume(e -> {
                    logger.error("Error adding note to trip {} for user {}: {}", tripName, chatId, e.getMessage());
                    return Mono.just(e.getMessage());
                });
    }
}
