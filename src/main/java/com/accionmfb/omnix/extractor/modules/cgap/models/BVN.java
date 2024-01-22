/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.extractor.modules.cgap.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@ToString
@Table(name = "bvn")
public class BVN implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "customer_bvn")
    private String customerBvn;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "mobile_number")
    private String mobileNumber;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "date_of_birth")
    private String dateOfBirth;
    @Column(name = "gender")
    private String gender;
    @Column(name = "source")
    private String source;
    @Column(name = "middle_name")
    private String middleName;
    @Column(name = "registration_date")
    private String registrationDate;
    @Column(name = "status")
    private String status;
    @JsonIgnore
    @ManyToOne
    private AppUser appUser;
    @Column(name = "failure_reason")
    private String failureReason;
}
