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
import java.util.logging.Level;
import java.io.File;


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
    public void processReport(LabReportRequest request) throws DocumentException, IOException {
        // Generate the PDF report
        String reportFileName = request.getPatientName() + "_CreatedPdfNew.pdf";
        pdfUtil.createBloodReport(request);
        
        // Get the full path to the generated PDF file
        String pdfFilePath = getPdfFilePath(reportFileName);
        
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
    }
    
    /**
     * Get the full path to the generated PDF file
     * @param reportFileName Name of the report file
     * @return Full path to the PDF file
     */
    private String getPdfFilePath(String reportFileName) {
        // Get the current working directory
        String currentDir = System.getProperty("user.dir");
        
        // Construct the full path to the PDF file
        String pdfFilePath = currentDir + File.separator + reportFileName;
        
        logger.info("Current directory: " + currentDir);
        logger.info("Constructed PDF path: " + pdfFilePath);
        
        return pdfFilePath;
    }
}
