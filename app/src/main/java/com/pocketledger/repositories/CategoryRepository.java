package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.CategoryDao;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.models.Category;
import com.pocketledger.models.Session;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.SessionManager;

import java.util.List;

public class CategoryRepository {

    private final CategoryDao categoryDao;
    private final SettingsDao settingsDao;
    private final SessionManager sessionManager;

    public CategoryRepository(Context context) {
        this.categoryDao = new CategoryDao(context);
        this.settingsDao = new SettingsDao(context);
        this.sessionManager = SessionManager.get(context);
    }

    public List<Category> list(String userId, String type) {
        return categoryDao.list(userId, type);
    }

    public Category find(String userId, String id) {
        return categoryDao.findById(userId, id);
    }

    public Category findByName(String userId, String name) {
        return categoryDao.findByName(userId, name);
    }

    public Category create(String userId, String name, String type, String icon, String color, boolean isFood) {
        Category category = new Category();
        category.id = Ids.newId();
        category.userId = userId;
        category.name = name;
        category.type = type;
        category.icon = icon;
        category.color = color;
        category.isDefault = false;
        category.isFood = isFood;
        category.createdAt = DateUtils.nowIso();
        category.updatedAt = category.createdAt;
        categoryDao.upsert(category);
        enqueue(userId, category.id);
        return category;
    }

    public void update(Category category) {
        category.updatedAt = DateUtils.nowIso();
        categoryDao.upsert(category);
        enqueue(category.userId, category.id);
    }

    public void delete(String userId, String id) {
        categoryDao.softDelete(userId, id, DateUtils.nowIso());
        enqueue(userId, id);
    }

    public boolean nameExists(String userId, String name, String excludeId) {
        return categoryDao.nameExists(userId, name, excludeId);
    }

    private void enqueue(String userId, String recordId) {
        Session session = sessionManager.getSession();
        if (session != null && !session.localOnly) {
            settingsDao.enqueue(userId, Constants.TABLE_CATEGORIES, recordId);
        }
    }
}
