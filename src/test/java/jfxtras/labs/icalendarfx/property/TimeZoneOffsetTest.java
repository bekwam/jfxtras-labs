package jfxtras.labs.icalendarfx.property;

import static org.junit.Assert.assertEquals;

import java.time.ZoneOffset;

import org.junit.Test;

import jfxtras.labs.icalendarfx.properties.component.timezone.TimeZoneOffsetFrom;
import jfxtras.labs.icalendarfx.properties.component.timezone.TimeZoneOffsetTo;

public class TimeZoneOffsetTest
{
    @Test
    public void canParseTimeZoneOffsetFrom()
    {
        String content = "TZOFFSETFROM:-0500";
        TimeZoneOffsetFrom madeProperty = TimeZoneOffsetFrom.parse(content);
        assertEquals(content, madeProperty.toContentLines());
        TimeZoneOffsetFrom expectedProperty = new TimeZoneOffsetFrom(ZoneOffset.of("-05:00"));
        assertEquals(expectedProperty, madeProperty);
    }
    
    @Test
    public void canParseTimeZoneOffsetTo()
    {
        String content = "TZOFFSETTO:+0000";
        TimeZoneOffsetTo madeProperty = TimeZoneOffsetTo.parse(content);
        assertEquals(content, madeProperty.toContentLines());
        TimeZoneOffsetTo expectedProperty = new TimeZoneOffsetTo(ZoneOffset.of("+00:00"));
        assertEquals(expectedProperty, madeProperty);
    }

}
