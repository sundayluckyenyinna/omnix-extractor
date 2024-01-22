package com.accionmfb.omnix.extractor.modules.cgap;

import com.accionmfb.omnix.extractor.modules.cgap.models.Account;
import com.accionmfb.omnix.extractor.modules.cgap.models.Branch;
import com.accionmfb.omnix.extractor.modules.cgap.models.Customer;
import com.accionmfb.omnix.extractor.modules.cgap.payload.AccountDetailsResponseDto;
import com.jbase.jremote.*;
import com.temenos.tocf.t24ra.T24Connection;
import com.temenos.tocf.t24ra.T24DefaultConnectionFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


@Slf4j
//@Service
@RequiredArgsConstructor
public class T24Service {

    private static final String T24_HOST = "192.168.1.110";
    private static final int T24_PORT = 1572;
    @Value("${omnix.start.morning}")
    private String startMorning;
    @Value("${omnix.end.morning}")
    private String endMorning;
    @Value("${omnix.start.afternoon}")
    private String startAfternoon;
    @Value("${omnix.end.afternoon}")
    private String endAfternoon;
    @Value("${omnix.start.evening}")
    private String startEvening;
    @Value("${omnix.end.evening}")
    private String endEvening;
    @Value("${omnix.start.night}")
    private String startNight;
    @Value("${omnix.end.night}")
    private String endNight;

    @Value("${omnix.t24.ofs.source}")
    public String OFS_STRING;

    @Value("${omnix.t24.ofs.id}")
    public String OFS_ID;

