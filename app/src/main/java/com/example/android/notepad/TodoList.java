package com.example.android.notepad;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TodoList extends AppCompatActivity {

    private LinearLayout todoListContainer;
    private LinearLayout incompleteTodosContainer;
    private LinearLayout completedTodosContainer;
    private List<Map<String, Object>> todoItems;
    private int selectedColor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.todo_list);

        todoListContainer = (LinearLayout) findViewById(R.id.todo_list_container);
        incompleteTodosContainer = (LinearLayout) findViewById(R.id.incomplete_todos);
        completedTodosContainer = (LinearLayout) findViewById(R.id.completed_todos);

        todoItems = new ArrayList<>();

        selectedColor = getResources().getColor(R.color.red);

        loadTodoItems();

        findViewById(R.id.add_todo_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddTodoDialog();
            }
        });

        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void showAddTodoDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.add_todo_dialog, null);
        builder.setView(dialogView);

        final EditText todoContent = (EditText) dialogView.findViewById(R.id.todo_content);
        final ImageView colorRed = (ImageView) dialogView.findViewById(R.id.color_red);
        final ImageView colorBlue = (ImageView) dialogView.findViewById(R.id.color_blue);
        final ImageView colorGreen = (ImageView) dialogView.findViewById(R.id.color_green);
        final ImageView colorYellow = (ImageView) dialogView.findViewById(R.id.color_yellow);
        final ImageView colorPurple = (ImageView) dialogView.findViewById(R.id.color_purple);
        Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);
        Button confirmButton = (Button) dialogView.findViewById(R.id.confirm_button);

        colorRed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.red);
                resetColorSelection(colorRed, colorBlue, colorGreen, colorYellow, colorPurple);
                colorRed.setBackgroundResource(R.drawable.color_picker_item_selected);
            }
        });

        colorBlue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.blue);
                resetColorSelection(colorRed, colorBlue, colorGreen, colorYellow, colorPurple);
                colorBlue.setBackgroundResource(R.drawable.color_picker_item_selected);
            }
        });

        colorGreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.green);
                resetColorSelection(colorRed, colorBlue, colorGreen, colorYellow, colorPurple);
                colorGreen.setBackgroundResource(R.drawable.color_picker_item_selected);
            }
        });

        colorYellow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.yellow);
                resetColorSelection(colorRed, colorBlue, colorGreen, colorYellow, colorPurple);
                colorYellow.setBackgroundResource(R.drawable.color_picker_item_selected);
            }
        });

        colorPurple.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedColor = getResources().getColor(R.color.purple);
                resetColorSelection(colorRed, colorBlue, colorGreen, colorYellow, colorPurple);
                colorPurple.setBackgroundResource(R.drawable.color_picker_item_selected);
            }
        });

        final AlertDialog dialog = builder.create();

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = todoContent.getText().toString().trim();
                if (content.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "请输入待办事项内容", Toast.LENGTH_SHORT).show();
                    return;
                }

                addTodoItem(content, selectedColor);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void resetColorSelection(ImageView... colors) {
        for (ImageView color : colors) {
            color.setBackgroundResource(R.drawable.color_picker_item);
        }
    }

    private void addTodoItem(String content, int color) {
        Map<String, Object> todoItem = new HashMap<>();
        todoItem.put("id", System.currentTimeMillis());
        todoItem.put("content", content);
        todoItem.put("color", color);
        todoItem.put("completed", false);

        todoItems.add(todoItem);
        refreshTodoList();
        saveTodoItems();
    }

    private void refreshTodoList() {
        incompleteTodosContainer.removeAllViews();
        completedTodosContainer.removeAllViews();

        for (final Map<String, Object> todoItem : todoItems) {
            boolean completed = (boolean) todoItem.get("completed");
            View todoView = createTodoView(todoItem);

            if (completed) {
                completedTodosContainer.addView(todoView);
            } else {
                incompleteTodosContainer.addView(todoView);
            }
        }
    }

    private View createTodoView(final Map<String, Object> todoItem) {
        LinearLayout todoView = new LinearLayout(this);
        todoView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        todoView.setOrientation(LinearLayout.HORIZONTAL);
        todoView.setPadding(0, 12, 0, 12);
        todoView.setGravity(Gravity.CENTER_VERTICAL);

        final CheckBox checkBox = new CheckBox(this);
        checkBox.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        checkBox.setChecked((boolean) todoItem.get("completed"));

        final TextView textView = new TextView(this);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        ));
        textView.setText((String) todoItem.get("content"));
        textView.setTextSize(16);
        textView.setTextColor((int) todoItem.get("color"));
        textView.setPadding(8, 0, 0, 0);

        if ((boolean) todoItem.get("completed")) {
            textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            textView.setAlpha(0.6f);
        }

        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean completed = checkBox.isChecked();
                todoItem.put("completed", completed);

                if (completed) {
                    textView.setPaintFlags(textView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    textView.setAlpha(0.6f);
                } else {
                    textView.setPaintFlags(textView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                    textView.setAlpha(1.0f);
                }

                refreshTodoList();
                saveTodoItems();
            }
        });

        todoView.addView(checkBox);
        todoView.addView(textView);

        return todoView;
    }

    private void saveTodoItems() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("todo_item_count", todoItems.size());

        for (int i = 0; i < todoItems.size(); i++) {
            Map<String, Object> todoItem = todoItems.get(i);
            editor.putLong("todo_item_" + i + "_id", (long) todoItem.get("id"));
            editor.putString("todo_item_" + i + "_content", (String) todoItem.get("content"));
            editor.putInt("todo_item_" + i + "_color", (int) todoItem.get("color"));
            editor.putBoolean("todo_item_" + i + "_completed", (boolean) todoItem.get("completed"));
        }

        editor.apply();
    }

    private void loadTodoItems() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int itemCount = preferences.getInt("todo_item_count", 0);

        todoItems.clear();

        for (int i = 0; i < itemCount; i++) {
            Map<String, Object> todoItem = new HashMap<>();
            todoItem.put("id", preferences.getLong("todo_item_" + i + "_id", 0));
            todoItem.put("content", preferences.getString("todo_item_" + i + "_content", ""));
            todoItem.put("color", preferences.getInt("todo_item_" + i + "_color", getResources().getColor(R.color.red)));
            todoItem.put("completed", preferences.getBoolean("todo_item_" + i + "_completed", false));

            todoItems.add(todoItem);
        }

        refreshTodoList();
    }
}