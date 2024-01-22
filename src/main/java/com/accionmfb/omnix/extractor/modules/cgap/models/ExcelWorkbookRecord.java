package com.accionmfb.omnix.extractor.modules.cgap.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "excel_workbook_record")
public class ExcelWorkbookRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workbook_name")
    private String workbookName;

    @Column(name = "read_date_time")
    private LocalDateTime readDateTime;

    @Column(name = "status")
    private String status;
}
