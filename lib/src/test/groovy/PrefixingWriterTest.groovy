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

        "write(String,0,1),write(int)",
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

        {
            pw, s ->
            boolean toggle = false
            for(var ch : s.chars)
            {
                if(toggle)
                {
                    pw.write("${ch}", 0, 1)
                }
                else
                {
                    pw.write(ch)
                }
                toggle = !toggle
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

    def "non-wrapping with colour codes (#writeLabel)"()
    {
        given:
            pw.setWrapLength(11)
        when:
            write(pw, "\033[31m\033[32mHello world\033[m")
        then:
            sw.toString().replace('\033','~') == "\033[31m\033[32mHello world\033[m".replace('\033','~')
        where:
            writeLabel << writeLabels
            write << writeFunctions
    }


    def "wrapping with colour reset codes (#writeLabel)"()
    {
        given:
            pw.setWrapLength(6)
            var a = "\033[m"
        when:
            write(pw, "${a}Hel${a}lo ${a}w${a}orld\n")
        then:
            sw.toString().replace('\033','~') == "${a}Hel${a}lo ${a}\nw${a}orld\n".replace('\033','~')
        where:
            writeLabel << writeLabels
            write << writeFunctions
    }

    def "wrapping with full colour codes (#writeLabel)"()
    {
        given:
            pw.setWrapLength(6)
            var a = "\033[31m"
            var b = "\033[42m"
            var c = "\033[;35m"
            var reset = "\033[m"
        when:
            write(pw, "${a}H${reset}e${b}l${a}lo wo${c}rld ag${reset}ain\n")
        then:
            sw.toString() == "${a}H${reset}e${b}l${a}lo ${reset}\n${b}${a}wo${c}rld ${reset}\n${c}ag${reset}ain\n"
        where:
            writeLabel << writeLabels
            write << writeFunctions
    }
}
