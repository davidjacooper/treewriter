package au.djac.jprintstream;

// import java.text.BreakIterator;
import java.util.Arrays;

public class CharUtils
{
    private static Map<String,Integer> gcLengthCache = new HashMap<>();

    public static int countGraphemeClustersCached(String s)
    {
        return gcLengthCache.computeIfAbsent(s, CharUtils::countGraphemeClusters);
    }

    public static int countGraphemeClusters(String s)
    {
        // int nChars = 0;
        // var bi = BreakIterator.getCharacterInstance();
        // bi.setText(s);
        // bi.first();
        // while(bi.next() != BreakIterator.DONE)
        // {
        //     nChars++;
        // }
        // return nChars;

        return (int) Arrays.stream(s.split("\\b{g}")).count();
    }


}
