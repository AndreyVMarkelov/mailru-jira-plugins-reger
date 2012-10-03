package ru.mail.jira.plugins;

/**
 * Configuration interface.
 * 
 * @author andrey
 */
public interface ConfigurationManager
{
    public String getDatabase();

    public String getDbHost();

    public String getDbPort();

    public String[] getHrReportGroups();

    public String getMailRecipient();

    public String getPassword();

    public String[] getReportGroups();

    public String getUsername();

    public void setDatabase(String value);

    public void setDbHost(String value);

    public void setDbPort(String value);

    public void setHrReportGroups(String[] value);

    public void setMailRecipient(String value);

    public void setPassword(String value);

    public void setReportGroups(String[] value);

    public void setUsername(String value);
}
