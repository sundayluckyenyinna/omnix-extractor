package com.accionmfb.omnix.extractor.modules.cgap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(value = PropertyNamingStrategy.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CgapExcelRecord {

    private String sn;
    private String accountOfficerCode;
    private String branch;
    private String bvn;
    private String customerNumber;
    private String disbursementAccount;
    private String interestRate;
    private String mobileNumber;
    private String productCode;
    private String requestedAmount;
    private String renewalRating;
    private String tenor;
    private String brightaCommitmentAccount;
    private String approvedAmount;
}
