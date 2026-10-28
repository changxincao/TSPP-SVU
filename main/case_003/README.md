# main/case_003

Experiment instance ID: `main/rep_003`. Master data seed: **20261023**.
Distribution: NORMAL; lane CV parameter: [0.3,0.5].
I=15, J=50, H=75, decision contexts=40, OOS realizations/context=1000.
Common loading range is [0.3,0.5]; selection upper fraction is 0.8.

Procurement parameters and historical observations are shared across this instance's decision contexts. Generation settings are stored in `seed.txt` and `config.json`.

From the repository root, generate into a new directory:

```sh
python code/generate.py main/case_003 --output generated/main/case_003
```

See the experiment-level README, root `GENERATION.md` and `DATA_DICTIONARY.md`
for generation rules and column definitions.
