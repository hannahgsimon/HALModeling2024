# OnLattice2DGrid

## Overview
In the "OnLattice2DCells" folder is the `OnLattice2DGrid` class, a component of the HALModeling2024 project. This class extends `AgentGrid2D<CellFunctions>` and serves as the foundation for modeling and simulating tumor and immune cell populations in a 2D grid environment. It supports agent-based simulations by facilitating cell interactions, movement, and other on-lattice dynamics. This Java code uses the Hybrid Automata Library (HAL) [1] to create a spatial agent-based model of an ordinary differential equations model [2] to simulate radio-immune response to spatially fractionated radiotherapy.

Code to develop and analyze statistical graphs for this model can be found at https://github.com/hannahgsimon/HALModeling2024Graphs.

## Prerequisites
- Java Development Kit (JDK): Version 8 or higher.
- HAL Framework: Ensure the HAL framework dependencies are installed and properly configured.

## Installation
1. Clone the repository:
    - git clone https://github.com/hannahgsimon/HALModeling2024.git
2. Navigate to the project directory: cd HALModeling2024/OnLattice2DCells
3. Import the project into your preferred Java IDE.
4. Build the project to ensure all dependencies are resolved.

## Radio-Immune Response Ordinary Differential Equations (ODE) Model [2]
- Tumor Cells: 𝑇<sub>𝑛+1</sub> = 𝑇<sub>𝑛</sub> 𝑒<sup>(𝜇−𝑍<sub>𝑛</sub>)</sup> 𝑆<sub>𝑇</sub>
- Lymphocytes: 𝐿<sub>𝑛+1</sub> = (1−𝜆<sub>𝐿</sub>) 𝑆<sub>𝐿</sub> 𝐿<sub>𝑛</sub> + 𝜌𝑇<sub>𝑛</sub> + 𝜓𝜀𝐴<sub>𝑛</sub>𝑇<sub>𝑛</sub>
- Triggering Cells: 𝐴<sub>𝑛+1</sub> = (1−𝑒) (𝐴<sub>𝑛</sub> 𝑆<sub>𝑖</sub> + ∆𝐴<sub>𝑛</sub>)
- Activation Function: 𝑒 = tanh⁡((1−𝑆<sub>𝑇</sub>) 𝑉<sub>𝐶</sub>)
- Primary Immune Response: 𝑍<sub>𝑝,𝑛</sub> = $\frac{wL_{n}}{1 + \frac{\kappa T_{n}^{2/3} L_{n}}{1}}$
- Secondary Immune Response: $Z_{s,n} = \sum_{i=0}^{n} \gamma \frac{(1 + c_i)}{(r + c_i)} Z_{p,i}$
- Doomed Cells: 𝐷<sub>𝑛+1</sub> = (1−𝜆<sub>𝐷</sub>) 𝐷<sub>𝑛</sub> + (1−𝑆<sub>𝑇</sub>) 𝑇<sub>𝑛</sub> 𝑒<sup>𝜇</sup> + 𝑆<sub>𝑇</sub> 𝑇<sub>𝑛</sub> 𝑒<sup>𝜇</sup> (1−𝑒<sup>−𝑍<sub>𝑛</sub></sup>)
- Surviving Fraction: 𝑆 = 𝑒<sup>(−𝛼𝑑<sub>𝑛</sub>−𝛽𝑑<sub>𝑛</sub><sup>2</sup>)</sup>

### Key
- **$(\alpha, \beta)_{T,L}$**: Radiation sensitivity  
- **$\mu$**: Tumor growth rate  
- **$\rho$**: Tumor infiltration rate  
- **$w$**: Rate of cell killing  
- **$\lambda_{D,L}$**: Decay constant  
- **$\psi$**: Radiation-induced infiltration  
- **$\kappa$**: Immune suppression effect  
- **$d_n$**: Radiation dose  


## Radio-Immune Response Spatial Model Agents
1. $${\color{blue}Lymphocytes} \space {\color{blue}(blue)}$$
    - Lymphocyte Migration: 𝜌𝑇<sub>𝑛</sub> + 𝜓𝜀𝐴<sub>𝑛</sub>𝑇<sub>𝑛</sub>  
        Depends on the presence of triggering cells:
        ```java
        if (TriggeringCells.count > 0)
        {
            new CellFunctions().lymphocyteMigration(model, win);
        }
        ```
    - Survival: 𝑆<sub>𝐿</sub> (1−𝜆<sub>𝐿</sub>)
    - Removal by Radiation: 1−𝑆<sub>𝐿</sub>
    - Removal by Exhaustion: 𝑆<sub>𝐿</sub> 𝜆<sub>𝐿</sub>
