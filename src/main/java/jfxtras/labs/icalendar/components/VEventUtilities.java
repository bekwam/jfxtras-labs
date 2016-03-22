package jfxtras.labs.icalendar.components;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jfxtras.labs.icalendar.components.VEventOld.EndType;
import jfxtras.labs.icalendar.properties.component.descriptive.Description;
import jfxtras.labs.icalendar.utilities.DateTimeUtilities;
import jfxtras.labs.icalendar.utilities.ICalendarUtilities;

public final class VEventUtilities
{
    private VEventUtilities() { }
    /**
     * Tests equality between two VEvent objects.  Treats v1 as expected.  Produces a JUnit-like
     * output if objects are not equal.
     * 
     * @param v1 - expected VEvent
     * @param v2 - actual VEvent
     * @param verbose - true = display list of unequal properties, false no display output
     * @return - equality result
     */
    public static <T> boolean isEqualTo(VEventOld<?,?> v1, VEventOld<?,?> v2, boolean verbose)
    {
        boolean vComponentResult = VComponentUtilities.isEqualTo(v1, v2, verbose);
        List<String> changedProperties = new ArrayList<>();
        Arrays.stream(VEventProperty.values())
        .forEach(p -> 
        {
            if (! (p.isPropertyEqual(v1, v2)))
            {
                changedProperties.add(p.toString() + " not equal:" + p.getPropertyValue(v1) + " " + p.getPropertyValue(v2));
            }
        });

        if (changedProperties.size() == 0)
        {
            return vComponentResult;
        } else
        {
            if (verbose)
            {
            System.out.println(changedProperties.stream().collect(Collectors.joining(System.lineSeparator())));
            }
            return false;
        }
    }
    
