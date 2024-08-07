package tools.jackson.datatype.jsr310.deser;

import java.time.Month;
import java.time.temporal.TemporalAccessor;

import org.junit.Test;
import org.junit.function.ThrowingRunnable;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.exc.InvalidFormatException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.datatype.jsr310.JavaTimeFeature;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.datatype.jsr310.MockObjectConfiguration;
import tools.jackson.datatype.jsr310.ModuleTestBase;

import static org.junit.Assert.*;

public class OneBasedMonthDeserTest extends ModuleTestBase
{
    static class Wrapper {
        public Month value;

        public Wrapper(Month v) { value = v; }
        public Wrapper() { }
    }

    @Test
    public void testDeserializationAsString01_oneBased() throws Exception
    {
        assertEquals(Month.JANUARY, readerForOneBased().readValue("\"1\""));
    }

    @Test
    public void testDeserializationAsString01_zeroBased() throws Exception
    {
        assertEquals(Month.FEBRUARY, readerForZeroBased().readValue("\"1\""));
    }


    @Test
    public void testDeserializationAsString02_oneBased() throws Exception
    {
        assertEquals(Month.JANUARY, readerForOneBased().readValue("\"JANUARY\""));
    }

    @Test
    public void testDeserializationAsString02_zeroBased() throws Exception
    {
        assertEquals(Month.JANUARY, readerForZeroBased().readValue("\"JANUARY\""));
    }

    @Test
    public void testBadDeserializationAsString01_oneBased() {
        assertError(
            () -> readerForOneBased().readValue("\"notamonth\""),
            InvalidFormatException.class,
            // Order of enumerated values not stable, so don't check:
            "Cannot deserialize value of type `java.time.Month` from String \"notamonth\":"
            +" not one of the values accepted for Enum class: ["
        );
    }

    static void assertError(ThrowingRunnable codeToRun, Class<? extends Throwable> expectedException, String expectedMessage) {
        try {
            codeToRun.run();
            fail(String.format("Expecting %s, but nothing was thrown!", expectedException.getName()));
        } catch (Throwable actualException) {
            if (!expectedException.isInstance(actualException)) {
                fail(String.format("Expecting exception of type %s, but %s was thrown instead", expectedException.getName(), actualException.getClass().getName()));
            }
            if (actualException.getMessage() == null || !actualException.getMessage().contains(expectedMessage)) {
                fail(String.format("Expecting exception with message containing: '%s', but the actual error message was:'%s'", expectedMessage, actualException.getMessage()));
            }
        }
    }


    @Test
    public void testDeserialization01_zeroBased() throws Exception
    {
        assertEquals(Month.FEBRUARY, readerForZeroBased().readValue("1"));
    }

    @Test
    public void testDeserialization01_oneBased() throws Exception
    {
        assertEquals(Month.JANUARY, readerForOneBased().readValue("1"));
    }

    @Test
    public void testDeserialization02_zeroBased() throws Exception
    {
        assertEquals(Month.SEPTEMBER, readerForZeroBased().readValue("\"8\""));
    }

    @Test
    public void testDeserialization02_oneBased() throws Exception
    {
        assertEquals(Month.AUGUST, readerForOneBased().readValue("\"8\""));
    }

    @Test
    public void testDeserializationWithTypeInfo01_oneBased() throws Exception
    {
        ObjectMapper MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule().enable(JavaTimeFeature.ONE_BASED_MONTHS))
            .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
            .build();

        TemporalAccessor value = MAPPER.readValue("[\"java.time.Month\",11]", TemporalAccessor.class);
        assertEquals(Month.NOVEMBER, value);
    }

    @Test
    public void testDeserializationWithTypeInfo01_zeroBased() throws Exception
    {
        ObjectMapper MAPPER = JsonMapper.builder()
                .addMixIn(TemporalAccessor.class, MockObjectConfiguration.class)
                .build();

        TemporalAccessor value = MAPPER.readValue("[\"java.time.Month\",\"11\"]", TemporalAccessor.class);
        assertEquals(Month.DECEMBER, value);
    }

    @Test
    public void testFormatAnnotation_zeroBased() throws Exception
    {
        Wrapper output = readerForZeroBased()
                .forType(Wrapper.class)
                .readValue("{\"value\":\"11\"}");
        assertEquals(new Wrapper(Month.DECEMBER).value, output.value);
    }

    @Test
    public void testFormatAnnotation_oneBased() throws Exception
    {
        Wrapper output = readerForOneBased()
                .forType(Wrapper.class)
                .readValue("{\"value\":\"11\"}");
        assertEquals(new Wrapper(Month.NOVEMBER).value, output.value);
    }

    /*
    /**********************************************************
    /* Tests for empty string handling
    /**********************************************************
     */

    @Test
    public void testDeserializeFromEmptyString() throws Exception
    {
        final ObjectMapper mapper = newMapper();

        // Nulls are handled in general way, not by deserializer so they are ok
        Month m = mapper.readerFor(Month.class).readValue(" null ");
        assertNull(m);

        // But coercion from empty String not enabled for Enums by default:
        try {
            mapper.readerFor(Month.class).readValue("\"\"");
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot coerce empty String");
        }
        // But can allow coercion of empty String to, say, null
        ObjectMapper emptyStringMapper = mapperBuilder()
                .withCoercionConfig(Month.class,
                        h -> h.setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull))
                .build();
        m = emptyStringMapper.readerFor(Month.class).readValue("\"\"");
        assertNull(m);
    }

    private ObjectReader readerForZeroBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule()
                        .disable(JavaTimeFeature.ONE_BASED_MONTHS))
                .build()
                .readerFor(Month.class);
    }

    private ObjectReader readerForOneBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule().enable(JavaTimeFeature.ONE_BASED_MONTHS))
                .build()
                .readerFor(Month.class);
    }
}
