package au.djac.treewriter;

import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * A {@code Writer} that automatically adds "prefixes" to the start of lines, even when lines are
 * wrapped. These prefixes themselves are built up and torn down through calls to
 * {@link addPrefix addPrefix}, {@link removePrefix removePrefix},
 * {@link replacePrefix replacePrefix} and {@link replacePrefixAfterLine replacePrefixAfterLine}.
 * These are expected to be interspersed with calls to the various {@code write} methods (which
 * supply the text to be prefixed). Changes to prefixes take effect after the next line break.
 *
 * <p>The main purpose of this class is to support {@link TreeWriter}, which provides a
 * higher-level interface for outputting tree structures. {@code PrefixingWriter} can in principle
 * be used by itself, though it lacks methods like {@code println} and {@code printf} (not being a
 * subclass of {@code PrintWriter}).
 *
 * <p>{@code PrefixingWriter} performs its own wrapping, independently of the forced wrapping
 * provided by any terminal environment, so that it can display the required prefixes again on the
 * next line before the wrapped text is allowed to continue. To this end, it must determine the
 * length of lines, either automatically (via the JAnsi library, if an appropriate terminal
 * environment exists), or by having the client code call {@link setWrapLength}. Otherwise, it will
 * assume a default "wrap length" of 80 characters.
 *
 * <p>{@code PrefixingWriter} supports ANSI escape codes (e.g., for changing text colours) in two
 * ways:
 * <ol>
 *   <li>ANSI escape codes (beginning with "{@code \033[}") do not count towards the current line
 *       length (since they don't appear on screen), and so won't cause lines to wrap early;
 *       and</li>
 *   <li>Colour/font changes produced by such codes are tracked, "paused" at the end of lines (and
 *       when lines wrap) so as not to affect the prefixes, and "resumed" on the next line after
 *       the prefixes are output.</li>
 * </ol>
 * <p>(The class won't output any ANSI codes itself unless they already appear in text provided to
 * one of the {@code write} methods.)
 *
 * @author David Cooper
 */
public class PrefixingWriter extends Writer
{
    /** The length to which lines are wrapped by default, if automatic detection fails. */
    public static final int DEFAULT_WRAP_LENGTH = 80;

    private Writer out;
    private Deque<String> prefixes = new LinkedList<>();
    private int totalPrefixLength = 0;
    private String nextLinePrefix = null;
    private int lineLength = 0;

    private int wrapLength;
    private boolean wrapLengthAuto;
    private AnsiState ansiState = new AnsiState();

    /**
     * Creates an instance that writes to another {@link Writer}.
     * @param out The {@code Writer} that will receive the output.
     */
    public PrefixingWriter(Writer out)
    {
        this.out = out;
        wrapLength = AnsiConsole.getTerminalWidth();
        wrapLengthAuto = (wrapLength > 0);
        if(!wrapLengthAuto)
        {
            wrapLength = DEFAULT_WRAP_LENGTH;
        }
    }

    /**
     * Creates an instance that writes to an {@link OutputStream}, with a specific character set.
     * @param out The {@code OutputStream} that will receive the output.
     * @param charset The character set for converting {@code char}s to {@code byte}s.
     */
    public PrefixingWriter(OutputStream out, Charset charset)
    {
        this(new OutputStreamWriter(out, charset));
    }

    /**
     * Creates an instance that writes to an {@link OutputStream}, with the default character set.
     * @param out The {@code OutputStream} that will receive the output.
     */
    public PrefixingWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    /**
     * Creates an instance that writes to {@link System#out}, with the default character set.
     */
    public PrefixingWriter()
    {
        this(System.out);
    }


    /**
     * Sets the maximum length of lines, in characters, before they are wrapped. This includes
     * both the prefix and the normal text to be displayed.
     *
     * Call {@link isWrapLengthAuto} first, if needed, to determine whether the existing wrap
     * length was able to be automatically determined.
     *
     * @param wrapLength The new maximum line length.
     */
    public void setWrapLength(int wrapLength)
    {
        this.wrapLength = wrapLength;
        this.wrapLengthAuto = false;
    }

    /**
     * Reports whether the current wrap length was automatically determined from the terminal
     * environment.
     *
     * @return {@code true} if the wrap length was automatically determined, or {@code false} if it
     * is either a default value, or was manually specified.
     */
    public boolean isWrapLengthAuto()
    {
        return wrapLengthAuto;
    }

    /**
     * Retrieves the existing maximum length of a line, in characters, including prefixes, before
     * the line is wrapped.
     *
     * <p>This is a fixed value unless/until it is altered by calling {@link setWrapLength}.
     *
     * @return The maximum line length.
     */
    public int getWrapLength()
    {
        return wrapLength;
    }

    /**
     * Retrieves the number of (visible) characters written so far to the current line,
     * <em>excluding</em> prefixes. This value changes each time any text is written.
     *
     * @return The number of visible characters after the prefix.
     */
    public int getUsedLineSpace()
    {
        return (lineLength == 0) ? 0 : (lineLength - totalPrefixLength);
    }

    /**
     * Retrieves the maximum allocation of (visible) characters to the current line, including any
     * already written, but <em>excluding</em> any prefixes. In other words, the "line space" is the
     * number of visible characters able to fit in between the prefixes on the left, and the
     * wrapping limit on the right.
     *
     * <p>This value will reduce when adding a new prefix (as the prefix takes up space otherwise
     * used for "normal" text output), and increase when removing a prefix.
     *
     * @return The current line space, or {@link Integer#MAX_VALUE} if the space is effectively
     * unlimited.
     */
    public int getLineSpace()
    {
        if(wrapLength <= 0 || wrapLength == Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        return wrapLength - totalPrefixLength;
    }

    /**
     * Retrieves the number of <em>additional</em> (visible) characters that can be written to the
     * current line before it wraps.
     *
     * @return The number of characters able to be written to the current line without wrapping, or
     * {@link Integer#MAX_VALUE} if the space is effectively unlimited.
     */
    public int getRemainingLineSpace()
    {
        if(wrapLength <= 0 || wrapLength == Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        return wrapLength - lineLength;
    }

    /**
     * Causes an additional prefix to be output at the start of each line. The prefix is appended to
     * the end (right) of any existing prefixes, so that from the next line onwards (until another
     * prefix change is made) the new prefix comes immediately before any "normal" (non-prefix)
     * text.
     *
     * @param newPrefix The new prefix string to append.
     */
    public void addPrefix(String newPrefix)
    {
        int visibleLen = AnsiState.visibleLength(newPrefix);
        var p = newPrefix;
        if(visibleLen != newPrefix.length() && !newPrefix.endsWith(AnsiState.RESET))
        {
            p += AnsiState.RESET;
        }
        prefixes.addLast(p);
        totalPrefixLength += visibleLen;
    }

    /**
     * Removes the most-recently added prefix string, so that the set of prefixes reverts to the
     * state prior to the last addition.
     */
    public void removePrefix()
    {
        if(prefixes.isEmpty())
        {
            throw new IllegalStateException("No prefix currently exists");
        }
        totalPrefixLength -= AnsiState.visibleLength(prefixes.removeLast());
        this.nextLinePrefix = null;
    }

    /**
     * A convenience method that removes one prefix and adds another.
     * @param newPrefix A replacement for the most-recently added prefix.
     */
    public void replacePrefix(String newPrefix)
    {
        removePrefix();
        addPrefix(newPrefix);
    }

    /**
     * Causes a prefix replacement to occur after the next line, generally where a particular prefix
     * is only intended to occur once (at a time).
     *
     * <p>Generally the client code would call {@link addPrefix} or {@link replacePrefix} to set a
     * particular one-time prefix, followed immediately by {@code replacePrefixAfterLine} to
     * specify the prefix to come after it.
     *
     * <p>This effect cannot easily be achieved by the client code in other ways, because the
     * transition from one line to the next may result from wrapping, which is not the client code's
     * responsibility.
     *
     * @param nextLinePrefix The new prefix to replace the current one <em>after</em> the next line.
     */
    public void replacePrefixAfterLine(String nextLinePrefix)
    {
        this.nextLinePrefix = nextLinePrefix;
    }

    /**
     * Outputs the prefixes. This is called immediately before the first characters are written
     * after a new line. (It is not necessarily immediately called after all line breaks; we wait
     * until we know there's more writing.)
     */
    private void writePrefix() throws IOException
    {
        for(var prefix : prefixes)
        {
            out.write(prefix);
        }
        lineLength += totalPrefixLength;
        if(lineLength >= wrapLength)
        {
            // Here, the prefixes have become so long that they take up all of (or more than) the
            // line capacity. This will look awful, but we just have to accept it and work with it.
            lineLength %= wrapLength;
        }
        ansiState.write(out);
    }

    /**
     * Effects a line break, whether because an actual '\n' was written, or because the text hit
     * the wrapLength.
     *
     * <p>This is where we apply a replacement prefix that's been delayed until after the next line.
     */
    private void lineBreak() throws IOException
    {
        if(lineLength == 0)
        {
            writePrefix();
        }
        if(!ansiState.isEmpty())
        {
            out.write(AnsiState.RESET);
        }
        out.write('\n');
        flush();
        lineLength = 0;
        if(nextLinePrefix != null)
        {
            replacePrefix(nextLinePrefix);
            nextLinePrefix = null;
        }
    }

    /**
     * Writes a single character (contained in the 16 low-order bits of the given integer).
     * @param ch The character value to be written.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(int ch) throws IOException
    {
        if(lineLength == 0)
        {
            writePrefix();
        }

        if(ch == '\n')
        {
            lineBreak();
        }
        else
        {
            if(wrapLength > 0 && lineLength == wrapLength)
            {
                lineBreak();
                writePrefix();
            }
            out.write(ch);
            lineLength++;
        }
    }

    /**
     * Writes part of an array of characters, assumed to <em>not</em> contain any ANSI codes or
     * line breaks. This method focuses on <em>wrapping</em> the output text as needed.
     */
    private void writeSegment(char[] buf, int off, int len) throws IOException
    {
        if(wrapLength == 0)
        {
            if(lineLength == 0)
            {
                writePrefix();
            }
            out.write(buf, off, len);
        }
        else
        {
            if(lineLength == 0)
            {
                writePrefix();
            }

            int currentOff = off;
            int remainingLen = len;
            while((lineLength + remainingLen) > wrapLength)
            {
                int partLength = wrapLength - lineLength;
                out.write(buf, currentOff, partLength);
                lineLength += partLength;
                lineBreak();
                writePrefix();
                currentOff += partLength;
                remainingLen -= partLength;
            }
            if(remainingLen > 0)
            {
                out.write(buf, currentOff, remainingLen);
                lineLength += remainingLen;
            }
        }
    }

    /**
     * Writes part of an array of characters.
     * @param buf Array of characters.
     * @param off Offset from which to start writing characters.
     * @param len Number of characters to write.
     * @throws IndexOutOfBoundsException If {@code off} or {@code len} are invalid.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(char[] buf, int off, int len) throws IOException
    {
        if(off < 0) { throw new IndexOutOfBoundsException("Offset cannot be negative"); }
        if(len < 0) { throw new IndexOutOfBoundsException("Length cannot be negative"); }

        int end = off + len;
        if(end > buf.length)
        {
            throw new IndexOutOfBoundsException("Offset + length exceed the buffer size");
        }

        int start = off;
        int lookahead = off;
        while(lookahead < end)
        {
            switch(buf[lookahead])
            {
                case '\033':
                    if(lookahead > start)
                    {
                        writeSegment(buf, start, lookahead - start);
                    }
                    int ansiCodeStart = lookahead;
                    lookahead++;
                    if(lookahead < end && buf[lookahead] == '[')
                    {
                        lookahead++;
                        while(lookahead < end && 0x20 <= buf[lookahead] && buf[lookahead] <= 0x3f)
                        {
                            lookahead++;
                        }
                        ansiState.update(buf, ansiCodeStart + 2, lookahead - ansiCodeStart - 1);
                        lookahead++;
                    }
                    if(lineLength == 0)
                    {
                        writePrefix();
                    }
                    out.write(buf, ansiCodeStart, lookahead - ansiCodeStart);
                    start = lookahead;
                    break;

                case '\n':
                    if(lookahead > start)
                    {
                        writeSegment(buf, start, lookahead - start);
                    }
                    lineBreak();
                    lookahead++;
                    start = lookahead;
                    break;

                default:
                    lookahead++;
            }
        }
        if(start < end)
        {
            writeSegment(buf, start, end - start);
        }
    }

    /**
     * Writes part of an array of characters.
     * @param buf Array of characters.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(char[] buf) throws IOException
    {
        write(buf, 0, buf.length);
    }

    /**
     * Writes a string.
     * @param s String to be written.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(String s) throws IOException
    {
        write(s.toCharArray());
    }

    /**
     * Writes part of a string.
     * @param s String to be written.
     * @param off Offset from which to start writing characters.
     * @param len Number of characters to write.
     * @throws IndexOutOfBoundsException If {@code off} or {@code len} are invalid.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void write(String s, int off, int len) throws IOException
    {
        // var buf = s.toCharArray();
        // write(buf, 0, buf.length);
        write(s.toCharArray(), off, len);
    }

    /**
     * Flushes the underlying stream/writer.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

    /**
     * Closes the underlying stream/writer.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void close() throws IOException
    {
        out.close();
    }
}
