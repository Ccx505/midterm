 NotePad-Android应用的介绍文档
一.初始应用的功能
1.应用主界面和笔记列表
功能描述：​ 应用启动后显示所有笔记的列表，每个笔记条目显示标题和最后修改时间。
实现代码：


    // NotesList.java - 主界面Activity
    public class NotesList extends ListActivity {
        private static final String[] PROJECTION = new String[] {
                NotePad.Notes._ID, // 0
                NotePad.Notes.COLUMN_NAME_TITLE, // 1
                NotePad.Notes.COLUMN_NAME_NOTE, // 2
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, // 3
        };
        
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);
        
        // 设置数据URI
        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }
        
        // 设置列表适配器
        setupAdapter();
    }
    }
菜单布局文件：

<!-- list_options_menu.xml -->

<menu xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@+id/menu_add"
          android:title="New note"
          android:icon="@android:drawable/ic_input_add" />
    <item android:id="@+id/menu_search"
          android:title="Search"
          android:icon="@android:drawable/ic_search_category_default" />
    <item android:id="@+id/menu_paste"
          android:title="Paste" />
</menu>
界面截图：

(https://github.com/Ccx505/midterm/blob/master/NotePad-main/屏幕截图 2025-12-06 165104.png)

2.新建笔记功能
功能描述：​ 点击菜单中的"New note"按钮创建新笔记，进入编辑界面。
实现代码：

    // NotesList.java - 新建笔记菜单处理
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        
    if (itemId == R.id.menu_add) {
        // 启动插入新笔记的Activity
        startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
        return true;
    }
    return super.onOptionsItemSelected(item);
    }
    // NoteEditor.java - 处理插入操作
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
    final Intent intent = getIntent();
    final String action = intent.getAction();
    
    if (Intent.ACTION_INSERT.equals(action)) {
        mState = STATE_INSERT;
        // 插入新笔记到数据库
        mUri = getContentResolver().insert(intent.getData(), null);
        
        if (mUri == null) {
            Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
            finish();
            return;
        }
        
        setResult(RESULT_OK, new Intent().setAction(mUri.toString()));
    }
    }
界面截图：

![屏幕截图 2025-12-06 165135](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165135.png)

3.编辑笔记功能
功能描述：​ 点击笔记条目进入编辑界面，可以修改笔记内容。
实现代码：

    // NoteEditor.java - 笔记编辑界面
    public class NoteEditor extends Activity {
        private static final String[] PROJECTION = new String[] {
                NotePad.Notes._ID,
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_NOTE
        };
        
    @Override
    protected void onResume() {
        super.onResume();
        
        if (mCursor != null) {
            mCursor.requery();
            mCursor.moveToFirst();
            
            // 显示笔记内容
            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);
            
            // 显示笔记标题
            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            }
        }
    }
    }


编辑界面布局：

```
<!-- note_editor.xml -->
<view xmlns:android="http://schemas.android.com/apk/res/android"
    class="com.example.android.notepad.NoteEditor$LinedEditText"
    android:id="@+id/note"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@android:color/transparent"
    android:padding="5dp"
    android:scrollbars="vertical"
    android:textSize="22sp"
    android:capitalize="sentences"/>
```

界面截图：

![屏幕截图 2025-12-06 165300](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165300.png)
二.拓展基本功能
（一）.笔记条目增加时间戳显示
1.功能要求
每个笔记显示修改时间，时间格式化为易读的格式。
2.实现思路和技术实现
数据库字段定义：



    // NotePad.java - 时间字段定义
    public final class NotePad {
        public static final class Notes implements BaseColumns {
            public static final String COLUMN_NAME_CREATE_DATE = "created";
            public static final String COLUMN_NAME_MODIFICATION_DATE = "modified";
            public static final String DEFAULT_SORT_ORDER = "modified DESC";
        }
    }
    
    时间戳处理逻辑：
    
    // NotePadProvider.java - 插入新笔记时设置时间
    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        // 获取当前时间戳
        Long now = Long.valueOf(System.currentTimeMillis());
        
    ContentValues values;
    if (initialValues != null) {
        values = new ContentValues(initialValues);
    } else {
        values = new ContentValues();
    }
    
    // 设置创建时间（如果未提供）
    if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE)) {
        values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
    }
    
    // 设置修改时间（如果未提供）
    if (!values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE)) {
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
    }
    
    // 执行数据库插入操作
    SQLiteDatabase db = mOpenHelper.getWritableDatabase();
    long rowId = db.insert(NotePad.Notes.TABLE_NAME, null, values);
    
    if (rowId > 0) {
        Uri noteUri = ContentUris.withAppendedId(NotePad.Notes.CONTENT_ID_URI_BASE, rowId);
        getContext().getContentResolver().notifyChange(noteUri, null);
        return noteUri;
    }
    
    throw new SQLException("Failed to insert row into " + uri);
    }
    
    时间格式化显示：
    
    // NotesList.java - 时间格式化工具方法
    private String formatTimestamp(String timestamp) {
        try {
            if (TextUtils.isEmpty(timestamp)) {
                return "未知时间";
            }
            
        // 将字符串时间戳转换为long类型
        long time = Long.parseLong(timestamp);
        
        // 创建日期格式化器
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        
        // 格式化为易读的日期时间字符串
        return sdf.format(new Date(time));
    } catch (NumberFormatException e) {
        Log.e(TAG, "时间格式转换错误: " + timestamp, e);
        return "时间格式错误";
    }
    }
    
    // 在适配器中应用时间格式化
    private void setupAdapter() {
        Cursor cursor = getNotesCursor(mCurrentSearchQuery);
        
    String[] dataColumns = {
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
    };
    
    int[] viewIDs = { android.R.id.text1, android.R.id.text2 };
    
    // 创建自定义适配器处理时间戳格式化
    mAdapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_2, 
            cursor, dataColumns, viewIDs) {
        @Override
        public void setViewText(TextView v, String text) {
            // 如果是第二行（时间戳字段），进行格式化
            if (v.getId() == android.R.id.text2) {
                text = formatTimestamp(text);
            }
            super.setViewText(v, text);
        }
    };
    
    setListAdapter(mAdapter);
    }
