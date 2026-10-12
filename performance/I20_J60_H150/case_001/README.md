# performance/I20_J60_H150/case_001

Experiment instance ID: `performance/I20_J60_S150_rep_000`. Master data seed: **20261021**.
Distribution: NORMAL; nominal lane CV: [0.3,0.5].
I=20, J=60, H=150, decision contexts=3, OOS realizations/context=0.
Common loading range is [0.3,0.5]; selection upper fraction is 0.8.

Procurement parameters and historical observations are shared across this instance's decision contexts. `seed.txt` plus `config.json`
define regeneration; `random_streams.json` records the derived seeds.
Decision contexts are stored in generation order.

From the repository root, generate into a new directory:

```sh
python code/generate.py performance/I20_J60_H150/case_001 --output generated/performance/I20_J60_H150/case_001
```

See the experiment-level README, root `GENERATION.md` and `DATA_DICTIONARY.md`
for generation rules and column definitions.
