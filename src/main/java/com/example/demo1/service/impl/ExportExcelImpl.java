package com.example.demo1.service.impl;

import com.example.demo1.dto.DemoDto;
import com.example.demo1.service.ExportExcel;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.*;
import java.util.List;



@Service
public class ExportExcelImpl implements ExportExcel {
    @Override
    public  ByteArrayInputStream writeExcel(List<DemoDto> demoDtos) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            createSheet1(demoDtos, workbook);
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Fail to import data to Excel file: " + e.getMessage());
        }
    }

    @Override
    public void writeExcel2(List<DemoDto> demoDtos) {
        //
        Workbook workbook = new XSSFWorkbook();

        //
        createSheet1(demoDtos, workbook);

        String folderPath = "C:\\Excel";

        String fileName = createFileName();
        //
        String filePath = folderPath + File.separator + fileName;

        //
        try (FileOutputStream fileOutputStream = new FileOutputStream(filePath)) {
            workbook.write(fileOutputStream);
            System.out.println("The file has been created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String createFileName() {
        String fileName = "FAILED_USERS_PROCESSOR_";
        String directoryPath = "C:\\Excel";
        File directory = new File(directoryPath);
        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            fileName = fileName + (files.length + 1);
        }
        return fileName  + ".xlsx";
    }

    private void createSheet1(List<DemoDto> demoDtos, Workbook workbook) {
        Sheet sheet1 = workbook.createSheet("Demo");
        sheet1.setDefaultColumnWidth(25);
        Font font = sheet1.getWorkbook().createFont();
        font.setFontName("Times New Roman");
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        // set title

        CellStyle cellStyle = getCellStyleHeader(sheet1, font);
        Row headerRow = sheet1.createRow(0);
        Cell cell = headerRow.createCell(0);
        cell.setCellValue("EMAIL");
        cell.setCellStyle(cellStyle);
        cell = headerRow.createCell(1);
        cell.setCellValue("USER ID");
        cell.setCellStyle(cellStyle);
        cell = headerRow.createCell(2);
        cell.setCellValue("FIRST NAME");
        cell.setCellStyle(cellStyle);
        cell = headerRow.createCell(3);
        cell.setCellValue("LAST NAME");
        cell.setCellStyle(cellStyle);
        cell = headerRow.createCell(4);
        cell.setCellValue("TERRITORY");
        cell.setCellStyle(cellStyle);
        cell = headerRow.createCell(5);
        cell.setCellValue("BRAND");
        cell.setCellStyle(cellStyle);
        // set data
        for (int i = 0; i < demoDtos.size(); i++) {
            Row row = sheet1.createRow(i + 1);
            Cell cell1 = row.createCell(0);
            cell1.setCellValue(demoDtos.get(i).getEmail() == null ? "" : demoDtos.get(i).getEmail());
            cell1.setCellStyle(getCellStyleRow(sheet1));
            cell1 = row.createCell(1);
            cell1.setCellValue(demoDtos.get(i).getUserId()== null ? "" : demoDtos.get(i).getUserId());
            cell1.setCellStyle(getCellStyleRow(sheet1));
            cell1 = row.createCell(2);
            cell1.setCellValue(demoDtos.get(i).getGivenName()== null ? "" : demoDtos.get(i).getGivenName());
            cell1.setCellStyle(getCellStyleRow(sheet1));
            cell1 = row.createCell(3);
            cell1.setCellValue(demoDtos.get(i).getFamilyName()== null ? "" : demoDtos.get(i).getFamilyName());
            cell1.setCellStyle(getCellStyleRow(sheet1));
            cell1 = row.createCell(4);
            cell1.setCellValue(demoDtos.get(i).getUserMetadataDto()== null || demoDtos.get(i).getUserMetadataDto().getLang()==null ? "" : demoDtos.get(i).getUserMetadataDto().getLang().substring(3,5));
            cell1.setCellStyle(getCellStyleRow(sheet1));
            cell1 = row.createCell(5);
            cell1.setCellValue(demoDtos.get(i).getAppMetadataDto() == null || demoDtos.get(i).getAppMetadataDto().getCode()== null  ? "" : demoDtos.get(i).getAppMetadataDto().getCode());
            cell1.setCellStyle(getCellStyleRow(sheet1));
        }
    }

    private static CellStyle getCellStyleHeader(Sheet sheet1, Font font) {

        CellStyle cellStyle = sheet1.getWorkbook().createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);
        return cellStyle;
    }

    private static CellStyle getCellStyleRow(Sheet sheet1) {
        Font font = sheet1.getWorkbook().createFont();
        font.setFontName("Times New Roman");
        font.setFontHeightInPoints((short) 12);
        CellStyle cellStyle = sheet1.getWorkbook().createCellStyle();
        cellStyle.setFont(font);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setWrapText(true);
        return cellStyle;
    }
}
