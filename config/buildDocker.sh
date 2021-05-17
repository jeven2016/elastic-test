#!/bin/bash

#install IK plugins
docker build --force-rm -t elasticsearch-plugins:7.9.3 --file Dockerfile .