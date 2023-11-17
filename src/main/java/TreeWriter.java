package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;

public class TreeWriter extends PrefixingWriter
{
    private TreeOptions options = new TreeOptions();

    public TreeWriter(OutputStream out, Charset charset)
    {
        super(out, charset);
    }

    public TreeWriter(OutputStream out)
    {
        super(out);
    }

    public TreeWriter(Writer out)
    {
        super(out);
    }

    public TreeOptions options()
    {
        return options;
    }

    public void startNode(boolean lastSibling) throws IOException
    {
        if(lineLength > 0)
        {
            lineBreak();
        }

        String connector, padding;
        if(lastSibling)
        {
            connector = options.getLastConnector();
            padding = options.getLastPaddingPrefix();
        }
        else
        {
            connector = options.getConnector();
            padding = options.getPaddingPrefix();
        }

        int preNodeLines = options.getPreNodeLines();
        if(preNodeLines == 0)
        {
            addPrefix(connector);
            replacePrefixAfterLine(padding);
        }
        else
        {
            addPrefix(options.getParentLine());
            for(int i = 0; i < preNodeLines; i++)
            {
                lineBreak();
            }
            replacePrefix(connector);
            replacePrefixAfterLine(padding);
        }
    }

    public void endNode()
    {
        removePrefix();
    }
}
