package org.justcards.android.models;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.justcards.android.utils.Constants;
import org.parceler.Parcel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Context.MODE_PRIVATE;
import static org.justcards.android.utils.AppUtils.isEmpty;
import static org.justcards.android.utils.Constants.DISPLAY_NAME;
import static org.justcards.android.utils.Constants.IS_ACTIVE;
import static org.justcards.android.utils.Constants.IS_DEALER;
import static org.justcards.android.utils.Constants.IS_SHOWING_CARDS;
import static org.justcards.android.utils.Constants.SCORE;
import static org.justcards.android.utils.Constants.TAG;
import static org.justcards.android.utils.Constants.USER_AVATAR_URI;
import static org.justcards.android.utils.Constants.USER_ID;
import static org.justcards.android.utils.Constants.USER_PREFS;
import static org.justcards.android.utils.PlayerUtils.getDefaultAvatar;

@Parcel(analyze = User.class)
public class User {

    //DO NOT CHANGE ANY VARIABLE NAME
    public String userId; //match this with userId of Parse to keep them unique
    public String displayName;
    public String avatarUri;
    public boolean isDealer;
    public boolean isShowingCards;
    public boolean isActive = true;
    public int score;
    public HashMap<String, Card> cards = new HashMap<>();

    public User() {
    }

    public User(String avatarUri, String displayName, String userId) {
        this.avatarUri = avatarUri;
        this.displayName = displayName;
        this.userId = userId;
    }

    public User(String avatarUri, String displayName, String userId, boolean isDealer, boolean isShowingCards, boolean isActive, int score) {
        this(avatarUri, displayName, userId);
        this.isDealer = isDealer;
        this.isShowingCards = isShowingCards;
        this.isActive = isActive;
        this.score = score;
    }

    public static User fromMap(Map<?, String> map) {
        String userId = map.get(USER_ID);
        String displayName = map.get(DISPLAY_NAME);
        String avatarUri = map.get(USER_AVATAR_URI);
        boolean isDealer = Boolean.valueOf(map.get(IS_DEALER));
        boolean isShowingCards = Boolean.valueOf(map.get(IS_SHOWING_CARDS));
        boolean isActive = Boolean.valueOf(map.get(IS_ACTIVE));
        int score = Integer.valueOf(map.get(SCORE));
        Log.d(TAG, "fromMap -- " + userId + " -- " + displayName + " -- " + avatarUri);
        return new User(avatarUri, displayName, userId, isDealer, isShowingCards, isActive, score);
    }

    public static User fromJson(String jsonString) {
        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();
        String userId = json.get(USER_ID).getAsString();
        String displayName = json.get(DISPLAY_NAME).getAsString();
        String avatarUri = json.get(USER_AVATAR_URI).getAsString();
        boolean isDealer = json.get(IS_DEALER).getAsBoolean();
        boolean isShowingCards = json.get(IS_SHOWING_CARDS).getAsBoolean();
        boolean isActive = json.get(IS_ACTIVE).getAsBoolean();
        int score = json.get(SCORE).getAsInt();
        Log.d(TAG, "fromJson--" + userId + "--" + displayName + "--" + avatarUri);
        return new User(avatarUri, displayName, userId, isDealer, isShowingCards, isActive, score);
    }

    public static JsonObject getJson(User user) {
        JsonObject json = new JsonObject();
        json.addProperty(USER_ID, user.getUserId());
        json.addProperty(DISPLAY_NAME, user.getDisplayName());
        json.addProperty(USER_AVATAR_URI, user.getAvatarUri());
        json.addProperty(IS_DEALER, user.isDealer());
        json.addProperty(Constants.IS_SHOWING_CARDS, user.isShowingCards());
        json.addProperty(Constants.IS_ACTIVE, user.isActive);
        json.addProperty(Constants.SCORE, user.getScore());
        return json;
    }

    public static User getCurrentUser(final Context context) {
        SharedPreferences userPrefs = context.getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        String userId = userPrefs.getString(USER_ID, "usedIdUnknown");
        String displayName = userPrefs.getString(DISPLAY_NAME, "unknown");
        String avatarUri = userPrefs.getString(USER_AVATAR_URI, getDefaultAvatar());
        boolean isDealer = userPrefs.getBoolean(IS_DEALER, false);
        boolean isShowingCards = userPrefs.getBoolean(IS_SHOWING_CARDS, false);
        boolean isActive = userPrefs.getBoolean(IS_ACTIVE, true);
        int score = userPrefs.getInt(SCORE, 0);
        return new User(avatarUri, displayName, userId, isDealer, isShowingCards, isActive, score);
    }

    public static boolean isSelf(final User user, final Context context) {
        return getCurrentUser(context).equals(user);
    }

    public static User get(final Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(USER_PREFS, MODE_PRIVATE);
        String userId = sharedPreferences.getString(USER_ID, "");
        String displayName = sharedPreferences.getString(DISPLAY_NAME, "");
        String avatarUri = sharedPreferences.getString(USER_AVATAR_URI, "");
        if (!displayName.isEmpty() && !userId.isEmpty()) {
            return new User(avatarUri, displayName, userId, false, false, true, 0);
        } else {
            return null;
        }
    }

    public void save(final Context context) {
        context.getSharedPreferences(USER_PREFS, MODE_PRIVATE)
                .edit()
                .putString(USER_ID, userId)
                .putString(DISPLAY_NAME, displayName)
                .putString(USER_AVATAR_URI, avatarUri)
                .putBoolean(IS_DEALER, isDealer)
                .putBoolean(IS_SHOWING_CARDS, isShowingCards)
                .putBoolean(IS_ACTIVE, isActive)
                .putInt(SCORE, score)
                .apply();
    }

    public User saveIsDealer(boolean isDealer, final Context context) {
        setDealer(isDealer);
        save(context);
        return this;
    }

    public User flipIsShowingCards(final Context context) {
        setShowingCards(!isShowingCards);
        save(context);
        return this;
    }

    public User flipIsActive(final Context context) {
        setActive(!isActive);
        save(context);
        return this;
    }

    public User saveScore(int score, final Context context) {
        setScore(score);
        save(context);
        return this;
    }

    public User resetForRound(final Context context) {
        resetForRound();
        save(context);
        return this;
    }

    public User reset(final Context context) {
        resetForRound();
        setDealer(false);
        setScore(0);
        save(context);
        return this;
    }

    public static void resetForRound(final List<User> users) {
        if (!isEmpty(users)) {
            for (User user : users) {
                user.resetForRound();
            }
        }
    }

    private void resetForRound() {
        isShowingCards = false;
        isActive = true;
        cards = new HashMap<>();
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getAvatarUri() {
        return avatarUri;
    }

    public boolean isDealer() {
        return isDealer;
    }

    public void setDealer(boolean dealer) {
        isDealer = dealer;
    }

    public boolean isShowingCards() {
        return isShowingCards;
    }

    public void setShowingCards(boolean showingCards) {
        isShowingCards = showingCards;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public HashMap<String, Card> getCards() {
        return cards;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId='" + userId + '\'' +
                ", displayName='" + displayName + '\'' +
                ", avatarUri='" + avatarUri + '\'' +
                ", isDealer=" + isDealer +
                ", isShowingCards=" + isShowingCards +
                ", isActive=" + isActive +
                ", score=" + score +
                ", cards=" + cards +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;
        return userId != null ? userId.equals(user.userId) : user.userId == null;
    }

    @Override
    public int hashCode() {
        return userId != null ? userId.hashCode() : 0;
    }
}