package ru.mail.jira.plugins.structs;

import java.util.HashMap;
import java.util.Map;
import ru.mail.jira.plugins.ArrivalUtils;

public class ArrivalUserStruct
{
    /**
     * Arrival time.
     */
    private String arrival;

    /**
     * Day type.
     */
    private String daytype;

    /**
     * Date.
     */
    private String eachdata;

    /**
     * Missed time.
     */
    private long missedTime;

    /**
     * Constructor.
     */
    public ArrivalUserStruct(long missedTime, String daytype, String arrival, String eachdata)
    {
        this.missedTime = missedTime;
        this.daytype = daytype;
        this.arrival = arrival;
        this.eachdata = eachdata;
    }

    public String getArrival()
    {
        return arrival;
    }

    public String getDaytype()
    {
        return daytype;
    }

    public String getEachdata()
    {
        return eachdata;
    }

    public long getMissedTime()
    {
        return missedTime;
    }

    public void setArrival(String arrival)
    {
        this.arrival = arrival;
    }

    public void setDaytype(String daytype)
    {
        this.daytype = daytype;
    }

    public void setEachdata(String eachdata)
    {
        this.eachdata = eachdata;
    }

    public void setMissedTime(long missedTime)
    {
        this.missedTime = missedTime;
    }

    public Map<String, String> toMap()
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("arrival", arrival);
        map.put("daytype", daytype);
        map.put("missedtime", ArrivalUtils.convertMissedMinutes(missedTime));
        map.put("eachdata", eachdata);

        return map;
    }

    @Override
    public String toString()
    {
        return "ArrivalUserStruct(missedTime=" + missedTime + ", daytype="
            + daytype + ", arrival=" + arrival + ", eachdata=" + eachdata + ")";
    }
}
