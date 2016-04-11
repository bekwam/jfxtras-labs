package jfxtras.labs.icalendar.properties.component.relationship.recurrenceid;

import java.time.LocalDateTime;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jfxtras.labs.icalendar.parameters.ParameterEnum;
import jfxtras.labs.icalendar.parameters.Range;
import jfxtras.labs.icalendar.parameters.Range.RangeType;
import jfxtras.labs.icalendar.properties.PropertyBaseUTC;
import jfxtras.labs.icalendar.properties.PropertyRecurrenceID;

public class RecurrenceIDLocalDateTime extends PropertyBaseUTC<RecurrenceIDLocalDateTime> implements PropertyRecurrenceID<LocalDateTime>
{
    /**
     * RANGE
     * Recurrence Identifier Range
     * RFC 5545, 3.2.13, page 23
     * 
     * To specify the effective range of recurrence instances from
     *  the instance specified by the recurrence identifier specified by
     *  the property.
     * 
     * Example:
     * RECURRENCE-ID;RANGE=THISANDFUTURE:19980401T133000Z
     * 
     * @author David Bal
     *
     */
    @Override
    public Range getRange() { return (range == null) ? null : range.get(); }
    @Override
    public ObjectProperty<Range> rangeProperty()
    {
        if (range == null)
        {
            range = new SimpleObjectProperty<>(this, ParameterEnum.RECURRENCE_IDENTIFIER_RANGE.toString());
        }
        return range;
    }
    private ObjectProperty<Range> range;
    @Override
    public void setRange(Range range)
    {
        if (range != null)
        {
            rangeProperty().set(range);
        }
    }
    public void setRange(String value) { setRange(new Range(value)); }
    public RecurrenceIDLocalDateTime withRange(Range altrep) { setRange(altrep); return this; }
    public RecurrenceIDLocalDateTime withRange(RangeType value) { setRange(new Range(value)); return this; }
    public RecurrenceIDLocalDateTime withRange(String content) { setRange(content); return this; }
    
    public RecurrenceIDLocalDateTime(LocalDateTime temporal)
    {
        // null as argument for string converter causes default converter from ValueType to be used
        super(temporal, null);
    }

    public RecurrenceIDLocalDateTime(CharSequence contentLine)
    {
        // null as argument for string converter causes default converter from ValueType to be used
        super(contentLine, null);
    }
    
    public RecurrenceIDLocalDateTime(RecurrenceIDLocalDateTime source)
    {
        super(source);
    }
}