package ru.mail.jira.plugins;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import ru.mail.jira.plugins.structs.ArrivalUserStruct;
import ru.mail.jira.plugins.structs.DateCaclStruct;

public class TimeCalculator
{
    /**
     * Start date.
     */
    private Calendar startDate;

    /**
     * End date.
     */
    private Calendar endDate;

    /**
     * Calendar range.
     */
    private Map<Date, Boolean> calRange;

    /**
     * Date list.
     */
    private List<Date> dateList;

    /**
     * Date formatter.
     */
    private SimpleDateFormat sdf;

    /**
     * Time formatter.
     */
    private SimpleDateFormat sdt;

    /**
     * Constructor.
     */
    public TimeCalculator(
        Date startDate,
        Date endDate,
        Map<Date, Boolean> calRange,
        List<Date> dateList)
    {
        this.startDate = Calendar.getInstance();
        this.endDate = Calendar.getInstance();
        this.startDate.setTime(startDate);
        this.endDate.setTime(endDate);
        this.calRange = calRange;
        this.dateList = dateList;
        this.sdf = new SimpleDateFormat("yyyy-MM-dd");
        this.sdt = new SimpleDateFormat("HH:mm");
    }

    /**
     * Calculate time.
     */
    public DateCaclStruct calculate()
    {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate.getTime());
        Calendar cal2 = Calendar.getInstance();
        if (endDate.compareTo(cal2) <= 0)
        {
            cal2.setTime(endDate.getTime());
        }

        long days = 0;
        long minutes = 0;
        long delays = 0;

        while (cal1.compareTo(cal2) <= 0)
        {
            Date rangeDate = isDateInMap(calRange, cal1.getTime());
            if (rangeDate != null)
            {
                if (!calRange.get(rangeDate))
                {
                    cal1.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                }
            }
            else
            {
                int dayOfWeek = cal1.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
                {
                    cal1.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                }
            }

            Date date;
            if ((date = isDateInArray(dateList, cal1.getTime())) != null)
            {
                Calendar currDate = Calendar.getInstance();
                currDate.setTime(date);

                int hours = currDate.get(Calendar.HOUR_OF_DAY) - 10;
                if (hours >= 0)
                {
                    int missedTime = hours * 60;
                    int mins = currDate.get(Calendar.MINUTE);
                    if (mins > 0)
                    {
                        missedTime += mins;
                    }

                    minutes += missedTime;
                    delays++;
                }
            }
            else
            {
                days++;
            }
            cal1.add(Calendar.DAY_OF_MONTH, 1);
        }

        return new DateCaclStruct(days, minutes, delays);
    }

    public List<Map<String, String>> caclArrivals()
    {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(startDate.getTime());
        Calendar cal2 = Calendar.getInstance();
        if (endDate.compareTo(cal2) <= 0)
        {
            cal2.setTime(endDate.getTime());
        }

        List<Map<String, String>> sgen = new ArrayList<Map<String, String>>();

        while (cal1.compareTo(cal2) <= 0)
        {
            Date rangeDate = isDateInMap(calRange, cal1.getTime());
            if (rangeDate != null)
            {
                if (!calRange.get(rangeDate))
                {
                    ArrivalUserStruct aus = new ArrivalUserStruct(0, "#f0fff0", "", sdf.format(cal1.getTime()));
                    sgen.add(aus.toMap());
                    cal1.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                }
            }
            else
            {
                int dayOfWeek = cal1.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY)
                {
                    ArrivalUserStruct aus = new ArrivalUserStruct(0, "#f0fff0", "", sdf.format(cal1.getTime()));
                    sgen.add(aus.toMap());
                    cal1.add(Calendar.DAY_OF_MONTH, 1);
                    continue;
                }
            }

            Date date;
            if ((date = isDateInArray(dateList, cal1.getTime())) != null)
            {
                Calendar currDate = Calendar.getInstance();
                currDate.setTime(date);

                int hours = currDate.get(Calendar.HOUR_OF_DAY) - 10;
                if (hours >= 0)
                {
                    long missedTime = hours * 60;
                    int mins = currDate.get(Calendar.MINUTE);
                    if (mins > 0)
                    {
                        missedTime += mins;
                    }

                    ArrivalUserStruct aus = new ArrivalUserStruct(missedTime, "#ffffff", sdt.format(date), sdf.format(cal1.getTime()));
                    if (missedTime > 0)
                    {
                        aus.setDaytype("#FFFFF0");
                    }
                    sgen.add(aus.toMap());
                }
                else
                {
                    ArrivalUserStruct aus = new ArrivalUserStruct(0, "#ffffff", sdt.format(date), sdf.format(cal1.getTime()));
                    sgen.add(aus.toMap());
                }
            }
            else
            {
                ArrivalUserStruct aus = new ArrivalUserStruct(0, "#fff5ee", " - ", sdf.format(cal1.getTime()));
                sgen.add(aus.toMap());
            }

            cal1.add(Calendar.DAY_OF_MONTH, 1);
        }

        return sgen;
    }

    private Date isDateInArray(List<Date> arrs, Date date)
    {
        for (Date d : arrs)
        {
            if (d.getYear() == date.getYear() && d.getMonth() == date.getMonth() && d.getDate() == date.getDate())
            {
                return d;
            }
        }

        return null;
    }

    private Date isDateInMap(Map<Date, ?> map, Date date)
    {
        for (Map.Entry<Date, ?> d : map.entrySet())
        {
            if (d.getKey().getYear() == date.getYear() && d.getKey().getMonth() == date.getMonth() && d.getKey().getDate() == date.getDate())
            {
                return d.getKey();
            }
        }

        return null;
    }
}
