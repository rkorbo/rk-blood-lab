package com.rk.bloodlab.serviceImpl;

import com.itextpdf.text.DocumentException;
import com.rk.bloodlab.dto.LabReportRequest;
import com.rk.bloodlab.service.ReportService;
import com.rk.bloodlab.service.WhatsAppService;
import com.rk.bloodlab.utility.PdfUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
        
        // Send WhatsApp notification if technician approved and phone number is provided
        if (request.isSendWhatsApp() && request.getPatientPhone() != null && !request.getPatientPhone().trim().isEmpty()) {
            try {
                // Check if PDF file exists before sending
                File pdfFile = new File(pdfFilePath);
                if (pdfFile.exists()) {
                    logger.info("PDF file exists, sending with attachment");
                    boolean whatsAppSent = whatsAppService.sendReportWithPdf(
                        request.getPatientPhone(), 
                        request.getPatientName(), 
                        reportFileName,
                        pdfFilePath
                    );
                    
                    if (whatsAppSent) {
                        logger.info("WhatsApp notification with PDF sent successfully to " + request.getPatientPhone());
                    } else {
                        logger.warning("Failed to send WhatsApp notification with PDF to " + request.getPatientPhone());
                    }
                } else {
                    logger.warning("PDF file not found, sending text-only notification");
                    boolean whatsAppSent = whatsAppService.sendReportNotification(
                        request.getPatientPhone(), 
                        request.getPatientName(), 
                        reportFileName
                    );
                    
                    if (whatsAppSent) {
                        logger.info("WhatsApp text notification sent successfully to " + request.getPatientPhone());
                    } else {
                        logger.warning("Failed to send WhatsApp text notification to " + request.getPatientPhone());
                    }
                }
                
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error sending WhatsApp notification", e);
            }
        } else if (request.isSendWhatsApp() && (request.getPatientPhone() == null || request.getPatientPhone().trim().isEmpty())) {
            logger.warning("WhatsApp requested but no phone number provided for patient: " + request.getPatientName());
        }
        
        // Email functionality (currently commented out)
        String to = "rkorbo@gmail.com";
        String subject = "Sample PDF Email";
        String text = "Here is your PDF attachment.";
        String attachmentFilePath = "/home/rahim/LocalProjects/rk-blood-lab/";
        //pdfUtil.sendEmailWithAttachment(to, subject, text, attachmentFilePath, reportFileName);
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
