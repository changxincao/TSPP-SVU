# Data dictionary

All files are UTF-8. Indices in procurement tables and decision-context directory names are zero-based. Decimal fields retain the generator's double-precision output. Synthetic shipment volumes and monetary rates use consistent synthetic units; Olist shipment volume is in kg.

## Per synthetic instance

- `seed.txt`: one integer master data seed.
- `config.json`: distribution, CV-parameter interval, I carriers, J lanes, H historical periods, decision-context count, OOS sample count and selection fraction. The stored `queries` field denotes the decision-context count; `experiment_id` records the corresponding experiment identifier.
- `random_streams.json`: derived random seeds used by the generator.
- `demand_parameters.csv`: lane, intercept a, beta_M/T/P/A, CV parameter, common loading rho and historical nominal procurement baseline.
- `procurement.json`: dimensions and selection limits plus three ordered tables:
  - `lanes`: `[lane_index, lane_name, spot_rate]`.
  - `carriers`: `[carrier_index, carrier_name, MQC_quantity, MQC_unit_penalty]`.
  - `pairs`: `[carrier_index, lane_index, eligible, capacity, contract_rate]` for every I×J pair. The `eligible` indicator determines whether a carrier can serve a lane. Transportation allocation is restricted to pairs with `eligible=true`.
- `history.csv`: H shared rows. Columns are `sample_id,t_index,M,T,P,A,L1,...,LJ`. The four model covariates M,T,P,A represent market state, time trend, promotion and attention; L1,...,LJ are shipment volumes.

## Per decision context

- `context.csv`: header M,T,P,A and one observed covariate vector specifying the decision context.
- `oos_demands.csv.gz`: 1,000 conditional OOS demand realizations, with the same columns as `history.csv`. Covariate coordinates repeat the decision context; sample IDs continue across the context-specific demand samples. This is a gzip-compressed CSV file.

Decision-context indices are given by the `query_NNN` directory names. Performance instances contain context files for the computation-time experiment, with the OOS sample count set to zero.

## Olist

`weekly_demands.csv` has 104 rows: `weekIndex` and 23 directional macro-region lane columns. The header defines the lane ordering. Each Olist `procurement.json` uses the lane names and ordering in this demand table, and the same procurement table schema as the synthetic instances, including the `eligible` indicator. The three procurement instances share the observed weekly demand series; their seeds generate different procurement parameters.

## Instance index

`case_index.csv` lists synthetic instance directories, experiment identifiers and master seeds.
