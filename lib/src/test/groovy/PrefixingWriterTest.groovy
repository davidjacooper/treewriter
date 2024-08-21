package au.djac.treewriter;

import spock.lang.*

class PrefixingWriterTest extends Specification
{
    StringWriter sw
    PrefixingWriter pw

    @Shared String[] writeLabels = [
        "write(String)",
        "write(String,int,int)",
        "write(char[])",
        "write(char[],int,int)",
        "write(int)",
    ]

    @Shared Closure[] writeFunctions = [
        {
            pw, s ->
            pw.write(s)
        },
        {
            pw, s ->
            pw.write("abc${s}xyz", 3, s.length())
        },
        {
            pw, s ->
            pw.write(s.chars)
        },
        {
            pw, s ->
            pw.write("abc${s}xyz".chars, 3, s.length())
        },
        {
            pw, s ->
            for(var ch : s.chars)
            {
                pw.write(ch as int)
            }
        },
    ]

    def setup()
    {
        sw = new StringWriter()
        pw = new PrefixingWriter(sw)
    }

    def "trivial (#writeLabel)"()
    {
        // Without creating any nodes, or triggering wrapping
        given:
            pw.setWrapLength(100)
            var s = "Hello world\n".repeat(1)
        when:
            write(pw, s)
        then:
            sw.toString() == s
        where:
            writeLabel << writeLabels
            write << writeFunctions
    }

    def "line wrapping (#writeLabel)"()
    {
        given:
            pw.setWrapLength(6)
        when:
            write(pw, "Hello world ".repeat(5))
        then:
            sw.toString() == "Hello \nworld \n".repeat(4) + "Hello \nworld "

        where:
            writeLabel << writeLabels
            write << writeFunctions
    }

    def "simple nodes (#writeLabel)"()
    {
        given:
            pw.setWrapLength(100)

        when:
            write(pw, "Level 1\n")
            pw.addPrefix("!!")
            write(pw, "Level 2\n")
            write(pw, "Level 2\n")
            write(pw, "Level 2")
            pw.addPrefix("@@")
            write(pw, "\nLevel 3")
            pw.replacePrefix("##")
            write(pw, "\nLevel 3\n")
            pw.removePrefix()
            write(pw, "Level 2\n")
            pw.addPrefix("**")
            pw.addPrefix("%%")
            write(pw, "Level 4\n")
            write(pw, "Level 4\n")
            pw.removePrefix()
            pw.removePrefix()
            pw.replacePrefix("^^")
            write(pw, "Level 2\n")
            pw.removePrefix()
            write(pw, "Level 1\n")
            pw.addPrefix("&&")
            write(pw, "Level 2\n")

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

        where:
            writeLabel << writeLabels
            write << writeFunctions
    }

    def "wrapping nodes (#writeLabel)"()
    {
        given:
            pw.setWrapLength(6)

        when:
            write(pw, "Hello world\n")
            for(var i : 1..7)
            {
                System.out.println(i)
                pw.addPrefix("!")
                write(pw, "Hello world\n")
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
        where:
            writeLabel << writeLabels
            write << writeFunctions
    }

    def "wrapping with colour codes (#writeLabel)"()
    {
        // I'm not thinking this through.
        // The expected result should include 'reset' and 'carry-over' codes, not just a
        // naively-wrapped version of the input.

        given:
            var sw = new StringWriter()
            var pw = new PrefixingWriter(sw)
            pw.setWrapLength(6)

//             var a = "\033[m"
//             var b = "\033[123m"
//             var c = "\033[32m"
            var a = "\033[m"
            var b = a
            var c = a

        when:
//             pw.addPrefix("${a}!${b}!${c}")
//             write(pw, "${a}Hel${b}lo \nw${c}orl${a}d\n")
//             pw.addPrefix("!!")
            write(pw, "${a}Hello world\n")

//             var actual = sw.toString().chars.toList()
//             var expected = ( "!!${a}Hel${b}l\n"
//                              + "!!o w${c}o\n"
//                              + "!!rl${a}d\n" ).chars.toList()

        then:
//             sw.toString() == ( "${a}!${b}!${c}${a}Hel${b}l\n"
//                              + "${a}!${b}!${c}o w${c}o\n"
//                              + "${a}!${b}!${c}rl${a}d\n" )
//             sw.toString().chars.toList().collect{it as int} == ( "!!${a}Hel${b}l\n"
//                              + "!!o w${c}o\n"
//                              + "!!rl${a}d\n" ).chars.toList().collect {it as int}
//             sw.toString() == ( "!!${a}Hel${b}l\n"
//                              + "!!o w${c}o\n"
//                              + "!!rl${a}d\n" )

//             actual == expected

            sw.toString().chars.toList().collect{it as int} == ("${a}Hello \nworld\n").chars.toList().collect {it as int}

        where:
            writeLabel << writeLabels
            write << writeFunctions

    }
}
