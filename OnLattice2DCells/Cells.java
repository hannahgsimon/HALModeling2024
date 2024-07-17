package OnLattice2DCells;

//Author: Hannah Simon, HTiigee on Git

public interface Cells
{
    String name();
    double dieProb();
    double divProb();
    int colorIndex();
    int count();

    //public double survivingFraction();
}