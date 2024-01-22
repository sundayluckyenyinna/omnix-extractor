package com.accionmfb.omnix.extractor.modules.cgap;

import com.accionmfb.omnix.extractor.modules.cgap.models.*;
import com.accionmfb.omnix.extractor.modules.cgap.payload.AccountDetailsResponseDto;
import com.accionmfb.omnix.extractor.modules.cgap.payload.BVNRequestPayload;
import com.accionmfb.omnix.extractor.modules.cgap.payload.BVNResponsePayload;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
//@Service
@RequiredArgsConstructor
public class ProcessLoanTemplate {

    private final T24Service t24Service;
    private final CgapRepository cgapRepository;
    private final RestTemplate restTemplate;

    @Value("${omnix.tier1.trans}")
    private BigDecimal tier1DepositLimit;

    @Value("${omnix.tier1.mobileWithdrawal}")
    private BigDecimal tier1WithdrawalLimit;

    @Value("${omnix.tier1.balance}")
    private BigDecimal tier1BalanceLimit;

    @Value("${omnix.tier1.mobileDaily}")
    private BigDecimal tier1DailyBalance;

    private final Environment env;

    File file = new File("C:\\Omnix\\cgap\\BATCH.xlsx");
    File baseFolder = new File("C:\\Omnix\\cgap");

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJERUZBVUxUIiwicm9sZXMiOiJbQUNDT1VOVF9CQUxBTkNFUywgQUNDT1VOVF9TVEFURU1FTlQsIEFDQ09VTlRfREVUQUlMUywgQUNDT1VOVF9PUEVOSU5HLCBBQ0NPVU5UX0dMT0JBTCwgV0FMTEVUX0FDQ09VTlRfT1BFTklORywgQUlSVElNRV9DQUxMQkFDSywgQUlSVElNRV9TRUxGLCBBSVJUSU1FX09USEVSUywgREFUQV9TRUxGLCBEQVRBX09USEVSUywgQUlSVElNRV9EQVRBX0RFVEFJTFMsIEJWTl9WQUxJREFUSU9OLCBDQUJMRV9UVl9TVUJTQ1JJUFRJT04sIENBQkxFX1RWX0RFVEFJTFMsIENBQkxFX1RWX0JJTExFUiwgQ1JFRElUX0JVUkVBVV9WQUxJREFUSU9OLCBJTkRJVklEVUFMX1dJVEhfQlZOLCBJTkRJVklEVUFMX1dJVEhPVVRfQlZOLCBDT1JQT1JBVEVfQ1VTVE9NRVIsIENVU1RPTUVSX0JPQVJESU5HLCBDVVNUT01FUl9ERVRBSUxTLCBVUERBVEVfQ1VTVE9NRVJfVElFUiwgVVBEQVRFX0NVU1RPTUVSX1NUQVRVUywgVVBEQVRFX0NVU1RPTUVSX1NUQVRFX1JFU0lERU5DRSwgVVBEQVRFX0NVU1RPTUVSX0NJVFlfUkVTSURFTkNFLCBVUERBVEVfQ1VTVE9NRVJfUkVTSURFTlRJQUxfQUREUkVTUywgVVBEQVRFX0NVU1RPTUVSX1BBU1NXT1JELCBVUERBVEVfQ1VTVE9NRVJfU0VDVVJJVFlfUVVFU1RJT04sIFVQREFURV9DVVNUT01FUl9QSU4sIFVQREFURV9DVVNUT01FUl9FTUFJTCwgVVBEQVRFX0NVU1RPTUVSX01BUklUQUxfU1RBVFVTLCBVUERBVEVfQ1VTVE9NRVJfTU9CSUxFX05VTUJFUiwgQVVUSF9DVVNUT01FUl9VU0lOR19QSU4sIEFVVEhfQ1VTVE9NRVJfVVNJTkdfUEFTU1dPUkQsIEVMRUNUUklDSVRZX0JJTExfUEFZTUVOVCwgRUxFQ1RSSUNJVFlfQklMTF9ERVRBSUxTLCBFTEVDVFJJQ0lUWV9CSUxMRVJTLCBMT0NBTF9GVU5EU19UUkFOU0ZFUiwgTE9DQUxfRlVORFNfVFJBTlNGRVJfV0lUSF9DSEFSR0UsIFJFVkVSU0VfTE9DQUxfRlVORFNfVFJBTlNGRVIsIE9QQVksIFBBWV9BVFRJVFVERSwgTklQX05BTUVfRU5RVUlSWSwgSU5URVJfQkFOS19GVU5EU19UUkFOU0ZFUiwgQUNDT1VOVF9UT19XQUxMRVRfRlVORFNfVFJBTlNGRVIsIElERU5USVRZX1ZBTElEQVRJT04sIEJPT0tfQVJUSVNBTl9MT0FOLCBQRU5ESU5HX0FSVElTQU5fTE9BTiwgRElTQlVSU0VfQVJUSVNBTl9MT0FOLCBBVVRIX0FSVElTQU5fTE9BTiwgUkVORVdfQVJUSVNBTl9MT0FOLCBCT09LX0RJR0lUQUxfTE9BTiwgRElTQlVSU0VfRElHSVRBTF9MT0FOLCBBQ0NFUFRfRElHSVRBTF9MT0FOLCBSRU5FV19ESUdJVEFMX0xPQU4sIExPQU5fU0VUVVAsIExPQU5fVFlQRV9MSVNUSU5HLCBTTVNfTk9USUZJQ0FUSU9OLCBUUkFOU0FDVElPTl9FTUFJTF9BTEVSVCwgTE9BTl9PRkZFUl9FTUFJTF9BTEVSVCwgTE9BTl9HVUFSQU5UT1JfRU1BSUxfQUxFUlQsIFdBTExFVF9CQUxBTkNFLCBXQUxMRVRfREVUQUlMUywgV0FMTEVUX0NVU1RPTUVSLCBDUkVBVEVfV0FMTEVULCBDTE9TRV9XQUxMRVQsIFdBTExFVF9BSVJUSU1FX1NFTEYsIFdBTExFVF9BSVJUSU1FX09USEVSUywgV0FMTEVUX0RBVEFfU0VMRiwgV0FMTEVUX0RBVEFfT1RIRVJTLCBXQUxMRVRfQ0FCTEVfVFZfU1VCU0NSSVBUSU9OLCBXQUxMRVRfRUxFQ1RSSUNJVFlfQklMTCwgV0FMTEVUX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX1RPX0FDQ09VTlRfRlVORFNfVFJBTlNGRVIsIFdBTExFVF9UT19JTlRFUl9CQU5LX1RSQU5TRkVSLCBJTlRFUl9CQU5LX1RPX1dBTExFVF9GVU5EU19UUkFOU0ZFUiwgV0FMTEVUX0lOVEVSX0JBTktfTkFNRV9FTlFVSVJZLCBDT05WRVJUX1dBTExFVF9UT19BQ0NPVU5ULCBEQVRBX1BMQU4sIEFERF9QT1NUSU5HX1JFU1RSSUNUSU9OLCBDQVJEX1JFUVVFU1QsIEZVTkRTX1RSQU5TRkVSX1NUQVRVUywgTklCU1NfUVJfUEFZTUVOVCwgQVVUSF9TRUNVUklUWV9RVUVTVElPTiwgVEVMTEVSLCBVUERBVEVfQ1VTVE9NRVJfQlZOLCBGVU5EU19UUkFOU0ZFUl9ERUxFVEUsIEFVVEhfQ1VTVE9NRVJfVVNJTkdfRklOR0VSUFJJTlQsIFVQREFURV9DVVNUT01FUl9GSU5HRVJfUFJJTlQsIEFDQ09VTlRfQkFMQU5DRSwgR1JVUFAsIEdPQUxTX0FORF9JTlZFU1RNRU5ULCBBR0VOQ1lfQkFOS0lORywgUlVCWVhdIiwiYXV0aCI6IlVjZktMSm80YWRmNnZhaHhDKzFKcVE9PSIsIkNoYW5uZWwiOiJERUZBVUxUIiwiSVAiOiIxMC4xMC4wLjUyIiwiaXNzIjoiQWNjaW9uIE1pY3JvZmluYW5jZSBCYW5rIiwiaWF0IjoxNzAyNjE4MTQxLCJleHAiOjYyNTE0Mjg0NDAwfQ.y3r0vju5BvHxrf_fgqMyth0xumcBEFHeYNTCPYt866w";

