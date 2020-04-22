package gen.primitives;

public enum Colour
{
    Black,
    Blue,
    Red,
    Green,
    Yellow,
    Grey,
    Magenta,
    Orange,
    Cyan,
    Maroon,
    None,
    ;

    private static Colour[] arr = new Colour[] { Black, Blue, Red, Green, Yellow, Grey, Magenta, Orange, Cyan, Maroon, None};

    public static Colour toColour(int idx)
    {
        return arr[idx];
    }

    public static int fromColour(Colour c)
    {
        switch (c)
        {
            case Black:     return 0;
            case Blue:      return 1;
            case Red:       return 2;
            case Green:     return 3;
            case Yellow:    return 4;
            case Grey:      return 5;
            case Magenta:   return 6;
            case Orange:    return 7;
            case Cyan:      return 8;
            case Maroon:    return 9;
            case None:      return 10;
        }

        return 0;
    }
}
