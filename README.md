# OnLattice2DGrid

## Overview
In the OnLattice2DCells folder is the OnLattice2DGrid class, a component of the HAL Modeling 2024 project. This class extends AgentGrid2D<CellFunctions> and serves as the foundation for modeling and simulating tumor and immune cell populations in a 2D grid environment. It supports agent-based simulations by facilitating cell interactions, movement, and other on-lattice dynamics. This Java code uses the Hybrid Automata Library (HAL) [1] to create a spatial agent-based model of an ordinary differential equations model [2] to simulate radio-immune response to spatially fractionated radiotherapy.

## Features
- Agents:
  1. Lymphocytes (blue)
  2. Triggering Cells (immune cells that attract lymphocytes to the tumor site) (green)
  3. Tumor Cells (red)
  4. Doomed Cells (dead tumor cells) (yellow)
- Agent Management: Tracks and manages agents (e.g., tumor and immune cells) within the 2D grid.
- Lattice-Based Simulation: Supports cell migration, proliferation, and other on-lattice behaviors.
- Customizable Interactions: Designed to work with various cell functions by integrating the CellFunctions class.
- Efficient Updates: Optimized for adding and removing multiple elements at each timestep, ensuring scalability for large simulations.

## Prerequisites
- Java Development Kit (JDK): Version 8 or higher.
- HAL Framework: Ensure the HAL framework dependencies are installed and properly configured.

## Installation
1. Clone the repository:
    - git clone https://github.com/hannahgsimon/HALModeling2024.git
2. Navigate to the project directory: cd HALModeling2024/OnLattice2DCells
3. Import the project into your preferred Java IDE (e.g., Microsoft Visual Studio Code, IntelliJ IDEA, Eclipse).
4. Build the project to ensure all dependencies are resolved.

## Usage
The simulation starts with the below initial conditions (modifiable in the code). You can update these parameters in the indicated lines of code to fit your specific simulation requirements.
- Figure: 2. There are figures 2, 3, 4, 5, and 6, each of which have different parameters as outlined in the FigParameters class.
    - *public static int figure = 2;*
- Scenario Active: false. If true, used for simulation testing of scenarios A, B, C, or D to study birfurcation of a tumor from controlled growth (immune limited) to uncontrolled growth (immune escape). The threshold is due to the value of immune suppression (*𝜅*).
    - *public static boolean scenarioActive = false; public static char scenario = 'A';*
- Initial Cell Populations at time *𝑡=0*: 0 lymphocytes, 1 tumor cell, & 500 initial triggering cells
    - *int lymphocitePopulation = 0;
        int tumorSize = 1;
        int triggeringPopulation = 500;*
- Grid Size: 100 x 100 cells
    - *int x = 100;
        int y = 100;*
- Timesteps: 1000. The total number of timesteps for which the simulation runs.
    - *int timesteps = 1000;*
- Total Radiation: Disabled. Radiates the entire grid.
    - *public static boolean totalRadiation = false*
- Center Radiation: Enabled. Calculates the tumor's center at that timestep and radiates a specified percentage (modifiable via *public static double targetPercentage = 1;*) of the tumor in a circular area centered on that point.
    - *centerRadiation = true*
- Spatial Radiation: Disabled. Calculates the tumor's center at the current timestep and defines a circular area around it with a specified radius (modifiable via *public static int radius = 10;*). Additional circular areas are calculated to fit within the tumor, maintaining a 2-cell gap between adjacent circles and a 1-cell buffer between each circle and the tumor's edge. Each circle is radiated if the percentage of tumor and doomed cells within it meets or exceeds the specified threshold (modifiable via *public static double thresholdPercentage = 0.8;*).
    - *spatialRadiation = false*
- Base Radiation Dose: *0 Gy*. The default radiation dose applied to the entire grid during each timestep when radiation is turned off.
    - *public static int baseRadiationDose = 0*
- Applied Radiation Dose: *10 Gy*. The radiation dose delivered using the specified method (total, center, or spatial) during active radiation timesteps.
    - *appliedRadiationDose = 10*
- Radiation Timesteps: 200. The timesteps during which radiation is applied using the specified method (total, center, or spatial).
    - *public static List<Integer> radiationTimesteps = List.of(200);*
- Each time step 1 triggering cell is removed. Lymphocyte migration depends on the presence of triggering cells. 


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