    public void processLoanTemplate() throws IOException {
        System.out.println("-------------- ENTRY POINT: {} ----------");
        List<ExcelRecordTracker> recordTrackers = getCurrentProcessableRecords();
        List<String> trackerSn = recordTrackers.stream().map(excelRecordTracker -> String.valueOf(excelRecordTracker.getSn())).collect(Collectors.toList());
        System.out.println("Record trackers: " +  trackerSn);
        List<CgapExcelRecord> cgapExcelRecords = getCgapRecordsFromExcelWithSerialNumbersIn(trackerSn);
        for(CgapExcelRecord cgapExcelRecord : cgapExcelRecords)
        {
            String customerCif = cgapExcelRecord.getCustomerNumber();
            String accountNumberFromExcel = cgapExcelRecord.getBrightaCommitmentAccount();
            String branch = cgapExcelRecord.getBranch();
            String accountOfficerCode = cgapExcelRecord.getAccountOfficerCode();
            String category = cgapExcelRecord.getProductCode();

            boolean isValidForProcessing = checkExistingCustomerValidForLoanProcessing(customerCif, accountNumberFromExcel, branch, accountOfficerCode, category);
            System.out.printf("Is customer with customer Number: %s valid for processing ? %s", customerCif, isValidForProcessing);
            if(isValidForProcessing){
                Customer customer = cgapRepository.findCustomerByCustomerNumber(customerCif);
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
                        .mobileNumber(customer.getMobileNumber())
                        .interestRate(cgapExcelRecord.getInterestRate())
                        .productCode(cgapExcelRecord.getProductCode())
                        .tenor(cgapExcelRecord.getTenor())
                        .accountOfficerCode(cgapExcelRecord.getAccountOfficerCode())
                        .status("SUCCESS")
                        .branch(cgapExcelRecord.getBranch())
                        .requestId("ACCION-NG_PO1_3_OID_".concat(String.valueOf(System.currentTimeMillis())))
                        .bvn(cgapExcelRecord.getBvn())
                        .guarantorSignOfferLetter(false)
                        .totalPerformingBalance(BigDecimal.ZERO)
                        .build();
                RubyxLoanRenewal createdLoanRenewal = cgapRepository.createRubyxLoanRenewalRecord(loanRenewal);
                if(createdLoanRenewal.getId() > 0){
                    ExcelRecordTracker tracker = recordTrackers.stream().filter(excelRecordTracker -> String.valueOf(excelRecordTracker.getSn()).equalsIgnoreCase(cgapExcelRecord.getSn())).findFirst().orElse(null);
                    if(Objects.nonNull(tracker)){
                        tracker.setStatus("COMPLETED");
                        cgapRepository.updatedExcelRecordTracker(tracker);
                    }
                }
            }
        }
    }

