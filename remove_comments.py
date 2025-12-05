import re

# 读取NoteEditor.java文件
with open('c:\\Users\\30280\\AndroidStudioProjects\\NotePad\\app\\src\\main\\java\\com\\example\\android\\notepad\\NoteEditor.java', 'r', encoding='utf-8') as file:
    content = file.read()

# 移除多行注释 /* ... */
content = re.sub(r'/*[\s\S]*?*/', '', content)

# 移除单行注释 // ...
content = re.sub(r'//.*', '', content)

# 移除多余的空行
content = re.sub(r'\n\s*\n', '\n', content)

# 移除行首的空格
content = re.sub(r'^\s+', '', content, flags=re.MULTILINE)

# 保存处理后的文件
with open('c:\\Users\\30280\\AndroidStudioProjects\\NotePad\\app\\src\\main\\java\\com\\example\\android\\notepad\\NoteEditor.java', 'w', encoding='utf-8') as file:
    file.write(content)

print("注释已删除")