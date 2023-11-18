package au.djac.treewriter;

public class NodeOptions
{
    private int topMargin = 0;
    private int topConnectorLength = 0;
    private String topConnector = "\u250a";
    private String parentLine = "\u2502";
    private String connector = "\u251c\u2500\u2500 ";
    private String lastConnector = "\u2514\u2500\u2500 ";
    private String paddingPrefix = null;
    private String lastPaddingPrefix = null;
    private NodeOptions next = null;

    public NodeOptions copy()
    {
        var newOpts = new NodeOptions();
        newOpts.topMargin = topMargin;
        newOpts.topConnectorLength = topConnectorLength;
        newOpts.topConnector = topConnector;
        newOpts.parentLine = parentLine;
        newOpts.connector = connector;
        newOpts.lastConnector = lastConnector;
        newOpts.paddingPrefix = paddingPrefix;
        newOpts.lastPaddingPrefix = lastPaddingPrefix;
        newOpts.next = next;
        return newOpts;
    }

    public NodeOptions topMargin(int topMargin)
    {
        this.topMargin = topMargin;
        return this;
    }

    public NodeOptions topConnectorLength(int topConnectorLength)
    {
        this.topConnectorLength = topConnectorLength;
        return this;
    }

    public NodeOptions topConnector(String topConnector)
    {
        this.topConnector = topConnector;
        return this;
    }

    public NodeOptions parentLine(String parentLine)
    {
        this.parentLine = parentLine;
        paddingPrefix = null;
        return this;
    }

    public NodeOptions connector(String connector)
    {
        this.connector = connector;
        paddingPrefix = null;
        return this;
    }

    public NodeOptions lastConnector(String lastConnector)
    {
        this.lastConnector = lastConnector;
        lastPaddingPrefix = null;
        return this;
    }

    public NodeOptions asciiLines()
    {
        topConnector = "|";
        parentLine = "|";
        connector = "+-- ";
        lastConnector = "\\-- ";
        paddingPrefix = "|   ";
        lastPaddingPrefix = "    ";
        return this;
    }

    public NodeOptions asLabel()
    {
        this.connector = getPaddingPrefix();
        return this;
    }

    public NodeOptions asPreLabel()
    {
        var n = next().topMargin(0).topConnectorLength(1);
        connector(getPaddingPrefix());
        return this;
    }

    public NodeOptions next()
    {
        if(next == null)
        {
            next = copy();
            //next.next = null;
        }
        return next;
    }

    public NodeOptions getNext()        { return next; }
    public int getTopMargin()           { return topMargin; }
    public int getTopConnectorLength()  { return topConnectorLength; }
    public String getTopConnector()     { return topConnector; }
    public String getParentLine()       { return parentLine; }
    public String getConnector()        { return connector; }
    public String getLastConnector()    { return lastConnector; }

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
