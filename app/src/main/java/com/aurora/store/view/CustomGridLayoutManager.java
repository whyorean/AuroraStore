package com.aurora.store.view;

import android.content.Context;
import android.util.TypedValue;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CustomGridLayoutManager extends GridLayoutManager {
    private static final int sColumnWidth = 200;
    private int mColumnWidth;
    private boolean mColumnWidthChanged = true;
    private boolean mWidthChanged = true;
    private int mWidth;

    public CustomGridLayoutManager(Context context, int columnWidth) {
        super(context, 1);
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    public CustomGridLayoutManager(Context context, int columnWidth, int orientation, boolean reverseLayout) {
        super(context, 1, orientation, reverseLayout);
        setColumnWidth(checkedColumnWidth(context, columnWidth));
    }

    private int checkedColumnWidth(Context context, int columnWidth) {
        if (columnWidth <= 0) {
            columnWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, sColumnWidth,
                    context.getResources().getDisplayMetrics());
        } else {
            columnWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, columnWidth,
                    context.getResources().getDisplayMetrics());
        }
        return columnWidth;
    }

    private void setColumnWidth(int newColumnWidth) {
        if (newColumnWidth > 0 && newColumnWidth != mColumnWidth) {
            mColumnWidth = newColumnWidth;
            mColumnWidthChanged = true;
        }
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        int width = getWidth();
        int height = getHeight();

        if (width != mWidth) {
            mWidthChanged = true;
            mWidth = width;
        }

        if (mColumnWidthChanged && mColumnWidth > 0 && width > 0 && height > 0
                || mWidthChanged) {
            int totalSpace;
            if (getOrientation() == RecyclerView.VERTICAL) {
                totalSpace = width - getPaddingRight() - getPaddingLeft();
            } else {
                totalSpace = height - getPaddingTop() - getPaddingBottom();
            }
            int spanCount = Math.max(1, totalSpace / mColumnWidth);
            setSpanCount(spanCount);
            mColumnWidthChanged = false;
            mWidthChanged = false;
        }
        super.onLayoutChildren(recycler, state);
    }
}
