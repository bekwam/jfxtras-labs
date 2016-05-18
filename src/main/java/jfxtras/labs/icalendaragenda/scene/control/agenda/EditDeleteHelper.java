package jfxtras.labs.icalendaragenda.scene.control.agenda;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javafx.util.Callback;
import jfxtras.labs.icalendaragenda.scene.control.agenda.ICalendarAgenda.StartEndRange;
import jfxtras.labs.icalendarfx.components.VComponent;
import jfxtras.labs.icalendarfx.components.VComponentLocatable;
import jfxtras.labs.icalendarfx.components.VEvent;
import jfxtras.labs.icalendarfx.components.VTodo;
import jfxtras.labs.icalendarfx.properties.PropertyType;
import jfxtras.labs.icalendarfx.properties.component.recurrence.RecurrenceRuleNew;
import jfxtras.labs.icalendarfx.utilities.DateTimeUtilities;
import jfxtras.labs.icalendarfx.utilities.DateTimeUtilities.DateTimeType;
import jfxtras.scene.control.agenda.Agenda.Appointment;

/**
 * Handles edit and delete functionality of VEvents and VTodos
 * 
 * @author David Bal
 *
 */
public class EditDeleteHelper<R>
{   
    private Collection<R> recurrences;
    private LocalDateTime startRange;
    private LocalDateTime endRange;
//    private List<AppointmentGroup> appointmentGroups;
//    private 
    private Callback2<VComponentLocatable<?>, Temporal, R> recurrenceCallBack;
    
    public EditDeleteHelper(
            Collection<R> recurrences,
            Callback2<VComponentLocatable<?>, Temporal, R>  recurrenceCallBack)
    {
        this.recurrences = recurrences;
        this.recurrenceCallBack = recurrenceCallBack;
    }
    
    
    /**
     * Makes appointments from VEVENT or VTODO for Agenda
     * Appointments are made between displayed range
     * 
     * @param vComponentEdited - calendar component
     * @return created appointments
     */
    public List<R> makeRecurrences(VComponentLocatable<?> vComponentEdited)
    {
        List<R> newRecurrences = new ArrayList<>();
        Boolean isWholeDay = vComponentEdited.getDateTimeStart().getValue() instanceof LocalDate;
        
        // Make start and end ranges in Temporal type that matches DTSTART
//        LocalDateTime startRange = getDateTimeRange().getStartLocalDateTime();
//        LocalDateTime endRange = getDateTimeRange().getEndLocalDateTime();
        final Temporal startRange2;
        final Temporal endRange2;
        if (isWholeDay)
        {
            startRange2 = LocalDate.from(startRange);
            endRange2 = LocalDate.from(endRange);            
        } else
        {
            startRange2 = vComponentEdited.getDateTimeStart().getValue().with(startRange);
            endRange2 = vComponentEdited.getDateTimeStart().getValue().with(endRange);            
        }
        vComponentEdited.streamRecurrences(startRange2, endRange2)
            .forEach(startTemporal -> 
            {
                R recurrence = recurrenceCallBack.call(vComponentEdited, startTemporal);
                newRecurrences.add(recurrence);
            });   // add appointments
        return newRecurrences;
    }
    
