/*
 * Copyright (C) 2016 Jorge Ruesga
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ruesga.rview.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.ruesga.rview.R;
import com.ruesga.rview.gerrit.model.AccountInfo;
import com.ruesga.rview.gerrit.model.DashboardInfo;
import com.ruesga.rview.misc.DashboardHelper;
import com.ruesga.rview.misc.SerializationManager;
import com.ruesga.rview.model.Account;
import com.ruesga.rview.model.CustomFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import androidx.annotation.NonNull;

import static com.ruesga.rview.preferences.Constants.DEFAULT_ANONYMOUS_HOME;
import static com.ruesga.rview.preferences.Constants.DEFAULT_AUTHENTICATED_HOME;
import static com.ruesga.rview.preferences.Constants.DEFAULT_DISPLAY_FORMAT;
import static com.ruesga.rview.preferences.Constants.DEFAULT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.HIGHLIGHT_SCORED_MESSAGE_MESSAGE;
import static com.ruesga.rview.preferences.Constants.MAX_SEARCH_HISTORY;
import static com.ruesga.rview.preferences.Constants.MY_FILTERS_GROUP_BASE_ID;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNTS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_ANIMATED_AVATARS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS_FORMAT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS_QUALITY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_CI_SHOW;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_CUSTOM_FILTERS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DASHBOARD;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DASHBOARD_OUTGOING_SORT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DIFF_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DISPLAY_FORMAT;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_DISPLAY_STATUSES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_FETCHED_ITEMS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_FOLLOWING;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HANDLE_LINKS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_TABS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HIGHLIGHT_UNREVIEWED;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_HOME_PAGE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_INLINE_COMMENT_IN_MESSAGES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_MESSAGES_FOLDED;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_MESSAGES_HIGHLIGHT_SCORED;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_NOTIFICATIONS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_NOTIFICATIONS_EVENTS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_SEARCH_HISTORY;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_SEARCH_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_SHORT_FILENAMES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TEXT_SIZE_FACTOR;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TOGGLE_CI_MESSAGES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_TOGGLE_TAGGED_MESSAGES;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_USE_CUSTOM_TABS;
import static com.ruesga.rview.preferences.Constants.PREF_ACCOUNT_WRAP_MODE;
import static com.ruesga.rview.preferences.Constants.PREF_IS_FIRST_RUN;
import static com.ruesga.rview.preferences.Constants.SEARCH_LAST_MODE;
import static com.ruesga.rview.preferences.Constants.SEARCH_MODE_CHANGE;

public class Preferences {

    private static String getPreferencesName(Context context) {
        return context.getPackageName();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences(getPreferencesName(context), Context.MODE_PRIVATE);
    }

    public static String getAccountPreferencesName(Account account) {
        return account.getAccountHash();
    }

    private static SharedPreferences getAccountPreferences(Context context, Account account) {
        return context.getSharedPreferences(getAccountPreferencesName(account), Context.MODE_PRIVATE);
    }

    public static String getDefaultHomePageForAccount(Account account) {
        if (account == null) {
            return DEFAULT_ANONYMOUS_HOME;
        }
        return account.hasAuthenticatedAccessMode() ?
                DEFAULT_AUTHENTICATED_HOME : DEFAULT_ANONYMOUS_HOME;
    }

    public static boolean isFirstRun(Context context) {
        return getPreferences(context).getBoolean(PREF_IS_FIRST_RUN, true);
    }

    public static void setFirstRun(Context context) {
        Editor editor = getPreferences(context).edit();
        editor.putBoolean(PREF_IS_FIRST_RUN, false);
        editor.apply();
    }

    public static Account getAccount(Context context) {
        final Gson gson = SerializationManager.getInstance();
        String value = getPreferences(context).getString(PREF_ACCOUNT, null);
        if (value == null) {
            return null;
        }

        // Ensure we obtain the most refreshed data from account
        Account account = gson.fromJson(value, Account.class);
        List<Account> accounts = getAccounts(context);
        for (Account acct : accounts) {
            if (acct.getAccountHash().equals(account.getAccountHash())) {
                return acct;
            }
        }
        return account;
    }

    public static void setAccount(Context context, Account account) {
        final Gson gson = SerializationManager.getInstance();
        Editor editor = getPreferences(context).edit();
        if (account != null) {
            editor.putString(PREF_ACCOUNT, gson.toJson(account));
        } else {
            editor.remove(PREF_ACCOUNT);
        }
        editor.apply();
    }

    public static List<Account> getAccounts(Context context) {
        final Gson gson = SerializationManager.getInstance();
        Set<String> set = getPreferences(context).getStringSet(PREF_ACCOUNTS, null);
        List<Account> accounts = new ArrayList<>();
        if (set != null) {
            for (String s : set) {
                accounts.add(gson.fromJson(s, Account.class));
            }
            Collections.sort(accounts);
        }
        return accounts;
    }

    public static List<Account> addOrUpdateAccount(Context context, @NonNull Account account) {
        return addOrUpdateAccount(context, account, account);
    }

    public static List<Account> addOrUpdateAccount(Context context,
                @NonNull Account oldAccount, @NonNull Account newAccount) {
        List<Account> accounts = getAccounts(context);
        int count = accounts.size();
        boolean found = false;
        for (int i = 0; i < count; i++) {
            Account acct = accounts.get(i);
            if (acct.getAccountHash().equals(oldAccount.getAccountHash())) {
                accounts.set(i, newAccount);
                found = true;
                break;
            }
        }
        if (!found) {
            accounts.add(newAccount);
        }

        Collections.sort(accounts);
        saveAccounts(context, accounts);
        return accounts;
    }

    public static List<Account> removeAccount(Context context, @NonNull Account account) {
        List<Account> accounts = getAccounts(context);
        Iterator<Account> it = accounts.iterator();
        while(it.hasNext()) {
            Account acct = it.next();
            if (acct.getAccountHash().equals(account.getAccountHash())) {
                it.remove();
                break;
            }
        }
        saveAccounts(context, accounts);
        return accounts;
    }

    @SuppressWarnings("Convert2streamapi")
    private static void saveAccounts(Context context, List<Account> accounts) {
        final Gson gson = SerializationManager.getInstance();
        Set<String> set = new HashSet<>();
        for (Account acct : accounts) {
            set.add(gson.toJson(acct));
        }

        Editor editor = getPreferences(context).edit();
        editor.putStringSet(PREF_ACCOUNTS, set);
        editor.apply();
    }

    public static void removeAccountPreferences(Context context, Account account) {
        Editor editor = getAccountPreferences(context, account).edit();
        editor.clear();
        editor.apply();
    }

    public static String getAccountHomePage(Context context, Account account) {
        if (account == null) {
            return getDefaultHomePageForAccount(null);
        }
        return getAccountPreferences(context, account).getString(PREF_ACCOUNT_HOME_PAGE,
                getDefaultHomePageForAccount(account));
    }

    public static int getAccountHomePageId(Context context, Account account) {
        if (account == null) {
            return context.getResources().getIdentifier(Constants.DEFAULT_ANONYMOUS_HOME,
                    "id", context.getPackageName());
        }

        String homePage = getAccountHomePage(context, account);
        if (homePage.startsWith(Constants.CUSTOM_FILTER_PREFIX)) {
            String id = homePage.substring(Constants.CUSTOM_FILTER_PREFIX.length());
            List<CustomFilter> filters = Preferences.getAccountCustomFilters(context, account);
            if (filters != null) {
                int i = 0;
                for (CustomFilter filter : filters) {
                    if (filter.mId.equals(id)) {
                        return MY_FILTERS_GROUP_BASE_ID + i;
                    }
                    i++;
                }
            }
        }
        int resId = context.getResources().getIdentifier(homePage, "id", context.getPackageName());
        if (resId == 0) {
            return context.getResources().getIdentifier(getDefaultHomePageForAccount(account),
                    "id", context.getPackageName());
        }
        return resId;
    }

    public static int getAccountFetchedItems(Context context, Account account) {
        if (account == null) {
            return Integer.valueOf(DEFAULT_FETCHED_ITEMS);
        }
        return Integer.valueOf(getAccountPreferences(context, account).getString(
                PREF_ACCOUNT_FETCHED_ITEMS, DEFAULT_FETCHED_ITEMS));
    }

    public static String getAccountDisplayFormat(Context context, Account account) {
        if (account == null) {
            return DEFAULT_DISPLAY_FORMAT;
        }
        return getAccountPreferences(context, account).getString(
                PREF_ACCOUNT_DISPLAY_FORMAT, DEFAULT_DISPLAY_FORMAT);
    }

    public static boolean isAccountAnimatedAvatars(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_ANIMATED_AVATARS, true);
    }

    public static boolean isAccountDisplayStatuses(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_DISPLAY_STATUSES, true);
    }

    public static boolean isAccountHighlightUnreviewed(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_HIGHLIGHT_UNREVIEWED, true);
    }

    public static boolean isAccountHandleLinks(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_HANDLE_LINKS, true);
    }

    public static void setAccountHandleLinks(Context context, Account account, boolean status) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HANDLE_LINKS, status);
        editor.apply();
    }

    public static boolean isAccountUseCustomTabs(Context context, Account account) {
        return account == null || getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_USE_CUSTOM_TABS, true);
    }

    public static String getAccountDiffMode(Context context, Account account) {
        String def = context.getResources().getBoolean(R.bool.config_is_tablet)
                ? Constants.DIFF_MODE_SIDE_BY_SIDE : Constants.DIFF_MODE_UNIFIED;
        if (account == null) {
            return def;
        }
        return getAccountPreferences(context, account).getString(PREF_ACCOUNT_DIFF_MODE, def);
    }

    public static void setAccountDiffMode(Context context, Account account, String mode) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putString(PREF_ACCOUNT_DIFF_MODE, mode);
        editor.apply();
    }

    public static boolean getAccountWrapMode(Context context, Account account) {
        return account == null ||
                getAccountPreferences(context, account).getBoolean(PREF_ACCOUNT_WRAP_MODE, true);
    }

    public static void setAccountWrapMode(Context context, Account account, boolean wrap) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_WRAP_MODE, wrap);
        editor.apply();
    }

    public static float getAccountTextSizeFactor(Context context, Account account) {
        if (account == null) {
            return Constants.DEFAULT_TEXT_SIZE_NORMAL;
        }

        return getAccountPreferences(context, account).getFloat(
                PREF_ACCOUNT_TEXT_SIZE_FACTOR, Constants.DEFAULT_TEXT_SIZE_NORMAL);
    }

    public static void setAccountTextSizeFactor(
            Context context, Account account, float textSizeFactor) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putFloat(PREF_ACCOUNT_TEXT_SIZE_FACTOR, textSizeFactor);
        editor.apply();
    }

    public static boolean isAccountHighlightTabs(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_TABS, true);
    }

    public static void setAccountHighlightTabs(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_TABS, highlight);
        editor.apply();
    }

    public static boolean isAccountHighlightTrailingWhitespaces(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES, true);
    }

    public static void setAccountHighlightTrailingWhitespaces(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_TRAILING_WHITESPACES, highlight);
        editor.apply();
    }

    public static boolean isAccountHighlightIntralineDiffs(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS, true);
    }

    public static void setAccountHighlightIntralineDiffs(
            Context context, Account account, boolean highlight) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putBoolean(PREF_ACCOUNT_HIGHLIGHT_INTRALINE_DIFFS, highlight);
        editor.apply();
    }


    public static int getAccountSearchMode(Context context, Account account) {
        if (account == null) {
            return Constants.SEARCH_MODE_CHANGE;
        }
        return getAccountPreferences(context, account).getInt(
                PREF_ACCOUNT_SEARCH_MODE, Constants.SEARCH_MODE_CHANGE);
    }

    public static void setAccountSearchMode(Context context, Account account, int mode) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putInt(PREF_ACCOUNT_SEARCH_MODE, mode);
        editor.apply();
    }

    public static boolean isAccountMessagesFolded(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_MESSAGES_FOLDED, false);
    }

    public static int getAccountMessagesHighlightScored(Context context, Account account) {
        if (account == null) {
            return HIGHLIGHT_SCORED_MESSAGE_MESSAGE;
        }
        return Integer.parseInt(getAccountPreferences(
                context, account).getString(PREF_ACCOUNT_MESSAGES_HIGHLIGHT_SCORED,
                        String.valueOf(HIGHLIGHT_SCORED_MESSAGE_MESSAGE)));
    }

    public static boolean isAccountInlineCommentInMessages(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_INLINE_COMMENT_IN_MESSAGES, true);
    }

    public static boolean isAccountToggleTaggedMessages(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_TOGGLE_TAGGED_MESSAGES, false);
    }

    public static boolean isAccountToggleCIAccountsMessages(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_TOGGLE_CI_MESSAGES, false);
    }

    public static boolean isAccountShortFilenames(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_SHORT_FILENAMES, false);
    }

    public static boolean isAccountImageAttachmentsOptimizations(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS, false);
    }

    public static Bitmap.CompressFormat getAccountImageAttachmentsOptimizationsFormat(
            Context context, Account account) {
        if (account == null) {
            return Bitmap.CompressFormat.JPEG;
        }
        return Bitmap.CompressFormat.valueOf(getAccountPreferences(
                context, account).getString(
                PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS_FORMAT,
                Bitmap.CompressFormat.JPEG.name()));
    }

    public static int getAccountImageAttachmentsOptimizationsQuality(
            Context context, Account account) {
        if (account == null) {
            return 5;
        }
        return getAccountPreferences(context, account).getInt(
                PREF_ACCOUNT_ATTACHMENTS_IMAGE_OPTIMIZATIONS_QUALITY, 5);
    }

    public static boolean isAccountShowCIStatuses(Context context, Account account) {
        return account == null || getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_CI_SHOW, true);
    }

    public static List<CustomFilter> getAccountCustomFilters(Context context, Account account) {
        if (account == null) {
            return null;
        }

        Set<String> set = getAccountPreferences(context, account)
                .getStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, null);
        if (set == null) {
            return null;
        }

        boolean save = false;
        List<CustomFilter> filters = new ArrayList<>(set.size());
        for (String s : set) {
            CustomFilter cf = SerializationManager.getInstance().fromJson(s, CustomFilter.class);
            if (cf.mId == null) {
                cf.mId = UUID.randomUUID().toString();
                save = true;
            }
            filters.add(cf);
        }
        Collections.sort(filters);

        if (save) {
            setAccountCustomFilters(context, account, filters);
        }
        return filters;
    }

    @SuppressWarnings("Convert2streamapi")
    public static void setAccountCustomFilters(
            Context context, Account account, List<CustomFilter> filters) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        if (filters == null || filters.isEmpty()) {
            editor.remove(PREF_ACCOUNT_CUSTOM_FILTERS);
        } else {
            Set<String> set = new HashSet<>();
            for (CustomFilter cf : filters) {
                set.add(SerializationManager.getInstance().toJson(cf));
            }
            editor.putStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, set);
        }
        editor.apply();
    }

    public static void saveAccountCustomFilter(
            Context context, Account account, CustomFilter filter) {
        if (account == null) {
            return;
        }

        Set<String> set = getAccountPreferences(context, account)
                .getStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, null);
        if (set == null) {
            set = new HashSet<>();
        } else {
            // https://android-review.googlesource.com/#/c/134312/
            set = new HashSet<>(set);
        }

        // Remove and readd the custom filter
        for (String s : set) {
            CustomFilter cf = SerializationManager.getInstance().fromJson(s, CustomFilter.class);
            if (cf.equals(filter)) {
                set.remove(s);
            }
        }
        set.add(SerializationManager.getInstance().toJson(filter));

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putStringSet(PREF_ACCOUNT_CUSTOM_FILTERS, set);
        editor.apply();
    }

    public static boolean isAccountNotificationsEnabled(Context context, Account account) {
        return account != null && getAccountPreferences(
                context, account).getBoolean(PREF_ACCOUNT_NOTIFICATIONS, false);
    }

    public static int getAccountNotificationsEvents(Context context, Account account) {
        if (account == null) {
            return 0;
        }

        int events = 0;
        Set<String> set = getAccountPreferences(
                context, account).getStringSet(PREF_ACCOUNT_NOTIFICATIONS_EVENTS, null);
        if (set != null) {
            for (String bitwise : set) {
                events |= Integer.valueOf(bitwise);
            }
        }
        return events;
    }

    public static String[] getAccountSearchHistory(Context context, Account account, int type) {
        if (account == null) {
            return null;
        }

        String v = getAccountPreferences(
                context, account).getString(PREF_ACCOUNT_SEARCH_HISTORY + type, null);
        if (TextUtils.isEmpty(v)) {
            return null;
        }

        final Gson gson = SerializationManager.getInstance();
        return gson.fromJson(v, String[].class);
    }

    public static void addAccountSearchHistory(
            Context context, Account account, int type, String query) {
        if (account == null) {
            return;
        }

        // Obtain the list
        String[] prev = getAccountSearchHistory(context, account, type);
        List<String> list = new ArrayList<>();
        if (prev != null) {
            list.addAll(Arrays.asList(prev));
        }
        int index = list.indexOf(query);
        if (index != -1) {
            list.remove(index);
        }
        list.add(0, query);

        // Trim array
        int size = list.size();
        if (size > MAX_SEARCH_HISTORY) {
            list.subList(MAX_SEARCH_HISTORY, size).clear();
        }

        final Gson gson = SerializationManager.getInstance();
        Editor editor = getAccountPreferences(context, account).edit();
        editor.putString(PREF_ACCOUNT_SEARCH_HISTORY + type, gson.toJson(list));
        editor.apply();
    }

    public static void clearAccountSearchHistory(Context context, Account account) {
        if (account == null) {
            return;
        }

        Editor editor = getAccountPreferences(context, account).edit();
        for (int i = SEARCH_MODE_CHANGE; i <= SEARCH_LAST_MODE; i++) {
            editor.remove(PREF_ACCOUNT_SEARCH_HISTORY + i);
        }
        editor.apply();
    }

    public static boolean hasAccountSearchHistory(Context context, Account account) {
        if (account == null) {
            return false;
        }

        for (int i = SEARCH_MODE_CHANGE; i <= SEARCH_LAST_MODE; i++) {
            String[] history = getAccountSearchHistory(context, account, i);
            if (history != null && history.length > 0) {
                return true;
            }
        }
        return false;
    }

    public static boolean isAccountDashboardOngoingSort(Context context, Account account) {
        return account != null && getAccountPreferences(context, account)
                .getBoolean(PREF_ACCOUNT_DASHBOARD_OUTGOING_SORT, false);
    }

    public static Set<AccountInfo> getAccountFollowingState(Context context, Account account) {
        if (account == null) {
            return new HashSet<>();
        }

        Set<String> followingAccounts = getAccountPreferences(
                context, account).getStringSet(PREF_ACCOUNT_FOLLOWING, null);
        if (followingAccounts == null) {
            return new HashSet<>();
        }

        Set<AccountInfo> accounts = new HashSet<>();
        for (String acct : followingAccounts) {
            accounts.add(SerializationManager.getInstance().fromJson(acct, AccountInfo.class));
        }
        return accounts;
    }

    public static boolean isAccountFollowingState(
            Context context, Account account, AccountInfo accountId) {
        for (AccountInfo acct : getAccountFollowingState(context, account)) {
            if (acct.accountId == accountId.accountId) {
                return true;
            }
        }
        return false;
    }

    public static void setAccountFollowingState(
            Context context, Account account, AccountInfo acct, boolean follow) {
        Set<AccountInfo> accounts = getAccountFollowingState(context, account);
        Set<String> followingAccounts = new HashSet<>();
        for (AccountInfo c : accounts) {
            if (c.accountId != acct.accountId) {
                followingAccounts.add(SerializationManager.getInstance().toJson(c));
            }
        }
        String json = SerializationManager.getInstance().toJson(acct);
        if (follow) {
            followingAccounts.add(json);
        }

        Editor editor = getAccountPreferences(context, account).edit();
        editor.putStringSet(PREF_ACCOUNT_FOLLOWING, followingAccounts);
        editor.apply();
    }

    public static DashboardInfo getAccountDashboard(Context context, Account account) {
        String dashboard = getAccountPreferences(
                context, account).getString(PREF_ACCOUNT_DASHBOARD, null);
        if (dashboard == null) {
            return DashboardHelper.createDefaultDashboard(context);
        }
        return SerializationManager.getInstance().fromJson(dashboard, DashboardInfo.class);
    }

    public static void setAccountDashboard(
            Context context, Account account, DashboardInfo dashboard) {
        Editor editor = getAccountPreferences(context, account).edit();
        editor.putString(PREF_ACCOUNT_DASHBOARD,
                SerializationManager.getInstance().toJson(dashboard));
        editor.apply();
    }
}
