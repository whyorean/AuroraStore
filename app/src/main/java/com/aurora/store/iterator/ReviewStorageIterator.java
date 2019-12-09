/*
 * Aurora Store
 * Copyright (C) 2019, Rahul Kumar Patel <whyorean@gmail.com>
 *
 * Yalp Store
 * Copyright (C) 2018 Sergey Yeriomin <yeriomin@gmail.com>
 *
 * Aurora Store is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Aurora Store is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Aurora Store.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */

package com.aurora.store.iterator;


import com.aurora.store.model.Review;

import java.util.ArrayList;
import java.util.List;

public class ReviewStorageIterator extends ReviewIterator {

    static private final int PAGE_SIZE = 15;

    private List<Review> reviewList = new ArrayList<>();
    private ReviewRetrieverIterator iterator;

    private ReviewRetrieverIterator getRetrievingIterator() {
        if (null == iterator) {
            iterator = new ReviewRetrieverIterator();
            iterator.setPackageName(packageName);
        }
        return iterator;
    }

    public void setRetrievingIterator(ReviewRetrieverIterator iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return reviewList.size() > (PAGE_SIZE * page) || getRetrievingIterator().hasNext();
    }

    @Override
    public List<Review> next() {
        page++;
        if (reviewList.size() < (PAGE_SIZE * (page + 1)) && getRetrievingIterator().hasNext()) {
            reviewList.addAll(getRetrievingIterator().next());
        }
        return current();
    }

    public boolean hasPrevious() {
        return page > 0;
    }

    public List<Review> previous() {
        page--;
        return current();
    }

    private List<Review> current() {
        int from = PAGE_SIZE * page;
        int to = from + PAGE_SIZE;
        return (from < 0 || to > reviewList.size()) ? new ArrayList<Review>() : reviewList.subList(from, to);
    }
}
