package tools.jackson.datatype.jsr310.ser;

import static org.junit.Assert.assertEquals;

import java.time.Month;

import org.junit.Test;

import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import tools.jackson.datatype.jsr310.JavaTimeFeature;
import tools.jackson.datatype.jsr310.JavaTimeModule;
import tools.jackson.datatype.jsr310.ModuleTestBase;

public class OneBasedMonthSerTest extends ModuleTestBase
{
    static class Wrapper {
        public Month month;

        public Wrapper(Month m) { month = m; }
        public Wrapper() { }
    }

    @Test
    public void testSerializationFromEnum() throws Exception
    {
        assertEquals( "\"JANUARY\"" , writerForOneBased()
            .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
        assertEquals( "\"JANUARY\"" , writerForZeroBased()
            .with(SerializationFeature.WRITE_ENUMS_USING_TO_STRING)
            .writeValueAsString(Month.JANUARY));
    }

    @Test
    public void testSerializationFromEnumWithPattern_oneBased() throws Exception
    {
        ObjectWriter w = writerForOneBased().with(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":1}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
    }

    @Test
    public void testSerializationFromEnumWithPattern_zeroBased() throws Exception
    {
        ObjectWriter w = writerForZeroBased().with(SerializationFeature.WRITE_ENUMS_USING_INDEX);
        assertEquals( "{\"month\":0}" , w.writeValueAsString(new Wrapper(Month.JANUARY)));
    }


    private ObjectWriter writerForZeroBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule().disable(JavaTimeFeature.ONE_BASED_MONTHS))
                .build()
                .writer();
    }

    private ObjectWriter writerForOneBased() {
        return JsonMapper.builder()
                .addModule(new JavaTimeModule().enable(JavaTimeFeature.ONE_BASED_MONTHS))
                .build()
                .writer();
    }

}
