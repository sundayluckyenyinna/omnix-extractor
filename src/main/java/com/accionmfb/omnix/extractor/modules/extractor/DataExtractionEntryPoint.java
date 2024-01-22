package com.accionmfb.omnix.extractor.modules.extractor;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Data
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExtractionEntryPoint {

    private final static int maxConcurrentThreads = 10;
    private final JdbcTemplate jdbcTemplate;
    private final static String WORKBOOK_NAME_PREFIX = "WORKBOOK_NAME_PREFIX_";

    @EventListener(value = ApplicationStartedEvent.class)
    public String startDataExtractionWithMultithreading(){
        List<String> lines = getLinesToProcessV2();
        log.info("Lines to process: {}", lines);

        try
        {
            int batchSize = maxConcurrentThreads;
            if(batchSize > lines.size()){
                batchSize = lines.size();
            }
            for (int i = 0; i < lines.size(); i+= batchSize) {
                List<String> batch = lines.subList(i, Math.min(i + batchSize, lines.size()));

                log.info("Current Batch: {}", batch);

                // Create the futures for the batches
                CompletableFuture<Void>[] futures = new CompletableFuture[batchSize];
                for(int j = 0; j < futures.length; j++){
                    futures[j] = CompletableFuture.runAsync(new DataExtractionJob(batch.get(j), jdbcTemplate));
                }

                // Run the futures concurrently for current batch and wait for the batch to complete to prevent multiple threads spin up and thus memory leaks.
                log.info("Processing current batch with group ID: {}", i + 1);
                CompletableFuture<Void> currentBatchFuture = CompletableFuture.allOf(futures);
                try{
                    currentBatchFuture.join();
                }catch (Exception exception){
                    log.info("Exception occurred while processing futures for current batch with group ID: {}", i + 1);
                    exception.printStackTrace();
                }

                log.info("Batch with group ID: {} processed. Moving to the next batch", i + 1);
            }
        }
        catch (Exception exception)
        {
            log.error("Exception occurred in main class while processing batch workbook. Exception message is: {}", exception.getMessage());
        }

        return "Success";
    }

    private List<String> getLinesToProcessV2(){
        try {
            List<String> allLinesInTBFile = listAllLineValuesInTBExcel();
            List<String> finalLines = allLinesInTBFile.stream()
                    .filter(line -> {
                        File currentDir = new File(".");
                        String currentDirAbsPath = currentDir.getAbsolutePath().concat(File.separator).concat("workbooks");
                        String workbookName = WORKBOOK_NAME_PREFIX.concat(line).concat(".xlsx");
                        String workbookAbsPath = currentDirAbsPath.concat(File.separator).concat(workbookName);
                        return !new File(workbookAbsPath).exists();
                    })
                    .collect(Collectors.toList());
            log.info("Final lines: {}", finalLines);
            log.info("Final lines size: {}", finalLines.size());
            return finalLines;
        }catch (Exception e){
            log.error("Exception occurred while reading lines to process. Exception message is: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static List<String> listAllLineValuesInTBExcel(){
        try {
            InputStream fileIs = new FileInputStream(getCurrentExcelFileForLinesInDir());
            Workbook lineWorkbook = new XSSFWorkbook(fileIs);
            List<String> lines = new ArrayList<>();
            Sheet firstSheet = lineWorkbook.getSheetAt(0);
            for(Row row : firstSheet){
                for(Cell cell : row){
                    int cellColumnIndex = cell.getColumnIndex();
                    CellType cellType = cell.getCellType();
                    boolean isValidCellType = cellType == CellType.NUMERIC || cellType == CellType.STRING;
                    if(cellColumnIndex == 0 && isValidCellType){
                        if(cellType == CellType.NUMERIC) {
                            double doubleCellValue = cell.getNumericCellValue();
                            int approxCellValue = (int) doubleCellValue;
                            lines.add(String.valueOf(approxCellValue));
                        }
                        else{
                            lines.add(String.valueOf(cell.getStringCellValue()));
                        }
                    }
                }
            }
            return lines;
        }catch (Exception e){
            log.error("Exception occurred while reading lines to process. Exception message is: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private static File getCurrentExcelFileForLinesInDir() throws FileNotFoundException {
        File currentDir = new File(".");
        return Arrays.stream(Objects.requireNonNull(currentDir.listFiles()))
                .filter(file -> file.getAbsolutePath().endsWith(".xlsx"))
                .findFirst()
                .orElseThrow(() -> new FileNotFoundException("No excel file found for lines!!!"));
    }
}
