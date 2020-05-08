package util;

public class T2<A, B>
{
    A a;
    B b;

    public A getA()
    {
        return a;
    }

    public void setA(A a)
    {
        this.a = a;
    }

    public B getB()
    {
        return b;
    }

    public void setB(B b)
    {
        this.b = b;
    }

    public T2(A a, B b)
    {
        this.a = a;
        this.b = b;
    }

    @Override
    public String toString()
    {
        return "{" + "a=" + a + ", b=" + b + '}';
    }
}