    /**
     * Parses the property-value pair to the matching property, if a match is found.
     * If no matching property, does nothing.
     * 
     * @param vEvent - object to add property values
     * @param propertyValuePair - property name-value pair (e.g. DTSTART and TZID=America/Los_Angeles:20160214T110000)
     * @return - true if property found and set, false otherwise
     */
//    public static boolean parse(VEvent<?,?> vEvent, Pair<String, String> propertyValuePair)
    public static boolean parse(VEventOld<?,?> vEvent, String propertyLine)
    {
        String propertyName = ICalendarUtilities.getPropertyName(propertyLine);
        System.out.println("parsing property:" + propertyName);
        VEventProperty vEventProperty = VEventProperty.propertyFromName(propertyName);
        if (vEventProperty != null)
        {
            vEventProperty.parseAndSetProperty(vEvent, propertyLine);
            return true;
        }
        return false;
    }
    /**
     * VEvent specific properties with the following data and methods:
     * iCalendar property name
     * setVComponent - parse string method
     * makeContentLine - toString method
     * isPropertyEqual - tests equality for the property between to VEvents
     * 
     * @author David Bal
     *
     */
    public enum VEventProperty
    {
        /**
         * DESCRIPTION: RFC 5545 iCalendar 3.8.1.12. page 84
         * This property provides a more complete description of the
         * calendar component than that provided by the "SUMMARY" property.
         * Example:
         * DESCRIPTION:Meeting to provide technical review for "Phoenix"
         *  design.\nHappy Face Conference Room. Phoenix design team
         *  MUST attend this meeting.\nRSVP to team leader.
         */
        DESCRIPTION ("DESCRIPTION", true)
        {
            @Override
            public void parseAndSetProperty(VEventOld<?,?> vEvent, String propertyLine)
            {
                if (vEvent.getDescription() == null)
                {
                    vEvent.setDescription(new Description(propertyLine));
                } else
                {
                    throw new IllegalArgumentException(toString() + " can only appear once in calendar component");                    
                }
            }
    
            @Override
            public Object getPropertyValue(VEventOld<?,?> vEvent)
            {
                return vEvent.getDescription();
            }
    
            @Override
            public String toPropertyString(VEventOld<?,?> vEvent)
            {
                return ((vEvent.getDescription() == null) || (vEvent.getDescription().getValue().isEmpty())) ? null : vEvent.getDescription().toContentLine();
            }
    
            @Override
            public boolean isPropertyEqual(VEventOld<?,?> v1, VEventOld<?,?> v2)
            {
                return (v1.getDescription() == null) ? (v2.getDescription() == null) : v1.getDescription().equals(v2.getDescription());
            }
    
            @Override
            public void copyProperty(VEventOld<?,?> source, VEventOld<?,?> destination)
            {
                destination.setDescription(source.getDescription());
            }
        } 
        /** 
         * DURATION from RFC 5545 iCalendar 3.8.2.5 page 99, 3.3.6 page 34
         * Can't be used if DTEND is used.  Must be one or the other.
         * */
      , DURATION ("DURATION", true)
        {
            @Override
            public void parseAndSetProperty(VEventOld<?,?> vEvent, String propertyLine)
            {
                if (vEvent.getDuration() == null)
                {
                    if (vEvent.getDateTimeEnd() == null)
                    {
                        vEvent.endPriority = EndType.DURATION;
                        String durationString = ICalendarUtilities.propertyLineToParameterMap(propertyLine)
                                .get(ICalendarUtilities.PROPERTY_VALUE_KEY);
                        vEvent.setDuration(Duration.parse(durationString));
                    } else
                    {
                        throw new IllegalArgumentException("Invalid VEvent: Can't contain both DTEND and DURATION.");
                    }
                } else
                {
                    throw new IllegalArgumentException(toString() + " can only appear once in calendar component");
                }
            }
    
            @Override
            public Object getPropertyValue(VEventOld<?,?> vEvent)
            {
                return vEvent.getDuration();
            }
            
            @Override
            public String toPropertyString(VEventOld<?,?> vEvent)
            {
                if (vEvent.getDuration() == null)
                {
                    return null;
                } else if (vEvent.endPriority == EndType.DURATION)
                {
                    return toString() + ":" + vEvent.getDuration();
                } else
                {
                    throw new RuntimeException("DURATION and EndPriority don't match");                
                }
            }
    
            @Override
            public boolean isPropertyEqual(VEventOld<?,?> v1, VEventOld<?,?> v2)
            {
                return (v1.getDuration() == null) ? (v2.getDuration() == null) : v1.getDuration().equals(v2.getDuration());
            }
    
            @Override
            public void copyProperty(VEventOld<?,?> source, VEventOld<?,?> destination)
            {
                destination.setDuration(source.getDuration());
            }
        } 
      /**
       * DTEND, Date-Time End. from RFC 5545 iCalendar 3.8.2.2 page 95
       * Specifies the date and time that a calendar component ends.
       * Can't be used if DURATION is used.  Must be one or the other.
       * Must be same Temporal type as dateTimeStart (DTSTART)
       */
      , DATE_TIME_END ("DTEND", true)
        {
            @Override
            public void parseAndSetProperty(VEventOld<?,?> vEvent, String propertyLine)
            {
                if (vEvent.getDateTimeEnd() == null)
                {
                    if (vEvent.getDuration() == null)
                    {
                        vEvent.endPriority = EndType.DTEND;
                        System.out.println("dtend string:" + propertyLine);
//                        ICalendarUtilities.propertyLineToParameterMap(propertyLine);

                        Temporal dateTime = DateTimeUtilities.parse(propertyLine);
                        vEvent.setDateTimeEnd(dateTime);
                    } else
                    {
                        throw new IllegalArgumentException("Invalid VEvent: Can't contain both DTEND and DURATION.");
                    }
                } else
                {
                    throw new IllegalArgumentException(toString() + " can only appear once in calendar component");                
                }
            }
    
            @Override
            public Object getPropertyValue(VEventOld<?,?> vEvent)
            {
                return vEvent.getDateTimeEnd();
            }
    
            @Override
            public String toPropertyString(VEventOld<?,?> vEvent)
            {
                if (vEvent.getDateTimeEnd() == null)
                {
                    return null;
                } else if (vEvent.endPriority == EndType.DTEND)
                {
//                    String tag = DateTimeUtilities.dateTimePropertyTag(toString(), vEvent.getDateTimeEnd());
                    return toString() + ":" + DateTimeUtilities.format(vEvent.getDateTimeEnd());
                } else
                {
                    throw new RuntimeException("DTEND and EndPriority don't match");
                }
            }
    
            @Override
            public boolean isPropertyEqual(VEventOld<?,?> v1, VEventOld<?,?> v2)
            {
                return (v1.getDateTimeEnd() == null) ? (v2.getDateTimeEnd() == null) : v1.getDateTimeEnd().equals(v2.getDateTimeEnd());
            }
    
            @Override
            public void copyProperty(VEventOld<?,?> source, VEventOld<?,?> destination)
            {
                destination.setDateTimeEnd(source.getDateTimeEnd());
            }
        }
      /**
       * LOCATION: RFC 5545 iCalendar 3.8.1.12. page 87
       * This property defines the intended venue for the activity
       * defined by a calendar component.
       * Example:
       * LOCATION:Conference Room - F123\, Bldg. 002
       */
      , LOCATION ("LOCATION", true)
        {
            @Override
            public void parseAndSetProperty(VEventOld<?,?> vEvent, String propertyLine)
            {
                vEvent.setLocation(propertyLine);
            }
    
            @Override
            public Object getPropertyValue(VEventOld<?,?> vEvent)
            {
                return vEvent.getLocation();
            }
            
            @Override
            public String toPropertyString(VEventOld<?,?> vEvent)
            {
                return ((vEvent.getLocation() == null) || (vEvent.getLocation().isEmpty())) ? null : toString()
                        + ":" + vEvent.getLocation();
            }
    
            @Override
            public boolean isPropertyEqual(VEventOld<?,?> v1, VEventOld<?,?> v2)
            {
                return (v1.getLocation() == null) ? (v2.getLocation() == null) : v1.getLocation().equals(v2.getLocation());
            }
    
            @Override
            public void copyProperty(VEventOld<?,?> source, VEventOld<?,?> destination)
            {
                destination.setLocation(source.getLocation());
            }
        };
    
