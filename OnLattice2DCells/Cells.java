package OnLattice2DCells;

//Author: Hannah Simon, HTiigee on Git

public interface Cells
{
    String name();

    double tumorGrowthRate();
    double tumorInfiltratioinRate();
    double rateOfCellKilling();
    double decayConstantOfD();
    double decayConstantOfL();
    double recoveryConstantOfA();
    double radiationInducedInfiltration();
    double immuneSuppressionEffect();

    double dieProb();
    double divProb();
    int color();
    int colorIndex();
    int count(); //parentheses indicate it's an initialized variable

    public void Init(int colorIndex);
    public void StepCell(double dieProb, double divProb);
    public void printPopulation(String name);
    public void decreaseCount()
    {

    }
}