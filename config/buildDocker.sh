#!/bin/bash

#install IK plugins
docker build --force-rm -t elasticsearch-plugins:7.12.1 --file Dockerfile .