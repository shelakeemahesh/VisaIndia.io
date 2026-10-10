package com.visasetu.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import com.visasetu.domain.entity.Trip;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class DocumentGenerationService {

    /**
     * Generate official ETA PDF grant letter using OpenPDF.
     */
    public byte[] generateEtaPdf(Trip trip) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Header Title
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("ELECTRONIC TRAVEL AUTHORIZATION (ETA)", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph sub = new Paragraph("GOVERNMENT OF INDIA - MINISTRY OF HOME AFFAIRS\nBUREAU OF IMMIGRATION", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            // Concept Disclaimer
            Font disclFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9);
            Paragraph discl = new Paragraph("Concept Project Demonstration — Not Affiliated with the Government of India", disclFont);
            discl.setAlignment(Element.ALIGN_CENTER);
            discl.setSpacingAfter(20);
            document.add(discl);

            // Table with applicant particulars
            Table table = new Table(2);
            table.setWidth(100);
            table.setBorderWidth(1);
            table.setPadding(6);

            table.addCell(new Paragraph("Application Reference ID:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getReferenceId(), FontFactory.getFont(FontFactory.COURIER_BOLD, 10)));

            table.addCell(new Paragraph("ETA Number:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getEtaNumber() != null ? trip.getEtaNumber() : "ETA-GRANTED-PENDING", FontFactory.getFont(FontFactory.COURIER_BOLD, 10)));

            table.addCell(new Paragraph("Applicant Full Name:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getApplicantName().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Nationality:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getNationality(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Visa Category:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getCategoryName(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Application Status:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getStatus(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));

            table.addCell(new Paragraph("Designated Port of Entry:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getPortOfArrival() != null ? trip.getPortOfArrival() : "37 Authorized Airports", FontFactory.getFont(FontFactory.HELVETICA, 10)));

            document.add(table);

            // Instructions
            Paragraph instr = new Paragraph("\nIMPORTANT TRAVEL INSTRUCTIONS:\n" +
                    "1. The applicant must carry a physical printout of this ETA confirmation at the time of travel.\n" +
                    "2. The passport must be valid for at least 6 months from the date of arrival with at least 2 blank pages.\n" +
                    "3. Biometrics of the applicant shall be mandatorily captured at immigration on arrival in India.\n" +
                    "4. This ETA is strictly non-extendable and non-convertible.",
                    FontFactory.getFont(FontFactory.HELVETICA, 9));
            instr.setSpacingBefore(15);
            document.add(instr);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate ETA PDF: {}", e.getMessage(), e);
            throw new RuntimeException("ETA PDF generation error: " + e.getMessage());
        }
    }

    /**
     * Generate PNG QR code byte array using ZXing.
     */
    public byte[] generateQrCodeImage(String text, int width, int height) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate QR Code image: {}", e.getMessage(), e);
            throw new RuntimeException("QR generation error: " + e.getMessage());
        }
    }
}