    /** Edit VEvent or VTodo */
    public <T> boolean handleEdit(
            VComponentLocatable<T> vComponentEdited
          , VComponentLocatable<T> vComponentOriginal
          , Collection<VComponentLocatable<T>> vComponents
          , Temporal startOriginalRecurrence
          , Temporal startRecurrence
          , Temporal endRecurrence
//          , Collection<Object> allRecurrences
//          , Collection<Object> componentRecurrences
          , Callback<Map<ChangeDialogOption, StartEndRange>, ChangeDialogOption> dialogCallback)
    {
        validateStartRecurrenceAndDTStart(vComponentEdited, startOriginalRecurrence, startRecurrence);
        final EditDeleteHelper.RRuleStatus rruleType = RRuleStatus.getRRuleType(vComponentEdited.getRecurrenceRule(), vComponentOriginal.getRecurrenceRule());
        System.out.println("rruleType:" + rruleType);
        boolean incrementSequence = true;
        Collection<R> newRecurrences = null;
        Collection<R> allRecurrences = recurrences;
        switch (rruleType)
        {
        case HAD_REPEAT_BECOMING_INDIVIDUAL:
            vComponentEdited.becomeNonRecurring(vComponentOriginal, startRecurrence, endRecurrence);
            // fall through
        case WITH_NEW_REPEAT: // no dialog
        case INDIVIDUAL:
            adjustDateTime(vComponentEdited, startOriginalRecurrence, startRecurrence, endRecurrence);
            if (! vComponentEdited.equals(vComponentOriginal))
            {
                newRecurrences = updateRecurrences(vComponentEdited);
            }
            break;
        case WITH_EXISTING_REPEAT:
            // Find which properties changed
            List<PropertyType> changedProperties = findChangedProperties(
                    vComponentEdited,
                    vComponentOriginal,
                    startOriginalRecurrence,
                    startRecurrence,
                    endRecurrence);
            /* Note:
             * time properties must be checked separately because changes are stored in startRecurrence and endRecurrence,
             * not the VComponents DTSTART and DTEND yet.  The changes to DTSTART and DTEND are made after the dialog
             * question is answered. */
//            changedProperties.addAll(changedStartAndEndDateTime(startOriginalRecurrence, startRecurrence, endRecurrence));
            // determine if any changed properties warrant dialog
//            changedPropertyNames.stream().forEach(a -> System.out.println("changed property:" + a));
            boolean provideDialog = requiresChangeDialog(changedProperties);
            if (changedProperties.size() > 0) // if changes occurred
            {
                List<VComponentLocatable<T>> relatedVComponents = Arrays.asList(vComponentEdited); // TODO - support related components
                final ChangeDialogOption changeResponse;
                if (provideDialog)
                {
                    Map<ChangeDialogOption, StartEndRange> choices = ChangeDialogOption.makeDialogChoices(vComponentEdited, startOriginalRecurrence);
                    changeResponse = dialogCallback.call(choices);
                } else
                {
                    changeResponse = ChangeDialogOption.ALL;
                }
                switch (changeResponse)
                {
                case ALL:
                    if (relatedVComponents.size() == 1)
                    {
                        adjustDateTime(vComponentEdited, startOriginalRecurrence, startRecurrence, endRecurrence);
//                        if (vComponentEdited.childComponentsWithRecurrenceIDs().size() > 0)
//                        {
                        // Adjust children components with RecurrenceIDs
                        vComponentEdited.childComponentsWithRecurrenceIDs()
                                .stream()
//                                .map(c -> c.getRecurrenceId())
                                .forEach(v ->
                                {
                                    Temporal newRecurreneId = adjustRecurrenceStart(v.getRecurrenceId().getValue(), startOriginalRecurrence, startRecurrence, endRecurrence);
                                    v.setRecurrenceId(newRecurreneId);
                                });
//                        }
                        newRecurrences = updateRecurrences(allRecurrences);
                    } else
                    {
                        throw new RuntimeException("Only 1 relatedVComponents currently supported");
                    }
                    break;
                case CANCEL:
                    vComponentOriginal.copyTo(this); // return to original
                    return false;
                case THIS_AND_FUTURE:
                    newRecurrences = editThisAndFuture(vComponentOriginal, vComponents, startOriginalRecurrence, startRecurrence, endRecurrence, allRecurrences);
                    break;
                case ONE:
                    newRecurrences = editOne(vComponentOriginal, vComponents, startOriginalRecurrence, startRecurrence, endRecurrence, allRecurrences);
                    break;
                default:
                    break;
                }
            }
        }
        if (! isValid()) throw new RuntimeException(errorString());
        if (incrementSequence) { incrementSequence(); }
        if (newRecurrences != null)
        {
            allRecurrences.clear();
            allRecurrences.addAll(newRecurrences);
        }
        return true;
    }
    
