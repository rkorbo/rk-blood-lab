package com.rk.bloodlab.controller;

import com.itextpdf.text.DocumentException;
import com.rk.bloodlab.auth.AuthenticationService;
import com.rk.bloodlab.dto.LabReportRequest;
import com.rk.bloodlab.service.ReportService;
import com.rk.bloodlab.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@CrossOrigin
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class LabController {

    @Autowired
    private final AuthenticationService service;

    @Autowired
    private ReportService labUser;

    @Autowired
    private WhatsAppService whatsAppService;

    @PostMapping("/report")
    public ResponseEntity<?> register(
            @RequestBody LabReportRequest request
    ) {
        Map<String, String> body = new HashMap<>();
        body.put("message", request.toString());
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @PostMapping("/lab")
    public ResponseEntity<?> lab(
            @RequestBody LabReportRequest request
    ) throws DocumentException, IOException {

        Map<String, Object> response = new HashMap<>();

        try {
            // Debug logging
            System.out.println("=== DEBUG INFO ===");
            System.out.println("Patient Name: " + request.getPatientName());
            System.out.println("Patient Phone: " + request.getPatientPhone());
            System.out.println("Send WhatsApp: " + request.isSendWhatsApp());
            System.out.println("=== END DEBUG ===");
            
            // Process the report
        labUser.processReport(request);
            
            // Build success message
            StringBuilder message = new StringBuilder("Report generated successfully! PDF file has been created.");
            
            // Add WhatsApp status to response
            if (request.isSendWhatsApp()) {
                if (request.getPatientPhone() != null && !request.getPatientPhone().trim().isEmpty()) {
                    message.append(" WhatsApp notification sent to patient.");
                    response.put("whatsappStatus", "sent");
                    response.put("whatsappNumber", request.getPatientPhone());
                } else {
                    message.append(" WhatsApp not sent - patient phone number missing.");
                    response.put("whatsappStatus", "failed");
                    response.put("whatsappError", "Phone number required");
                }
            } else {
                response.put("whatsappStatus", "not_requested");
            }
            
            response.put("message", message.toString());
            response.put("patientName", request.getPatientName());
            response.put("reportFileName", request.getPatientName() + "_CreatedPdfNew.pdf");
            response.put("status", "success");
            
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            response.put("message", "Error generating report: " + e.getMessage());
            response.put("status", "error");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/test")
    public void classify(@RequestBody LabReportRequest request) throws IOException {
        try (PDDocument document = PDDocument.load(new File(Objects.requireNonNull(getClass().getResource("/data/LABTEMPLATE.pdf")).getFile()))) {
            document.getClass();
            if (!document.isEncrypted()) {
                PDFTextStripperByArea stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                PDFTextStripper tStripper = new PDFTextStripper();
                String pdfFileInText = tStripper.getText(document);
                //System.out.println("Text:" + st);
                // split by whitespace
                String[] lines = pdfFileInText.split("\\r?\\n");

                for (String line : lines) {
                    labUser.writeLines(line, request);
                //    System.out.println(line);
                }
            }
        }
    }

    @PostMapping("/test-whatsapp")
    public ResponseEntity<Map<String, Object>> testWhatsApp(@RequestBody Map<String, String> request) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            String phoneNumber = request.get("phoneNumber");
            String patientName = request.get("patientName");
            
            if (phoneNumber == null || patientName == null) {
                response.put("success", false);
                response.put("message", "Phone number and patient name are required");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Test WhatsApp service
            boolean result = whatsAppService.sendReportNotification(phoneNumber, patientName, "Test_Report.pdf");
            
            response.put("success", result);
            response.put("message", result ? "WhatsApp test message sent successfully" : "Failed to send WhatsApp message");
            response.put("phoneNumber", phoneNumber);
            response.put("patientName", patientName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error testing WhatsApp: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/test-twilio-config")
    public ResponseEntity<Map<String, Object>> testTwilioConfig() {
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Test Twilio configuration
            boolean isConfigured = whatsAppService.isTwilioConfigured();
            
            response.put("success", true);
            response.put("twilioConfigured", isConfigured);
            response.put("message", isConfigured ? "Twilio is properly configured" : "Twilio configuration is missing or invalid");
            
            // Add configuration details (without exposing sensitive data)
            response.put("accountSidPresent", whatsAppService.getAccountSid() != null && !whatsAppService.getAccountSid().equals("your_account_sid_here"));
            response.put("authTokenPresent", whatsAppService.getAuthToken() != null && !whatsAppService.getAuthToken().equals("your_auth_token_here"));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error testing Twilio configuration: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
