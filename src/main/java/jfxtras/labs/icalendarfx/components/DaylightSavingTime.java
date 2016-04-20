package jfxtras.labs.icalendarfx.components;

// both this class and Standard are identical - need to extend common class
public class DaylightSavingTime extends StandardOrSavingsBase<DaylightSavingTime>
{
    @Override
    public VComponentEnum componentType()
    {
        return VComponentEnum.DAYLIGHT;
    }
    
    /*
     * CONSTRUCTORS
     */
    public DaylightSavingTime() { }
    
    public DaylightSavingTime(String contentLines)
    {
        super(contentLines);
    }
}