package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.lang.Math;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

//Author: Hannah Simon, HTigee on Git

class CellFunctions extends AgentSQ2Dunstackable<OnLattice2DCells.OnLattice2DGrid>
{
    int color;
    public void Init(int colorIndex)
    {
        this.color = Util.CategorialColor(colorIndex);
    }

    public void StepCell(GridWindow win, List<int[]> occupiedSpaces)
    {
        for (int i = 0; i < occupiedSpaces.size(); i++)
        {
            //win.GetPix(occupiedSpaces.get(i)[0], occupiedSpaces.get(i)[1]);
//            System.out.println(j);
//            System.out.println(Util.CategorialColor(Lymphocytes.colorIndex));
//            System.out.println(Util.CategorialColor(TumorCells.colorIndex));

            if (win.GetPix(occupiedSpaces.get(i)[0], occupiedSpaces.get(i)[1]) == Util.CategorialColor(Lymphocytes.colorIndex))
            {
                if (G.rng.Double() < Lymphocytes.dieProb)
                {
                    Lymphocytes.count--;
                    Dispose();
                }
            }

            else if (win.GetPix(occupiedSpaces.get(i)[0], occupiedSpaces.get(i)[1]) == Util.CategorialColor(TumorCells.colorIndex))
            {
                if (G.rng.Double() < TumorCells.dieProb)
                {
                    win.SetPix(occupiedSpaces.get(i)[0], occupiedSpaces.get(i)[1], Util.CategorialColor(DoomedCells.colorIndex));
                    //this.color = Util.CategorialColor(DoomedCells.colorIndex);
                    TumorCells.count--;
                    DoomedCells.count++;
                }
                else if (G.rng.Double() < (TumorCells.dieProb + TumorCells.divProb))
                {
                    int options = MapEmptyHood(G.divHood);
                    if (options > 0)
                    {
                        G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(TumorCells.colorIndex); //creates a new agent in a random  location in the neighborhood around the cell
                        TumorCells.count++;
                    }
                }
            }

            else if (win.GetPix(occupiedSpaces.get(i)[0], occupiedSpaces.get(i)[1]) == Util.CategorialColor(DoomedCells.colorIndex))
            {
                if (G.rng.Double() < DoomedCells.dieProb)
                {
                    DoomedCells.count--;
                    Dispose();
                }
            }
        }
    }

    public int mapEmptyHood(OnLattice2DGrid G)
    {
        int options = MapEmptyHood(G.divHood);
        return options;
    }

    public void lymphociteMigration(List<int[]> availableSpaces, OnLattice2DGrid G)
    {
        //System.out.println(availableSpaces); //Not necessary, just a check
        int newLymphocytes = (int) (Lymphocytes.tumorInfiltrationRate * TumorCells.count);
        Collections.shuffle(availableSpaces);
        int spacesToPick = Math.min(newLymphocytes, availableSpaces.size()); // Ensure we don’t pick more spaces than available

        for (int i = 0; i < spacesToPick; i++)
        {
            G.NewAgentSQ(availableSpaces.get(i)[0], availableSpaces.get(i)[1]).Init(Lymphocytes.colorIndex);
        }

        Lymphocytes.count += spacesToPick;
    }

    public static double getDecayConstant(String className, String decayConstant) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(decayConstant);
            Object value = field.get(null); // Static field, use 'null' for static access
            return (Double) value;
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

