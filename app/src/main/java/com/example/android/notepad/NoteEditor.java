package com.example.android.notepad;

import static com.example.android.notepad.R.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class NoteEditor extends Activity {
    private static final String TAG = "NoteEditor";

    private static final String[] PROJECTION =
            new String[] {
                    NotePad.Notes._ID,
                    NotePad.Notes.COLUMN_NAME_TITLE,
                    NotePad.Notes.COLUMN_NAME_NOTE,
                    NotePad.Notes.COLUMN_NAME_CATEGORY_ID
            };

    private static final String ORIGINAL_CONTENT = "origContent";

    private static final int STATE_EDIT = 0;
    private static final int STATE_INSERT = 1;

    private int mState;
    private Uri mUri;
    private Cursor mCursor;
    private EditText mText;
    private EditText mTitleText;
    private String mOriginalContent;
    private String mOriginalTitle;
    private long mCategoryId = 1;
    private Button mCategoryButton;
    private String action;

    public static class LinedEditText extends EditText {
        private Rect mRect;
        private Paint mPaint;

        public LinedEditText(Context context, AttributeSet attrs) {
            super(context, attrs);
            mRect = new Rect();
            mPaint = new Paint();
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0x800000FF);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            int count = getLineCount();
            Rect r = mRect;
            Paint paint = mPaint;

            for (int i = 0; i < count; i++) {
                int baseline = getLineBounds(i, r);
                canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
            }

            super.onDraw(canvas);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();

        action = intent.getAction();

        if (Intent.ACTION_EDIT.equals(action)) {
            mState = STATE_EDIT;
            mUri = intent.getData();
        } else if (Intent.ACTION_INSERT.equals(action)
                || Intent.ACTION_PASTE.equals(action)) {
            mState = STATE_INSERT;
            mUri = getContentResolver().insert(intent.getData(), null);

            if (mUri == null) {
                Log.e(TAG, "Failed to insert new note into " + getIntent().getData());
                finish();
                return;
            }

            setResult(RESULT_OK, (new Intent()).setAction(mUri.toString()));

        } else {
            Log.e(TAG, "Unknown action, exiting");
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        setContentView(R.layout.note_editor);

        mText = (EditText) findViewById(R.id.note);
        mTitleText = (EditText) findViewById(R.id.note_title);

        mOriginalContent = null;
        mOriginalTitle = null;

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mCursor = managedQuery(
                mUri,
                PROJECTION,
                null,
                null,
                null
        );

        if (Intent.ACTION_PASTE.equals(action)) {
            performPaste();
            mState = STATE_EDIT;
        }

        if (mCursor != null) {
            mCursor.requery();

            mCursor.moveToFirst();

            if (mState == STATE_EDIT) {
                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                Resources res = getResources();
                String text = String.format(res.getString(R.string.title_edit), title);
                setTitle(text);
            } else if (mState == STATE_INSERT) {
                setTitle(getText(R.string.title_create));
            }

            int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
            String title = mCursor.getString(colTitleIndex);
            mTitleText.setTextKeepState(title);
            if (title != null && !title.isEmpty()) {
                mTitleText.setSelection(title.length());
            }

            int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
            String note = mCursor.getString(colNoteIndex);
            mText.setTextKeepState(note);

            int colCategoryIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_CATEGORY_ID);
            if (!mCursor.isNull(colCategoryIndex)) {
                mCategoryId = mCursor.getLong(colCategoryIndex);
            }

            if (mCategoryButton == null) {
                setupCategoryButton();
            }
            updateCategoryButtonText();

            if (mOriginalContent == null) {
                mOriginalContent = note;
            }
            if (mOriginalTitle == null) {
                mOriginalTitle = title;
            }

        } else {
            setTitle(getText(R.string.error_title));
            mText.setText(getText(R.string.error_message));
        }

        if (mCategoryButton != null) {
            updateCategoryButtonText();
        }

        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY_ID, mCategoryId);

        if (mUri != null && mState == STATE_EDIT) {
            getContentResolver().update(
                    mUri,
                    values,
                    null,
                    null
            );
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(ORIGINAL_CONTENT, mOriginalContent);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mCursor != null) {
            String text = mText.getText().toString();
            String title = mTitleText.getText().toString();
            int textLength = text.length();
            int titleLength = title.length();

            if (isFinishing() && (textLength == 0) && (titleLength == 0)) {
                setResult(RESULT_CANCELED);
                deleteNote();

            } else if (mState == STATE_EDIT) {
                updateNote(text, null);
            } else if (mState == STATE_INSERT) {
                updateNote(text, null);
                mState = STATE_EDIT;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.editor_options_menu, menu);

        if (mState == STATE_EDIT) {
            Intent intent = new Intent(null, mUri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
            menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                    new ComponentName(this, NoteEditor.class), null, intent, 0, null);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
        String savedNote = mCursor.getString(colNoteIndex);
        String currentNote = mText.getText().toString();
        if (savedNote.equals(currentNote)) {
            menu.findItem(R.id.menu_revert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_revert).setVisible(true);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id== R.id.menu_save) {
            String text = mText.getText().toString();
            String title = mTitleText.getText().toString();
            updateNote(text, title);
            finish();
        } else if (id == R.id.menu_delete) {
            deleteNote();
            finish();
        } else if (id == R.id.menu_revert) {
            cancelNote();
        }
        return super.onOptionsItemSelected(item);
    }

    private final void performPaste() {
        ClipboardManager clipboard = (ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);

        ContentResolver cr = getContentResolver();

        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null) {

            String text=null;
            String title=null;

            ClipData.Item item = clip.getItemAt(0);

            Uri uri = item.getUri();

            if (uri != null && NotePad.Notes.CONTENT_ITEM_TYPE.equals(cr.getType(uri))) {
                Cursor orig = cr.query(
                        uri,
                        PROJECTION,
                        null,
                        null,
                        null
                );

                if (orig != null) {
                    if (orig.moveToFirst()) {
                        int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                        int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                        text = orig.getString(colNoteIndex);
                        title = orig.getString(colTitleIndex);
                    }
                    orig.close();
                }
            }

            if (text == null) {
                text = item.coerceToText(this).toString();
            }

            updateNote(text, title);
        }
    }

    private final void updateNote(String text, String title) {
        ContentValues values = new ContentValues();
        values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY_ID, mCategoryId);

        if (title == null && mTitleText != null) {
            title = mTitleText.getText().toString();
        }

        if (mState == STATE_INSERT && (title == null || title.trim().isEmpty())) {
            int length = text.length();
            title = text.substring(0, Math.min(30, length));

            if (length > 30) {
                int lastSpace = title.lastIndexOf(' ');
                if (lastSpace > 0) {
                    title = title.substring(0, lastSpace);
                }
            }
        }

        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);

        values.put(NotePad.Notes.COLUMN_NAME_NOTE, text);

        getContentResolver().update(
                mUri,
                values,
                null,
                null
        );
    }

    private final void cancelNote() {
        if (mCursor != null) {
            if (mState == STATE_EDIT) {
                mCursor.requery();
                mCursor.moveToFirst();
                int colNoteIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_NOTE);
                String note = mCursor.getString(colNoteIndex);
                mText.setTextKeepState(note);

                int colTitleIndex = mCursor.getColumnIndex(NotePad.Notes.COLUMN_NAME_TITLE);
                String title = mCursor.getString(colTitleIndex);
                mTitleText.setTextKeepState(title);
            } else if (mState == STATE_INSERT) {
                setResult(RESULT_CANCELED);
                deleteNote();
                finish();
            }
        }
    }

    private final void deleteNote() {
        if (mUri != null) {
            getContentResolver().delete(
                    mUri,
                    null,
                    null
            );
        }
    }

    private void setupCategoryButton() {
        mCategoryButton = (Button) findViewById(R.id.category_button);
        if (mCategoryButton != null) {
            mCategoryButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCategoryDialog();
                }
            });
        }
    }

    private void updateCategoryButtonText() {
        if (mCategoryButton != null) {
            Cursor categoryCursor = getContentResolver().query(
                    NotePad.Categories.CONTENT_URI,
                    new String[]{NotePad.Categories._ID, NotePad.Categories.COLUMN_NAME_NAME},
                    NotePad.Categories._ID + " = ?",
                    new String[]{String.valueOf(mCategoryId)},
                    null
            );

            if (categoryCursor != null && categoryCursor.moveToFirst()) {
                int nameColumn = categoryCursor.getColumnIndex(NotePad.Categories.COLUMN_NAME_NAME);
                String categoryName = categoryCursor.getString(nameColumn);
                mCategoryButton.setText(categoryName);
                categoryCursor.close();
            } else {
                mCategoryButton.setText(R.string.uncategorized);
            }
        }
    }

    private void showCategoryDialog() {
        final Cursor categoryCursor = getContentResolver().query(
                NotePad.Categories.CONTENT_URI,
                new String[]{NotePad.Categories._ID, NotePad.Categories.COLUMN_NAME_NAME},
                null,
                null,
                NotePad.Categories.COLUMN_NAME_NAME
        );

        if (categoryCursor == null) {
            return;
        }

        final String[] categoryNames = new String[categoryCursor.getCount()];
        final long[] categoryIds = new long[categoryCursor.getCount()];
        int selectedIndex = 0;

        int i = 0;
        while (categoryCursor.moveToNext()) {
            int idColumn = categoryCursor.getColumnIndex(NotePad.Categories._ID);
            int nameColumn = categoryCursor.getColumnIndex(NotePad.Categories.COLUMN_NAME_NAME);

            categoryIds[i] = categoryCursor.getLong(idColumn);
            categoryNames[i] = categoryCursor.getString(nameColumn);

            if (categoryIds[i] == mCategoryId) {
                selectedIndex = i;
            }

            i++;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择分类")
                .setSingleChoiceItems(categoryNames, selectedIndex, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mCategoryId = categoryIds[which];
                        updateCategoryButtonText();

                        String title = mTitleText.getText().toString();
                        ContentValues values = new ContentValues();
                        values.put(NotePad.Notes.COLUMN_NAME_CATEGORY_ID, mCategoryId);
                        values.put(NotePad.Notes.COLUMN_NAME_TITLE, title);
                        getContentResolver().update(mUri, values, null, null);

                        dialog.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton(R.string.manage_categories, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(NoteEditor.this, CategoryManager.class);
                        startActivityForResult(intent, 1);
                    }
                });

        builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            updateCategoryButtonText();
        }
    }
}