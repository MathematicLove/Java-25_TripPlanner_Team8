package org.tripplanner.repositories.mongodb;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
public class UserDBO {

    @Id
    private ObjectId id;

    private Long chatId;

    private ObjectId tripInPlanning;
    private ObjectId ongoingTrip;

    private List<ObjectId> plannedTrips;
    private List<ObjectId> currentTrips;  // Trips that are not finished yet
    private List<ObjectId> tripHistory;

    private List<ObjectId> points;

    // Конструкторы
    public UserDBO() {}

    public UserDBO(Long chatId, ObjectId tripInPlanning, ObjectId ongoingTrip,
                   List<ObjectId> plannedTrips, List<ObjectId> currentTrips, List<ObjectId> tripHistory) {
        this.chatId = chatId;
        this.tripInPlanning = tripInPlanning;
        this.ongoingTrip = ongoingTrip;
        this.plannedTrips = plannedTrips;
        this.currentTrips = currentTrips;
        this.tripHistory = tripHistory;
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public ObjectId getTripInPlanning() {
        return tripInPlanning;
    }

    public void setTripInPlanning(ObjectId tripInPlanning) {
        this.tripInPlanning = tripInPlanning;
    }

    public ObjectId getOngoingTrip() {
        return ongoingTrip;
    }

    public void setOngoingTrip(ObjectId ongoingTrip) {
        this.ongoingTrip = ongoingTrip;
    }

    public List<ObjectId> getPlannedTrips() {
        return plannedTrips;
    }

    public void setPlannedTrips(List<ObjectId> plannedTrips) {
        this.plannedTrips = plannedTrips;
    }

    public List<ObjectId> getCurrentTrips() {
        return currentTrips;
    }

    public void setCurrentTrips(List<ObjectId> currentTrips) {
        this.currentTrips = currentTrips;
    }

    public List<ObjectId> getTripHistory() {
        return tripHistory;
    }

    public void setTripHistory(List<ObjectId> tripHistory) {
        this.tripHistory = tripHistory;
    }

    public List<ObjectId> getPoints() {
        return points;
    }

    public void setPoints(List<ObjectId> points) {
        this.points = points;
    }
}
