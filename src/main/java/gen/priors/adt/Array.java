package gen.priors.adt;

import gen.grid.ColorGrid;
import gen.primitives.Pos;
import gen.priors.abstraction.Attribute;
import gen.priors.abstraction.AttributeExtractor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Array extends ArrayList
{
    public Object last()
    {
        return get(size() - 1);
    }

    public Array firstEq(AttributeExtractor attr)
    {
        Array arr = new Array();
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

    public Object first()
    {
        return get(0);
    }

    public void sort(AttributeExtractor f)
    {
        sort(Comparator.comparing(f::extract));
    }

    Array groupBy(AttributeExtractor f)
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

    public ColorGrid get(Pos pos)
    {
        for(Object o : this)
        {
            if(o instanceof ColorGrid)
            {
                if(((ColorGrid) o).arrayPos.equals(pos))
                {
                    return (ColorGrid) o;
                }
            }
        }

        return null;
    }

    public void filter(AttributeExtractor f)
    {

    }
}
