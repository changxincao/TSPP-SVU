# Computational performance inputs

Six instance sizes are considered: (I,J)=(20,60),(30,120),(40,200), each with H=75 or H=150. Each size has three instances (seeds 20261021–20261023) and three randomly sampled decision contexts per instance. Distribution: Normal; lane CV parameter: [0.3,0.5]; common loadings: [0.3,0.5]; upper selection fraction: 0.8. Other generation rules follow the main experiment.

The case paths and experiment identifiers are listed in `case_index.csv`. Repository directory names use H for the historical sample count, while the experiment identifiers use S.

This experiment measures computational performance, with an OOS sample count of zero recorded in the configuration. Within each instance, all methods use the same procurement parameters, historical observations and decision contexts. For each historical sample size H, trend spans t/H over the complete history.
