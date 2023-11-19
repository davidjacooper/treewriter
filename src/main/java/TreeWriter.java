package au.djac.treewriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.function.*;

/**
 * Provides a text-based tree-drawing capability in the form of a specialised {@link PrintWriter}.
 * Trees are written progressively, in depth-first order, branching down and to the right.
 * Formatting can be customised via {@link getOptions getOptions}, {@link setOptions setOptions}
 * and the {@link NodeOptions} class.
 *
 * <p>In some client applications, a tree-based data structure will already exist to represent the
 * tree to be displayed. However, it may not always be necessary to create such a data structure,
 * and in general it will suffice that:
 * <ol>
 *   <li>Node data can be supplied in depth-first order; and</li>
 *   <li>It is known in advance whether a given node is the last among its siblings or not.</li>
 * </ol>
 *
 * <p>If this is the case, then client code can simply call {@link startNode(boolean)} to enter
 * a new node context, and {@link endNode() endNode} to exit it, with pairs of such calls occurring
 * within other pairs to represent nested tree nodes, for instance. (The boolean flag indicates
 * whether a given node will be the final sibling or not.)
 *
 * <p>Calls to any standard {@code PrintWriter} methods, like
 * {@link PrintWriter#println(String) println} or {@link PrintWriter#printf printf}, will supply the
 * text for the current node. This text may contain newlines and ANSI escape codes (specifically SGR
 * codes for altering colours). Text supplied this way will be wrapped as needed, while not
 * disturbing the tree representation, assuming that the correct line width can be determined
 * (automatically via JAnsi, or manually by calling {@link setWrapLength setWrapLength}).
 *
 * <p>Alternatively, if a tree structure is available, then you can call {@link printTree printTree}
 * or {@link forTree forTree}. Here, tree nodes must be represented as individual objects <em>of
 * some consistent type or supertype</em> (this package does not define what these may be), and you
 * just need to supply the logic for retrieving a {@code Collection} of child nodes from any given
 * node.
 *
 * <p>{@code TreeWriter} provides a higher-level interface over the top of {@link PrefixingWriter}.
 *
 * @author David Cooper
 */
public class TreeWriter extends PrintWriter
{
    private PrefixingWriter prefixOut;
    private NodeOptions stdOptions = new NodeOptions();
    private NodeOptions[] nextOptions = new NodeOptions[10];
    private int depth = 0;

    public TreeWriter(PrefixingWriter out)
    {
        super(out);
        this.prefixOut = out;
    }

    public TreeWriter(Writer out)
    {
        super(new PrefixingWriter(out));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    public TreeWriter(OutputStream out, Charset charset)
    {
        super(new PrefixingWriter(out, charset));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    public TreeWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    public TreeWriter()
    {
        this(System.out);
    }

    public void setOptions(NodeOptions newOptions)
    {
        this.stdOptions = newOptions;
    }

    public void setWrapLength(int wrapLength)
    {
        prefixOut.setWrapLength(wrapLength);
    }

    public NodeOptions getOptions()    { return stdOptions; }
    public int getWrapLength()         { return prefixOut.getWrapLength(); }
    public int getLineLength()         { return prefixOut.getLineLength(); }
    public int getTotalLineSpace()     { return prefixOut.getTotalLineSpace(); }
    public int getRemainingLineSpace() { return prefixOut.getRemainingLineSpace(); }

    public void startNode(boolean lastSibling)
    {
        var opts = stdOptions;
        // if(nextOptions != null)
        // {
        //     opts = nextOptions;
        //     nextOptions = nextOptions.getNext();
        // }
        if(depth < nextOptions.length && nextOptions[depth] != null)
        {
            opts = nextOptions[depth];
        }
        startNode(lastSibling, opts);
    }

    public void startNode(boolean lastSibling, NodeOptions nodeOptions)
    {
        depth++;
        try
        {
            if(prefixOut.getLineLength() > 0)
            {
                prefixOut.lineBreak();
            }

            String connector, padding;
            if(lastSibling)
            {
                connector = nodeOptions.getEndConnector();
                padding = nodeOptions.getEndPaddingPrefix();
            }
            else
            {
                connector = nodeOptions.getMidConnector();
                padding = nodeOptions.getMidPaddingPrefix();
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
                prefixOut.addPrefix(nodeOptions.getMidPaddingPrefix());
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

        //nextOptions = nodeOptions.getNext();
        var nextSiblingOptions = nodeOptions.getNextSiblingOptions();
        var firstChildOptions = nodeOptions.getFirstChildOptions();
        if((nextSiblingOptions != null || firstChildOptions != null) && depth >= nextOptions.length)
        {
            nextOptions = Arrays.copyOf(nextOptions, depth * 2);
        }
        nextOptions[depth - 1] = nextSiblingOptions;
        nextOptions[depth] = firstChildOptions;
    }

    public void endNode()
    {
        prefixOut.removePrefix();
        if(depth < nextOptions.length)
        {
            // Ensure that subsequent subtrees don't inherit the current (ending) subtree's
            // options.
            nextOptions[depth] = null;
        }
        depth--;
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