    /** If startRecurrence isn't valid due to a RRULE change, change startRecurrence and
     * endRecurrence to closest valid values
     */
    // TODO - VERITFY THIS WORKS - changed from old version
    private static void validateStartRecurrenceAndDTStart(VComponentLocatable<?> vComponentEdited, Temporal startOriginalRecurrence, Temporal startRecurrence)
    {
//        boolean isStreamedValue;
        if (vComponentEdited.getRecurrenceRule() != null)
        {
            Temporal firstTemporal = vComponentEdited.getRecurrenceRule().getValue()
                    .streamRecurrences(vComponentEdited.getDateTimeStart().getValue())
                    .findFirst()
                    .get();
            if (! firstTemporal.equals(vComponentEdited.getDateTimeStart().getValue()))
            {
                vComponentEdited.setDateTimeStart(firstTemporal);
            }
        }
    }
    
    /* Adjust DTSTART and DTEND, DUE, or DURATION by recurrence's start and end date-time */
    private static void adjustDateTime(
            VComponentLocatable<?> vComponentEdited,
            Temporal startOriginalRecurrence,
            Temporal startRecurrence,
            Temporal endRecurrence)
    {
        Temporal newStart = adjustRecurrenceStart(
                vComponentEdited.getDateTimeStart().getValue(),
                startOriginalRecurrence,
                startRecurrence,
                endRecurrence);
        vComponentEdited.setDateTimeStart(newStart);
        System.out.println("new DTSTART:" + newStart);
        vComponentEdited.setEndOrDuration(startRecurrence, endRecurrence);
//        endType().setDuration(this, startRecurrence, endRecurrence);
    }

    /* Adjust DTSTART of RECURRENCE-ID */
    private static Temporal adjustRecurrenceStart(Temporal initialStart
            , Temporal startOriginalRecurrence
            , Temporal startRecurrence
            , Temporal endRecurrence)
    {
        DateTimeType newDateTimeType = DateTimeType.of(startRecurrence);
        ZoneId zone = (startRecurrence instanceof ZonedDateTime) ? ZoneId.from(startRecurrence) : null;
        Temporal startAdjusted = newDateTimeType.from(initialStart, zone);
        Temporal startOriginalRecurrenceAdjusted = newDateTimeType.from(startOriginalRecurrence, zone);

        // Calculate shift from startAdjusted to make new DTSTART
        final TemporalAmount startShift;
        if (newDateTimeType == DateTimeType.DATE)
        {
            startShift = Period.between(LocalDate.from(startOriginalRecurrence), LocalDate.from(startRecurrence));
        } else
        {
            startShift = Duration.between(startOriginalRecurrenceAdjusted, startRecurrence);
        }
        return startAdjusted.plus(startShift);
    }
    
    private Collection<Object> updateRecurrences(VComponentLocatable<?> vComponentEdited)
    {
        Collection<? extends Object> recurrences = appointments();
        Collection<Appointment> componentRecurrences = vComponentAppointmentMap.get(vComponentEdited);
        Collection<Object> instancesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
        instancesTemp.addAll(recurrences);
        instancesTemp.removeIf(a -> componentRecurrences.stream().anyMatch(a2 -> a2 == a));
//        instances().clear(); // clear VEvent of outdated appointments
        instancesTemp.addAll(makeRecurrences(vComponentEdited)); // make new recurrences and add to main collection (added to VEvent's collection in makeAppointments)
        return instancesTemp;
    }
    
