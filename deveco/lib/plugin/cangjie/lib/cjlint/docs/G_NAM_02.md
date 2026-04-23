G.NAM.02 源文件名采用全小写加下划线风格

【级别】建议

【描述】

- 文件名不采用驼峰的原因是：不同系统对文件名大小写处理不同（如 Windows 系统不区分大小写，但是 Unix/Linux、Mac 系统则默认区分）。
- 如果文件只包含一个包外部可见的顶层元素，那么选择该顶层元素的名称，以此命名。否则，选择能代表主要内容的元素名称作为文件名。源文件名称使用全小写加下划线风格。

【正例】

```cangjie
// my_class.cj
public class MyClass {
    // CODE
}
```

【反例】

```cangjie
// MyClass.cj  文件名不符合：使用了驼峰命名
public class MyClass {
    // CODE
}
```