3.实现效果

笔记列表显示格式化的时间戳
新建笔记自动记录创建时间

    修改笔记自动更新修改时间

界面截图：

![屏幕截图 2025-12-06 165104](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165104.png)

修改笔记后时间戳也会改变，时间戳显示最近一次修改的时间

![屏幕截图 2025-12-06 165636](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165636.png)

（二）.增强的搜索功能
1.功能要求
支持按标题和内容搜索，提供实时搜索反馈。
2.实现思路和技术实现
搜索查询实现：

    // NotesList.java - 搜索查询逻辑
    private Cursor getNotesCursor(String query) {
        String selection = null;
        String[] selectionArgs = null;
    // 如果有搜索查询，构建搜索条件
    if (!TextUtils.isEmpty(query)) {
        // 同时搜索标题和内容字段
        selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                   NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        selectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
    }
    
    // 执行查询，按修改时间倒序排列
    return managedQuery(getIntent().getData(), PROJECTION, selection, 
            selectionArgs, NotePad.Notes.DEFAULT_SORT_ORDER);
      }
    内容提供器搜索支持：
    
    // NotePadProvider.java - 搜索URI处理
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, 
                       String[] selectionArgs, String sortOrder) {
        
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(NotePad.Notes.TABLE_NAME);
    
    switch (sUriMatcher.match(uri)) {
        case SEARCH:
            qb.setProjectionMap(sNotesProjectionMap);
            
            // 从URI参数获取搜索关键词
            String query = uri.getQueryParameter(NotePad.Notes.SEARCH_QUERY_PARAM);
            if (query != null && !query.isEmpty()) {
                // 构建搜索条件
                qb.appendWhere(NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                              NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?");
                
                // 处理搜索参数
                String[] newSelectionArgs = new String[]{"%" + query + "%", "%" + query + "%"};
                if (selectionArgs != null && selectionArgs.length > 0) {
                    String[] combinedArgs = new String[selectionArgs.length + 2];
                    System.arraycopy(newSelectionArgs, 0, combinedArgs, 0, 2);
                    System.arraycopy(selectionArgs, 0, combinedArgs, 2, selectionArgs.length);
                    selectionArgs = combinedArgs;
                } else {
                    selectionArgs = newSelectionArgs;
                }
            }
            break;
            
        // 其他case处理...
    }
    
    // 执行查询
    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
    cursor.setNotificationUri(getContext().getContentResolver(), uri);
    return cursor;
    }
    
    搜索URI定义：
    
    // NotePad.java - 搜索相关URI定义
    public static final Uri SEARCH_URI = Uri.parse(SCHEME + AUTHORITY + PATH_SEARCH);
    public static final String SEARCH_QUERY_PARAM = "q";


3.实现效果

支持标题和内容全文搜索
实时显示搜索结果数量

    模糊匹配算法

界面截图：

![屏幕截图 2025-12-06 165123](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165123.png)

选择搜索后弹出搜索框，输入想要搜索的的内容或者标题，会显示搜索结果

![屏幕截图 2025-12-06 165711](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165711.png)

三.拓展附加功能
（一）.按时间排序
1.功能要求
笔记默认按修改时间倒序排列，最新修改的笔记显示在最前面。
2.实现思路和技术实现

```
排序定义：

// NotePad.java - 默认排序顺序
public static final String DEFAULT_SORT_ORDER = "modified DESC";

查询应用排序：

// 在所有查询中使用默认排序
Cursor cursor = managedQuery(
        getIntent().getData(),            // 数据URI
        PROJECTION,                       // 投影列
        selection,                        // 选择条件
        selectionArgs,                    // 选择参数
        NotePad.Notes.DEFAULT_SORT_ORDER  // 排序顺序
);

数据库查询优化：

// NotePadProvider.java - 查询方法中的排序处理
String orderBy;
if (TextUtils.isEmpty(sortOrder)) {
    // 使用默认排序
    orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
} else {
    // 使用传入的排序
    orderBy = sortOrder;
}

Cursor c = qb.query(db, projection, selection, selectionArgs, null, null, orderBy);
```



3.实现效果

笔记按修改时间倒序排列
最新修改的笔记始终显示在列表顶部

    提供一致的用户体验

界面截图：

![屏幕截图 2025-12-06 165104](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165104.png)

修改笔记后，时间戳会改变，顺序也会改变

![屏幕截图 2025-12-06 165636](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165636.png)

（二）.UI美化
1.功能要求
使用明亮的主题提升用户体验，改善界面视觉效果。
2.实现思路和技术实现
主题设置：


​    
    <!-- AndroidManifest.xml - 应用主题配置 -->
    <application android:icon="@drawable/app_notes"
     android:label="@string/app_name">
    <activity android:name="NoteEditor"
        android:theme="@android:style/Theme.Holo.Light"
        android:screenOrientation="sensor"
        android:configChanges="keyboardHidden|orientation">
        
        <intent-filter android:label="@string/resolve_edit">
            <action android:name="android.intent.action.VIEW" />
            <action android:name="android.intent.action.EDIT" />
            <category android:name="android.intent.category.DEFAULT" />
            <data android:mimeType="vnd.android.cursor.item/vnd.google.note" />
        </intent-filter>
    </activity>
    
    <activity android:name="TitleEditor"
        android:label="@string/title_edit_title"
        android:icon="@drawable/ic_menu_edit"
        android:theme="@android:style/Theme.Holo.Dialog"
        android:windowSoftInputMode="stateVisible">
    </activity>
    </application>


    布局优化：
    
    // NotesList.java - 使用系统标准布局
    private void setupAdapter() {
        // 使用系统提供的双行布局
        mAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_list_item_2, // 系统双行布局
                cursor,
                dataColumns,
                viewIDs
        ) {
            @Override
            public void setViewText(TextView v, String text) {
                // 自定义文本显示逻辑
                if (v.getId() == android.R.id.text2) {
                    text = formatTimestamp(text);
                }
                super.setViewText(v, text);
            }
        };
    setListAdapter(mAdapter);
    }
    对话框样式优化：
    
    // NotesList.java - 搜索对话框样式
    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("搜索笔记");
        
    // 设置对话框样式
    final EditText input = new EditText(this);
    input.setHint("输入关键词搜索标题或内容...");
    input.setSingleLine(true);
    
    // 自动弹出键盘
    builder.setView(input);
    AlertDialog dialog = builder.create();
    dialog.show();
    
    input.requestFocus();
    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }


