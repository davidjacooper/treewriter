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
 * supply the text to be prefixed).
 *
 * <p>The main purpose of this class is to support {@link TreeWriter}, which provides a
 * higher-level interface for outputting tree structures. {@code PrefixingWriter} can in principle
 * be used by itself, though it lacks methods like {@code println} and {@code printf}, not being a
 * subclass of {@code PrintWriter}.
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
    public static final int DEFAULT_WRAP_LENGTH = 80;

    private Writer out;
    private Deque<String> prefixes = new LinkedList<>();
    private String nextLinePrefix = null;
    protected int lineLength = 0;

    private int wrapLength;
    private AnsiState ansiState = new AnsiState();

    {
        wrapLength = AnsiConsole.getTerminalWidth();
        if(wrapLength == 0)
        {
            wrapLength = DEFAULT_WRAP_LENGTH;
        }
    }

    public PrefixingWriter(OutputStream out, Charset charset)
    {
        this.out = new OutputStreamWriter(out, charset);
    }

    public PrefixingWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    public PrefixingWriter()
    {
        this(System.out);
    }

    public PrefixingWriter(Writer out)
    {
        this.out = out;
    }

    public void setWrapLength(int wrapLength)
    {
        this.wrapLength = wrapLength;
    }

    public int getWrapLength()         { return wrapLength; }
    public int getLineLength()         { return lineLength; }
    public int getTotalLineSpace()     { return getLineCapacity(0); }
    public int getRemainingLineSpace() { return getLineCapacity(lineLength); }

    public int getLineCapacity(int deduction)
    {
        if(wrapLength <= 0 || wrapLength == Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }

        int capacity = wrapLength - deduction;
        for(var prefix : prefixes)
        {
            capacity -= prefix.length();
        }
        return capacity;
    }

    public void addPrefix(String newPrefix)
    {
        prefixes.addLast(newPrefix);
    }

    public void removePrefix()
    {
        if(prefixes.isEmpty())
        {
            throw new IllegalStateException("No prefix currently exists");
        }
        prefixes.removeLast();
        this.nextLinePrefix = null;
    }

    public void replacePrefix(String newPrefix)
    {
        removePrefix();
        addPrefix(newPrefix);
    }

    public void replacePrefixAfterLine(String nextLinePrefix)
    {
        this.nextLinePrefix = nextLinePrefix;
    }

    private void writePrefix() throws IOException
    {
        for(var prefix : prefixes)
        {
            out.write(prefix);
            lineLength += prefix.length();
        }
        ansiState.write(out);
    }

    protected void lineBreak() throws IOException
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
            }
            out.write(ch);
            lineLength++;
        }
    }

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
            if(lineLength >= wrapLength)
            {
                lineLength %= wrapLength;
            }

            if(lineLength == 0)
            {
                writePrefix();
            }

            while((lineLength + len) > wrapLength)
            {
                int partLength = wrapLength - lineLength;
                out.write(buf, off, partLength);
                lineBreak();
                writePrefix();
                off += partLength;
                len -= partLength;
            }
            if(len > 0)
            {
                out.write(buf, off, len);
                lineLength += len;
            }
        }
    }

    @Override
    public void write(char[] buf, int off, int len) throws IOException
    {
        if(off < 0) { throw new IndexOutOfBoundsException("Offset cannot be negative"); }
        if(len < 0) { throw new IndexOutOfBoundsException("Length cannot be negative"); }
        if(off + len > buf.length)
        {
            throw new IndexOutOfBoundsException("Offset + length exceed the buffer size");
        }

        int start = off;
        int lookahead = off;
        while(lookahead < len)
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
                    if(lookahead < len && buf[lookahead] == '[')
                    {
                        lookahead++;
                        while(lookahead < len && 0x20 <= buf[lookahead] && buf[lookahead] <= 0x3f)
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
        if(start < len)
        {
            writeSegment(buf, start, len - start);
        }
    }

    @Override
    public void write(char[] buf) throws IOException
    {
        write(buf, 0, buf.length);
    }

    @Override
    public void write(String s) throws IOException
    {
        write(s.toCharArray());
    }

    @Override
    public void write(String s, int off, int len) throws IOException
    {
        var buf = s.toCharArray();
        write(buf, 0, buf.length);
    }

    @Override
    public void flush() throws IOException
    {
        out.flush();
    }

    @Override
    public void close() throws IOException
    {
        out.close();
    }
}
