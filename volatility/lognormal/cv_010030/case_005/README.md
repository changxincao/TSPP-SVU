# volatility/lognormal/cv_010030/case_005

Experiment instance ID: `lognormal_cv010030/rep_005`. Master data seed: **20261025**.
Distribution: LOGNORMAL; nominal lane CV: [0.1,0.3].
I=15, J=50, H=75, decision contexts=40, OOS realizations/context=1000.
Common loading range is [0.3,0.5]; selection upper fraction is 0.8.

Procurement parameters and historical observations are shared across this instance's decision contexts. `seed.txt` plus `config.json`
define regeneration; `random_streams.json` records the derived seeds.
Decision contexts are stored in generation order.

From the repository root, generate into a new directory:

```sh
python code/generate.py volatility/lognormal/cv_010030/case_005 --output generated/volatility/lognormal/cv_010030/case_005
```

See the experiment-level README, root `GENERATION.md` and `DATA_DICTIONARY.md`
for generation rules and column definitions.
