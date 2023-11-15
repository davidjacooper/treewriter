package au.djac.jprinttree;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Arrays;

public class PrefixingPrintStream extends PrintStream
{
    private final Charset prefixCharset;
    private byte[] prefixBuffer = new byte[100];
    private int totalPrefixLength = 0;

    private int[] prefixLengths = new int[20];
    private int nPrefixes = 0;

    private String nextLinePrefix = null;
    private boolean postNewLine = true;

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

        // Track the length of this individual prefix, by adding it to a list.
        nPrefixes++;
        if(prefixLengths.length <= nPrefixes)
        {
            prefixLengths = Arrays.copyOf(prefixLengths, nPrefixes * 2);
        }
        prefixLengths[nPrefixes - 1] = nNewBytes;

        // Append the prefix to the total aggregate prefix.
        int newLength = totalPrefixLength + nNewBytes;
        if(prefixBuffer.length < newLength)
        {
            prefixBuffer = Arrays.copyOf(prefixBuffer, newLength * 2);
        }
        System.arraycopy(newBytes, 0, prefixBuffer, totalPrefixLength, nNewBytes);
        totalPrefixLength = newLength;
    }

    public void removePrefix()
    {
        if(nPrefixes == 0)
        {
            throw new IllegalStateException("No prefix currently exists");
        }

        nPrefixes--;
        totalPrefixLength -= prefixLengths[nPrefixes];
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
        if(postNewLine)
        {
            try
            {
                out.write(prefixBuffer, 0, totalPrefixLength);
            }
            catch(IOException e)
            {
                setError();
            }
            postNewLine = false;
        }

        if(b == '\n') // TODO: how do we handle '\r\n' line breaks?
        {
            postNewLine = true;
            if(nextLinePrefix != null)
            {
                replacePrefix(nextLinePrefix);
                nextLinePrefix = null;
            }
        }

        super.write(b);
    }

    @Override
    public void write(byte[] bytes, int off, int len) //throws IOException
    {
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



// package au.djac.jprinttree;
//
// import java.io.*;
// import java.nio.charset.Charset;
//
// public class PrefixingOutputStream extends FilterOutputStream
// {
//     private final Charset prefixCharset;
//     private byte[] prefixBuffer = new byte[50];
//     private int totalPrefixLength = 0;
//
//     private String nextLinePrefix = null;
//     private boolean postNewLine = true;
//
//     public PrefixingOutputStream(OutputStream out, Charset prefixCharset)
//     {
//         super(out);
//         this.prefixCharset = prefixCharset;
//     }
//
//     public PrefixingOutputStream(OutputStream out)
//     {
//         this(out, Charset.defaultCharset());
//     }
//
//     public void addPrefix(String newPrefix)
//     {
//         byte[] newBytes = newPrefix.getBytes(prefixCharset);
//
//         int nNewBytes = newBytes.length;
//         System.out.printf("\033[30;1mPrefixingOutputStream.addPrefix(): newBytes==%s; nNewBytes==%d\033[m\n", java.util.Arrays.toString(newBytes), nNewBytes);
//
//         if(nNewBytes > (int)Byte.MAX_VALUE)
//         {
//             throw new IllegalArgumentException(
//                 "Prefixes can be at most " + Byte.MAX_VALUE + " bytes long");
//         }
//
//         int newLength = totalPrefixLength + nNewBytes + 1;
//         if(prefixBuffer.length < newLength)
//         {
//             byte[] newBuffer = new byte[newLength * 2];
//             System.arraycopy(prefixBuffer, 0, newBuffer, 0, prefixBuffer.length);
//             prefixBuffer = newBuffer;
//         }
//         System.arraycopy(newBytes, 0, prefixBuffer, totalPrefixLength, nNewBytes);
//         totalPrefixLength = newLength;
//         prefixBuffer[totalPrefixLength - 1] = (byte)nNewBytes;
//         System.out.printf("\033[30;1mPrefixingOutputStream.addPrefix(): prefixBuffer==%s; totalPrefixLength==%d\033[m\n", java.util.Arrays.toString(prefixBuffer), totalPrefixLength);
//     }
//
//     public void removePrefix()
//     {
//         if(totalPrefixLength == 0)
//         {
//             throw new IllegalStateException("No prefix currently exists");
//         }
//         totalPrefixLength--;
//         totalPrefixLength -= (int)prefixBuffer[totalPrefixLength];
//     }
//
//     public void replacePrefix(String newPrefix)
//     {
//         removePrefix();
//         addPrefix(newPrefix);
//     }
//
//     public void replacePrefixAfterLine(String nextLinePrefix)
//     {
//         this.nextLinePrefix = nextLinePrefix;
//     }
//
//     @Override
//     public void write(int b) throws IOException
//     {
//         if(postNewLine)
//         {
//             out.write(prefixBuffer, 0, totalPrefixLength);
//             postNewLine = false;
//         }
//
//         if(b == '\n') // TODO: how do we handle '\r\n' line breaks?
//         {
//             postNewLine = true;
//             if(nextLinePrefix != null)
//             {
//                 replacePrefix(nextLinePrefix);
//                 nextLinePrefix = null;
//             }
//         }
//
//         out.write(b);
//     }
//
//     @Override
//     public void write(byte[] bytes, int off, int len) throws IOException
//     {
//         for(int i = off; i < len; i++)
//         {
//             write(bytes[i]);
//         }
//     }
//
//     @Override
//     public void write(byte[] bytes) throws IOException
//     {
//         write(bytes, 0, bytes.length);
//     }
// }
//
//
// // package au.djac.jprinttree;
// //
// // public class NodePrintStream extends PrintStream
// // {
// //     private boolean connector = false;
// //     private boolean newLine = true;
// //
// //     // public NodePrintStream(
// //
// //     public void connector()
// //     {
// //         this.connector = true;
// //     }
// //
// //     @Override
// //     public void write(int b) throws IOException
// //     {
// //         if(connector)
// //         {
// //             if(!newLine)
// //             {
// //
// //             }
// //         }
// //
// //         if(newLine)
// //         {
// //
// //         }
// //
// //         if(b == '\n')
// //         {
// //             newLine = true;
// //         }
// //         else if(newLine)
// //         {
// //
// //         }
// //         super.write(b);
// //     }
// //
// //     @Override
// //     public void write(byte[] bytes, int off, int len) throws IOException
// //     {
// //         for(int i = off; i < len; i++)
// //         {
// //             write(bytes[i]);
// //         }
// //     }
// //
// //     @Override
// //     public void write(byte[] bytes) throws IOException
// //     {
// //         write(bytes, 0, bytes.length);
// //     }
// // }
