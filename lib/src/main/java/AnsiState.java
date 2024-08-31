package au.djac.treewriter;
import java.io.*;
import java.util.*;

/**
 * Tracks the colour/font state of console text, in response to ANSI escape sequences. This is used
 * by {@link PrefixingWriter} to help it "pause" and "resume" colour/font changes
 * to text.
 *
 * <p>The tracking is done simplistically. Rather than keeping a record of actual foreground and
 * background colours, and other flags, {@code AnsiState} currently just accumulates the codes it
 * receives via the {@link append append} method in a buffer. You can then call {@link write write}
 * to have it output the buffer contents in order to restore the colour state. If/when an SGR reset
 * code is received (where one of the numbers in the escape sequence is 0), the buffer is cleared.
 *
 * <p>Additionally, {@link visibleLength} is tangentially-related to the rest of the class, and
 * just finds string lengths excluding ANSI escape sequences.
 */
public class AnsiState
{
    /** A standalone ANSI SGR reset code, for returning subsequent console text to its default
        colour/font settings. */
    public static final String RESET = "\033[m";

    private static Map<String,Integer> visibleLengths = new HashMap<>();

    /**
     * Utility method for computing (and caching) the number of characters in a string that would
     * actually be printed to the terminal, which excludes characters making up ANSI escape
     * sequences. For instance, the string "\033[1mHello\033[m" has a total length of 12, but a
     * visible length of 5.
     *
     * @param ansiString The string to check.
     * @return The number of characters to be displayed.
     */
    public static int visibleLength(String ansiString)
    {
        return visibleLengths.computeIfAbsent(
            ansiString,
            s ->
            {
                int len = ansiString.length();
                int visLen = 0;

                int i = 0;
                while(i < len)
                {
                    if(i < (len - 1) && ansiString.charAt(i) == '\033'
                                     && ansiString.charAt(i + 1) == '[')
                    {
                        i += 2;
                        char ch = ansiString.charAt(i);
                        while(i < len && ch >= 0x20 && ch <= 0x3f)
                        {
                            i++;
                            ch = ansiString.charAt(i);
                        }
                    }
                    else
                    {
                        visLen++;
                    }
                    i++;
                }
                return visLen;
            });
    }

    private char[] buffer = new char[10];
    private int index = 0;
    private int newIndex = 0;
    private char firstCh = '\0';

    /** Creates a new instance. */
    public AnsiState() {}

    /**
     * Reports whether the state remains at the default.
     * @return {@code true} if the current state (still) represents the default console text state.
     */
    public boolean isEmpty()
    {
        return index == 0;
    }

    private boolean digit(char c)
    {
        return '0' <= c && c <= '9';
    }

    /**
     * Append a new character to the current ANSI state. This is assumed to be part of an ANSI
     * code, not including the starting "\033[". The method reports (true/false) whether this
     * character represents the end of a sequence.
     *
     * <p>Internally, AnsiState accumulates multiple ANSI colour (SGR, or "Select Graphics
     * Rendition") codes, waiting to be written out, but will clear them if it receives a reset
     * code.
     *
     * <p>The caller must determine (beforehand) that the character is actually part of an ANSI
     * code, though this method will detect and ignore non-SGR codes.
     *
     * @param ch The next ANSI code character.
     * @return Whether the given character ends the current sequence.
     */
    public boolean append(char ch)
    {
        if((newIndex + 2) >= buffer.length)
        {
            char[] newBuf = new char[buffer.length * 2 + 1];
            System.arraycopy(buffer, 0, newBuf, 0, buffer.length);
            buffer = newBuf;
        }

        if(newIndex == index)
        {
            buffer[newIndex] = '\033';
            buffer[newIndex + 1] = '[';
            newIndex += 2;
            firstCh = ch;
        }
        buffer[newIndex] = ch;
        newIndex++;

        if(0x20 <= ch && ch <= 0x3f)
        {
            // Sequence keeps going
            return false;
        }
        // Sequence is ending; ch is the final character.

        int len = newIndex - index;
        if(ch != 'm')
        {
            // Sequence isn't an SGR (select graphics rendition) code; ignore it.
            newIndex = index;
        }
        else if(len == 3 || (len == 4 && firstCh == '0'))
        {
            // Simple reset code
            index = 0;
            newIndex = 0;
        }
        else if(index > 0 &&
            (!digit(firstCh) || (firstCh == '0' && !digit(buffer[index + 3]))))
        {
            System.arraycopy(buffer, index, buffer, 0, len);
            index = len;
            newIndex = len;
        }
        else
        {
            index = newIndex;
        }
        return true;
    }

    /**
     * Outputs the current state.
     * @param w A {@code Writer} used to perform the output.
     * @throws IOException If the underlying {@code Writer} reports an IO-related error.
     */
    public void write(Writer w) throws IOException
    {
        w.write(buffer, 0, index);
    }
}
