package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class NotesList extends ListActivity {
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
            NotePad.Notes.COLUMN_NAME_CATEGORY_ID,
            NotePad.Notes.COLUMN_NAME_COMPLETED
    };

    private static final int COLUMN_INDEX_TITLE = 1;
    private static final int COLUMN_INDEX_MODIFICATION_DATE = 2;
    private static final int COLUMN_INDEX_CATEGORY_ID = 3;
    private static final int COLUMN_INDEX_COMPLETED = 4;
    private static final int COLUMN_INDEX_ID = 0;

    private HashMap<Long, String> mCategoryColors;

    private static final int[] BACKGROUND_COLORS = {
            android.R.color.white,
            android.R.color.holo_blue_light,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_purple
    };
    private static final String[] COLOR_NAMES = {
            "默认", "浅蓝色", "浅绿色", "浅橙色", "浅紫色"
    };
    private int mCurrentColorIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.notes_list);

        initUI();

        mCategoryColors = new HashMap<Long, String>();

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();

        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        String sortOrder = NotePad.Notes.COLUMN_NAME_COMPLETED + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER;
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                sortOrder
        );

        String[] from = new String[] {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_CATEGORY_ID,
                NotePad.Notes.COLUMN_NAME_COMPLETED
        };

        int[] to = new int[] {
                R.id.text1,
                R.id.text2,
                R.id.category_text,
                R.id.note_completed_checkbox
        };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.notes_row,
                cursor,
                from,
                to
        );

        loadCategoryColors();

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                View rootView = view;
                while (rootView.getParent() != null && rootView.getId() != R.id.text1 && !(rootView instanceof ListView)) {
                    rootView = (View) rootView.getParent();
                }

                if (columnIndex == COLUMN_INDEX_MODIFICATION_DATE) {
                    long timestamp = cursor.getLong(columnIndex);
                    String formattedDate = formatDate(timestamp);
                    ((TextView) view).setText(formattedDate);
                    return true;
                }
                else if (columnIndex == COLUMN_INDEX_CATEGORY_ID) {
                    long categoryId = cursor.getLong(columnIndex);
                    Cursor categoryCursor = getContentResolver().query(
                            NotePad.Categories.CONTENT_URI,
                            new String[]{NotePad.Categories.COLUMN_NAME_NAME},
                            NotePad.Categories._ID + " = ?",
                            new String[]{String.valueOf(categoryId)},
                            null);

                    String categoryName = "";
                    if (categoryCursor != null && categoryCursor.moveToFirst()) {
                        int nameIndex = categoryCursor.getColumnIndex(NotePad.Categories.COLUMN_NAME_NAME);
                        if (!categoryCursor.isNull(nameIndex)) {
                            categoryName = categoryCursor.getString(nameIndex);
                        }
                        categoryCursor.close();
                    }

                    TextView textView = (TextView) view;
                    if (categoryName != null) {
                        textView.setText(categoryName);
                    }
                    String color = getCategoryColor(categoryId);

                    if (color != null && !color.isEmpty()) {
                        try {
                            int colorInt = android.graphics.Color.parseColor(color);
                            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                            drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                            drawable.setCornerRadius(8);
                            int bgColor = android.graphics.Color.argb(200,
                                    android.graphics.Color.red(colorInt),
                                    android.graphics.Color.green(colorInt),
                                    android.graphics.Color.blue(colorInt));
                            drawable.setColor(bgColor);
                            rootView.setBackgroundDrawable(drawable);
                        } catch (Exception e) {
                            rootView.setBackgroundResource(0);
                        }
                    } else {
                        rootView.setBackgroundResource(0);
                    }

                    return true;
                } else if (columnIndex == COLUMN_INDEX_COMPLETED) {
                    final boolean completed = cursor.getInt(columnIndex) == 1;
                    final CheckBox checkBox = (CheckBox) view;
                    final long noteId = cursor.getLong(COLUMN_INDEX_ID);
                    final TextView titleView = (TextView) rootView.findViewById(R.id.text1);

                    checkBox.setChecked(completed);

                    if (completed) {
                        titleView.setPaintFlags(titleView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                    } else {
                        titleView.setPaintFlags(titleView.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                    }

                    checkBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            v.setPressed(true);
                            boolean newCompletedState = checkBox.isChecked();
                            updateNoteCompletionStatus(noteId, newCompletedState);
                        }
                    });

                    checkBox.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            return true;
                        }
                    });

                    return true;
                }
                return false;
            }
        });

        setListAdapter(adapter);

        setupCategoryFilterBar();

        registerForContextMenu(getListView());
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }

    private void setupCategoryFilterBar() {
        HorizontalScrollView scrollView = new HorizontalScrollView(this);

        LinearLayout categoryLayout = new LinearLayout(this);
        categoryLayout.setOrientation(LinearLayout.HORIZONTAL);
        categoryLayout.setPadding(8, 8, 8, 8);

        Button allButton = createCategoryButton(getString(R.string.all_categories), 0);
        allButton.setSelected(true);
        categoryLayout.addView(allButton);

        loadCategories(categoryLayout);

        Button manageButton = new Button(this);
        manageButton.setText(getString(R.string.manage_categories));

        manageButton.setTextColor(android.graphics.Color.WHITE);
        manageButton.setTextSize(14);
        manageButton.setPadding(16, 8, 16, 8);

        android.graphics.drawable.GradientDrawable manageDrawable = new android.graphics.drawable.GradientDrawable();
        manageDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        manageDrawable.setCornerRadius(20);
        manageDrawable.setColor(android.graphics.Color.parseColor("#FF9800"));
        manageDrawable.setStroke(2, android.graphics.Color.parseColor("#FFB74D"));

        manageButton.setBackgroundDrawable(manageDrawable);

        manageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(NotesList.this, CategoryManager.class));
            }
        });
        LinearLayout.LayoutParams manageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        manageParams.setMargins(8, 4, 8, 4);
        manageButton.setLayoutParams(manageParams);
        categoryLayout.addView(manageButton);

        scrollView.addView(categoryLayout);

        LinearLayout categoryFilterContainer = (LinearLayout) findViewById(R.id.category_filter_container);
        if (categoryFilterContainer != null) {
            categoryFilterContainer.removeAllViews();
            categoryFilterContainer.addView(scrollView);
        }
    }

    private Button createCategoryButton(String name, final long categoryId, String color) {
        Button button = new Button(this);
        button.setText(name);

        button.setTextColor(android.graphics.Color.BLACK);
        button.setTextSize(14);
        button.setPadding(16, 8, 16, 8);

        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
        drawable.setCornerRadius(20);

        if (name.equals(getString(R.string.all_categories))) {
            drawable.setColor(android.graphics.Color.parseColor("#42A5F5"));
            button.setTextColor(android.graphics.Color.WHITE);
        } else if (color != null && !color.isEmpty()) {
            try {
                int colorInt = android.graphics.Color.parseColor(color);
                drawable.setColor(colorInt);
                int r = (colorInt >> 16) & 0xFF;
                int g = (colorInt >> 8) & 0xFF;
                int b = colorInt & 0xFF;
                double brightness = (0.299 * r + 0.587 * g + 0.114 * b) / 255;
                if (brightness < 0.5) {
                    button.setTextColor(android.graphics.Color.WHITE);
                }
            } catch (Exception e) {
                drawable.setColor(android.graphics.Color.LTGRAY);
            }
        } else {
            drawable.setColor(android.graphics.Color.LTGRAY);
        }

        drawable.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));

        button.setBackgroundDrawable(drawable);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filterNotesByCategory(categoryId);
                updateCategoryButtonSelection((Button) v);
            }
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 4, 8, 4);
        button.setLayoutParams(params);
        return button;
    }

    private Button createCategoryButton(String name, final long categoryId) {
        return createCategoryButton(name, categoryId, null);
    }

    private void loadCategories(LinearLayout categoryLayout) {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
                NotePad.Categories.CONTENT_URI,
                new String[] { NotePad.Categories._ID, NotePad.Categories.COLUMN_NAME_NAME, NotePad.Categories.COLUMN_NAME_COLOR },
                null, null, NotePad.Categories.DEFAULT_SORT_ORDER
        );

        if (cursor != null) {
            try {
                // 先加载所有非"未分类"的分类
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    // 跳过ID为1的"未分类"分类（将在最后添加）
                    if (id != 1) {
                        String name = cursor.getString(1);
                        String color = cursor.getString(2);

                        Button categoryButton = createCategoryButton(name, id, color);
                        categoryLayout.addView(categoryButton);
                    }
                }
                
                // 加载完成后，添加"未分类"分类（ID为1）
                String uncategorizedName = getString(R.string.uncategorized);
                Button uncategorizedButton = createCategoryButton(uncategorizedName, 1, null);
                categoryLayout.addView(uncategorizedButton);
            } finally {
                cursor.close();
            }
        }
    }

    private void filterNotesByCategory(long categoryId) {
        String selection = null;
        String[] selectionArgs = null;

        if (categoryId > 0) {
            selection = NotePad.Notes.COLUMN_NAME_CATEGORY_ID + " = ?";
            selectionArgs = new String[] { String.valueOf(categoryId) };
        }

        String sortOrder = NotePad.Notes.COLUMN_NAME_COMPLETED + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER;

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder);

        ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
    }

    private void updateCategoryButtonSelection(Button selectedButton) {
        LinearLayout parentLayout = (LinearLayout) selectedButton.getParent();
        if (parentLayout != null) {
            for (int i = 0; i < parentLayout.getChildCount(); i++) {
                View child = parentLayout.getChildAt(i);
                if (child instanceof Button) {
                    Button button = (Button) child;
                    button.setSelected(child == selectedButton);
                    if (button.getText().equals(getString(R.string.all_categories))) {
                        android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
                        drawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                        drawable.setCornerRadius(20);
                        drawable.setColor(android.graphics.Color.parseColor("#42A5F5"));
                        drawable.setStroke(2, android.graphics.Color.parseColor("#E0E0E0"));
                        button.setBackgroundDrawable(drawable);
                        button.setTextColor(android.graphics.Color.WHITE);
                    }
                }
            }
        }
    }

    private void showSearchDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.search_notes);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.search_dialog, null);
        builder.setView(dialogView);

        final EditText searchEditText = (EditText) dialogView.findViewById(R.id.search_edit_text);
        final Button searchButton = (Button) dialogView.findViewById(R.id.button_search);

        if (searchButton != null) {
            searchButton.setEnabled(false);
        }

        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchButton != null) {
                        searchButton.setEnabled(s.length() > 0);
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        builder.setPositiveButton(R.string.search, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (searchEditText != null) {
                    String searchText = searchEditText.getText().toString();
                    searchNotes(searchText);
                }
            }
        });

        final AlertDialog dialog = builder.create();

        Button cancelButton = (Button) dialogView.findViewById(R.id.button_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (searchButton != null) {
            searchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (searchEditText != null) {
                        String searchText = searchEditText.getText().toString();
                        searchNotes(searchText);
                        dialog.dismiss();
                    }
                }
            });
        }

        dialog.show();
    }

    private void searchNotes(String searchText) {
        String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ? OR " +
                NotePad.Notes.COLUMN_NAME_NOTE + " LIKE ?";
        String[] selectionArgs = new String[] { "%" + searchText + "%", "%" + searchText + "%" };

        String sortOrder = NotePad.Notes.COLUMN_NAME_COMPLETED + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER;

        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                sortOrder);

        ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);

        Toast.makeText(this, getString(R.string.search_results, cursor.getCount()), Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menu_add) {
            startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
            return true;
        } else if (itemId == R.id.menu_search) {
            showSearchDialog();
            return true;
        } else if (itemId == R.id.menu_paste) {
            pasteNote();
            return true;
        } else if (itemId == R.id.menu_category_manager) {
            startActivity(new Intent(getApplicationContext(), CategoryManager.class));
            return true;
        } else if (itemId == R.id.menu_toggle_background) {
            mCurrentColorIndex = (mCurrentColorIndex + 1) % BACKGROUND_COLORS.length;
            LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);
            mainLayout.setBackgroundResource(BACKGROUND_COLORS[mCurrentColorIndex]);
            Toast.makeText(this, "已切换到" + COLOR_NAMES[mCurrentColorIndex] + "背景", Toast.LENGTH_SHORT).show();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @SuppressWarnings("deprecation")
    private void pasteNote() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        boolean hasText = false;
        String text = null;

        try {
            if (clipboard.hasPrimaryClip()) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                CharSequence textSequence = item.getText();
                if (textSequence != null) {
                    text = textSequence.toString();
                    hasText = true;
                }
            }
        } catch (Exception e) {
            if (clipboard.getText() != null) {
                text = clipboard.getText().toString();
                hasText = true;
            }
        }

        if (hasText && text != null) {
            ContentValues values = new ContentValues();
            values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

            String title = "Pasted Note";
            String[] lines = text.split("\\n");
            if (lines.length > 0 && !lines[0].trim().isEmpty()) {
                title = lines[0].trim();
                if (title.length() > 50) {
                    title = title.substring(0, 50) + "...";
                }
            }
            values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

            Uri newNoteUri = getContentResolver().insert(NotePad.Notes.CONTENT_URI, values);

            Toast.makeText(NotesList.this, R.string.note_created, Toast.LENGTH_SHORT).show();

            requery();
        }
    }

    private void initUI() {
        View searchButton = findViewById(R.id.search_button);
        View createNoteButton = findViewById(R.id.create_note_button);
        View todoToggleButton = findViewById(R.id.todo_toggle_button);

        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchDialog();
            }
        });

        createNoteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_INSERT, getIntent().getData());
                startActivity(intent);
            }
        });

        todoToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(NotesList.this, TodoList.class);
                startActivity(intent);
            }
        });

        LinearLayout mainLayout = (LinearLayout) findViewById(R.id.main_layout);
        mainLayout.setBackgroundResource(BACKGROUND_COLORS[mCurrentColorIndex]);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(
                Menu.CATEGORY_ALTERNATIVE,
                0,
                0,
                new ComponentName(this, NotesList.class),
                null,
                intent,
                0,
                null);

        return super.onCreateOptionsMenu(menu);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        boolean hasClip = false;
        try {
            if (clipboard.hasPrimaryClip()) {
                hasClip = true;
            }
        } catch (Exception e) {
            if (clipboard.getText() != null) {
                hasClip = true;
            }
        }

        MenuItem pasteItem = menu.findItem(R.id.menu_paste);
        if (pasteItem != null) {
            pasteItem.setEnabled(hasClip);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;
        MenuItem deleteItem = menu.findItem(R.id.menu_delete);
        if (deleteItem != null) {
            deleteItem.setEnabled(haveItems);
        }

        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;

        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            return false;
        }

        long id = info.id;

        Uri noteUri = Uri.withAppendedPath(getIntent().getData(), Long.toString(id));

        int itemId = item.getItemId();
        if (itemId == R.id.context_open) {
            startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
            return true;
        } else if (itemId == R.id.context_delete) {
            getContentResolver().delete(noteUri, null, null);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = Uri.withAppendedPath(getIntent().getData(), "" + id);

        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }

    private void loadCategoryColors() {
        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
                NotePad.Categories.CONTENT_URI,
                new String[] { NotePad.Categories._ID, NotePad.Categories.COLUMN_NAME_COLOR },
                null, null, null
        );

        if (cursor != null) {
            try {
                mCategoryColors.clear();
                while (cursor.moveToNext()) {
                    long id = cursor.getLong(0);
                    String color = cursor.getString(1);
                    mCategoryColors.put(id, color);
                }
            } finally {
                cursor.close();
            }
        }
    }

    private String getCategoryColor(long categoryId) {
        if (mCategoryColors.isEmpty()) {
            loadCategoryColors();
        }
        return mCategoryColors.get(categoryId);
    }

    private void updateCategoryColors() {
        loadCategoryColors();
        if (getListAdapter() != null && ((SimpleCursorAdapter)getListAdapter()).getCursor() != null) {
            ((SimpleCursorAdapter)getListAdapter()).notifyDataSetChanged();
        }
    }

    private void requery() {
        String sortOrder = NotePad.Notes.COLUMN_NAME_COMPLETED + " ASC, " + NotePad.Notes.DEFAULT_SORT_ORDER;
        Cursor cursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                sortOrder);

        ((SimpleCursorAdapter) getListAdapter()).changeCursor(cursor);
    }

    private void updateNoteCompletionStatus(long noteId, boolean completed) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_COMPLETED, completed ? 1 : 0);

        Uri noteUri = Uri.withAppendedPath(NotePad.Notes.CONTENT_URI, Long.toString(noteId));
        getContentResolver().update(noteUri, values, null, null);

        requery();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        requery();
        updateCategoryColors();
    }
}