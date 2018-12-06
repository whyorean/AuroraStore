package com.dragons.aurora.model;

public class FavouriteItem {

    private boolean isSelected;
    private App app;

    public FavouriteItem(App app) {
        this.app = app;
    }

    public App getApp() {
        return app;
    }

    public boolean getSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

}
