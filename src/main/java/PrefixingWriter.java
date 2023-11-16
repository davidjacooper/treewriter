package au.djac.jprinttree;

import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class PrefixingWriter extends Writer
{
    private Writer out;
    private Deque<String> prefixes = new LinkedList<>();
    private String nextLinePrefix = null;
    protected int lineLength = 0;

    // private int wrapLength = AnsiConsole.getTerminalWidth();
    private int wrapLength = 80;

    public PrefixingWriter(OutputStream out, Charset charset)
    {
        this.out = new OutputStreamWriter(out, charset);
    }

    public PrefixingWriter(OutputStream out)
    {
        this(out, Charset.defaultCharset());
    }

    public PrefixingWriter(Writer out)
    {
        this.out = out;
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
    }

    protected void lineBreak() throws IOException
    {
        if(lineLength == 0)
        {
            writePrefix();
        }
        out.write("\n");
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

    private void writeLine(char[] buf, int off, int len) throws IOException
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

        int i = off;
        int j = off;
        while(j < len)
        {
            if(buf[j] == '\n')
            {
                if(j > i)
                {
                    writeLine(buf, i, j - i);
                }
                lineBreak();
                i = j + 1;
            }
            j++;
        }
        if(i < len)
        {
            writeLine(buf, i, len - i);
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
