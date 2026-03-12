package com.rk.bloodlab.utility;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.rk.bloodlab.dto.LabReportRequest;
import com.rk.bloodlab.dto.ReportDetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;

@Component
public class PdfUtil {

    @Value("${range.hemo_min}")
    private long hemo_min;

    @Value("${range.hemo_max}")
    private long hemo_max;

    @Value("${range.leu_min}")
    private long leu_min;

    @Value("${range.leu_max}")
    private long leu_max;

    @Value("${range.mch_min}")
    private long mch_min;

    @Value("${range.mch_max}")
    private long mch_max;

    @Value("${range.mchc_min}")
    private double mchc_min;

    @Value("${range.mchc_max}")
    private double mchc_max;

    @Value("${range.pdw_min}")
    private double pdw_min;

    @Value("${range.pdw_max}")
    private double pdw_max = 15.2;

    @Value("${footer.dr_name}")
    private String dr_name;

    @Value("${footer.dr_degree}")
    private String dr_degree;

    @Value("${footer.tech_name}")
    private String tech_name;

    @Value("${footer.tech_pos}")
    private String tech_pos;

    @Autowired
    private JavaMailSender mailSender;

    @Value("${report.template.path:data/lab_editable_final.pdf}")
    private String reportTemplatePath;

    @Value("${report.output.root:}")
    private String reportOutputRoot;

