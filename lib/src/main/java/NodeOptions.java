package au.djac.treewriter;

import java.util.function.Consumer;

/**
 * A set of options that determine how {@link TreeWriter} draws tree nodes.
 */
public class NodeOptions
{
    private int _topMargin = 0;
    private int _topConnectorLength = 0;
    private String _topConnector = "\u250a";
    private String _parentLine = "\u2502";
    private String _midConnector = "\u251c\u2500\u2500 ";
    private String _endConnector = "\u2514\u2500\u2500 ";
    private String _midPaddingPrefix = null;
    private String _endPaddingPrefix = null;

    private NodeOptions _nextSiblingOptions = null;
    private NodeOptions _firstChildOptions = null;

    /**
     * Creates a separate copy of this set of node options.
     * @return A new {@code NodeOptions} instance with the same options as this one.
     */
    public NodeOptions copy()
    {
        var newOpts = new NodeOptions();
        newOpts._topMargin = _topMargin;
        newOpts._topConnectorLength = _topConnectorLength;
        newOpts._topConnector = _topConnector;
        newOpts._parentLine = _parentLine;
        newOpts._midConnector = _midConnector;
        newOpts._endConnector = _endConnector;
        newOpts._midPaddingPrefix = _midPaddingPrefix;
        newOpts._endPaddingPrefix = _endPaddingPrefix;
        newOpts._nextSiblingOptions = _nextSiblingOptions;
        newOpts._firstChildOptions = _firstChildOptions;
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
        this._topMargin = topMargin;
        return this;
    }

    /**
     * Sets the "top connector length" &ndash; the number of lines preceding a node that contain
     * the "top connector", a visual mechanism for connecting the node to a preceding node <em>in a
     * special way</em>. This is <em>not</em> the normal mechanism for drawing just a basic tree
     * structure. Rather, it is used (for instance) to draw "pre labels".
     *
     * <p>This is 0 by default (no top connector), and would typically be 0 or 1. If non-zero values
     * for both {@link topMargin(int) topMargin} and {@code topConnectorLength} are given, the
     * margin precedes the top connector.
     *
     * @see TreeWriter#startPreLabelNode
     * @param topConnectorLength A new "top connector length".
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions topConnectorLength(int topConnectorLength)
    {
        this._topConnectorLength = topConnectorLength;
        return this;
    }

    /**
     * Sets the "top connector" string, to be printed before a node
     * ({@link topConnectorLength(int) topConnectorLength} times) to indicate that it's connected
     * to a preceding node <em>in a special way</em>. This is <em>not</em> the normal mechanism for
     * drawing just a basic tree structure. Rather, it is used (for instance) to draw "pre labels".
     *
     * <p>By default, this is "┊", the unicode "BOX DRAWINGS LIGHT QUADRUPLE DASH VERTICAL"
     * character, 0x250a.
     *
     * @see TreeWriter#startPreLabelNode
     * @param topConnector A new "top connector" string.
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions topConnector(String topConnector)
    {
        this._topConnector = topConnector;
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
        this._parentLine = parentLine;
        _midPaddingPrefix = null;
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
        this._midConnector = midConnector;
        _midPaddingPrefix = null;
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
        this._endConnector = endConnector;
        _endPaddingPrefix = null;
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
        _topConnector = "|";
        _parentLine = "|";
        _midConnector = "+-- ";
        _endConnector = "\\-- ";
        _midPaddingPrefix = "|   ";
        _endPaddingPrefix = "    ";
        return this;
    }

    /**
     * Intended for individual nodes &ndash; formats a "label" node, by removing the
     * {@link midConnector(String) midConnector} (setting it to the
     * {@link parentLine(String) parentLine} with padding spaces). Use this to annotate parts of the
     * tree without creating (what look like) more nodes.
     *
     * @param children True if the label needs an adjacent vertical line (mid-padding) to connect
     *   to subsequent child nodes
     * @return This {@code NodeOptions} instance.
     */
    public NodeOptions asLabel(boolean children)
    {
        // this._midConnector = getMidPaddingPrefix();
        this._midConnector = children ? getEndPaddingPrefix() : getMidPaddingPrefix();
        return this;
    }

