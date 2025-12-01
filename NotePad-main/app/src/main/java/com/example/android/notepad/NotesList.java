/*
 * Copyright (C) 2007 The Android Open Source Project
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

package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Displays a list of notes. Will display notes from the {@link Uri}
 * provided in the incoming Intent if there is one, otherwise it defaults to displaying the
 * contents of the {@link NotePadProvider}.
 *
 * NOTE: Notice that the provider operations in this Activity are taking place on the UI thread.
 * This is not a good practice. It is only done here to make the code more readable. A real
 * application should use the {@link android.content.AsyncQueryHandler} or
 * {@link android.os.AsyncTask} object to perform operations asynchronously on a separate thread.
 */
public class NotesList extends ListActivity {

    // For logging and debugging
    private static final String TAG = "NotesList";

    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_NOTE, // 2 - 用于内容搜索
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 3 - 时间戳字段
            NotePad.Notes.COLUMN_NAME_BACKGROUND_COLOR // 4 - 新增：背景颜色字段
    };

    /** The index of the title column */
    private static final int COLUMN_INDEX_TITLE = 1;
    /** The index of the note content column */
    private static final int COLUMN_INDEX_NOTE = 2;
    /** The index of the modification date column */
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 3;
    /** The index of the background color column */
    private static final int COLUMN_INDEX_BACKGROUND_COLOR = 4; // 新增

    // 搜索相关变量
    private SimpleCursorAdapter mAdapter;
    private String mCurrentSearchQuery = "";

    /**
     * onCreate is called when Android starts this Activity from scratch.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The user does not need to hold down the key to use menu shortcuts.
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        /* If no data is given in the Intent that started this Activity, then this Activity
         * was started when the intent filter matched a MAIN action. We should use the default
         * provider URI.
         */
        // Gets the intent that started this Activity.
        Intent intent = getIntent();

        // If there is no data associated with the Intent, sets the data to the default URI, which
        // accesses a list of notes.
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        /*
         * Sets the callback for context menu activation for the ListView. The listener is set
         * to be this Activity. The effect is that context menus are enabled for items in the
         * ListView, and the context menu is handled by a method in NotesList.
         */
        getListView().setOnCreateContextMenuListener(this);

        // 初始化适配器并设置列表
        setupAdapter();
    }

    /**
     * 设置适配器
     */
    private void setupAdapter() {
        // 执行查询
        Cursor cursor = getNotesCursor(mCurrentSearchQuery);

        // 数据列映射
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };

        // 视图ID映射 - 使用自定义布局中的ID
        int[] viewIDs = {
                R.id.text1,        // 标题
                R.id.timestamp     // 时间戳
        };

        // 创建自定义适配器
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,  // 使用自定义布局
                cursor,
                dataColumns,
                viewIDs
        ) {
            @Override
            public void setViewText(TextView v, String text) {
                // 如果是时间戳字段，进行格式化
                if (v.getId() == R.id.timestamp) {
                    text = formatTimestamp(text);
                }
                super.setViewText(v, text);
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // 获取列表项视图
                View view = super.getView(position, convertView, parent);

                // 获取当前笔记的数据
                Cursor cursor = (Cursor) getItem(position);
                if (cursor != null) {
                    // 获取背景颜色
                    int backgroundColor = cursor.getInt(COLUMN_INDEX_BACKGROUND_COLOR);

                    // 更新颜色指示器
                    updateColorIndicator(view, backgroundColor);
                }
                return view;
            }
        };

        // 设置列表适配器
        setListAdapter(mAdapter);
    }

    /**
     * 更新颜色指示器
     */
    private void updateColorIndicator(View listItemView, int color) {
        // 查找颜色指示器视图
        View colorIndicator = listItemView.findViewById(R.id.color_indicator);

        if (colorIndicator != null) {
            if (color != NotePad.Notes.DEFAULT_BACKGROUND_COLOR) {
                // 如果不是默认白色，显示颜色指示器
                colorIndicator.setBackgroundColor(color);
                colorIndicator.setVisibility(View.VISIBLE);
            } else {
                // 如果是白色，隐藏颜色指示器
                colorIndicator.setVisibility(View.GONE);
            }
        }
    }

    /**
     * 格式化时间戳
     */
    private String formatTimestamp(String timestamp) {
        try {
            if (TextUtils.isEmpty(timestamp)) {
                return "未知时间";
            }

            long time = Long.parseLong(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            return sdf.format(new Date(time));
        } catch (NumberFormatException e) {
            return "时间格式错误";
        }
    }

    /**
     * 获取笔记游标，支持搜索
     */
    private Cursor getNotesCursor(String query) {
        String selection = null;
        String[] selectionArgs = null;

        // 如果有搜索查询，添加搜索条件
        if (!TextUtils.isEmpty(query)) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                    NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
            selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
        }

        /* Performs a managed query. The Activity handles closing and requerying the cursor
         * when needed.
         */
        return managedQuery(
                getIntent().getData(),            // Use the default content URI for the provider.
                PROJECTION,                       // Return the note ID, title and modification date.
                selection,                         // Search where clause
                selectionArgs,                    // Search arguments
                NotePad.Notes.DEFAULT_SORT_ORDER  // Use the default sort order.
        );
    }

    /**
     * 执行搜索
     */
    private void performSearch(String query) {
        mCurrentSearchQuery = query;
        // 重新查询并更新游标
        Cursor newCursor = getNotesCursor(query);
        mAdapter.changeCursor(newCursor);
        mAdapter.notifyDataSetChanged();

        int count = newCursor != null ? newCursor.getCount() : 0;
        String message = count > 0 ?
                "找到 " + count + " 个匹配结果" : "没有找到包含 \"" + query + "\" 的笔记";

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * 清除搜索
     */
    private void clearSearch() {
        mCurrentSearchQuery = "";
        Cursor newCursor = getNotesCursor("");
        mAdapter.changeCursor(newCursor);
        mAdapter.notifyDataSetChanged();
        Toast.makeText(this, "显示所有笔记", Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示搜索对话框
     */
    private void showSearchDialog() {
        // 创建AlertDialog构建器
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("搜索笔记");

        // 创建输入框
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("输入关键词搜索标题或内容...");
        input.setSingleLine(true);

        // 设置布局参数
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        );
        input.setLayoutParams(lp);

        // 创建容器并添加输入框
        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        int padding = dpToPx(20);
        container.setPadding(padding, padding, padding, padding);
        container.addView(input);

        builder.setView(container);

        // 设置按钮
        builder.setPositiveButton("搜索", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String query = input.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                } else {
                    Toast.makeText(NotesList.this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
                }
            }
        });

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // 显示对话框
        android.app.AlertDialog dialog = builder.create();
        dialog.show();

        // 自动弹出键盘
        input.requestFocus();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    /**
     * dp转px
     */
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    /**
     * Called when the user clicks the device's Menu button the first time for
     * this Activity. Android passes in a Menu object that is populated with items.
     *
     * @param menu A Menu object to which items should be added.
     * @return True to display the menu.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate menu from XML resource
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // The paste menu item is enabled if there is data on the clipboard.
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);

        // If the clipboard contains an item, enables the Paste option on the menu.
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            // If the clipboard is empty, disables the menu's Paste option.
            mPasteItem.setEnabled(false);
        }

        // Gets the number of notes currently being displayed.
        final boolean haveItems = getListAdapter().getCount() > 0;

        // If there are any notes in the list (which implies that one of
        // them is selected), then we need to generate the actions that
        // can be performed on the current selection. This will be a combination
        // of our own specific actions along with any extensions that can be
        // found.
        if (haveItems) {

            // This is the selected item.
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());

            // Creates an array of Intents with one element. This will be used to send an Intent
            // based on the selected menu item.
            Intent[] specifics = new Intent[1];

            // Sets the Intent in the array to be an EDIT action on the URI of the selected note.
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            // Creates an array of menu items with one element. This will contain the EDIT option.
            MenuItem[] items = new MenuItem[1];

            // Creates an Intent with no specific action, using the URI of the selected note.
            Intent intent = new Intent(null, uri);

            /* Adds the category ALTERNATIVE to the Intent, with the note ID URI as its
             * data. This prepares the Intent as a place to group alternative options in the
             * menu.
             */
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            /*
             * Add alternatives to the menu
             */
            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,  // Add the Intents as options in the alternatives group.
                    Menu.NONE,                  // A unique item ID is not required.
                    Menu.NONE,                  // The alternatives don't need to be in order.
                    null,                       // The caller's name is not excluded from the group.
                    specifics,                  // These specific options must appear first.
                    intent,                     // These Intent objects map to the options in specifics.
                    Menu.NONE,                  // No flags are required.
                    items                       // The menu items generated from the specifics-to-
                    // Intents mapping
            );
            // If the Edit menu item exists, adds shortcuts for it.
            if (items[0] != null) {
                // Sets the Edit menu item shortcut to numeric "1", letter "e"
                items[0].setShortcut('1', 'e');
            }
        } else {
            // If the list is empty, removes any existing alternative actions from the menu
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        // Displays the menu
        return true;
    }

    /**
     * This method is called when the user selects an option from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.menu_add) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_INSERT. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (itemId == R.id.menu_paste) {
            /*
             * Launches a new Activity using an Intent. The intent filter for the Activity
             * has to have action ACTION_PASTE. No category is set, so DEFAULT is assumed.
             * In effect, this starts the NoteEditor Activity in NotePad.
             */
            startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
            return true;
        } else if (itemId == R.id.menu_search) {
            // 显示搜索对话框
            showSearchDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);

        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        int id = item.getItemId();
        if (id == R.id.context_open) {
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "Note", noteUri));
            return true;
        } else if (id == R.id.context_delete) {
            getContentResolver().delete(noteUri, null, null);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        try {
            // 构建笔记URI - 使用正确的URI格式
            Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_URI, id);

            // 使用明确的类名启动NoteEditor
            Intent editIntent = new Intent(this, NoteEditor.class);
            editIntent.setAction(Intent.ACTION_EDIT);
            editIntent.setData(noteUri);

            // 添加调试信息
            Log.d(TAG, "启动NoteEditor: " + noteUri.toString());

            startActivity(editIntent);

        } catch (Exception e) {
            Log.e(TAG, "启动笔记编辑器失败: " + e.getMessage(), e);
            Toast.makeText(this, "无法打开笔记", Toast.LENGTH_SHORT).show();
        }
    }
}