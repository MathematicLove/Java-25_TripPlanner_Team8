package org.tripplanner.domain;

import java.util.ArrayList;
import java.util.List;

import org.bson.types.ObjectId;

public class User {

    private Long chatId;
    private Trip tripInPlanning;
    private List<Trip> plannedTrips;
    private List<Trip> currentTrips;  // Trips that are not finished yet
    private Trip ongoingTrip;
    private List<Trip> tripHistory;
    private List<ObjectId> points;

    public User() {
    }

    public User(Long chatId, Trip tripInPlanning, List<Trip> plannedTrips,
                List<Trip> currentTrips, Trip ongoingTrip, List<Trip> tripHistory) {
        this.chatId = chatId;
        this.tripInPlanning = tripInPlanning;
        this.plannedTrips = plannedTrips;
        this.currentTrips = currentTrips;
        this.ongoingTrip = ongoingTrip;
        this.tripHistory = tripHistory;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Trip getTripInPlanning() {
        return tripInPlanning;
    }

    public void setTripInPlanning(Trip tripInPlanning) {
        this.tripInPlanning = tripInPlanning;
    }

    public List<Trip> getPlannedTrips() {
        return plannedTrips;
    }

    public void setPlannedTrips(List<Trip> plannedTrips) {
        this.plannedTrips = plannedTrips;
    }

    public List<Trip> getCurrentTrips() {
        return currentTrips;
    }

    public void setCurrentTrips(List<Trip> currentTrips) {
        this.currentTrips = currentTrips;
    }

    public Trip getOngoingTrip() {
        return ongoingTrip;
    }

    public void setOngoingTrip(Trip ongoingTrip) {
        this.ongoingTrip = ongoingTrip;
    }

    public List<Trip> getTripHistory() {
        return tripHistory;
    }

    public void setTripHistory(List<Trip> tripHistory) {
        this.tripHistory = tripHistory;
    }

    public List<ObjectId> getPoints() {
        return points != null ? points : new ArrayList<>();
    }

    public void setPoints(List<ObjectId> points) {
        this.points = points;
    }
}
