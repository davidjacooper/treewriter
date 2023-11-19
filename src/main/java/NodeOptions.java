package au.djac.treewriter;

import java.util.function.Consumer;

/**
 * A set of options that determine how {@link TreeWriter} draws tree nodes.
 */
public class NodeOptions
{
    private int topMargin = 0;
    private int topConnectorLength = 0;
    private String topConnector = "\u250a";
    private String parentLine = "\u2502";
    private String midConnector = "\u251c\u2500\u2500 ";
    private String endConnector = "\u2514\u2500\u2500 ";
    private String midPaddingPrefix = null;
    private String endPaddingPrefix = null;

    private NodeOptions nextSiblingOptions = null;
    private NodeOptions firstChildOptions = null;

    /**
     * Creates a separate copy of this set of node options.
     * @return A new {@code NodeOptions} instance with the same options as this one.
     */
    public NodeOptions copy()
    {
        var newOpts = new NodeOptions();
        newOpts.topMargin = topMargin;
        newOpts.topConnectorLength = topConnectorLength;
        newOpts.topConnector = topConnector;
        newOpts.parentLine = parentLine;
        newOpts.midConnector = midConnector;
        newOpts.endConnector = endConnector;
        newOpts.midPaddingPrefix = midPaddingPrefix;
        newOpts.endPaddingPrefix = endPaddingPrefix;
        newOpts.nextSiblingOptions = nextSiblingOptions;
        newOpts.firstChildOptions = firstChildOptions;
        return newOpts;
    }

