package org.bootcamp.fiftytwo.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import org.bootcamp.fiftytwo.R;
import org.bootcamp.fiftytwo.adapters.SelectCardsAdapter;
import org.bootcamp.fiftytwo.models.Card;
import org.bootcamp.fiftytwo.utils.CardUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * A placeholder fragment containing a simple view.
 */
public class SelectCardsFragment extends Fragment {

    public SelectCardsAdapter mAdapter;
    private Unbinder unbinder;

    @BindView(R.id.rvCards) RecyclerView rvCards;
    @BindView(R.id.btnLoadMore)
    Button btnLoadMore;
    private List<Card> cards;

    public SelectCardsFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new SelectCardsAdapter(getContext(), new ArrayList<>());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_select_cards, container, false);
        unbinder = ButterKnife.bind(this, view);

        cards = CardUtil.selectDefaults(CardUtil.generateDeck(1, true));

        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 6);
        rvCards.setAdapter(mAdapter);
        rvCards.setLayoutManager(layoutManager);
        mAdapter.addAll(cards);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }

    @OnClick(R.id.btnLoadMore)
    public void loadMore(View view){
        mAdapter.addAll(CardUtil.selectDefaults(CardUtil.generateDeck(1, true)));
    }

    public List<Card> getCards() {
        return mAdapter.getCards();
    }
}