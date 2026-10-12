# Main experiment inputs

Five instances (`case_001`–`case_005`) correspond to experiment IDs `rep_001`–`rep_005`, with seeds 20261021–20261025. Each instance has I=15 carriers, J=50 lanes, H=75 historical observations, 40 randomly sampled decision contexts and 1,000 conditional OOS demand realizations per context. Distribution: Normal; nominal lane CV: [0.3,0.5]; common loadings: [0.3,0.5]; selected-carrier bounds: [2,12].

Each instance stores procurement parameters and historical data shared by all methods and decision contexts. The 25 rolling validation origins use 50 past observations (`0:49` to predict observation 50, through `24:73` to predict observation 74); decisions for OOS evaluation use all 75 historical observations. Validation subsets are obtained from `history.csv`. The trend coordinate is T=1 in each test context. OOS demands are not used to generate procurement parameters or tune hyperparameters.

See `GENERATION.md` and `DATA_DICTIONARY.md` in the repository root.
