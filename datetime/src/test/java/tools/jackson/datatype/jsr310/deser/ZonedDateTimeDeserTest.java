package tools.jackson.datatype.jsr310.deser;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonFormat.Feature;

import tools.jackson.core.type.TypeReference;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeFeature;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.datatype.jsr310.ModuleTestBase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class ZonedDateTimeDeserTest extends ModuleTestBase
{
    private final ObjectReader READER = newMapper().readerFor(ZonedDateTime.class);

    private final ObjectReader READER_NON_NORMALIZED_ZONEID = JsonMapper.builder()
            .addModule(new JavaTimeModule().disable(JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID))
            .build()
            .readerFor(ZonedDateTime.class);
    
    private final TypeReference<Map<String, ZonedDateTime>> MAP_TYPE_REF = new TypeReference<Map<String, ZonedDateTime>>() { };

    static class WrapperWithFeatures {
        @JsonFormat(without = JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
        public ZonedDateTime value;
    }

    static class WrapperWithReadTimestampsAsNanosDisabled {
        @JsonFormat(
            without=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public ZonedDateTime value;

        public WrapperWithReadTimestampsAsNanosDisabled() { }
        public WrapperWithReadTimestampsAsNanosDisabled(ZonedDateTime v) { value = v; }
    }

    static class WrapperWithReadTimestampsAsNanosEnabled {
        @JsonFormat(
            with=Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS
        )
        public ZonedDateTime value;

        public WrapperWithReadTimestampsAsNanosEnabled() { }
        public WrapperWithReadTimestampsAsNanosEnabled(ZonedDateTime v) { value = v; }
    }

    @Test
    public void testDeserFromString() throws Exception
    {
        assertEquals("The value is not correct.",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                READER.readValue(q("2000-01-01T12:00Z")));
    }

    // [modules-java#281]
    @Test
    public void testDeserFromStringNoZoneIdNormalization() throws Exception
    {
        // 11-Nov-2023, tatu: Not sure this is great test but... does show diff
        //   behavior with and without `JavaTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID`
        assertEquals("The value is not correct.",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, TimeZone.getTimeZone("UTC").toZoneId()),
                READER_NON_NORMALIZED_ZONEID.readValue(q("2000-01-01T12:00Z")));
    }

    @Test
    public void testDeserializationAsInt01() throws Exception
    {
        ObjectReader reader = newMapper().readerFor(WrapperWithReadTimestampsAsNanosDisabled.class);
        ZonedDateTime date = ZonedDateTime.of(
            LocalDateTime.ofEpochSecond(1, 1000000, ZoneOffset.UTC),
            ZoneOffset.UTC);
        WrapperWithReadTimestampsAsNanosDisabled actual =
            reader.readValue(a2q("{'value':1001}"));
        assertEquals("The value is not correct.", date, actual.value);
    }

    @Test
    public void testDeserializationAsInt02() throws Exception
    {
        ObjectReader reader = newMapper().readerFor(WrapperWithReadTimestampsAsNanosEnabled.class);
        ZonedDateTime date = ZonedDateTime.of(
            LocalDateTime.ofEpochSecond(1, 0, ZoneOffset.UTC),
            ZoneOffset.UTC);
        WrapperWithReadTimestampsAsNanosEnabled actual =
            reader.readValue(a2q("{'value':1}"));
        assertEquals("The value is not correct.", date, actual.value);
    }

    @Test
    public void testDeserializationComparedToStandard() throws Throwable
    {
        String inputString = "2021-02-01T19:49:04.0513486Z";

        assertEquals("The value is not correct.",
                DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(inputString, ZonedDateTime::from),
                READER.readValue(q(inputString)));
    }

    @Test
    public void testDeserializationComparedToStandard2() throws Throwable
    {
        String inputString = "2021-02-01T19:49:04.0513486Z[UTC]";

        ZonedDateTime converted = newMapperBuilder()
                .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
                .build()
                .readerFor(ZonedDateTime.class).readValue(q(inputString));

        assertEquals("The value is not correct.",
                DateTimeFormatter.ISO_ZONED_DATE_TIME.parse(inputString, ZonedDateTime::from),
                converted);
    }

    @Test
    public void testBadDeserializationAsString01() throws Throwable
    {
        try {
            READER.readValue(q("notazone"));
            fail("Should nae pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.ZonedDateTime` from String");
        }
    }
    
    @Test
    public void testDeserializationAsArrayDisabled() throws Throwable
    {
        try {
            READER.readValue("[\"2000-01-01T12:00Z\"]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.ZonedDateTime` from Array");
        }
    }
    
    @Test
    public void testDeserializationAsEmptyArrayDisabled() throws Throwable
    {
        try {
            READER.readValue("[]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.ZonedDateTime` from Array");
        }
        try {
            newMapper()
                .readerFor(ZonedDateTime.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .readValue("[]");
            fail("expected MismatchedInputException");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize value of type `java.time.ZonedDateTime` from Array");
        }
    }

    @Test
    public void testDeserializationAsArrayEnabled() throws Throwable
    {
        ZonedDateTime value = newMapper()
    			.readerFor(ZonedDateTime.class)
               .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
    			.readValue("[\"2000-01-01T12:00Z\"]");
        assertEquals("The value is not correct.",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC),
                value);
    }
    
    @Test
    public void testDeserializationAsEmptyArrayEnabled() throws Throwable
    {
        ZonedDateTime value = newMapper()
                .readerFor(ZonedDateTime.class)
                .with(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS,
                        DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT)
                .readValue("[]");
        assertNull(value);
    }

    @Test
    public void testDeserializationWithZonePreserved() throws Throwable
    {
        WrapperWithFeatures wrapper = newMapper()
                .readerFor(WrapperWithFeatures.class)
                .readValue("{\"value\":\"2000-01-01T12:00+01:00\"}");
        assertEquals("Timezone should be preserved.",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0 ,0, ZoneOffset.ofHours(1)),
                wrapper.value);
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testLenientDeserializeFromEmptyString() throws Exception {

        String key = "zoneDateTime";
        ObjectMapper mapper = newMapper();
        ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, ZonedDateTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        ZonedDateTime actualDateFromNullStr = actualMapFromNullStr.get(key);
        assertNull(actualDateFromNullStr);

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, ""));
        Map<String, ZonedDateTime> actualMapFromEmptyStr = objectReader.readValue(valueFromEmptyStr);
        ZonedDateTime actualDateFromEmptyStr = actualMapFromEmptyStr.get(key);
        assertEquals("empty string failed to deserialize to null with lenient setting", null, actualDateFromEmptyStr);
    }

    @Test ( expected =  MismatchedInputException.class)
    public void testStrictDeserializeFromEmptyString() throws Exception {

        final String key = "zonedDateTime";
        final ObjectMapper mapper = mapperBuilder()
                .withConfigOverride(ZonedDateTime.class,
                        o -> o.setFormat(JsonFormat.Value.forLeniency(false)))
            .build();
        final ObjectReader objectReader = mapper.readerFor(MAP_TYPE_REF);

        String valueFromNullStr = mapper.writeValueAsString(asMap(key, null));
        Map<String, ZonedDateTime> actualMapFromNullStr = objectReader.readValue(valueFromNullStr);
        assertNull(actualMapFromNullStr.get(key));

        String valueFromEmptyStr = mapper.writeValueAsString(asMap(key, ""));
        objectReader.readValue(valueFromEmptyStr);
    }

    /*
    /**********************************************************
    /* Tests for ISO-8601 ZonedDateTimes that are colonless
    /**********************************************************
     */

    @Test
    public void testDeserializationWithoutColonInOffset() throws Throwable
    {
        WrapperWithFeatures wrapper = READER
                .forType(WrapperWithFeatures.class)
                .readValue("{\"value\":\"2000-01-01T12:00+0100\"}");

        assertEquals("Value parses as if it were with colon",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0 ,0, ZoneOffset.ofHours(1)),
                wrapper.value);
    }

    @Test
    public void testDeserializationWithoutColonInTimeZoneWithTZDB() throws Throwable
    {
        WrapperWithFeatures wrapper = READER
                .forType(WrapperWithFeatures.class)
                .readValue("{\"value\":\"2000-01-01T12:00+0100[Europe/Paris]\"}");
        assertEquals("Timezone should be preserved.",
                ZonedDateTime.of(2000, 1, 1, 12, 0, 0 ,0, ZoneId.of("Europe/Paris")),
                wrapper.value);
    }
}
