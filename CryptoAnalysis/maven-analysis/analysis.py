#!/usr/bin/python


import sys
from subprocess import call

PROJECTS = "maven_20_most_referenced_projects"

# Read in the file
with open(PROJECTS, 'r') as file:
  lines = file.readlines()
 
for line in lines:
	if line.startswith("#"): 
		continue
	line = line.replace("'","")
	print("Analyzing " + line)
	groupId,artifactId,_,version = line.split(":")
  	call(["python", "cryptocheck.py",groupId, artifactId, version])
  	