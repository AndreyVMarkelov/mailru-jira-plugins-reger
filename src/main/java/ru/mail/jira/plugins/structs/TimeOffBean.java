package ru.mail.jira.plugins.structs;

import java.util.Date;

/**
 * @author andrey
 */
public class TimeOffBean
{
    private String absent;

    private String comment;

    private Date delayDate;

    private String delayTime;

    private long id;

    private String reporter;

    private String user;

    /**
     * Default constructor.
     */
    public TimeOffBean() {}

    public TimeOffBean(
        long id,
        String reporter,
        String user,
        Date delayDate,
        String delayTime,
        String absent,
        String comment)
    {
        this.id = id;
        this.reporter = reporter;
        this.user = user;
        this.delayDate = delayDate;
        this.delayTime = delayTime;
        this.absent = absent;
        this.comment = comment;
    }

    public String getAbsent()
    {
        return absent;
    }

    public String getComment()
    {
        return comment;
    }

    public Date getDelayDate()
    {
        return delayDate;
    }

    public String getDelayTime()
    {
        return delayTime;
    }

    public long getId()
    {
        return id;
    }

    public String getReporter()
    {
        return reporter;
    }

    public String getUser()
    {
        return user;
    }

    public void setAbsent(String absent)
    {
        this.absent = absent;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public void setDelayDate(Date delayDate)
    {
        this.delayDate = delayDate;
    }

    public void setDelayTime(String delayTime)
    {
        this.delayTime = delayTime;
    }

    public void setId(long id)
    {
        this.id = id;
    }

    public void setReporter(String reporter)
    {
        this.reporter = reporter;
    }

    public void setUser(String user)
    {
        this.user = user;
    }

    @Override
    public String toString()
    {
        return "TimeOffBean[id=" + id + ", reporter=" + reporter + ", user="
            + user + ", delayDate=" + delayDate + ", delayTime="
            + delayTime + ", absent=" + absent + ", comment=" + comment + "]";
    }
}
