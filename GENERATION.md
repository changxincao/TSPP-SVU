# Synthetic input generation

Use `code/generate.py` to generate an instance from its saved seed and configuration. The output follows the repository layout.

## Demand parameters and covariates

For lane j,

`mu_j(x) = a_j + beta_jM*M + beta_jT*T + beta_jP*P + beta_jA*A`.

Draw `a_j ~ U(10,100)` and four independent positive coefficients `beta_jk ~ U(0,10*a_j)`. Covariate coordinates are ordered **M,T,P,A**: market state, time trend, promotion and attention. M, P and A are independent U(0,1) draws. Historical trend is `T_t=t/H`, with zero-based `t=0,...,H-1`; T=1 in every test decision context.

The procurement baseline is the historical nominal level:

`dbar_j = a_j + 0.5*(beta_jM+beta_jP+beta_jA) + ((H-1)/(2*H))*beta_jT`.

The saved `demand_parameters.csv` records this baseline.

## Residual distribution

A fixed U(0,1) lane quantile is mapped to the configured CV-parameter interval. Each lane also has a fixed common loading `rho_j ~ U(0.3,0.5)`. Each period uses one shared G~N(0,1) and independent lane variables Z_j~N(0,1):

`z_j = rho_j*G + sqrt(1-rho_j^2)*Z_j`.

- **Normal:** `D_j=mu_j*(1+CV_j*z_j)`. If a draw is negative, redraw that lane's independent Z_j while retaining the period's G.
- **Lognormal:** `v_j=log(1+CV_j^2)` and `D_j=mu_j*exp(-v_j/2+sqrt(v_j)*z_j)`. The multiplicative error has mean one and the specified lane CV parameter.

All methods evaluated on an instance use the same historical observations, decision contexts and OOS demand realizations. Across paired volatility settings, procurement parameters, demand coefficients and covariates remain fixed; only the residual distribution and CV-parameter interval vary.

## Procurement parameters

1. Each carrier covers `round(0.5*J)` randomly selected lanes. Any uncovered lane is repaired by a donor-lane swap, retaining carrier coverage counts.
2. Draw lane base rates `rbar_j ~ U(20,100)`, persistent carrier factors `F_i ~ U(0.7,1.3)`, and local factors `L_ij ~ U(0.9,1.1)`. Eligible rates are `r_ij=rbar_j*F_i*L_ij`.
3. Eligible capacities are `q_ij=dbar_j*U(0.3,0.5)`.
4. Spot rates are `U(2,3)` times the lane's arithmetic mean eligible contract rate.
5. MQC quantities are `p_i=U(0.15,0.35)*sum_{j eligible}dbar_j`; the per-unit shortfall penalty `h_i` is the minimum eligible rate for carrier i.
6. Selection limits are `ceil(0.1*I)` and `ceil(0.8*I)`.

The `eligible` indicator specifies whether a carrier can serve a lane. Transportation allocation is restricted to pairs with `eligible=true`.

## Experiment settings

Main and volatility experiments use master seeds 20261021–20261025. Each performance setting uses 20261021–20261023 and sets the OOS sample count to zero.

## Generate an instance

From the repository root:

```sh
python code/generate.py INSTANCE_PATH --output OUTPUT
```

`INSTANCE_PATH` contains `seed.txt` and `config.json`. The command writes the procurement parameters, demand parameters, historical observations, decision contexts and OOS demand realizations to a new output directory.
