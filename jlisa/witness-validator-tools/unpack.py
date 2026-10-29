#!/usr/bin/env python3
"""
This script unpacks the file store provided in the context of SV-COMP.

The file store (e.g., https://doi.org/10.5281/zenodo.3630188)
is published anually for each SV-COMP.
It stores all verification tasks and witness files used and produced by any verifier
in that iteration of SV-COMP. To avoid content duplicates and naming conflicts,
the files are stored by their sha256 hash.

The results archive of the corresponding SV-COMP (e.g., https://doi.org/10.5281/zenodo.3630205)
contains files `*fileHashes.json.gz` for each verifier and validator run.
These files list the files used in that run and map them to their hashes,
as listed in the file store.
This script takes these json files and recreateis the original structure.
It uses hardlinks to do so.

Example usage:
```
scripts/prepare-tables/unpackFileStores.py results-verified/*fileHashes.json.gz results-validated/*fileHashes.json.gz
```

See `scripts/prepare_tables/unpackFileStores.py --help` for information about the command-line interface.

Required:
    - Python 3.6
Recommended:
    - progressbar2
        To see progress, install python module `progressbar2`.
        To install with pip, run `pip install progressbar2`.
"""

# %%
import argparse
import json
import gzip
from pathlib import Path
#import _logging as logging
import logging
import os
import sys

try:
    import progressbar
except ModuleNotFoundError:

    class DummyProgress:
        def progressbar(self, vs, **kwargs):
            return vs

    progressbar = DummyProgress()
else:
    progressbar.streams.wrap_stderr()

# %%
FILE_STORE = Path("fileByHash")


def eager_search(store_hash, target_suffix):
    # The globbing in this method is a big bottleneck,
    # so we should not use this, if possible
    logging.debug("Using glob to find correct file")
    store_files = list(FILE_STORE.glob(f"{store_hash}*"))
    if len(store_files) > 1:
        logging.info(f"Hash duplicate: {store_hash}")
        store_files = [f for f in store_files if f.name.endswith(target_suffix)]
    if len(store_files) == 0:
        return None
    assert len(store_files) == 1
    return store_files[0]


def unpack(hashes, output_dir):
    for target, store_hash in progressbar.progressbar(hashes.items()):
        target = output_dir / Path(target)
        if target.exists():
            logging.info(
                f"File {str(target)} already exists. File from store is ignored."
            )
            continue
        target_suffix = target.name.split(".")[-1]
        # Guessing the file name right is tremendously faster than globbing for the hash.
        # First guess: hash + suffix
        source = FILE_STORE / (store_hash + "." + target_suffix)
        if not source.exists():
            # Second guess: just the hash
            source = FILE_STORE / store_hash
        if not source.exists():
            # Uncomment this to also search for other store_hash patterns
            # This is very expensive!
            # source = eager_search(store_hash, target_suffix)
            source = None
            if not source:
                logging.warning(
                    f"Missing file in {FILE_STORE} for {target}. Expected hash: {store_hash}"
                )
                continue
        target.parent.mkdir(parents=True, exist_ok=True)
        os.link(source, target)


def parse(json_file):
    if json_file.name.endswith(".gz"):
        file_open = gzip.open
    else:
        file_open = open
    with file_open(json_file) as inp:
        return json.load(inp)


def main(argv=None):
    if not argv:
        argv = sys.argv[1:]
    parser = argparse.ArgumentParser(description="Unpack the SV-COMP file store.")
    parser.add_argument(
        "--output-dir", "-o", default=".", help="Output directory to unpack to"
    )
    parser.add_argument("file", nargs="+", help="JSON files to unpack")
    opts = parser.parse_args(argv)
    opts.output_dir = Path(opts.output_dir)
    #logging.init(logging.DEBUG, "unpack-file-stores")

    hashes = dict()
    logging.info(f"Parsing {len(opts.file)} file(s).")
    for f in progressbar.progressbar(opts.file):
        f = Path(f)
        hashes.update(parse(f))
    logging.info("Parsing finished. Unpacking store.")
    unpack(hashes, output_dir=opts.output_dir)


# %%
if __name__ == "__main__":
    sys.exit(main())