3.实现效果

使用明亮的Holo主题
一致的界面风格

    更好的用户体验

界面截图：

新建笔记的时候可以点击右上角落的，弹出颜色选择的选项，点击背景颜色的选项就可以选择颜色

![屏幕截图 2025-12-06 165823](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165823.png)

![屏幕截图 2025-12-06 165830](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165830.png)





进入笔记编辑页面也可以更改已存在的笔记的颜色

![屏幕截图 2025-12-06 165842](C:\Users\Len\Pictures\Screenshots\屏幕截图 2025-12-06 165842.png)

四.技术架构详解
1.内容提供器设计
URI定义和匹配：

```
// NotePad.java - URI常量定义
public static final Uri CONTENT_URI = Uri.parse(SCHEME + AUTHORITY + PATH_NOTES);
public static final Uri CONTENT_ID_URI_BASE = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID);
public static final Uri CONTENT_ID_URI_PATTERN = Uri.parse(SCHEME + AUTHORITY + PATH_NOTE_ID + "/#");
public static final Uri SEARCH_URI = Uri.parse(SCHEME + AUTHORITY + PATH_SEARCH);

// URI匹配器配置
private static final UriMatcher sUriMatcher;
static {
    sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);
    sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);
    sUriMatcher.addURI(NotePad.AUTHORITY, "search", SEARCH);
    sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);
}
```

2.数据库设计
表结构创建：

    // NotePadProvider.java - 数据库表创建
    static class DatabaseHelper extends SQLiteOpenHelper {
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER"
                    + ");");
        }
        
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        db.execSQL("DROP TABLE IF EXISTS notes");
        onCreate(db);
    }
    }


五.项目结构说明

```
NotePad/
├── src/
│   └── com/example/android/notepad/
│       ├── NotesList.java          # 主界面Activity
│       ├── NoteEditor.java         # 笔记编辑Activity  
│       ├── TitleEditor.java        # 标题编辑Activity
│       ├── NotePadProvider.java    # 内容提供器
│       ├── NotePad.java            # 数据契约类
│       └── NotesLiveFolder.java     # Live Folder支持
├── res/
│   ├── layout/
│   │   ├── noteslist_item.xml      # 笔记列表项布局
│   │   ├── note_editor.xml         # 笔记编辑布局
│   │   └── title_editor.xml        # 标题编辑布局
│   ├── menu/
│   │   ├── list_options_menu.xml   # 主界面菜单
│   │   ├── editor_options_menu.xml # 编辑界面菜单
│   │   └── list_context_menu.xml   # 上下文菜单
│   └── values/
│       └── strings.xml              # 字符串资源
└── AndroidManifest.xml              # 应用清单文件
```


