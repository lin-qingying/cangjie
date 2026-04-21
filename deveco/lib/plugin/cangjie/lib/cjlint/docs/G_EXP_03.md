G.EXP.03 && 、 ||、? 和 ?? 操作符的右侧操作数不要修改程序状态

【级别】要求

【描述】

逻辑与（`&&`）、逻辑或（`||`）、问号操作符（`?`）和 coalescing（`??`）表达式中的右操作数是否被求值，取决于左操作数的求值结果，当左操作数的求值结果可以得到整个表达式的结果时，不会再计算右操作数的结果。如果右操作数可能修改程序状态，则不能确定该修改是否发生，因此，规定逻辑与、逻辑或、问号操作符和 coalescing 操作符的右操作数中不要修改程序状态。

这里修改程序状态主要指修改变量及其成员（如修改全局变量、取放锁）、进行 IO 操作（如读写文件，收发网络包）等。

【正例】

```cangjie
var count: Int64 = 0

func add(x: Int64): Int64 {
    count += x // 修改了全局变量
    return count
}

main(): Int64 {
    let isOk = false
    let num = 5
    if (isOk) {  // 使用显式的条件判断来区分操作是否被执行
        if (add(num) != 0) {
            return 0
        } else {
            return 1
        }
    } else {
        return 1
    }
}
```

【反例】

```cangjie
var count: Int64 = 0

func add(x: Int64): Int64 {
    count += x // 修改了全局变量
    return count
}

main(): Int64 {
    let isOk = false
    let num = 5
    if (isOk && (add(num) != 0)) {  // 不符合： && 的右操作数中修改了程序状态
        return 0
    } else {
        return 1
    }
}
```