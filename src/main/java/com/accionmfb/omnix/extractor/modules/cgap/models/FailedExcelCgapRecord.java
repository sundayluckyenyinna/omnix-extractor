package com.accionmfb.omnix.extractor.modules.cgap.models;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "failed_excel_record")
@JsonNaming(value = PropertyNamingStrategy.SnakeCaseStrategy.class)
public class FailedExcelCgapRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_officer_code")
    private String accountOfficerCode;

    @Column(name = "branch")
    private String branch;

    @Column(name = "bvn")
    private String bvn;

    @Column(name = "customer_number")
    private String customerNumber;

    @Column(name = "disbursement_account")
    private String disbursementAccount;

    @Column(name = "interest_rate")
    private String interestRate;

    @Column(name = "mobile_number")
    private String mobileNumber;

    @Column(name = "product_code")
    private String productCode;

    @Column(name = "requested_amount")
    private String requestedAmount;

    @Column(name = "renewal_rating")
    private String renewalRating;

    @Column(name = "tenor")
    private String tenor;

    @Column(name = "brighta_commitment_account")
    private String brightaCommitmentAccount;

    @Column(name = "approved_amount")
    private String approvedAmount;

    @Column(name = "failure_reason")
    private String failureReason;
}
