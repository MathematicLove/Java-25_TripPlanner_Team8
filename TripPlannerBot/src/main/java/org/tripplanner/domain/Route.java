package org.tripplanner.domain;

import java.time.LocalDate;

public class Route {

    private LocalDate startDate;
    private LocalDate endDate;
    private Point pointTo;

    public Route() {
    }

    public Route(LocalDate startDate, LocalDate endDate, Point pointTo) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.pointTo = pointTo;
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

    public Point getPointTo() {
        return pointTo;
    }

    public void setPointTo(Point pointTo) {
        this.pointTo = pointTo;
    }
}
