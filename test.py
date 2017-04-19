#!/usr/bin/python

import sys
import subprocess32
import os
from subprocess32 import check_output, PIPE, TimeoutExpired

import re

test_dir = "data/"
dylib_location = "lib"
jar_path = "out/artifacts/EnumExperiment_jar/EnumExperiment.jar"


#java -Djava.library.path=lib/ -jar out/artifacts/EnumExperiment_jar/EnumExperiment.jar

def main(strategy, search_depth, complex_query_depth):
  print "file, collected, visited, time"
  for root, dirs, filenames in os.walk(test_dir):
    for f in filenames:
      if ("X" in f) or ("R" in f):
        continue
      target_file = os.path.join(root, f)
      print os.path.join(root, f), ",",'%s' % ', '.join(map(str, run(target_file, strategy, search_depth, complex_query_depth)))

def run(target_file, pruning_strategy, max_depth, complex_query_depth):
  try:
    command = ["java", "-Djava.library.path=" + dylib_location ,"-jar", jar_path, target_file, "-d", str(max_depth), pruning_strategy, "--complex-aggr-depth", str(complex_query_depth)]
    output = check_output(command, stdin=PIPE, stderr=PIPE, timeout=300)

    for s in output.splitlines():
      if s.startswith("[#Collected]"):
        collected = int(s[s.index("]") + 1:])
      if s.startswith("[#Visited]"):
        visited = int(s[s.index("]") + 1:])
      if s.startswith("[[Synthesis Time]]"):
        time = float(s[s.index("]]") + 2:-1])

    return [collected, visited, time]
  except TimeoutExpired:
    return [-1, -1, -1]

if __name__ == '__main__':
  if (sys.argv[1] != "-a" or sys.argv[1] != "-c"):
    print "Error: Pruning strategy not correctly provided"
  else:
    main(sys.argv[1], sys.argv[2], sys.argv[3])
