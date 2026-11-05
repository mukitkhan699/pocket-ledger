package com.pocketledger.repositories;

import android.content.Context;

import com.pocketledger.database.CategoryDao;
import com.pocketledger.database.MealDao;
import com.pocketledger.database.SettingsDao;
import com.pocketledger.database.TransactionDao;
import com.pocketledger.models.Category;
import com.pocketledger.models.MealEntry;
import com.pocketledger.models.MealSettings;
import com.pocketledger.models.Session;
import com.pocketledger.models.Transaction;
import com.pocketledger.utils.Constants;
import com.pocketledger.utils.DateUtils;
import com.pocketledger.utils.Ids;
import com.pocketledger.utils.SessionManager;

import java.time.LocalDate;
import java.util.List;

public class MealRepository {

    private final MealDao mealDao;
    private final SettingsDao settingsDao;
    private final CategoryDao categoryDao;
    private final TransactionDao transactionDao;
    private final SessionManager sessionManager;

    public MealRepository(Context context) {
        this.mealDao = new MealDao(context);
        this.settingsDao = new SettingsDao(context);
        this.categoryDao = new CategoryDao(context);
        this.transactionDao = new TransactionDao(context);
        this.sessionManager = SessionManager.get(context);
    }

    public MealSettings getSettings(String userId) {
        return settingsDao.getMealSettings(userId);
    }

    public MealSettings saveSettings(MealSettings settings) {
        settings.updatedAt = DateUtils.nowIso();
        if (settings.createdAt == null) {
            settings.createdAt = settings.updatedAt;
        }
        if (settings.id == null) {
            settings.id = Ids.deterministic("meal-settings:" + settings.userId);
        }
        settingsDao.saveMealSettings(settings);
        enqueue(settings.userId, Constants.TABLE_MEAL_SETTINGS, settings.id);
        return settings;
    }

    public List<MealEntry> getForDate(String userId, String date) {
        return mealDao.getForDate(userId, date);
    }

    public List<MealEntry> getForRange(String userId, LocalDate start, LocalDate end) {
        return mealDao.getForRange(userId, start.toString(), end.toString());
    }

    public int ensureDailyMeals(String userId, LocalDate date) {
        MealSettings settings = settingsDao.getMealSettings(userId);
        if (!settings.autoGenerate) {
            return 0;
        }
        int inserted = 0;
        String dateValue = date.toString();
        String now = DateUtils.nowIso();
        for (String type : new String[]{Constants.MEAL_MORNING, Constants.MEAL_LUNCH, Constants.MEAL_DINNER}) {
            if (mealDao.existsIncludingDeleted(userId, dateValue, type)) {
                continue;
            }
            MealEntry entry = new MealEntry();
            entry.id = Ids.mealId(userId, dateValue, type);
            entry.userId = userId;
            entry.mealDate = dateValue;
            entry.mealType = type;
            entry.amountMinor = settings.priceFor(type);
            entry.state = Constants.STATE_PLANNED;
            entry.createdAt = now;
            entry.updatedAt = now;
            entry.deleted = false;
            mealDao.upsert(entry);
            enqueue(userId, Constants.TABLE_MEAL_ENTRIES, entry.id);
            inserted++;
        }
        return inserted;
    }

    public MealEntry createOrRestore(String userId, String date, String mealType, long amountMinor, String state) {
        MealEntry existing = mealDao.findAny(userId, date, mealType);
        if (existing != null && !existing.deleted) {
            return null;
        }
        String now = DateUtils.nowIso();
        MealEntry entry;
        if (existing != null) {
            entry = existing;
            entry.deleted = false;
            entry.amountMinor = amountMinor;
            entry.state = state;
            entry.updatedAt = now;
        } else {
            entry = new MealEntry();
            entry.id = Ids.mealId(userId, date, mealType);
            entry.userId = userId;
            entry.mealDate = date;
            entry.mealType = mealType;
            entry.amountMinor = amountMinor;
            entry.state = state;
            entry.createdAt = now;
            entry.updatedAt = now;
            entry.deleted = false;
        }
        mealDao.upsert(entry);
        enqueue(userId, Constants.TABLE_MEAL_ENTRIES, entry.id);
        syncTransaction(entry);
        return entry;
    }