    /**
     * Intended for individual nodes &ndash; formats a "pre-label" node, similar to an
     * {@link asLabel ordinary label}, but connected to the next sibling node via a
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
        this._nextSiblingOptions = nextSiblingOptions;
        return this;
    }

    /**
     * Sets another {@code NodeOptions} instance to be applied to the first child of the current
     * node (whichever node(s) <em>this</em> {@code NodeOptions} instance is applied to). This is
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
        this._firstChildOptions = firstChildOptions;
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
        if(_nextSiblingOptions == null)
        {
            _nextSiblingOptions = copy();
        }
        init.accept(_nextSiblingOptions);
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
        if(_firstChildOptions == null)
        {
            _firstChildOptions = copy();
        }
        init.accept(_firstChildOptions);
        return this;
    }


    /**
     * Retrieves the options (if any) to be applied to the next sibling node (of the node(s) to
     * which <em>these</em> settings are applied).
     * @return The next sibling options, or {@code null} if there aren't any.
     */
    public NodeOptions getNextSiblingOptions()
    {
        return _nextSiblingOptions;
    }

    /**
     * Retrieves the options (if any) to be applied to the first child node (of the node(s) to which
     * <em>these</em> settings are applied).
     * @return The first child options, or {@code null} if there aren't any.
     */
    public NodeOptions getFirstChildOptions()
    {
        return _firstChildOptions;
    }

    /**
     * Retrieves the size of the "top margin".
     * @see #topMargin(int)
     * @return The top margin length.
     */
    public int getTopMargin()
    {
        return _topMargin;
    }

    /**
     * Retrieves the size of the "top connector".
     * @see #topConnectorLength(int)
     * @return The top connector length.
     */
    public int getTopConnectorLength()
    {
        return _topConnectorLength;
    }

    /**
     * Retrives the string representing the "top connector".
     * @see #topConnector(String)
     * @return The top connector string.
     */
    public String getTopConnector()
    {
        return _topConnector;
    }

    /**
     * Retrives the string representing the "parent line".
     * @see #parentLine(String)
     * @return The parent line string.
     */
    public String getParentLine()
    {
        return _parentLine;
    }

    /**
     * Retrives the string representing the "mid connector".
     * @see #midConnector(String)
     * @return The mid connector string.
     */
    public String getMidConnector()
    {
        return _midConnector;
    }

    /**
     * Retrives the string representing the "end connector".
     * @see #endConnector(String)
     * @return The end connector string.
     */
    public String getEndConnector()
    {
        return _endConnector;
    }

    /**
     * Retrieves the padding string that precedes each line of a node, other than a final child
     * node, and apart from where the actual mid connector is drawn. This is automatically created
     * based on the {@link parentLine(String) parentLine} and the size of the
     * {@link midConnector(String) midConnector}.
     *
     * @return The mid padding string.
     */
    public String getMidPaddingPrefix()
    {
        if(_midPaddingPrefix == null)
        {
            _midPaddingPrefix =
                _parentLine +
                " ".repeat(Math.max(0, AnsiState.visibleLength(_midConnector) -
                                       AnsiState.visibleLength(_parentLine)));
        }
        return _midPaddingPrefix;
    }

    /**
     * Retrieves the padding string that precedes each line of a final child node, apart from where
     * the actual end connector is drawn. This is automatically created based on the size of the
     * {@link endConnector(String) endConnector}.
     *
     * @return The end padding string.
     */
    public String getEndPaddingPrefix()
    {
        if(_endPaddingPrefix == null)
        {
            _endPaddingPrefix = " ".repeat(AnsiState.visibleLength(_endConnector));
        }
        return _endPaddingPrefix;
    }

}
