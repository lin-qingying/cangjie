G.EXP.06 Bool 类型比较应避免多余的 == 或 !=

【级别】建议

【描述】

在 if 表达式、while、do while 表达式等使用到 Bool 类型表达式的位置，对于 Bool 类型的判断，应该避免多余的 == 或 !=。

【正例】

```cangjie
func isZero(x: Int64):Bool {
    return x == 0
}

main(): Int64 {
    var a = true
    var b = isZero(1)
    if (a && !b) {
        return 1
    } else {
        return 0
    }
}
```

【反例】

```cangjie
func isZero(x: Int64):Bool {
    return (x == 0) == true
}

main(): Int64 {
    var a = true
    var b = isZero(1)
    if (a == true && b != true) {
        return 1
    } else {
        return 0
    }
}
```