package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.function.*;

public class TreePrintWriter extends PrintWriter
{
    private TreeWriter treeOut;

    public TreePrintWriter(OutputStream out, Charset charset)
    {
        super(new TreeWriter(out, charset));
        this.treeOut = (TreeWriter) this.out;
    }

    public TreePrintWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    public TreePrintWriter(Writer out)
    {
        super(new TreeWriter(out));
        this.treeOut = (TreeWriter) this.out;
    }

    public TreePrintWriter(TreeWriter out)
    {
        super(out);
        this.treeOut = out;
    }

    public TreeOptions options()
    {
        return treeOut.options();
    }

    public void startNode(boolean lastSibling)
    {
        try
        {
            treeOut.startNode(lastSibling);
        }
        catch(IOException e)
        {
            setError();
        }
    }

    public void endNode()
    {
        treeOut.removePrefix();
    }

    public <N> void printTree(N node,
                              Function<? super N,Collection<? extends N>> childFn,
                              Function<? super N,String> nodeToString)
    {
        forTree(node, childFn, n -> print(nodeToString.apply(n)));
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
