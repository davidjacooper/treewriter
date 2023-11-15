package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.*;

public class TreePrintStream extends PrefixingPrintStream
{
    private int preNodeLines = 1;

    private String parentLine = "\u2502";
    private String connector = "\u251c\u2500\u2500 ";
    private String lastConnector = "\u2514\u2500\u2500 ";

    private boolean uninitialisedPadding = true;
    private String paddingPrefix = null;
    private String lastPaddingPrefix = null;


    public TreePrintStream(OutputStream out, Charset charset)
    {
        super(out, charset);
    }

    public TreePrintStream(OutputStream out)
    {
        super(out);
    }

    public TreePrintStream preNodeLines(int preNodeLines)
    {
        this.preNodeLines = preNodeLines;
        return this;
    }

    public TreePrintStream asciiLines()
    {
        parentLine = "|";
        connector = "+-- ";
        lastConnector = "\\-- ";
        paddingPrefix = "|   ";
        lastPaddingPrefix = "    ";
        uninitialisedPadding = false;
        return this;
    }

    public TreePrintStream parentLineCh(String parentLine)
    {
        this.parentLine = parentLine;
        uninitialisedPadding = true;
        return this;
    }

    public TreePrintStream connector(String connector)
    {
        this.connector = connector;
        uninitialisedPadding = true;
        return this;
    }

    public TreePrintStream lastConnector(String lastConnector)
    {
        this.lastConnector = lastConnector;
        uninitialisedPadding = true;
        return this;
    }

    private void initPadding()
    {
        if(uninitialisedPadding)
        {
            uninitialisedPadding = false;
            int connLen = connector.length();
            paddingPrefix = parentLine + " ".repeat(Math.max(0, connLen - parentLine.length()));
            lastPaddingPrefix = " ".repeat(connLen);
        }
    }

    public void startNode(boolean lastSibling) //throws IOException
    {
        initPadding();
        if(preNodeLines == 0)
        {
            addPrefix(lastSibling ? lastConnector : connector);
            replacePrefixAfterLine(lastSibling ? lastPaddingPrefix : paddingPrefix);
        }
        else
        {
            addPrefix(paddingPrefix);
            for(int i = 0; i < preNodeLines; i++)
            {
                write('\n');
            }
            replacePrefix(lastSibling ? lastConnector : connector);
            replacePrefixAfterLine(lastSibling ? lastPaddingPrefix : paddingPrefix);
        }
    }

    public void endNode()
    {
        removePrefix();
    }

    public <N> void printTree(N node,
                              Function<? super N,Collection<? extends N>> childFn,
                              Function<? super N,String> nodeToString)
    {
        forTree(node, childFn, n -> println(nodeToString.apply(n)));

        // println(nodeToString.apply(node));
        //
        // var children = childFn.apply(node);
        // int i = children.size();
        //
        // for(var child : children)
        // {
        //     startNode(i == 1);
        //     printTree(child, childFn, nodeToString);
        //     endNode();
        //     i--;
        // }
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
