#!/bin/bash
#SBATCH -p core
#SBATCH -n 2
#SBATCH -t 12:00:00
#SBATCH -J some_job_name
#SBATCH -o test-%j.out
#SBATCH -e test-%j.error

sleep 20
echo "Hello Cluster!"
