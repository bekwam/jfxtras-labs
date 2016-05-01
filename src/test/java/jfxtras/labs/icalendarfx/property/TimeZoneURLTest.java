package jfxtras.labs.icalendarfx.property;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Test;

import jfxtras.labs.icalendarfx.properties.component.timezone.TimeZoneURL;

public class TimeZoneURLTest
{
    @Test
    public void canParseTimeZoneURL() throws URISyntaxException
    {
        String content = "TZURL:http://timezones.example.org/tz/America-Los_Angeles.ics";
        TimeZoneURL property = TimeZoneURL.parse(content);
        String madeContentLine = property.toContentLines();
        assertEquals(content, madeContentLine);
        assertEquals(new URI("http://timezones.example.org/tz/America-Los_Angeles.ics"), property.getValue());
    }
}