    public void createLabPdf(LabReportRequest request) throws IOException, DocumentException {

        PdfReader reader = new PdfReader(AppConstants.LAB_TEMPLATE);
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("CreatedPdf.pdf"));
        AcroFields form = stamper.getAcroFields();
        form.setField(AppConstants.LAB_NAME, "Test Lab");
        form.setField(AppConstants.DATE, "12/09/2023");
        form.setField(AppConstants.PATIENT_NAME, "Rahim");
        form.setField(AppConstants.GENDER, "Male");
        form.setField(AppConstants.REFERRED_BY, "Prath");
        form.setField(AppConstants.AGE, "26");
        stamper.close();
    }

    public static void main(String[] args) throws DocumentException, IOException {
        PdfUtil pdfUtil = new PdfUtil();
        pdfUtil.createBloodReport(LabReportRequest.builder().patientName("Rahim").age(30).details(ReportDetail.builder().hemoglobin(20).build()).build());
    }

    public Path createBloodReport(LabReportRequest request) throws IOException, DocumentException {

        ReportDetail detail = request.getDetails();

        Path outputPath = resolveReportOutputPath(request.getPatientName());

        if (isNonFillableTemplate(reportTemplatePath)) {
            createBloodReportOverlay(reportTemplatePath, request, detail, outputPath);
        } else {
            PdfReader reader = createPdfReader(reportTemplatePath);
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(outputPath.toFile()));
            AcroFields form = stamper.getAcroFields();

            form.setField(AppConstants.PATIENT_NAME, request.getPatientName());
            form.setField(AppConstants.AGE, request.getAge() + AppConstants.SEPARATOR + request.getGender());
            form.setField(AppConstants.REFERRED_BY, request.getRefBy());
            form.setField(AppConstants.REG_NO, request.getRegOn() + AppConstants.SEPARATOR + request.getUhid());
            form.setField(AppConstants.INVESTIGATION, request.getInvestigation());
            form.setField(AppConstants.REG_ON, request.getRegisteredOn());
            form.setField(AppConstants.COLL_ON, request.getCollectedOn());
            form.setField(AppConstants.REC_ON, request.getReceivedOn());
            form.setField(AppConstants.REPO_ON, request.getReportedOn());

            form.setField(AppConstants.HEMO, String.valueOf(detail.getHemoglobin()));
            form.setField(AppConstants.HEMO_INIT, getInitValue(detail.getHemoglobin(), hemo_min, hemo_max));

            form.setField(AppConstants.LEU, String.valueOf(detail.getLeukocyte()));
            form.setField(AppConstants.LEU_INIT, getInitValue(detail.getLeukocyte(), leu_min, leu_max));

            form.setField(AppConstants.NEU, String.valueOf(detail.getNeutrophils()));
            form.setField(AppConstants.LYMP, String.valueOf(detail.getLymphocyte()));
            form.setField(AppConstants.EOSIN, String.valueOf(detail.getEosinophils()));
            form.setField(AppConstants.MONO, String.valueOf(detail.getMonocytes()));
            form.setField(AppConstants.BASO, String.valueOf(detail.getBasophils()));

            form.setField(AppConstants.PLATE, String.valueOf(detail.getPlatelet()));
            form.setField(AppConstants.RBC, String.valueOf(detail.getRbc()));
            form.setField(AppConstants.HCT, String.valueOf(detail.getHct()));
            form.setField(AppConstants.MCV, String.valueOf(detail.getMcv()));

            form.setField(AppConstants.MCH, String.valueOf(detail.getMch()));
            form.setField(AppConstants.MCH_INIT, getInitValue(detail.getMch(), mch_min, mch_max));
            form.setField(AppConstants.MCHC, String.valueOf(detail.getMchc()));
            form.setField(AppConstants.MCHC_INIT, getInitValue(detail.getMchc(), mchc_min, mchc_max));

            form.setField(AppConstants.MPV, String.valueOf(detail.getMpv()));
            form.setField(AppConstants.SD, String.valueOf(detail.getSd()));
            form.setField(AppConstants.CV, String.valueOf(detail.getCv()));
            form.setField(AppConstants.LCR, String.valueOf(detail.getLcr()));

            form.setField(AppConstants.PDW, String.valueOf(detail.getPdw()));
            form.setField(AppConstants.PDW_INIT, getInitValue(detail.getPdw(), pdw_min, pdw_max));

            form.setField(AppConstants.ABO, detail.getAbo());
            form.setField(AppConstants.RH, detail.getRh());

            form.setField(AppConstants.DR_NAME, dr_name);
            form.setField(AppConstants.DR_DEGREE, dr_degree);
            form.setField(AppConstants.TECH_NAME, tech_name);
            form.setField(AppConstants.TECH_POS, tech_pos);

            stamper.close();
        }
        return outputPath;
    }

    private boolean isNonFillableTemplate(String templatePath) {
        try (PDDocument doc = openTemplateAsPdDocument(templatePath)) {
            PDAcroForm form = doc.getDocumentCatalog().getAcroForm();
            return form == null;
        } catch (Exception e) {
            // If we can't inspect, fall back to AcroFields path (existing behavior)
            return false;
        }
    }

    private PDDocument openTemplateAsPdDocument(String templatePath) throws IOException {
        if (templatePath != null && templatePath.startsWith("classpath:")) {
            String resourcePath = templatePath.substring("classpath:".length());
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }
            InputStream in = PdfUtil.class.getResourceAsStream(resourcePath);
            if (in == null) {
                throw new IOException("Template not found on classpath: " + templatePath);
            }
            return PDDocument.load(in);
        }
        return PDDocument.load(new File(templatePath));
    }

    /**
     * Overlay-based template filling for non-fillable PDFs.
     * Coordinates are derived from the provided WorkingPDF sample (page 1).
     * PDFBox uses bottom-left origin for drawing; extracted yDirAdj is from top,
     * so we convert by: yDraw = pageHeight - yDirAdj.
     */
    private void createBloodReportOverlay(String templatePath,
                                         LabReportRequest request,
                                         ReportDetail detail,
                                         Path outputPath) throws IOException {
        try (PDDocument doc = openTemplateAsPdDocument(templatePath)) {
            if (doc.getNumberOfPages() < 1) {
                throw new IOException("Template has no pages: " + templatePath);
            }

            PDPage page = doc.getPage(0);
            PDRectangle box = page.getMediaBox();
            float pageHeight = box.getHeight();

            try (PDPageContentStream cs = new PDPageContentStream(
                    doc,
                    page,
                    PDPageContentStream.AppendMode.APPEND,
                    true,
                    true
            )) {
                // Standard fonts (no embedding). For Kannada/Unicode later, embed a TTF.

                // -------------------------
                // Patient details (2 columns)
                // -------------------------
                final float leftLabelX = 80f;
                final float leftValueX = 180f;
                final float rightLabelX = 350f;
                final float rightValueX = 440f;

                final float patientRow1Y = 150f;
                final float patientRow2Y = 170f;

                cs.setFont(PDType1Font.HELVETICA, 11);
                drawAtTopCoords(cs, pageHeight, leftLabelX, patientRow1Y, "Patient Name");
                drawAtTopCoords(cs, pageHeight, leftLabelX, patientRow2Y, "Referred By");
                drawAtTopCoords(cs, pageHeight, rightLabelX, patientRow1Y, "Age/Sex");
                drawAtTopCoords(cs, pageHeight, rightLabelX, patientRow2Y, "Date");

                cs.setFont(PDType1Font.HELVETICA_BOLD, 11);
                drawAtTopCoords(cs, pageHeight, leftValueX, patientRow1Y, safeUpper(request.getPatientName()));
                drawAtTopCoords(cs, pageHeight, leftValueX, patientRow2Y, safeUpper(request.getRefBy()));
                drawAtTopCoords(cs, pageHeight, rightValueX, patientRow1Y, formatAgeSex(request));
                drawAtTopCoords(cs, pageHeight, rightValueX, patientRow2Y, effectiveDate(request));

                // -------------------------
                // Section title
                // -------------------------
                cs.setFont(PDType1Font.HELVETICA_BOLD, 13);

                // title
                drawCenteredAtTopCoords(cs, pageHeight, 305f, 205f,
                        "COMPLETE BLOOD COUNT REPORT",
                        PDType1Font.HELVETICA_BOLD,
                        13f);

                // One minimal separator line below title
                cs.setLineWidth(0.7f);
                cs.setStrokingColor(0);
                strokeDashedHorizontalRuleAtTopY(cs, pageHeight, 218f, 70f, 540f);

                // -------------------------
                // 4-column table layout
                // Column 1: Test Name
                // Column 2: Result
                // Column 3: Unit
                // Column 4: Normal Range
                // -------------------------
                final float testNameX = 80f;
                final float resultCenterX = 330f; // centered column
                final float unitX = 400f;
                final float rangeX = 470f;

                final float tableLineLeftX = 70f;
                final float tableLineRightX = 540f;

                final float tableStartY = 244f; // slightly lower for better spacing
                final float rowH = 13f; // compact spacing

                // Header row (optional but professional)
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10f);
                final float headerTextY = tableStartY;

                drawAtTopCoords(cs, pageHeight, testNameX, headerTextY, "Test Name");
                drawCenteredAtTopCoords(cs, pageHeight, resultCenterX, headerTextY, "Result", PDType1Font.HELVETICA_BOLD, 10f);
                drawAtTopCoords(cs, pageHeight, unitX, headerTextY, "Unit");
                drawAtTopCoords(cs, pageHeight, rangeX, headerTextY, "Normal Range");

                // Data rows begin below header
                final int baseRow = 1;

                // Helper lambdas (Java 8 compatible via small local method usage)
                int r = baseRow;

                // WBC
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "W.B.C. Total Count",
                        formatWbc(detail.getLeukocyte()),
                        "cels/cumm",
                        "5000-10000");

                // Differential count
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Neutrophils",
                        String.valueOf(detail.getNeutrophils()),
                        "%",
                        "40-75");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Lymphocytes",
                        String.valueOf(detail.getLymphocyte()),
                        "%",
                        "20-45");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Eosinophils",
                        String.valueOf(detail.getEosinophils()),
                        "%",
                        "1-6");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Monocytes",
                        String.valueOf(detail.getMonocytes()),
                        "%",
                        "1-6");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Basophils",
                        String.valueOf(detail.getBasophils()),
                        "%",
                        "0-1");

                // RBC + indices
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "RBC Count",
                        formatDouble(detail.getRbc(), 2),
                        "M/cumm",
                        "4.2-5.8");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Haemoglobin",
                        formatDouble(detail.getHemoglobin(), 1),
                        "gm/dl",
                        "11.5-16.0");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Packed Cell Volume",
                        String.valueOf(detail.getHct()),
                        "%",
                        "36.0-55.0");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "M.C.V.",
                        formatDouble(detail.getMcv(), 1),
                        "fl",
                        "80-100");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "MCH",
                        formatDouble(detail.getMch(), 1),
                        "pg",
                        "26.0-34.0");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "MCHC",
                        formatDouble(detail.getMchc(), 1),
                        "gm/dl",
                        "30.0-37.0");
                drawCbcRow(cs, pageHeight, tableStartY, rowH, r++,
                        testNameX, resultCenterX, unitX, rangeX,
                        "Platelet Count",
                        formatDouble(detail.getPlatelet(), 2),
                        "LCumm",
                        "1.4-4.5");

                // Bottom border after last row
                final int lastRowIdx = r - 1;
                strokeDashedHorizontalRuleAtTopY(cs, pageHeight, tableStartY + (lastRowIdx * rowH) + 8f, tableLineLeftX, tableLineRightX);
            }

            doc.save(outputPath.toFile());
        }
    }

    private void drawCbcRow(PDPageContentStream cs,
                            float pageHeight,
                            float tableStartY,
                            float rowH,
                            int rowIdx,
                            float testNameX,
                            float resultCenterX,
                            float unitX,
                            float rangeX,
                            String testName,
                            String result,
                            String unit,
                            String range) throws IOException {
        float y = tableStartY + (rowIdx * rowH);

        // Test name (normal)
        cs.setFont(PDType1Font.HELVETICA, 10.5f);
        drawAtTopCoords(cs, pageHeight, testNameX, y, testName);

        // Result (bold)
        cs.setFont(PDType1Font.HELVETICA_BOLD, 10.5f);
        drawCenteredAtTopCoords(cs, pageHeight, resultCenterX, y, result, PDType1Font.HELVETICA_BOLD, 10.5f);

        // Unit & range (slightly smaller)
        cs.setFont(PDType1Font.HELVETICA, 9.5f);
        drawAtTopCoords(cs, pageHeight, unitX, y, unit);
        drawAtTopCoords(cs, pageHeight, rangeX, y, range);
    }

    private void drawAtTopCoords(PDPageContentStream cs, float pageHeight, float x, float yFromTop, String text) throws IOException {
        if (text == null) return;
        String t = text.trim();
        if (t.isEmpty()) return;

        float y = pageHeight - yFromTop;
        cs.beginText();
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private void drawCenteredAtTopCoords(PDPageContentStream cs,
                                         float pageHeight,
                                         float centerX,
                                         float yFromTop,
                                         String text,
                                         PDType1Font font,
                                         float fontSize) throws IOException {
        if (text == null) return;
        String t = text.trim();
        if (t.isEmpty()) return;

        float textW = (font.getStringWidth(t) / 1000f) * fontSize;
        float x = centerX - (textW / 2f);
        float y = pageHeight - yFromTop;

        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(x, y);
        cs.showText(t);
        cs.endText();
    }

    private void strokeHorizontalRuleAtTopY(PDPageContentStream cs,
                                            float pageHeight,
                                            float yFromTop,
                                            float x1,
                                            float x2) throws IOException {
        float y = pageHeight - yFromTop;
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private void strokeDashedHorizontalRuleAtTopY(PDPageContentStream cs,
                                                  float pageHeight,
                                                  float yFromTop,
                                                  float x1,
                                                  float x2) throws IOException {
        // Dash style similar to typical lab report separators
        cs.setLineDashPattern(new float[]{8f, 5f}, 0f);
        strokeHorizontalRuleAtTopY(cs, pageHeight, yFromTop, x1, x2);
        // Reset to solid to avoid affecting other drawing
        cs.setLineDashPattern(new float[]{}, 0f);
    }

    private String safeUpper(String v) {
        if (v == null) return "";
        return v.trim().toUpperCase(Locale.ROOT);
    }

    private String formatAgeSex(LabReportRequest request) {
        String sex = request.getGender() == null ? "" : request.getGender().trim();
        if (sex.equalsIgnoreCase("male") || sex.equalsIgnoreCase("m")) sex = "M";
        else if (sex.equalsIgnoreCase("female") || sex.equalsIgnoreCase("f")) sex = "F";
        else if (!sex.isEmpty()) sex = sex.substring(0, 1).toUpperCase(Locale.ROOT);
        return request.getAge() + "Y " + sex;
    }

    private String safeDate(String v) {
        if (v == null) return "";
        return v.trim().replace('-', '.').replace('/', '.');
    }

    private String effectiveDate(LabReportRequest request) {
        String v = request.getReportedOn();
        if (v == null || v.trim().isEmpty()) {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        }
        return safeDate(v);
    }

    private String formatWbc(long wbc) {
        // Sample shows "8.000" style; keep 3 decimals by dividing if required is unknown.
        // If backend already sends absolute count (e.g. 8000), format as 8.000 by /1000.
        if (wbc >= 1000) {
            return formatDouble(wbc / 1000.0, 3);
        }
        return formatDouble(wbc, 3);
    }

    private String formatDouble(double v, int decimals) {
        return String.format(Locale.US, "%." + decimals + "f", v);
    }

    private PdfReader createPdfReader(String templatePath) throws IOException {
        if (templatePath != null && templatePath.startsWith("classpath:")) {
            String resourcePath = templatePath.substring("classpath:".length());
            if (!resourcePath.startsWith("/")) {
                resourcePath = "/" + resourcePath;
            }
            InputStream in = PdfUtil.class.getResourceAsStream(resourcePath);
            if (in == null) {
                throw new IOException("Template not found on classpath: " + templatePath);
            }
            return new PdfReader(in);
        }
        return new PdfReader(templatePath);
    }

    public Path resolveReportOutputPath(String patientName) throws IOException {
        Path baseDir;
        if (reportOutputRoot != null && !reportOutputRoot.trim().isEmpty()) {
            baseDir = Paths.get(reportOutputRoot.trim());
        } else {
            String userHome = System.getProperty("user.home");
            Path documents = Paths.get(userHome, "Documents");
            baseDir = Files.exists(documents) ? documents : Paths.get(userHome);
            baseDir = baseDir.resolve("RK-Blood-Lab").resolve("Reports");
        }

        String safePatient = sanitizePathSegment(patientName);
        Path patientDir = baseDir.resolve(safePatient);
        Files.createDirectories(patientDir);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = safePatient + "_" + timestamp + ".pdf";
        return patientDir.resolve(fileName);
    }

    private String sanitizePathSegment(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }
        // Windows-illegal: \ / : * ? " < > | and control chars
        String sanitized = value.trim().replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]+", "_");
        sanitized = sanitized.replaceAll("\\s+", " ").trim();
        if (sanitized.isEmpty()) {
            return "Unknown";
        }
        return sanitized.length() > 80 ? sanitized.substring(0, 80).trim() : sanitized;
    }

    private String getInitValue(double value, double min, double max) {

        return value < min
                ? "L"
                : value > max
                ? "H"
                : "";

    }

    public void sendEmailWithAttachment(String to, String subject, String text, String attachmentFilePath, String filename) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text);

            // Attach the PDF file
            helper.addAttachment(filename, new File(attachmentFilePath));

            mailSender.send(message);
        } catch (javax.mail.MessagingException e) {
            e.printStackTrace();
            // Handle the exception
        }
    }
}