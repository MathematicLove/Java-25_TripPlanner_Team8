package org.tripplanner.modules.triphelper;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.tripplanner.domain.Point;
import org.tripplanner.domain.Trip;
import org.tripplanner.repositories.PointDAO;
import org.tripplanner.repositories.TripDAO;
import org.tripplanner.repositories.UserDAO;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TripHelperService {

    private final UserDAO userDAO;
    private final TripDAO tripDAO;
    private final PointDAO pointDAO;
    private static final double NOTIFICATION_RADIUS_KM = 1.0; // 1 km radius for notifications

    public TripHelperService(UserDAO userDAO, TripDAO tripDAO, PointDAO pointDAO) {
        this.userDAO = userDAO;
        this.tripDAO = tripDAO;
        this.pointDAO = pointDAO;
    }

    public Flux<Trip> getOngoingTrips(Long chatId) {
        return userDAO.getCurrentTrips(chatId);
    }

    public Flux<Point> listAllTripPoints(String tripId) {
        return tripDAO.getAllPoints(tripId);
    }

    public Mono<Point> markPointVisited(String pointId) {
        return pointDAO.markPointVisited(pointId);
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

    // Calculate distance between two points using Haversine formula
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth's radius in kilometers

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
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

    // Update user's current location and check for nearby points
    public Mono<Void> updateUserLocation(Long chatId, double latitude, double longitude) {
        return userDAO.getCurrentTrips(chatId)
            .flatMap(trip -> tripDAO.getAllPoints(trip.getId())
                .filter(point -> !point.isVisited())
                .filter(point -> calculateDistance(latitude, longitude, point.getLatitude(), point.getLongitude()) <= NOTIFICATION_RADIUS_KM)
                .collectList()
                .flatMap(points -> {
                    if (!points.isEmpty()) {
                        return notifyNearbyPoints(chatId, points);
                    }
                    return Mono.empty();
                }))
            .then();
    }
}