    public static double getSurvivingFraction(double radiationDose, String className, String alpha, String beta) throws Exception
    {
        try
        {
            //double radiationDose = 10;

            //String className = "OnLattice2DCells.Figure4";
            Class<?> clazz = Class.forName(className);

            Field field1 = clazz.getDeclaredField(alpha);
            Object value1 = field1.get(null); // Static field, use 'null' for static access

            Field field2 = clazz.getDeclaredField(beta);
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

    public static double getTumorInfiltrationRate(String className) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField("tumorInfiltratioinRate");
            Object value = field.get(null); // Static field, use 'null' for static access
            return (Double) value;
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

    public static double getPrimaryImmuneResponse(String className, String rateOfCellKilling, String immuneSuppressionEffect) throws Exception
    {
        try
        {
            double primaryImmuneResponse;
            double concentrationAntiPD1_PDL1 = 0;

            Class<?> clazz = Class.forName(className);

            Field field1 = clazz.getDeclaredField(rateOfCellKilling);
            Object value1 = field1.get(null);

            Field field2 = clazz.getDeclaredField(immuneSuppressionEffect);
            Object value2 = field2.get(null);

            return ((Double) value1 * Lymphocytes.count) / (1 + ((Double) value2 * Math.pow(TumorCells.count, 2/3) * Lymphocytes.count) / (1 + concentrationAntiPD1_PDL1));
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

    public static double getTumorGrowthRate(String className) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField("tumorGrowthRate");
            Object value = field.get(null);
            return (Double) value;
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
    public static double divProb = 0;
    public static int colorIndex = 0;
    public static int count = 0;
    public static double survivingFractionL;
    public static double decayConstantOfL;
    public static double tumorInfiltrationRate;

    static
    {
        try
        {
            String fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            decayConstantOfL = CellFunctions.getDecayConstant(fullName, "decayConstantOfL");
            survivingFractionL = CellFunctions.getSurvivingFraction(OnLattice2DGrid.radiationDose, fullName, "radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
            dieProb = 1 - survivingFractionL + survivingFractionL * decayConstantOfL;
            tumorInfiltrationRate = CellFunctions.getTumorInfiltrationRate(fullName);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

abstract class TumorCells implements Cells
{
    public static String name = "Tumor Cells";
    public static double dieProb;
    public static double divProb;
    public static int colorIndex = 1;
    public static int count = 0;
    public static double survivingFractionT;
    public static double primaryImmuneResponse;

    static
    {
        try
        {
            String fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            survivingFractionT = CellFunctions.getSurvivingFraction(OnLattice2DGrid.radiationDose, fullName, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            primaryImmuneResponse = CellFunctions.getPrimaryImmuneResponse(fullName, "rateOfCellKilling", "immuneSuppressionEffect");
            dieProb = 1 - survivingFractionT + survivingFractionT * primaryImmuneResponse;

            divProb = survivingFractionT * (1 - primaryImmuneResponse) + CellFunctions.getTumorGrowthRate(fullName);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

abstract class DoomedCells implements Cells
{
    public static String name = "Doomed Cells";
    public static double dieProb;
    public static double divProb = 0;
    public static int colorIndex = 3;
    public static int count = 0;
    public static double decayConstantOfD;

    static
    {
        try
        {
            String fullName = "OnLattice2DCells." + OnLattice2DGrid.className;
            decayConstantOfD = CellFunctions.getDecayConstant(fullName, "decayConstantOfD");
            dieProb = decayConstantOfD;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
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

    public static int figureCount;
    public static String className = "Figure2";
    public static int radiationDose;

    public static final String directory = "C:\\Users\\Hannah\\Documents\\HALModeling2024Outs\\";
    public static final String fileName = "TrialRun.csv";
    public static final String fullPath = directory + fileName;

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, OnLattice2DCells.CellFunctions.class);
    }

    public void Init()
    {
        //model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(TumorCells.colorIndex);
        int tumorSize = 1; //number of cells in initial tumor before beginning treatment
        int lymphocitePopulation = 1; //number of initial lymphocite cells before beginning treatment

        for (int i = 0; i < tumorSize; i++)
        {
            NewAgentSQ(xDim/2, yDim/2).Init(TumorCells.colorIndex);
//            int options = new OnLattice2DCells.CellFunctions().mapEmptyHood(this);
//            if (options > 0)
//            {
//                this.NewAgentSQ(this.divHood[this.rng.Int(options)]).Init(TumorCells.colorIndex); //creates a new agent in a random  location in the neighborhood around the cell
//                TumorCells.count++;
//            }

        }

        for (int i = 0; i < lymphocitePopulation; i++)
        {
            NewAgentSQ(2, 20).Init(Lymphocytes.colorIndex);
        }

        Lymphocytes.count += lymphocitePopulation;
        TumorCells.count += tumorSize;
    }

    public void StepCells (GridWindow win, List<int[]> occupiedSpaces)
    {
        //loop over every cell in the grid, calls the StepCell method in the cellFunctions class

        for (OnLattice2DCells.CellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell(win, occupiedSpaces);
        }
    }

    public void getAvailableSpaces(GridWindow win)
    {
        List<int[]> availableSpaces = new ArrayList<>(); //This is a list of arrays, each array will store x- and y-coodinate
        for (int i = 0; i < length; i++)
        {
            OnLattice2DCells.CellFunctions cell = GetAgent(i);
            if (cell == null)
            {
                cell = NewAgentSQ(i);
                availableSpaces.add(new int[]{(int) cell.Xpt(),(int) cell.Ypt()});
                //System.out.print((int) cell.Xpt() + " "); //Not necessary, just a check
                //System.out.println((int) cell.Ypt()); //Not necessary, just a check
                cell.Dispose();
            }
        }
        //availableSpaces.forEach(space -> System.out.println(Arrays.toString(space))); //Not necessary, just a check
        //OnLattice2DCells.CellFunctions cell = new OnLattice2DCells.CellFunctions();
        //cell.lymphociteMigration(availableSpaces);
        new OnLattice2DCells.CellFunctions().lymphociteMigration(availableSpaces, this); //Doing the above 2 lines in 1 line
    }

    public List<int[]> getOccupiedSpaces(GridWindow win)
    {
        List<int[]> occupiedSpaces = new ArrayList<>();
        for (int i = 0; i < length; i++)
        {
            OnLattice2DCells.CellFunctions cell = GetAgent(i);
            if (cell != null)
            {
                occupiedSpaces.add(new int[]{(int) cell.Xpt(),(int) cell.Ypt()});
            }
        }
        return occupiedSpaces;
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
        //GifMaker(outputPath,timeBetweenFramesMS,loopContinuously?);
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

    public void saveToCSV(String fullPath, boolean append, int timestep)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep," + Lymphocytes.name + "," + TumorCells.name + "," + DoomedCells.name);
                writer.newLine();
            }
            writer.write(timestep + "," + Lymphocytes.count + "," + TumorCells.count + "," + DoomedCells.count);
            writer.newLine();
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void printPopulation(String name, int colorIndex, int count)
    {
        System.out.println("Population of " + name + " (" + findColor(colorIndex) + "): " + count);
    }

    public static void main (String[] args) throws Exception
    {

        for (figureCount = 1; figureCount < 6; figureCount++)
        {
            if (figureCount == 1)
            {
                className = "Figure2";
            }
            else if (figureCount == 2)
            {
                className = "Figure3";
            }
            else if (figureCount == 3)
            {
                className = "Figure4";
            }
            else if (figureCount == 4)
            {
                className = "Figure5";
            }
            else if (figureCount == 5)
            {
                className = "Figure6";
            }
            else if (figureCount == 6)
            {
                System.exit(0);
            }

            for (radiationDose = 10; radiationDose <= 20; radiationDose += 5)
            {
                System.out.println(className + ":\nRadiation Dose: " + radiationDose);

                int x = 100;
                int y = 100;
                int timesteps = 1000;
                GridWindow win = new GridWindow(x, y, 5);
                OnLattice2DCells.OnLattice2DGrid model = new OnLattice2DCells.OnLattice2DGrid(x, y);

                model.Init();

                int timestep = 0;
                model.saveToCSV(fullPath, false, timestep);
                timestep++;
                //Lymphocytes.dieProb = CellFunctions.survivingFraction("radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
                //TumorCells.dieProb = CellFunctions.survivingFraction("radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");

                for (int i = 0; i < timesteps; i++) //this for loop loops over all the time steps. The model stops running after we finish all timesteps.
                {
                    win.TickPause(1);
                    if (model.Pop() == 0)
                    {
                        timesteps += timestep - 1;

                        model.Init();
                        timestep = 0;
                        model.saveToCSV(fullPath, true, timestep);
                        timestep++;
                    }
                    //I considered adding checks for if either lymphocytes or tumor cells only reaches zero, but not necessary

                    new OnLattice2DCells.CellFunctions().StepCell(win, model.getOccupiedSpaces(win));

                    //model.StepCells(win, model.getOccupiedSpaces(win));
                    model.getAvailableSpaces(win);
                    model.DrawModel(win);

                    model.saveToCSV(fullPath, true, timestep);
                    timestep++;
                }

                model.printPopulation(Lymphocytes.name, Lymphocytes.colorIndex, Lymphocytes.count);
                model.printPopulation(TumorCells.name, TumorCells.colorIndex, TumorCells.count);
                model.printPopulation(DoomedCells.name, DoomedCells.colorIndex, DoomedCells.count);
                System.out.println("Population Total: " + model.Pop());
                System.out.println();
                System.exit(0);
            }
        }
    }
}