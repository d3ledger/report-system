FROM openjdk:8-jre
WORKDIR /opt/data-collector
COPY ./build/libs/data-collector-1.0.jar /opt/data-collector/data-collector.jar
EXPOSE 8080

ADD ./deploy/data-collector_run.sh /data-collector_run.sh
ENTRYPOINT ["sh","/data-collector_run.sh"]