    public boolean checkExistingCustomerValidForLoanProcessing(String customerCif, String accountNumberFromExcel, String branch, String accountOfficerCode, String category){
        CustomerDetailsFromT24 detailsFromT24 = t24Service.customerDetailsFromT24(customerCif);
        System.out.println(detailsFromT24);

        if(Objects.nonNull(detailsFromT24) && Objects.nonNull(detailsFromT24.getResponseCode()) && detailsFromT24.getResponseCode().equalsIgnoreCase("00")){
            Customer omnixCustomer = cgapRepository.findCustomerByMobileNumber(detailsFromT24.getMobileNumber());
            if(Objects.nonNull(omnixCustomer)){

                // Check the authenticity of the customer's ownership of account
                log.info("Found customer with number: {} | {}", omnixCustomer.getCustomerNumber(), omnixCustomer.getMobileNumber());

                // Get all records of the customer
                String userCredentials = env.getProperty("omnix.channel.user.ussd");
                System.out.println("ACCOUNTING SYNC IN PROGRESS...");
                List<AccountDetailsResponseDto> accountsFromT24 = t24Service.fetchAccountDetailsList(customerCif, branch, userCredentials);
                for(AccountDetailsResponseDto account : accountsFromT24){
                    Account existing = cgapRepository.getAccountUsingAccountNumber(account.getAccountNumber());
                    if(Objects.isNull(existing)){
                        String retryAccountNumber = "0";
                        createNewAccountRecord(omnixCustomer, account.getAccountNumber(), account.getT24AccountNumber(), account.getCategoryCode());
                        log.info("Synced account with account number: {} for customer: {}", account.getAccountNumber(), customerCif);
                    }
                }
                List<String> accounts = cgapRepository.getAllCustomerAccountNumbers(omnixCustomer);
                log.info("Account Numbers of customer's in Omnix: {}", accounts);
                boolean isPassedValidation = accounts.contains(accountNumberFromExcel);
                System.out.printf("Customer with Customer Number: %s PASSED VALIDATION! ?? %s", customerCif, isPassedValidation);
                return isPassedValidation;
            }
            else{
                // Create the customer and the associated account and return true
                log.info("Creating customer");
                return t24Service.syncCustomerRecored(detailsFromT24.getMobileNumber(), "USSD");
            }
        }
        return false;
    }

