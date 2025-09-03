package com.example.bfhl_qualifier.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "bfhl")
public class BfhlProps {
    private String name;
    private String regNo;
    private String email;
    private String finalQuery;

    // getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFinalQuery() { return finalQuery; }
    public void setFinalQuery(String finalQuery) { this.finalQuery = finalQuery; }
}
