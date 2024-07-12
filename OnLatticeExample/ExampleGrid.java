package OnLatticeExample;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;

class ExampleCell extends AgentSQ2Dunstackable<ExampleGrid>
{
    int color;
    public void Init()
    {
        this.color = Util.RGB(G.rng.Double(), G.rng.Double(), G.rng.Double());
    }

    public void StepCell(double dieProb, double divProb) {
        if (G.rng.Double() < dieProb)
        {
            //cell will die
            Dispose();
            (ExampleGrid.count)--;
        }

        if (G.rng.Double() < divProb)
        {
            //cell will divide if space is available
            int options = MapEmptyHood(G.divHood);
            if (options > 0)
            {
                G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(); //creates a new agent in a random  location in the neighborhood around the cell
                (ExampleGrid.count)++;
            }

        }
    }
}

public class ExampleGrid extends AgentGrid2D<ExampleCell>
{
    public static int count = 1; //this keeps track of the # of cells
    Rand rng = new Rand();
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

    public void DrawModel(GridWindow win)
    {
        int color;
        for (int i = 0; i < length; i++)
        {
            /*if(GetAgent(i) != null)
            {
                color = Util.WHITE;
            }*/
            ExampleCell cell = GetAgent(i);
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

    public static void main (String[] args)
    {
        int x = 100;
        int y = 100;
        int timesteps = 1000;
        double dieProb = 0.1;
        double divProb = 0.2;
        GridWindow win = new GridWindow(x, y, 5);
        ExampleGrid model = new ExampleGrid(x, y);
        AgentList model2 = new AgentList();

        //initialize model
        model.NewAgentSQ(model.xDim/2, model.yDim/2).Init();

        for (int i = 0; i < timesteps; i++) //this for loop loops over all the time steps. The model stops running after we finish all timesteps.
        {
            win.TickPause(2);
            if (model.Pop() == 0)
            {
                model.NewAgentSQ(model.xDim/2, model.yDim/2).Init();
            }
            model.StepCells(dieProb, divProb);  //run the StepCells method
            model.DrawModel(win);   //run the DrawModel method
            /*if ((ExampleGrid.count) == 0)
            {
                System.err.println("All cells died by chance; restarting the program. Loading...");
                //win.Close(); cannot have this unless I edit the UIWindow class to not exit the program on line 72, for example by asking the user
                main(args);
            }*/
        }
        System.out.println("Total number of cells: " + ExampleGrid.count);
        System.out.println("Total number of cells: " + model.Pop());
        //System.out.println("Total number of cells: " + model.AllAgents().size()); doesn't work because also counts the cells that died last run
    }
}