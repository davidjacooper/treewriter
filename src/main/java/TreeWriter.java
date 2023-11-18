package au.djac.treewriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.*;

public class TreeWriter extends PrintWriter
{
    private NodeOptions stdOptions = new NodeOptions();
    private NodeOptions nextOptions = null;
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

    public NodeOptions options()
    {
        return stdOptions;
    }

    public void startNode(boolean lastSibling)
    {
        NodeOptions opts = stdOptions;
        if(nextOptions != null)
        {
            opts = nextOptions;
            nextOptions = nextOptions.getNext();
        }
        startNode(lastSibling, opts);
    }

    public void startNode(boolean lastSibling, NodeOptions nodeOptions)
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
                connector = nodeOptions.getLastConnector();
                padding = nodeOptions.getLastPaddingPrefix();
            }
            else
            {
                connector = nodeOptions.getConnector();
                padding = nodeOptions.getPaddingPrefix();
            }

            int topMargin = nodeOptions.getTopMargin();
            int topConnectorLength = nodeOptions.getTopConnectorLength();
            if(topMargin == 0 && topConnectorLength == 0)
            {
                prefixOut.addPrefix(connector);
                prefixOut.replacePrefixAfterLine(padding);
            }
            else
            {
                prefixOut.addPrefix(nodeOptions.getPaddingPrefix());
                for(int i = 0; i < topMargin; i++)
                {
                    prefixOut.lineBreak();
                }

                var topConnector = nodeOptions.getTopConnector();
                for(int i = 0; i < topConnectorLength; i++)
                {
                    println(topConnector);
                }
                prefixOut.replacePrefix(connector);
                prefixOut.replacePrefixAfterLine(padding);
            }
        }
        catch(IOException e)
        {
            setError();
        }
        nextOptions = nodeOptions.getNext();
    }

    public void endNode()
    {
        prefixOut.removePrefix();
    }

    public void startLabelNode()
    {
        startNode(false, stdOptions.copy().asLabel());
    }

    public void startPreLabelNode()
    {
        startNode(false, stdOptions.copy().asPreLabel());
    }

    public void printLabel(String s)
    {
        startLabelNode();
        println(s);
        endNode();
    }

    public void printPreLabel(String s)
    {
        startPreLabelNode();
        println(s);
        endNode();
    }

    public void printfLabel(String format, Object... args)
    {
        startLabelNode();
        printf(format, args);
        endNode();
    }

    public void printfPreLabel(String format, Object... args)
    {
        startPreLabelNode();
        printf(format, args);
        endNode();
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
