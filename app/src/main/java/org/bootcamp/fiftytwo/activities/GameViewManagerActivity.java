package org.bootcamp.fiftytwo.activities;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.bootcamp.fiftytwo.R;
import org.bootcamp.fiftytwo.application.FiftyTwoApplication;
import org.bootcamp.fiftytwo.fragments.CardsFragment;
import org.bootcamp.fiftytwo.fragments.ChatAndLogFragment;
import org.bootcamp.fiftytwo.fragments.DealerViewFragment;
import org.bootcamp.fiftytwo.fragments.DealingOptionsFragment;
import org.bootcamp.fiftytwo.fragments.PlayerFragment;
import org.bootcamp.fiftytwo.fragments.PlayerViewFragment;
import org.bootcamp.fiftytwo.fragments.ScoringFragment;
import org.bootcamp.fiftytwo.interfaces.Observable;
import org.bootcamp.fiftytwo.interfaces.Observer;
import org.bootcamp.fiftytwo.models.Card;
import org.bootcamp.fiftytwo.models.ChatLog;
import org.bootcamp.fiftytwo.models.Game;
import org.bootcamp.fiftytwo.models.User;
import org.bootcamp.fiftytwo.network.ParseUtils;
import org.bootcamp.fiftytwo.utils.Constants;
import org.bootcamp.fiftytwo.utils.PlayerUtils;
import org.bootcamp.fiftytwo.views.PlayerViewHelper;
import org.json.JSONException;
import org.json.JSONObject;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindDrawable;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.bootcamp.fiftytwo.models.User.fromJson;
import static org.bootcamp.fiftytwo.utils.AppUtils.getList;
import static org.bootcamp.fiftytwo.utils.AppUtils.isEmpty;
import static org.bootcamp.fiftytwo.utils.Constants.FRAGMENT_CHAT_TAG;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_CARDS;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_CURRENT_VIEW_PLAYER;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_GAME_NAME;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DEAL_CARDS;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_DEAL_CARDS_TO_TABLE;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_NEW_PLAYER_ADDED;
import static org.bootcamp.fiftytwo.utils.Constants.PARSE_PLAYER_LEFT;
import static org.bootcamp.fiftytwo.utils.Constants.PLAYER_TAG;
import static org.bootcamp.fiftytwo.utils.Constants.TABLE_TAG;
import static org.bootcamp.fiftytwo.views.PlayerViewHelper.getPlayerFragmentTag;