    /**
     * Generates a list of iCalendar property names that have different values from the 
     * input parameter
     * 
     * equal checks are encapsulated inside the enum VComponentProperty
     */
    public static List<PropertyType> findChangedProperties(
            VComponentLocatable<?> vComponentEdited,
            VComponentLocatable<?> vComponentOriginal,
            Temporal startOriginalInstance,
            Temporal startInstance,
            Temporal endInstance)
    {
        List<PropertyType> changedProperties = new ArrayList<>();
        vComponentEdited.properties()
                .stream()
                .map(p -> p.propertyType())
                .forEach(t ->
                {
                    Object p1 = t.getProperty(vComponentEdited);
                    Object p2 = t.getProperty(vComponentOriginal);
                    if (! p1.equals(p2))
                    {
                        changedProperties.add(t);
                    }
                });
        
        /* Note:
         * time properties must be checked separately because changes are stored in startRecurrence and endRecurrence,
         * not the VComponents DTSTART and DTEND yet.  The changes to DTSTART and DTEND are made after the dialog
         * question is answered. */
        if (! startOriginalInstance.equals(startInstance))
        {
            changedProperties.add(PropertyType.DATE_TIME_START);
        }
        
        TemporalAmount durationNew = DateTimeUtilities.temporalAmountBetween(startInstance, endInstance);
        TemporalAmount durationOriginal = vComponentEdited.getActualDuration();
        if (! durationOriginal.equals(durationNew))
        {
            if (vComponentEdited instanceof VEvent)
            {
                if (! (((VEvent) vComponentEdited).getDateTimeEnd() == null))
                {
                    changedProperties.add(PropertyType.DATE_TIME_END);                    
                }
            } else if (vComponentEdited instanceof VTodo)
            {
                if (! (((VTodo) vComponentEdited).getDateTimeDue() == null))
                {
                    changedProperties.add(PropertyType.DATE_TIME_DUE);                    
                }                
            }
            boolean isDurationNull = vComponentEdited.getDuration() == null;
            if (! isDurationNull)
            {
                changedProperties.add(PropertyType.DURATION);                    
            }
        }   
        
        return changedProperties;
    }

   static Collection<?> updateRecurrences(VComponentLocatable<?> vComponentEdited, Collection<?> recurrences)
   {
       Collection<Object> recurrencesTemp = new ArrayList<>(); // use temp array to avoid unnecessary firing of Agenda change listener attached to appointments
       recurrencesTemp.addAll(recurrences);
       recurrencesTemp.removeIf(a -> updateInstances().stream().anyMatch(a2 -> a2 == a));
       recurrences().clear(); // clear VEvent of outdated appointments
       recurrencesTemp.addAll(makeRecurrences(vComponentEdited)); // make new appointments and add to main collection (added to VEvent's collection in makeAppointments)
       return recurrencesTemp;
   }

   // TODO - DOUBLE CHECK THIS LIST - WHY NO DESCRIPTION, FOR EXAMPLE?
   private final static List<PropertyType> DIALOG_REQUIRED_PROPERTIES = Arrays.asList(
           PropertyType.CATEGORIES,
           PropertyType.COMMENT,
           PropertyType.DATE_TIME_START,
           PropertyType.ORGANIZER,
           PropertyType.SUMMARY
           );
   
