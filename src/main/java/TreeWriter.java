package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.*;

public class TreeWriter extends PrintWriter
{
    private TreeOptions options = new TreeOptions();
    private PrefixingWriter prefixOut;

    public TreeWriter(OutputStream out, Charset charset)
    {
        super(new PrefixingWriter(out, charset));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    public TreeWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    public TreeWriter(Writer out)
    {
        super(new PrefixingWriter(out));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    public TreeWriter(PrefixingWriter out)
    {
        super(out);
        this.prefixOut = out;
    }

    public TreeOptions options()
    {
        return options;
    }

    public void startNode(boolean lastSibling)
    {
        try
        {
            if(prefixOut.getLineLength() > 0)
            {
                prefixOut.lineBreak();
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
                prefixOut.addPrefix(connector);
                prefixOut.replacePrefixAfterLine(padding);
            }
            else
            {
                prefixOut.addPrefix(options.getParentLine());
                for(int i = 0; i < preNodeLines; i++)
                {
                    prefixOut.lineBreak();
                }
                prefixOut.replacePrefix(connector);
                prefixOut.replacePrefixAfterLine(padding);
            }
        }
        catch(IOException e)
        {
            setError();
        }
    }

    public void endNode()
    {
        prefixOut.removePrefix();
    }

    public <N> void printTree(N node,
                              Function<? super N,Collection<? extends N>> childFn,
                              Function<? super N,String> nodeToString)
    {
        forTree(node, childFn, n -> print(nodeToString.apply(n)));
        println();
    }

    public <N> void forTree(N node,
                            Function<? super N,Collection<? extends N>> childFn,
                            Consumer<? super N> nodePrinter)
    {
        nodePrinter.accept(node);

        var children = childFn.apply(node);
        int i = children.size();

        for(var child : children)
        {
            startNode(i == 1);
            forTree(child, childFn, nodePrinter);
            endNode();
            i--;
        }
    }
}
