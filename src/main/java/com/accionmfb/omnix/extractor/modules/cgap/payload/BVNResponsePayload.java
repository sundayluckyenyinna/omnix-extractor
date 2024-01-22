/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.accionmfb.omnix.extractor.modules.cgap.payload;

import lombok.*;

/**
 *
 * @author bokon
 */
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class BVNResponsePayload {

    private String firstName;
    private String lastName;
    private String middleName;
    private String mobileNumber;
    private String dob;
    private String bvn;
    private String gender;
    private String image;
    private String responseCode;
}
