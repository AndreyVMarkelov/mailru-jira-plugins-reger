package ru.mail.jira.plugins;

import net.jcip.annotations.Immutable;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * JAXB representation of a group of projects.
 */
@Immutable
@XmlRootElement
public class HTMLRepresentation
{
    @XmlElement
    private String html;

    public HTMLRepresentation(String html)
    {
        this.html = html;
    }

    @Override
    public String toString()
    {
        return "HTMLRepresentation(html=" + html + ")";
    }
}