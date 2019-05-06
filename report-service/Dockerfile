FROM openjdk:8-jre
WORKDIR /opt/report-service
COPY ./build/libs/report-service-1.0.jar /opt/data-collector/report-service.jar
EXPOSE 8080

## THE LIFE SAVER
ADD https://github.com/ufoscout/docker-compose-wait/releases/download/2.5.0/wait /wait
RUN chmod +x /wait
## Launch the wait tool and then the application
ADD ./deploy/report-service_run.sh /report-service_run.sh
ENTRYPOINT ["sh","/report-service_run.sh"]

