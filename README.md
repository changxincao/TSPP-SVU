# TSPP-SVU: experiment data and generation code

This repository contains the experimental datasets and synthetic-instance generation code for transportation service procurement.

## Contents

| Directory | Distribution / lane CV parameter | Instances | Decision contexts per instance | OOS demand realizations per context |
|---|---|---:|---:|---:|
| `main` | Normal / [0.3,0.5] | 5 | 40 | 1,000 |
| `volatility/normal/cv_010030` | Normal / [0.1,0.3] | 5 | 40 | 1,000 |
| `volatility/normal/cv_040060` | Normal / [0.4,0.6] | 5 | 40 | 1,000 |
| `volatility/lognormal/cv_010030` | Lognormal / [0.1,0.3] | 5 | 40 | 1,000 |
| `volatility/lognormal/cv_040060` | Lognormal / [0.4,0.6] | 5 | 40 | 1,000 |
| `performance` | Normal / [0.3,0.5]; six instance sizes | 18 | 3 | 0 |
| `olist` | Observed weekly shipment volumes | 3 synthetic procurement instances | 51 weekly decisions | One observed demand realization per week |

A synthetic instance consists of procurement parameters, a shared historical dataset and a set of decision contexts. A decision context is the observed covariate vector at which a procurement decision is made; its conditional demand realizations are used for out-of-sample (OOS) evaluation. For Olist, the decision contexts correspond to successive weeks with observed demand.

This repository contains the instances used in the reported experiments. `case_index.csv` lists each synthetic instance path, experiment identifier and master seed.

## Layout

```text
code/GenerateData.java           One shared synthetic generator
code/generate.py                 Generation launcher and output formatting
main/case_001/
    seed.txt                    One master data seed
    config.json                 Experiment settings
    random_streams.json         Derived random-stream seeds
    procurement.json            Procurement parameters shared by all contexts
    demand_parameters.csv       Lane coefficients, CV parameter and loading
    history.csv                 One shared historical sample pool
    queries/query_000/
        context.csv
        oos_demands.csv.gz       Gzip-compressed CSV
volatility/{normal,lognormal}/cv_*/case_*/
performance/I*_J*_H*/case_*/      Same layout, with OOS sample count set to 0
olist/weekly_demands.csv         One shared processed weekly demand series
olist/case_*/{seed.txt,procurement.json}
```

Each instance has its own `README.md`; each experiment directory describes its configuration. Procurement parameters and historical samples are stored once per instance and shared across its decision contexts. The directory names `queries/query_NNN` are storage identifiers for these contexts. Gzip files can be read with Python `gzip.open`, R `gzfile`, or any standard gzip utility.

## Requirements

- Python 3 (standard library only)
- Java 21

## Generate an instance

From the repository root:

```sh
python code/generate.py main/case_001 --output generated/case_001
```

The script reads `seed.txt` and `config.json` and writes the CSV, JSON and gzip files shown above. Specify a new output directory. See [GENERATION.md](GENERATION.md) and [DATA_DICTIONARY.md](DATA_DICTIONARY.md).

## Olist data and attribution

The Olist directory contains the processed weekly demand table and three synthetic procurement datasets. The source is the [Brazilian E-Commerce Public Dataset by Olist](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce). The processed demand table remains subject to its [CC BY-NC-SA 4.0 license](https://creativecommons.org/licenses/by-nc-sa/4.0/). See [olist/README.md](olist/README.md) for data files and units.
