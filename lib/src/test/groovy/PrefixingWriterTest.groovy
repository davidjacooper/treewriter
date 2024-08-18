package au.djac.treewriter;

import spock.lang.*

class PrefixingWriterTest extends Specification
{
    def "trivial"()
    {
        // Without creating any nodes, or triggering wrapping
        given:
            var sw = new StringWriter()
            var pw = new PrefixingWriter(sw)
            pw.setWrapLength(100)
            var s = "Hello world\n".repeat(100)
        when:
            pw.write(s)
        then:
            sw.toString() == s
    }

    def "line wrapping"()
    {
        given:
            var sw = new StringWriter()
            var pw = new PrefixingWriter(sw)
            pw.setWrapLength(6)
        when:
            pw.write("Hello world ".repeat(5))
        then:
            sw.toString() == "Hello \nworld \n".repeat(4) + "Hello \nworld "
    }

    def "simple nodes"()
    {
        given:
            var sw = new StringWriter()
            var pw = new PrefixingWriter(sw)
            pw.setWrapLength(100)

        when:
            pw.write("Level 1\n")
            pw.addPrefix("!!")
            pw.write("Level 2\n")
            pw.write("Level 2\n")
            pw.write("Level 2")
            pw.addPrefix("@@")
            pw.write("\nLevel 3")
            pw.replacePrefix("##")
            pw.write("\nLevel 3\n")
            pw.removePrefix()
            pw.write("Level 2\n")
            pw.addPrefix("**")
            pw.addPrefix("%%")
            pw.write("Level 4\n")
            pw.write("Level 4\n")
            pw.removePrefix()
            pw.removePrefix()
            pw.replacePrefix("^^")
            pw.write("Level 2\n")
            pw.removePrefix()
            pw.write("Level 1\n")
            pw.addPrefix("&&")
            pw.write("Level 2\n")

        then:
            sw.toString() == ( "Level 1\n"
                                + "!!Level 2\n"
                                + "!!Level 2\n"
                                + "!!Level 2\n"
                                + "!!@@Level 3\n"
                                + "!!##Level 3\n"
                                + "!!Level 2\n"
                                + "!!**%%Level 4\n"
                                + "!!**%%Level 4\n"
                                + "^^Level 2\n"
                                + "Level 1\n"
                                + "&&Level 2\n" )
    }

    def "wrapping nodes"()
    {
        given:
            var sw = new StringWriter()
            var pw = new PrefixingWriter(sw)
            pw.setWrapLength(6)

        when:
            pw.write("Hello world\n")
            for(var i : 1..7)
            {
                System.out.println(i)
                pw.addPrefix("!")
                pw.write("Hello world\n")
            }

        then:
            sw.toString() == ( "Hello \nworld\n"
                             + "!Hello\n! worl\n!d\n"
                             + "!!Hell\n!!o wo\n!!rld\n"
                             + "!!!Hel\n!!!lo \n!!!wor\n!!!ld\n"
                             + "!!!!He\n!!!!ll\n!!!!o \n!!!!wo\n!!!!rl\n!!!!d\n"
                             + "!!!!!H\n!!!!!e\n!!!!!l\n!!!!!l\n!!!!!o\n!!!!! \n"
                                + "!!!!!w\n!!!!!o\n!!!!!r\n!!!!!l\n!!!!!d\n"
                             + "!!!!!!Hello \n!!!!!!world\n"
                             + "!!!!!!!Hello\n!!!!!!! worl\n!!!!!!!d\n" )
    }
}
