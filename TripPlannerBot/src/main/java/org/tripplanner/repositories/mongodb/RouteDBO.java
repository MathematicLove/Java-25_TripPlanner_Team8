package org.tripplanner.repositories.mongodb;

import java.time.LocalDate;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "routes")
public class RouteDBO {

    @Id
    private ObjectId id;

    private LocalDate startDate;
    private LocalDate endDate;
    private ObjectId pointTo;

    // Конструкторы
    public RouteDBO() {}

    public RouteDBO(LocalDate startDate, LocalDate endDate, ObjectId pointTo) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.pointTo = pointTo;
    }

    // Getters and Setters
    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
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

    public ObjectId getPointTo() {
        return pointTo;
    }

    public void setPointTo(ObjectId pointTo) {
        this.pointTo = pointTo;
    }
}
