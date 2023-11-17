package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.*;

public class TreeOptions
{
    private int preNodeLines = 1;

    private String parentLine = "\u2502";
    private String connector = "\u251c\u2500\u2500 ";
    private String lastConnector = "\u2514\u2500\u2500 ";
    private String paddingPrefix = null;
    private String lastPaddingPrefix = null;


    public TreeOptions preNodeLines(int preNodeLines)
    {
        this.preNodeLines = preNodeLines;
        return this;
    }

    public TreeOptions asciiLines()
    {
        parentLine = "|";
        connector = "+-- ";
        lastConnector = "\\-- ";
        paddingPrefix = "|   ";
        lastPaddingPrefix = "    ";
        return this;
    }

    public TreeOptions parentLine(String parentLine)
    {
        this.parentLine = parentLine;
        paddingPrefix = null;
        return this;
    }

    public TreeOptions connector(String connector)
    {
        this.connector = connector;
        paddingPrefix = null;
        return this;
    }

    public TreeOptions lastConnector(String lastConnector)
    {
        this.lastConnector = lastConnector;
        lastPaddingPrefix = null;
        return this;
    }

    public int getPreNodeLines()     { return preNodeLines; }
    public String getParentLine()    { return parentLine; }
    public String getConnector()     { return connector; }
    public String getLastConnector() { return lastConnector; }

    public String getPaddingPrefix()
    {
        if(paddingPrefix == null)
        {
            paddingPrefix = parentLine + " ".repeat(Math.max(0, connector.length() - parentLine.length()));
        }
        return paddingPrefix;
    }

    public String getLastPaddingPrefix()
    {
        if(lastPaddingPrefix == null)
        {
            lastPaddingPrefix = " ".repeat(lastConnector.length());
        }
        return lastPaddingPrefix;
    }
}
