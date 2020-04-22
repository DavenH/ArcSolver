package gen.primitives;

import gen.priors.adt.Array;

public class Pos
{
    public static final Pos[] neighbours = new Pos[] {
            new Pos(-1, 0),
            new Pos(1, 0),
            new Pos(0, -1),
            new Pos(0, 1)
    };

    public static final Pos[] neighboursDiag = new Pos[] {
            new Pos(-1, -1),
            new Pos(-1, 0),
            new Pos(-1, 1),
            new Pos(0, -1),
            new Pos(0, 1),
            new Pos(1, -1),
            new Pos(1, 0),
            new Pos(1, 1)
    };

    public int x;
    public int y;

    public Pos(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    public Pos right(int howMuch)   { return plus(howMuch, 0);   }
    public Pos up(int howMuch)      { return plus(0, howMuch);   }
    public Pos left(int howMuch)    { return minus(howMuch, 0);  }
    public Pos down(int howMuch)    { return minus(0, howMuch);  }

    public Pos copy()               { return new Pos(x, y); }

    public Pos plus(Pos d)          { return plus(d.x, d.y); }
    public Pos plus(int x, int y)   { return new Pos(this.x + x, this.y + y); }
    public Pos minus(Pos d)         { return plus(-d.x, -d.y); }
    public Pos minus(int x, int y)  { return plus(-x, -y); }
    public Pos transpose()          { return new Pos(y, x); }

    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }

    public int get(int index)
    {
        return index == 0 ? x : y;
    }

    public static Array permute(int n, int m)
    {
        Array arr = new Array();

        for(int i = 0; i < n; ++i)
            for(int j = 0; j < m; ++j)
                arr.add(new Pos(i, j));

        return arr;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Pos && ((Pos) o).x == x && ((Pos) o).y == y;
    }

    public Pos fromTop(int height)  { return new Pos(x, height - 1 - y); }
    public Pos fromRight(int width)  { return new Pos(width - 1 - x, y); }
    public Pos fromTopRight(int width, int height)  { return new Pos(width - 1 - x, height - 1 - y); }
}
