package com.codedev.demo.controller;

import com.codedev.demo.job.EmailJob;
import com.codedev.demo.model.EmailRequest;
import com.codedev.demo.model.EmailResponse;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;

@RestController
@Slf4j
public class EmailSchedulerController {

    // private static final Logger logger = LoggerFactory.getLogger(EmailJobSchedulerController.class);

    @Autowired
    private Scheduler scheduler;

    @PostMapping("/schedule/email")
    public ResponseEntity<EmailResponse> scheduleEmail(@Valid @RequestBody EmailRequest emailRequest) {
        try {
            // if request json data went wrong
            if (emailRequest.getDateTime() == null || emailRequest.getTimeZone() == null) {
                EmailResponse emailResponse = new EmailResponse(
                        false,
                        "Something wrong with the json data, please check!");
                return ResponseEntity.badRequest().body(emailResponse);
            }

            // if the dateTime of email is old, don't schedule
            ZonedDateTime dateTime = ZonedDateTime.of(
                    emailRequest.getDateTime(), emailRequest.getTimeZone());
            if (dateTime.isBefore(ZonedDateTime.now())) {
                EmailResponse emailResponse = new EmailResponse(
                        false,
                        "dateTime must be after current time");
                // .status(HttpStatus.BAD_REQUEST)
                return ResponseEntity.badRequest().body(emailResponse);
            }

            // create jobDetail
            JobDetail jobDetail = buildJobDetail(emailRequest);

            // create trigger
            Trigger trigger = buildJobTrigger(jobDetail, dateTime);

            // add jobDetail and trigger to scheduler
            // scheduler 要知道执行何种类型的 job
            // 每次当 scheduler 执行 job 时，在调用其execute(…)方法之前会创建该类的一个新的实例；
            // 执行完毕，对该实例的引用就被丢弃了，实例会被垃圾回收；
            scheduler.scheduleJob(jobDetail, trigger);

            EmailResponse emailResponse = new EmailResponse(
                    true,
                    jobDetail.getKey().getName(),
                    jobDetail.getKey().getGroup(),
                    "Email Scheduled Successfully!");
            return ResponseEntity.ok(emailResponse);
        } catch (SchedulerException ex) {
            log.error("Error scheduling email", ex);
            EmailResponse emailResponse = new EmailResponse(
                    false,
                    "Error while scheduling email. Please try later!");
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(emailResponse);
        }
    }

    @GetMapping("/test/get")
    public ResponseEntity<String> testApi() {
        return ResponseEntity.ok("Test Get API");
    }

    private JobDetail buildJobDetail(EmailRequest scheduleEmailRequest) {
        // define job data (map)
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("email", scheduleEmailRequest.getEmail());
        jobDataMap.put("subject", scheduleEmailRequest.getSubject());
        jobDataMap.put("body", scheduleEmailRequest.getBody());

        // build a jobDetail
        return JobBuilder.newJob(EmailJob.class) // contain the job class
                .withIdentity(UUID.randomUUID().toString(), "email-jobs") // job id, group
                .withDescription("Send Email Job")
                .usingJobData(jobDataMap) // contain the job data
                .storeDurably() // store the jobDetail in db
                .build();
    }

    private Trigger buildJobTrigger(JobDetail jobDetail, ZonedDateTime startAt) {
        // build a trigger for a jobDetail
        return TriggerBuilder.newTrigger()
                .forJob(jobDetail)
                .withIdentity(jobDetail.getKey().getName(), "email-triggers") // trigger id, group
                .withDescription("Send Email Trigger")
//                .startNow() // 一旦加入scheduler，立即生效
//                .withSchedule(
//                        SimpleScheduleBuilder
//                                .simpleSchedule() // 使用 SimpleTrigger
//                                .withIntervalInSeconds(1) // 每隔一秒执行一次
//                                .repeatForever()) // 一直执行
//                .build();
                .startAt(Date.from(startAt.toInstant())) // time to trigger
                .withSchedule(
                        SimpleScheduleBuilder
                                .simpleSchedule()
                                .withMisfireHandlingInstructionFireNow())
                .build();
    }
}
