# FFI 与 unsafe

## 目标

这一课只处理 C 边界的基本形状。你会声明 `foreign` 函数，理解为什么调用要放进 `unsafe`，并学会把不安全边界缩小到一个薄层。

## 声明外部函数

调用 C 函数时，在仓颉中写 `foreign` 声明：

```cj
foreign func rand(): Int32
foreign func printf(fmt: CString, ...): Int32
```

`foreign` 只写声明，不写函数体。参数和返回值要符合 C 与仓颉之间的类型映射。变长参数只能放在参数列表最后。

## 调用要放在 `unsafe` 中

```cj
foreign func rand(): Int32

main() {
    let value = unsafe { rand() }
    println("随机数：${value}")
}
```

`unsafe` 不是装饰，它是在提醒调用者：这里跨出了仓颉正常安全边界。C 函数可能依赖指针、手动内存、调用约定和外部库状态。

## 把边界包起来

不要让业务代码到处写 `unsafe`。把它包进很小的函数：

```cj
foreign func rand(): Int32

func nextRandom(): Int32 {
    unsafe { rand() }
}

main() {
    println(nextRandom())
}
```

以后要替换随机实现、处理错误、限制范围，都集中在 `nextRandom`。

## 字符串和内存要成对处理

调用需要 C 字符串的函数时，分配和释放必须配对：

```cj
foreign func printf(fmt: CString, ...): Int32

main() {
    unsafe {
        var fmt = LibC.mallocCString("task %d\n")
        printf(fmt, 1)
        LibC.free(fmt)
    }
}
```

这类代码只应该出现在边界层。不要把 C 字符串指针传到任务模型或仓库对象里。

## 任务本中的位置

如果任务本需要调用 C 库，建议新增一个包：

- `taskbook.native`：只放 `foreign` 声明和 `unsafe` 包装函数。
- `taskbook.model`：不导入 native 包。
- `taskbook.store`：不直接持有 C 指针。
- `main.cj`：只调用安全包装后的函数。

依赖方向应该从应用层指向边界层，而不是让边界类型污染业务模型。

## 检查点

确认你能解释：

- `foreign` 函数为什么不能有函数体？
- 为什么调用 `foreign` 函数要写 `unsafe`？
- 为什么 `unsafe` 应该集中到很小的函数里？
- C 字符串分配和释放为什么必须配对？

## 练习

写一个 `nativeRandomBelow(limit: Int32): Int32` 包装函数。函数内部调用 `rand()`，入口只调用这个包装函数，不直接写 `unsafe`。