    private final Environment env;
    private final CgapRepository cgapRepository;

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }

    public CustomerDetailsFromT24 customerDetailsFromT24(String customerId) {
        DefaultJConnectionFactory connectionFactory = new DefaultJConnectionFactory();
        connectionFactory.setPort(T24_PORT);
        connectionFactory.setHost(T24_HOST);
        JConnection conn;
        CustomerDetailsFromT24 customerResponse = new CustomerDetailsFromT24();
        Set<String> setObject = new HashSet<>();
        try {
            conn = connectionFactory.getConnection();
            JStatement stmt = conn.createStatement();
            stmt.setFetchSize(10);
            String command = "LIST FBNK.CUSTOMER WITH @ID EQ ".concat(customerId.trim()).concat(" BVN TEL.MOBILE NO.PAGE");
            JResultSet rst = stmt.execute(command);
            customerResponse = new CustomerDetailsFromT24();
            String customerNumber = "";
            String bvn = "";
            String mobileNumber = "";
            JDynArray row;
            while (rst.next()) {
                row = rst.getRow();
                boolean rowAdded = setObject.add(row.get(1));
                if (!rowAdded) {
                    continue;
                }
                try {
                    customerNumber = row.get(1);
                    bvn = row.get(2);
                    mobileNumber = row.get(3);
                    customerResponse.setResponseCode("00");
                    customerResponse.setResponseMessage("Customer retrieved");
                    customerResponse.setCustomerId(customerNumber);
                    customerResponse.setBvn(bvn);
                    customerResponse.setMobileNumber(mobileNumber);
                } catch (Exception e) {
                }
            }

        } catch (JRemoteException ex) {
            System.out.println(ex);
        }
        log.info("Customer Details from T24 {}", customerResponse);
        return customerResponse;
    }

    public char getTimePeriod() {
        char timePeriod = 'M';
        int hour = LocalDateTime.now().getHour();
        int morningStart = Integer.parseInt(startMorning);
        int morningEnd = Integer.parseInt(endMorning);
        int afternoonStart = Integer.parseInt(startAfternoon);
        int afternoonEnd = Integer.parseInt(endAfternoon);
        int eveningStart = Integer.parseInt(startEvening);
        int eveningEnd = Integer.parseInt(endEvening);
        int nightStart = Integer.parseInt(startNight);
        int nightEnd = Integer.parseInt(endNight);
        //Check the the period of the day
        if (hour >= morningStart && hour <= morningEnd) {
            timePeriod = 'M';
        }
        if (hour >= afternoonStart && hour <= afternoonEnd) {
            timePeriod = 'A';
        }
        if (hour >= eveningStart && hour <= eveningEnd) {
            timePeriod = 'E';
        }
        if (hour >= nightStart && hour <= nightEnd) {
            timePeriod = 'N';
        }
        return timePeriod;
    }

    public boolean syncCustomerRecored(String mobileNumber, String channel) {

        DefaultJConnectionFactory connectionFactory = new DefaultJConnectionFactory();
        connectionFactory.setPort(T24_PORT);
        connectionFactory.setHost(T24_HOST);
        JConnection conn;
        Customer oCustomer = null;
        String branchCode = null;
        try {
            conn = connectionFactory.getConnection();
            JStatement stmt = conn.createStatement();
            stmt.setFetchSize(10);
            String command = "LIST FBNK.CUSTOMER WITH TEL.MOBILE LIKE ...".concat(mobileNumber.trim()).concat("... SHORT.NAME NAME.1 MARRIED.STATUS MNEMONIC WORK.GEN KYC.LEVEL BIRTH.INCORP.DATE E.MAIL.ADDRESS STREET SUBURB.TOWN PROVINCE.STATE SECTOR WORK.TITLE PROFESSION ACCOUNT.OFFICER COMPANY.BOOK OTHER.OFFICER BVN NO.PAGE");
            JResultSet rst = stmt.execute(command);

            Customer newCustomer = new Customer();

            while (rst.next()) {
                try {
                    JDynArray row = rst.getRow();
                    String customerNumber = row.get(1);
                    String lastName = row.get(2);
                    String otherName = row.get(3);
                    String maritalStatus = row.get(4);
                    String mnemonic = row.get(5);
                    String gender = row.get(6);
                    String kycTier = row.get(7);
                    String dob = row.get(8);
                    String email = row.get(9);
                    String address = row.get(10);
                    String city = row.get(11);
                    String state = row.get(12);
                    String sector = row.get(13);
                    String title = row.get(14);
                    String education = row.get(15);
                    String accountOfficer = row.get(16);
                    branchCode = row.get(17);
                    String otherOfficer = row.get(18);

                    Branch branch = cgapRepository.getBranchUsingBranchCode(branchCode);

                    String referalCode = UUID.randomUUID().toString().substring(0, 9);
                    newCustomer.setBranch(branch);
                    newCustomer.setAccountOfficerCode(accountOfficer);
                    newCustomer.setAppUser(cgapRepository.getAppUserUsingChannel(channel));
                    newCustomer.setCreatedAt(LocalDateTime.now());
                    newCustomer.setCustomerNumber(customerNumber);
                    newCustomer.setCustomerType("INDIVIDUAL");
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

                    //convert String to LocalDate
                    LocalDate localDate = LocalDate.parse(dob, formatter);
                    newCustomer.setDob(localDate);
                    newCustomer.setEmail(email == null ? "" : email);
                    newCustomer.setEducationLevel(education);
                    newCustomer.setGender(gender.equalsIgnoreCase("2") ? "FEMALE" : "MALE");
                    newCustomer.setLastName(lastName);
                    newCustomer.setKycTier(kycTier);
                    newCustomer.setMaritalStatus(maritalStatus);
                    newCustomer.setMnemonic(mnemonic);
                    newCustomer.setMobileNumber(mobileNumber);
                    newCustomer.setOtherName(otherName);
                    newCustomer.setOtherOfficerCode(otherOfficer);
                    newCustomer.setResidenceAddress(address);
                    newCustomer.setResidenceCity(city);
                    newCustomer.setResidenceState(state);
                    newCustomer.setSector(sector);
                    newCustomer.setStatus("ACTIVE");
                    newCustomer.setTitle(title);
                    newCustomer.setRequestId(referalCode);
                    newCustomer.setTimePeriod(getTimePeriod());
                    newCustomer.setReferalCode(referalCode);
                    oCustomer = cgapRepository.createCustomer(newCustomer);

                    //TODO fetch accounts
                    String userCredentials = env.getProperty("omnix.channel.user.".concat(channel.toLowerCase()));
                    ArrayList<AccountDetailsResponseDto> list = fetchAccountDetailsList(customerNumber, branchCode, userCredentials);
                    if (list != null && !list.isEmpty() ) {
                        for (AccountDetailsResponseDto responseData : list) {
                            if (responseData.getResponseCode().equalsIgnoreCase("00")) {
                                Account oAccount = new Account();
                                oAccount.setAccountBalance(BigDecimal.ZERO);
                                oAccount.setAccountNumber(responseData.getAccountNumber());
                                oAccount.setAppUser(oCustomer.getAppUser());
                                oAccount.setBranch(oCustomer.getBranch());
                                oAccount.setCategory(responseData.getCategoryCode());
                                oAccount.setCreatedAt(LocalDateTime.now());
                                oAccount.setCustomer(oCustomer);
                                oAccount.setOldAccountNumber(responseData.getT24AccountNumber());
                                oAccount.setPrimaryAccount(true);
                                oAccount.setProduct(cgapRepository.getProductByCategoryCode(responseData.getCategoryCode()));
                                oAccount.setRequestId(UUID.randomUUID().toString().substring(0, 9));
                                oAccount.setStatus("SUCCESS");
                                oAccount = cgapRepository.createAccount(oAccount);
                            }
                        }
                    }
                    break;
                } catch (Exception e) {
                }
            }
            return true;
        } catch (JRemoteException ex) {
            return false;
        }
    }

    public ArrayList<AccountDetailsResponseDto> fetchAccountDetailsList(String customerNumber, String branchCode, String userCredentials) {
        ArrayList<AccountDetailsResponseDto> listOfAccountsWithDetails = new ArrayList<>();

        try {
            if (customerNumber != null && !customerNumber.isEmpty()) {

                StringBuilder ofsBase = new StringBuilder("");
                ofsBase.append("ENQUIRY.SELECT,,")
                        .append(userCredentials)
                        .append("/")
                        .append(branchCode)
                        .append(",AMFB.CUST.AC,CUST.NO:EQ=")//.append("516555");
                        .append(customerNumber);

                log.info("OFS ACCOUNT REQUEST: {}", ofsBase);
                String sResponse = postToT24(ofsBase.toString());

                try {
                    log.info("OFS ACCOUNTS LIST RESPONSE: {}", sResponse);
                    sResponse = sResponse.split("Branch,")[1];
                    String acccont[] = sResponse.split("\",\"");

                    for (String line : acccont) {
                        if (line.contains("\t")) {
                            String itemList[] = line.split("\t");
                            line = itemList[2].replace("\"", "");

                            AccountDetailsResponseDto responseData = getAccountDetails(line, branchCode, userCredentials);
                            listOfAccountsWithDetails.add(responseData);
                        }
                    }
                } catch (Exception e) {
                    log.info("Exception occurred while trying to fetch customer core accounts. Exception message is: {}", e.getMessage());
                    return new ArrayList<>();
                }

            }

        } catch (Exception ex) {
            log.info("Exception occurred while fetching customer's account from core. Exception message is: {}", ex.getMessage());
            listOfAccountsWithDetails = new ArrayList<>();
            AccountDetailsResponseDto oAccountDetailsResponseDto = new AccountDetailsResponseDto();
            oAccountDetailsResponseDto.setResponseCode("06");
            listOfAccountsWithDetails.add(oAccountDetailsResponseDto);
            return new ArrayList<>();
        }

        return listOfAccountsWithDetails;
    }


    public AccountDetailsResponseDto getAccountDetails(String accountNnumber, String branchCode, String userCredentials) {
        AccountDetailsResponseDto responseData = new AccountDetailsResponseDto();

        try {
            StringBuilder ofsBase = new StringBuilder("");
            ofsBase.append("ENQUIRY.SELECT,,")
                    .append(userCredentials)
                    .append("/")
                    .append(branchCode)
                    .append(",ACCION.ACCOUNT.DETAIL,ACCOUNT.NUMBER:EQ=")
                    .append(accountNnumber);

            String sResponse = postToT24(ofsBase.toString());

            sResponse = sResponse.replaceFirst(",\"", "^");
            String[] items = getMessageTokens(sResponse, "^");
            ArrayList list = new ArrayList();
            int count = 0;
            String testParam = "";
            for (int k = 0; k < items.length; k++) {
                testParam = items[0];
                if (k > 0) {
                    list.add(items[k]);
                    count++;
                }
            }

            if (testParam.contains("SECURITY VIOLATION DURING SIGN ON PROCESS")) {
                AccountDetailsResponseDto data = new AccountDetailsResponseDto();
                data.setResponseCode("06");
                data.setResponseMessage("SECURITY VIOLATION DURING SIGN ON PROCESS.");
                return data;
            }
            if (testParam.contains("No records were found that matched the selection criteria")) {
                AccountDetailsResponseDto data = new AccountDetailsResponseDto();
                data.setResponseCode("03");
                data.setResponseMessage("INVALID ACCOUNT.");
                return data;
            }
            for (int k = 0; k < count; k++) {
                String val = ((String) list.get(k)).replaceAll("\"", "");
                items = val.split("\t");

                responseData.setAccountNumber(items[0].replaceAll("\\n", ""));
                responseData.setAccountName(items[1]);
                responseData.setCustomerId(items[2].trim());
                responseData.setAccountDescription(items[3]);
                responseData.setCurrency(items[4]);
                double lockedAmount = 0;
                try {
                    if (items.length > 22) {
                        lockedAmount = Double.parseDouble(items[22].trim().replaceAll(",", ""));
                    }
                } catch (NumberFormatException numberFormatException) {
                }
                try {
                    responseData.setAvailableBalance(Double.parseDouble(items[5].trim()) - lockedAmount);
                } catch (NumberFormatException numberFormatException) {
                }
                try {
                    responseData.setLedgerBalance(Double.parseDouble(items[8].trim()));
                } catch (NumberFormatException numberFormatException) {
                }
                try {
                    responseData.setAccountRestricton(Integer.parseInt(items[9]));
                } catch (Exception e) {
                    responseData.setAccountRestricton(0);
                }

                try {
                    if (items.length > 23) {
                        String code = items[23].trim().replaceAll(",", "");
                        String values[] = code.split("\\*");
                        if (values.length > 1) {

                            int countOfValues = values.length;
                            responseData.setCategoryCode(values[countOfValues - 1]);
                        } else {
                            responseData.setCategoryCode(items[23].trim().replaceAll(",", ""));
                        }
                    }
                } catch (NumberFormatException numberFormatException) {
                }

                responseData.setAccountStatus(items[12]);
                responseData.setBranchCode(items[13].trim());
                responseData.setBranchName(items[14].trim());

                responseData.setT24AccountNumber(items[17]);

                if (responseData.getAccountNumber().isEmpty()) {
                    responseData.setAccountNumber(responseData.getT24AccountNumber());
                }
                responseData.setResponseCode("00");
            }

        } catch (Exception ex) {
            responseData.setResponseCode("06");
            responseData.setResponseMessage("System error.");
        }
        return responseData;
    }

    public static String[] getMessageTokens(String msg, String delim) {
        StringTokenizer tokenizer = new StringTokenizer(msg, delim);
        ArrayList tokens = new ArrayList(10);
        for (; tokenizer.hasMoreTokens(); tokens.add(tokenizer.nextToken())) {
        }
        return (String[]) (String[]) tokens.toArray(new String[1]);
    }


    public String postToT24(String requestBody) {
        try {
            log.info("T24 - Host: {}, T24 - Port: {}", T24_HOST, T24_PORT);
            T24DefaultConnectionFactory connectionFactory = new T24DefaultConnectionFactory();
            connectionFactory.setHost(T24_HOST);
            connectionFactory.setPort(T24_PORT); //
            connectionFactory.enableCompression();

            Properties properties = new Properties();
            properties.setProperty("allow input", "true");
            properties.setProperty(OFS_STRING, OFS_ID);
            connectionFactory.setConnectionProperties(properties);

            T24Connection t24Connection = connectionFactory.getConnection();
            String ofsResponse = t24Connection.processOfsRequest(requestBody);

            t24Connection.close();
            return ofsResponse;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }
}
