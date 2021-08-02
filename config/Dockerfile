
#这里不是使用的ECK 里面的镜像，注意区别
FROM elasticsearch:7.12.1
LABEL maintainer Jeven Wang

RUN cd /usr/share/elasticsearch/plugins/ && \
    mkdir ik && \
    cd ik

ADD plugins/elasticsearch-analysis-ik-7.12.1.zip /usr/share/elasticsearch/plugins/ik
RUN cd /usr/share/elasticsearch/plugins/ik && \
    unzip *.zip && \
    ls -l /usr/share/elasticsearch/plugins/ik/

#RUN unzip elasticsearch-analysis-ik-7.9.3.zip && ls -l
