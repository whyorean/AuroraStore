package com.aurora.store.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.core.graphics.ColorUtils;

import com.aurora.store.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsLinkView extends RelativeLayout {

    @BindView(R.id.link_container)
    RelativeLayout layoutLink;
    @BindView(R.id.img_link)
    ImageView imgLink;
    @BindView(R.id.txt_link_title)
    TextView txtLinkTitle;

    private String linkText;
    private int linkImageId;
    private @ColorRes
    int color = R.color.colorAccent;
    private OnClickListener onClickListener;

    public DetailsLinkView(Context context) {
        super(context);
        init(context);
    }

    public DetailsLinkView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DetailsLinkView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public DetailsLinkView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public int getColor() {
        return getContext().getResources().getColor(color);
    }

    public void setColor(int color) {
        this.color = color;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public int getLinkImageId() {
        return linkImageId;
    }

    public void setLinkImageId(int linkImageId) {
        this.linkImageId = linkImageId;
    }

    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    @Override
    public void setOnClickListener(OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
    }

    private void init(Context context) {
        View view = inflate(context, R.layout.view_links, this);
        ButterKnife.bind(this, view);
    }

    public void build() {
        txtLinkTitle.setText(getLinkText());
        imgLink.setImageDrawable(getContext().getDrawable(getLinkImageId()));
        layoutLink.setOnClickListener(getOnClickListener());
        imgLink.setColorFilter(getColor());
        layoutLink.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(getColor(), 60)));
    }
}
