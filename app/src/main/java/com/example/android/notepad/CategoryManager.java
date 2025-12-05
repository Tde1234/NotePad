package com.example.android.notepad;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CategoryManager extends AppCompatActivity {
    private static final int MENU_ITEM_EDIT = 1;
    private static final int MENU_ITEM_DELETE = 2;

    private long mSelectedCategoryId = -1;

    private List<Map<String, Object>> mCategoryList = new ArrayList<>();
    private CategoryAdapter mAdapter;

    private static final String[] PRESET_COLORS = {
            "#FF5252", "#FF4081", "#E040FB", "#7C4DFF",
            "#536DFE", "#448AFF", "#40C4FF", "#18FFFF",
            "#64FFDA", "#69F0AE", "#B2FF59", "#EEFF41",
            "#FFFF00", "#FFD740", "#FFAB40", "#FF6E40",
            "#795548", "#9E9E9E", "#607D8B", "#ffffffff"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.category_manager);
        setTitle("分类管理");

        ListView listView = (ListView) findViewById(android.R.id.list);

        mAdapter = new CategoryAdapter();
        listView.setAdapter(mAdapter);

        registerForContextMenu(listView);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadCategories();
    }

    private void loadCategories() {
        mCategoryList.clear();

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(
                NotePad.Categories.CONTENT_URI,
                null,
                null,
                null,
                NotePad.Categories.DEFAULT_SORT_ORDER
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(NotePad.Categories._ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_NAME));
                String color = cursor.getString(cursor.getColumnIndexOrThrow(NotePad.Categories.COLUMN_NAME_COLOR));

                Map<String, Object> map = new HashMap<>();
                map.put("_id", id);
                map.put("name", name);
                map.put("color", color);
                mCategoryList.add(map);
            }
            cursor.close();
        }

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(Menu.NONE, Menu.FIRST, Menu.NONE, "添加分类");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Menu.FIRST) {
            showAddCategoryDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        mSelectedCategoryId = (Long) mCategoryList.get(info.position).get("_id");

        if (mSelectedCategoryId != 1) {
            menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, "编辑");
            menu.add(Menu.NONE, MENU_ITEM_DELETE, Menu.NONE, "删除");
        } else {
            menu.add(Menu.NONE, MENU_ITEM_EDIT, Menu.NONE, "修改颜色");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_ITEM_EDIT:
                showEditCategoryDialog(mSelectedCategoryId);
                return true;
            case MENU_ITEM_DELETE:
                confirmDeleteCategory(mSelectedCategoryId);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showAddCategoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加分类");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        final EditText nameEditText = (EditText) dialogView.findViewById(R.id.category_name);
        final FrameLayout colorPreview = (FrameLayout) dialogView.findViewById(R.id.color_preview);
        final ImageView colorPickerButton = (ImageView) dialogView.findViewById(R.id.color_picker_button);

        final String[] selectedColor = {"#000000"};
        colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));

        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog(selectedColor, colorPreview);
            }
        });

        builder.setView(dialogView);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = nameEditText.getText().toString().trim();
                if (TextUtils.isEmpty(name)) {
                    Toast.makeText(CategoryManager.this, "分类名称不能为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isCategoryNameExists(name, -1)) {
                    Toast.makeText(CategoryManager.this, "分类名称已存在", Toast.LENGTH_SHORT).show();
                    return;
                }

                addCategory(name, selectedColor[0]);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showEditCategoryDialog(final long categoryId) {
        Map<String, Object> category = null;
        for (Map<String, Object> map : mCategoryList) {
            if ((Long) map.get("_id") == categoryId) {
                category = map;
                break;
            }
        }

        if (category == null) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(categoryId == 1 ? "修改颜色" : "编辑分类");

        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        final EditText nameEditText = (EditText) dialogView.findViewById(R.id.category_name);
        final FrameLayout colorPreview = (FrameLayout) dialogView.findViewById(R.id.color_preview);
        final ImageView colorPickerButton = (ImageView) dialogView.findViewById(R.id.color_picker_button);

        String name = (String) category.get("name");
        final String[] selectedColor = {(String) category.get("color")};

        if (categoryId == 1) {
            nameEditText.setVisibility(View.GONE);
        } else {
            nameEditText.setText(name);
        }

        colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));

        colorPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showColorPickerDialog(selectedColor, colorPreview);
            }
        });

        builder.setView(dialogView);

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = name;

                if (categoryId != 1) {
                    newName = nameEditText.getText().toString().trim();
                    if (TextUtils.isEmpty(newName)) {
                        Toast.makeText(CategoryManager.this, "分类名称不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (isCategoryNameExists(newName, categoryId)) {
                        Toast.makeText(CategoryManager.this, "分类名称已存在", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }

                updateCategory(categoryId, newName, selectedColor[0]);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void showColorPickerDialog(final String[] selectedColor, final View colorPreview) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择颜色");

        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null);
        GridView colorGrid = (GridView) dialogView.findViewById(R.id.color_picker);
        colorGrid.setAdapter(new ColorAdapter());

        final ImageView colorPick = (ImageView) dialogView.findViewById(R.id.color_pick);
        if (colorPick != null) {
            colorPick.setBackgroundColor(Color.parseColor(selectedColor[0]));
        }

        colorGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedColor[0] = PRESET_COLORS[position];
                colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));

                if (colorPick != null) {
                    colorPick.setBackgroundColor(Color.parseColor(selectedColor[0]));
                }
            }
        });

        Button colorPickerButton = (Button) dialogView.findViewById(R.id.color_picker_button);
        if (colorPickerButton != null) {
            colorPickerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    colorPreview.setBackgroundColor(Color.parseColor(selectedColor[0]));
                    if (colorPick != null) {
                        colorPick.setBackgroundColor(Color.parseColor(selectedColor[0]));
                    }
                }
            });
        }

        final AlertDialog dialog = builder.create();

        dialog.setView(dialogView);

        Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        Button okButton = (Button) dialogView.findViewById(R.id.ok_button);
        if (okButton != null) {
            okButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        dialog.show();
    }

    private void confirmDeleteCategory(final long categoryId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认删除");
        builder.setMessage("确定要删除此分类吗？使用该分类的笔记将变为未分类。");

        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteCategory(categoryId);
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void addCategory(String name, String color) {
        ContentResolver cr = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(NotePad.Categories.COLUMN_NAME_NAME, name);
        values.put(NotePad.Categories.COLUMN_NAME_COLOR, color);

        cr.insert(NotePad.Categories.CONTENT_URI, values);

        loadCategories();
    }

    private void updateCategory(long id, String name, String color) {
        ContentResolver cr = getContentResolver();

        ContentValues values = new ContentValues();

        if (id != 1) {
            values.put(NotePad.Categories.COLUMN_NAME_NAME, name);
        }

        values.put(NotePad.Categories.COLUMN_NAME_COLOR, color);

        Uri uri = ContentUris.withAppendedId(NotePad.Categories.CONTENT_URI, id);

        cr.update(uri, values, null, null);

        loadCategories();
    }

    private void deleteCategory(long id) {
        ContentResolver cr = getContentResolver();

        Uri uri = ContentUris.withAppendedId(NotePad.Categories.CONTENT_URI, id);

        try {
            cr.delete(uri, null, null);

            loadCategories();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isCategoryNameExists(String name, long excludeId) {
        ContentResolver cr = getContentResolver();

        String selection = NotePad.Categories.COLUMN_NAME_NAME + " = ?";
        String[] selectionArgs = {name};

        if (excludeId > 0) {
            selection += " AND " + NotePad.Categories._ID + " != ?";
            selectionArgs = new String[]{name, String.valueOf(excludeId)};
        }

        Cursor cursor = cr.query(
                NotePad.Categories.CONTENT_URI,
                new String[]{NotePad.Categories._ID},
                selection,
                selectionArgs,
                null
        );

        boolean exists = cursor != null && cursor.getCount() > 0;
        if (cursor != null) {
            cursor.close();
        }

        return exists;
    }

    private class CategoryAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mCategoryList.size();
        }

        @Override
        public Object getItem(int position) {
            return mCategoryList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return (Long) mCategoryList.get(position).get("_id");
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(CategoryManager.this).inflate(
                        R.layout.category_list_item, parent, false);

                holder = new ViewHolder();
                holder.nameTextView = (TextView) convertView.findViewById(R.id.category_name);
                holder.colorIndicator = (FrameLayout) convertView.findViewById(R.id.category_color_indicator);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            Map<String, Object> category = mCategoryList.get(position);
            String name = (String) category.get("name");
            String color = (String) category.get("color");

            holder.nameTextView.setText(name);
            holder.colorIndicator.setBackgroundColor(Color.parseColor(color));

            return convertView;
        }

        private class ViewHolder {
            TextView nameTextView;
            FrameLayout colorIndicator;
        }
    }

    private class ColorAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return PRESET_COLORS.length;
        }

        @Override
        public Object getItem(int position) {
            return PRESET_COLORS[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = new View(CategoryManager.this);
                int size = (int) getResources().getDimension(R.dimen.color_item_size);
                ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(size, size);
                view.setLayoutParams(params);
                view.setPadding(4, 4, 4, 4);
            }

            view.setBackgroundColor(Color.parseColor(PRESET_COLORS[position]));
            return view;
        }
    }
}