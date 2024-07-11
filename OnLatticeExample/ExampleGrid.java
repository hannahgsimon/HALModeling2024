package OnLatticeExample;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Rand;
import HAL.Util;

class ExampleCell extends AgentSQ2Dunstackable<ExampleGrid>
{
    public void StepCell(double dieProb, double divProb)
        if (G.rng.Double() < dieProb)
        {
            //cell will die
            Dispose();
        }

        if (G.rng.Double() < divProb)
        {
            //cell will divide if space is available
            int options = MapEmptyHood(G.divHood);
            if (options > 0)
            {
                G.NewAgentSQ(G.divHood[G.rng.Int(options)]); //creates a new agent in a random  location in the neighborhood around the cell

            }
        }
}

public class ExampleGrid extends AgentGrid2D<ExampleCell>
{
    Rand rng = new Rand();
    double dieProb = 0; //set later
    double divProb = 0; //set later
    int[] divHood = Util.VonNeumannHood(false);
    /*Alternate way to do line above:
    int[] divHood = Util.GenHood2D(new int[]
            {
                1,0,
                -1,0,
                0,1,
                0,-1
            });*/

    public ExampleGrid(int x, int y)
    {
        super(x, y, OnLatticeExample.ExampleCell.class);
    }

    public void StepCells (double dieProb, double divProb)
    {
        //loop over every cell in the grid, calls the StepCell method in the ExampleCell class
        for (ExampleCell cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell(dieProb, divProb);
        }
    }

}
