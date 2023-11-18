package au.djac.treewriter;
import java.io.*;

public class AnsiState
{
    public static final String RESET = "\033[m";
    private StringBuffer colourCode = new StringBuffer();

    public AnsiState() {}

    public boolean isEmpty()
    {
        return colourCode.length() == 0;
    }

    private boolean digit(char c)
    {
        return '0' <= c && c <= '9';
    }

    public void update(char[] buf, int off, int len)
    {
        // Ignore any non-SGR ("select graphic rendition") codes
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

    public void write(Writer w) throws IOException
    {
        w.append(colourCode);
    }
}