2. $${\color{lightgreen}\text{Triggering Cells (immune cells that attract lymphocytes to the tumor site, green)}}$$
    - Survival: 𝜆<sub>𝐴</sub> (1−𝑆<sub>𝐿</sub>) (1−𝜀) + 𝑆<sub>𝑖</sub> (1−𝜀)
    - Removal by Radiation: (1−𝑆<sub>𝑖</sub>) (1−𝜆<sub>𝐴</sub>)
    - Removal by Activation: (1−𝑆<sub>𝑖</sub>) 𝜆<sub>𝐴</sub> 𝜀 + 𝑆<sub>𝑖</sub> 𝜀
    - Each timestep 1 random triggering cell is removed.
3. $${\color{red}Tumor\ Cells\ (red)}$$
    - Survival: 𝑆<sub>𝑇</sub> (1−𝑍<sub>𝑛</sub>) (1−𝜇)
    - Division: 𝑆<sub>𝑇</sub> (1−𝑍<sub>𝑛</sub>) 𝜇
    - Doomed by Radiation: 1−𝑆<sub>𝑇</sub>
    - Doomed by Immune System: 𝑆<sub>𝑇</sub> 𝑍<sub>𝑛</sub>
4. $${\color{yellow}Doomed} \space {\color{yellow}Cells} \space {\color{yellow}(dead} \space {\color{yellow}tumor} \space {\color{yellow}cells,} \space {\color{yellow}yellow)}$$
    - Remain on Grid: 1−𝜆<sub>𝐷</sub>
    - Clearance: 𝜆<sub>𝐷</sub>
       
## Spatial Model Features
- Agent-Based Model (ABM): This computer simulation studies the interactions between agents (here, cells), radiation, and time.
- On-Lattice ABM: Discrete, limited in where the agents can move.
- 2D Grid: The space with defined dimensions within which agents can move.
- Stochastic: At each timestep, each agent can have one of several random outcomes with probabilities defined in the `CellFunctions` class, such as death, division, and survival (see agent definitions above).
- Agent Management: Agents and their attributes can be tracked within the 2D grid.
- Figures: Each figure (2-6) is defined by different parameters, which are specified in the `FigParameters` class and derived from Table 1 [2]. The only modification occurs in Figure 3, where the tumor infiltration rate is set to 0.05 instead of 0.5 to improve model performance by preventing excessive lymphocyte migration.
- Scenarios: See the "Simulation GIFs" folder for definitions of each scenario (A-E), which is a figure with immune suppression effect (𝜅) at a threshold value.
- Sample Simulations: The "Simulation GIFs" folder defines and contains GIFs of figure 3 with different radiation approaches, and of each scenario (A-E). These were generated with `writeGIF = true`.

## Usage
Before running the code, you will need to update the file paths if any of the following booleans are set to true: `printCounts`, `printProbabilities`, `printNeighbors`, or `writeGIF`.

The simulation starts with the below initial conditions (modifiable in the code). You can update these parameters in the indicated lines of code to fit your specific simulation requirements.
- **<ins>Figure</ins>:** 2. The figure for which the code will run.
     ```java
    public static int figure = 2;
     ```
- **<ins>Scenario Active</ins>:** Disabled. Used for simulation testing of scenarios A, B, C, or D to study birfurcation of a tumor from controlled growth (immune limited) to uncontrolled growth (immune escape). The threshold is determined by the value of immune suppression (*𝜅*).
    ```java
    public static boolean scenarioActive = false; public static char scenario = 'A';
    ```
- **<ins>Initial Cell Populations at Timestep 0</ins>:** 0 lymphocytes, 1 tumor cell, & 500 triggering cells. The tumor cells are initialized at the center of the grid, while all other cells are randomly distributed in the remaining spaces.
    ```java
    int lymphocitePopulation = 0;
    int tumorSize = 1;
    int triggeringPopulation = 500;
    ```
- **<ins>Grid Size</ins>:** 100 x 100 cells.
    ```java
    int x = 100;
    int y = 100;
    ```
- **<ins>Timesteps</ins>:** 1000. The total number of timesteps for which the simulation runs.
    ```java
    int timesteps = 1000;
    ```
- **<ins>Total Radiation</ins>:** Disabled. Radiates the entire grid.
    ```java
    public static boolean totalRadiation = false
    ```
- **<ins>Center Radiation</ins>:** Enabled. Calculates the tumor's center at that timestep and radiates a specified percentage (modifiable via *public static double targetPercentage = 0.7;*) of the tumor in a circular area centered on that point.
    ```java
    centerRadiation = true
    ```
