package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.lang.Math;
import java.lang.reflect.Field;

//Author: Hannah Simon, HTiigee on Git

class CellFunctions extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid>
{
    int color;
    public void Init(int colorIndex)
    {
        this.color = Util.CategorialColor(colorIndex);
    }

    public void StepCell(double dieProb, double divProb, int colorIndex)
    {
        if (G.rng.Double() < dieProb)
        {
            Dispose();
            if (this.color == Util.CategorialColor(Lymphocytes.colorIndex))
            {
                Lymphocytes.count--;
            }
            else if (this.color == Util.CategorialColor(TumorCells.colorIndex))
            {
                TumorCells.count--;
                DoomedCells.count++;
                this.color = Util.CategorialColor(DoomedCells.colorIndex);
            }
            else
            {
                DoomedCells.count--;
            }
        }

        if (G.rng.Double() < divProb)
        {
            int options = MapEmptyHood(G.divHood);
            if (options > 0)
            {
                G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(colorIndex); //creates a new agent in a random  location in the neighborhood around the cell
                if (this.color == Util.CategorialColor(Lymphocytes.colorIndex))
                {
                    Lymphocytes.count++;
                }
                else if (this.color == Util.CategorialColor(TumorCells.colorIndex))
                {
                    TumorCells.count++;
                }
                else
                {
                    DoomedCells.count++;
                }
            }
        }
    }

