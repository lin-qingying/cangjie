G.EXP.07 比较两个表达式时，左侧倾向于变化，右侧倾向于不变

【级别】建议

【描述】

当可变变量与常量比较时，如果常量放左侧，如 `if (MAX == v)` 不符合阅读习惯，而 `if (MAX > v)` 更是难于理解。

应当按阅读、表达习惯，将常量放右侧。

【正例】

```cangjie
import std.collection.ArrayList

const MAX_LEN = 99999

func maxIndex(arr: ArrayList<Int>) {
    let len = arr.size
    if (len > MAX_LEN) {
        throw Exception("too long")
    } else {
        var i = 0
        var maxI = 0
        while (i < len) {
            if (arr[i] > arr[maxI]) {
                maxI = i
            }
            i++
        }
        return maxI
    }
}
```

【反例】

```cangjie
import std.collection.ArrayList

const MAX_LEN = 99999

func maxIndex(arr: ArrayList<Int>) {
    let len = arr.size
    // 不符合，常量在左，let 修饰的变量在右
    if (MAX_LEN < len) {
        throw Exception("too long")
    } else {
        var i = 0
        var maxI = 0
        // 不符合，let 修饰的变量在左，var 修饰的变量在右
        while (len > i) {
            if (arr[i] > arr[maxI]) {
                maxI = i
            }
            i++
        }
        return maxI
    }
}
```

【例外场景】

如使用 `if (MIN < a && a < MAX)` 用来描述区间时，前半段表达式中不可变变量在左侧也是允许的。