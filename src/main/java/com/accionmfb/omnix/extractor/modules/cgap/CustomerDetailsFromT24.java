package com.accionmfb.omnix.extractor.modules.cgap;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class CustomerDetailsFromT24 {
    private String responseCode;
    private String responseMessage;
    private String customerId;
    private String bvn;
    private String mobileNumber;
}
