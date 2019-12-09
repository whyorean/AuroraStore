package com.aurora.store.model;

import lombok.Data;

@Data
public class CategoryModel {
    String categoryId;
    String categoryTitle;
    String categoryImageUrl;

    public CategoryModel(String categoryId, String categoryTitle, String categoryImageUrl) {
        this.categoryId = categoryId;
        this.categoryTitle = categoryTitle;
        this.categoryImageUrl = categoryImageUrl;
    }
}
