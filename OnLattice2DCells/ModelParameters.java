package OnLattice2DCells;

public interface ModelParameters
{
    double radiationSensitivityOfTumorCellsAlpha();
    double radiationSensitivityOfTumorCellsBeta();
    double radiationSensitivityOfLymphocytesAlpha();
    double radiationSensitivityOfLymphocytesBeta();
    double tumorGrowthRate();
    double tumorInfiltratioinRate();
    double rateOfCellKilling();
    double decayConstantOfD();
    double decayConstantOfL();
    double recoveryConstantOfA();
    double radiationInducedInfiltration();
    double immuneSuppressionEffect();
}