- **<ins>Spatial Radiation</ins>:** Disabled. Calculates the tumor's center at the current timestep and defines a circular area around it with a specified radius (modifiable via *public static int radius = 10;*). Additional circular areas are calculated to fit within the tumor, maintaining a 2-cell gap between adjacent circles and a 1-cell buffer between each circle and the tumor's edge. Each circle is radiated if the percentage of tumor and doomed cells within it meets or exceeds the specified threshold (modifiable via *public static double thresholdPercentage = 0.8;*).
    ```java
    spatialRadiation = false
    ```
- **<ins>Base Radiation Dose</ins>:** *0 Gy*. The default radiation dose applied to the entire grid during each timestep when radiation is turned off.
    ```java
    public static int baseRadiationDose = 0
    ```
- **<ins>Applied Radiation Dose</ins>:** *10 Gy*. The radiation dose delivered using the specified method (total, center, or spatial) during active radiation timesteps.
    ```java
    appliedRadiationDose = 10
    ```
- **<ins>Radiation Timesteps</ins>:** 200. The timesteps during which radiation is applied using the specified method (total, center, or spatial).
    ```java
    public static List<Integer> radiationTimesteps = List.of(200);
    ```
- **<ins>Neighborhood Radius for Lymphocyte Migration</ins>:** 0.75. The percentage of the minimum grid dimension within which lymphocytes can migrate. The closer to the center of the tumor an empty space is, the higher the probability a lymphocyte has of migrating there.
    ```java
    double radiusFraction = 0.75;
    ```
- **<ins>Lymphocyte Density Management</ins>:** 4. The maximum number of lymphocyte neighbors any grid space can have in an adjacent space (including diagonally).
    ```java
    int maxNeighbors = 4;
    ```
- **<ins>Print Counts</ins>:** Disabled. Outputs a CSV file with the following data at each timestep: populations of lymphocytes, triggering cells, all tumor cells, tumor cells previously exposed to radiation, all doomed cells, and doomed cells that are dead from radiation. Additionally, for the entire grid, it records the probabilities of lymphocyte death, tumor cell death from radiation, tumor cell death from immune system activity, and tumor cell division. It also prints the average surviving fraction of tumor cells (including the surviving fraction for radiation-exposed tumor cells during their last moment of radiation), primary immune response, secondary immune response, total immune response, the number of lymphocytes attempting to migrate onto the grid, and immune suppression effect.
    ```java
    public static final boolean printCounts = false
    ```
- **<ins>Print Probabilities</ins>:** Disabled. Outputs a CSV file with the following data for every cell on the grid at each timestep: type, color, whether it was previously exposed to radiation, the dose of radiation received that timestep, whether the cell is dead from radiation. It also records the probabilities of death, activation, death from radiation, death from immune system activity, and division. It also prints the number of lymphocyte neighbors (including diagonally).
    ```java
    printProbabilities = false
    ```
- **<ins>Print Neighbors</ins>:** Disabled. Outputs a CSV file with the number of lymphocyte neighbors (including diagonally) at each timestep, for both every cell on the grid and empty grid spaces.
    ```java
    printNeighbors = false
    ```
- **<ins>Write GIF</ins>:** Disabled. Outputs a GIF file of the simulation.
    ```java
    public static boolean writeGIF = false;
    ```
- **<ins>Immune Suppression Effect Threshold</ins>:** Disabled. When enabled, the immune suppression effect (*𝜅*) dynamically adjusts at each timestep to its threshold value, as defined by equation 5 [2]: $e_{\kappa} = \frac{w}{\mu \ T_{n}^{2/3}} - \frac{1}{L_{n} \ T_{n}^{2/3}}$
    ```java
    public static boolean immuneSuppressionEffectThreshold = false;
    ```

## Contributing
Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new branch for your feature or bug fix:
    - git checkout -b feature-name
3. Commit your changes and push the branch:
    - git commit -m "Add new feature"
    - git push origin feature-name
4. Open a pull request and describe your changes in detail.

## References
[1] Bravo, R. R., Baratchart, E., West, J., Schenck, R. O., Miller, A. K., Gallaher, J., Gatenbee, C. D., Basanta, D., Robertson-Tessi, M., & Anderson, A. R. A. (2020). Hybrid Automata Library: A flexible platform for hybrid modeling with real-time visualization. *PLOS Computational Biology*, 16(3), e1007635. https://doi.org/10.1371/journal.pcbi.1007635

[2] Cho, Y.-B., Yoon, N., Suh, J. H., & Scott, J. G. (2023). Radio-immune response modelling for spatially fractionated radiotherapy. *Physics in Medicine & Biology*, 68(16), 165010. https://doi.org/10.1088/1361-6560/ace819

## License
This project is licensed under the MIT License. See the LICENSE file for details.

## Contact
For questions or feedback, please contact Hannah G. Simon at hgsimon2@gmail.com.
