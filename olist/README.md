# Olist experiment data

The shipment-volume data are derived from the [Brazilian E-Commerce Public Dataset by Olist](https://www.kaggle.com/datasets/olistbr/brazilian-ecommerce). The processed weekly demand data are shared under the source's [CC BY-NC-SA 4.0 license](https://creativecommons.org/licenses/by-nc-sa/4.0/).

`weekly_demands.csv` contains 104 weekly observations for 23 directional lanes. Shipment volumes are measured in kilograms.

The `case_001`–`case_003` directories contain the corresponding synthetic procurement parameters and their generation seeds. These parameters are generated rather than observed Olist carrier contracts. All three instances use the same weekly demand table.

See [DATA_DICTIONARY.md](../DATA_DICTIONARY.md) for file and field definitions.
