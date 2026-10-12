# Distribution and volatility inputs

Four experimental settings form a 2×2 comparison: Normal versus Lognormal, crossed with nominal lane CV [0.1,0.3] versus [0.4,0.6]. Each setting uses the same five master seeds, 20261021–20261025, I=15 carriers, J=50 lanes, H=75 historical observations, 40 randomly sampled decision contexts and 1,000 conditional OOS demand realizations per context. Common loadings remain in [0.3,0.5], and selection bounds are [2,12].

For corresponding instances across the four settings, procurement parameters, nominal demand coefficients, historical covariates and decision contexts are unchanged. The residual distribution and CV interval vary. Rejection of negative Normal demands may also change subsequent random draws, so equal master seeds do not imply identical residual realizations across settings.

Training and validation windows follow the main experiment. All settings use the shared Java data generator.
