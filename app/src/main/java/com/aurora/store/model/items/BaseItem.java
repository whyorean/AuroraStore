package com.aurora.store.model.items;

import android.view.View;

import com.aurora.store.R;
import com.aurora.store.model.App;
import com.mikepenz.fastadapter.items.AbstractItem;

import org.jetbrains.annotations.NotNull;

import lombok.Getter;
import lombok.Setter;

public abstract class BaseItem extends AbstractItem<BaseViewHolder> {

    @Getter
    @Setter
    private App app;
    @Getter
    @Setter
    private String packageName;

    public BaseItem(App app) {
        this.app = app;
        this.packageName = app.getPackageName();
    }

    @Override
    public abstract int getLayoutRes();

    @NotNull
    @Override
    public abstract BaseViewHolder getViewHolder(@NotNull View view);

    @Override
    public int getType() {
        return R.id.fastadapter_item;
    }
}
