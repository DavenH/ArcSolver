package gen.priors.adt;

import gen.grid.ColorGrid;
import gen.primitives.Pos;
import gen.priors.abstraction.Attribute;
import gen.priors.abstraction.AttributeExtractor;
import gen.priors.abstraction.ComparableAttr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Array<T> extends ArrayList<T>
{
    public T last()
    {
        return get(size() - 1);
    }

    public Array<T> firstEq(AttributeExtractor attr)
    {
        Array<T> arr = new Array();
        Attribute lastAttr = attr.extract(first());

        for(int i = 1; i < size(); ++i)
        {
            if(attr.extract(get(i)).equals(lastAttr))
                arr.add(get(i));
        }

        return arr;
    }

    public Array lastEq(AttributeExtractor attr)
    {
        Array arr = new Array();
        Attribute lastAttr = attr.extract(last());

        for(int i = size() - 2; i >= 0; --i)
        {
            if(attr.extract(get(i)).equals(lastAttr))
                arr.add(get(i));
        }

        return arr;
    }

    public T first()
    {
        return get(0);
    }

    public void sort(AttributeExtractor attr)
    {
        sort(Comparator.comparing(attr::extract));
    }

    public T max(AttributeExtractor extractor)
    {
        T maximum = null;
        ComparableAttr maxAttr = null;

        for(T t : this)
        {
            if(maximum == null)
            {
                maximum = t;
                maxAttr = extractor.extract(t);
            }
            else
            {
                ComparableAttr attr = extractor.extract(t);
                if(maxAttr.compareTo(attr) < 0)
                {
                    maxAttr = attr;
                    maximum = t;
                }
            }
        }

        return maximum;
    }

    Array<Array<T>> groupBy(AttributeExtractor f)
    {
        Map<Attribute, Array> map = new HashMap<>();

        for(Object o : this)
        {
            Attribute attr = f.extract(o);
            Array old = map.getOrDefault(attr, new Array());
            old.add(o);
            map.put(attr, old);
        }

        Array arrays = new Array();
        map.forEach((attr, arr) -> arrays.add(arr));
        return arrays;
    }

    public T get(Pos pos)
    {
        for(T o : this)
        {
            if(o instanceof ColorGrid)
            {
                if(((ColorGrid) o).arrayPos.equals(pos))
                {
                    return o;
                }
            }
        }

        throw new RuntimeException("Array does not contain color grids, or none have this array position " + pos.toString());
    }

    public void filter(AttributeExtractor f)
    {

    }
}
