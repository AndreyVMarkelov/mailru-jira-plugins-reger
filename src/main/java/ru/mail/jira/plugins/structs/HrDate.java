package ru.mail.jira.plugins.structs;

import java.util.Date;

/**
 * This structure keeps information about HR register data.
 * 
 * @author Andrey Markelov
 */
public class HrDate
{
    private Date hrDate;

    private int month;

    private int year;

    public Date getHrDate()
    {
        return hrDate;
    }

    public int getMonth()
    {
        return month;
    }

    public int getYear()
    {
        return year;
    }

    public void setHrDate(Date hrDate)
    {
        this.hrDate = hrDate;
    }

    public void setMonth(int month)
    {
        this.month = month;
    }

    public void setYear(int year)
    {
        this.year = year;
    }

    @Override
    public String toString()
    {
        return "HrDate(year=" + year + ", month=" + month + ", hrDate=" + hrDate + ")";
    }
}