public class GameViewManagerActivity extends AppCompatActivity implements
        PlayerViewFragment.onPlayListener,
        DealerViewFragment.OnDealListener,
        DealingOptionsFragment.OnDealOptionsListener,
        ChatAndLogFragment.OnChatAndLogListener,
        CardsFragment.OnLogEventListener,
        ScoringFragment.OnScoreFragmentInteractionListener,
        Observer {

    private List<User> mPlayers = new ArrayList<>();
    private List<Card> mCards;
    private String gameName;
    private ParseUtils parseUtils;

    private boolean isCurrentViewPlayer = true;
    private boolean isShowingPlayerFragment = true; //false is showing dealer fragment
    private boolean isShowingChat = false;

    private PlayerViewFragment playerViewFragment;
    private DealerViewFragment dealerViewFragment;
    private ChatAndLogFragment chatAndLogFragment;

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.fab) FloatingActionButton fab;
    @BindView(R.id.ibComment) ImageButton ibComment;
    @BindView(R.id.ibSettings) ImageButton ibSettings;
    @BindDrawable(R.drawable.ic_comment) Drawable ic_comment;
    @BindDrawable(R.drawable.ic_cancel) Drawable ic_cancel;

    private static final String TAG = GameViewManagerActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_view_manager);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);

        initGameParams();
        initFragments();

        ((FiftyTwoApplication) getApplication()).addObserver(this);
    }

    private void initGameParams() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            gameName = bundle.getString(PARAM_GAME_NAME);
            mCards = Parcels.unwrap(bundle.getParcelable(PARAM_CARDS));
            isCurrentViewPlayer = bundle.getBoolean(PARAM_CURRENT_VIEW_PLAYER);
            Toast.makeText(getApplicationContext(), "Joining Game: " + gameName, Toast.LENGTH_SHORT).show();

            //Join channel for updates
            parseUtils = new ParseUtils(this, gameName);
            parseUtils.saveCurrentUser(isCurrentViewPlayer);

            //Get previously joined players
            parseUtils.fetchPreviouslyJoinedUsers(gameName, this);
            parseUtils.joinChannel();

            // Add myself to game
            Game.save(gameName, User.getCurrentUser(this));
        }
    }

    private void initFragments() {
        playerViewFragment = PlayerViewFragment.newInstance(null, null);
        dealerViewFragment = DealerViewFragment.newInstance(mCards, null);
        chatAndLogFragment = ChatAndLogFragment.newInstance(1);

        if (isCurrentViewPlayer) {
            fab.setVisibility(View.GONE);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.flGameContainer, playerViewFragment)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.flGameContainer, playerViewFragment)
                    .add(R.id.flGameContainer, dealerViewFragment)
                    .hide(playerViewFragment)
                    .commit();
        }
        isShowingPlayerFragment = isCurrentViewPlayer;

        // TODO: This custom data generation is temporary and for testing purposes only
        if (isEmpty(mPlayers)) {
            mPlayers = PlayerUtils.getPlayers(4);
            if (dealerViewFragment != null) dealerViewFragment.addPlayers(mPlayers);
        }

        final View rootView = getWindow().getDecorView().getRootView();
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                PlayerViewHelper.addPlayers(GameViewManagerActivity.this, R.id.clGameLayout, mPlayers);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    @OnClick(R.id.fab)
    public void switchView() {
        getSupportFragmentManager()
                .beginTransaction()
                .hide(isShowingPlayerFragment ? playerViewFragment : dealerViewFragment)
                .show(isShowingPlayerFragment ? dealerViewFragment : playerViewFragment)
                .commit();
        this.isShowingPlayerFragment ^= true;
    }

    // TODO: may be use Dialog fragment and reuse that with other fragment when user leave??
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Exiting from game")
                .setMessage("Are you sure you want to exit from game?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    parseUtils.changeGameParticipation(false);
                    parseUtils.removeChannel();
                    parseUtils.deleteUserFromDb(gameName, User.getCurrentUser(this));
                    ((FiftyTwoApplication) getApplication()).removeAllObservers();
                    finish();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @OnClick(R.id.ibComment)
    public void toggleChatAndLogView(View v) {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!isShowingChat) {
            fragmentTransaction.replace(R.id.flLogContainer, chatAndLogFragment, FRAGMENT_CHAT_TAG);
            ibComment.setImageDrawable(ic_cancel);
            isShowingChat = true;
        } else {
            fragmentTransaction.remove(chatAndLogFragment);
            ibComment.setImageDrawable(ic_comment);
            isShowingChat = false;
        }
        fragmentTransaction.commit();
    }

    @Override
    public void onDealerOptionsShowing(boolean isDealerOptionShowing) {
        //Show and hide all players when dealer option is showing to un-clutter the view
        for (User player : mPlayers) {
            String playerTag = getPlayerFragmentTag(player);
            Fragment playerFragmentByTag = getSupportFragmentManager().findFragmentByTag(playerTag);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (isDealerOptionShowing) {
                transaction.hide(playerFragmentByTag);
            } else {
                transaction.show(playerFragmentByTag);
            }
            transaction.commit();
        }
    }

    @Override
    public boolean onDeal(List<Card> cards, User player) {
        Fragment playerFragment = getSupportFragmentManager().findFragmentByTag(getPlayerFragmentTag(player));
        if (playerFragment != null && !isEmpty(cards)) {
            boolean result = ((PlayerFragment) playerFragment).stackCards(cards);
            if (result) {
                parseUtils.dealCards(player, cards);
            }
            return result;
        }
        return false;
    }

    @Override
    public boolean onDealTable(List<Card> cards, boolean toSink) {
        if (!isEmpty(cards)) {
            if (!toSink) {
                parseUtils.dealCardsToTable(cards);
                return true;
            } else {
                // TODO: Handle Drop to Sink here
                return true;
            }
        }
        return false;
    }

    @Override
    public void onNewLogEvent(String whoPosted, String details) {
        Log.d(Constants.TAG, GameViewManagerActivity.class.getSimpleName() + "--" + details + "--" + whoPosted);
        chatAndLogFragment.addNewLogEvent(whoPosted, details);
    }

    @Override
    public synchronized void onUpdate(final Observable o, final Object identifier, final Object arg) {
        String event = identifier.toString();
        Log.d(TAG, "onUpdate: " + event);

        switch (event) {
            case PARSE_NEW_PLAYER_ADDED:
                runOnUiThread(() -> {
                    User user = User.fromJson((JSONObject) arg);
                    addPlayersToView(getList(user));
                });
                break;
            case PARSE_PLAYER_LEFT:
                runOnUiThread(() -> {
                    User user = User.fromJson((JSONObject) arg);
                    removePlayersFromView(getList(user));
                });
                break;
            case PARSE_DEAL_CARDS:
                try {
                    JSONObject details = (JSONObject) arg;
                    User from = fromJson(details);
                    JSONObject toUserDetails = details.getJSONObject(PLAYER_TAG);
                    User to = fromJson(toUserDetails);
                    List<Card> cards = new Gson().fromJson(details.getString(PARAM_CARDS), new TypeToken<List<Card>>() {}.getType());
                    handleDeal(cards, from, to);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
            case PARSE_DEAL_CARDS_TO_TABLE:
                try {
                    JSONObject details = (JSONObject) arg;
                    User from = fromJson(details);
                    List<Card> cards = new Gson().fromJson(details.getString(PARAM_CARDS), new TypeToken<List<Card>>() {}.getType());
                    handleDealTable(from, cards);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public void addPlayersToView(List<User> players) {
        mPlayers.addAll(players);
        if (dealerViewFragment != null) {
            dealerViewFragment.addPlayers(players);
        }
        PlayerViewHelper.addPlayers(this, R.id.clGameLayout, players);
        for (User player : players) {
            onNewLogEvent(player.getDisplayName(), player.getDisplayName() + " joined.");
        }
    }

    public void removePlayersFromView(List<User> players) {
        for (User player : players) {
            mPlayers.remove(player);
            if (dealerViewFragment != null) {
                dealerViewFragment.removePlayer(player);
            }
            String userViewTag = getPlayerFragmentTag(player);
            Fragment userFragment = getSupportFragmentManager().findFragmentByTag(userViewTag);
            if (userFragment != null) {
                getSupportFragmentManager().beginTransaction().remove(userFragment).commitNow();
            } else {
                Log.e(TAG, "NULL Failed to remove view for " + userViewTag);
            }
            onNewLogEvent(player.getDisplayName(), player.getDisplayName() + " left.");
        }
    }

    public void handleDeal(List<Card> cards, User from, User to) {
        if (!isEmpty(cards) && from != null && from.isDealer() && to != null) {
            if (isCurrentViewPlayer) {
                Fragment playerFragment = getSupportFragmentManager().findFragmentByTag(getPlayerFragmentTag(to));
                if (playerFragment != null) {
                    ((PlayerFragment) playerFragment).stackCards(cards);
                }
            }
            if (parseUtils.getCurrentUser().equals(to) && playerViewFragment != null) {
                Fragment fragment = playerViewFragment.getChildFragmentManager().findFragmentByTag(PLAYER_TAG);
                if (fragment != null) {
                    ((CardsFragment) fragment).stackCards(cards);
                }
            }
        }
    }

    public void handleDealTable(User from, List<Card> cards) {
        if (!isEmpty(cards) && from != null && from.isDealer()) {
            Fragment fragment = playerViewFragment.getChildFragmentManager().findFragmentByTag(TABLE_TAG);
            if (fragment != null) {
                ((CardsFragment) fragment).stackCards(cards);
            }
        }
    }

    @Override
    public void onPlay() {
        // Do Nothing
    }

    @Override
    public void onDealOptionSelected(Bundle bundle) {
        // Do Nothing
    }

    @Override
    public void onChat(ChatLog item) {
        // Do Nothing
    }

    @Override
    public void onScoreFragmentInteraction(boolean saveClicked) {
        //Do Nothing
    }
}