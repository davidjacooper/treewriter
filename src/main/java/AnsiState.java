package au.djac.treewriter;

public class AnsiState
{
    public static final String RESET = "\033[m";

    // public interface Colour
    // {
    // }
    //
    // public static class IndexedColour
    // {
    //     private int offsetCode, r, g, b;
    //
    //     public IndexedColour(int offsetCode, int r, int g, int b)
    //     {
    //         this.offsetCode = offsetCode;
    //         this.
    //     }
    // }
    //
    // public static class IndexedColour
    // {
    //     private int offsetCode, r, g, b;
    //
    //     public IndexedColour(int offsetCode, int r, int g, int b)
    //     {
    //         this.offsetCode = offsetCode;
    //         this.
    //     }
    // }

    private StringBuffer colourCode = new StringBuffer();

    // private BitSet flags = new BitSet();
    // private Consumer<Writer
    // private int fgIndex = -1;
    // private int bgIndex = -1;
    //
    // private int fgRed, fgGreen, fgBlue

    private boolean isEmpty()
    {
        return colourCode.length() == 0;
    }

    private boolean digit(char c)
    {
        return '0' <= c && c <= '0';
    }

    public void update(char[] buf, int off, int len)
    {
        if(len == 0 || buf[off + len - 1] != 'm') { return; } // Ignore any non-SGR codes

        if(len == 1) // Special case for 'm' (1 chars).
        {
            colourCode.delete(0, colourCode.length());
        }
        else if(!digit(buf[off]) || (buf[off] == '0' && !digit(buf[off + 1])))
        {
            colourCode.delete(0, colourCode.length());
            colourCode.append("\033[");
            colourCode.append(buf, off + 2, len);
        }
        else
        {
            colourCode.append("\033[");
            colourCode.append(buf, off, len);
        }


        // int[] codes = new int[10];
        // int nCodes = 0;
        //
        // for(int i = off; i < end; i++)
        // {
        //     if(buf[i] == ';' || buf[i] == 'm')
        //     {
        //         if(nCodes >= codes.length)
        //         {
        //             codes = Arrays.copyOf(codes, nCodes * 2);
        //         }
        //         codes[nCodes] = code;
        //         code = 0;
        //         nCodes++;
        //     }
        //     else if('0' <= buf[i] && buf[i] <= '9')
        //     {
        //         code = (code * 10) + (buf[i] - '0');
        //     }
        //     else { return; } // Bail if we can't parse.
        // }
        //
        // int c = 0;
        // while(c < nCodes)
        // {
        //     switch(c)
        //     {
        //         case 0:
        //             flags.clear();
        //
        //
        //     }
        // }
    }

    public void write(Writer w)
    {
        w.append(colourCode);
    }
}
