package com.accionmfb.omnix.extractor.modules.cgap.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;

@Getter
@Setter
@ToString
@Entity
@Table(name = "excel_record_tracker")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class ExcelRecordTracker {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    String sn;
    @Column(name = "customer_number")
    String customerNumber;
    String status;
}
