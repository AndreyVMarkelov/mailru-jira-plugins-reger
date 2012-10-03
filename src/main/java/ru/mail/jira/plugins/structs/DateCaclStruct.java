package ru.mail.jira.plugins.structs;

import java.util.HashMap;
import java.util.Map;

public class DateCaclStruct
{
    private long days;

    private long delays;

    private long minutes;

    public DateCaclStruct(long days, long minutes, long delays)
    {
        this.days = days;
        this.minutes = minutes;
        this.delays = delays;
    }

    public long getDays()
    {
        return days;
    }

    public long getDelays()
    {
        return delays;
    }

    public long getMinutes()
    {
        return minutes;
    }

    public Map<String, String> toMap()
    {
        Map<String, String> map = new HashMap<String, String>();
        map.put("minutes", Long.toString(minutes));
        map.put("days", Long.toString(days));
        map.put("delays", Long.toString(delays));

        return map;
    }

    @Override
    public String toString()
    {
        return "DateCaclStruct(days=" + days + ", minutes=" + minutes + ", delays=" + delays + ")";
    }
}
