G.PKG.01 避免在 import 声明中使用通配符 `*`

【级别】建议

【描述】

使用 `import xxx.*` 会导致如下问题：

- 代码可读性问题：开发者难以从代码中清楚地看到当前包依赖其它包的哪些声明，也很难看出一个声明导入自哪个包；

- 形成意外的重载。

【正例】

```cangjie
// test1.cj
package test1

public open class Base {
    ...
}

public class Sub <: Base {
    ...
}

public func f(a: Base) {
    ...
}

//file test2.cj
package test2
import test1.Sub

class Basa {
    var m = Sub()
}

func f(a: Basa) {
    ...
}

main() {
    f(Base()) // Error，误将 Basa 写成了 Base，会编译报错
}
```

【反例】

```cangjie
// test1.cj
package test1

public open class Base {
    ...
}

public class Sub <: Base {
    ...
}

public func f(a: Base) {
    ...
}

//file test2.cj
package test2
import test1.*

class Basa {
    var m = Sub()
}

func f(a: Basa) {
    ...
}

main() {
    f(Base()) // Miswriting Basa as Base, but no compiler error.
}
```