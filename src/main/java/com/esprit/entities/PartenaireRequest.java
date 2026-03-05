package com.esprit.entities;

import java.time.LocalDateTime;

public class PartenaireRequest {
    private int id;
    private int userId;
    private String status; // EN_ATTENTE, ACCEPTE, REFUSE
    private LocalDateTime requestDate;
    private LocalDateTime responseDate;
    private Integer adminId;

    public PartenaireRequest() {}

    public PartenaireRequest(int userId) {
        this.userId = userId;
        this.status = "EN_ATTENTE";
        this.requestDate = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public LocalDateTime getResponseDate() { return responseDate; }
    public void setResponseDate(LocalDateTime responseDate) { this.responseDate = responseDate; }
    public Integer getAdminId() { return adminId; }
    public void setAdminId(Integer adminId) { this.adminId = adminId; }
}
