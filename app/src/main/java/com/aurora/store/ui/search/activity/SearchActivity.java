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
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.OneTimeWorkRequest;

import com.aurora.store.Constants;
import com.aurora.store.R;
import com.aurora.store.model.items.SearchSuggestionItem;
import com.aurora.store.ui.details.DetailsActivity;
import com.aurora.store.ui.search.SearchSuggestionModel;
import com.aurora.store.ui.single.activity.BaseActivity;
import com.aurora.store.util.ContextUtil;
import com.aurora.store.util.Util;
import com.aurora.store.util.ViewUtil;
import com.aurora.store.util.WorkerUtil;
import com.aurora.store.util.diff.SuggestionDiffCallback;
import com.aurora.store.worker.ApiValidator;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.mikepenz.fastadapter.FastAdapter;
import com.mikepenz.fastadapter.adapters.ItemAdapter;
import com.mikepenz.fastadapter.diff.FastAdapterDiffUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchActivity extends BaseActivity {

    @BindView(R.id.search_view)
    TextInputEditText searchView;
    @BindView(R.id.recycler)
    RecyclerView recyclerView;
    @BindView(R.id.action2)
    ImageView action2;
    @BindView(R.id.coordinator)
    CoordinatorLayout coordinator;
    @BindView(R.id.fab_ime)
    ExtendedFloatingActionButton fabIme;

    private String query;
    private boolean imeVisible = false;
    private SearchSuggestionModel model;
    private InputMethodManager inputMethodManager;

    private FastAdapter<SearchSuggestionItem> fastAdapter;
    private ItemAdapter<SearchSuggestionItem> itemAdapter;

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
        model.getSuggestions().observe(this, this::dispatchAppsToAdapter);

        model.getError().observe(this, errorType -> {
            switch (errorType) {
                case NO_API:
                case SESSION_EXPIRED: {
                    buildAndTestApi();
                    break;
                }
                case NO_NETWORK: {
                    showSnackBar(coordinator, R.string.error_no_network, v -> {
                        model.fetchSuggestions(query);
                    });
                    break;
                }
            }
        });/*

        coordinator.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (coordinator != null) {
                int heightDiff = coordinator.getRootView().getHeight() - coordinator.getHeight();
                if (heightDiff > ViewUtil.dpToPx(this, 100 *//*Dirty, but works*//*)) {
                    fabIme.hide();
                } else {
                    fabIme.show();
                }
            }
        });*/

        onNewIntent(getIntent());
    }

    @OnClick(R.id.action1)
    public void goBack() {
        onBackPressed();
    }

    @OnClick(R.id.fab_ime)
    public void toggleKeyBoard() {
        if (inputMethodManager != null) {
            if (imeVisible) {
                inputMethodManager.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT);
            } else {
                inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(),
                        InputMethodManager.HIDE_IMPLICIT_ONLY);
            }
            imeVisible = !imeVisible;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        searchView.requestFocus();

        if (!StringUtils.isEmpty(searchView.getText())) {
            toggleKeyBoard();
        }
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

    private void buildAndTestApi() {
        final OneTimeWorkRequest workRequest = WorkerUtil.getWorkRequest(ApiValidator.TAG,
                WorkerUtil.getNetworkConstraints(),
                ApiValidator.class);

        WorkerUtil.enqueue(this, this, workRequest, workInfo -> {
            switch (workInfo.getState()) {
                case FAILED:
                    showSnackBar(coordinator, R.string.toast_api_build_failed, null);
                    break;

                case SUCCEEDED:
                    model.getSuggestions();
                    break;
            }
        });
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

    private void dispatchAppsToAdapter(List<SearchSuggestionItem> searchSuggestionItems) {
        final FastAdapterDiffUtil fastAdapterDiffUtil = FastAdapterDiffUtil.INSTANCE;
        final SuggestionDiffCallback suggestionDiffCallback = new SuggestionDiffCallback();
        final DiffUtil.DiffResult diffResult = fastAdapterDiffUtil.calculateDiff(itemAdapter, searchSuggestionItems, suggestionDiffCallback);
        fastAdapterDiffUtil.set(itemAdapter, diffResult);
    }

    private void setupSuggestionRecycler() {
        fastAdapter = new FastAdapter<>();
        itemAdapter = new ItemAdapter<>();

        fastAdapter.setOnClickListener((view, searchSuggestionItemIAdapter, searchSuggestionItem, position) -> {
            final String title = searchSuggestionItem.getSuggestEntry().getTitle();
            final String packageName = searchSuggestionItem.getSuggestEntry().getPackageNameContainer().getPackageName();
            final String query = packageName.isEmpty() ? title : packageName;

            if (Util.isSearchByPackageEnabled(this) && isPackageName(query)) {
                openDetailsActivity(query);
            } else {
                openSearchResultActivity(query);
            }
            return false;
        });

        fastAdapter.addAdapter(0, itemAdapter);

        recyclerView.setAdapter(fastAdapter);
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
}
