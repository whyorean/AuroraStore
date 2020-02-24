package com.aurora.store.ui.search.activity;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.section.SearchSuggestionSection;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.search.SearchSuggestionModel;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.dragons.aurora.playstoreapiv2.SearchSuggestEntry;
import com.google.android.material.textfield.TextInputEditText;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter;

public class SearchActivity extends BaseActivity implements SearchSuggestionSection.ClickListener {

    @BindView(R.id.search_view)
    TextInputEditText searchView;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.action2)
    ImageView action2;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;

    private String query;
    private SearchSuggestionModel model;
    private SearchSuggestionSection section;
    private SectionedRecyclerViewAdapter adapter;
    private InputMethodManager inputMethodManager;

    private static boolean isPackageName(String query) {
        if (TextUtils.isEmpty(query)) {
            return false;
        }
        String pattern = "([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)+[\\p{L}_$][\\p{L}\\p{N}_$]*";
        Pattern r = Pattern.compile(pattern);
        return r.matcher(query).matches();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        ButterKnife.bind(this);
        setupSearch();
        setupSuggestionRecycler();

        Object object = getSystemService(Service.INPUT_METHOD_SERVICE);
        inputMethodManager = (InputMethodManager) object;

        model = new ViewModelProvider(this).get(SearchSuggestionModel.class);
        model.getSuggestions().observe(this, searchSuggestEntries -> {
            dispatchAppsToAdapter(searchSuggestEntries);
        });

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED: {
                    Util.validateApi(this);
                    break;
                }
                case NO_NETWORK: {
                    showSnackBar(coordinator, R.string.error_no_network, v -> {
                        model.fetchSuggestions(query);
                    });
                    break;
                }
            }
        });
        onNewIntent(getIntent());
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }

    @OnClick(R.id.fab_ime)
    public void toggleKeyBoard() {
        if (inputMethodManager != null)
            inputMethodManager.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.requestFocus();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getScheme() != null && intent.getScheme().equals("market")) {
            if (intent.getData() != null) {
                query = intent.getData().getQueryParameter("q");
                searchView.setText(query);
            } else {
                Toast.makeText(this, "Empty query received", Toast.LENGTH_SHORT).show();
                finishAfterTransition();
            }
        }
    }

    private void setupSearch() {
        action2.setImageDrawable(getDrawable(R.drawable.ic_cancel));
        action2.setOnClickListener(v -> {
            searchView.setText("");
        });
        searchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!StringUtils.isEmpty(s)) {
                    query = s.toString();
                    ContextUtil.runOnUiThread(() -> model.fetchSuggestions(query), 500);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        searchView.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                query = searchView.getText().toString();
                if (!query.isEmpty()) {
                    openSearchResultActivity(query);
                    return true;
                }
            }
            return false;
        });
    }

    private void dispatchAppsToAdapter(List<SearchSuggestEntry> suggestEntryList) {
        List<SearchSuggestEntry> oldList = section.getList();

        if (!oldList.isEmpty()) {
            section.getList().clear();
            adapter.notifyDataSetChanged();
        }

        if (!suggestEntryList.isEmpty()) {
            section.addData(suggestEntryList);
            adapter.notifyDataSetChanged();
        }
    }

    private void setupSuggestionRecycler() {
        section = new SearchSuggestionSection(this, this);
        adapter = new SectionedRecyclerViewAdapter();
        adapter.addSection(section);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
    }

    private void openDetailsActivity(String packageName) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra(Constants.INTENT_PACKAGE_NAME, packageName);
        startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
    }

    private void openSearchResultActivity(String query) {
        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra("QUERY", query);
        startActivity(intent, ViewUtil.getEmptyActivityBundle(this));
    }

    @Override
    public void onClick(String query) {
        if (Util.isSearchByPackageEnabled(this) && isPackageName(query)) {
            openDetailsActivity(query);
        } else {
            openSearchResultActivity(query);
        }
    }
}
