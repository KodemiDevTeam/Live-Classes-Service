package com.example.live_classes_service.contoller;

import com.example.live_classes_service.model.LiveClassEntity;
import com.example.live_classes_service.repository.LiveClassRepository;
import com.example.live_classes_service.service.impl.S3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/webhooks")

public class VideoSDKWebhookController {


    private final LiveClassRepository repository;
    private final S3Service s3Service;

    public VideoSDKWebhookController(LiveClassRepository repository, S3Service s3Service) {
        this.repository = repository;
        this.s3Service = s3Service;
    }


    @PostMapping("/videosdk")
    public ResponseEntity<String> handleWebhook(@RequestBody Map<String, Object> body) {

        try {

            String webhookType = (String) body.get("webhookType");

            log.info("Received VideoSDK webhook: {}", webhookType);

            if ("recording-stopped".equals(webhookType)) {

                Map<String, Object> data = (Map<String, Object>) body.get("data");

                String roomId = (String) data.get("roomId");
                String fileUrl = (String) data.get("fileUrl");

                log.info("Recording stopped for room {} file {}", roomId, fileUrl);

                LiveClassEntity session = repository.findByRoomId(roomId);

                if (session != null) {

                    String s3Url = s3Service.uploadRecording(fileUrl, session.getLiveClassId());

                    session.setRecordingUrl(s3Url);
                    session.setIsRecording(false);

                    repository.save(session);

                    log.info("Recording uploaded to S3 {}", s3Url);

                } else {

                    log.warn("No session found for roomId {}", roomId);

                }
            }

            return ResponseEntity.ok("Webhook processed");

        } catch (Exception e) {

            log.error("Webhook processing failed", e);

            return ResponseEntity.internalServerError().body("Webhook error");
        }
    }
}