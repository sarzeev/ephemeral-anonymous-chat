package com.example.chatbackend.security;

import com.example.chatbackend.model.Message;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class MessageHmacValidator {

    public String generateHmac(String encodedSessionKey, Message message) {
        try {
            byte[] rawKey = Base64.getDecoder().decode(encodedSessionKey);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(rawKey, "HmacSHA256"));
            byte[] digest = mac.doFinal(signaturePayload(message).getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (IllegalArgumentException | NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Unable to compute message HMAC", exception);
        }
    }

    public boolean isValid(String encodedSessionKey, Message message) {
        return generateHmac(encodedSessionKey, message).equals(message.hmacSignature());
    }

    private String signaturePayload(Message message) {
        return message.messageId()
                + '|'
                + message.sessionId()
                + '|'
                + message.senderId()
                + '|'
                + message.encryptedPayload()
                + '|'
                + message.iv();
    }
}

