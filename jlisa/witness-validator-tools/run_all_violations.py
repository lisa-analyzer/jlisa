import csv
import os
import subprocess

results = {}

top_level = {
    "VALID_ASSERT": "SV-COMP26_valid-assert",
    "NO_RUNTIME": "SV-COMP26_no-runtime-exception",
}

env = os.environ.copy()
#env['JAVA_HOME'] = '/usr/lib/jvm/java-26-openjdk'

cmd = [
    "./build/install/jlisa-witness-validator/bin/jlisa-witness-validator",
    "--witness", 
    "data/svcomp-26/results-verified/jbmc.2025-12-09_16-35-43.files/TOP_LEVEL/BENCHMARK/witness.graphml",
    "--benchmark",
    "data/svcomp-26/benchmarks/java/", # jbmc-regression/CharSequenceBug
    "--extra-sources", 
    "svcomp-26/benchmarks/java/common"       , '--verbose' 
]

def process_benchmarks():
    for i, row in enumerate(reader):
        if i == 0:
            continue

        _, bench, _, tp, _ = row

        local = cmd.copy()


        if tp == 'valid-assert':
            local[2] = local[2].replace('TOP_LEVEL', top_level['VALID_ASSERT'])
        elif tp == 'no-runtime-exception':
            continue
            local[2] = local[2].replace('TOP_LEVEL', top_level['NO_RUNTIME'])

        bench_path = bench.split('/')
        local[2] = local[2].replace('BENCHMARK', bench.split('/')[-1])
        assert os.path.exists(local[2])

        local[4] = local[4] + bench
        assert os.path.exists(local[4])

        out = subprocess.run(local, env=env, capture_output=True)
        result = out.stdout.decode().strip()

        if result == 'Witness Correct':
            print(0)
        else:
            results[bench] = [result, out.stderr.decode("utf-8")]
            print(1)
            print(bench)
            print(result)
            print(out.stderr.decode("utf-8"))
            print()

        with open('output/report.csv', 'a', newline="", encoding="utf-8") as o:
            spamwriter = csv.writer(o)
            for k, v in results.items():
                r = [k] + v
                spamwriter.writerow(r)

with open('data/svcomp-26/benchmark_tasks_1.csv') as f:
    reader = csv.reader(f) #, delimiter=' ', quotechar='|')
    open('output/report.csv', 'w').close()
    process_benchmarks()
