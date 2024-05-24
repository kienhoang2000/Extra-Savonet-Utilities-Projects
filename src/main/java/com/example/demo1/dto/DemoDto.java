package com.example.demo1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DemoDto {

    private String email;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("user_metadata")
    private UserMetadataDto userMetadataDto;

    @JsonProperty("app_metadata")
    private AppMetadataDto appMetadataDto;
}