        // Map to match up string tag to ICalendarProperty enum
        private static Map<String, VEventProperty> propertyFromTagMap = makePropertiesFromNameMap();
        private static Map<String, VEventProperty> makePropertiesFromNameMap()
        {
            Map<String, VEventProperty> map = new HashMap<>();
            VEventProperty[] values = VEventProperty.values();
            for (int i=0; i<values.length; i++)
            {
                map.put(values[i].toString(), values[i]);
            }
            return map;
        }
        /** get VEventProperty enum from property name */
        public static VEventProperty propertyFromName(String propertyName)
        {
            return propertyFromTagMap.get(propertyName.toUpperCase());
        }
        
        private String name;
        /* indicates if providing a dialog to allow user to confirm edit is required. 
         * False means no confirmation is required or property is only modified by the implementation, not by the user */
        boolean dialogRequired;
        
        VEventProperty(String name, boolean dialogRequired)
        {
            this.name = name;
            this.dialogRequired = dialogRequired;
        }
        
        @Override
        public String toString() { return name; }
        public boolean isDialogRequired() { return dialogRequired; }
        
        /*
         * ABSTRACT METHODS
         */
        
        /** sets VEvent's property for this VEventProperty to parameter value
         * value is a string that is parsed if necessary to the appropriate type
         */
        public abstract void parseAndSetProperty(VEventOld<?,?> vEvent, String propertyLine);
    
        /** gets VEvent's property value for this VEventProperty */
        public abstract Object getPropertyValue(VEventOld<?,?> vEvent);
        
        /** makes content line (RFC 5545 3.1) from a VEvent property  */
        public abstract String toPropertyString(VEventOld<?,?> vEvent);
        
        /** Checks is corresponding property is equal between v1 and v2 */
        public abstract boolean isPropertyEqual(VEventOld<?,?> v1, VEventOld<?,?> v2);
        
        /** Copies property value from source to destination */
        public abstract void copyProperty(VEventOld<?,?> source, VEventOld<?,?> destination);
        
    }
}