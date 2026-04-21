G.NAM.04 函数名称应采用小驼峰命名

【级别】建议

【描述】

1. 函数名称采用小驼峰命名风格。例如，sendMessage 或 stopServer。

   格式如下：

   - 建议优先将 field 对外的接口实现成属性，而不是 getXXX/setXXX，会更简洁。
   - 布尔属性名建议加 is 或 has，例如：isEmpty。
   - 函数名称建议使用以下格式：has + 名词 / 形容词 ()、动词 ()、动词 + 宾语 ()。
   - 回调函数（callback）允许介词 + 动词形式命名，如: onCreate, onDestroy, toString 其中动词主要用在动作的对象自身上，如 document.print()。

2. 下划线可能出现在单元测试函数名称中，用于分隔名称的逻辑组件，每个组件都使用小驼峰命名法。例如，一种典型的模式是 `<methodUnderTest>_<state>`，又例如 `pop_emptyStack`，命名测试函数没有唯一的正确方法。

【正例】

```cangjie
// 符合：函数名使用小驼峰
func addExample(start: Int64, size: Int64) {
    return start + size
}

// 符合：函数名使用小驼峰
func printAdd(add: (Int64, Int64) -> Int64): Unit {
    println(add(1, 2))
}
```

【反例】

```cangjie
// 不符合：函数名使用大驼峰
func GenerateChildren(page: Int64) {
     println(page.toString())
}
```