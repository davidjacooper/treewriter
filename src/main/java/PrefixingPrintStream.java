package au.djac.jprinttree;

import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class PrefixingPrintStream extends PrintStream
{
    private static final int INITIAL_BUFFER_SIZE = 100;
    private static final int INITIAL_LENGTH_STACK_SIZE = 20;

    private final Charset prefixCharset;
    private byte[] prefixBuffer = new byte[INITIAL_BUFFER_SIZE];
    private int totalPrefixByteLength = 0;
    private int totalPrefixGCLength = 0;

    private int[] prefixByteLengths = new int[INITIAL_LENGTH_STACK_SIZE];
    private int[] prefixGCLengths = new int[INITIAL_LENGTH_STACK_SIZE];
    private int nPrefixes = 0;


    private String nextLinePrefix = null;
    private boolean postNewLine = true;
    private int lineGCLength = 0;

    public enum Wrap { UNHANDLED, CHARACTER, WORD }

    private Wrap wrap = Wrap.CHARACTER;
    private int wrapWidth = -1; // Use AnsiConsole.getTerminalWidth()
    // private char[] wordWrapBuffer = new char[20];


    public PrefixingPrintStream(OutputStream out, boolean autoFlush, Charset charset)
    {
        super(out, autoFlush, charset);
        this.prefixCharset = charset;
    }

    public PrefixingPrintStream(OutputStream out, boolean autoFlush)
    {
        this(out, autoFlush, Charset.defaultCharset());
    }

    public PrefixingPrintStream(OutputStream out, Charset charset)
    {
        this(out, false);
    }

    public PrefixingPrintStream(OutputStream out)
    {
        this(out, false, Charset.defaultCharset());
    }

    public void addPrefix(String newPrefix)
    {
        byte[] newBytes = newPrefix.getBytes(prefixCharset);
        int nNewBytes = newBytes.length;
        int nNewGraphemeClusters = CharUtils.countGraphemeClustersCached(newPrefix);

        // Track the length of this individual prefix, by adding it to a list.
        nPrefixes++;
        if(prefixByteLengths.length <= nPrefixes)
        {
            prefixByteLengths = Arrays.copyOf(prefixByteLengths, nPrefixes * 2);
            prefixGCLengths   = Arrays.copyOf(prefixGCLengths, nPrefixes * 2);
        }
        prefixByteLengths[nPrefixes - 1] = nNewBytes;
        prefixGCLengths  [nPrefixes - 1] = nNewGraphemeClusters;

        // Append the prefix to the total aggregate prefix.
        int newByteLength = totalPrefixByteLength + nNewBytes;
        if(prefixBuffer.length < newByteLength)
        {
            prefixBuffer = Arrays.copyOf(prefixBuffer, newByteLength * 2);
        }
        System.arraycopy(newBytes, 0, prefixBuffer, totalPrefixByteLength, nNewBytes);
        totalPrefixByteLength = newLength;
        totalPrefixGCLength += nNewGraphemeClusters;
    }

    public void removePrefix()
    {
        if(nPrefixes == 0)
        {
            throw new IllegalStateException("No prefix currently exists");
        }

        nPrefixes--;
        totalPrefixByteLength -= prefixByteLengths[nPrefixes];
        totalPrefixGCLength -= prefixGCLengths[nPrefixes];
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

    @Override
    public void write(int b) //throws IOException
    {
        if(lineGCLength == 0)
        {
            // At the start of a line; output the prefix before anything else.
            try
            {
                out.write(prefixBuffer, 0, totalPrefixLength);
                lineGCLength = totalPrefixGCLength;
            }
            catch(IOException e)
            {
                setError();
            }
            //postNewLine = false;
        }

        if(b == '\n') // TODO: how do we handle '\r\n' line breaks?
        {
            //postNewLine = true;
            lineGCLength = 0;
            if(nextLinePrefix != null)
            {
                replacePrefix(nextLinePrefix);
                nextLinePrefix = null;
            }
        }
        else
        {
            // Assume (a little on faith) that write(int) is only going to be called for characters
            // representable in 8 bits.
            lineGCLength++;
        }

        super.write(b);
    }

    @Override
    public void write(byte[] bytes, int off, int len) //throws IOException
    {
        if(lineGCLength == 0)
        {

        }
        for(int i = off; i < len; i++)
        {
            write(bytes[i]);
        }
    }

    @Override
    public void write(byte[] bytes) //throws IOException
    {
        write(bytes, 0, bytes.length);
    }
}
