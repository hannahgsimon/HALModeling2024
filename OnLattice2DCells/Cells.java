package OnLattice2DCells;

//Author: Hannah Simon, HTiigee on Git

public interface Cells
{
    String name();
    double dieProb();
    double divProb();
    int colorIndex();
    int count();

    public void Init(int colorIndex);
    public void StepCell(double dieProb, double divProb);
    public void printPopulation(String name);
}