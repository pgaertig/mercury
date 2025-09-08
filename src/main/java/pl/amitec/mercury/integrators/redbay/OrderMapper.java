package pl.amitec.mercury.integrators.redbay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.amitec.mercury.clients.bitbee.types.*;
import pl.amitec.mercury.util.StringUtils;
import pl.redbay.ws.client.types.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OrderMapper {

    private static final Logger LOG = LoggerFactory.getLogger(OrderMapper.class);
    private final Map<Long, BigDecimal> taxToPercent;

    public OrderMapper(Map<Long, BigDecimal> taxToPercent) {
        this.taxToPercent = taxToPercent;
    }

    public RbOrder getRbOrder(Order bbOrder) {
        var rbOrder = new pl.redbay.ws.client.types.RbOrder();
        rbOrder.setCurrency("PLN");
        rbOrder.setDescription(bbOrder.comment());
        rbOrder.setIp("148.251.123.143");

        rbOrder.useDelivery(getDelivery(bbOrder.delivery()))
            .usePayment(getPayment(bbOrder.payment()))
            .useContact(getAddress(bbOrder.contact()))
            .useInvoice(getAddress(bbOrder.invoice()))
            .useReceiver(getAddress(bbOrder.receiver()))
            .useAdded(bbOrder.added().toString())
            .useSource("bitbee")
            .useSourceid(bbOrder.uniqueNumber())
            .setProperties(new RbArrayOfOrderProperties().useItems(
                    List.of(
                        new RbOrderProperty().useName("BITBEE_ORDER_NO").useValue(bbOrder.uniqueNumber())
                    )
            ));
        rbOrder.setPositions(getPositions(bbOrder.positions()));

        return rbOrder;
    }

    private RbPayment getPayment(Payment payment) {
        return new RbPayment().useName(payment.name());
    }

    private RbDelivery getDelivery(Delivery delivery) {
        return new RbDelivery().useName(delivery.name()).useCost(delivery.cost());
    }

    private RbAddress getAddress(OrderParty address) {
        // TODO country/province
        return new RbAddress()
                .useCity(StringUtils.nonEmpty(address.city(), "Brak"))
                .useStreet(StringUtils.nonEmpty(address.street(), "Brak"))
                .useFlat(address.flat())
                .usePostcode(StringUtils.nonEmpty(address.postcode(), "99-999"))
                .useForname(address.forname())
                .useSurname(address.surname())
                .usePhone(address.phone())
                .useEmail(address.email())
                .useCompany(Optional.ofNullable(address.company())
                        .map(company -> new RbCompany()
                                .useNip(company.nip())
                                .useRegon(company.regon())
                                .useFullname(company.fullname()))
                        .orElse(null)
                );
    }

    public RbArrayOfPositions getPositions(List<OrderPosition> positionsList) {
        return new RbArrayOfPositions().useItems(positionsList.stream()
                .map(this::getPosition).toList());
    }

    private RbPosition getPosition(OrderPosition orderPosition) {
        return new RbPosition()
                .useVariant(orderPosition.variantSourceId())
                .useCode(orderPosition.code())
                //.useEan()
                .useQuantity(orderPosition.quantity())
                .useTax(getTax(orderPosition))
                .usePrice(orderPosition.price())
                .useFinalprice(orderPosition.brutto())
                .useName(orderPosition.name())
                .useComment(orderPosition.comment())
                .useSettings(new RbArrayOfPositionSettings().useItems(
                        List.of(
                                new RbPositionSetting().useName("BITBEE_ID").useValue(orderPosition.id().toString()),
                                new RbPositionSetting().useName("BITBEE_TO_STRING").useValue(orderPosition.toString())
                        )
                ));
    }

    private RbTax getTax(OrderPosition orderPosition) {
        BigDecimal percent = taxToPercent.get(orderPosition.taxId());
        return new RbTax()
                .useName(orderPosition.taxName())
                .usePercent(percent.longValue());

    }
}
