package org.bootcamp.fiftytwo.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.parse.ParseCloud;
import com.parse.ParsePush;

import org.bootcamp.fiftytwo.models.Card;
import org.bootcamp.fiftytwo.models.GameTable;
import org.bootcamp.fiftytwo.models.User;
import org.bootcamp.fiftytwo.utils.Constants;

import java.util.HashMap;
import java.util.List;

import static io.fabric.sdk.android.Fabric.TAG;
import static org.bootcamp.fiftytwo.models.User.getJson;
import static org.bootcamp.fiftytwo.utils.Constants.COMMON_IDENTIFIER;
import static org.bootcamp.fiftytwo.utils.Constants.FROM_POSITION;
import static org.bootcamp.fiftytwo.utils.Constants.FROM_TAG;
import static org.bootcamp.fiftytwo.utils.Constants.ON_TAG;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_CARDS;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_CARD_COUNT;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_PLAYER;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DEAL_CARDS;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DEAL_CARDS_TO_SINK;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DEAL_CARDS_TO_TABLE;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DROP_CARD_TO_SINK;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_EXCHANGE_CARD_WITH_TABLE;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_NEW_PLAYER_ADDED;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_PLAYER_LEFT;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_SCORE_UPDATED;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_SWAP_CARD_WITHIN_PLAYER;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_TOGGLE_CARD;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_TOGGLE_CARDS_LIST;
import static org.bootcamp.fiftytwo.utils.Constants.POSITION;
import static org.bootcamp.fiftytwo.utils.Constants.SERVER_FUNCTION_NAME;
import static org.bootcamp.fiftytwo.utils.Constants.TABLE_PICKED;
import static org.bootcamp.fiftytwo.utils.Constants.TO_POSITION;
import static org.bootcamp.fiftytwo.utils.Constants.TO_SHOW;
import static org.bootcamp.fiftytwo.utils.NetworkUtils.isNetworkAvailable;

/**
 * The code that processes this function is listed at:
 *
 * @link {https://github.com/rogerhu/parse-server-push-marker-example/blob/master/cloud/main.js}
 */
public class ParseUtils {

    private Context context;
    private String gameName; //used for channel name
    private User currentLoggedInUser;

    public ParseUtils(final Context context, final String gameName) {
        this.gameName = gameName;
        this.context = context;
        currentLoggedInUser = User.getCurrentUser(context);
    }

    public User getCurrentUser() {
        return currentLoggedInUser;
    }

    public void saveCurrentUserIsDealer(boolean isDealer) {
        currentLoggedInUser.setDealer(isDealer);
        currentLoggedInUser.save(context);
    }

    public void saveCurrentUserIsShowingCards(boolean isShowingCards) {
        currentLoggedInUser.setShowingCards(isShowingCards);
        currentLoggedInUser.save(context);
    }

    public static boolean isSelf(final User user) {
        return user.getUserId().equalsIgnoreCase(User.getCurrentUser().getObjectId());
    }

    public void joinChannel() {
        if (isNetworkAvailable(context)) {
            ParsePush.subscribeInBackground(gameName, e -> {
                if (e == null) {
                    Log.d(TAG, "done: Join Channel Succeeded!");
                    changeGameParticipation(true);
                } else {
                    Log.e(TAG, "done: Join Channel Failed: " + e.getMessage());
                }
            });
        }
    }

    public void removeChannel() {
        if (isNetworkAvailable(context)) {
            ParsePush.unsubscribeInBackground(gameName, e -> {
                if (e == null) {
                    Log.d(TAG, "done: Leave Channel Succeeded!");
                    changeGameParticipation(false);
                } else {
                    Log.e(TAG, "done: Leave Channel Failed: " + e.getMessage());
                }
            });
        }
    }

    private void sendBroadcast(final JsonObject payload) {
        if (isNetworkAvailable(context)) {
            HashMap<String, String> data = new HashMap<>();
            data.put("customData", payload.toString());
            data.put("channel", gameName);
            ParseCloud.callFunctionInBackground(SERVER_FUNCTION_NAME, data, (object, e) -> {
                if (e == null) {
                    Log.d(TAG, "sendBroadcast: Succeeded! " + payload.toString());
                } else {
                    Log.e(TAG, "sendBroadcast: Failed: Message: " + e.getMessage() + ": Object: " + object);
                }
            });
        }
        //TODO: retry this operation if it's network failure..
    }

