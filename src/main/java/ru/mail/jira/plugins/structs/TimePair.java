package ru.mail.jira.plugins.structs;

import java.sql.Timestamp;

/**
 * This structure contains information about "arrival time" and time when
 *   user must not be registered.
 * 
 * @author Andrey Markelov
 */
public class TimePair
{
    /**
     * Cancel registration time.
     */
    private Timestamp cTime;

    /**
     * Arrival time.
     */
    private Timestamp timeOn;

    /**
     * Default constructor.
     */
    public TimePair() {}

    /**
     * Constructor.
     */
    public TimePair(Timestamp cTime, Timestamp timeOn)
    {
        this.cTime = cTime;
        this.timeOn = timeOn;
    }

    public Timestamp getcTime()
    {
        return cTime;
    }

    public Timestamp getTimeOn()
    {
        return timeOn;
    }

    public boolean isCTime()
    {
        return cTime != null;
    }

    public boolean isTimeOn()
    {
        return timeOn != null;
    }

    public void setcTime(Timestamp cTime)
    {
        this.cTime = cTime;
    }

    public void setTimeOn(Timestamp timeOn)
    {
        this.timeOn = timeOn;
    }

    @Override
    public String toString()
    {
        return "TimePair[cTime=" + cTime + ", timeOn=" + timeOn + "]";
    }
}