    private Customer createCustomerAndAccount(CustomerDetailsFromT24 detailsFromT24, String accountNumber, String oldAccountNumber, String branch, String accountOfficerCode, String category){
        // Do BVN validation to get more customer details
        BVNRequestPayload bvnRequestPayload = new BVNRequestPayload();
        bvnRequestPayload.setRequestId(String.valueOf(System.currentTimeMillis()));
        bvnRequestPayload.setHash(UUID.randomUUID().toString());
        bvnRequestPayload.setBvn(detailsFromT24.getBvn());
        bvnRequestPayload.setMobileNumber(detailsFromT24.getMobileNumber());

        LocalDate dob = LocalDate.of(1990, 12, 10);
        String gender = "MALE";
        String firstName = "";
        String lastName = "";
        String middleName = "";
        String otherName = "";
        BVN bvn = null;
            BVNResponsePayload bvnResponsePayload = this.doBVNValidation(bvnRequestPayload);
            log.info("Bvn validation response: {}", bvnResponsePayload);
            if(Objects.nonNull(bvnResponsePayload) && bvnResponsePayload.getResponseCode().equalsIgnoreCase("00")) {
                dob = valueOrDefault(LocalDate.parse(bvnResponsePayload.getDob()), LocalDate.of(1990, 12, 10));
                gender = valueOrDefault(bvnResponsePayload.getGender(), "MALE");
                firstName = valueOrDefault(bvnResponsePayload.getFirstName(), "");
                lastName = valueOrDefault(bvnResponsePayload.getLastName(), "");
                middleName = valueOrDefault(bvnResponsePayload.getMiddleName(), "");
                otherName = String.join(middleName, firstName);
                bvn = cgapRepository.getBVNUsingBvnNumber(bvnResponsePayload.getBvn());
            }
            String mnemonic = UUID.randomUUID().toString().substring(0,4) + lastName;
            Customer newCustomer = new Customer();
            newCustomer.setCreatedAt(LocalDateTime.now());
            newCustomer.setAppUser(cgapRepository.getAppUserUsingUsername("DEFAULT"));
            newCustomer.setStatus("ACTIVE");
            newCustomer.setCustomerType("INDIVIDUAL");
            newCustomer.setAccountOfficerCode(accountOfficerCode);
            newCustomer.setOtherOfficerCode("9998");
            newCustomer.setSector("1000");
            newCustomer.setBranch(cgapRepository.getBranchUsingBranchCode(branch));
            newCustomer.setDob(dob);
            newCustomer.setKycTier("2"); // Because BVN is present
            newCustomer.setEmail("");
            newCustomer.setMobileNumber(detailsFromT24.getMobileNumber());
            newCustomer.setCustomerNumber(detailsFromT24.getCustomerId());
            newCustomer.setGender(gender);
            newCustomer.setOtherName(otherName);
            newCustomer.setLastName(lastName);
            newCustomer.setMaritalStatus("");
            newCustomer.setMnemonic(mnemonic);
            newCustomer.setTimePeriod(t24Service.getTimePeriod());
            newCustomer.setReferalCode(UUID.randomUUID().toString().substring(0, 9));
            newCustomer.setBalanceLimit(tier1BalanceLimit);
            newCustomer.setDepositLimit(tier1DepositLimit);
            newCustomer.setDailyLimit(tier1DailyBalance);
            newCustomer.setWithdrawalLimit(tier1WithdrawalLimit);
            newCustomer.setBvn(bvn);
            newCustomer.setImei("");

            Customer createdCustomer = cgapRepository.createCustomer(newCustomer);
            if(createdCustomer.getId() > 0){
                log.info("Customer created successfully. System will now go ahead to create account for customer with mobile number: {}", createdCustomer.getMobileNumber());
                Account account = createNewAccountRecord(createdCustomer, accountNumber, oldAccountNumber, category) ;
                if(account.getId() > 0){
                    log.info("System created account record successfully...");
                }
            }else{
                log.error("System could not create customer due to an exception.");
            }
            return createdCustomer;
    }

