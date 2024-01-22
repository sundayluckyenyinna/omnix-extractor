package com.accionmfb.omnix.extractor.modules.extractor;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ExcelUtil {

    public static String generateExcelWorkbook(List<List<Map<String, Object>>> pages, String category) throws IOException {
        String workbookName = "WORKBOOK_".concat(category).concat(".xlsx");
        log.info("Generating workbook with name: {}", workbookName);

        if(!pages.isEmpty() && !pages.get(0).isEmpty()) {

            // Get the list of the headers
            List<String> headerNames = new ArrayList<>(pages.get(0).get(0).keySet());

            // Create the workbook
            Workbook workbook = new XSSFWorkbook();

            // Fill the sheet with the header rows and the other rows
            int num = 1;
            for (List<Map<String, Object>> page : pages) {
                createWorkbookSheet(workbook,num,  page, headerNames, category);
                num++;
            }

            File currentDir = new File(".");
            String absolutePath = currentDir.getAbsolutePath();
            String fullFileName = absolutePath.concat(File.separator).concat(workbookName);

            FileOutputStream fileOutputStream = new FileOutputStream(fullFileName);
            workbook.write(fileOutputStream);
            workbook.close();

            log.info("Excel Workbook: {} generated successfully...", workbookName);
        }
        return "Success";
    }

    public static void createWorkbookSheet(Workbook workbook, int sheetNum, List<Map<String, Object>> recordMaps, List<String> headerNames, String category){
        String sheetName = category.concat("_" + sheetNum);
        Sheet sheet = workbook.createSheet(sheetName);

        // Create the headerRows
        Row headerRow = sheet.createRow(0);
        for(int i = 0; i < headerNames.size(); i++)
        {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headerNames.get(i));
        }

        for(int i = 0; i < recordMaps.size(); i++)
        {
            // Create the row that corresponds to the map record
            Row currentRow = sheet.createRow(i + 1);
            Map<String, Object> currentRecord = recordMaps.get(i);

            int index = 0;
            for(Map.Entry<String, Object> entry : currentRecord.entrySet())
            {
                Cell column = currentRow.createCell(index);
                if(index == 4){
                    column.setCellValue(getBigDecimalValue(String.valueOf(entry.getValue())));
                }else{
                    column.setCellValue(String.valueOf(returnOrDefault(entry.getValue(), "")));
                }
                index++;
            }
        }
    }


    private static double getBigDecimalValue(String value){
        try {
            return new BigDecimal(value).doubleValue();
        }catch (Exception exception){
            return BigDecimal.ZERO.doubleValue();
        }
    }

    private static Object returnOrDefault(Object value, Object defaultValue){
        return Objects.isNull(value) ? defaultValue : value;
    }
}
