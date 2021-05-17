#!/bin/bash

docker cp elasticsearch-analysis-ik-7.9.3.zip es01:/usr/share/elasticsearch/plugins
#
#docker exec -it es01 bash
#
#cd plugins
#mkdir ik
#mv elasticsearch-analysis-ik-7.9.3.zip ik
#unzip ik/*.zip