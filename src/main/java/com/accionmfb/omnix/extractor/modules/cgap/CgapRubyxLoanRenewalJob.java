package com.accionmfb.omnix.extractor.modules.cgap;


import com.accionmfb.omnix.extractor.modules.cgap.models.*;
import com.accionmfb.omnix.extractor.modules.cgap.payload.AccountDetailsResponseDto;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
//@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class CgapRubyxLoanRenewalJob {

    private final File baseFolder = new File("C:\\Omnix\\cgap");
    private final T24Service t24Service;
    private final CgapRepository cgapRepository;
    private final Environment env;



    @Scheduled(fixedDelay = 5000, initialDelay = 2000)
    public void processCgapRecords(){

        List<File> excelFiles = getExcelWorkbookNotYetProcessed();

        for(File excelFile : excelFiles)
        {
            log.info("Current file: {}", excelFile.getAbsoluteFile());

            try
            {
                FileInputStream inputStream = new FileInputStream(excelFile);
                Workbook workbook = new XSSFWorkbook(inputStream);
                List<Sheet> sheets = getAllSheetsInWorkbook(workbook);

                // Get all CgapRecord Object from the excel workbook
                List<CgapExcelRecord> cgapExcelRecords = new ArrayList<>();
                for(Sheet sheet : sheets) {
                    for (Row row : sheet) {
                        Map<String, Object> rowMap;
                        if (row.getRowNum() > 0) {
                            rowMap = getExcelRecordMap(row, false);
                            CgapExcelRecord cgapExcelRecord = miniMapper().readValue(miniMapper().writeValueAsBytes(rowMap), CgapExcelRecord.class);
                            cgapExcelRecords.add(cgapExcelRecord);
                        }
                    }
                }

                // Filter out the records for all empty fiels
                cgapExcelRecords = cgapExcelRecords.stream().filter(cgapExcelRecord -> !isAllFieldsEmpty(cgapExcelRecord)).collect(Collectors.toList());

                System.out.println(miniMapper().writerWithDefaultPrettyPrinter().writeValueAsString(cgapExcelRecords));

                // Hold a counter for all the object processed
                int count = 0;

                // Sync customer details in Omnix from core
                String mobileNumber;
                for(CgapExcelRecord cgapExcelRecord : cgapExcelRecords)
                {
                    String customerCif = cgapExcelRecord.getCustomerNumber();
                    CustomerDetailsFromT24 detailsFromT24 = t24Service.customerDetailsFromT24(customerCif);
                    if(Objects.nonNull(detailsFromT24) && Objects.nonNull(detailsFromT24.getMobileNumber())){
                        mobileNumber = detailsFromT24.getMobileNumber();
                    }else{
                        mobileNumber = cgapExcelRecord.getMobileNumber();
                    }

                    Customer customer = cgapRepository.findCustomerByMobileNumber(mobileNumber);
                    if(Objects.isNull(customer)) {
                        System.out.printf("No customer record for CIF: %s in Omnix. Syncing customer details and account details from Core to Omnix", customerCif);
                        t24Service.syncCustomerRecored(mobileNumber, "USSD");
                    }
                    else{
                        log.info("Customer details found in Omnix. Syncing accounts from core.");
                        String userCredentials = env.getProperty("omnix.channel.user.ussd");
//                        List<AccountDetailsResponseDto> accountDetailsResponseDtos = t24Service.fetchAccountDetailsList(customerCif, cgapExcelRecord.getBranch(), userCredentials);
                        List<AccountDetailsResponseDto> accountDetailsResponseDtos = t24Service.fetchAccountDetailsList(customerCif, "NG0010001", userCredentials);


                        System.out.printf("Accounts from core for customer with CIF: %s   = %s", customerCif, accountDetailsResponseDtos.stream().map(a -> {
                            Map<String, String> map = new HashMap<>();
                            map.put("accountNumber", a.getAccountNumber());
                            map.put("branch", a.getBranchCode());
                            return map;
                        }).collect(Collectors.toList()));

                        for(AccountDetailsResponseDto detailsResponseDto : accountDetailsResponseDtos){
                            String coreAccountNumber = detailsResponseDto.getAccountNumber();
                            String omnixableAccountNumber = "0" + detailsResponseDto.getAccountNumber();

                            Account omnixAccountRecord = cgapRepository.getAccountHavingAccountNumberIn(List.of(coreAccountNumber, omnixableAccountNumber));
                            if(Objects.nonNull(omnixAccountRecord)){
                                omnixAccountRecord.setCategory(detailsResponseDto.getCategoryCode());
                                omnixAccountRecord.setAccountNumber(coreAccountNumber);
                                omnixAccountRecord.setOldAccountNumber(detailsResponseDto.getT24AccountNumber());
                                omnixAccountRecord.setBranch(cgapRepository.getBranchUsingBranchCode(detailsResponseDto.getBranchCode()));
                                cgapRepository.updateAccountRecord(omnixAccountRecord);
                                log.info("Account for customer with CIF: {} updated successfully!", customerCif);
                            }else{
                                Account oAccount = new Account();
                                oAccount.setAccountBalance(BigDecimal.ZERO);
                                oAccount.setAccountNumber(detailsResponseDto.getAccountNumber());
                                oAccount.setAppUser(customer.getAppUser());
                                oAccount.setBranch(cgapRepository.getBranchUsingBranchCode(detailsResponseDto.getBranchCode()));
                                oAccount.setCategory(detailsResponseDto.getCategoryCode());
                                oAccount.setCreatedAt(LocalDateTime.now());
                                oAccount.setCustomer(customer);
                                oAccount.setOldAccountNumber(detailsResponseDto.getT24AccountNumber());
                                oAccount.setPrimaryAccount(true);
                                oAccount.setProduct(cgapRepository.getProductByCategoryCode(detailsResponseDto.getCategoryCode()));
                                oAccount.setRequestId(String.valueOf(System.currentTimeMillis()));
                                oAccount.setStatus("SUCCESS");
                                oAccount = cgapRepository.createAccount(oAccount);
                                if(oAccount.getId() > 0){
                                    log.info("Account created for customer with CIF : {} created successfully", customerCif);
                                }
                            }
                        }
                    }


                    // Do validation for ownership of account
                    List<String> accountNumbersNowInOmnix = cgapRepository.getAllCustomerAccountNumbers(cgapRepository.findCustomerByCustomerNumber(customerCif));
                    if(cgapExcelRecord.getBrightaCommitmentAccount().length() == 9){
                        cgapExcelRecord.setBrightaCommitmentAccount(getZeroForFrontPadding(cgapExcelRecord.getBrightaCommitmentAccount()) + cgapExcelRecord.getBrightaCommitmentAccount());
                    }else if (cgapExcelRecord.getDisbursementAccount().length() == 9){
                        cgapExcelRecord.setDisbursementAccount(getZeroForFrontPadding(cgapExcelRecord.getDisbursementAccount()) + cgapExcelRecord.getDisbursementAccount());
                    }

                    boolean isBCPresent = accountNumbersNowInOmnix.contains(cgapExcelRecord.getBrightaCommitmentAccount());
                    boolean isDisAccountPresent = accountNumbersNowInOmnix.contains(cgapExcelRecord.getDisbursementAccount());
                    log.info("Accounts from Omnix for customer with CIF: {} = {}", customerCif, accountNumbersNowInOmnix);

                    if(isBCPresent && isDisAccountPresent)
                    {
                        log.info("Customer passed validation of ownership of brighta commitment account.");
                        String customerNumber = cgapExcelRecord.getCustomerNumber();
                        String productCode = cgapExcelRecord.getProductCode();
                        Branch disbursementAccountBranch = cgapRepository.getAccountUsingAccountNumber(cgapExcelRecord.getDisbursementAccount()).getBranch();
                        Branch bcAccountBranch = cgapRepository.getAccountUsingAccountNumber(cgapExcelRecord.getBrightaCommitmentAccount()).getBranch();

                        String branchCode;
                        if(Objects.nonNull(disbursementAccountBranch) && Objects.nonNull(disbursementAccountBranch.getBranchCode())){
                            branchCode = disbursementAccountBranch.getBranchCode();
                        }
                        else if(Objects.nonNull(bcAccountBranch) && Objects.nonNull(bcAccountBranch.getBranchCode())){
                            branchCode = bcAccountBranch.getBranchCode();
                        }
                        else{
                            branchCode = null;
                        }

                        RubyxLoanRenewal existingLoan = cgapRepository.getRubyxLoanByCustomerNumberAndProductCode(customerNumber, productCode);
                        if(Objects.isNull(existingLoan)) {
                            RubyxLoanRenewal loanRenewal = RubyxLoanRenewal.builder()
                                    .currentLoanCycle(4)
                                    .renewalAmount(cgapExcelRecord.getRequestedAmount())
                                    .renewalRating(cgapExcelRecord.getRenewalRating())
                                    .loanAmount(cgapExcelRecord.getApprovedAmount())
                                    .totalLoanBalance(BigDecimal.ZERO)
                                    .renewalScore(cgapExcelRecord.getRenewalRating())
                                    .brightaCommitmentAccount(cgapExcelRecord.getBrightaCommitmentAccount())
                                    .createdAt(LocalDateTime.now())
                                    .creditBureauNoHit(false)
                                    .contactCenterEmailSent(false)
                                    .noOfTotalLoans(0)
                                    .noOfPerformingLoans(0)
                                    .creditBureauSearchDone(true)
                                    .customerApply(true)
                                    .customerNumber(customerCif)
                                    .customerSignOfferLetter(false)
                                    .customerSmsSent(false)
                                    .customer(cgapRepository.findCustomerByCustomerNumber(customerCif))
                                    .disbursementAccount(cgapExcelRecord.getDisbursementAccount())
                                    .guarantorIdVerified(false)
                                    .mobileNumber(mobileNumber)
                                    .interestRate(cgapExcelRecord.getInterestRate())
                                    .productCode(cgapExcelRecord.getProductCode())
                                    .tenor(cgapExcelRecord.getTenor())
                                    .accountOfficerCode(cgapExcelRecord.getAccountOfficerCode())
                                    .status("SUCCESS")
                                    .branch(branchCode)
                                    .requestId("ACCION-NG_PO1_3_OID_".concat(String.valueOf(System.currentTimeMillis())))
                                    .bvn(cgapExcelRecord.getBvn())
                                    .guarantorSignOfferLetter(false)
                                    .totalPerformingBalance(BigDecimal.ZERO)
                                    .build();
                            RubyxLoanRenewal createdLoanRenewal = cgapRepository.createRubyxLoanRenewalRecord(loanRenewal);
                            if (createdLoanRenewal.getId() > 0) {
                               log.info("Rubyx loan record created successfully!");
                            }
                        }else{
                            log.info("Rubyx Loan record already exists by virtue of customer CIF and product code");
                            log.info("System will update the branch code");
                            existingLoan.setBranch(branchCode);
                            cgapRepository.updateRubyxLoanRenewal(existingLoan);
                        }
                    }
                    else{
                        // Keep the failed ones in the table.
                        FailedExcelCgapRecord failedExcelCgapRecord = miniMapper().readValue(miniMapper().writeValueAsBytes(cgapExcelRecord), FailedExcelCgapRecord.class);
                        if(!isBCPresent && !isDisAccountPresent){
                            failedExcelCgapRecord.setFailureReason(String.format("Both Brighta Commitment account and Disbursement does not associate with customer with CIF: %s", customerCif));
                        }
                        else if(!isBCPresent & isDisAccountPresent){
                            failedExcelCgapRecord.setFailureReason(String.format("The Brighta Commitment account is not associated with the customer with CIF: %s", customerCif));
                        }
                        else {
                            failedExcelCgapRecord.setFailureReason(String.format("The Disbursement account is not associated with the customer with CIF: %s", customerCif));
                        }
                        cgapRepository.saveFailedExcelRecords(failedExcelCgapRecord);
                    }

                    count += 1;
                }

                log.info("Count of excel file processed: {}", count);
                log.info("Size of CgapLoan object: {}", cgapExcelRecords.size());
                if(count == cgapExcelRecords.size()){
                    // Create and lock the record
                    log.info("Rubyx loan record created successfully. System will now create the excel workbook record to prevent another reading");
                    ExcelWorkbookRecord workbookRecord = new ExcelWorkbookRecord();
                    workbookRecord.setWorkbookName(excelFile.getName());
                    workbookRecord.setReadDateTime(LocalDateTime.now());
                    workbookRecord.setStatus("COMPLETED");
                    cgapRepository.saveExcelWorkbookRecord(workbookRecord);
                    log.info("Excel workbook locked successfully");
                }

            }
            catch (Exception exception)
            {
                log.info("Exception while trying to process excel sheet records. Exception message is: {}", exception.getMessage());
            }
        }
    }


    private Map<String, Object> getExcelRecordMap(Row row, boolean firstTime){
        Map<String, Object> rowMap = new HashMap<>();
        rowMap.put("sn", resolveCellType(row, 0));
        rowMap.put("account_officer_code", resolveCellType(row, 1));
        rowMap.put("branch", resolveCellType(row, 2));
        rowMap.put("bvn", resolveCellType(row, 3));
        rowMap.put("customer_number", resolveCellType(row, 4));
        rowMap.put("disbursement_account", resolveCellType(row, 5));
        rowMap.put("interest_rate", resolveCellType(row, 6));
        rowMap.put("mobile_number", resolveCellType(row, 7));
        rowMap.put("product_code", resolveCellType(row, 8));
        rowMap.put("requested_amount", resolveCellType(row, 9).replaceAll(",", "").trim());
        rowMap.put("renewal_rating", resolveCellType(row, 10));
        rowMap.put("tenor", resolveCellType(row, 11));
        rowMap.put("brighta_commitment_account", resolveCellType(row, 12));
        rowMap.put("approved_amount", resolveCellType(row, 13).replaceAll(",", "").trim());
        if(firstTime){
            rowMap.put("status", "PENDING");
        }
        return rowMap;
    }


    private String resolveCellType(Row row, int cellIndex){
        Cell cell = row.getCell(cellIndex);
        String cellValue = Strings.EMPTY;
        if(Objects.nonNull(cell)){
            CellType cellType = cell.getCellType();
            switch (cellType){
                case NUMERIC: {
                    if(cellIndex == 3){
                        cellValue = new BigDecimal(String.valueOf(cell.getNumericCellValue())).toString();
                    }
                    else if(cellIndex == 6) {
                        cellValue = String.valueOf(cell.getNumericCellValue());
                    }else{
                        cellValue = String.valueOf((int)cell.getNumericCellValue());
                    }
                    break;
                }
                case _NONE: cellValue = Strings.EMPTY; break;
                case BLANK: cellValue = Strings.EMPTY; break;
                case BOOLEAN: cellValue = String.valueOf(cell.getBooleanCellValue()); break;
                case STRING: cellValue = String.valueOf(cell.getStringCellValue()); break;
                default: cellValue = Strings.EMPTY;
            }
        }
        return cellValue;
    }

    private File getSingleExcelFileNotYetProcessed(){
        return getExcelWorkbookNotYetProcessed()
                .stream()
                .filter(File::exists)
                .filter(file -> {
                    String fileName = file.getName();
                    ExcelWorkbookRecord record = cgapRepository.getExcelWorkbookRecordByName(fileName);
                    return Objects.isNull(record);
                })
                .findFirst()
                .orElse(null);
    }

    public List<File> getExcelWorkbookNotYetProcessed(){
        List<File> workbooks = new ArrayList<>();
        for (File excelFile : getExcelWorkbooksInFolder()){
            ExcelWorkbookRecord record = cgapRepository.getExcelWorkbookRecordByName(excelFile.getName());
            if(Objects.isNull(record)){
                workbooks.add(excelFile);
            }
        }
        return workbooks;
    }

    public List<File> getExcelWorkbooksInFolder(){
        File[] files = baseFolder.listFiles();
        if(Objects.nonNull(files)){
            return Arrays.stream(files)
                    .filter(f -> f.getName().endsWith(".xlsx"))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private ObjectMapper miniMapper(){
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static <T> T valueOrDefault(T value, T defaultValue) {
        try {
            return Objects.isNull(value) ? defaultValue : value;
        }catch (Exception exception) {
            return defaultValue;
        }
    }

    public List<Sheet> getAllSheetsInWorkbook(Workbook workbook){
        int noOfSheets = workbook.getNumberOfSheets();
        List<Sheet> sheets = new ArrayList<>();
        for(int i = 0; i < noOfSheets; i++){
            sheets.add(workbook.getSheetAt(i));
        }
        return sheets;
    }

    private String getZeroForFrontPadding(String excelAccountNumber){
        excelAccountNumber = excelAccountNumber.trim();
        int nums = 10 - excelAccountNumber.length();
        return "0".repeat(Math.max(0, nums));
    }

    private boolean isAllFieldsEmpty(Object object){
        return Arrays.stream(object.getClass().getDeclaredFields()).allMatch(field -> {
            field.setAccessible(true);
            try {
                return Objects.isNull(field.get(object)) || String.valueOf(field.get(object)).trim().isEmpty();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
