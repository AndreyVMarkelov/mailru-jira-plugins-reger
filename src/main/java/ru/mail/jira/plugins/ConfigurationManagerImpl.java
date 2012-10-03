package ru.mail.jira.plugins;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

/**
 * Implementation of <code>ru.mail.jira.plugins.ConfigurationManager</code>.
 * 
 * @author andrey
 */
public class ConfigurationManagerImpl
    implements ConfigurationManager
{
    private final PluginSettingsFactory pluginSettingsFactory;

    private final String PLUGIN_KEY = "regerConfig";

    private final static String DB_HOST_KEY = "DB_HOST_KEY";
    private final static String DB_PORT_KEY = "DB_PORT_KEY";
    private final static String DATABASE_KEY = "DATABASE_KEY";
    private final static String USERNAME_KEY = "USERNAME_KEY";
    private final static String PASSWORD_KEY = "PASSWORD_KEY";
    private final static String RECIPIENT_KEY = "RECIPIENT_KEY";
    private final static String REPORT_GROUP_KEY = "REPORT_GROUP_KEY";
    private final static String HR_REPORT_GROUP_KEY = "HR_REPORT_GROUP_KEY";

    public ConfigurationManagerImpl(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public String getDatabase()
    {
        return getStringProperty(DATABASE_KEY);
    }

    @Override
    public String getDbHost()
    {
        return getStringProperty(DB_HOST_KEY);
    }

    @Override
    public String getDbPort()
    {
        return getStringProperty(DB_PORT_KEY);
    }

    @Override
    public String getMailRecipient()
    {
        return getStringProperty(RECIPIENT_KEY);
    }

    @Override
    public String getPassword()
    {
        return getStringProperty(PASSWORD_KEY);
    }

    @Override
    public String[] getReportGroups()
    {
        String groups = getStringProperty(REPORT_GROUP_KEY);
        if (null == groups)
        {
            return null;
        }

        return groups.split("&");
    }

    private String getStringProperty(String key)
    {
        return (String) pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).get(key);
    }

    @Override
    public String getUsername()
    {
        return getStringProperty(USERNAME_KEY);
    }

    @Override
    public void setDatabase(String value)
    {
        setStringProperty(DATABASE_KEY, value);
    }

    @Override
    public void setDbHost(String value)
    {
        setStringProperty(DB_HOST_KEY, value);
    }

    @Override
    public void setDbPort(String value)
    {
        setStringProperty(DB_PORT_KEY, value);
    }

    @Override
    public void setMailRecipient(String value)
    {
        setStringProperty(RECIPIENT_KEY, value);
    }

    @Override
    public void setPassword(String value)
    {
        setStringProperty(PASSWORD_KEY, value);
    }

    @Override
    public void setReportGroups(String[] value)
    {
        if (value == null)
        {
            return;
        }

        StringBuilder result = new StringBuilder();
        for (String item : value)
        {
            result.append(item).append("&");
        }
        setStringProperty(REPORT_GROUP_KEY, result.toString());
    }

    private void setStringProperty(String key, String value)
    {
        pluginSettingsFactory.createSettingsForKey(PLUGIN_KEY).put(key, value);
    }

    @Override
    public void setUsername(String value)
    {
        setStringProperty(USERNAME_KEY, value);
    }

    @Override
    public String[] getHrReportGroups()
    {
        String groups = getStringProperty(HR_REPORT_GROUP_KEY);
        if (null == groups)
        {
            return null;
        }

        return groups.split("&");
    }

    @Override
    public void setHrReportGroups(String[] value)
    {
        if (value == null)
        {
            return;
        }

        StringBuilder result = new StringBuilder();
        for (String item : value)
        {
            result.append(item).append("&");
        }
        setStringProperty(HR_REPORT_GROUP_KEY, result.toString());
    }
}
