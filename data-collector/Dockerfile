FROM openjdk:8-jre
WORKDIR /opt/data-collector
COPY ./build/libs/data-collector-1.0.jar /opt/data-collector/data-collector.jar
EXPOSE 8080

## THE LIFE SAVER
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN chmod +x /wait
## Launch the wait tool and then the application
ADD ./deploy/data-collector_run.sh /data-collector_run.sh
ENTRYPOINT ["sh","/data-collector_run.sh"]