    public static double survivingFraction(double radiationDose, String className, String alpha, String beta) throws Exception
    {
        try
        {
            double radiationDose = 10;

            String className = "OnLattice2DCells.Figure4";
            Class<?> clazz = Class.forName(className);

            Field field1 = clazz.getDeclaredField("radiationSensitivityOfLymphocytesAlpha");
            Object value1 = field1.get(null); // Static field, use 'null' for static access

            Field field2 = clazz.getDeclaredField("radiationSensitivityOfLymphocytesBeta");
            Object value2 = field2.get(null);

            return Math.exp((Double) value1 * -radiationDose - (Double) value2 * Math.pow(radiationDose, 2));
            //return (Double) (-x.radiationSensitivityOfLymphocytesAlpha * radiationDose - radiationSensitivityOfLymphocytesBeta * Math.pow(radiationDose, 2));
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Class not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (NoSuchFieldException e)
        {
            System.err.println("Field not found: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
        catch (Exception e)
        {
            System.err.println("Error during reflection: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}

class Lymphocytes
{
    public static String name = "Lymphocyte Cells";
    public static double dieProb;
    public static double survivingFractionL;

    static
    {
        try
        {
            survivingFractionL = CellFunctions.survivingFraction("radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
            dieProb = 1 - survivingFractionL + survivingFractionL * decayConstantOfL;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static double divProb = 1 - dieProb;
    //public static double divProb = tumorGrowthRate;
    public static int colorIndex = 0;
    public static int count = 0;
}

abstract class TumorCells implements Cells
{
    public static String name = "Tumor Cells";
    public static double dieProb = 0.1;
    public static double divProb = 0.2;
    public static int colorIndex = 1;
    public static int count = 0;
}

abstract class DoomedCells implements Cells
{
    public static String name = "Doomed Cells";
    public static double dieProb = 0.1;
    public static double divProb = 0.2;
    public static int colorIndex = 3;
    public static int count = 0;
}

abstract class Figure2 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0; //null
    public static double radiationSensitivityOfTumorCellsBeta = 0;  //null
    public static double radiationSensitivityOfLymphocytesAlpha = 0; //null
    public static double radiationSensitivityOfLymphocytesBeta = 0; //null
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltratioinRate = 0.1;
    public static double rateOfCellKilling = 0.05;
    public static double decayConstantOfD = 0.039;
    public static double decayConstantOfL = 0.335;
    public static double recoveryConstantOfA = 0.039;
    public static double radiationInducedInfiltration = 0; //null
    public static double immuneSuppressionEffect = 0.012;
}

abstract class Figure3 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltratioinRate = 0.5;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 0; //null
    public static double immuneSuppressionEffect = 0.51;
}

abstract class Figure4 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltratioinRate = 0.5;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 300;
    public static double immuneSuppressionEffect = 1.1;
}

abstract class Figure5 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltratioinRate = 0.5;
    public static double rateOfCellKilling = 0.135;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.045;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 300;
    public static double immuneSuppressionEffect = 1.1;
}

abstract class Figure6 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.214;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0214;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.03;
    public static double tumorInfiltratioinRate = 0.1;
    public static double rateOfCellKilling = 0.004;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.056;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 4.6;
    public static double immuneSuppressionEffect = 0.5;
}

public class OnLattice2DGrid extends AgentGrid2D<OnLattice2DCells.CellFunctions>
{
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, OnLattice2DCells.CellFunctions.class);
    }

    public void Init()
    {
        //model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(Lymphocytes.colorIndex);
        NewAgentSQ(20, 20).Init(Lymphocytes.colorIndex);
        NewAgentSQ(10, 10).Init(TumorCells.colorIndex);
        NewAgentSQ(0, 0).Init(DoomedCells.colorIndex);
        Lymphocytes.count++;
        TumorCells.count++;
        DoomedCells.count++;
    }

    public void StepCells (double dieProb, double divProb, int colorIndex)
    {
        //loop over every cell in the grid, calls the StepCell method in the cellFunctions class
        for (OnLattice2DCells.CellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell(dieProb, divProb, colorIndex);
        }
    }

    public void DrawModel(GridWindow win)
    {
        int color;
        for (int i = 0; i < length; i++)
        {
            OnLattice2DCells.CellFunctions cell = GetAgent(i);
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

    public String findColor(int colorIndex)
    {
        if (colorIndex == 0)
        {
            return "blue";
        }
        else if (colorIndex == 1)
        {
            return "red";
        }
        else if (colorIndex == 2)
        {
            return "green";
        }
        else if (colorIndex == 3)
        {
            return "yellow";
        }
        else if (colorIndex == 4)
        {
            return "orange";
        }
        else if (colorIndex == 5)
        {
            return "cyan";
        }
        else if (colorIndex == 6)
        {
            return "pink";
        }
        else if (colorIndex == 7)
        {
            return "blue";
        }
        else if (colorIndex == 8)
        {
            return "brown";
        }
        else if (colorIndex == 9)
        {
            return "light blue";
        }
        else if (colorIndex == 10)
        {
            return "light red";
        }
        else if (colorIndex == 11)
        {
            return "light green";
        }
        else if (colorIndex == 12)
        {
            return "light yellow";
        }
        else if (colorIndex == 13)
        {
            return "light purple";
        }
        else if (colorIndex == 14)
        {
            return "light orange";
        }
        else if (colorIndex == 15)
        {
            return "light cyan";
        }
        else if (colorIndex == 16)
        {
            return "light pink";
        }
        else if (colorIndex == 17)
        {
            return "light brown";
        }
        else if (colorIndex == 18)
        {
            return "light gray";
        }
        else
        {
            return "dark gray";
        }
    }

    public void printPopulation(String name, int colorIndex, int count)
    {
        System.out.println("Total number of " + name + " (" + findColor(colorIndex) + "): " + count);
    }

    public static void main (String[] args) throws Exception {
        int x = 100;
        int y = 100;
        int timesteps = 1000;
        GridWindow win = new GridWindow(x, y, 5);
        OnLattice2DCells.OnLattice2DGrid model = new OnLattice2DCells.OnLattice2DGrid(x, y);
        AgentList model2 = new AgentList();

        model.Init();
        //Lymphocytes.dieProb = CellFunctions.survivingFraction("radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
        //TumorCells.dieProb = CellFunctions.survivingFraction("radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");

        for (int i = 0; i < timesteps; i++) //this for loop loops over all the time steps. The model stops running after we finish all timesteps.
        {
            win.TickPause(20);
            if (model.Pop() == 0)
            {
                model.Init();
            }
            model.StepCells(Lymphocytes.dieProb, Lymphocytes.divProb, Lymphocytes.colorIndex);
            model.StepCells(TumorCells.dieProb, TumorCells.divProb, TumorCells.colorIndex);
            model.StepCells(DoomedCells.dieProb, DoomedCells.divProb, DoomedCells.colorIndex);
            model.DrawModel(win);
        }

        model.printPopulation(Lymphocytes.name, Lymphocytes.colorIndex, Lymphocytes.count);
        model.printPopulation(TumorCells.name, TumorCells.colorIndex, TumorCells.count);
        model.printPopulation(DoomedCells.name, DoomedCells.colorIndex, DoomedCells.count);
    }
}