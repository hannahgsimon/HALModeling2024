package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import HAL.Gui.GifMaker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

//Author: Hannah Simon, hannahgsimon on Git

class CellFunctions extends AgentSQ2Dunstackable<OnLattice2DGrid>
{
    Type type;
    int color;
    int radiationDose;
    boolean radiated;
    boolean deathFromRadiation;
    Double dieProb;
    Double dieProbRad;
    Double dieProbImm;
    Double divProb;
    Double activateProb;

    public enum Type
    {
        LYMPHOCYTE,
        TUMOR,
        DOOMED,
        TRIGGERING
    }

    public void Init (Type type)
    {
        this.type = type;
        this.radiationDose = OnLattice2DGrid.baseRadiationDose;
        if (OnLattice2DGrid.baseRadiationDose == 0)
        {
            this.radiated = false;
        }
        else
        {
            this.radiated = true;
            if (type == Type.TUMOR)
            {
                TumorCells.countRad++;
            }
        }
        this.deathFromRadiation = false;

        if (type == Type.LYMPHOCYTE)
        {
            this.color = Util.CategorialColor(Lymphocytes.colorIndex);
            this.dieProb = Lymphocytes.dieProb;
            this.dieProbRad = null; this.dieProbImm = null; this.divProb = null; this.activateProb = null;
        }
        else if (type == Type.TUMOR)
        {
            this.color = Util.CategorialColor(TumorCells.colorIndex);
            this.dieProb = null;
            this.dieProbRad = TumorCells.dieProbRad;
            this.dieProbImm = TumorCells.dieProbImm;
            this.divProb = TumorCells.divProb;
            this.activateProb = null;

        }
        else if (type == Type.TRIGGERING)
        {
            this.color = Util.CategorialColor(TriggeringCells.colorIndex);
            this.dieProb = TriggeringCells.dieProb;
            this.dieProbRad = null; this.dieProbImm = null; this.divProb = null;
            this.activateProb = TriggeringCells.activateProb;
        }
    }

    public void InitDoomed (boolean radiation)
    {
        this.type = Type.DOOMED;
        this.color = Util.CategorialColor(DoomedCells.colorIndex);
        this.dieProb = DoomedCells.dieProb;
        this.dieProbRad = null; this.dieProbImm = null; this.divProb = null; this.activateProb = null;
        if (radiation)
        {
            this.deathFromRadiation = true;
        }
    }

