# volatility/normal/cv_010030/case_004

Experiment instance ID: `normal_cv010030/rep_004`. Master data seed: **20261024**.
Distribution: NORMAL; nominal lane CV: [0.1,0.3].
I=15, J=50, H=75, decision contexts=40, OOS realizations/context=1000.
Common loading range is [0.3,0.5]; selection upper fraction is 0.8.

Procurement parameters and historical observations are shared across this instance's decision contexts. `seed.txt` plus `config.json`
define regeneration; `random_streams.json` records the derived seeds.
Decision contexts are stored in generation order.

From the repository root, generate into a new directory:

```sh
python code/generate.py volatility/normal/cv_010030/case_004 --output generated/volatility/normal/cv_010030/case_004
```

See the experiment-level README, root `GENERATION.md` and `DATA_DICTIONARY.md`
for generation rules and column definitions.
