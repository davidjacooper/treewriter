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
 * tree to be displayed. However, it may not always be necessary to create such a data structure.
 * The requirements for using {@code TreeWriter} are only that:
 * <ol>
 *   <li>Node data must be supplied in depth-first order; and</li>
 *   <li>It must be known in advance whether a given node is the last among its siblings or
 *       not.</li>
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
 * some consistent type or supertype</em>, and you just need to supply the logic for retrieving a
 * {@code Collection} of child nodes from any given node, and for displaying a given node.
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

    /**
     * Creates an instance that writes to an existing {@link PrefixingWriter} instance.
     * ({@code TreeWriter} needs a {@code PrefixingWriter} writer in <em>all</em> cases. This
     * constructor uses one provided by the client code. The other constructors all create one.)
     *
     * @param out The prefixing writer that will underpin the tree writer.
     */
    public TreeWriter(PrefixingWriter out)
    {
        super(out);
        this.prefixOut = out;
    }

    /**
     * Creates an instance that writes to an existing {@link Writer}. An intermediate
     * {@link PrefixingWriter} will be created as well.
     *
     * @param out The writer that will receive the output.
     */
    public TreeWriter(Writer out)
    {
        super(new PrefixingWriter(out));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    /**
     * Creates an instance that writes to an {@link OutputStream}, with a specific character set. An
     * intermediate {@link PrefixingWriter} will be created as well.
     *
     * @param out The output stream that will receive the output.
     * @param charset The character set for converting {@code char}s to {@code byte}s.
     */
    public TreeWriter(OutputStream out, Charset charset)
    {
        super(new PrefixingWriter(out, charset));
        this.prefixOut = (PrefixingWriter) this.out;
    }

    /**
     * Creates an instance that writes to an {@link OutputStream}, using the default character set.
     * An intermediate {@link PrefixingWriter} will be created as well.
     *
     * @param out The output stream that will receive the output.
     */
    public TreeWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    /**
     * Creates an instance that writes to {@link System#out}, using the default character set. An
     * intermediate {@link PrefixingWriter} will be created as well.
     */
    public TreeWriter()
    {
        this(System.out);
    }

    /**
     * Sets the {@link NodeOptions} used to format nodes by default (when no other
     * {@code NodeOptions} object is in effect).
     *
     * @param newOptions The new set of node options, to replace the current ones.
     */
    public void setOptions(NodeOptions newOptions)
    {
        this.stdOptions = newOptions;
    }

    /**
     * Sets the maximum length of lines, in characters, before they are wrapped (by setting
     * underlying {@code PrefixingWriter} accordingly). This includes both the tree-drawing
     * characters and the node text.
     *
     * Call {@link isWrapLengthAuto} first, if needed, to determine whether the existing wrap
     * length was able to be automatically determined.
     *
     * @param wrapLength The new maximum line length.
     */
    public void setWrapLength(int wrapLength)
    {
        prefixOut.setWrapLength(wrapLength);
    }

    /**
     * Retrieves the existing default node options.
     * @return The current node options.
     */
    public NodeOptions getOptions()
    {
        return stdOptions;
    }

    /**
     * Reports whether the current wrap length was automatically determined from the terminal
     * environment (based on the underlying {@code PrefixingWriter}).
     *
     * @return {@code true} if the wrap length was automatically determined, or {@code false} if it
     * is either a default value, or was manually specified.
     */
    public boolean isWrapLengthAuto()
    {
        return prefixOut.isWrapLengthAuto();
    }

    /**
     * Retrieves the existing maximum length of a line, in characters, <em>including</em> the
     * tree-drawing characters, before the line is wrapped (based on the underlying
     * {@code PrefixingWriter}).
     *
     * <p>This is a fixed value unless/until it is altered by calling {@link setWrapLength}.
     *
     * @return The maximum line length.
     */
    public int getWrapLength()
    {
        return prefixOut.getWrapLength();
    }

    /**
     * Retrieves the number of characters written so far to the current line, including tree-drawing
     * characters at the beginning, if any (based on the underlying {@code PrefixingWriter}).
     *
     * <p>This value changes each time any text is written. Note that a value of 0 is somewhat
     * special, as any further text written at this point will cause an increase <em>not just</em>
     * by the length of the text, but also by a number of tree-drawing characters, needing to be
     * output first.
     *
     * @return The current line length.
     */
    public int getProgressiveLineLength()
    {
        return prefixOut.getProgressiveLineLength();
    }

    /**
     * Retrieves the maximum allocation of characters to the current line, including any already
     * written, but <em>excluding</em> any tree-drawing characters (based on the underlying
     * {@code PrefixingWriter}). In other words, the "line space" is the number of characters able
     * to fit in between the tree-drawing characters on the left, and the wrapping limit on the
     * right.
     *
     * <p>This value will reduce when descending to a new tree level, when more characters in each
     * line are set aside to draw the tree structure. The value will increase again when ascending
     * back up the tree.
     *
     * @return The current line space.
     */
    public int getLineSpace()
    {
        return prefixOut.getLineSpace();
    }

    /**
     * Retrieves the number of <em>additional</em> characters that could be written to the current
     * line before it wraps (based on the underlying {@code PrefixingWriter}).
     *
     * @return The number of characters able to be written to the current line without wrapping.
     */
    public int getRemainingLineSpace()
    {
        return prefixOut.getRemainingLineSpace();
    }

    /**
     * Creates a new node. Any further text written (e.g., via {@code println} or {@code printf}),
     * <em>prior</em> to the next call to {@code startNode()} or {@link endNode}, will form the
     * node's content.
     *
     * <p>This call really creates a node <em>context</em>, which lasts until a matching call to
     * {@link endNode}. Any further pairs of {@code startNode}/{@code endNode} calls in-between
     * will create child nodes underneath this node.
     *
     * <p>The node is formatted according to the tree's standard formatting options (as set via
     * {@link getOptions} or {@link setOptions}), <em>or</em>, if applicable, by flow-on effects
     * from a previous call to {@link startNode(boolean,NodeOptions)}, where an explicitly-provided
     * {@code NodeOptions} instance itself specified further options for child or sibling nodes.
     *
     * @param lastSibling {@code true} if this is the final child of a given parent node, or
     *   {@code false} if there will be more sibling nodes to come. (Specifying the wrong value here
     *   will result in an extraneous or disconnected line.)
     */
    public void startNode(boolean lastSibling)
    {
        var opts = stdOptions;
        if(depth < nextOptions.length && nextOptions[depth] != null)
        {
            opts = nextOptions[depth];
        }
        startNode(lastSibling, opts);
    }

    /**
     * Creates a new node and specifies formatting options for it explicitly. This works like
     * {@link startNode(boolean)}, except that the formatting options here are provided directly by
     * the client code (overriding both the defaults, and any flow-on effects from previous calls to
     * this method).
     *
     * <p>In the simplest case, this method can cause a single node to be formatted differently to
     * the rest of the tree. In more complex cases, the provided {@link NodeOptions} instance can
     * specify further options to be applied to its child and sibling nodes, but only when
     * {@link startNode(boolean)} (without explicit {@code NodeOptions}) is subsequently called.
     *
     * @param lastSibling {@code true} if this is the final child of a given parent node, or
     *   {@code false} if there will be more sibling nodes to come. (Specifying the wrong value here
     *   will result in an extraneous or disconnected line.)
     * @param nodeOptions The options to be used to format this new node.
     */
    public void startNode(boolean lastSibling, NodeOptions nodeOptions)
    {
        depth++;
        try
        {
            if(prefixOut.getProgressiveLineLength() > 0)
            {
                prefixOut.write('\n');
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
                    prefixOut.write('\n');
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

        var nextSiblingOptions = nodeOptions.getNextSiblingOptions();
        var firstChildOptions = nodeOptions.getFirstChildOptions();
        if((nextSiblingOptions != null || firstChildOptions != null) && depth >= nextOptions.length)
        {
            nextOptions = Arrays.copyOf(nextOptions, depth * 2);
        }
        nextOptions[depth - 1] = nextSiblingOptions;
        nextOptions[depth] = firstChildOptions;
    }

    /**
     * Convenience method for starting a "label node", intended to annotate the tree rather being a
     * "real" tree node. Label nodes lack a connector line joining them to the parent.
     */
    public void startLabelNode()
    {
        startNode(false, stdOptions.copy().asLabel());
    }

    /**
     * Convenience method for starting a "pre-label node", intended to annotate a subsequent node.
     * Pre-label nodes lack the normal connector to the parent, but cause the node below to have a
     * "top connector" line, joining it to the pre-label.
     */
    public void startPreLabelNode()
    {
        startNode(false, stdOptions.copy().asPreLabel());
    }

    /**
     * Ends a node context previously started with either {@link startNode(boolean)},
     * {@link startNode(boolean,NodeOptions)}, or the more specialised {@link startLabelNode} or
     * {@link startPreLabelNode}.
     */
    public void endNode()
    {
        if(depth == 0)
        {
            throw new IllegalStateException("No node to end");
        }
        prefixOut.removePrefix();
        if(depth < nextOptions.length)
        {
            // Ensure that subsequent subtrees don't inherit the current (ending) subtree's
            // options.
            nextOptions[depth] = null;
        }
        depth--;
    }

    /**
     * Convenience method for displaying a label node (intended to annotate the tree rather than
     * being a "real" tree node) in one step.
     * @param s The text of the label to display.
     */
    public void printLabel(String s)
    {
        startLabelNode();
        println(s);
        endNode();
    }

    /**
     * Convenience method for displaying a pre-label node (intended to annotate a subsequent node)
     * in one step.
     * @param s The text of the pre-label node.
     */
    public void printPreLabel(String s)
    {
        startPreLabelNode();
        println(s);
        endNode();
    }

    /**
     * Convenience method for displaying a label node (intended to annotate the tree rather than
     * being a "real" tree node), using {@link PrintWriter#printf} semantics.
     * @param format A format string.
     * @param args Arguments referenced by the format specifiers in the format string.
     */
    public void printfLabel(String format, Object... args)
    {
        startLabelNode();
        printf(format, args);
        endNode();
    }

    /**
     * Convenience method for displaying a pre-label node (intended to annotate a subsequent node),
     * using {@link PrintWriter#printf} semantics.
     * @param format A format string.
     * @param args Arguments referenced by the format specifiers in the format string.
     */
    public void printfPreLabel(String format, Object... args)
    {
        startPreLabelNode();
        printf(format, args);
        endNode();
    }

    /**
     * Convenience method for printing an entire tree (or subtree), based on a data structure,
     * assuming each node can be represented by a simple string value. The client code is
     * responsible for implementing the data structure. It must simply provide functions for
     * traversing and converting nodes.
     *
     * @param <N> The type of the nodes in the tree. If the tree contains nodes of different types,
     *   {@code N} must be a common supertype.
     * @param node A reference to the root node in the tree (or subtree).
     * @param childFn A {@link Function} for taking a node object and retrieving a
     *   {@link Collection} of child node objects from it.
     * @param nodeToString A {@link Function} for obtaining the string representation for a given
     *   node.
     */
    public <N> void printTree(N node,
                              Function<? super N,Collection<? extends N>> childFn,
                              Function<? super N,String> nodeToString)
    {
        forTree(node, childFn, n -> print(nodeToString.apply(n)));
        println();
    }

    /**
     * Convenience method for printing an entire tree (or subtree), based on a data structure. The
     * client code is responsible for implementing the data structure. It must simply provide
     * functions for traversing and converting nodes.
     *
     * <p>Unlike {@link printTree printTree}, this method does not assume that each node can
     * (conveniently) be represented by a single string value. Rather, it permits the client code to
     * supply a "node printer" for each node, which can make arbitrary calls back to the
     * {@code TreeWriter}.
     *
     * @param <N> The type of the nodes in the tree. If the tree contains nodes of different types,
     *   {@code N} must be a common supertype.
     * @param node A reference to the root node in the tree (or subtree).
     * @param childFn A {@link Function} for taking a node object and retrieving a
     *   {@link Collection} of child node objects from it.
     * @param nodePrinter A {@link Consumer}, taking a node reference. It is assumed this will make
     *   appropriate calls back to the {@code TreeWriter} to display the node's content.
     */
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
