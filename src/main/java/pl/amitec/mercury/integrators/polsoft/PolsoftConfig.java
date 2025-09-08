package pl.amitec.mercury.integrators.polsoft;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;

@Data
public class PolsoftConfig {
    PolsoftFtpConfig ftp;
    String department;
    PolsoftProductsConfig products;

    @Value("${polsoft.orders.enabled}")
    boolean ordersEnabled;

    @Value("${polsoft.invoices.enabled}")
    boolean invoicesEnabled;
}
