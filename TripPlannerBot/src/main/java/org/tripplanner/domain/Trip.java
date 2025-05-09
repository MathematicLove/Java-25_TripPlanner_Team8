package org.tripplanner.domain;

import java.time.LocalDate;
import java.util.List;

public class Trip {

    private String id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer rating;
    private Point startPoint;
    private List<Point> points;
    private List<Route> routes;

    public Trip() {
    }

    public Trip(String name, LocalDate startDate, LocalDate endDate, Integer rating,
                Point startPoint, List<Point> points, List<Route> routes) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.rating = rating;
        this.startPoint = startPoint;
        this.points = points;
        this.routes = routes;
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

    public Point getStartPoint() {
        return startPoint;
    }

    public void setStartPoint(Point startPoint) {
        this.startPoint = startPoint;
    }

    public List<Point> getPoints() {
        return points;
    }

    public void setPoints(List<Point> points) {
        this.points = points;
    }

    public List<Route> getRoutes() {
        return routes;
    }

    public void setRoutes(List<Route> routes) {
        this.routes = routes;
    }
}
