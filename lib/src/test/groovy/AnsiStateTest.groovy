package au.djac.treewriter

import spock.lang.*

class AnsiStateTest extends Specification
{
    def "test visibleLength"()
    {
        expect:
            expectedLen == AnsiState.visibleLength(s)

        where:
            s                               || expectedLen
            ""                              || 0
            "\033[m"                        || 0
            "\033[12345m\033[12345m"        || 0
            "Hello"                         || 5
            "\033[1mHello"                  || 5
            "Hello\033[m"                   || 5
            "\033[1mHello\033[m"            || 5
            "\033[1mHe\033[31mllo\033[m"    || 5
    }

    String getState(AnsiState state)
    {
        var writer = new StringWriter()
        state.write(writer)
        return writer.toString()
    }

    void updateState(AnsiState state, String chars)
    {
        for(var ch : chars.chars)
        {
            state.append(ch)
        }
    }

    def "test state"()
    {
        given:
            StringWriter writer
            var ansiState = new AnsiState()

        expect:
            "" == getState(ansiState)
            ansiState.isEmpty()

        when:
            updateState(ansiState, "45;1m") // Accumulate #1 (last char is 'm')
        then:
            "\033[45;1m" == getState(ansiState)
            !ansiState.isEmpty()

        when:
            updateState(ansiState, "34m") // Accumulate #2
        then:
            "\033[45;1m\033[34m" == getState(ansiState)
            !ansiState.isEmpty()

        when:
            updateState(ansiState, "m") // Reset code
        then:
            "" == getState(ansiState)
            ansiState.isEmpty()

        when:
            updateState(ansiState, "31;41;1m")
        then:
            "\033[31;41;1m" == getState(ansiState)
            !ansiState.isEmpty()

        when:
            updateState(ansiState, "0m") // Reset code #2
        then:
            "" == getState(ansiState)
            ansiState.isEmpty()

        when:
            updateState(ansiState, "31;41;1m")
            updateState(ansiState, ";32m") // Reset code + other info
        then:
            "\033[;32m" == getState(ansiState)
            !ansiState.isEmpty()

        when:
            updateState(ansiState, "0;31m") // Reset code + other info #2
        then:
            "\033[0;31m" == getState(ansiState)
            !ansiState.isEmpty()

        when:
            updateState(ansiState, "45;1x") // Last char is not 'm' -- no effect
        then:
            "\033[0;31m" == getState(ansiState)
            !ansiState.isEmpty()
    }
}
