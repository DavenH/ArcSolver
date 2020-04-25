package gen.priors.abstraction;

public class ValueCategoricalAttr<T> implements CategoricalAttr
{
    T value;
    public T getValue() { return value; }

    public ValueCategoricalAttr() {}

    public ValueCategoricalAttr(T value)
    {
        this.value = value;
    }

    @Override
    public boolean equals(CategoricalAttr other)
    {
        return other instanceof ValueCategoricalAttr && ((ValueCategoricalAttr) other).getValue().equals(value);
    }
}
