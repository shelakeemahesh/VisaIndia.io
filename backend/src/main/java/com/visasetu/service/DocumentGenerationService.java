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
            Paragraph title = new Paragraph("VISAINDIA.IO — VISA PREPARATION DOSSIER", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(5);
            document.add(title);

            Font subFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph sub = new Paragraph("INDEPENDENT TRAVEL DOCUMENT ASSISTANT & APPLICATION SUMMARY\nOFFICIAL APPLICATION PORTAL: HTTPS://INDIANVISAONLINE.GOV.IN", subFont);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(15);
            document.add(sub);

            // Statutory Disclaimer
            Font disclFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9);
            Paragraph discl = new Paragraph("STATUTORY NOTICE: THIS DOSSIER IS A PREPARATION RECORD AND CHECKLIST SUMMARY PROVIDED BY VISAINDIA.IO.\nIT IS NOT AN OFFICIAL VISA, NOT AN ELECTRONIC TRAVEL AUTHORIZATION (ETA), AND DOES NOT GRANT ENTRY INTO INDIA.\nOFFICIAL VISAS MUST BE SUBMITTED DIRECTLY AT THE OFFICIAL GOVERNMENT PORTAL: HTTPS://INDIANVISAONLINE.GOV.IN", disclFont);
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

            table.addCell(new Paragraph("Preparation Status:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getPreparationStatus() != null ? trip.getPreparationStatus() : "PREPARATION_IN_PROGRESS", FontFactory.getFont(FontFactory.COURIER_BOLD, 10)));

            table.addCell(new Paragraph("Applicant Full Name:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getApplicantName().toUpperCase(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Nationality:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getNationality(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Target Visa Category:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getCategoryName(), FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Intended Port of Entry:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph(trip.getPortOfArrival() != null ? trip.getPortOfArrival() : "Designated Authorized Port", FontFactory.getFont(FontFactory.HELVETICA, 10)));

            table.addCell(new Paragraph("Official Government Portal:", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            table.addCell(new Paragraph("https://indianvisaonline.gov.in/evisa/Registration", FontFactory.getFont(FontFactory.COURIER_BOLD, 10)));

            document.add(table);

            // Instructions
            Paragraph instr = new Paragraph("\nTRAVELER PREPARATION CHECKLIST & OFFICIAL INSTRUCTIONS:\n" +
                    "1. Complete your official submission and payment directly on https://indianvisaonline.gov.in.\n" +
                    "2. Ensure your passport has at least 6 months validity remaining and at least 2 blank pages.\n" +
                    "3. Carry a recent 2x2 inch front-facing white background photograph per official specifications.\n" +
                    "4. This dossier summarizes your travel preparation data for easy entry into the official portal.",
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
