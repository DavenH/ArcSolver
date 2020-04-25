package gen.primitives;

import gen.priors.adt.Array;
import gen.priors.spatial.Compass;

public class Pos
{
    public static final Pos[] neighbours = new Pos[] {
            new Pos(-1, 0),
            new Pos(1, 0),
            new Pos(0, -1),
            new Pos(0, 1)
    };

    public static final Pos[] corners = new Pos[] {
            new Pos(-1, -1),
            new Pos(1, -1),
            new Pos(1, -1),
            new Pos(1, 1)
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

    public Pos left(Integer howMuch)    { return minus(howMuch, 0);  }
    public Pos right(Integer howMuch)   { return plus(howMuch, 0);   }
    public Pos up(Integer howMuch)      { return plus(0, howMuch);   }
    public Pos down(Integer howMuch)    { return minus(0, howMuch);  }

    public Pos plus(Pos d)          { return plus(d.x, d.y); }
    public Pos plus(int x, int y)   { return new Pos(this.x + x, this.y + y); }
    public Pos minus(Pos d)         { return plus(-d.x, -d.y); }
    public Pos minus(int x, int y)  { return plus(-x, -y); }
    public Pos transpose()          { return new Pos(y, x); }

    public Pos copy()               { return new Pos(x, y); }

    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }

    public Compass directionTo(Pos p)
    {
        int deltaX = p.x - x;
        int deltaY = p.y - y;

        if(deltaX < 0)
        {
            if(deltaY < 0)  return Compass.SW;
            if(deltaY == 0)  return Compass.W;
            if(deltaY > 0)  return Compass.NW;
        }
        else if(deltaX == 0)
        {
            if(deltaY < 0)  return Compass.S;
            if(deltaY == 0)  throw new RuntimeException("Cannot make a direction to same location!");
            if(deltaY > 0)  return Compass.N;
        }
        else if(deltaX > 0)
        {
            if(deltaY < 0)  return Compass.SE;
            if(deltaY == 0)  return Compass.E;
            if(deltaY > 0)  return Compass.NE;
        }

        throw new RuntimeException("Impossible state " + toString() + " " + p.toString());
    }

    public int get(int index)
    {
        return index == 0 ? x : y;
    }

    public static Array<Pos> permute(int n, int m)
    {
        Array<Pos> arr = new Array<>();

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
