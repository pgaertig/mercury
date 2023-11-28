package pl.amitec.mercury.clients.bitbee.impl;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.OffsetTimeSerializer;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetTime;

public class BitbeeFormatsModule extends SimpleModule {
    public BitbeeFormatsModule() {
        super("bitbee-formats");
        addDeserializer(OffsetTime.class, new OffsetTimeDeserializer());
        addDeserializer(LocalTime.class, new LocalTimeDeserializer());
        addDeserializer(LocalDate.class, new LocalDateDeserializer());

        addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE);
        addSerializer(OffsetTime.class, OffsetTimeSerializer.INSTANCE);
        addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE);
    }
}