package org.tripplanner.repositories.mongodb;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "trips")
public class TripDBO {

    @Id
    private ObjectId id;

    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer rating;

    private ObjectId startPoint;
    private List<ObjectId> points;
    private List<ObjectId> routes;

    // Конструкторы
    public TripDBO() {
        this.points = Collections.emptyList();
        this.routes = Collections.emptyList();
    }

    public TripDBO(String name, LocalDate startDate, LocalDate endDate, Integer rating,
                   ObjectId startPoint, List<ObjectId> points, List<ObjectId> routes) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rating = rating;
        this.startPoint = startPoint;
        this.points = points != null ? points : Collections.emptyList();
        this.routes = routes != null ? routes : Collections.emptyList();
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public ObjectId getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(ObjectId startPoint) {
        this.startPoint = startPoint;
    }

    public List<ObjectId> getPoints() {
        return points;
    }

    public void setPoints(List<ObjectId> points) {
        this.points = points;
    }

    public List<ObjectId> getRoutes() {
        return routes;
    }

    public void setRoutes(List<ObjectId> routes) {
        this.routes = routes;
    }
}
