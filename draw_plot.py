import os
import sys
import numpy as np
import matplotlib.pyplot as plt
import csv

log_file_1 = "log/scythe_2_1.log"
log_file_2 = "log/c_2_1.log"

def main(log_file_1, log_file_2):
  stats = {}
  with open(log_file_1) as csvfile:
    reader = csv.DictReader(csvfile, skipinitialspace=True)
    for row in reader:
      entry = {}
      entry["visited"] = row["visited"]
      entry["collected_1"] = row["collected"]
      entry["time_1"] = row["time"]
      stats[row["file"]] = entry
  with open(log_file_2) as csvfile:
    reader = csv.DictReader(csvfile, skipinitialspace=True)
    for row in reader:
      if ((stats[row["file"]]["visited"] != row["visited"])
            and (stats[row["file"]]["visited"] != "-1") and (row["visited"] != "-1")):
        print "[fatal error] visited number not equal", stats[row["file"]]["visited"], row["visited"]
      stats[row["file"]]["collected_2"] = row["collected"]
      stats[row["file"]]["time_2"] = row["time"]

  x = []
  index = []
  y1 = []
  y2 = []
  time_1 = [] 
  time_2 = []
  i = 1
  for f in stats:
    entry = stats[f]
    #print entry
    if entry["time_1"] != "-1" and entry["time_2"] != "-1":
      x.append(f)
      index.append(i)
      i += 1
      y1.append( float(entry["collected_1"]) / float(entry["visited"]))
      y2.append( float(entry["collected_2"]) / float(entry["visited"]))
      time_1.append(float(entry["time_1"]))
      time_2.append(float(entry["time_2"]))
    
  plot2(index, y1, y2, [0, len(x), 0, 1])
  plot2(index, time_1, time_2, [0, len(x), 0, 1])

def plot(x, y1, y2, axis):
  #plt.plot(x, y1, 'r-')
  plt.plot(x, y1, 'ro')
  #plt.plot(x, y2, 'b-')
  plt.plot(x, y2, 'bo')
  plt.axis(axis)
  plt.show()

def plot2(x, y1, y2, axis):
  fig, ax = plt.subplots()
  ind = np.arange(len(x))
  opacity = 0.5
  width = 0.5
  ax.bar(ind, y1, width, color='b', alpha=opacity)
  ax.bar(ind, y2, width, color='r', alpha=opacity)
  #ax.set_ylabel('Scores')
  #ax.set_title('Scores by group and gender')
  #ax.set_xticks(ind + width)
  #ax.set_xticklabels(x)
  plt.show()

def plot3(x, y1, y2, axis):
  delta = []
  for i in range(0, len(y1)):
    delta.append(y2[i] - y1[i])
  fig, ax = plt.subplots()
  ind = np.arange(len(x))
  width = 0.5
  ax.bar(ind, delta, width, color='b')
  #ax.set_ylabel('Scores')
  #ax.set_title('Scores by group and gender')
  #ax.set_xticks(ind + width)
  #ax.set_xticklabels(x)
  plt.show()

if __name__ == '__main__':
  if len(sys.argv) == 3:
    log_file_1 = sys.argv[1]
    log_file_2 = sys.argv[2]
  main(log_file_1, log_file_2)