    public void StepCell()
    {
        if (this.type == Type.LYMPHOCYTE)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Lymphocytes.count--;
                Dispose();
                int[] space = {this.Xsq(), this.Ysq()};
                reduceLymphocyteDensity(G, space);
            }
        }

        else if (this.type == Type.TUMOR)
        {
            if (G.rng.Double() < this.dieProbRad)
            {
                if (this.radiated)
                {
                    TumorCells.countRad--;
                }
                this.InitDoomed(true);
                TumorCells.count--;
                DoomedCells.count++;
                DoomedCells.countRad++;
            }
            else if (G.rng.Double() < (this.dieProbRad + this.dieProbImm))
            {
                if (this.radiated)
                {
                    TumorCells.countRad--;
                }
                this.InitDoomed(false);
                TumorCells.count--;
                DoomedCells.count++;
                DoomedCells.countImm++;
            }
            else if (G.rng.Double() < (this.dieProbRad + this.dieProbImm + this.divProb))
            {
                mapEmptyHood();
            }
        }

        else if (this.type == Type.DOOMED)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Dispose();
                DoomedCells.count--;
                if (this.deathFromRadiation)
                {
                    DoomedCells.countRad--;
                }
                else
                {
                    DoomedCells.countImm--;
                }
            }
        }

        else if (this.type == Type.TRIGGERING)
        {
            if (G.rng.Double() < this.dieProb)
            {
                Dispose();
                TriggeringCells.count--;
                OnLattice2DGrid.triggeringDied = true;;
            }
            else if (G.rng.Double() < (this.dieProb + this.activateProb))
            {
                Dispose();
                TriggeringCells.count--;
                OnLattice2DGrid.triggeringDied = true;
            }
        }
    }

    public void mapEmptyHood()
    {
        int options = MapEmptyHood(G.divHood);
        if (options > 0)
        {
            G.NewAgentSQ(G.divHood[G.rng.Int(options)]).Init(Type.TUMOR);
            TumorCells.count++;
        }
    }

    public void disposeRandomTriggering(OnLattice2DGrid model)
    {
        Collections.shuffle(OnLattice2DGrid.triggeringSpaces);
        model.GetAgent(OnLattice2DGrid.triggeringSpaces.get(0)[0], OnLattice2DGrid.triggeringSpaces.get(0)[1]).Dispose();
        TriggeringCells.count--;
    }

    private static final int[][] DIRECTIONS = {
            {0, 1}, // N
            {1, 1}, // NE
            {1, 0},  // E
            {1, -1},  // SE
            {0, -1},  // S
            {-1, -1}, // SW
            {-1, 0}, // W
            {-1, 1} // NW
    };

    public boolean checkLymphocyteDensity(OnLattice2DGrid model, int[] space)
    {
        int maxNeighbors = 4;
        if (OnLattice2DGrid.lymphocyteNeighbors[space[0]][space[1]] > maxNeighbors)
        {
            return false;
        }
        for (int[] dir : DIRECTIONS)
        {
            int xNeighbor = space[0] + dir[0];
            int yNeighbor = space[1] + dir[1];
            if (xNeighbor >= 0 && xNeighbor < model.xDim && yNeighbor >= 0 && yNeighbor < model.yDim &&
                    model.GetAgent(xNeighbor, yNeighbor) != null && model.GetAgent(xNeighbor, yNeighbor).type == Type.LYMPHOCYTE &&
                    OnLattice2DGrid.lymphocyteNeighbors[xNeighbor][yNeighbor] == maxNeighbors)
            {
                return false;
            }
        }

        for (int[] dir : DIRECTIONS)
        {
            int xNeighbor = space[0] + dir[0];
            int yNeighbor = space[1] + dir[1];
            if (xNeighbor >= 0 && xNeighbor < model.xDim && yNeighbor >= 0 && yNeighbor < model.yDim)
            {
                OnLattice2DGrid.lymphocyteNeighbors[xNeighbor][yNeighbor]++;
            }
        }
        // Note: Each lymphocyte must be added immediately after running this method, or will cause a bug.
        return true;
    }

    public void reduceLymphocyteDensity(OnLattice2DGrid model, int[] space)
    {
        for (int[] dir : DIRECTIONS)
        {
            int xNeighbor = space[0] + dir[0];
            int yNeighbor = space[1] + dir[1];
            if (xNeighbor >= 0 && xNeighbor < model.xDim && yNeighbor >= 0 && yNeighbor < model.yDim)
            {
                OnLattice2DGrid.lymphocyteNeighbors[xNeighbor][yNeighbor]--;
            }
        }
    }

    public void lymphocyteMigration(OnLattice2DGrid G, GridWindow win)
    {
        double volumeDamagedTumorCells = (double) DoomedCells.countRad / (DoomedCells.count + TumorCells.count);
        double survivingFractionT;
        if (OnLattice2DGrid.currentRadiationDose == OnLattice2DGrid.baseRadiationDose)
        {
            survivingFractionT = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
        }
        else
        {
            double survivingFractionTUnradiated = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
            double survivingFractionTRadiated = getSurvivingFraction(OnLattice2DGrid.currentRadiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
            survivingFractionT = (TumorCells.countRad * survivingFractionTRadiated + (TumorCells.count - TumorCells.countRad) * survivingFractionTUnradiated) / TumorCells.count;
        }

        double activation = Math.tanh((1 - survivingFractionT) * volumeDamagedTumorCells);
        OnLattice2DGrid.newLymphocytesAttempted = (int) (FigParameters.tumorInfiltrationRate * TumorCells.count + FigParameters.radiationInducedInfiltration * activation * TriggeringCells.count * TumorCells.count);

        int minDim = Math.min(win.xDim, win.yDim);
        double radiusFraction = 0.75; //Maximum value is 1
        int neighborhoodRadius = (int) Math.max(1, (double) minDim /2 * radiusFraction); // Ensure radius is at least 1

        //Calculate weights and probabilities for each pixel
        double[][] probabilities = new double[win.xDim][win.yDim]; //default value of all entries is initially zero
        double totalProbability = 0;
        List<int[]> availableSpacesInRadius = new ArrayList<>();

        for (int[] availableSpace : OnLattice2DGrid.availableSpaces)
        {
            double weightSum = 0;
            boolean possible = false;
            for (int[] tumorCell : OnLattice2DGrid.tumorSpaces)
            {
                double distance = Math.sqrt(Math.pow(availableSpace[0] - tumorCell[0], 2) + Math.pow(availableSpace[1] - tumorCell[1], 2));
                if (distance <= neighborhoodRadius)
                {
                    double weight = 1.0 / (distance + 1); // Higher weight for closer pixels
                    weightSum += weight;
                    possible = true;
                }
            }
            if (possible)
            {
                availableSpacesInRadius.add(availableSpace);
            }
            probabilities[availableSpace[0]][availableSpace[1]] = weightSum;
            totalProbability += weightSum;
        }

        //Select `spacesToPick` pixels based on the weighted probability distribution
        int spacesToPick = Math.min(OnLattice2DGrid.newLymphocytesAttempted, availableSpacesInRadius.size());
        Random random = new Random();
        List<int[]> selectedPixels = new ArrayList<>();

        int count = 0;
        WhileLoop:
        while (!availableSpacesInRadius.isEmpty() && spacesToPick > 0)
        {
            double rand = totalProbability * random.nextDouble(); //This normalizes the probabilities more efficiently! :)
            double cumulativeProbability = 0.0;
            Iterator<int[]> iterator = availableSpacesInRadius.iterator();
            while (iterator.hasNext())
            {
                int[] space = iterator.next();
                cumulativeProbability += probabilities[space[0]][space[1]];
                if (rand < cumulativeProbability && checkLymphocyteDensity(G, space))
                {
                    selectedPixels.add(space);
                    G.NewAgentSQ(space[0], space[1]).Init(Type.LYMPHOCYTE);
                    Lymphocytes.count++;
                    iterator.remove();
                    //OnLattice2DGrid.availableSpaces.remove(space);
                    totalProbability -= probabilities[space[0]][space[1]];
                    count++;
                    if (count == spacesToPick)
                    {
                        break WhileLoop;
                    }
                    break;
                }
                else if (rand < cumulativeProbability && !checkLymphocyteDensity(G, space))
                {
                    iterator.remove();
                    //OnLattice2DGrid.availableSpaces.remove(space);
                    totalProbability -= probabilities[space[0]][space[1]];
                }
            }
        }
        /* If less lymphocytes are added than what's in spacesToPick, it means that there weren't enough available
        spaces in the radius with max # of lymphocyte neighbors permitted */
    }

    public void randomInitialization(OnLattice2DGrid G, int cellPopulation, Type type)
    {
        int spacesToPick = Math.min(cellPopulation, OnLattice2DGrid.availableSpaces.size());
        Collections.shuffle(OnLattice2DGrid.availableSpaces);

        if (type == Type.LYMPHOCYTE)
        {
            int count = 0;
            for (int i = 0; i < OnLattice2DGrid.availableSpaces.size(); i++)
            {
                int x = OnLattice2DGrid.availableSpaces.get(i)[0];
                int y = OnLattice2DGrid.availableSpaces.get(i)[1];
                int[] space = {x, y};
                if (checkLymphocyteDensity(G, space))
                {
                    G.NewAgentSQ(x, y).Init(type);
                    Lymphocytes.count++;
                    count++;
                }
                if (count == spacesToPick)
                {
                    break;
                }
            }
        }

        else if (type == Type.TRIGGERING)
        {
            for (int i = 0; i < spacesToPick; i++)
            {
                int x = OnLattice2DGrid.availableSpaces.get(i)[0];
                int y = OnLattice2DGrid.availableSpaces.get(i)[1];
                G.NewAgentSQ(x, y).Init(type);
                OnLattice2DGrid.triggeringSpaces.add(new int[]{x, y});
                TriggeringCells.count++;
            }
        }

    }

    public static void getImmuneSuppressionEffectThreshold(boolean init)
    {
        if (init)
        {
            FigParameters.immuneSuppressionEffect =
                    (FigParameters.rateOfCellKilling / (FigParameters.tumorGrowthRate * Math.pow(TumorCells.count, ((double) 2 / 3))));
        }
        else
        {
            FigParameters.immuneSuppressionEffect =
                    (FigParameters.rateOfCellKilling / (FigParameters.tumorGrowthRate * Math.pow(TumorCells.count, ((double) 2 / 3)))
                            - 1 / (Lymphocytes.count * Math.pow(TumorCells.count, ((double) 2 / 3))));
        }
    }

    public static void getImmuneResponse()
    {
        double concentrationAntiPD1_PDL1 = 0;
        OnLattice2DGrid.primaryImmuneResponse = ((Double) FigParameters.rateOfCellKilling * Lymphocytes.count) /
                (1 + ((FigParameters.immuneSuppressionEffect * Math.pow(TumorCells.count, ((double) 2 / 3)) * Lymphocytes.count) / (1 + concentrationAntiPD1_PDL1)));

        double concentrationAntiCTLA4 = 0;
        double sensitivityFactorZs = 0.0314;
        int NormalizationFactor = 5;
        OnLattice2DGrid.secondaryImmuneResponse += sensitivityFactorZs * ((1 + concentrationAntiCTLA4) /
                (NormalizationFactor + concentrationAntiCTLA4)) * OnLattice2DGrid.primaryImmuneResponse;

        OnLattice2DGrid.immuneResponse = OnLattice2DGrid.primaryImmuneResponse + OnLattice2DGrid.secondaryImmuneResponse;
    }

    public static double getSurvivingFraction(double radiationDose, double alpha, double beta)
    {
        return Math.exp(alpha * -radiationDose - beta * Math.pow(radiationDose, 2));
    }

    public static double getLymphocytesProb(int radiationDose)
    {
        double survivingFractionL = getSurvivingFraction(radiationDose,FigParameters.radiationSensitivityOfLymphocytesAlpha, FigParameters.radiationSensitivityOfLymphocytesBeta);
        return 1 - survivingFractionL + (survivingFractionL * FigParameters.decayConstantOfL);
    }

    public static double[] getTumorCellsProb(int radiationDose)
    {
        double survivingFractionT = getSurvivingFraction(radiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
        double dieProbRad = 1 - survivingFractionT;
        double dieProbImm = survivingFractionT * OnLattice2DGrid.immuneResponse;
        double divProb = survivingFractionT * (1 - OnLattice2DGrid.immuneResponse) * FigParameters.tumorGrowthRate;
        return new double[]{dieProbRad, dieProbImm, divProb};
    }

    public static double[] getTriggeringCellsProb(int radiationDose)
    {
        double volumeDamagedTumorCells = (double) DoomedCells.countRad / (DoomedCells.count + TumorCells.count);
        double survivingFractionTUnradiated = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
        double survivingFractionTRadiated = getSurvivingFraction(OnLattice2DGrid.appliedRadiationDose, FigParameters.radiationSensitivityOfTumorCellsAlpha, FigParameters.radiationSensitivityOfTumorCellsBeta);
        TriggeringCells.SurvivingFractionTLast = (TumorCells.countRad * survivingFractionTRadiated + (TumorCells.count - TumorCells.countRad) * survivingFractionTUnradiated) / TumorCells.count;

        double activation = Math.tanh((1 - TriggeringCells.SurvivingFractionTLast) * volumeDamagedTumorCells);
        double survivingFractionL = getSurvivingFraction(radiationDose,FigParameters.radiationSensitivityOfLymphocytesAlpha, FigParameters.radiationSensitivityOfLymphocytesBeta);
        double survivingFractionI =  survivingFractionL;
        double dieProb = (1 - survivingFractionI) * (1 - FigParameters.recoveryConstantOfA);
        double activateProb = (1 - survivingFractionI) * FigParameters.recoveryConstantOfA * activation + survivingFractionI * activation;
        return new double[]{dieProb, activateProb};
    }
}

class Lymphocytes
{
    public static String name = "Lymphocyte Cells";
    public static double dieProb;
    public static int colorIndex = 0;
    public static int count;

    public void Lymphocytes()
    {
        count = 0;
    }
}

class TumorCells
{
    public static String name = "Tumor Cells";
    public static double dieProbRad;
    public static double dieProbImm;
    public static double divProb;
    public static int colorIndex = 1;
    public static int count, countRad;

    public void TumorCells()
    {
        count = 0; countRad = 0;
    }
}

class DoomedCells
{
    public static String name = "Doomed Cells";
    public static String nameRad = "Doomed Cells Radiation";
    public static String nameImm = "Doomed Cells Immune";
    public static double dieProb;
    public static int colorIndex = 3;
    public static int count, countRad, countImm;

    public void DoomedCells()
    {
        count = 0; countRad = 0; countImm = 0;
        dieProb = FigParameters.decayConstantOfD;
    }
}

class TriggeringCells
{
    public static String name = "Triggering Cells";
    public static double dieProb;
    public static double activateProb;
    public static int colorIndex = 2;
    public static int count;
    public static double SurvivingFractionTLast;

    public void TriggeringCells()
    {
        count = 0;
    }
}

class FigParameters
{
    int figure;
    public static double radiationSensitivityOfTumorCellsAlpha; //null
    public static double radiationSensitivityOfTumorCellsBeta;  //null
    public static double radiationSensitivityOfLymphocytesAlpha; //null
    public static double radiationSensitivityOfLymphocytesBeta; //null
    public static double tumorGrowthRate;
    public static double tumorInfiltrationRate;
    public static double rateOfCellKilling;
    public static double decayConstantOfD;
    public static double decayConstantOfL;
    public static double recoveryConstantOfA;
    public static double radiationInducedInfiltration; //null
    public static double immuneSuppressionEffect;

    public FigParameters(int figure)
    {
        this.figure = figure;
        if (figure == 2)
        {
            radiationSensitivityOfTumorCellsAlpha = 0; //null
            radiationSensitivityOfTumorCellsBeta = 0;  //null
            radiationSensitivityOfLymphocytesAlpha = 0; //null
            radiationSensitivityOfLymphocytesBeta = 0; //null
            tumorGrowthRate = 0.217;
            tumorInfiltrationRate = 0.1;
            rateOfCellKilling = 0.05;
            decayConstantOfD = 0.039;
            decayConstantOfL = 0.335;
            recoveryConstantOfA = 0.039;
            radiationInducedInfiltration = 0; //null
            immuneSuppressionEffect = 0.031;
        }
        else if (figure == 3)
        {
            radiationSensitivityOfTumorCellsAlpha = 0.05;
            radiationSensitivityOfTumorCellsBeta = 0.0114;
            radiationSensitivityOfLymphocytesAlpha = 0.182;
            radiationSensitivityOfLymphocytesBeta = 0.143;
            tumorGrowthRate = 0.217;
            tumorInfiltrationRate = 0.05; //in original ODE model, is 0.5
            rateOfCellKilling = 0.135;
            decayConstantOfD = 0.045;
            decayConstantOfL = 0.045;
            recoveryConstantOfA = 0.045;
            radiationInducedInfiltration = 0; //null
            immuneSuppressionEffect = 0.100;
        }
        else if (figure == 4)
        {
            radiationSensitivityOfTumorCellsAlpha = 0.05;
            radiationSensitivityOfTumorCellsBeta = 0.0114;
            radiationSensitivityOfLymphocytesAlpha = 0.182;
            radiationSensitivityOfLymphocytesBeta = 0.143;
            tumorGrowthRate = 0.217;
            tumorInfiltrationRate = 0.5;
            rateOfCellKilling = 0.135;
            decayConstantOfD = 0.045;
            decayConstantOfL = 0.045;
            recoveryConstantOfA = 0.045;
            radiationInducedInfiltration = 300;
            immuneSuppressionEffect = 1.1;
        }
        else if (figure == 5)
        {
            radiationSensitivityOfTumorCellsAlpha = 0.05;
            radiationSensitivityOfTumorCellsBeta = 0.0114;
            radiationSensitivityOfLymphocytesAlpha = 0.182;
            radiationSensitivityOfLymphocytesBeta = 0.143;
            tumorGrowthRate = 0.217;
            tumorInfiltrationRate = 0.5;
            rateOfCellKilling = 0.135;
            decayConstantOfD = 0.045;
            decayConstantOfL = 0.045;
            recoveryConstantOfA = 0.045;
            radiationInducedInfiltration = 300;
            immuneSuppressionEffect = 1.1;
        }
        else if (figure == 6)
        {
            radiationSensitivityOfTumorCellsAlpha = 0.214;
            radiationSensitivityOfTumorCellsBeta = 0.0214;
            radiationSensitivityOfLymphocytesAlpha = 0.182;
            radiationSensitivityOfLymphocytesBeta = 0.143;
            tumorGrowthRate = 0.03;
            tumorInfiltrationRate = 0.1;
            rateOfCellKilling = 0.004;
            decayConstantOfD = 0.045;
            decayConstantOfL = 0.056;
            recoveryConstantOfA = 0.045;
            radiationInducedInfiltration = 4.6;
            immuneSuppressionEffect = 0.5;
        }
        else
        {
            System.err.println("Figure " + figure + " is not a valid figure number.");
            System.exit(0);
        }
    }
}

class ScenarioParameters
{
    char scenario;
    public ScenarioParameters(char scenario)
    {
        this.scenario = scenario;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
        String dateTime = LocalDateTime.now().format(formatter);
        String fileNameWithDate = OnLattice2DGrid.fileName1.replace(".csv", "_" + dateTime + ".csv");
        OnLattice2DGrid.fullPath1 = OnLattice2DGrid.directory + "Scenario" + scenario + "\\" + OnLattice2DGrid.fileName1.replace(".csv", "_" + dateTime + ".csv");

        if (scenario == 'A')
        {
            OnLattice2DGrid.figure = 2;
            new FigParameters(OnLattice2DGrid.figure);
            OnLattice2DGrid.totalRadiation = false; OnLattice2DGrid.centerRadiation = false; OnLattice2DGrid.spatialRadiation = false;
            FigParameters.immuneSuppressionEffect = 0.031;
        }
        else if (scenario == 'B')
        {
            OnLattice2DGrid.figure = 3;
            new FigParameters(OnLattice2DGrid.figure);
            OnLattice2DGrid.totalRadiation = false; OnLattice2DGrid.centerRadiation = false; OnLattice2DGrid.spatialRadiation = false;
            FigParameters.immuneSuppressionEffect = 0.1;
        }
        else if (scenario == 'C')
        {
            OnLattice2DGrid.figure = 3;
            new FigParameters(OnLattice2DGrid.figure);
            OnLattice2DGrid.baseRadiationDose = 0;
            OnLattice2DGrid.appliedRadiationDose = 10;
            OnLattice2DGrid.radiationTimesteps = List.of(200);
            OnLattice2DGrid.totalRadiation = false; OnLattice2DGrid.centerRadiation = true; OnLattice2DGrid.spatialRadiation = false;
            OnLattice2DGrid.targetPercentage = 0.7;
            FigParameters.immuneSuppressionEffect = 0.1;
        }
        else if (scenario == 'D')
        {
            OnLattice2DGrid.figure = 3;
            new FigParameters(OnLattice2DGrid.figure);
            OnLattice2DGrid.baseRadiationDose = 0;
            OnLattice2DGrid.appliedRadiationDose = 10;
            OnLattice2DGrid.radiationTimesteps = List.of(200);
            OnLattice2DGrid.totalRadiation = false; OnLattice2DGrid.centerRadiation = true; OnLattice2DGrid.spatialRadiation = false;
            OnLattice2DGrid.targetPercentage = .85;
            FigParameters.immuneSuppressionEffect = 0.1;
        }
        else if (scenario == 'E')
        {
            OnLattice2DGrid.figure = 3;
            new FigParameters(OnLattice2DGrid.figure);
            OnLattice2DGrid.baseRadiationDose = 0;
            OnLattice2DGrid.appliedRadiationDose = 10;
            OnLattice2DGrid.radiationTimesteps = List.of(200);
            OnLattice2DGrid.totalRadiation = false; OnLattice2DGrid.centerRadiation = true; OnLattice2DGrid.spatialRadiation = false;
            OnLattice2DGrid.targetPercentage = 1;
            FigParameters.immuneSuppressionEffect = 0.1;
        }
        else
        {
            System.err.printf("Invalid scenario: %s.%nPlease provide a valid scenario (A, B, C, D, E) or set 'scenarioActive' to false.%n", scenario);
            System.exit(0);
        }
    }
}

public class OnLattice2DGrid extends AgentGrid2D<CellFunctions>
{
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);

    public static int figure = 2;
    public static int baseRadiationDose = 0, currentRadiationDose = baseRadiationDose, appliedRadiationDose = 10;
    public static List<Integer> radiationTimesteps = List.of(200);
    public static boolean totalRadiation = false, centerRadiation = true, spatialRadiation = false;
    public static double targetPercentage = 0.7;
    public static double thresholdPercentage = 0.8; public static int radius = 10;
    public static boolean scenarioActive = false; public static char scenario = 'A';

    public static double immuneResponse, primaryImmuneResponse, secondaryImmuneResponse = 0;
    public static int newLymphocytesAttempted;
    public static boolean triggeringDied;
    public static boolean immuneSuppressionEffectThreshold = false;

    public static List<int[]> availableSpaces = new ArrayList<>();
    public static List<int[]> tumorSpaces = new ArrayList<>();
    public static List<int[]> triggeringSpaces = new ArrayList<>();
    public static List<int[]> lymphocyteSpaces = new ArrayList<>();
    public static List<int[]> radiatedPixels = new ArrayList<>();
    public static List<int[]> allPixels = new ArrayList<>();
    public static int[][] lymphocyteNeighbors;

    public static final String directory = "C:\\Users\\Hannah\\Documents\\HALModeling2024Outs\\";
    public static final String fileName1 = "TrialRunCounts.csv";
    public static String fullPath1 = directory + fileName1;
    public static final String fileName2 = "TrialRunProbabilities.csv";
    public static final String fullPath2 = directory + fileName2;
    public static final String fileName3 = "LymphocyteNeighbors.csv";
    public static final String fullPath3 = directory + fileName3;
    public static final boolean printCounts = false, printProbabilities = false, printNeighbors = false;
    public static boolean writeGIF = false;

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, CellFunctions.class);
    }

    public void Init(GridWindow win, OnLattice2DGrid model)
    {
        if ((totalRadiation && centerRadiation) ||
                (totalRadiation && spatialRadiation) ||
                (centerRadiation && spatialRadiation))
        {
            System.err.println("Two types of radiation are on; choose one for the model to run, or will not run as intended.");
            System.exit(0);
        }
        if (centerRadiation && (targetPercentage <= 0 || targetPercentage > 1))
        {
            System.err.println(
                    "Error: Target percentage for center radiation must be greater than 0 and less than or equal to 1.\n" +
                            "Current values:\n" +
                            "  Center Radiation: " + centerRadiation + "\n" +
                            "  Target Percentage: " + targetPercentage + "\n" +
                            "Please update the targetPercentage to a valid value.");
            System.exit(0);
        }
        else if (spatialRadiation && (thresholdPercentage <= 0 || thresholdPercentage > 1))
        {
            System.err.println(
                    "Error: Threshold percentage for spatial radiation must be greater than 0 and less than or equal to 1.\n" +
                            "Current values:\n" +
                            "  Spatial Radiation: " + spatialRadiation + "\n" +
                            "  Threshold Percentage: " + thresholdPercentage + "\n" +
                            "Please update the thresholdPercentage to a valid value.");
            System.exit(0);
        }
        else if (spatialRadiation && (radius <= 0 || radius > xDim/2 || radius > yDim/2))
        {
            System.err.println(
                    "Error: Radius for spatial radiation must be greater than 0 and less than or equal to half the grid dimensions.\n" +
                            "Current values:\n" +
                            "  Spatial Radiation: " + spatialRadiation + "\n" +
                            "  Radius: " + radius + "\n" +
                            "  Grid Dimensions: xDim = " + xDim + ", yDim = " + yDim + "\n" +
                            "Please update the radius to a valid value.");
            System.exit(0);
        }

        lymphocyteNeighbors = new int[model.xDim][model.yDim];
        int lymphocitePopulation = 0;
        int tumorSize = 1;
        int triggeringPopulation = 500;
        if (lymphocitePopulation + tumorSize + triggeringPopulation > model.xDim * model.yDim)
        {
            System.err.println("Error: Number of cells exceeds grid size.\n" +
                    "Maximum Grid Capacity: " + (model.xDim * model.yDim) + " cells");
            System.exit(0);
        }

        currentRadiationDose = baseRadiationDose;
        Lymphocytes.dieProb = CellFunctions.getLymphocytesProb(baseRadiationDose);
        if (lymphocitePopulation > 0)
        {
            updateSpaces(win);
            if (tumorSize > 0)
            {
                OnLattice2DGrid.availableSpaces.removeIf(arr -> arr[0] == xDim/2 && arr[1] == yDim/2);

            }
            new CellFunctions().randomInitialization(this, lymphocitePopulation, CellFunctions.Type.LYMPHOCYTE);
        }

        TumorCells.count += tumorSize;
        if (immuneSuppressionEffectThreshold)
        {
            CellFunctions.getImmuneSuppressionEffectThreshold(Lymphocytes.count <= 1);
        }
        CellFunctions.getImmuneResponse();
        double[] Tvalues = CellFunctions.getTumorCellsProb(baseRadiationDose);
        TumorCells.count -= tumorSize;
        TumorCells.dieProbRad = Tvalues[0]; TumorCells.dieProbImm = Tvalues[1]; TumorCells.divProb = Tvalues[2];

        if (tumorSize > 0)
        {
            model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(CellFunctions.Type.TUMOR);
            TumorCells.count++;
        }
        if (tumorSize > 1)
        {
            for (int i = 0; i < tumorSize; i++)
            {
                for (CellFunctions cell:this)
                {
                    cell.mapEmptyHood();
                    if (TumorCells.count == tumorSize)
                    {
                        i = tumorSize;
                        break;
                    }
                }
            }
        }

        double[] Avalues = CellFunctions.getTriggeringCellsProb(baseRadiationDose);
        TriggeringCells.dieProb = Avalues[1]; TriggeringCells.activateProb = Avalues[1];
        if (triggeringPopulation > 0)
        {
            updateSpaces(win);
            new CellFunctions().randomInitialization(this, triggeringPopulation, CellFunctions.Type.TRIGGERING);
        }
    }

    public void StepCells (OnLattice2DGrid model)
    {
        triggeringDied = false;
        for (CellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell();
        }
        if (TriggeringCells.count > 0 && !triggeringDied)
        {
            new CellFunctions().disposeRandomTriggering(model);
        }
    }

    public void updateSpaces(GridWindow win)
    {
        availableSpaces.clear(); tumorSpaces.clear(); triggeringSpaces.clear(); lymphocyteSpaces.clear();

        for (int i = 0; i < length; i++)
        {
            CellFunctions cell = GetAgent(i);
            if (cell == null)
            {
                cell = NewAgentSQ(i);
                availableSpaces.add(new int[]{cell.Xsq(),cell.Ysq()});
                cell.Dispose();
            }
            else if (cell.type == CellFunctions.Type.TUMOR)
            {
                tumorSpaces.add(new int[]{cell.Xsq(), cell.Ysq()});
            }
            else if (cell.type == CellFunctions.Type.TRIGGERING)
            {
                triggeringSpaces.add(new int[]{cell.Xsq(), cell.Ysq()});
            }
            else if (cell != null && cell.type == CellFunctions.Type.LYMPHOCYTE)
            {
                lymphocyteSpaces.add(new int[]{cell.Xsq(), cell.Ysq()});
            }
            /* Didn't put condition for doomed cell spaces because not necessary for this algorithm. If add it later,
            then need to add the condition cell != null and just make availableSpaces last condition
             */
        }
    }

    public void DrawModelandUpdateProb(GridWindow win, GifMaker gif)
    {
        int color;

        if (immuneSuppressionEffectThreshold)
        {
            CellFunctions.getImmuneSuppressionEffectThreshold(Lymphocytes.count <= 1);
        }
        CellFunctions.getImmuneResponse();
        double[] Tvalues = CellFunctions.getTumorCellsProb(baseRadiationDose);
        TumorCells.dieProbRad = Tvalues[0]; TumorCells.dieProbImm = Tvalues[1]; TumorCells.divProb = Tvalues[2];
        double[] Avalues = CellFunctions.getTriggeringCellsProb(baseRadiationDose);
        TriggeringCells.dieProb = Avalues[0]; TriggeringCells.activateProb = Avalues[1];

        for (int i = 0; i < length; i++)
        {
            CellFunctions cell = GetAgent(i);
            if (cell != null)
            {
                color = cell.color;
                if (cell.type == CellFunctions.Type.TUMOR)
                {
                    cell.dieProbRad = TumorCells.dieProbRad;
                    cell.dieProbImm = TumorCells.dieProbImm;
                    cell.divProb = TumorCells.divProb;
                    //If radiating twice in a row, this is not needed only for tumor cells in the circle. But not worth writing code for.
                }
                else if (cell.type == CellFunctions.Type.TRIGGERING)
                {
                    cell.dieProb = TriggeringCells.dieProb;
                    cell.activateProb = TriggeringCells.activateProb;
                }
            }
            else
            {
                color = Util.BLACK;
            }
            win.SetPix(i, color);
        }
        if (writeGIF) gif.AddFrame(win);
    }

    public int[] getTumorCoord()
    {
        int minX = tumorSpaces.get(0)[0];
        int maxX = tumorSpaces.get(tumorSpaces.size() - 1)[0];
        int minY = yDim;
        int maxY = 0;
        for (int[] tumorCell : tumorSpaces)
        {
            if (tumorCell[1] < minY) minY = tumorCell[1];
            if (tumorCell[1] > maxY) maxY = tumorCell[1];
        }
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;
        return new int[]{minX, maxX, minY, maxY, centerX, centerY};
    }

    public void centerRadiationArea(GridWindow win, int[] tumorCoord)
    {
        //int centerX = xDim/2; int centerY = yDim/2;
        int targetPixelsInCircle = (int) (TumorCells.count * targetPercentage);

        int radius = 0;
        for (int testRadius = (int) Math.sqrt(targetPixelsInCircle / Math.PI); testRadius < xDim/2; testRadius++)
        {
            radiatedPixels.clear();
            int count = 0;
            for (int i = tumorCoord[0]; i <= tumorCoord[1]; i++)
            {
                for (int j = tumorCoord[2]; j <= tumorCoord[3]; j++)
                {
                    if (isInsideCircle(i, j, tumorCoord[4], tumorCoord[5], testRadius))
                    {
                        radiatedPixels.add(new int[]{i, j});
                        if (GetAgent(i, j) != null && GetAgent(i, j).type == CellFunctions.Type.TUMOR)
                        {
                            count++;
                        }
                    }
                }
            }
            if (count >= targetPixelsInCircle)
            {
                radius = testRadius;
                break;
            }
        }
    }

    public void spatialRadiationArea(GridWindow win, int[] tumorCoord)
    {
        List<int[]> tumorCenters = new ArrayList<>();
        List<int[]> combinedDirections = new ArrayList<>();
        int minX, maxX, minY, maxY;

        if (tumorCoord[4] - radius - 1  >= 0 && tumorCoord[4] + radius + 1  < xDim &&
                tumorCoord[5] - radius - 1  >= 0 && tumorCoord[5] + radius + 1  < yDim)
        {
            tumorCenters.add(new int[]{tumorCoord[4], tumorCoord[5]});
            minX = tumorCoord[4]; maxX = tumorCoord[4]; minY = tumorCoord[5]; maxY = tumorCoord[5];
        }
        else
        {
            System.out.println("Grid isn't big enough for spatial radiation with radius of " + radius);
            return;
        }

        final int[][] directions = {
                {0, 1}, // N
                {1, 0},  // E
                {0, -1},  // S
                {-1, 0}, // W
        };

        for (int i = 0; i < 4; i++)
        {
            int count = 0;
            while (true)
            {
                int xOffset = directions[i][0] * ((count + 1) * 2 + (2 * (count + 1)) * radius);
                int yOffset = directions[i][1] * ((count + 1) * 2 + (2 * (count + 1)) * radius);
                int newX = tumorCoord[4] + xOffset;
                int newY = tumorCoord[5] + yOffset;

                if (newX - radius - 1 >= 0 && newX + radius + 1 < xDim &&
                        newY - radius - 1 >= 0 && newY + radius + 1 < yDim)
                {
                    tumorCenters.add(new int[]{newX, newY});
                    combinedDirections.add(new int[]{newX, newY}); // Store for diagonal checking
                    if (newX < minX)
                        minX = newX;
                    else if (newX > maxX)
                        maxX = newX;
                    if (newY < minY)
                        minY = newY;
                    else if (newY > maxY)
                        maxY = newY;
                    count++;
                }
                else
                {
                    break;
                }
            }
        }

        // Check additional combinations for diagonal overlaps
        for (int[] point1 : combinedDirections)
        {
            for (int[] point2 : combinedDirections)
            {
                if (point1 != point2)
                {
                    int diagonalX = point1[0];
                    int diagonalY = point2[1];
                    if (diagonalX - radius - 1 >= 0 && diagonalX + radius + 1 < xDim &&
                            diagonalY - radius - 1 >= 0 && diagonalY + radius + 1 < yDim &&
                            diagonalX != tumorCoord[4] && diagonalY != tumorCoord[5])
                    {
                        tumorCenters.add(new int[]{diagonalX, diagonalY});
                    }
                }
            }
        }

        minX = minX - radius; maxX = maxX + radius; minY = minY - radius; maxY = maxY + radius;

        //for (int[] center : tumorCenters) {System.out.println(Arrays.toString(center));}

        int numCenters = tumorCenters.size();
        List<int[]>[] radiatedPixelCircle = new ArrayList[numCenters];
        for (int k = 0; k < numCenters; k++)
        {
            radiatedPixelCircle[k] = new ArrayList<>();
        }
        int[] tumorCount = new int[numCenters];
        int[] doomedCount = new int[numCenters];

        for (int i = minX; i <= maxX; i++)
        {
            for (int j = minY; j <= maxY; j++)
            {
                for (int k = 0; k < numCenters; k++)
                {
                    if (isInsideCircle(i, j, tumorCenters.get(k)[0], tumorCenters.get(k)[1], radius))
                    {
                        radiatedPixelCircle[k].add(new int[]{i, j});
                        if (GetAgent(i, j) != null && GetAgent(i, j).type == CellFunctions.Type.TUMOR)
                        {
                            tumorCount[k]++;
                        }
                        else if (GetAgent(i, j) != null && GetAgent(i, j).type == CellFunctions.Type.DOOMED)
                        {
                            doomedCount[k]++;
                        }
                        break; // No need to check other centers if this one matches
                    }
                }
            }
        }

        int count = 0;
        System.out.println("Attempting spatial radiation. " + numCenters + " circles being checked.");
        for (int k = 0; k < numCenters; k++)
        {
            if ((double) (tumorCount[k] + doomedCount[k]) / radiatedPixelCircle[k].size() >= thresholdPercentage)
            {
                radiatedPixels.addAll(radiatedPixelCircle[k]);
                count++;
            }
        }
        System.out.println("Circles radiated: " + count + "\n");

        //for (int[] pixel : radiatedPixels) {win.SetPix(pixel[0], pixel[1], Util.GREEN);}
    }

    public static boolean isInsideCircle(int i, int j, int centerX, int centerY, int radius)
    {
        int dx = i - centerX;
        int dy = j - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public void radiationApplied()
    {
        currentRadiationDose = appliedRadiationDose;
        double LDieProb = CellFunctions.getLymphocytesProb(currentRadiationDose);
        double[] Tvalues = CellFunctions.getTumorCellsProb(currentRadiationDose);
        double[] Avalues = CellFunctions.getTriggeringCellsProb(currentRadiationDose);

        for (int[] pixel : radiatedPixels)
        {
            CellFunctions cell = GetAgent(pixel[0], pixel[1]);
            if (cell != null)
            {
                cell.radiationDose = currentRadiationDose;
                if (cell.type == CellFunctions.Type.LYMPHOCYTE)
                {
                    cell.dieProb = LDieProb;
                }
                else if (cell.type == CellFunctions.Type.TUMOR)
                {
                    cell.dieProbRad = Tvalues[0]; cell.dieProbImm = Tvalues[1]; cell.divProb = Tvalues[2];
                    if (!cell.radiated)
                    {
                        TumorCells.countRad++;
                    }
                }
                else if (cell.type == CellFunctions.Type.TRIGGERING)
                {

                    cell.dieProb = Avalues[0]; cell.activateProb = Avalues[1];
                }
                cell.radiated = true;
            }
        }
    }

    public void radiationUnapplied()
    {
        currentRadiationDose = baseRadiationDose;

        for (int[] pixel : radiatedPixels)
        {
            CellFunctions cell = GetAgent(pixel[0], pixel[1]);
            if (cell != null)
            {
                cell.radiationDose = currentRadiationDose;
                if (cell.type == CellFunctions.Type.LYMPHOCYTE)
                {
                    cell.dieProb = Lymphocytes.dieProb;
                }
            }
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
        else if (colorIndex == 19)
        {
            return "dark gray";
        }
        return "unknown color";
    }

    public void saveCountsToCSV(String fullPath1, boolean append, int timestep)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath1, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep,Lymphocytes,TriggeringCells,TumorCells,TumorCellsRad,DoomedCells," +
                        "DoomedCellsRad,Lymphocytes DieProb,Tumor DieProbRad,Tumor DieProbImm,Tumor DivProb," +
                        "SurvivingFractionTLast,PrimaryImmuneResponse,SecondaryImmuneResponse,ImmuneResponse," +
                        "LymphocyteMigrationAttempted,ImmuneSuppression");
                writer.newLine();
            }
            writer.write(timestep + "," + Lymphocytes.count + "," + TriggeringCells.count + "," + TumorCells.count + "," + TumorCells.countRad + "," +
                    DoomedCells.count + "," + DoomedCells.countRad + "," + Lymphocytes.dieProb + "," + TumorCells.dieProbRad + "," +
                    TumorCells.dieProbImm + "," + TumorCells.divProb + "," + TriggeringCells.SurvivingFractionTLast + "," +
                    OnLattice2DGrid.primaryImmuneResponse + "," + OnLattice2DGrid.secondaryImmuneResponse + "," +
                    OnLattice2DGrid.immuneResponse + "," + OnLattice2DGrid.newLymphocytesAttempted  + "," + FigParameters.immuneSuppressionEffect);
            writer.newLine();
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void saveProbabilitiesToCSV(String fullPath2, boolean append, int timestep, boolean duringRadiation)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath2, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep,Cell,Type,Color,Radiated,RadiationDose,DeathFromRadiation," +
                        "DieProb,ActivateProb,DieProbRad,DieProbImm,DivProb,LymphocyteNeighbors");
                writer.newLine();
            }

            if (duringRadiation)
            {
                writer.write("Before Radiation Effects\n");
            }

            if (timestep >= 0)
            {
                for (int i = 0; i < length; i++)
                {
                    OnLattice2DCells.CellFunctions cell = GetAgent(i);
                    if (cell != null)
                    {
                        writer.write(timestep + "," + cell + "," + cell.type + "," + cell.color + "," + cell.radiated + "," +
                                cell.radiationDose + "," + cell.deathFromRadiation + "," + cell.dieProb + "," + cell.activateProb + "," +
                                cell.dieProbRad + "," + cell.dieProbImm + "," + cell.divProb + "," + lymphocyteNeighbors[cell.Xsq()][cell.Ysq()]);
                        writer.newLine();
                    }
                }
            }

            if (duringRadiation)
            {
                writer.write("\nAfter Radiation Effects");
            }

            writer.newLine();
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void saveLymphocyteNeighborstoCSV(String fullPath3, boolean append, int timestep)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath3, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep,Type,Lymphocyte Neighbors");
                writer.newLine();
            }
            if (timestep >= 0)
            {
                for (int i = 0; i < length; i++)
                {
                    OnLattice2DCells.CellFunctions cell = GetAgent(i);
                    if (cell != null)
                    {
                        writer.write(timestep + "," + cell.type + "," + lymphocyteNeighbors[cell.Xsq()][cell.Ysq()]);
                        writer.newLine();
                    }
                    else
                    {
                        cell = NewAgentSQ(i);
                        writer.write(timestep + ",empty," + lymphocyteNeighbors[cell.Xsq()][cell.Ysq()]);
                        writer.newLine();
                        cell.Dispose();
                    }
                }
                /* Alternate Visual Format:
                writer.write(timestep  + ",");
                for (int i = 0; i < xDim; i++)
                {
                    writer.write(i + (i < xDim - 1 ? "," : ""));
                }
                writer.newLine();
                for (int j = 0; j < yDim; j++)
                {
                    writer.write(j + "");
                    for (int i = 0; i < xDim; i++)
                    {
                        String cellContent;
                        OnLattice2DCells.CellFunctions cell = GetAgent(i, j);
                        if (cell != null)
                        {
                            cellContent = cell.type + " " + lymphocyteNeighbors[i][j];
                        }
                        else
                        {
                            cellContent = String.valueOf(lymphocyteNeighbors[i][j]);
                        }
                        writer.write("," + cellContent);
                    }
                    writer.newLine();
                }*/
            }

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

    public static void main (String[] args)
    {
        System.out.print("Scenario Active: " + scenarioActive);
        if (scenarioActive)
        {
            System.out.print("    Scenario: " + scenario);
            new ScenarioParameters(scenario);
        }
        else
        {
            new FigParameters(figure);
        }
        System.out.println("\nFigure: " + figure + "\nTotal Radiation: " + totalRadiation +
                "   Center Radiation: " + centerRadiation + "    Spatial Radiation: " + spatialRadiation);
        if (totalRadiation || centerRadiation || spatialRadiation)
        {
            System.out.println("Base Radiation Dose: " + baseRadiationDose + " Gy\nApplied Radiation Dose: " + appliedRadiationDose + " Gy" +
                    "\nTimesteps Applied: " + radiationTimesteps);
        }
        if (centerRadiation)
        {
            System.out.println("Center radiation target percentage is " + targetPercentage);
        }
        else if (spatialRadiation)
        {
            System.out.println("Spatial radiation threshold percentage is " + thresholdPercentage + " and preset radius is " + radius);
        }
        if (!immuneSuppressionEffectThreshold)
        {
            System.out.println("Immune Suppression Effect: " + FigParameters.immuneSuppressionEffect);
        }
        System.out.println("\nSave Counts to CSV: " + printCounts +"   Save Probabilities to CSV: " + printProbabilities +
                "   Save GIF (slows code down): " + writeGIF + "\n");

        int x = 100;
        int y = 100;
        int timesteps = 1000;
        GridWindow win = new GridWindow(x, y, 5);
        OnLattice2DGrid model = new OnLattice2DGrid(x, y);
        for (int i = 0; i < model.xDim; i++)
        {
            for (int j = 0; j < model.yDim; j++)
            {
                allPixels.add(new int[]{i, j});
            }
        }

        new Lymphocytes().Lymphocytes();
        new TumorCells().TumorCells();
        new DoomedCells().DoomedCells();
        new TriggeringCells().TriggeringCells();

        model.Init(win, model);
        if (printCounts) model.saveCountsToCSV(fullPath1, false, 0);
        if (printProbabilities) model.saveProbabilitiesToCSV(fullPath2, false, 0, false);
        if (printNeighbors) model.saveLymphocyteNeighborstoCSV(fullPath3, false, 0);

        GifMaker gif = new GifMaker(directory + "TrialRunGif.gif",1,false);

        for (int i = 1; i <= timesteps; i++)
        {
            win.TickPause(1);

            if (radiationTimesteps.contains(i) && TumorCells.count > 20)
            {
                if (totalRadiation)
                {
                    radiatedPixels.addAll(allPixels);
                    model.radiationApplied();
                }
                else if (centerRadiation)
                {
                    model.centerRadiationArea(win, new OnLattice2DGrid(x, y).getTumorCoord());
                    model.radiationApplied();
                }
                else if (spatialRadiation)
                {
                    model.spatialRadiationArea(win, new OnLattice2DGrid(x, y).getTumorCoord());
                    model.radiationApplied();
                }
                if (printProbabilities) model.saveProbabilitiesToCSV(fullPath2, true, i, true);

            }
            else if (radiationTimesteps.contains(i - 1))
            {
                model.radiationUnapplied();
                radiatedPixels.clear();
            }

            model.StepCells(model);

            model.updateSpaces(win);
            if (TriggeringCells.count > 0)
            {
                new CellFunctions().lymphocyteMigration(model, win);
            }

            if (printCounts) model.saveCountsToCSV(fullPath1, true, i);
            if (printProbabilities) model.saveProbabilitiesToCSV(fullPath2, true, i, false);
            if (printNeighbors) model.saveLymphocyteNeighborstoCSV(fullPath3, true, i);

            if (i == timesteps) writeGIF = true;
            model.DrawModelandUpdateProb(win, gif); //get occupied spaces to use for stepCells method, rerun if model pop goes to 0

            //if (model.Pop() == 0)
            if (TumorCells.count == 0)
            {
                System.out.println("Timestep tumor population reached 0: " + i + "\n");
                break;
                /*model.Init(win, model);
                if (printCounts) model.saveCountsToCSV(fullPath1, true, 0);
                if (printProbabilities) model.saveProbabilitiesToCSV(fullPath2, true, 0, win, false);
                if (printNeighbors) model.saveLymphocyteNeighborstoCSV(fullPath3, true, 0);
                i = 1;*/
            }
        }

        gif.Close();

        model.printPopulation(Lymphocytes.name, Lymphocytes.colorIndex, Lymphocytes.count);
        model.printPopulation(TumorCells.name, TumorCells.colorIndex, TumorCells.count);
        model.printPopulation(DoomedCells.name, DoomedCells.colorIndex, DoomedCells.count);
        model.printPopulation(TriggeringCells.name, TriggeringCells.colorIndex, TriggeringCells.count);
        System.out.println("Population Total: " + model.Pop());
        model.updateSpaces(win);
        System.out.println("Unoccupied Spaces: " +  availableSpaces.size());
        System.out.println();
    }
}