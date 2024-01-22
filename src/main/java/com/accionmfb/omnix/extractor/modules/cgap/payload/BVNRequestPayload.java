package com.accionmfb.omnix.extractor.modules.cgap.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BVNRequestPayload
{
    private String bvn;
    private String mobileNumber;
    private String requestId;
    private String hash;

}
