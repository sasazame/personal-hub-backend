package com.zametech.personalhub.application.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.zametech.personalhub.domain.model.User;
import com.zametech.personalhub.domain.model.UserSocialAccount;
import com.zametech.personalhub.domain.repository.UserSocialAccountRepository;
import com.zametech.personalhub.infrastructure.security.TokenEncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import com.zametech.personalhub.common.exception.TokenDecryptionException;

/**
 * OAuth2認証を使用したGoogle Calendar Service
 * ユーザーごとのアクセストークンを使用してカレンダーにアクセス
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleCalendarOAuth2Service {
    
    private static final String APPLICATION_NAME = "Personal Hub Calendar Sync";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String PROVIDER_NAME = "google";
    
    private final UserSocialAccountRepository socialAccountRepository;
    private final TokenEncryptionService tokenEncryptionService;
    private final GoogleOidcService googleOidcService;
    
    /**
     * ユーザーのOAuth2トークンを使用してCalendarサービスを作成
     */
    public Calendar getCalendarService(User user) throws IOException, GeneralSecurityException {
        // ユーザーのGoogleアカウント情報を取得
        Optional<UserSocialAccount> socialAccountOpt = socialAccountRepository
            .findByUserIdAndProvider(user.getId(), PROVIDER_NAME);
        
        if (socialAccountOpt.isEmpty()) {
            throw new IllegalStateException("User has not connected their Google account");
        }
        
        UserSocialAccount socialAccount = socialAccountOpt.get();
        
        // アクセストークンを復号化
        String accessToken = tokenEncryptionService.decryptToken(
            socialAccount.getAccessTokenEncrypted()
        );
        
        if (accessToken == null) {
            throw new IllegalStateException("Access token could not be decrypted. Please re-authenticate with Google.");
        }
        
        // トークンの有効期限をチェック
        if (socialAccount.getTokenExpiresAt() != null && 
            socialAccount.getTokenExpiresAt().isBefore(LocalDateTime.now())) {
            log.info("Access token expired for user {}, attempting to refresh", user.getId());
            try {
                googleOidcService.refreshAccessToken(socialAccount);
                // リフレッシュ後、再度アクセストークンを取得
                accessToken = tokenEncryptionService.decryptToken(
                    socialAccount.getAccessTokenEncrypted()
                );
            } catch (Exception e) {
                log.error("Failed to refresh access token for user {}", user.getId(), e);
                throw new IllegalStateException("Failed to refresh access token", e);
            }
        }
        
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        
        // OAuth2認証情報を作成
        Credential credential = new GoogleCredential.Builder()
            .setTransport(HTTP_TRANSPORT)
            .setJsonFactory(JSON_FACTORY)
            .build()
            .setAccessToken(accessToken);
        
        return new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }
    
    /**
     * ユーザーのカレンダーリストを取得
     */
    public List<CalendarListEntry> getUserCalendars(User user) {
        try {
            Calendar service = getCalendarService(user);
            log.info("Fetching calendar list for user {}", user.getId());
            
            CalendarList calendarList = service.calendarList().list().execute();
            List<CalendarListEntry> items = calendarList.getItems();
            
            if (items == null || items.isEmpty()) {
                log.warn("No calendars found for user {}", user.getId());
                return Collections.emptyList();
            }
            
            log.info("Found {} calendars for user {}", items.size(), user.getId());
            for (CalendarListEntry entry : items) {
                log.info("Calendar: {} ({})", entry.getSummary(), entry.getId());
            }
            
            return items;
        } catch (Exception e) {
            log.error("Error fetching calendars for user {}: {}", user.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * カレンダーからイベントを取得
     */
    public List<Event> getCalendarEvents(User user, String calendarId, 
                                       LocalDateTime startTime, LocalDateTime endTime) {
        try {
            Calendar service = getCalendarService(user);
            log.info("Fetching events from calendar {} for user {}", calendarId, user.getId());
            
            Events events = service.events().list(calendarId)
                .setTimeMin(new com.google.api.client.util.DateTime(
                    startTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setTimeMax(new com.google.api.client.util.DateTime(
                    endTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute();
            
            List<Event> items = events.getItems();
            log.info("Found {} events in calendar {}", items.size(), calendarId);
            
            return items;
        } catch (Exception e) {
            log.error("Error fetching events from calendar {} for user {}: {}", 
                    calendarId, user.getId(), e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * カレンダーにイベントを作成
     */
    public Optional<String> createCalendarEvent(User user, String calendarId, 
                                              com.zametech.personalhub.domain.model.Event domainEvent) {
        try {
            Calendar service = getCalendarService(user);
            
            // Google Calendar Event形式に変換
            Event googleEvent = convertToGoogleEvent(domainEvent);
            
            Event createdEvent = service.events().insert(calendarId, googleEvent).execute();
            log.info("Created event {} in calendar {} for user {}", 
                    createdEvent.getId(), calendarId, user.getId());
            
            return Optional.of(createdEvent.getId());
        } catch (Exception e) {
            log.error("Error creating event in calendar {} for user {}: {}", 
                    calendarId, user.getId(), e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * カレンダーのイベントを更新
     */
    public boolean updateCalendarEvent(User user, String calendarId, String eventId,
                                     com.zametech.personalhub.domain.model.Event domainEvent) {
        try {
            // 繰り返しイベントのインスタンスかチェック
            if (eventId.contains("_")) {
                log.warn("Skipping update for recurring event instance: {}", eventId);
                return true; // エラーではなく、スキップとして扱う
            }
            
            Calendar service = getCalendarService(user);
            
            // Google Calendar Event形式に変換
            Event googleEvent = convertToGoogleEvent(domainEvent);
            
            service.events().update(calendarId, eventId, googleEvent).execute();
            log.info("Updated event {} in calendar {} for user {}", 
                    eventId, calendarId, user.getId());
            
            return true;
        } catch (Exception e) {
            log.error("Error updating event {} in calendar {} for user {}: {}", 
                    eventId, calendarId, user.getId(), e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * ドメインイベントをGoogle Calendar Event形式に変換
     */
    private Event convertToGoogleEvent(com.zametech.personalhub.domain.model.Event domainEvent) {
        Event googleEvent = new Event()
            .setSummary(domainEvent.getTitle())
            .setDescription(domainEvent.getDescription())
            .setLocation(domainEvent.getLocation());
        
        // 開始・終了時刻の設定
        if (domainEvent.getStartDateTime() != null) {
            if (domainEvent.isAllDay()) {
                // 全日イベントの場合
                googleEvent.setStart(new com.google.api.services.calendar.model.EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(true,
                        domainEvent.getStartDateTime().toLocalDate().atStartOfDay()
                            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        null)));
            } else {
                // 時刻指定イベントの場合
                googleEvent.setStart(new com.google.api.services.calendar.model.EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(
                        domainEvent.getStartDateTime().atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()))
                    .setTimeZone(java.time.ZoneId.systemDefault().getId()));
            }
        }
        
        if (domainEvent.getEndDateTime() != null) {
            if (domainEvent.isAllDay()) {
                // 全日イベントの場合
                googleEvent.setEnd(new com.google.api.services.calendar.model.EventDateTime()
                    .setDate(new com.google.api.client.util.DateTime(true,
                        domainEvent.getEndDateTime().toLocalDate().plusDays(1).atStartOfDay()
                            .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        null)));
            } else {
                // 時刻指定イベントの場合
                googleEvent.setEnd(new com.google.api.services.calendar.model.EventDateTime()
                    .setDateTime(new com.google.api.client.util.DateTime(
                        domainEvent.getEndDateTime().atZone(java.time.ZoneId.systemDefault())
                            .toInstant().toEpochMilli()))
                    .setTimeZone(java.time.ZoneId.systemDefault().getId()));
            }
        }
        
        // リマインダーの設定
        if (domainEvent.getReminderMinutes() != null) {
            googleEvent.setReminders(new com.google.api.services.calendar.model.Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Collections.singletonList(
                    new com.google.api.services.calendar.model.EventReminder()
                        .setMethod("popup")
                        .setMinutes(domainEvent.getReminderMinutes())
                )));
        }
        
        return googleEvent;
    }
}