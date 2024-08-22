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

        if(ch != 'm')
        {
            // Sequence isn't an SGR (select graphics rendition) code; ignore it.
            newIndex = index;
            return true;
        }

        int len = newIndex - index;
        if(len == 3 || (len == 4 && firstCh == '0'))
        {
            // Simple reset code
            index = 0;
            newIndex = 0;
            return true;
        }

        if(index > 0 &&
            (!digit(firstCh) || (firstCh == '0' && !digit(buffer[index + 3]))))
        {
            System.arraycopy(buffer, index, buffer, 0, len);
            index = len;
            newIndex = len;
            return true;
        }

        index = newIndex;
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


// package au.djac.treewriter;
// import java.io.*;
// import java.util.*;
//
// /**
//  * Tracks the colour/font state of console text, in response to ANSI escape sequences. This is used
//  * by {@link PrefixingWriter} to help it "pause" and "resume" colour/font changes
//  * to text.
//  *
//  * <p>The tracking is done simplistically. Rather than keeping a record of actual foreground and
//  * background colours, and other flags, {@code AnsiState} currently just accumulates the codes it
//  * receives via the {@link update update} method in a buffer. You can then call {@link write write}
//  * to have it output the buffer contents in order to restore the colour state. If/when an SGR reset
//  * code is received (where one of the numbers in the escape sequence is 0), the buffer is cleared.
//  *
//  * <p>Additionally, {@link visibleLength} is tangentially-related to the rest of the class, and
//  * just finds string lengths excluding ANSI escape sequences.
//  */
// public class AnsiState
// {
//     /** A standalone ANSI SGR reset code, for returning subsequent console text to its default
//         colour/font settings. */
//     public static final String RESET = "\033[m";
//
//     private static Map<String,Integer> visibleLengths = new HashMap<>();
//
//     /**
//      * Utility method for computing (and caching) the number of characters in a string that would
//      * actually be printed to the terminal, which excludes characters making up ANSI escape
//      * sequences. For instance, the string "\033[1mHello\033[m" has a total length of 12, but a
//      * visible length of 5.
//      *
//      * @param ansiString The string to check.
//      * @return The number of characters to be displayed.
//      */
//     public static int visibleLength(String ansiString)
//     {
//         return visibleLengths.computeIfAbsent(
//             ansiString,
//             s ->
//             {
//                 int len = ansiString.length();
//                 int visLen = 0;
//
//                 int i = 0;
//                 while(i < len)
//                 {
//                     if(i < (len - 1) && ansiString.charAt(i) == '\033'
//                                      && ansiString.charAt(i + 1) == '[')
//                     {
//                         i += 2;
//                         char ch = ansiString.charAt(i);
//                         while(i < len && ch >= 0x20 && ch <= 0x3f)
//                         {
//                             i++;
//                             ch = ansiString.charAt(i);
//                         }
//                     }
//                     else
//                     {
//                         visLen++;
//                     }
//                     i++;
//                 }
//                 return visLen;
//             });
//     }
//
//     @SuppressWarnings("PMD.AvoidStringBufferField")
//     private final StringBuilder colourCode = new StringBuilder();
//
//     private char[] tmpBuffer = new char[10];
//     private int bufIndex = 0;
//
//
//     /** Creates a new instance. */
//     public AnsiState() {}
//
//     /**
//      * Reports whether the state remains at the default.
//      * @return {@code true} if the current state (still) represents the default console text state.
//      */
//     public boolean isEmpty()
//     {
//         return colourCode.length() == 0;
//     }
//
//     private boolean digit(char c)
//     {
//         return '0' <= c && c <= '9';
//     }
//
//     public boolean append(char ch)
//     {
//         if(bufIndex >= tmpBuffer.length)
//         {
//             char[] newBuf = new char[tmpBuffer.length * 2 + 1];
//             System.arraycopy(tmpBuffer, 0, newBuf, 0, tmpBuffer.length);
//             tmpBuffer = newBuf;
//         }
//         tmpBuffer[bufIndex] = ch;
//         bufIndex++;
//         if(ch < 0x20 || 0x3f < ch)
//         {
//             update(tmpBuffer);
//             bufIndex = 0;
//             return true;
//         }
//         return false;
//     }
//
//     /**
//      * Update the current state in accordance with a new ANSI escape sequence. Though ANSI escape
//      * sequences start with an escape character (\033) then a "[", these two characters should be
//      * omitted when calling this method.
//      *
//      * <p>Currently, only SGR (select graphics rendition) codes ending in "m" are acted upon. These
//      * are the codes that affect colour and font characteristics. Other codes, including for cursor
//      * movement, are currently ignored.
//      *
//      * @param buf The escape sequence characters, not including "\033[".
//      */
//     public void update(char[] buf)
//     {
//         // Ignore any non-SGR ("select graphic rendition") codes.
//         // (In future, we might want to take account of codes that move the cursor back and forth,
//         // but there's likely a limit to what we can do with them.)
//         if(buf.length == 0 || buf[buf.length - 1] != 'm') { return; }
//
//         if(buf.length == 1 || (buf.length == 2 && buf[0] == '0')) // \033[m or \033[0m
//         {
//             // Simple reset code
//             colourCode.delete(0, colourCode.length());
//         }
//         else if(!digit(buf[0]) || (buf[0] == '0' && !digit(buf[1])))
//         {
//             // Starts with '\033[;... or \033[0;...; i.e., a reset code followed by something else.
//             // This means the state gets replaced.
//             colourCode.delete(0, colourCode.length());
//             colourCode.append("\033[").append(buf);
//         }
//         else
//         {
//             // No reset code; the state must be appended.
//             colourCode.append("\033[").append(buf);
//         }
//     }
//
//     /**
//      * Outputs the current state.
//      * @param w A {@code Writer} used to perform the output.
//      * @throws IOException If an IO-related error occurs.
//      */
//     public void write(Writer w) throws IOException
//     {
//         w.append(colourCode);
//     }
//
//
// }
