"""Generate a synthetic instance from its saved seed and configuration."""
import argparse
import csv
import gzip
import io
import json
from pathlib import Path
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
META = ["sample_id", "t_index"]
CONTEXT = ["M", "T", "P", "A"]


def read_instance(path):
    rows = [line.split("\t") for line in path.read_text(encoding="utf-8").splitlines()]
    assert rows[0] == ["TRBSVU_INSTANCE_TSV_V1"] and rows[-1] == ["END"]
    model = {"history": [], "oos": []}
    market = {"lanes": [], "carriers": [], "pairs": []}
    for row in rows[1:-1]:
        tag, *f = row
        if tag == "SEEDS": model["streams"] = [int(x) for x in f]
        elif tag == "DIMENSIONS":
            market.update(zip(["I", "J", "selection_lower", "selection_upper"], map(int, f)))
        elif tag == "LANE": market["lanes"].append([int(f[0]), f[1], float(f[2])])
        elif tag == "CARRIER":
            market["carriers"].append([int(f[0]), f[1], float(f[2]), float(f[3])])
        elif tag == "PAIR":
            market["pairs"].append([int(f[0]), int(f[1]), f[2] == "true", float(f[3]), float(f[4])])
        elif tag == "TEST_CONTEXT": model["context"] = list(map(float, f[0].split(",")))
        elif tag == "SAMPLE":
            sample = [int(f[1]), int(f[2]), *map(float, f[3].split(",")),
                      *map(float, f[4].split(","))]
            model[f[0].lower()].append(sample)
        else: raise ValueError("Unexpected record: " + tag)
    model["procurement"] = market
    return model


def json_write(path, value):
    path.write_text(json.dumps(value, indent=2, allow_nan=False) + "\n", encoding="utf-8")


def csv_write(path, header, rows, compressed=False):
    if compressed:
        with path.open("wb") as raw, gzip.GzipFile(filename="", mode="wb", fileobj=raw, mtime=0) as gz:
            with io.TextIOWrapper(gz, encoding="utf-8", newline="") as stream:
                writer = csv.writer(stream, lineterminator="\n")
                writer.writerow(header); writer.writerows(rows)
    else:
        with path.open("w", encoding="utf-8", newline="") as stream:
            writer = csv.writer(stream, lineterminator="\n")
            writer.writerow(header); writer.writerows(rows)


def export_case(source, destination, config, seed):
    destination.mkdir(parents=True, exist_ok=False)
    json_write(destination / "config.json", config)
    (destination / "seed.txt").write_text(str(seed) + "\n", encoding="ascii")
    first = None
    for q in range(config["queries"]):
        model = read_instance(source / f"query_{q:03d}.instance.tsv")
        if first is None:
            first = model
            json_write(destination / "procurement.json", model["procurement"])
            json_write(destination / "random_streams.json", dict(zip(
                ["parameters", "procurement", "contexts", "historical_noise", "oos_noise"], model["streams"])))
            csv_write(destination / "history.csv", META + CONTEXT + [f"L{j+1}" for j in range(config["J"])], model["history"])
        else:
            for field in ("procurement", "history", "streams"):
                assert model[field] == first[field], f"Shared {field} differs at query {q}"
        query = destination / "queries" / f"query_{q:03d}"
        query.mkdir(parents=True)
        csv_write(query / "context.csv", CONTEXT, [model["context"]])
        if config["oos_count"]:
            csv_write(query / "oos_demands.csv.gz", META + CONTEXT + [f"L{j+1}" for j in range(config["J"])], model["oos"], True)
        assert len(model["history"]) == config["H"] and len(model["oos"]) == config["oos_count"]


def generate_case(case, output):
    if output.exists():
        raise FileExistsError(f"Output directory already exists: {output}")
    config = json.loads((case / "config.json").read_text(encoding="utf-8"))
    seed = int((case / "seed.txt").read_text(encoding="utf-8"))
    with tempfile.TemporaryDirectory(prefix="tspp-data-") as tmp:
        raw = Path(tmp) / "raw"
        subprocess.run(["java", str(ROOT / "code/GenerateData.java"), str(raw), config["distribution"],
                        str(config["cv_low"]), str(config["cv_high"]), str(seed),
                        *[str(config[k]) for k in ["I", "J", "H", "queries", "oos_count", "selection_fraction"]]], check=True)
        export_case(raw, output, config, seed)
        parameters = raw / "demand_parameters.csv"
        (output / "demand_parameters.csv").write_bytes(parameters.read_bytes())


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("case", type=Path, help="Instance directory containing seed.txt and config.json")
    parser.add_argument("--output", type=Path, required=True, help="New instance output directory")
    args = parser.parse_args()
    generate_case(args.case.resolve(), args.output.resolve())
    print(f"Generated instance: {args.output.resolve()}")


if __name__ == "__main__":
    main()