    /**
     * Broadcast whether current user is joining the game or leaving
     *
     * @param joining true if joining the game , false if leaving the game
     */
    private void changeGameParticipation(boolean joining) {
        JsonObject payload = getJson(currentLoggedInUser);
        if (joining) {
            payload.addProperty(COMMON_IDENTIFIER, PARSE_NEW_PLAYER_ADDED);
        } else {
            payload.addProperty(COMMON_IDENTIFIER, PARSE_PLAYER_LEFT);
        }
        sendBroadcast(payload);
    }

    /**
     * Dealer dealing cards to a particular user
     *
     * @param toUser to whom this is sent
     * @param card   which card
     */
    public void dealCards(final User toUser, final Card card) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_PLAYER, getJson(toUser));
        payload.add(PARAM_CARDS, new Gson().toJsonTree(card));
        payload.addProperty(COMMON_IDENTIFIER, PARSE_DEAL_CARDS);
        sendBroadcast(payload);
    }

    /**
     * Dealer moving cards to table
     *
     * @param cards which cards
     */
    public void dealCardsToTable(final List<Card> cards) {
        ParseDB.deleteGameTables(gameName, () -> {
            GameTable.save(gameName, cards);
            JsonObject payload = getJson(currentLoggedInUser);
            /*payload.add(PARAM_CARDS, new Gson().toJsonTree(cards));*/
            payload.addProperty(PARAM_CARD_COUNT, cards.size());
            payload.addProperty(COMMON_IDENTIFIER, PARSE_DEAL_CARDS_TO_TABLE);
            sendBroadcast(payload);
        });
    }

    public void dealCardsToSink(final List<Card> cards) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_CARDS, new Gson().toJsonTree(cards));
        payload.addProperty(COMMON_IDENTIFIER, PARSE_DEAL_CARDS_TO_SINK);
        sendBroadcast(payload);
    }

    /**
     * Player picks up a card from the table or drops one on the table
     *
     * @param card            which card
     * @param fromPosition    position of the card where it was picked from
     * @param toPosition      position of the card where it is dropped in
     * @param pickedFromTable true: if picked from table, false: if dropped on table
     */
    public void exchangeCardWithTable(final Card card, final int fromPosition, final int toPosition, final boolean pickedFromTable) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_CARDS, new Gson().toJsonTree(card));
        payload.addProperty(FROM_POSITION, fromPosition);
        payload.addProperty(TO_POSITION, toPosition);
        payload.addProperty(TABLE_PICKED, pickedFromTable);
        payload.addProperty(COMMON_IDENTIFIER, PARSE_EXCHANGE_CARD_WITH_TABLE);
        sendBroadcast(payload);
    }

    /**
     * Player swaps a card within his/her hand from one position to another
     *
     * @param card         which card
     * @param fromPosition position of the card where it was picked from
     * @param toPosition   position of the card where it is dropped in
     */
    public void swapCardWithinPlayer(final Card card, final int fromPosition, final int toPosition) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_CARDS, new Gson().toJsonTree(card));
        payload.addProperty(FROM_POSITION, fromPosition);
        payload.addProperty(TO_POSITION, toPosition);
        payload.addProperty(COMMON_IDENTIFIER, PARSE_SWAP_CARD_WITHIN_PLAYER);
        sendBroadcast(payload);
    }

    /**
     * Player picks up a card from a stack of cards and drops them on to the Sink
     *
     * @param card         which card
     * @param fromTag      from which stack of cards
     * @param fromPosition from which position in the stack of cards
     */
    public void dropCardToSink(final Card card, final String fromTag, final int fromPosition) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_CARDS, new Gson().toJsonTree(card));
        payload.addProperty(FROM_TAG, fromTag);
        payload.addProperty(FROM_POSITION, fromPosition);
        payload.addProperty(COMMON_IDENTIFIER, PARSE_DROP_CARD_TO_SINK);
        sendBroadcast(payload);
    }

    public void toggleCard(final Card card, final int position, final String onTag) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(PARAM_CARDS, new Gson().toJsonTree(card));
        payload.addProperty(POSITION, position);
        payload.addProperty(ON_TAG, onTag);
        payload.addProperty(COMMON_IDENTIFIER, PARSE_TOGGLE_CARD);
        sendBroadcast(payload);
    }

    public void toggleCardsList(boolean toShow) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.addProperty(TO_SHOW, toShow);
        payload.addProperty(COMMON_IDENTIFIER, PARSE_TOGGLE_CARDS_LIST);
        sendBroadcast(payload);
    }

    public void updateUsersScore(final List<User> users) {
        JsonObject payload = getJson(currentLoggedInUser);
        payload.add(Constants.USER_TAG_SCORE, new Gson().toJsonTree(users));
        payload.addProperty(COMMON_IDENTIFIER, PARSE_SCORE_UPDATED);
        sendBroadcast(payload);
    }
}