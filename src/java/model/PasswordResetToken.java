package model;

import java.time.LocalDateTime;

public class PasswordResetToken {

    private int tokenId;
    private int userId;
    private String resetToken;
    private LocalDateTime expiredAt;
    private boolean isUsed;
    private LocalDateTime createdAt;
    private User user;

    public PasswordResetToken() {}

    public PasswordResetToken(int tokenId, int userId, String resetToken,
                              LocalDateTime expiredAt, boolean isUsed, LocalDateTime createdAt) {
        this.tokenId    = tokenId;
        this.userId     = userId;
        this.resetToken = resetToken;
        this.expiredAt  = expiredAt;
        this.isUsed     = isUsed;
        this.createdAt  = createdAt;
    }

    public int getTokenId()                             { return tokenId; }
    public void setTokenId(int tokenId)                 { this.tokenId = tokenId; }

    public int getUserId()                              { return userId; }
    public void setUserId(int userId)                   { this.userId = userId; }

    public String getResetToken()                       { return resetToken; }
    public void setResetToken(String resetToken)        { this.resetToken = resetToken; }

    public LocalDateTime getExpiredAt()                 { return expiredAt; }
    public void setExpiredAt(LocalDateTime expiredAt)   { this.expiredAt = expiredAt; }

    public boolean isUsed()                             { return isUsed; }
    public void setUsed(boolean isUsed)                 { this.isUsed = isUsed; }

    public LocalDateTime getCreatedAt()                 { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt)   { this.createdAt = createdAt; }

    public User getUser()                               { return user; }
    public void setUser(User user)                      { this.user = user; }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiredAt);
    }
}
