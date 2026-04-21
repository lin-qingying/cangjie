G.CLS.01 override 父类函数时不要增加函数的可访问性

【级别】建议

【描述】

增加 override 函数的可访问性，子类将拥有比预期更大的访问权限。

【正例】

```cangjie
open class Base {
    protected open func f(a: Int64): Int64 {
        //do some sensitive operations
        return a
    }
}
class Sub <: Base {
    protected override func f(a: Int64): Int64 {
        return a + 1
    }
}
```

该正确示例中，子类覆写的基类 `f()` 函数与基类保持一致为 `protected`。

【反例】

```cangjie
open class Base {
    protected open func f(a: Int64): Int64 {
        return a
    }
}
class Sub <: Base {
    public override func f(a: Int64): Int64 {
        super.f(a)
        //do some sensitive operations
    }

    public func g(a: Int64): Int64 {
        super.f(a)  // 这种也算是增加可访问性。
    }
}
```

上面的错误代码中，子类 override 了基类的 `f()` 函数，并增加了函数的可访问性。基类 `Base` 定义的 `f()` 函数为 `protected` 的，子类 `Sub` 定义该函数为 `public` 的，从而增加了 `f()` 的访问性。因此，任何 `Sub` 的使用者都可以调用此函数。