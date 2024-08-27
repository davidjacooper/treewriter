package au.djac.treewriter;
import spock.lang.*

class TreeWriterTest extends Specification
{
    static final def U = ['-': '\u2500',
                          '|': '\u2502',
                          '+': '\u251c',
                          '\\': '\u2514',
                          ':': '\u250a']

    String unicodeChars(String ascii)
    {
        return ascii.collect{U.get(it) ?: it}.join()
    }

    StringWriter sw
    TreeWriter tw

    def setup()
    {
        sw = new StringWriter()
        tw = new TreeWriter(sw)
    }

    def "trivial"()
    {
        when:
            tw.println("Hello world")

        then:
            sw.toString() == "Hello world\n"
    }

    def "basic tree"()
    {
        when:
            tw.println("root")
            tw.startNode(false)
            tw.println("node1")
            tw.endNode()
            tw.startNode(true)
            tw.println("node2")
            tw.endNode()

        then:
            sw.toString() == unicodeChars(/\
                root
                +-- node1
                \-- node2
            /.stripIndent())
    }

    def "medium-complexity tree (#label)"()
    {
        when:
            config(tw)
            tw.println("root")
            tw.startNode(false)
                tw.println("nodeA")
                tw.startNode(false)
                    tw.println("nodeAA")
                    tw.startNode(true)
                        tw.println("nodeAAA")
                    tw.endNode()
                tw.endNode()
                tw.startNode(true)
                    tw.println("nodeAB")
                    tw.startNode(true)
                        tw.println("nodeABA")
                    tw.endNode()
                tw.endNode()
            tw.endNode()
            tw.startNode(true)
                tw.println("nodeB")
                tw.startNode(false)
                    tw.println("nodeBA")
                    tw.startNode(true)
                        tw.println("nodeBAA")
                    tw.endNode()
                tw.endNode()
                tw.startNode(true)
                    tw.println("nodeBB")
                    tw.startNode(true)
                        tw.println("nodeBBA")
                    tw.endNode()
                tw.endNode()
            tw.endNode()

        then:
            sw.toString() == convert(/\
                root
                +-- nodeA
                |   +-- nodeAA
                |   |   \-- nodeAAA
                |   \-- nodeAB
                |       \-- nodeABA
                \-- nodeB
                    +-- nodeBA
                    |   \-- nodeBAA
                    \-- nodeBB
                        \-- nodeBBA
            /.stripIndent())

        where:
            label           | config                      | convert
            "ascii"         | { it.options.asciiLines() } | { it }
            "unicode-boxes" | {}                          | { unicodeChars(it) }
    }

    @Shared
    var listTree = [
        "root",
        [
            "nodeA",
            [
                "nodeAA",
                [
                    "nodeAAA",
                ],
            ],
            [
                "nodeAB",
                [
                    "nodeABA",
                ]
            ]
        ],
        [
            "nodeB",
            [
                "nodeBA",
                [
                    "nodeBAA",
                ],
            ],
            [
                "nodeBB",
                [
                    "nodeBBA",
                ]
            ]
        ],
    ]

    @Shared
    var listTreeOutput = /\
        root
        +-- nodeA
        |   +-- nodeAA
        |   |   \-- nodeAAA
        |   \-- nodeAB
        |       \-- nodeABA
        \-- nodeB
            +-- nodeBA
            |   \-- nodeBAA
            \-- nodeBB
                \-- nodeBBA
    /.stripIndent()


    def "printTree with object structure"()
    {
        when:
            tw.printTree(
                listTree,
                { it.drop(1) },
                { it[0] }
            )
        then:
            sw.toString() == unicodeChars(listTreeOutput)
    }


    def "printTree with node stream"()
    {
        when:
            tw.printTree(
                listTree,
                { it.size() - 1 },
                { it.drop(1).stream() },
                { it[0] }
            )
        then:
            sw.toString() == unicodeChars(listTreeOutput)
    }

    def "forTree with object structure"()
    {
        when:
            tw.forTree(
                listTree,
                { it.drop(1) },
                { tw.print(it[0]) }
            )
            tw.println();
        then:
            sw.toString() == unicodeChars(listTreeOutput)
    }

    def "forTree with node stream"()
    {
        when:
            tw.forTree(
                listTree,
                { it.size() - 1 },
                { it.drop(1).stream() },
                { tw.print(it[0]) }
            )
            tw.println();
        then:
            sw.toString() == unicodeChars(listTreeOutput)
    }

    def "labels and pre-labels"()
    {
        when:
            tw.println("root")
            tw.startLabelNode(true)
            tw.println("root label")
            tw.endNode()

            tw.startPreLabelNode()
            tw.println("node1 prelabel")
            tw.endNode()
            tw.startNode(false)
            tw.println("node1")
            tw.startLabelNode(false)
            tw.println("node1 label")
            tw.endNode()
            tw.endNode()

            tw.startPreLabelNode()
            tw.println("node2 prelabel")
            tw.endNode()
            tw.startNode(true)
            tw.println("node2")
            tw.startLabelNode(false)
            tw.println("node2 label")
            tw.endNode()
            tw.endNode()

        then:
            sw.toString() == unicodeChars(/\
                root
                |   root label
                |   node1 prelabel
                |   :
                +-- node1
                |       node1 label
                |   node2 prelabel
                |   :
                \-- node2
                        node2 label
            /.stripIndent())
    }

    /*
    TODO:

    NodeOptions: nextSiblingOptions, firstChildOptions
    */
}

