import au.djac.treewriter.*;
import java.util.*;
import java.util.stream.*;

public class TreeWriterDemo
{
    public static void main(String[] args)
    {
        var writer = new TreeWriter();
        dataStructure(writer);
        substrings(writer);
        recursiveBinaryTree(writer);
        coloursWrappingOptions(writer);
        customNodes(writer);
        labels(writer);
        optionChains(writer);
        lineSpace(writer);
    }

    static void dataStructure(TreeWriter writer)
    {
        class Node
        {
            String name, description;
            List<Node> children;
            Node(String n, String d, List<Node> c)
            {
                name = n;
                description = d;
                children = c;
            }
        }

        var root =
            new Node("A", "alpha", List.of(
                new Node("B", "beta", List.of(
                    new Node("C", "gamma", List.of()),
                    new Node("D", "delta", List.of(
                        new Node("E", "epsilon", List.of()))))),
                new Node("F", "zeta", List.of(
                    new Node("G", "eta", List.of())))));

        writer.forTree(
            root,
            node -> node.children,
            node ->
            {
                writer.println(node.name);
                writer.println(node.description);
            }
        );
    }

    static void substrings(TreeWriter writer)
    {
        writer.println();
        writer.printTree("hello",
                         s -> IntStream.range(1, s.length())
                                       .mapToObj(s::substring)
                                       .collect(Collectors.toList()),
                         String::valueOf);
    }

    static void recursiveBinaryTree(TreeWriter writer)
    {
        writer.println();
        writer.println("root");
        recursiveBinaryTree(writer, 1);
    }

    static void recursiveBinaryTree(TreeWriter writer, int depth)
    {
        if(depth > 3) { return; }

        writer.startNode(true);
        writer.printf("Level %d, child A\n", depth);
        recursiveBinaryTree(writer, depth + 1);
        writer.endNode();

        writer.startNode(false);
        writer.printf("Level %d, child B\n", depth);
        recursiveBinaryTree(writer, depth + 1);
        writer.endNode();
    }

    static void coloursWrappingOptions(TreeWriter writer)
    {
        String blueBg = "\033[44m";
        String redFg = "\033[31;1m";
        String reset = "\033[m";

        String s = "012345678";

        var originalOpts = writer.getOptions().copy();
        writer.getOptions()
              .topMargin(1)
              .parentLine("\033[32m\u2551\u2551")
              .midConnector("\033[32m\u2560\033[35m\u256c\u2550\u2550 ")
              .endConnector("\033[32m\u255a\033[35m\u2569\u2550\u2550 ");

        writer.println();
        writer.println(blueBg + s.repeat(15) + reset);

        writer.startNode(true);
        writer.println((s + redFg + s + blueBg + s + reset).repeat(10));
        writer.endNode();

        writer.startNode(false);
        writer.println((s + blueBg + s + redFg + s + reset).repeat(10));
        writer.endNode();

        writer.setOptions(originalOpts);
    }

    static void customNodes(TreeWriter writer)
    {
        writer.println();
        writer.println("root");

        writer.startNode(true, new NodeOptions().midConnector("\u251d\u2501\u2501\u2501\u2501> ")
                                                 .topMargin(2));
        writer.println("alpha");

        writer.startNode(false, writer.getOptions().copy().endConnector("\u2558"));
        writer.println("beta");

        writer.startNode(false, writer.getOptions().copy().endConnector("\u2570\u2508\u2573 ")
                                                         .topMargin(1));
        writer.println("gamma");

        writer.endNode();
        writer.endNode();
        writer.endNode();

        writer.startNode(false);
        writer.println("delta");
        writer.endNode();
    }

    static void labels(TreeWriter writer)
    {
        writer.println();
        writer.println("root");

        writer.getOptions().topMargin(1);

        writer.startPreLabelNode();
        writer.println("pre-label A");
        writer.endNode();

        writer.startNode(true);
        writer.println("A");

        writer.startLabelNode(true);
        writer.println("label A");
        writer.endNode();

        writer.startNode(false);
        writer.println("child");
        writer.endNode();
        writer.endNode();

        writer.printPreLabel("pre-label B");
        writer.startNode(false);
        writer.println("B");
        writer.printLabel(true, "label B");
        writer.startNode(false);
        writer.println("child");
        writer.endNode();
        writer.endNode();

        writer.setOptions(new NodeOptions());
    }

    static void optionChains(TreeWriter writer)
    {
        writer.println();
        writer.println("root");

        var opts1 = new NodeOptions();
        var opts2 = new NodeOptions();
        var opts3 = new NodeOptions();

        opts1.midConnector("\u251c(1)\u2500 ")
             .endConnector("\u2514(1)\u2500 ")
             .firstChildOptions(opts2)
             .nextSiblingOptions(opts3);

        opts2.endConnector("\u251c(2)\u2500 ")
             .endConnector("\u2514(2)\u2500 ")
             .firstChildOptions(opts1);

        opts3.midConnector("\u251c(3)\u2500 ")
             .endConnector("\u2514(3)\u2500 ")
             .nextSiblingOptions(opts1);

        // This first node will dictate the options to be used across all its child and sibling
        // nodes.
        writer.startNode(true, opts1);
        writer.println("node");

        // A series of child nodes, _without_ any options explicitly provided. The options provided
        // beforehand will cause an alternation between opts1 and opts2.
        for(var i = 0; i < 5; i++)
        {
            writer.startNode(false);
            writer.println("child");
        }
        for(var i = 0; i < 5; i++)
        {
            writer.endNode();
        }
        writer.endNode();

        // A series of sibling nodes, again without any options explicitly provided. The options
        // provided beforehand will cause an alternation between opts1 and opts3.
        for(var i = 0; i < 5; i++)
        {
            writer.startNode(true);
            writer.println("sibling");
            writer.endNode();
        }

        writer.startNode(false, writer.getOptions());
        writer.println("end");
        writer.endNode();
    }

    static void lineSpace(TreeWriter writer)
    {
        writer.println();
        for(var i = 0; i < 5; i++)
        {
            writer.print("left-aligned");
            writer.print(" ".repeat(writer.getRemainingLineSpace() - "right-aligned".length()));
            writer.print("right-aligned");
            writer.startNode(false);
        }
        writer.println("end");
        for(var i = 0; i < 5; i++)
        {
            writer.endNode();
        }
    }

}
