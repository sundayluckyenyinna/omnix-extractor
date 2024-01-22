package com.accionmfb.omnix.extractor.modules.cgap.payload;

import lombok.Data;

/**
 *
 * @author dofoleta
 */
@Data
public class AccountDetailsResponseDto {

    private double ledgerBalance;
    private String responseCode;
    private String responseMessage;
    private int accountRestricton;
    private String accountStatus;
    private String customerId = "";
    private String accountName = "";
    private String accountNumber = "";
    private String currency = "";
    private double availableBalance;
    private String branchCode = "";
    private String branchName = "";
    private String accountType;
    private String productCode;
    private String accountDescription;
    private String productDescription;

    private String t24AccountNumber;
    private String openingDate;
    private String categoryCode;
    private int kycTier;
}