    private Account createNewAccountRecord(Customer oCustomer, String accountNumber, String oldAccountNumber, String category) {
        Account oAccount = new Account();
        oAccount.setAccountBalance(BigDecimal.ZERO);
        oAccount.setAccountNumber(accountNumber);
        oAccount.setAppUser(oCustomer.getAppUser());
        oAccount.setBranch(oCustomer.getBranch());
        oAccount.setCategory(category);
        oAccount.setCreatedAt(LocalDateTime.now());
        oAccount.setCustomer(oCustomer);
        oAccount.setOldAccountNumber(oldAccountNumber);
        oAccount.setPrimaryAccount(true);
        oAccount.setProduct(cgapRepository.getProductByCategoryCode(category));
        oAccount.setRequestId(String.valueOf(System.currentTimeMillis()));
        oAccount.setStatus("SUCCESS");
        oAccount = cgapRepository.createAccount(oAccount);
        return oAccount;
    }

    public List<ExcelRecordTracker> getCurrentProcessableRecords() throws IOException {
        Long count = cgapRepository.getCountOfExcelRecordTracker(); // Very important to prevent double entry!!!
        List<ExcelRecordTracker> processableRecords = cgapRepository.getAllPendingExcelRecords();
        if(processableRecords.isEmpty() && count <= 0){
            processableRecords = getRecordsFromExcel();
        }
        return processableRecords;
    }

    public List<ExcelRecordTracker> getRecordsFromExcel() throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);
        List<ExcelRecordTracker> trackers = new ArrayList<>();
        for(Row row : sheet)
        {
            Map<String, Object> rowMap;
            if(row.getRowNum() > 0){
                rowMap = getExcelRecordMap(row, true);
                ExcelRecordTracker tracker = miniMapper().readValue(miniMapper().writeValueAsBytes(rowMap), ExcelRecordTracker.class);
                cgapRepository.saveExcelRecordTracker(tracker);
                trackers.add(tracker);
            }
        }
        return trackers;
    }

    public List<CgapExcelRecord> getCgapRecordsFromExcelWithSerialNumbersIn(List<String> ids) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        Workbook workbook = new XSSFWorkbook(inputStream);
        Sheet sheet = workbook.getSheetAt(0);

        List<CgapExcelRecord> cgapExcelRecords = new ArrayList<>();
        for(Row row : sheet)
        {
            Map<String, Object> rowMap;
            if(row.getRowNum() > 0) {
                String snValue = resolveCellType(row, 0);
                if (ids.contains(snValue)) {
                    rowMap = getExcelRecordMap(row, false);
                    CgapExcelRecord cgapExcelRecord = miniMapper().readValue(miniMapper().writeValueAsBytes(rowMap), CgapExcelRecord.class);
                    cgapExcelRecords.add(cgapExcelRecord);
                }
            }
        }
        return cgapExcelRecords;
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

    @SneakyThrows
    private BVNResponsePayload doBVNValidation(BVNRequestPayload requestPayload) {
       String bvnResourceUrl = "http://192.168.1.7:1003/validation";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN);
        HttpEntity<BVNRequestPayload> entity = new HttpEntity<>(requestPayload, headers);
        BVNResponsePayload responsePayload = new BVNResponsePayload();
        try {
            ResponseEntity<String> bvnResponseEntity = restTemplate.postForEntity(bvnResourceUrl, entity, String.class);
            if(bvnResponseEntity.getStatusCode().is2xxSuccessful()) {
                responsePayload = miniMapper().readValue(bvnResponseEntity.getBody(), BVNResponsePayload.class);
            }
        }catch (Exception exception) {
            log.error("Exception occurred while trying to get customer's BVN details: {}", exception.getMessage());
            responsePayload.setResponseCode("96");
        }

        log.info("BVN response: {}", miniMapper().writeValueAsString(responsePayload));
        return responsePayload;
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

}
