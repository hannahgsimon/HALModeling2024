package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

//Author: Hannah Simon, HTiigee on Git

abstract class Lymphocytes extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid> implements Cells
{
    public static String name = "Lympocyte Cells";
    public static double dieProb = 0.1;
    public static double divProb = 0.2;
    public static int colorIndex = 2;
    public static int count = 1;

    public void Init(int colorIndex)
    {

    }

    public void StepCell(double dieProb, double divProb)
    {

    }
}

abstract class tumorCells extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid> implements Cells
{
    public static String name = "Tumor Cells";
    public static double dieProb = 0.1;
    public static double divProb = 0.2;
    public static int colorIndex = 1;
    public static int count = 1;
}

abstract class doomedCells extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid> implements Cells
{
    public static String name = "Doomed Cells";
    public static double dieProb = 0.1;
    public static double divProb = 0.2;
    public static int colorIndex = 10;
    public static int count = 1;
}

class cellFunctions extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid>
{
    int color;
    public void Init(int colorIndex)
    {
        //this.color = Util.RGB(G.rng.Double(), G.rng.Double(), G.rng.Double());
        this.color = Util.CategorialColor(colorIndex);
    }

    public void StepCell(double dieProb, double divProb, int colorIndex, int count)
    {
        if (G.rng.Double() < dieProb)
        {
            //cell will die
            Dispose();
            count--;
        }

        if (G.rng.Double() < divProb)
        {
            //cell will divide if space is available
            int options = MapEmptyHood(G.divHood);
            if (options > 0)
            {
                G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(colorIndex); //creates a new agent in a random  location in the neighborhood around the cell
                count++;
            }

        }
    }
}

public class OnLattice2DGrid extends AgentGrid2D<OnLattice2DCells.cellFunctions>
{
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, OnLattice2DCells.cellFunctions.class);
    }

    public void StepCells (double dieProb, double divProb, int colorIndex, int count)
    {
        //loop over every cell in the grid, calls the StepCell method in the cellFunctions class
        for (OnLattice2DCells.cellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell(dieProb, divProb, colorIndex, count);
        }
    }

    public void DrawModel(GridWindow win)
    {
        int color;
        for (int i = 0; i < length; i++)
        {
            /*if(GetAgent(i) != null)
            {
                color = Util.WHITE;
            }*/
            OnLattice2DCells.cellFunctions cell = GetAgent(i);
            if (cell != null)
            {
                color = cell.color;
            }
            else
            {
                color = Util.BLACK;
            }
            win.SetPix(i, color);
        }
    }

    public void printPopulation(String name, int count)
    {
        System.out.println("Total number of " + name + ": " + count);
    }

    public static void main (String[] args)
    {
        int x = 100;
        int y = 100;
        int timesteps = 1000;
        GridWindow win = new GridWindow(x, y, 5);
        OnLattice2DCells.OnLattice2DGrid model = new OnLattice2DCells.OnLattice2DGrid(x, y);
        AgentList model2 = new AgentList();

        //model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(Lymphocytes.colorIndex);
        model.NewAgentSQ(0, 0).Init(Lymphocytes.colorIndex);
        model.NewAgentSQ(10, 10).Init(tumorCells.colorIndex);
        model.NewAgentSQ(20, 20).Init(doomedCells.colorIndex);

        for (int i = 0; i < timesteps; i++) //this for loop loops over all the time steps. The model stops running after we finish all timesteps.
        {
            win.TickPause(20);
//            if (model.Pop() == 0)
//            {
//                model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(Lymphocytes.colorIndex);
//            }
            model.StepCells(Lymphocytes.dieProb, Lymphocytes.divProb, Lymphocytes.colorIndex, Lymphocytes.count);
            model.StepCells(tumorCells.dieProb, tumorCells.divProb, tumorCells.colorIndex, tumorCells.count);
            model.StepCells(doomedCells.dieProb, doomedCells.divProb, doomedCells.colorIndex, doomedCells.count);
            model.DrawModel(win);
        }

        model.printPopulation(Lymphocytes.name, Lymphocytes.count);
        model.printPopulation(tumorCells.name, tumorCells.count);
        model.printPopulation(doomedCells.name, doomedCells.count);
    }
}