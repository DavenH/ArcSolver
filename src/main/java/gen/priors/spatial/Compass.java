package gen.priors.spatial;

public enum Compass
{
    N, NE, E, SE, S, SW, W, NW;

    public boolean isVertical() { return this == N || this == S; }
    public boolean isHorizontal() { return this == E || this == W; }
    public boolean isSW() { return this == S || this == SW || this == W; }
    public boolean isNE() { return this == N || this == NE || this == E; }

    public Compass perpTo(Compass dir)
    {
        switch (dir)
        {
            case N: return E;
            case NE: return SE;
            case E: return S;
            case SE: return SW;
            case S: return W;
            case SW: return NW;
            case W: return N;
            case NW: return NE;
        }
        return N;
    }

    public Compass opposite(Compass dir)
    {
        switch (dir)
        {
            case N: return S;
            case NE: return SW;
            case E: return W;
            case SE: return NW;
            case S: return N;
            case SW: return NE;
            case W: return E;
            case NW: return SE;
        }
        return N;
    }
}
