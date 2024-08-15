package OnLattice2DCells;

import HAL.GridsAndAgents.AgentGrid2D;
import HAL.GridsAndAgents.AgentList;
import HAL.GridsAndAgents.AgentSQ2Dunstackable;
import HAL.Gui.GridWindow;
import HAL.Rand;
import HAL.Util;
import java.lang.Math;
import java.lang.reflect.Field;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import HAL.Gui.GifMaker;
import java.util.*;

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
            }
            else if (G.rng.Double() < (this.dieProb + this.activateProb))
            {
                Dispose();
                TriggeringCells.count--;
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

    public void lymphocyteMigration(OnLattice2DGrid G, GridWindow win) throws Exception
    {
        double volumeDamagedTumorCells = (double) DoomedCells.countRad / (DoomedCells.count + TumorCells.count);
        double survivingFractionT;
        if (OnLattice2DGrid.currentRadiationDose == OnLattice2DGrid.baseRadiationDose)
        {
            survivingFractionT = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
        }
        else
        {
            double survivingFractionTUnradiated = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            double survivingFractionTRadiated = getSurvivingFraction(OnLattice2DGrid.currentRadiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            survivingFractionT = (TumorCells.countRad * survivingFractionTRadiated + (TumorCells.count - TumorCells.countRad) * survivingFractionTUnradiated) / TumorCells.count;
        }

        double activation = Math.tanh((1 - survivingFractionT) * volumeDamagedTumorCells);
        OnLattice2DGrid.newLymphocytesAttempted = (int) (Lymphocytes.tumorInfiltrationRate * TumorCells.count + getRadiationInducedInfiltration() * activation * TriggeringCells.count * TumorCells.count);

        int minDim = Math.min(win.xDim, win.yDim);
        double radiusFraction = 0.25;
        int neighborhoodRadius = (int) Math.max(1, minDim * radiusFraction); // Ensure radius is at least 1

        //Calculate weights and probabilities for each pixel
        double[][] probabilities = new double[win.xDim][win.yDim]; //default value of all entries is initially zero
        double totalProbability = 0;
        List<int[]> availableSpaces1InRadius = new ArrayList<>();
        for (int[] availableSpace : OnLattice2DGrid.availableSpaces1)
        {
            double weightSum = 0;
            boolean possible = false;
            for (int[] tumorCell : OnLattice2DGrid.tumorSpaces1)
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
                availableSpaces1InRadius.add(availableSpace);
            }
            probabilities[availableSpace[0]][availableSpace[1]] = weightSum;
            totalProbability += weightSum;
        }

        //Select `spacesToPick` pixels based on the weighted probability distribution
        int spacesToPick = Math.min(OnLattice2DGrid.newLymphocytesAttempted, availableSpaces1InRadius.size());
        Random random = new Random();
        List<int[]> selectedPixels = new ArrayList<>();

        for (int i = 0; i < spacesToPick; i++)
        {
            double rand = totalProbability * random.nextDouble(); //This normalizes the probabilities more efficiently! :)
            double cumulativeProbability = 0.0;
            for (int[] availableSpaceInRadius : availableSpaces1InRadius)
            {
                cumulativeProbability += probabilities[availableSpaceInRadius[0]][availableSpaceInRadius[1]];
                if (rand < cumulativeProbability)
                {
                    selectedPixels.add(availableSpaceInRadius);
                    availableSpaces1InRadius.remove(availableSpaceInRadius);
                    totalProbability -= probabilities[availableSpaceInRadius[0]][availableSpaceInRadius[1]];
                    break;
                }
            }
        }
        //if selectedPixels.size() < spacesToPick, means that no available spaces are within the neighborhoodRadius

        //Lymphocyte Migration
        for (int[] pixel : selectedPixels)
        {
            G.NewAgentSQ(pixel[0], pixel[1]).Init(Type.LYMPHOCYTE);
            Lymphocytes.count++;
        }
    }

    public void randomInitialization(OnLattice2DGrid G, int cellPopulation, Type type)
    {
        int spacesToPick = Math.min(cellPopulation, OnLattice2DGrid.availableSpaces.size());
        Collections.shuffle(OnLattice2DGrid.availableSpaces);
        for (int i = 0; i < spacesToPick; i++)
        {
            G.NewAgentSQ(OnLattice2DGrid.availableSpaces.get(0)[0], OnLattice2DGrid.availableSpaces.get(0)[1]).Init(type);
            OnLattice2DGrid.availableSpaces.removeIf(arr -> arr[0] == OnLattice2DGrid.availableSpaces.get(0)[0] && arr[1] == OnLattice2DGrid.availableSpaces.get(0)[1]);
        }
        if (type == Type.LYMPHOCYTE)
        {
            Lymphocytes.count += spacesToPick;
        }
        else if (type == Type.TRIGGERING)
        {
            TriggeringCells.count += spacesToPick;
        }
    }

    public static double getTumorGrowthRate() throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);
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

    public static double getTumorInfiltrationRate() throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);
            Field field = clazz.getDeclaredField("tumorInfiltrationRate");
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

    public static double getDecayConstant(String decayConstant) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);
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

    public static double getRecoveryConstantOfA() throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);
            Field field = clazz.getDeclaredField("recoveryConstantOfA");
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

    public static double getRadiationInducedInfiltration() throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);
            Field field = clazz.getDeclaredField("radiationInducedInfiltration");
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

    public static void getImmuneResponse() throws Exception
    {
        try
        {
            double concentrationAntiPD1_PDL1 = 0;

            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);

            Field field1 = clazz.getDeclaredField("rateOfCellKilling");
            Object value1 = field1.get(null);

            Field field2 = clazz.getDeclaredField("immuneSuppressionEffect");
            Object value2 = field2.get(null);

            OnLattice2DGrid.primaryImmuneResponse = ((Double) value1 * Lymphocytes.count) / (1 + (((Double) value2 * Math.pow(TumorCells.count, ((double) 2 / 3)) * Lymphocytes.count) / (1 + concentrationAntiPD1_PDL1)));

            double concentrationAntiCTLA4 = 0;
            double sensitivityFactorZs = 0.0314;
            int NormalizationFactor = 5;
            OnLattice2DGrid.secondaryImmuneResponse += sensitivityFactorZs * ((1 + concentrationAntiCTLA4) / (NormalizationFactor + concentrationAntiCTLA4)) * OnLattice2DGrid.primaryImmuneResponse;

            OnLattice2DGrid.immuneResponse = OnLattice2DGrid.primaryImmuneResponse + OnLattice2DGrid.secondaryImmuneResponse;
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

    public static double getSurvivingFraction(double radiationDose, String alpha, String beta) throws Exception
    {
        try
        {
            Class<?> clazz = Class.forName(OnLattice2DGrid.fullName);

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

    public static double getLymphocytesProb(int radiationDose) throws Exception
    {
        try
        {
            double survivingFractionL = getSurvivingFraction(radiationDose,"radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
            return 1 - survivingFractionL + (survivingFractionL * Lymphocytes.decayConstantOfL);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static double[] getTumorCellsProb(int radiationDose) throws Exception
    {
        try
        {
            double survivingFractionT = getSurvivingFraction(radiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            double dieProbRad = 1 - survivingFractionT;
            double dieProbImm = survivingFractionT * OnLattice2DGrid.immuneResponse;
            double divProb = survivingFractionT * (1 - OnLattice2DGrid.immuneResponse) * TumorCells.tumorGrowthRate;
            return new double[]{dieProbRad, dieProbImm, divProb};
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static double[] getTriggeringCellsProb(int radiationDose) throws Exception
    {
        try
        {
            double volumeDamagedTumorCells = (double) DoomedCells.countRad / (DoomedCells.count + TumorCells.count);
            double survivingFractionTUnradiated = getSurvivingFraction(OnLattice2DGrid.baseRadiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            double survivingFractionTRadiated = getSurvivingFraction(OnLattice2DGrid.appliedRadiationDose, "radiationSensitivityOfTumorCellsAlpha", "radiationSensitivityOfTumorCellsBeta");
            TriggeringCells.SurvivingFractionTLast = (TumorCells.countRad * survivingFractionTRadiated + (TumorCells.count - TumorCells.countRad) * survivingFractionTUnradiated) / TumorCells.count;

            double activation = Math.tanh((1 - TriggeringCells.SurvivingFractionTLast) * volumeDamagedTumorCells);
            double survivingFractionL = getSurvivingFraction(radiationDose,"radiationSensitivityOfLymphocytesAlpha", "radiationSensitivityOfLymphocytesBeta");
            double survivingFractionI =  survivingFractionL;
            double dieProb = (1 - survivingFractionI) * (1 - TriggeringCells.recoveryConstantOfA);
            double activateProb = (1 - survivingFractionI) * TriggeringCells.recoveryConstantOfA * activation + survivingFractionI * activation;
            return new double[]{dieProb, activateProb};
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class Lymphocytes
{
    public static String name = "Lymphocyte Cells";
    public static double dieProb;
    public static int colorIndex = 0;
    public static int count;
    public static double decayConstantOfL;
    public static double tumorInfiltrationRate;
    public static double radiationInducedInfiltration;

    public void Lymphocytes()
    {
        count = 0;
        try
        {
            decayConstantOfL = CellFunctions.getDecayConstant("decayConstantOfL");
            tumorInfiltrationRate = CellFunctions.getTumorInfiltrationRate();
            radiationInducedInfiltration = CellFunctions.getRadiationInducedInfiltration();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
    public static double tumorGrowthRate;

    public void TumorCells()
    {
        count = 0; countRad = 0;
        try
        {
            tumorGrowthRate = CellFunctions.getTumorGrowthRate();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
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
    public static double decayConstantOfD;

    public void DoomedCells()
    {
        count = 0; countRad = 0; countImm = 0;
        try
        {
            decayConstantOfD = CellFunctions.getDecayConstant("decayConstantOfD");
            dieProb = decayConstantOfD;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}

class TriggeringCells
{
    public static String name = "Triggering Cells";
    public static double dieProb;
    public static double activateProb;
    public static int colorIndex = 2;
    public static int count;
    public static double recoveryConstantOfA;
    public static double SurvivingFractionTLast;

    public void TriggeringCells()
    {
        count = 0;
        try
        {
            recoveryConstantOfA = CellFunctions.getRecoveryConstantOfA();
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
    public static double tumorInfiltrationRate = 0.1;
    public static double rateOfCellKilling = 0.05;
    public static double decayConstantOfD = 0.039;
    public static double decayConstantOfL = 0.335;
    public static double recoveryConstantOfA = 0.039;
    public static double radiationInducedInfiltration = 0; //null
    public static double immuneSuppressionEffect = 0.015;
}

abstract class Figure3 implements ModelParameters
{
    public static double radiationSensitivityOfTumorCellsAlpha = 0.05;
    public static double radiationSensitivityOfTumorCellsBeta = 0.0114;
    public static double radiationSensitivityOfLymphocytesAlpha = 0.182;
    public static double radiationSensitivityOfLymphocytesBeta = 0.143;
    public static double tumorGrowthRate = 0.217;
    public static double tumorInfiltrationRate = 0.05;
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
    public static double tumorInfiltrationRate = 0.5;
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
    public static double tumorInfiltrationRate = 0.5;
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
    public static double tumorInfiltrationRate = 0.1;
    public static double rateOfCellKilling = 0.004;
    public static double decayConstantOfD = 0.045;
    public static double decayConstantOfL = 0.056;
    public static double recoveryConstantOfA = 0.045;
    public static double radiationInducedInfiltration = 4.6;
    public static double immuneSuppressionEffect = 0.5;
}

public class OnLattice2DGrid extends AgentGrid2D<CellFunctions>
{
    Rand rng = new Rand();
    int[] divHood = Util.VonNeumannHood(false);

    public static String className = "Figure3";
    public static String fullName = "OnLattice2DCells." + className;
    public static int baseRadiationDose, currentRadiationDose, appliedRadiationDose;
    public static List<Integer> radiationTimesteps = List.of(200, 300, 500, 800);
    public static boolean spatialRadiation = true;

    public static double immuneResponse, primaryImmuneResponse, secondaryImmuneResponse = 0;
    public static int newLymphocytesAttempted;

    public static List<int[]> availableSpaces = new ArrayList<>();
    public static List<int[]> availableSpaces1 = new ArrayList<>();
    public static List<int[]> tumorSpaces = new ArrayList<>();
    public static List<int[]> tumorSpaces1 = new ArrayList<>();
    public static List<int[]> pixelsInCircle = new ArrayList<>();

    public static final String directory = "C:\\Users\\Hannah\\Documents\\HALModeling2024Outs\\";
    public static final String fileName1 = "TrialRunCounts.csv";
    public static final String fullPath1 = directory + fileName1;
    public static final String fileName2 = "TrialRunProbabilities.csv";
    public static final String fullPath2 = directory + fileName2;

    public OnLattice2DGrid(int x, int y)
    {
        super(x, y, CellFunctions.class);
    }

    public void Init(GridWindow win, OnLattice2DGrid model) throws Exception
    {
        //model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(TumorCells.colorIndex);
        int lymphocitePopulation = 0;
        int tumorSize = 0;
        int triggeringPopulation = 50;
        if (lymphocitePopulation + tumorSize + triggeringPopulation > model.xDim * model.yDim)
        {
            System.err.println("Error: Number of cells exceeds grid size.\n" +
                    "Maximum Grid Capacity: " + (model.xDim * model.yDim) + " cells");
            System.exit(0);
        }

        for (int x = 0; x < xDim; x++)
        {
            for (int y = 0; y < yDim; y++)
            {
                availableSpaces.add(new int[]{x, y});
            }
        }
        availableSpaces.removeIf(arr -> arr[0] == model.xDim/2 && arr[1] == model.yDim/2);

        baseRadiationDose = 0; currentRadiationDose = 0; appliedRadiationDose = 10;
        Lymphocytes.dieProb = CellFunctions.getLymphocytesProb(baseRadiationDose);
        if (lymphocitePopulation > 0)
        {
            new CellFunctions().randomInitialization(this, lymphocitePopulation, CellFunctions.Type.LYMPHOCYTE);
        }

        TumorCells.count += tumorSize;
        CellFunctions.getImmuneResponse();
        double[] Tvalues = CellFunctions.getTumorCellsProb(baseRadiationDose);
        TumorCells.count -= tumorSize;
        TumorCells.dieProbRad = Tvalues[0]; TumorCells.dieProbImm = Tvalues[1]; TumorCells.divProb = Tvalues[2];

        if (tumorSize > 0)
        {
            model.NewAgentSQ(model.xDim/2, model.yDim/2).Init(CellFunctions.Type.TUMOR);
            TumorCells.count++;
        }
        else
        {
            availableSpaces.add(new int[] {model.xDim/2, model.yDim/2});
            //adds it at end of list, not in numerical order
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
            getavailableSpaces1(win, false, triggeringPopulation, CellFunctions.Type.TRIGGERING);
        }
    }

    public void StepCells ()
    {
        for (CellFunctions cell:this) //this is a for-each loop, "this" refers to this grid
        {
            cell.StepCell();
        }
    }

    public void getavailableSpaces1(GridWindow win, boolean migration, int cellPopulation,
                                          OnLattice2DCells.CellFunctions.Type type) throws Exception
    {
        if (TumorCells.count + DoomedCells.count + TriggeringCells.count == this.xDim * this.yDim)
        {
            availableSpaces1.clear();
        }

        for (int i = 0; i < length; i++)
        {
            CellFunctions cell = GetAgent(i);
            if (cell == null)
            {
                cell = NewAgentSQ(i);
                availableSpaces1.add(new int[]{(int) cell.Xpt(),(int) cell.Ypt()});
                cell.Dispose();
            }
            else if (cell != null && cell.type == CellFunctions.Type.TUMOR)
            {
                tumorSpaces1.add(new int[]{(int) cell.Xpt(),(int) cell.Ypt()});
            }
        }
        if (migration)
        {
            new CellFunctions().lymphocyteMigration(this, win);
        }
        else if (!migration)
        {
            new CellFunctions().randomInitialization(this, cellPopulation, type);
        }
    }

    public void DrawModelandUpdateProb(GridWindow win, GifMaker gif) throws Exception
    {
        int color;

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
        //gif.AddFrame(win);
    }
    
    public void spatialRadiationArea(GridWindow win)
    {
        //int centerX = xDim/2; int centerY = yDim/2;
        double targetPercentage = 0.5;
        int targetPixelsInCircle = (int) (TumorCells.count * targetPercentage);

        int minX = xDim;
        int maxX = 0;
        int minY = yDim;
        int maxY = 0;
        for (int x = 0; x < xDim; x++)
        {
            for (int y = 0; y < yDim; y++)
            {
                if (win.GetPix(x, y) == Util.CategorialColor(TumorCells.colorIndex))
                {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        int centerX = (minX + maxX) / 2;
        int centerY = (minY + maxY) / 2;

        int radius = 0;
        for (int testRadius = 1; testRadius <= xDim; testRadius++)
        {
            int count = 0;
            for (int i = 0; i < xDim; i++)
            {
                for (int j = 0; j < yDim; j++)
                {
                    if (isInsideCircle(i, j, centerX, centerY, testRadius) && win.GetPix(i, j) == Util.CategorialColor(TumorCells.colorIndex))
                    {
                        count++;
                        if (count >= targetPixelsInCircle)
                        {
                            radius = testRadius;
                            j = yDim; i = xDim; testRadius = xDim + 1;
                        }
                    }
                }
            }
        }

        for (int i = 0; i < xDim; i++)
        {
            for (int j = 0; j < yDim; j++)
            {
                if (isInsideCircle(i, j, centerX, centerY, radius))
                {
                    //win.SetPix(i, j, Util.GREEN);
                    OnLattice2DGrid.pixelsInCircle.add(new int[]{i, j});
                }
            }
        }
    }

    public static boolean isInsideCircle(int i, int j, int centerX, int centerY, int radius)
    {
        int dx = i - centerX;
        int dy = j - centerY;
        return dx * dx + dy * dy <= radius * radius;
    }

    public void spatialRadiationApplied(GridWindow win) throws Exception
    {
        currentRadiationDose = appliedRadiationDose;
        double LDieProb = CellFunctions.getLymphocytesProb(currentRadiationDose);
        double[] Tvalues = CellFunctions.getTumorCellsProb(currentRadiationDose);
        double[] Avalues = CellFunctions.getTriggeringCellsProb(currentRadiationDose);

        for (int[] pixel : pixelsInCircle)
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

    public void spatialRadiationUnapplied(GridWindow win) throws Exception
    {
        currentRadiationDose = baseRadiationDose;

        for (int[] pixel : pixelsInCircle)
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
//                writer.write("Timestep," + Lymphocytes.name + "," + TumorCells.name + "," + DoomedCells.name);
                writer.write("Timestep,Lymphocytes,TriggeringCells,TumorCells,TumorCellsRad,DoomedCells," +
                        "DoomedCellsRad,Lymphocytes DieProb,Tumor DieProbRad,Tumor DieProbImm,Tumor DivProb," +
                        "SurvivingFractionTLast," +
                        "PrimaryImmuneResponse,SecondaryImmuneResponse,ImmuneResponse,LymphocyteMigrationAttempted");
                writer.newLine();
            }
            //writer.write(timestep + "," + Lymphocytes.count + "," + TumorCells.count + "," + DoomedCells.count);
            writer.write(timestep + "," + Lymphocytes.count + "," + TriggeringCells.count + "," + TumorCells.count + "," + TumorCells.countRad + "," +
                    DoomedCells.count + "," + DoomedCells.countRad + "," + Lymphocytes.dieProb + "," + TumorCells.dieProbRad + "," +
                    TumorCells.dieProbImm + "," + TumorCells.divProb + "," + TriggeringCells.SurvivingFractionTLast + "," +
                    OnLattice2DGrid.primaryImmuneResponse + "," + OnLattice2DGrid.secondaryImmuneResponse + "," +
                    OnLattice2DGrid.immuneResponse + "," + OnLattice2DGrid.newLymphocytesAttempted);
            writer.newLine();
        }
        catch (IOException e)
        {
            System.err.println("Failed to write CSV file: " + e.getMessage());
            e.printStackTrace();
            System.exit(0);
        }
    }

    public void saveProbabilitiesToCSV(String fullPath2, boolean append, int timestep, GridWindow win, boolean duringRadiation)
    {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fullPath2, append)))
        {
            if (timestep == 0)
            {
                writer.write("Timestep, Cell, Type, Color, Radiated, RadiationDose, DeathFromRadiation, DieProb, ActivateProb, DieProbRad, DieProbImm, DivProb");
                writer.newLine();
            }

            if (duringRadiation)
            {
                writer.write("Before Radiation Effects\n");
            }

            if (timestep > 97 && timestep < 109)
            {
                for (int i = 0; i < length; i++)
                {
                    OnLattice2DCells.CellFunctions cell = GetAgent(i);
                    if (cell != null && cell.type == CellFunctions.Type.DOOMED)
                    {
                        writer.write(timestep + "," + cell + "," + cell.type + "," + cell.color + "," + cell.radiated + "," +
                                cell.radiationDose + "," + cell.deathFromRadiation + "," + cell.dieProb + "," + cell.activateProb + "," +
                                cell.dieProbRad + "," + cell.dieProbImm + "," + cell.divProb);
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

    public void printPopulation(String name, int colorIndex, int count)
    {
        System.out.println("Population of " + name + " (" + findColor(colorIndex) + "): " + count);
    }

    public static void main (String[] args) throws Exception
    {
        System.out.println(className + ":\nBase Radiation Dose: " + baseRadiationDose + " Gy");

        int x = 100;
        int y = 100;
        int timesteps = 1000;
        GridWindow win = new GridWindow(x, y, 5);
        OnLattice2DGrid model = new OnLattice2DGrid(x, y);

        new Lymphocytes().Lymphocytes();
        new TumorCells().TumorCells();
        new DoomedCells().DoomedCells();
        new TriggeringCells().TriggeringCells();

        model.Init(win, model);
        model.saveCountsToCSV(fullPath1, false, 0);
        model.saveProbabilitiesToCSV(fullPath2, false, 0, win, false);

        GifMaker gif = new GifMaker(directory + "TrialRunGif.gif",1,false);

        for (int i = 1; i <= timesteps; i++)
        {
            win.TickPause(1);

            if (radiationTimesteps.contains(i) && TumorCells.count > 20)
            {
                model.spatialRadiationArea(win);
                model.spatialRadiationApplied(win);
                model.saveProbabilitiesToCSV(fullPath2, true, i, win, true);
            }
            else if (radiationTimesteps.contains(i - 1))
            {
                model.spatialRadiationUnapplied(win);
                pixelsInCircle.clear();
            }

            model.StepCells();

            availableSpaces1.clear(); tumorSpaces1.clear();
            model.getavailableSpaces1(win, true, 0, CellFunctions.Type.LYMPHOCYTE); //Lymphocyte Migration

            model.saveCountsToCSV(fullPath1, true, i);
            model.saveProbabilitiesToCSV(fullPath2, true, i, win, false);

            model.DrawModelandUpdateProb(win, gif); //get occupied spaces to use for stepCells method, rerun if model pop goes to 0

            if (model.Pop() == 0)
            {
                model.Init(win, model);
                model.saveCountsToCSV(fullPath1, true, 0);
                model.saveProbabilitiesToCSV(fullPath2, true, 0, win, false);
                i = 1;
            }
        }

        gif.Close();

        model.printPopulation(Lymphocytes.name, Lymphocytes.colorIndex, Lymphocytes.count);
        model.printPopulation(TumorCells.name, TumorCells.colorIndex, TumorCells.count);
        model.printPopulation(DoomedCells.name, DoomedCells.colorIndex, DoomedCells.count);
        model.printPopulation(TriggeringCells.name, TriggeringCells.colorIndex, TriggeringCells.count);
        System.out.println("Population Total: " + model.Pop());
        System.out.println("Unoccupied Spaces: " +  availableSpaces1.size());
        System.out.println();
        availableSpaces1.clear(); tumorSpaces1.clear();
    }
}