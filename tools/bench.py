import csv
import subprocess
from dataclasses import dataclass
import os
import pandas as pd
import subprocess
from pathlib import Path

benchmark_path = 'data/svcomp-26/benchmark.csv'
output_path = 'output/report-full.csv'
smoke_run = True

env = os.environ.copy()
binary_path = Path("./build/install/jlisa-witness-validator/bin/jlisa-witness-validator")
#env['JAVA_HOME'] = '/usr/lib/jvm/java-26-openjdk'

df = pd.read_csv(benchmark_path)

top_level = {
    "VALID_ASSERT": "SV-COMP26_valid-assert",
    "NO_RUNTIME": "SV-COMP26_no-runtime-exception",
}

def get_tool_dir(tool):
    return list(Path('data/svcomp-26/results-verified/').glob(f'{tool}.*'))[0].name

class Bench:
    tool: str
    name: str
    group: str
    path: Path
    properties: str
    expectedVerdict: str
    status: str
    category: str
    
    def __init__(self, bench):
        self.tool = bench['tool']
        self.name = bench['name']
        self.group = bench['group']
        self.path = Path(bench['path'])
        self.properties = bench['properties']
        self.expectedVerdict = bench['expectedVerdict']
        self.status = bench['status']
        self.category = bench['category']

class Cmd:
    benchmark: Bench
    binary: Path
    verbose: bool

    def __init__(self, benchmark: Bench, binary: Path, verbose: bool):
        self.benchmark = benchmark
        self.binary = binary
        self.verbose = verbose

    def _build_witness(self) -> str:
        base = "data/svcomp-26/results-verified/TOOL/TOP_LEVEL/BENCHMARK/witness.graphml"
        prp = self.benchmark.properties

        if prp == 'valid-assert':
            base = base.replace('TOP_LEVEL', top_level['VALID_ASSERT'])
        elif prp == 'no-runtime-exception':
            base = base.replace('TOP_LEVEL', top_level['NO_RUNTIME'])
    
        base = base.replace('TOOL', get_tool_dir(self.benchmark.tool))
        base = base.replace('BENCHMARK', self.benchmark.name)
        print(base)
        assert os.path.exists(base)

        return base
    
    def _build_benchmark(self) -> str:
        base = "data/svcomp-26/benchmarks/java"
        base = Path(base) / Path(*self.benchmark.path.parts[3:])
    
        print(base)
        assert os.path.exists(base)
    
        return base


    def build(self) -> list[str]:
        cmd = [
            self.binary,
            "--witness", 
            self._build_witness(),
            "--benchmark",
            self._build_benchmark()
        ]

        if self.verbose:
            cmd.append('--verbose')
        
        return cmd

class ResultWriter:
    
    def __init__(self, path):
        self.path = path
    
    def initialize(self):
        with open(self.path, 'w', newline="", encoding="utf-8") as o:
            spamwriter = csv.writer(o)
            spamwriter.writerow(['tool', 'name', 'group', 'path', 'properties',
                'expectedVerdict', 'status', 'category', 'validation result', 'validation log'
            ])
        
    def write_result(self, bench: Bench, result, log):
        out = [bench.tool, bench.name, bench.group, bench.path, bench.properties, bench.expectedVerdict, bench.status, bench.category, result, log]
        
        with open(self.path, 'a', newline="", encoding="utf-8") as o:
            spamwriter = csv.writer(o)
            spamwriter.writerow(out)


writer = ResultWriter(output_path)
writer.initialize()

def process_bench(index, bench):
    b = Bench(bench)
    cmd = Cmd(b, binary_path, True)
    
    if smoke_run:
        result = 'smoke run'
        log = 'SMOKE RUN'
    else:
        out = subprocess.run(cmd.build(), env=env, capture_output=True)
        result = out.stdout.decode().strip()
        log = out.stderr.decode("utf-8")

    if result == 'Witness Correct':
        print(0)
    else:
        print(1)
        print(bench)
        print(result)
        print(log)
    
    print()
    
    writer.write_result(b, result, log)

tools = df['tool'].unique()

for t in tools:
    tool_mask = (df['tool'] == t)
    status_mask = (df['status'] == 'false')
    category_mask = (df['category'] == 'correct')

    mask = tool_mask & status_mask & category_mask

    for r in df[mask].iterrows():
        index, bench = r
        process_bench(index, bench)

