G.OPR.01 尽量避免违反使用习惯的操作符重载

【级别】建议

【描述】

重载操作符时要有充分的理由，尽量避免改变操作符的原有使用习惯，例如使用 `+` 操作符来做减法运算，避免对基础类型重载已内置支持的操作符。

【正例】：

```cangjie
struct Point {
    Point(let x: Int64, let y: Int64) {
    }

    operator func +(rhs: Point): Point { // 符合：为 Point 重载加法操作符
        return Point(this.x + rhs.x, this.y + rhs.y)
    }
}
```

【反例】

```cangjie
struct Point {
    Point(let x: Int64, let y: Int64) {
    }

    // 不符合：为 Point 重载加法操作符，但其实成员间做的是减法操作
    operator func +(rhs: Point): Point {
        return Point(this.x - rhs.x, this.y - rhs.y)
    }
}

extend Int64 {
    operator func +(right: Float64) { // 不符合：对基础类型重载已内置支持的操作符
        // CODE
    }
}
```