package org.tripplanner.repositories.mongodb;

import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
import org.tripplanner.domain.Trip;
import org.tripplanner.domain.User;

@Component
public class UserMapper {

    public UserDBO toDbo(User user) {
        UserDBO dbo = new UserDBO();
        dbo.setId(new ObjectId());
        dbo.setChatId(user.getChatId());

        // Сохраняем ID поездок
        if (user.getTripInPlanning() != null) {
            dbo.setTripInPlanning(new ObjectId(user.getTripInPlanning().getId()));
        }
        if (user.getOngoingTrip() != null) {
            dbo.setOngoingTrip(new ObjectId(user.getOngoingTrip().getId()));
        }
        if (user.getPlannedTrips() != null) {
            dbo.setPlannedTrips(user.getPlannedTrips().stream()
                    .map(trip -> new ObjectId(trip.getId()))
                    .collect(Collectors.toList()));
        }
        if (user.getCurrentTrips() != null) {
            dbo.setCurrentTrips(user.getCurrentTrips().stream()
                    .map(trip -> new ObjectId(trip.getId()))
                    .collect(Collectors.toList()));
        }
        if (user.getTripHistory() != null) {
            dbo.setTripHistory(user.getTripHistory().stream()
                    .map(trip -> new ObjectId(trip.getId()))
                    .collect(Collectors.toList()));
        }
        dbo.setPoints(user.getPoints());

        return dbo;
    }

    public User fromDbo(UserDBO dbo) {
        if (dbo == null) return null;

        User user = new User();
        user.setChatId(dbo.getChatId());
        user.setPoints(dbo.getPoints());

        // Создаем временные объекты Trip с ID для последующей загрузки
        if (dbo.getTripInPlanning() != null) {
            Trip trip = new Trip();
            trip.setId(dbo.getTripInPlanning().toHexString());
            user.setTripInPlanning(trip);
        }
        if (dbo.getOngoingTrip() != null) {
            Trip trip = new Trip();
            trip.setId(dbo.getOngoingTrip().toHexString());
            user.setOngoingTrip(trip);
        }
        if (dbo.getPlannedTrips() != null) {
            user.setPlannedTrips(dbo.getPlannedTrips().stream()
                    .map(id -> {
                        Trip trip = new Trip();
                        trip.setId(id.toHexString());
                        return trip;
                    })
                    .collect(Collectors.toList()));
        }
        if (dbo.getCurrentTrips() != null) {
            user.setCurrentTrips(dbo.getCurrentTrips().stream()
                    .map(id -> {
                        Trip trip = new Trip();
                        trip.setId(id.toHexString());
                        return trip;
                    })
                    .collect(Collectors.toList()));
        }
        if (dbo.getTripHistory() != null) {
            user.setTripHistory(dbo.getTripHistory().stream()
                    .map(id -> {
                        Trip trip = new Trip();
                        trip.setId(id.toHexString());
                        return trip;
                    })
                    .collect(Collectors.toList()));
        }

        return user;
    }
}
