package ru.mail.jira.plugins;

import java.util.Comparator;
import java.util.Map;

public class UserMapComparator
    implements Comparator
{
    private Map  _data = null;

    public UserMapComparator (Map data)
    {
        super();
        _data = data;
    }

    public int compare(Object o1, Object o2)
    {
        String e1 = (String) _data.get(o1);
        String e2 = (String) _data.get(o2);
        return e1.compareToIgnoreCase(e2);
    }
}
