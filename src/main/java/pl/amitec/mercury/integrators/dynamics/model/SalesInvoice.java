package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesInvoice(
        @JsonProperty("No") String no,
        @JsonProperty("@odata.etag") String etag,
        @JsonProperty("Posting_Date") LocalDate postingDate,
        @JsonProperty("Sell_to_Customer_No") String sellToCustomerNo,
        @JsonProperty("Sell_to_Customer_Name") String sellToCustomerName,
        @JsonProperty("Sell_to_Address") String sellToAddress,
        @JsonProperty("Amount") BigDecimal amount,
        @JsonProperty("Amount_Including_VAT") BigDecimal amountIncludingVat,
        @JsonProperty("Order_Date") LocalDate orderDate,
        @JsonProperty("Order_No") String orderNo
) {}
