G.ERR.03 避免对 Option 类型使用 getOrThrow 函数

【级别】建议

【描述】

仓颉使用 Option 类型来避免空指针问题，若对 Option 类型使用 getOrThrow 来获取其内容，容易导致忽略异常的处理，造成等同于空指针的效果。因此应尽量避免对 Option 类型使用 getOrThrow 函数。

【正例】

```cangjie
const DEFAULT_VALUE = 0

func getOne(dict: HashMap<String, Int64>, name: String): Int64 {
    return dict.get(name) ?? DEFAULT_VALUE
}
```

该正确示例中，在 Option 中值不存在的情况下提供了默认值，而不是使用 getOrThrow。

【反例】

```cangjie
func getOne(dict: HashMap<String, Int64>, name: String): Int64 {
    return dict.get(name).getOrThrow()
}
```

该错误示例没有考虑传入的名字可能不存在的情况，只使用了 getOrThrow 而没有处理异常。这是一种危险的编码风格，并不推荐。

【例外场景】

对于调用开源三方件，三方件中通过 getOrThrow 抛出 NoneValueException 异常时，可以捕获 NoneValueException，并对该异常进行处理。