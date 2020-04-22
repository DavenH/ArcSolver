package sci;

import problem.Task;

import java.util.*;

/**
 * A model of scientific method control flow
 */
public class ScientificMethod
{
    Model model;

    public ScientificMethod()
    {
        model = new Model();
    }

    // what is curious?
    // answers to what is curious can become hypotheses
    // validated hypotheses become theories

    public void iterate(Task task)
    {
        boolean outputReconstructed = false;
        int boredom = 0;
        double curiosity = 0;
        Set<Hypothesis<?, ?>> hypotheses = new HashSet<>();

        while(! outputReconstructed)
        {
            List<Task.Sample> samples = task.getTrainSamples();

            List<Set<Entity>> allEntitySetsIn = new ArrayList<>();
            List<Set<Entity>> allEntitySetsOut = new ArrayList<>();

            for(int i = 0; i < samples.size(); ++i)
            {
                allEntitySetsIn.add(Collections.emptySet());
                allEntitySetsOut.add(Collections.emptySet());
            }

            Set<Entity> commonInputEntities = new HashSet<>();

            int entityBoredom = 0;
            while(commonInputEntities.isEmpty())
            {
                for(int i = 0; i < samples.size(); ++i)
                {
                    Set<Entity> entities = model.findEntities(samples.get(i).input, allEntitySetsIn.get(i), entityBoredom);
                    allEntitySetsIn.set(i, entities);
                }

                if(allEntitySetsIn.isEmpty())
                    break;  // don't know what this case is, but it's baad

                commonInputEntities = new HashSet<>(allEntitySetsIn.get(0));
                for(int i = 1; i < allEntitySetsIn.size(); ++i)
                    commonInputEntities.retainAll(allEntitySetsIn.get(i));

                ++boredom;
            }

            entityBoredom = 0;

            Set<Entity> commonOutputEntities = new HashSet<>();
            while(commonOutputEntities.isEmpty())
            {
                for(int i = 0; i < samples.size(); ++i)
                {
                    Set<Entity> entities = model.findEntities(samples.get(i).output, commonInputEntities,
                                                              allEntitySetsOut.get(i), entityBoredom);
                    allEntitySetsOut.set(i, entities);
                }

                if(allEntitySetsOut.isEmpty())
                    break;  // don't know what this case is, but it's baad

                commonOutputEntities = new HashSet<>(allEntitySetsOut.get(0));
                for(int i = 1; i < allEntitySetsOut.size(); ++i)
                    commonOutputEntities.retainAll(allEntitySetsOut.get(i));

                ++boredom;
            }



            Set<Transformation> transformations = model.findMappings(commonInputEntities, commonOutputEntities);
        }
    }
}
