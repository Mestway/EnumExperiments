#!/usr/bin/python

import sys
import subprocess32
import os
from subprocess32 import check_output, PIPE, TimeoutExpired

import re

test_dir = "data/dev_set"
dylib_location = "lib"
jar_path = "out/artifacts/EnumExperiment_jar/EnumExperiment.jar"

#java -Djava.library.path=lib/ -jar out/artifacts/EnumExperiment_jar/EnumExperiment.jar

def main():
  print "file, collected, visited, time"
  for root, dirs, filenames in os.walk(test_dir):
    for f in filenames:
      target_file = os.path.join(root, f)
      print f, ",",'%s' % ', '.join(map(str, run(target_file, "-a", str(1), str(0))))

def run(target_file, pruning_strategy, max_depth, complex_query_depth):
  try:
    command = ["java", "-Djava.library.path=" + dylib_location ,"-jar", jar_path, target_file, "-d", max_depth, pruning_strategy, "--complex-aggr-depth", complex_query_depth]
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
  main()
