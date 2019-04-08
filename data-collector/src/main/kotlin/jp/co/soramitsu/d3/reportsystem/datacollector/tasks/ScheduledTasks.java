package jp.co.soramitsu.d3.reportsystem.datacollector.tasks;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasks.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(fixedDelayString = "${scheduling.iroha.block.request}", initialDelay=5000)
    public void processBlock() {
        log.info("The time is now {}", dateFormat.format(new Date()));
    }
}