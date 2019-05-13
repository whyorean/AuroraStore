package com.aurora.store.api;

import com.dragons.aurora.playstoreapiv2.DocV2;
import com.dragons.aurora.playstoreapiv2.GooglePlayAPI;
import com.dragons.aurora.playstoreapiv2.SearchIterator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchIterator2 extends AppListIterator2 {

    static private final String DOCID_FRAGMENT_MORE_RESULTS = "more_results";

    private DocV2 mainResult;
    private String query;
    private List<String> relatedTags = new ArrayList<>();

    public SearchIterator2(GooglePlayAPI googlePlayApi, String query) {
        super(googlePlayApi);
        this.query = query;
        String url = GooglePlayAPI.SEARCH_URL;
        Map<String, String> params = new HashMap<String, String>();
        params.put("c", "3");
        params.put("q", query);
        firstPageUrl = googlePlayApi.getClient().buildUrl(url, params);
    }

    public List<String> getRelatedTags() {
        Set<String> set = new HashSet<>(relatedTags);
        relatedTags.clear();
        relatedTags.addAll(set);
        return relatedTags;
    }

    public String getQuery() {
        return query;
    }

    @Override
    public List<DocV2> next() {
        List<DocV2> next = new ArrayList<>(super.next());
        if (null != mainResult) {
            if (next.size() > 0 && !next.get(0).getDetails().getAppDetails().getPackageName().equals(mainResult.getDetails().getAppDetails().getPackageName())) {
                next.add(0, mainResult);
            }
            mainResult = null;
        }
        return next;
    }

    @Override
    protected DocV2 getRootDoc(DocV2 doc) {
        DocV2.Builder builder = null;
        DocV2 mainResult = null;
        for (DocV2 child : doc.getChildList()) {

            if (child.getDocType() == 53) {
                relatedTags.add(child.getTitle());
            }

            if (!isRootDoc(child)) {
                continue;
            }
            if (child.getChildCount() == 1) {
                mainResult = child.getChild(0);
            }
            if (child.getDocid().contains(DOCID_FRAGMENT_MORE_RESULTS)) {
                builder = child.toBuilder();
            }
        }
        if (null != mainResult && null != builder) {
            this.mainResult = mainResult;
            return builder.addChild(0, mainResult).build();
        }
        return super.getRootDoc(doc);
    }

    @Override
    protected boolean isRootDoc(DocV2 doc) {
        return super.isRootDoc(doc);
    }
}

