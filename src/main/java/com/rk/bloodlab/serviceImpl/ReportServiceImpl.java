package com.rk.bloodlab.serviceImpl;

import com.itextpdf.text.DocumentException;
import com.rk.bloodlab.dto.LabReportRequest;
import com.rk.bloodlab.service.ReportService;
import com.rk.bloodlab.service.WhatsAppService;
import com.rk.bloodlab.service.EmailService;
import com.rk.bloodlab.utility.PdfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.logging.Logger;
import java.nio.file.Path;


@Service
public class ReportServiceImpl implements ReportService {

    private static final Logger logger = Logger.getLogger(ReportServiceImpl.class.getName());

    @Autowired
    PdfUtil pdfUtil;

    @Autowired
    WhatsAppService whatsAppService;

    @Autowired
    EmailService emailService;

    @Value("${lab.technician.email}")
    private String technicianEmail;

    @Override
    public void writeLines(String line, LabReportRequest request) {
        StringBuilder updated = null;
        String[] arr = line.split("#");
        for(String s:arr) {
            if(s.contains("LAB NAME: ______________________")) {
                 updated = new StringBuilder("LAB NAME:" + request.getPatientName());
            } else if(s.contains("DATE:__________________")) {
                 updated.append(" DATE:" + request.getRefBy());
            }
        }
        System.out.println(updated);
    }

    @Override
    public Path processReport(LabReportRequest request) throws DocumentException, IOException {
        // Generate the PDF report into Documents\RK-Blood-Lab\Reports\<Patient_Name>\
        Path outputPath = pdfUtil.createBloodReport(request);
        String reportFileName = outputPath.getFileName().toString();
        String pdfFilePath = outputPath.toAbsolutePath().toString();
        
        logger.info("PDF report generated: " + reportFileName);
        logger.info("PDF file path: " + pdfFilePath);

        // Send the PDF to the lab technician via email
        String subject = "New Blood Report: " + request.getPatientName();
        String body = "Dear Lab Technician,\n\nA new blood report has been generated for patient: " + request.getPatientName() + ".\nPlease find the report attached.\n\nRegards,\nRK Blood Lab";
        boolean emailSent = emailService.sendEmailWithAttachment(
            technicianEmail,
            subject,
            body,
            pdfFilePath
        );
        if (emailSent) {
            logger.info("Report emailed successfully to lab technician: " + technicianEmail);
        } else {
            logger.warning("Failed to email report to lab technician: " + technicianEmail);
        }

        return outputPath;
    }
}
