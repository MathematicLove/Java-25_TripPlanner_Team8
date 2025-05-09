package org.tripplanner.repositories.mongodb;

import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "points")
public class PointDBO {

    @Id
    private ObjectId id;

    private String name;
    private double latitude;
    private double longitude;
    private boolean visited;
    private List<String> notes;

    // Конструкторы
    public PointDBO() {}

    public PointDBO(String name, double latitude, double longitude, boolean visited, List<String> notes) {
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.visited = visited;
        this.notes = notes;
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
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
