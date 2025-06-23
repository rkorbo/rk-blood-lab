package com.rk.bloodlab.dto;

import lombok.*;

@Data
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LabReportRequest {

    private String patientName;
    private int age;
    private String gender;
    private String refBy;
    private long regOn;
    private long uhid;
    private String investigation;
    private String registeredOn;
    private String collectedOn;
    private String receivedOn;
    private String reportedOn;
    private String patientPhone;
    private boolean sendWhatsApp;
    private ReportDetail details;

}