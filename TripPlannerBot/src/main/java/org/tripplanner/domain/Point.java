package org.tripplanner.domain;

import java.util.List;

public class Point {

    private String id;
    private String name;
    private double latitude;
    private double longitude;
    private boolean visited;
    private List<String> notes;

    public Point() {
    }

    public Point(String name, double latitude, double longitude, boolean visited, List<String> notes) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.visited = visited;
        this.notes = notes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isVisited() {
        return visited;
    }

    public void setVisited(boolean visited) {
        this.visited = visited;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(List<String> notes) {
        this.notes = notes;
    }
}