   /**
    * Return true if ANY changed property requires a dialog, false otherwise
    * 
    * @param changedPropertyNames - list from {@link #findChangedProperties(VComponent)}
    * @return
    */
   boolean requiresChangeDialog(List<PropertyType> changedPropertyNames)
   {
       return changedPropertyNames.stream()
               .map(p -> DIALOG_REQUIRED_PROPERTIES.contains(p))
               .anyMatch(b -> b == true);
   }
   
//   /**
//    * Options available when editing or deleting a repeatable appointment.
//    * Sometimes all options are not available.  For example, a one-part repeating
//    * event doesn't have the SEGMENT option.
//    */
//   public enum ChangeDialogOption
//   {
//       ONE                  // individual instance
//     , ALL                  // entire series
//     , THIS_AND_FUTURE      // selected instance and all in the future
//     , CANCEL;              // do nothing
//       
//       public static Map<ChangeDialogOption, StartEndRange> makeDialogChoices(VComponentLocatable<?> vComponent, Temporal startInstance)
//       {
//           Map<ChangeDialogOption, StartEndRange> choices = new LinkedHashMap<>();
//           choices.put(ChangeDialogOption.ONE, new StartEndRange(startInstance, startInstance));
//           Temporal end = vComponent.lastRecurrence();
//           if (! vComponent.isIndividual())
//           {
//               if (! vComponent.isLastRecurrence(startInstance))
//               {
//                   Temporal start = (startInstance == null) ? vComponent.getDateTimeStart() : startInstance; // set initial start
//                   choices.put(ChangeDialogOption.THIS_AND_FUTURE, new StartEndRange(start, end));
//               }
//               choices.put(ChangeDialogOption.ALL, new StartEndRange(vComponent.getDateTimeStart(), end));
//           }
//           return choices;
//       }        
//   }
   
//    @Deprecated // TODO - consider replace with simple booleans
     enum RRuleStatus
    {
        INDIVIDUAL ,
        WITH_EXISTING_REPEAT ,
        WITH_NEW_REPEAT, 
        HAD_REPEAT_BECOMING_INDIVIDUAL;
      
        public static RRuleStatus getRRuleType(RecurrenceRuleNew rruleNew, RecurrenceRuleNew rruleOld)
        {
            if (rruleNew == null)
            {
                if (rruleOld == null)
                { // doesn't have repeat or have old repeat either
                    return RRuleStatus.INDIVIDUAL;
                } else {
                    return RRuleStatus.HAD_REPEAT_BECOMING_INDIVIDUAL;
                }
            } else
            { // RRule != null
                if (rruleOld == null)
                {
                    return RRuleStatus.WITH_NEW_REPEAT;                
                } else
                {
                    return RRuleStatus.WITH_EXISTING_REPEAT;
                }
            }
        }
    }

     /**
      * Options available when editing or deleting a repeatable appointment.
      * Sometimes all options are not available.  For example, a one-part repeating
      * event doesn't have the SEGMENT option.
      */
     public enum ChangeDialogOption
     {
         ONE                  // individual instance
       , ALL                  // entire series
       , THIS_AND_FUTURE      // selected instance and all in the future
       , CANCEL;              // do nothing
         
         public static Map<ChangeDialogOption, StartEndRange> makeDialogChoices(VComponentLocatable<?> vComponent, Temporal startInstance)
         {
             Map<ChangeDialogOption, StartEndRange> choices = new LinkedHashMap<>();
             choices.put(ChangeDialogOption.ONE, new StartEndRange(startInstance, startInstance));
             Temporal end = vComponent.lastRecurrence();
             if (! (vComponent.getRecurrenceRule() == null))
             {
                 if (! vComponent.lastRecurrence().equals(startInstance))
                 {
                     Temporal start = (startInstance == null) ? vComponent.getDateTimeStart().getValue() : startInstance; // set initial start
                     choices.put(ChangeDialogOption.THIS_AND_FUTURE, new StartEndRange(start, end));
                 }
                 choices.put(ChangeDialogOption.ALL, new StartEndRange(vComponent.getDateTimeStart().getValue(), end));
             }
             return choices;
         }        
     }
     
     /** Based on {@link Callback<P,R>} */
     @FunctionalInterface
     public interface Callback2<P1, P2, R> {
         /**
          * The <code>call</code> method is called when required, and is given 
          * two arguments of type P1 and P2, with a requirement that an object of type R
          * is returned.
          *
          * @param param1 The first argument upon which the returned value should be
          *      determined.
          * @param param1 The second argument upon which the returned value should be
          *      determined.
          * @return An object of type R that may be determined based on the provided
          *      parameter value.
          */
         public R call(P1 param1, P2 param2);
     }
}
