G.FUN.02 禁止函数有未被使用的参数

【级别】要求

【描述】

未被使用的参数往往是因为设计发生了变动造成的，它可能导致传参时出现不正确的参数匹配。

【反例】

```cangjie
func logInfo(fileName: String, lineNo: Int64): Unit {
    println(fileName)
}
```

【例外场景】

回调函数和 interface 实现等情形，可以用`_`代替未被使用的参数。

```cangjie
interface I {
    func f(cfg: String) {
        println(cfg)
    }
}
 
class DefaultImpl <: I {
    func f(_: String) {
        println("default")
    }
}
```