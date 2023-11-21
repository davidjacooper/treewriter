package au.djac.treewriter;
import java.io.*;

/**
 * Tracks the colour/font state of console text, in response to ANSI escape sequences. This is used
 * by {@link PrefixingWriter} to help it "pause" and "resume" colour/font changes
 * to text.
 *
 * <p>The tracking is done simplistically. Rather than keeping a record of actual foreground and
 * background colours, and other flags, {@code AnsiState} currently just accumulates the codes it
 * receives via the {@link update update} method in a buffer. You can then call {@link write write}
 * to have it output the buffer contents in order to restore the colour state. If/when an SGR reset
 * code is received (where one of the numbers in the escape sequence is 0), the buffer is cleared.
 */
public class AnsiState
{
    /** A standalone ANSI SGR reset code, for returning subsequent console text to its default
        colour/font settings. */
    public static final String RESET = "\033[m";

    private StringBuffer colourCode = new StringBuffer();

    /** Creates a new instance. */
    public AnsiState() {}

    /**
     * Reports whether the state remains at the default.
     * @return {@code true} if the current state (still) represents the default console text state.
     */
    public boolean isEmpty()
    {
        return colourCode.length() == 0;
    }

    private boolean digit(char c)
    {
        return '0' <= c && c <= '9';
    }

    /**
     * Update the current state in accordance with a new ANSI escape sequence. Though ANSI escape
     * sequences start with an escape character (\033) then a "[", these two characters should be
     * omitted when calling this method.
     *
     * <p>Currently, only SGR (select graphics rendition) codes ending in "m" are acted upon. These
     * are the codes that affect colour and font characteristics. Other codes, including for cursor
     * movement, are currently ignored.
     *
     * @param buf The character sequence in which the escape sequence occurs.
     * @param off The buffer position <em>following</em> the "[" character.
     * @param len The number of characters in the sequence following the "[" character. (This should
     *   normally be at least 1.)
     */
    public void update(char[] buf, int off, int len)
    {
        // Ignore any non-SGR ("select graphic rendition") codes.
        // (In future, we might want to take account of codes that move the cursor back and forth,
        // but there's likely a limit to what we can do with them.)
        if(len == 0 || buf[off + len - 1] != 'm') { return; }

        if(len == 1) // \033[m
        {
            // Simple reset code
            colourCode.delete(0, colourCode.length());
        }
        else if(!digit(buf[off]) || (buf[off] == '0' && !digit(buf[off + 1])))
        {
            // Starts with '\033[;... or \033[0;...; i.e., a reset code followed by something else.
            // This means the state gets replaced.
            colourCode.delete(0, colourCode.length());
            colourCode.append("\033[");
            colourCode.append(buf, off + 2, len - 2);
        }
        else
        {
            // No reset code; the state must be appended.
            colourCode.append("\033[");
            colourCode.append(buf, off, len);
        }
    }

    /**
     * Outputs the current state.
     * @param w A {@code Writer} used to perform the output.
     * @throws IOException If an IO-related error occurs.
     */
    public void write(Writer w) throws IOException
    {
        w.append(colourCode);
    }
}
