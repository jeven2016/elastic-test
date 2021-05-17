FROM elasticsearch:7.9.3
LABEL maintainer Jeven Wang

RUN cd /usr/share/elasticsearch/plugins/ && \
    mkdir ik && \
    cd ik

ADD plugins/elasticsearch-analysis-ik-7.9.3.zip /usr/share/elasticsearch/plugins/ik
RUN cd /usr/share/elasticsearch/plugins/ik && \
    unzip *.zip && \
    ls -l /usr/share/elasticsearch/plugins/ik/

#RUN unzip elasticsearch-analysis-ik-7.9.3.zip && ls -l