    /**
     * Sets the "top margin" &ndash; the number of blank* lines preceding each node. (*The lines
     * will still contain the "prefix", comprising the line-drawing characters needed to connect
     * the various levels of the tree together.)
     *
     * <p>This is 0 by default, and would typically be 0 or 1.
     *
     * @param topMargin A new "top margin" value.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions topMargin(int topMargin)
    {
        this.topMargin = topMargin;
        return this;
    }

    /**
     * Sets the "top connector length" &ndash; the number of lines preceding a node that contain
     * the "top connector", a visual mechanism for connecting the node to a preceding node. This is
     * <em>not</em> the normal mechanism for drawing just a basic tree structure. Rather, it is
     * used (for instance) to draw "pre labels". See {@link TreeWriter#startPreLabelNode}.
     *
     * <p>This is 0 by default (no top connector), and would typically be 0 or 1. If non-zero values
     * for both {@link topMargin(int) topMargin} and {@code topConnectorLength} are given, the
     * margin precedes the top connector.
     *
     * @param topConnectorLength A new "top connector length".
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions topConnectorLength(int topConnectorLength)
    {
        this.topConnectorLength = topConnectorLength;
        return this;
    }

    /**
     * Sets the "top connector" string, to be printed before a node
     * ({@link topConnectorLength(int) topConnectorLength} times) to indicate that it's connected
     * to a preceding node. This is <em>not</em> the normal mechanism for drawing just a basic tree
     * structure. Rather, it is used (for instance) to draw "pre labels". See
     * {@link TreeWriter#startPreLabelNode}.
     *
     * <p>By default, this is "┊", the unicode "BOX DRAWINGS LIGHT QUADRUPLE DASH VERTICAL"
     * character, 0x250a.
     *
     * @param topConnector A new "top connector" string.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions topConnector(String topConnector)
    {
        this.topConnector = topConnector;
        return this;
    }

    /**
     * Sets the "parent line" string, normally a vertical line helping to connect a parent node to
     * various child nodes underneath it.
     *
     * <p>By default, this is "│", the unicode "BOX DRAWINGS LIGHT VERTICAL" character, 0x2502.
     *
     * @param parentLine A new "parent line" string.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions parentLine(String parentLine)
    {
        this.parentLine = parentLine;
        midPaddingPrefix = null;
        return this;
    }

    /**
     * Sets the "mid connector" string, representing the horizontal part of the line connecting
     * a parent node to each non-final child node, as well as the join between vertical and
     * horizontal lines. (The <em>final</em> child node, for any given parent, is shown using an
     * {@link endConnector(String) endConnector} instead.)
     *
     * <p>By default, the mid connector is "├── ", or 0x251c ("BOX DRAWINGS LIGHT VERTICAL AND
     * RIGHT"), then two times 0x2500 ("BOX DRAWINGS LIGHT HORIZONTAL"), then an ordinary space (4
     * characters total).
     *
     * <p>The width of the mid connector largely determines the horizontal size of the tree structure
     * (apart from the actual node text).
     *
     * @param midConnector A new "mid connector" string.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions midConnector(String midConnector)
    {
        this.midConnector = midConnector;
        midPaddingPrefix = null;
        return this;
    }

    /**
     * Sets the "end connector" string, similar to {@link midConnector(String) midConnector}, but
     * specfically for the final child node (for any given parent).
     *
     * <p>By default, the end connector is "└── ", or 0x2514 ("BOX DRAWINGS LIGHT UP AND RIGHT"),
     * then two times 0x2500 ("BOX DRAWINGS LIGHT HORIZONTAL"), then an ordinary space (4
     * characters total).
     *
     * @param endConnector A new "end connector" string.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions endConnector(String endConnector)
    {
        this.endConnector = endConnector;
        endPaddingPrefix = null;
        return this;
    }

    /**
     * Convenience method for setting all the line-drawing characters to ASCII characters, in case
     * Unicode box-drawing characters are not supported.
     *
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions asciiLines()
    {
        topConnector = "|";
        parentLine = "|";
        midConnector = "+-- ";
        endConnector = "\\-- ";
        midPaddingPrefix = "|   ";
        endPaddingPrefix = "    ";
        return this;
    }

    /**
     * Intended for individual nodes &ndash; formats a "label" node, by removing the
     * {@link midConnector(String) midConnector} (setting it to the
     * {@link parentLine(String) parentLine} with padding spaces). Use this to annotate parts of the
     * tree without creating (what look like) more nodes.
     *
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions asLabel()
    {
        this.midConnector = getMidPaddingPrefix();
        return this;
    }

    /**
     * Intended for individual nodes &ndash; formats a "pre-label" node, similar to an ordinary
     * label (see {@link asLabel}), but connected to the next sibling node via a
     * {@link topConnector(String) topConnector}. Use this to annotate a node prior to the node's
     * main text.
     *
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions asPreLabel()
    {
        nextSiblingOptions(n -> n.topMargin(0).topConnectorLength(1));
        midConnector(getMidPaddingPrefix());
        return this;
    }

    /**
     * Sets another {@code NodeOptions} instance to be applied to the sibling node following the
     * current node. That is, the "next sibling options" apply to the next node at the same level of
     * the tree, following whichever node(s) <em>this</em> {@code NodeOptions} instance is applied
     * to.
     *
     * <p>In the simplest case, this comes into effect when calling
     * {@link TreeWriter#startNode(boolean,NodeOptions)} (<em>with</em> a {@code NodeOptions}
     * argument), followed perhaps by some child nodes, followed by a call to
     * {@link TreeWriter#startNode(boolean)} (<em>without</em> a {@code NodeOptions} argument) at
     * the same tree level. The latter would normally use the "standard" options set for the
     * {@link TreeWriter}, but if the {@code NodeOptions} passed to the first call contains a "next
     * sibling" {@code NodeOptions} instance, then that will be used in the second call.
     *
     * <p>In combination with {@link firstChildOptions(NodeOptions) firstChildOptions}, this permits
     * you to specify complex patterns of node options applying to arbitrary subtrees. The next
     * sibling and first child options can be cyclic references back to the current instance, in
     * order to apply specific options to all sibling and/or child nodes in a subtree. You can form
     * arbitrarily-long chains and/or cycles of {@code NodeOptions} instances, as needed.
     *
     * @param nextSiblingOptions The options to be supplied to the sibling node following the
     *     current node.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions nextSiblingOptions(NodeOptions nextSiblingOptions)
    {
        this.nextSiblingOptions = nextSiblingOptions;
        return this;
    }

    /**
     * Sets another {@code NodeOptions} instance to be applied to the first child of the current
     * node (whichever node(s) <em>this</em> {@code NodeOptions} instance is applied to. This is
     * similar in concept to {@link nextSiblingOptions}.
     *
     * <p>In the simplest case, this comes into effect when calling
     * {@link TreeWriter#startNode(boolean,NodeOptions)} (<em>with</em> a {@code NodeOptions}
     * argument), followed immediately by a call to {@link TreeWriter#startNode(boolean)}
     * (<em>without</em> a {@code NodeOptions} argument), which creates a child node. The latter
     * would normally use the "standard" options set for the {@link TreeWriter}, but if the
     * {@code NodeOptions} passed to the first call contains a "first child" {@code NodeOptions}
     * instance, then that will be used in the second call.
     *
     * <p>In combination with {@link nextSiblingOptions(NodeOptions) nextSiblingOptions}, this
     * permits you to specify complex patterns of node options applying to arbitrary subtrees. The
     * next sibling and first child options can be cyclic references back to the current instance,
     * in order to apply specific options to all sibling and/or child nodes in a subtree. You can
     * form arbitrarily-long chains and/or cycles of {@code NodeOptions} instances, as needed.
     *
     * @param firstChildOptions The options to be supplied to the first child node of the current
     *     node.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions firstChildOptions(NodeOptions firstChildOptions)
    {
        this.firstChildOptions = firstChildOptions;
        return this;
    }

    /**
     * Creates (if necessary) and initialises another {@code NodeOptions} instance representing the
     * "next sibling" options. This is a convenience method roughly equivalent to calling
     * {@link nextSiblingOptions(NodeOptions)} with an explicitly-created copy of the current
     * instance (though no copy is made if the "next sibling" options already exists).
     *
     * @param init An initialiser for the next sibling options. If nothing needs to be changed, you
     *     can pass in "<code>n -&gt; {}</code>".
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions nextSiblingOptions(Consumer<NodeOptions> init)
    {
        if(nextSiblingOptions == null)
        {
            nextSiblingOptions = copy();
        }
        init.accept(nextSiblingOptions);
        return this;
    }

    /**
     * Creates (if necessary) and initialises another {@code NodeOptions} instance representing the
     * "first child" options. This is a convenience method roughly equivalent to calling
     * {@link firstChildOptions(NodeOptions)} with an explicitly-created copy of the current
     * instance (though no copy is made if the "first child" options already exists).
     *
     * @param init An initialiser for the first child options. If nothing needs to be changed, you
     *     can pass in "<code>n -&gt; {}</code>".
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions firstChildOptions(Consumer<NodeOptions> init)
    {
        if(firstChildOptions == null)
        {
            firstChildOptions = copy();
        }
        init.accept(firstChildOptions);
        return this;
    }


    public NodeOptions getNextSiblingOptions() { return nextSiblingOptions; }
    public NodeOptions getFirstChildOptions()  { return firstChildOptions; }
    public int getTopMargin()           { return topMargin; }
    public int getTopConnectorLength()  { return topConnectorLength; }
    public String getTopConnector()     { return topConnector; }
    public String getParentLine()       { return parentLine; }
    public String getMidConnector()     { return midConnector; }
    public String getEndConnector()     { return endConnector; }

    public String getMidPaddingPrefix()
    {
        if(midPaddingPrefix == null)
        {
            midPaddingPrefix =
                parentLine +
                " ".repeat(Math.max(0, midConnector.length() - parentLine.length()));
        }
        return midPaddingPrefix;
    }

    public String getEndPaddingPrefix()
    {
        if(endPaddingPrefix == null)
        {
            endPaddingPrefix = " ".repeat(endConnector.length());
        }
        return endPaddingPrefix;
    }

}
