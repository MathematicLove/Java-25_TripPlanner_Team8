package org.tripplanner.domain;

import java.util.List;

public class User {

    private Long chatId;
    private Trip tripInPlanning;
    private List<Trip> plannedTrips;
    private Trip ongoingTrip;
    private List<Trip> tripHistory;

    public User() {
    }

    public User(Long chatId, Trip tripInPlanning, List<Trip> plannedTrips,
                Trip ongoingTrip, List<Trip> tripHistory) {
        this.chatId = chatId;
        this.tripInPlanning = tripInPlanning;
        this.plannedTrips = plannedTrips;
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
}
