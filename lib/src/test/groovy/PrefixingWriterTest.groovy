package au.djac.treewriter;
import spock.lang.*

/**
 * PrefixingWriter tries to do several things at once:
 * 1. Adding prefixes to lines;
 * 2. Line wrapping and measurements of line spacing;
 * 3. ANSI-code parsing;
 * 4. Efficient interoperability with the standard Writer interface, so that writing happens
 *    _either_ character-by-character (write(int)), or in bulk.
 *
 * Each test case addresses some aspect of 1-3, and in each test case we iterate over the different
 * write() overloads (and some extra combinations of write()s). Each test case has a 'where' clause
 * that selects elements of the 'writeFunctions' list.
 */
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
        "write(int),write(String,0,1)",
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
        {
            pw, s ->
            boolean toggle = false
            for(var ch : s.chars)
            {
                if(toggle)
                {
                    pw.write(ch)
                }
                else
                {
                    pw.write("${ch}", 0, 1)
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

    def "trivial (#label)"()
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
            label << writeLabels
            write << writeFunctions
    }

    def "line wrapping (#label)"()
    {
        given:
            pw.setWrapLength(6)
        when:
            write(pw, "Hello world ".repeat(5))
        then:
            sw.toString() == "Hello \nworld \n".repeat(4) + "Hello \nworld "

        where:
            label << writeLabels
            write << writeFunctions
    }

    def "simple nodes (#label)"()
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
            label << writeLabels
            write << writeFunctions
    }

    def "wrapping nodes (#label)"()
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
            label << writeLabels
            write << writeFunctions
    }

    def "replace prefix after next line (#label)"()
    {
        given:
            pw.setWrapLength(7)
        when:
            pw.addPrefix("!!");
            pw.replacePrefixAfterLine("@@@");
            write(pw, "Hello world\n");
        then:
            sw.toString() == "!!Hello\n@@@ wor\n@@@ld\n"
        where:
            label << writeLabels
            write << writeFunctions
    }

    def "non-wrapping with colour codes (#label)"()
    {
        given:
            pw.setWrapLength(11)
        when:
            write(pw, "\033[31m\033[32mHello world\033[m")
        then:
            sw.toString().replace('\033','~') == "\033[31m\033[32mHello world\033[m".replace('\033','~')
        where:
            label << writeLabels
            write << writeFunctions
    }


    def "wrapping with colour reset codes (#label)"()
    {
        given:
            pw.setWrapLength(6)
            var a = "\033[m"
        when:
            write(pw, "${a}Hel${a}lo ${a}w${a}orld\n")
        then:
            sw.toString().replace('\033','~') == "${a}Hel${a}lo ${a}\nw${a}orld\n".replace('\033','~')
        where:
            label << writeLabels
            write << writeFunctions
    }

    def "wrapping with full colour codes (#label)"()
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
            label << writeLabels
            write << writeFunctions
    }

    def "wrapping with prefix colour codes (#label)"()
    {
        given:
            pw.setWrapLength(8)
        when:
            pw.addPrefix("\033[31m!\033[32m!\033[m")
            write(pw, "Hello world\n")
        then:
            sw.toString() == "\033[31m!\033[32m!\033[mHello \n\033[31m!\033[32m!\033[mworld\n"
        where:
            label << writeLabels
            write << writeFunctions
    }

    def "very long ansi codes (#label)"()
    {
        given:
            pw.setWrapLength(100)
        when:
            var s = "Hello \033[31;32;33;34;35;36;37;38;39;40;41;42;43;44;45;46;47;48;49mworld"
            write(pw, s)
        then:
            sw.toString() == s
        where:
            label << writeLabels
            write << writeFunctions
    }

    def "line space measurement"()
    {
        when:
            pw.setWrapLength(5)
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 0
            pw.getRemainingLineSpace() == 5

        when:
            pw.write('.')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 1
            pw.getRemainingLineSpace() == 4

        when:
            pw.write('\033[m')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 1
            pw.getRemainingLineSpace() == 4

        when:
            pw.write('\033[31m..')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 3
            pw.getRemainingLineSpace() == 2

        when:
            pw.write('\n')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 0
            pw.getRemainingLineSpace() == 5

        when:
            pw.write('12345')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 5
            pw.getRemainingLineSpace() == 0

        when:
            pw.write('.')
        then:
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 1
            pw.getRemainingLineSpace() == 4

        when:
            pw.addPrefix("!!")
        then:
            // Since we've already started writing to the current line, any new prefix shouldn't
            // take effect yet.
            pw.getWrapLength() == 5
            pw.getLineSpace() == 5
            pw.getUsedLineSpace() == 1
            pw.getRemainingLineSpace() == 4

        when:
            pw.write('\n.')
        then:
            // Now the new prefix should take effect.
            pw.getWrapLength() == 5
            pw.getLineSpace() == 3
            pw.getUsedLineSpace() == 1
            pw.getRemainingLineSpace() == 2
    }

    // TODO:
    // - accessors
    //   - isWrapLengthAuto
}
