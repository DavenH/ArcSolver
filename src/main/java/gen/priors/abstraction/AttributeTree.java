package gen.priors.abstraction;

import gen.grid.ColorGrid;
import gen.grid.Grid;
import gen.priors.adt.Array;

public class AttributeTree
{
    Grid root;

    public AttributeTree(ColorGrid root)
    {
        this.root = root;
    }

    public void build(Grid node)
    {
        // test transitional entropy
        // test autocorrelation on meta-entropy
        // assess indicators ...
        // remove lattice
        //


        Array<Grid> children = new Array<>();

        // if found multiple color grids:
        for(Grid child : children)
        {
            node.addChild(child);
            build(child);
        }
    }

//    public void calculateCorrelations();
}
