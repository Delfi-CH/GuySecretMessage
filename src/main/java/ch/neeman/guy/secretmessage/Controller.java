package ch.neeman.guy.secretmessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/secretmessage")
@CrossOrigin("*")
public class Controller {

    private final BCryptPasswordEncoder encoder;
    private final SecretMessageRepository repo;
    private final SecretMessageApplication secretMessageApplication;

    @Autowired
    public Controller(BCryptPasswordEncoder encoder, SecretMessageRepository repo, SecretMessageApplication secretMessageApplication) {
        this.encoder = encoder;
        this.repo = repo;
        this.secretMessageApplication = secretMessageApplication;
    }

    @PostMapping("/createmessage")
    public ResponseEntity<String> createMessage(@RequestBody Map<String, String> request) {

        String message = request.get("message");
        String password = encoder.encode(request.get("password"));

        String language = request.get("language");

        Boolean selfdelete = Boolean.valueOf(request.get("selfdelete"));
        LocalDateTime expireAt;
        if (selfdelete) {
            expireAt = null;
        } else {
            expireAt = LocalDateTime.now().plusMinutes(Integer.parseInt(request.get("expirationminute")));
        }
        String uuid = UUID.randomUUID().toString();

        SecretMessage msg = new SecretMessage(
                message,
                password,
                expireAt,
                selfdelete,
                uuid,
                language
        );

        SecretMessage saved = repo.save(msg);

        return ResponseEntity.ok(uuid);
    }

    @PostMapping("/showmessage/{uuid}")
    public ResponseEntity<Map<String, String>> showMessage(@PathVariable String uuid, @RequestBody Map<String, String> request) {
        String password = repo.findByUuid(uuid)
                .map(SecretMessage::getPassword)
                .orElse(null);
        String message = repo.findByUuid(uuid)
                .map(SecretMessage::getMessage)
                .orElse(null);
        Boolean selfdelete = repo.findByUuid(uuid)
                .map(SecretMessage::getSelfdelete)
                .orElse(null);
        String language = repo.findByUuid(uuid)
                .map(SecretMessage::getLanguage)
                .orElse(null);
        if (encoder.matches(request.get("password"), password)) {
            if (selfdelete) {
                repo.findByUuid(uuid).ifPresent(repo::delete);
            }
            return ResponseEntity.ok(Map.of(
                    "message", message,
                    "language", language
            ));
        }
        return ResponseEntity.status(403).body(Map.of("error", "Password is wrong!"));
    }

    @PostMapping("/checkExistance/{uuid}")
    public ResponseEntity<String> checkExistance(@PathVariable String uuid) {
        return repo.findByUuid(uuid)
                .map(secretMessage -> {
                    if (secretMessage.getExpireAt() == null) {
                        return ResponseEntity.ok("Message exists (self-delete enabled)!");
                    }

                    if (LocalDateTime.now().isAfter(secretMessage.getExpireAt())) {
                        repo.delete(secretMessage);
                        return ResponseEntity.status(410).body("Message has expired!");
                    }

                    return ResponseEntity.ok("Message exists!");
                })
                .orElseGet(() -> ResponseEntity.status(404).body("Message not found"));
    }
}
