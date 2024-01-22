package com.accionmfb.omnix.extractor.modules.extractor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = false)
public class DataExtractionJob implements Runnable{

    private final String line;
    private final JdbcTemplate jdbcTemplate;
    private final static int PAGE_SIZE = 10000;
    private final static String WORKBOOK_NAME_PREFIX = "WORKBOOK_NAME_PREFIX_";

    public DataExtractionJob(String line, JdbcTemplate jdbcTemplate) {
        this.line = line;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run() {

        // Create the workbook first physically and write in chunks to it.
        String workbookName = WORKBOOK_NAME_PREFIX.concat(this.line).concat(".xlsx");
        File currentDir = new File(".");
        String currentDirAbsPath = currentDir.getAbsolutePath().concat(File.separator).concat("workbooks");
        String workbookAbsPath = currentDirAbsPath.concat(File.separator).concat(workbookName);


        try
        {
            Workbook workbook = new XSSFWorkbook();
            log.info("Workbook created successfully with absolute path: {}", workbookAbsPath);

            int currentPageNumber = 1;
            while(true) {
                int offset = (currentPageNumber - 1) * PAGE_SIZE;
                String sql = "SELECT * FROM GL_EXTRACT2 WHERE LINE = %s AND booking_date >= '2023-01-01' AND booking_date <= '2023-12-31' ORDER BY id ASC OFFSET %s ROWS FETCH NEXT %s ROWS  ONLY";
                sql = String.format(sql, line, offset, PAGE_SIZE);
                List<Map<String, Object>> next1000Records = jdbcTemplate.queryForList(sql);
                if(!next1000Records.isEmpty()) {
                    List<String> headerNames = new ArrayList<>(next1000Records.get(0).keySet());
                    log.info("Creating new sheet for workbook: {}", workbookName);
                    ExcelUtil.createWorkbookSheet(workbook, currentPageNumber, next1000Records, headerNames, line);
                    log.info("New sheet for workbook: {} created successfully", workbookName);
                    currentPageNumber++;
                }else{
                    log.info("No next record found, breaking from the writing loop and closing the current workbook with name: {}", workbookName);
                    break;
                }
            }

            FileOutputStream fileOutputStream = new FileOutputStream(workbookAbsPath);
            log.info("Creating workbook with name: {}", workbookName);
            workbook.write(fileOutputStream);
            workbook.close();
            fileOutputStream.close();
            log.info("Workbook with name: {} done, dusted and closed.", workbookName);
        }
        catch (Exception exception){
            log.error("Exception occurred while creating workbook with name: {} and exception message is: {}", workbookName, exception.getMessage());
        }

    }
}
