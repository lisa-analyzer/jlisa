import csv
import os
import pandas as pd
import subprocess
from pathlib import Path

benchmark_path = 'data/svcomp-26/benchmark.csv'
output_path = 'output/report-full.csv'

df = pd.read_csv(benchmark_path)

print(df)

top_level = {
    "VALID_ASSERT": "SV-COMP26_valid-assert",
    "NO_RUNTIME": "SV-COMP26_no-runtime-exception",
}

env = os.environ.copy()
#env['JAVA_HOME'] = '/usr/lib/jvm/java-26-openjdk'

cmd = [
    "./build/install/jlisa-witness-validator/bin/jlisa-witness-validator",
    "--witness", 
    "data/svcomp-26/results-verified/TOOL/TOP_LEVEL/BENCHMARK/witness.graphml",
    "--benchmark",
    "data/svcomp-26/benchmarks/java", # jbmc-regression/CharSequenceBug
    "--extra-sources", 
    "svcomp-26/benchmarks/java/common"       , '--verbose' 
]

def get_tool_dir(tool):
    return list(Path('data/svcomp-26/results-verified/').glob(f'{tool}.*'))[0].name

def build_command(tool, prp, name, path):
    local = cmd.copy()
    if prp == 'valid-assert':
        local[2] = local[2].replace('TOP_LEVEL', top_level['VALID_ASSERT'])
    elif prp == 'no-runtime-exception':
        return None
        local[2] = local[2].replace('TOP_LEVEL', top_level['NO_RUNTIME'])

    #bench_path = bench.split('/')
    #local[2] = local[2].replace('BENCHMARK', bench.split('/')[-1])
    local[2] = local[2].replace('TOOL', get_tool_dir(tool))
    local[2] = local[2].replace('BENCHMARK', name)
    print(local[2])
    assert os.path.exists(local[2])

    local[4] = Path(local[4]) / Path(*path.parts[3:])

    print(local[4])
    assert os.path.exists(local[4])

    return local
    

def write_result(bench, result, log):
    with open(output_path, 'a', newline="", encoding="utf-8") as o:
        spamwriter = csv.writer(o)
        print(bench)
        out = bench.copy()
        out['validation result'] = result
        out['validation log'] = log
        
        spamwriter.writerow(out)


def process_bench(index, bench):
    tool = bench['tool']
    name = bench['name']
    group = bench['group']
    path = bench['path']
    properties = bench['properties']
    expectedVerdict = bench['expectedVerdict']
    status = bench['status']
    category = bench['category']

    print(name)
    print(path)
    local = build_command(tool, properties, name, Path(path))
    print(local)
    #return 
    
    if local is None:
        return

    out = subprocess.run(local, env=env, capture_output=True)
    result = out.stdout.decode().strip()

    if result == 'Witness Correct':
        print(0)
    else:
        #results[bench] = [result, out.stderr.decode("utf-8")]
        print(1)
        print(bench)
        print(result)
        print(out.stderr.decode("utf-8"))
        print()

    write_result(bench, result, out.stderr.decode("utf-8"))
    #print(index, tool, name, expectedVerdict, status)


#print(df['category'].unique())

with open(output_path, 'w', newline="", encoding="utf-8") as o:
    spamwriter = csv.writer(o)
    spamwriter.writerow(["tool", "name", "group", "path", "property", "expected", "status", "category",	"result", "log"])

tools = df['tool'].unique()

for t in tools:
    tool_mask = (df['tool'] == t)
    assert_mask = (df['properties'] == 'valid-assert')
    status_mask = (df['status'] == 'false')
    category_mask = (df['category'] == 'correct')

    mask = tool_mask & assert_mask & status_mask & category_mask

    for r in df[mask].iterrows():
        index, bench = r
        process_bench(index, bench)
        break

    