    public void setState(MealEntry entry, String state) {
        entry.state = state;
        entry.updatedAt = DateUtils.nowIso();
        mealDao.upsert(entry);
        enqueue(entry.userId, Constants.TABLE_MEAL_ENTRIES, entry.id);
        syncTransaction(entry);
    }

    public void updateAmount(MealEntry entry, long amountMinor) {
        entry.amountMinor = amountMinor;
        entry.updatedAt = DateUtils.nowIso();
        mealDao.upsert(entry);
        enqueue(entry.userId, Constants.TABLE_MEAL_ENTRIES, entry.id);
        if (entry.isConsumed()) {
            syncTransaction(entry);
        }
    }

    public void delete(MealEntry entry) {
        removeLinkedTransaction(entry);
        mealDao.softDelete(entry.userId, entry.id, DateUtils.nowIso());
        enqueue(entry.userId, Constants.TABLE_MEAL_ENTRIES, entry.id);
    }

    public long consumedForDate(String userId, String date) {
        return mealDao.consumedForDate(userId, date);
    }

    public long plannedForDate(String userId, String date) {
        return mealDao.sumState(userId, date, date, Constants.STATE_PLANNED);
    }

    public long nonSkippedForDate(String userId, String date) {
        return mealDao.sumNonSkipped(userId, date, date);
    }

    public long consumedForRange(String userId, LocalDate start, LocalDate end) {
        return mealDao.sumState(userId, start.toString(), end.toString(), Constants.STATE_CONSUMED);
    }

    public long plannedForRange(String userId, LocalDate start, LocalDate end) {
        return mealDao.sumState(userId, start.toString(), end.toString(), Constants.STATE_PLANNED);
    }

    private void syncTransaction(MealEntry entry) {
        if (entry.isConsumed()) {
            ensureLinkedTransaction(entry);
        } else {
            removeLinkedTransaction(entry);
        }
    }

    private void ensureLinkedTransaction(MealEntry entry) {
        Category category = categoryDao.findByName(entry.userId, categoryName(entry.mealType));
        if (category == null) {
            category = categoryDao.findByName(entry.userId, "Other");
        }
        Transaction existing = transactionDao.findByMealEntryId(entry.userId, entry.id);
        String now = DateUtils.nowIso();
        Transaction transaction;
        if (existing == null) {
            transaction = new Transaction();
            transaction.id = Ids.mealTransactionId(entry.id);
            transaction.userId = entry.userId;
            transaction.createdAt = now;
            transaction.mealEntryId = entry.id;
        } else {
            transaction = existing;
        }
        transaction.type = Constants.TYPE_EXPENSE;
        transaction.categoryId = category == null ? null : category.id;
        transaction.description = entry.label() + " meal";
        transaction.notes = "From hostel meal tracking";
        transaction.amountMinor = entry.amountMinor;
        transaction.occurredAt = occurredAt(entry);
        transaction.updatedAt = now;
        transaction.deleted = false;
        transactionDao.upsert(transaction);
        enqueue(entry.userId, Constants.TABLE_TRANSACTIONS, transaction.id);
    }

    private void removeLinkedTransaction(MealEntry entry) {
        Transaction existing = transactionDao.findByMealEntryId(entry.userId, entry.id);
        if (existing != null) {
            transactionDao.softDelete(entry.userId, existing.id, DateUtils.nowIso());
            enqueue(entry.userId, Constants.TABLE_TRANSACTIONS, existing.id);
        }
    }

    private String occurredAt(MealEntry entry) {
        LocalDate date = LocalDate.parse(entry.mealDate);
        int hour;
        switch (entry.mealType) {
            case Constants.MEAL_MORNING:
                hour = 8;
                break;
            case Constants.MEAL_LUNCH:
                hour = 13;
                break;
            default:
                hour = 20;
                break;
        }
        return DateUtils.atLocalDateTime(date, hour, 0);
    }

    private String categoryName(String mealType) {
        switch (mealType) {
            case Constants.MEAL_MORNING:
                return "Morning Food";
            case Constants.MEAL_LUNCH:
                return "Lunch";
            default:
                return "Dinner";
        }
    }

    private void enqueue(String userId, String table, String recordId) {
        Session session = sessionManager.getSession();
        if (session != null && !session.localOnly) {
            settingsDao.enqueue(userId, table, recordId);
        }
    }
}
