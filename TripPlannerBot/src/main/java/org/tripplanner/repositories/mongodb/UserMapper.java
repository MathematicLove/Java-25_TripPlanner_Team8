package org.tripplanner.repositories.mongodb;

import java.util.Collections;
import java.util.stream.Collectors;

import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;
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

        return dbo;
    }

    public User fromDbo(UserDBO dbo) {
        if (dbo == null) return null;

        User user = new User();
        user.setChatId(dbo.getChatId());

        // Trip-сущности загружаются в сервисе, пока null или пустые
        user.setTripInPlanning(null);
        user.setOngoingTrip(null);
        user.setPlannedTrips(Collections.emptyList());
        user.setCurrentTrips(Collections.emptyList());
        user.setTripHistory(Collections.emptyList());

        return user;
    }
}
