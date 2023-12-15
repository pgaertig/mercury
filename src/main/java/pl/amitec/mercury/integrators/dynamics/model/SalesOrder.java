package pl.amitec.mercury.integrators.dynamics.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record SalesOrder(
        String id,
        @JsonProperty("@odata.etag") String etag,
        @JsonProperty("Entry_No") String entryNo,
        @JsonProperty("Order_No") String orderNo,
        @JsonProperty("Customer_No") String customerNo,
        @JsonProperty("Customer_Name") String customerName,
        @JsonProperty("External_Document_No") String externalDocumentNo,
        @JsonProperty("Document_Date") LocalDate documentDate,
        @JsonProperty("Contact") String contact,
        @JsonProperty("Line_No") Long lineNo,
        @JsonProperty("Line_Type") String lineType,
        @JsonProperty("No") String no,
        @JsonProperty("Description") String description,
        @JsonProperty("Quantity") Long quantity,
        @JsonProperty("Unit_Of_Measure") String unitOfMeasure,
        @JsonProperty("Unit_Price") BigDecimal unitPrice,
        @JsonProperty("VAT_Percent") BigDecimal vatPercent,
        @JsonProperty("Line_Amount") BigDecimal lineAmount
) {
}
/*
        {
            "@odata.etag": "W/\"JzIwOzExMDY2NzYwMDI4MTg5OTIzMjAxMTswMDsn\"",
            "Entry_No": 13,
            "Order_No": "ORDER0001",
            "Customer_No": "10000",
            "Customer_Name": "Customer name",
            "External_Document_No": "",
            "Document_Date": "2023-04-12",
            "Contact": "TestUser",
            "Line_No": 1,
            "Line_Type": "ITEM",
            "No": "1972-S",
            "Description": "MUNICH Swivel Chair, yellow",
            "Quantity": 1,
            "Unit_Of_Measure": "PSC",
            "Unit_Price": 100,
            "VAT_Percent": 23,
            "Line_Amount": 100
        },
 */
