package jfxtras.labs.icalendar.properties;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import jfxtras.labs.icalendar.parameters.AlternateTextRepresentation;
import jfxtras.labs.icalendar.parameters.ICalendarParameter;
import jfxtras.labs.icalendar.properties.component.descriptive.Summary;
import jfxtras.labs.icalendar.properties.component.relationship.Contact;

/**
 * Property with language, alternate text display, and a text-based value
 *  
 * @param <T>
 * @see Comment
 * @see Contact
 * @see Description
 * @see Location
 * @see Resources
 * @see Summary
 */
public abstract class PropertyTextBase3<T> extends PropertyTextBase2<T>
{
    /**
     * ALTREP : Alternate Text Representation
     * To specify an alternate text representation for the property value.
     * 
     * Example:
     * DESCRIPTION;ALTREP="CID:part3.msg.970415T083000@example.com":
     *  Project XYZ Review Meeting will include the following agenda
     *   items: (a) Market Overview\, (b) Finances\, (c) Project Man
     *  agement
     *
     *The "ALTREP" property parameter value might point to a "text/html"
     *content portion.
     *
     * Content-Type:text/html
     * Content-Id:<part3.msg.970415T083000@example.com>
     *
     * <html>
     *   <head>
     *    <title></title>
     *   </head>
     *   <body>
     *     <p>
     *       <b>Project XYZ Review Meeting</b> will include
     *       the following agenda items:
     *       <ol>
     *         <li>Market Overview</li>
     *         <li>Finances</li>
     *         <li>Project Management</li>
     *       </ol>
     *     </p>
     *   </body>
     * </html>
     */
    public AlternateTextRepresentation getAlternateTextRepresentation() { return (alternateTextRepresentation == null) ? _alternateTextRepresentation : alternateTextRepresentation.get(); }
    public ObjectProperty<AlternateTextRepresentation> alternateTextRepresentationProperty()
    {
        if (alternateTextRepresentation == null)
        {
            alternateTextRepresentation = new SimpleObjectProperty<>(this, ICalendarParameter.ALTERNATE_TEXT_REPRESENTATION.toString(), _alternateTextRepresentation);
            alternateTextRepresentation.addListener((observable, oldValue, newValue) ->
            {
                if (newValue == null)
                {
                    parameters().remove(oldValue);
                } else
                {
                    parameters().add(newValue);
                }
            });
        }
        return alternateTextRepresentation;
    }
    private AlternateTextRepresentation _alternateTextRepresentation;
    private ObjectProperty<AlternateTextRepresentation> alternateTextRepresentation;
    public void setAlternateTextRepresentation(AlternateTextRepresentation alternateTextRepresentation)
    {
        if (this.alternateTextRepresentation == null)
        {
            _alternateTextRepresentation = alternateTextRepresentation;
        } else
        {
            this.alternateTextRepresentation.set(alternateTextRepresentation);
        }
    }
    public T withAlternateTextRepresentation(String content) { setAlternateTextRepresentation(new AlternateTextRepresentation(content)); return (T) this; }
    
    /*
     * CONSTRUCTORS
     */    
    protected PropertyTextBase3(String name, String propertyString)
    {
        super(name, propertyString);
    }

    // copy constructor
    public PropertyTextBase3(PropertyTextBase3<T> property)
    {
        super(property);
        if (getAlternateTextRepresentation() != null)
        {
            setAlternateTextRepresentation(property.getAlternateTextRepresentation());
        }
    }

    public PropertyTextBase3(String name)
    {
        super(name);
    }
}
