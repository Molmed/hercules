#!/bin/bash -l
set -o nounset
set -o errexit

# Will download the test data compiled on mm-cluster 
mkdir -p test_data
rsync -r --progress mm-cluster:/data/hercules_test_data/minimal_test_data/ test_data/
