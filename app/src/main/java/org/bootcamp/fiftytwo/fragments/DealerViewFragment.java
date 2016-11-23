package org.bootcamp.fiftytwo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.jcmore2.shakeit.ShakeIt;
import com.jcmore2.shakeit.ShakeListener;

import org.bootcamp.fiftytwo.R;
import org.bootcamp.fiftytwo.models.Card;
import org.bootcamp.fiftytwo.models.User;
import org.bootcamp.fiftytwo.utils.CardUtil;
import org.bootcamp.fiftytwo.utils.PlayerUtils;
import org.bootcamp.fiftytwo.views.PlayerViewHelper;
import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.bootcamp.fiftytwo.utils.AppUtils.getParcelable;
import static org.bootcamp.fiftytwo.utils.AppUtils.isEmpty;
import static org.bootcamp.fiftytwo.utils.Constants.DEALER_TAG;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_CARDS;
import static org.bootcamp.fiftytwo.utils.Constants.PARAM_PLAYERS;
import static org.bootcamp.fiftytwo.utils.Constants.TAG;
import static org.bootcamp.fiftytwo.views.PlayerViewHelper.getPlayerFragmentTag;

public class DealerViewFragment extends Fragment {

    private List<Card> mCards = new ArrayList<>();
    private List<User> mPlayers = new ArrayList<>();
    private OnDealerListener mDealerListener;
    private Unbinder unbinder;

    @BindView(R.id.flDealerViewContainer) FrameLayout flDealerViewContainer;

    public interface OnDealerListener {
        void onDeal();
    }

    public static DealerViewFragment newInstance(List<Card> cards, List<User> players) {
        DealerViewFragment fragment = new DealerViewFragment();
        Bundle args = new Bundle();
        if (!isEmpty(cards))
            args.putParcelable(PARAM_CARDS, getParcelable(cards));
        if (!isEmpty(players))
            args.putParcelable(PARAM_PLAYERS, getParcelable(players));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null) {
            List<Card> cards = Parcels.unwrap(bundle.getParcelable(PARAM_CARDS));
            List<User> players = Parcels.unwrap(bundle.getParcelable(PARAM_PLAYERS));
            mCards = isEmpty(cards) ? CardUtil.generateDeck(1, false) : cards;
            mPlayers = isEmpty(players) ? PlayerUtils.getPlayers(4) : players;
        }

        // TODO: Remove this and library from gradle and service from manifest if we don't need shake
        // OR else implement what to do when it's shaken
        // Optimize it to save battery..use it to detect shake and then stop it
        ShakeIt.initializeShakeService(getActivity(), new ShakeListener() {
            @Override
            public void onShake(float force) {
                Log.d(TAG, "shaking phone");
            }

            @Override
            public void onAccelerationChanged(float x, float y, float z) {}
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dealer_view, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onStop() {
        super.onStop();
        ShakeIt.stopShakeService(getActivity());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        Fragment dealerCardsFragment = CardsFragment.newInstance(mCards, DEALER_TAG);

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.flDealerContainer, dealerCardsFragment, DEALER_TAG)
                .commit();

        PlayerViewHelper.addPlayers(this, R.id.flDealerViewContainer, mPlayers);
    }

    @OnClick(R.id.ibDeal)
    public void deal(View view) {
        int dealCount = 1;
        CardsFragment dealerFragment = (CardsFragment) getChildFragmentManager().findFragmentByTag(DEALER_TAG);
        List<Card> cards = dealerFragment.getCards();
        if (cards.size() >= mPlayers.size() * dealCount) {
            for (User player : mPlayers) {
                Fragment playerFragment = getChildFragmentManager().findFragmentByTag(getPlayerFragmentTag(player));
                if (playerFragment != null) {
                    List<Card> drawnCards = dealerFragment.drawCards(dealCount, false);
                    if (!isEmpty(drawnCards)) {
                        ((PlayerFragment) playerFragment).stackCards(drawnCards);
                    }
                }
            }
        } else {
            Snackbar.make(view, "Not enough cards to deal", Snackbar.LENGTH_SHORT).show();
        }

        if (mDealerListener != null) {
            mDealerListener.onDeal();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnDealerListener) {
            mDealerListener = (OnDealerListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mDealerListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }
}