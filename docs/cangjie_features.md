# 仓颉语言特性整理

> 基于官方文档 [仓颉编程语言开发指南 v1.1.0-beta.24](https://docs.cangjie-lang.cn/cjnative/user_manual/source_zh_cn/first_understanding/basic.html) 整理
> 面向编译器开发参考用途

---

## 目录

1. [语言定位与整体特点](#1-语言定位与整体特点)
2. [基本概念](#2-基本概念)
3. [基础数据类型](#3-基础数据类型)
4. [函数](#4-函数)
5. [结构体 struct](#5-结构体-struct)
6. [枚举与模式匹配](#6-枚举与模式匹配)
7. [类和接口](#7-类和接口)
8. [泛型](#8-泛型)
9. [扩展 extend](#9-扩展-extend)
10. [集合类型](#10-集合类型)
11. [包与可见性](#11-包与可见性)
12. [异常处理](#12-异常处理)
13. [并发编程](#13-并发编程)
14. [宏与元编程](#14-宏与元编程)
15. [反射与注解](#15-反射与注解)
16. [跨语言互操作](#16-跨语言互操作)
17. [与 Kotlin 的关键差异对照](#17-与-kotlin-的关键差异对照)
18. [函数深层特性](#18-函数深层特性)
19. [类型系统深层特性](#19-类型系统深层特性)
20. [接口深层特性](#20-接口深层特性)
21. [泛型约束深层细节](#21-泛型约束深层细节)
22. [子类型关系完整规则](#22-子类型关系完整规则)
23. [扩展深层规则](#23-扩展深层规则)
24. [作用域与变量遮蔽](#24-作用域与变量遮蔽)
25. [enum 深层特性](#25-enum-深层特性)
26. [模式匹配深层细节](#26-模式匹配深层细节)
27. [属性 prop 深层特性](#27-属性prop深层特性)
28. [泛型类型的子类型关系（不变性）](#28-泛型类型的子类型关系不变性)
29. [关键字完整列表](#29-关键字完整列表)
30. [类型转换完整规则](#30-类型转换完整规则)
31. [并发编程深层特性](#31-并发编程深层特性)
32. [宏系统深层特性](#32-宏系统深层特性)
33. [编译器实现关键注意事项](#33-编译器实现关键注意事项)

---

## 1. 语言定位与整体特点

仓颉是华为自研的面向全场景应用开发的通用编程语言，主要特点：

- **多范式**：支持函数式、命令式、面向对象
- **静态强类型** + **类型推断**
- **自动内存管理**（GC）+ 运行时安全检查
- **用户态轻量线程**（原生协程）
- **C 互操作**、Python 互操作
- **词法宏**元编程，支持 eDSL 构建
- **多后端**：CJNative（编译为原生二进制）、CJVM（字节码）

---

## 2. 基本概念

### 2.1 标识符

- 普通标识符：`XID_Start` 开头，后接 `XID_Continue`，支持中文
- 原始标识符：反引号包裹，可使用关键字作为标识符，如 `` `while` ``
- 行尾分号可省略

### 2.2 变量声明

```cangjie
let x: Int64 = 10      // 不可变
var y: Int64 = 20      // 可变
let z = 30             // 类型推断
```

### 2.3 表达式

- 仓颉中几乎所有构造都是表达式（包括 `if`、`match` 等）
- `if` 可作为表达式返回值
- Flow 表达式：管道风格链式调用

---

## 3. 基础数据类型

| 类型 | 说明 |
|------|------|
| `Int8/16/32/64`、`UInt8/16/32/64`、`IntNative`、`UIntNative` | 整数类型 |
| `Float32`、`Float64` | 浮点类型 |
| `Bool` | 布尔类型，`true` / `false` |
| `Rune` | 字符类型，Unicode 码点 |
| `String` | 字符串，支持插值 `"hello ${name}"` |
| `(T1, T2, ...)` | 元组类型 |
| `Array<T>` | 定长数组 |
| `Range<T>` | 区间类型，`0..10`（不含右端）、`0..=10`（含右端） |
| `Unit` | 无意义返回值，类似 `void` |
| `Nothing` | 永不返回，用于 `throw`、无限循环等 |

### 3.1 字符串插值

```cangjie
let name = "仓颉"
let s = "Hello, ${name}!"   // 插值表达式
let s2 = "1 + 1 = ${1 + 1}"
```

---

## 4. 函数

### 4.1 定义与调用

```cangjie
func add(a: Int64, b: Int64): Int64 {
    a + b   // 最后一个表达式作为返回值，return 可省略
}
```

### 4.2 命名参数与默认值

```cangjie
func zone(top!: Int64 = 0, left!: Int64 = 0, bottom!: Int64, right!: Int64) {}

zone(bottom: 10, right: 10)           // top/left 使用默认值
zone(right: 10, bottom: 10)           // 命名参数可乱序
```

### 4.3 函数作为一等公民

```cangjie
let f: (Int64, Int64) -> Int64 = add  // 函数类型
func apply(f: (Int64) -> Int64, x: Int64): Int64 { f(x) }
```

### 4.4 Lambda 表达式

```cangjie
let double = { x: Int64 => x * 2 }
let add = { a: Int64, b: Int64 => a + b }
```

### 4.5 尾随 Lambda（语法糖）

```cangjie
// lambda 是最后一个参数时可写在括号外
str.map { s => "Cangjie: " + s }

// 构建 eDSL 的核心机制
Column {
    Row {
        Text("hello")
    }
}
```

### 4.6 闭包

函数可以捕获外部变量，形成闭包。

### 4.7 嵌套函数

函数内部可以定义函数。

### 4.8 函数重载

同名函数参数类型不同即构成重载。

### 4.9 操作符重载

```cangjie
operator func +(other: MyType): MyType { ... }
```

### 4.10 const 函数与常量求值

```cangjie
const func factorial(n: Int64): Int64 { ... }
const val = factorial(5)   // 编译期求值
```

---

## 5. 结构体 struct

- **值类型**：赋值和传参时复制
- 不支持继承
- 支持实现接口
- `mut` 函数：允许修改 struct 实例的字段

```cangjie
struct Point {
    var x: Float64
    var y: Float64

    mut func move(dx: Float64, dy: Float64) {
        x += dx
        y += dy
    }
}
```

### 主构造函数

```cangjie
struct Rectangle {
    Rectangle(let width: Int64, let height: Int64) {}
}
```

---

## 6. 枚举与模式匹配

### 6.1 枚举（代数数据类型）

```cangjie
enum Shape {
    | Circle(Float64)
    | Rectangle(Float64, Float64)
    | Triangle
}
```

### 6.2 Option 类型

```cangjie
// 内置 Option<T>，等价于 Some(T) | None
let x: ?Int64 = Some(42)
let y: ?Int64 = None
```

### 6.3 match 表达式

```cangjie
match (shape) {
    case Circle(r) => 3.14 * r * r
    case Rectangle(w, h) => w * h
    case Triangle => 0.0
}
```

### 6.4 if-let 和 while-let

```cangjie
if let Some(v) = optVal {
    println(v)
}

while let Some(v) = iter.next() {
    println(v)
}
```

### 6.5 模式种类

- 常量模式：`case 0`
- 绑定模式：`case x`
- 元组模式：`case (a, b)`
- 类型模式：`case x: Int64`
- 枚举构造器模式
- 通配符：`case _`
- 守卫条件：`case x where x > 0`

---

## 7. 类和接口

### 7.1 class 定义

- **引用类型**：赋值传参不复制，多变量共享同一对象
- 支持继承（单继承）
- 默认不可继承，需加 `open` 才能被子类继承
- `class` 只能定义在源文件顶层

```cangjie
class Rectangle {
    let width: Int64
    let height: Int64

    public init(width: Int64, height: Int64) {
        this.width = width
        this.height = height
    }

    public func area(): Int64 {
        width * height
    }
}
```

### 7.2 访问修饰符

| 修饰符 | 可见范围 |
|--------|----------|
| `private` | 仅当前类定义内 |
| `internal`（默认） | 当前包及子包 |
| `protected` | 当前模块 + 子类 |
| `public` | 模块内外均可见 |

### 7.3 主构造函数

```cangjie
class Rectangle {
    public Rectangle(let width: Int64, let height: Int64) {}
}
```

### 7.4 静态成员

```cangjie
class Rectangle {
    static let defaultColor = "red"

    static func typeName(): String { "Rectangle" }
}

Rectangle.typeName()
```

### 7.5 继承

```cangjie
open class Animal {
    public open func speak(): Unit { println("...") }
}

class Dog <: Animal {
    public override func speak(): Unit { println("Woof") }
}
```

- 使用 `<:` 表示继承或实现
- 父类构造函数用 `super(args)` 调用，必须在构造函数第一行
- 覆盖实例函数：父类用 `open`，子类用 `override`
- 重定义静态函数：子类用 `redef`

### 7.6 sealed 抽象类

```cangjie
// 只能在本包内被继承
sealed abstract class Expr {}
class Num <: Expr {}
class Add <: Expr {}
```

### 7.7 abstract class

```cangjie
abstract class Shape {
    public func area(): Float64   // 抽象函数，无函数体
}
```

### 7.8 This 类型

```cangjie
open class Builder {
    func setName(name: String): This {
        // ...
        return this
    }
}
// 子类调用时返回类型自动为子类类型
```

### 7.9 终结器

```cangjie
class Resource {
    ~init() {
        // GC 时调用，用于释放资源
    }
}
```

### 7.10 接口

```cangjie
interface Drawable {
    func draw(): Unit
    func area(): Float64   // 可有默认实现
}

class Circle <: Drawable {
    public func draw(): Unit { ... }
    public func area(): Float64 { ... }
}
```

- 一个类可实现多个接口：`class A <: InterfaceB & InterfaceC`
- 接口可以继承其他接口

### 7.11 属性 prop

```cangjie
class Circle {
    private var _radius: Float64 = 0.0

    public prop radius: Float64 {
        get() { _radius }
        set(v) { _radius = v }
    }
}

// 只读属性
public prop area: Float64 {
    get() { 3.14 * _radius * _radius }
}
```

### 7.12 类型转换

```cangjie
let a: Animal = Dog()
let d = a as Dog        // 安全转换，返回 ?Dog
let d2 = a as! Dog      // 强制转换，失败时抛异常
```

---

## 8. 泛型

### 8.1 泛型函数

```cangjie
func identity<T>(x: T): T { x }
```

### 8.2 泛型类/接口/struct/enum

```cangjie
class Box<T> {
    let value: T
    init(value: T) { this.value = value }
}

interface Container<T> {
    func get(): T
}
```

### 8.3 泛型约束

```cangjie
func printAll<T>(items: Array<T>): Unit where T <: ToString {
    for (item in items) { println(item.toString()) }
}

// 多约束
func foo<T>(): Unit where T <: InterfaceA & InterfaceB { }
```

### 8.4 类型别名

```cangjie
type StringMap<V> = HashMap<String, V>
```

---

## 9. 扩展 extend

不修改原类型定义，为其添加新功能：

```cangjie
extend String {
    public func shout(): String {
        this.toUpperCase() + "!"
    }
}

"hello".shout()  // "HELLO!"
```

### 接口扩展

```cangjie
extend Int64 <: Printable {
    public func print(): Unit { println(this) }
}
```

---

## 10. 集合类型

| 类型 | 说明 |
|------|------|
| `ArrayList<T>` | 动态数组 |
| `HashSet<T>` | 哈希集合 |
| `HashMap<K, V>` | 哈希映射 |

均实现 `Iterable` 接口，支持 `for-in` 遍历：

```cangjie
for (item in list) { ... }
for ((k, v) in map) { ... }
```

---

## 11. 包与可见性

```cangjie
package com.example.myapp     // 声明包

import std.collection.*       // 导入
from std import collection.{ArrayList, HashMap}  // 选择导入
```

- 程序入口：`main()` 函数
- 顶层声明默认 `internal` 可见性
- 支持重导出：`public import`

---

## 12. 异常处理

```cangjie
class MyException <: Exception {
    init(msg: String) { super(msg) }
}

try {
    throw MyException("出错了")
} catch (e: MyException) {
    println(e.message)
} finally {
    println("清理")
}
```

- 使用 `Option<T>` 作为轻量错误处理替代方案
- 运行时自动检查：数组越界、类型转换失败、数值溢出

---

## 13. 并发编程

### 13.1 用户态轻量线程

```cangjie
let t = spawn {
    println("子线程执行")
}
t.join()
```

### 13.2 同步机制

```cangjie
let mutex = ReentrantMutex()
synchronized (mutex) {
    // 临界区
}
```

- 并发对象库：线程安全的数据结构，方法调用无需额外加锁
- 支持 `sleep(duration)`

---

## 14. 宏与元编程

### 14.1 词法宏

在编译时变换代码，基于 Token 操作：

```cangjie
macro myMacro(tokens: Tokens): Tokens {
    // 操作 AST Token，返回新 Token
    quote { println("macro generated") }
}

@myMacro
func foo() {}
```

### 14.2 quote 表达式

```cangjie
let code: Tokens = quote {
    let x = 1 + 2
}
```

### 14.3 内置编译标记

```cangjie
@Deprecated("请使用 newFunc 替代")
func oldFunc() {}
```

---

## 15. 反射与注解

### 15.1 注解

```cangjie
@Annotation
public struct MyAnnotation {
    let value: String
}

@MyAnnotation("hello")
class MyClass {}
```

### 15.2 动态特性

支持运行时类型信息查询（反射），可获取类型名、成员列表等。

---

## 16. 跨语言互操作

### 16.1 C 互操作

```cangjie
@C
func malloc(size: UIntNative): CPointer<Unit>

foreign func c_function(x: Int32): Int32

unsafe {
    let ptr = malloc(100)
}
```

### 16.2 Python 互操作

支持调用 Python 函数和访问 Python 对象。

---

## 17. 与 Kotlin 的关键差异对照

> 用于编写仓颉编译器（基于 Kotlin 编译器架构）时的快速参考

| 特性 | Kotlin | 仓颉 |
|------|--------|------|
| 继承语法 | `class B : A()` | `class B <: A` |
| 接口实现 | `class B : A, C` | `class B <: A & C` |
| 泛型约束 | `<T : Bound>` | `where T <: Bound` |
| 可空类型 | `String?` | `?String` 或 `Option<String>` |
| 属性 getter/setter | `get()` / `set(v)` | `get()` / `set(v)`（语法类似） |
| 密封类 | `sealed class`（子类任意文件） | `sealed abstract class`（只能本包继承） |
| 抽象类 | `abstract class` | `abstract class` |
| 不可继承 | 默认（`final`） | 默认（需 `open` 才可继承） |
| 伴生对象 | `companion object` | 无，使用 `static` 成员代替 |
| 扩展函数 | `fun Type.method()` | `extend Type { func method() }` |
| Lambda | `{ x -> x + 1 }` | `{ x: Int64 => x + 1 }` |
| 尾随 Lambda | `foo { }` | `foo { }`（相同） |
| 字符串插值 | `"hello $name"` | `"hello ${name}"` |
| 空值处理 | `null` / `?.` / `?:` | `None` / `Option` + `match` |
| 协程 | `suspend` + `coroutine` | 原生轻量线程（`spawn`） |
| context parameters | ✅ 有 | ❌ 无 |
| 隐式 this | ✅ 多层嵌套 | ✅ 当前类 this 可隐式 |
| 操作符重载 | `operator fun` | `operator func` |
| 主构造函数 | `class A(val x: Int)` | `class A { A(let x: Int64) {} }` |
| 静态函数重定义 | 无（用 `companion object`） | `redef static func` |
| 无原生 lazy | `by lazy { }` | 需手动用 `Option` 实现 |
| 终结器 | 无（用 `Closeable`） | `~init()` |

---

## 18. 函数深层特性

### 18.1 非命名参数 vs 命名参数

```cangjie
// 非命名参数：调用时不写参数名，按位置传入
func add(a: Int64, b: Int64): Int64 { a + b }
add(1, 2)

// 命名参数：参数名后加 !，调用时必须写参数名
func add(a!: Int64, b!: Int64): Int64 { a + b }
add(a: 1, b: 2)   // 命名参数可乱序
add(b: 2, a: 1)   // OK
```

关键规则：
- **只有命名参数才能有默认值**，非命名参数不能有默认值
- 非命名参数必须出现在命名参数**之前**（不能交叉）
- 函数参数是**不可变**的，函数体内不能对其赋值

### 18.2 函数体类型推导

函数体的类型由最后一个"项"决定：
- 最后一项是**表达式** → 函数体类型 = 该表达式类型
- 最后一项是**变量定义或函数声明**，或函数体为空 → 类型为 `Unit`

```cangjie
func add(a: Int64, b: Int64): Int64 {
    a + b   // 最后一项是表达式，返回类型推导为 Int64，return 可省略
}

func foo(): Unit {
    let s = "Hello"
    print(s)   // 最后一项是函数调用（Unit），return 可省略
}
```

### 18.3 return 表达式的类型

`return` 表达式本身的类型是 `Nothing`，而不是后面跟随表达式的类型。这意味着：

```cangjie
// return 可出现在任何需要 Nothing 类型的地方
let x: Int64 = if (cond) { 1 } else { return }
```

### 18.4 函数调用语法糖（尾随 Lambda）

当函数的最后一个参数是函数类型时，Lambda 可写在括号外：

```cangjie
// 标准写法
list.filter({ x => x > 0 })

// 尾随 Lambda 写法
list.filter { x => x > 0 }

// 若参数只有 Lambda，括号可省略
spawn { doSomething() }
```

这是仓颉 eDSL 能力的核心，UI 框架大量使用此特性：

```cangjie
Column {           // 等价于 Column({ ... })
    Row {
        Text("hi")
    }
}
```

### 18.5 变量遮蔽（Shadowing）

局部变量可以遮蔽外层同名变量，在局部作用域内使用的是内层变量：

```cangjie
let r = 0   // 全局变量

func add(a: Int64, b: Int64): Int64 {
    var r = 0   // 局部变量遮蔽全局 r
    r = a + b   // 这里操作的是局部 r
    return r
}
```

函数参数作用域从定义处到函数体结束，不能在同一作用域内重新定义同名参数：

```cangjie
func add(a: Int64, b: Int64): Int64 {
    var a_ = a // OK
    var b = b  // Error，重定义了参数 b
    return a
}
```

---

## 19. 类型系统深层特性

### 19.1 This 类型占位符

`This` 只能用作实例成员函数的返回类型，代指当前类的类型。子类调用时自动解析为子类类型：

```cangjie
open class Builder {
    public open func setName(name: String): This {
        // ...
        return this
    }
}

class ConcreteBuilder <: Builder {
    // 继承的 setName 返回类型自动变为 ConcreteBuilder，无需 override
}

let b: ConcreteBuilder = ConcreteBuilder().setName("foo")  // 类型正确
```

若函数体只返回 `this`，返回类型会被自动推断为 `This`。

### 19.2 Object 类型

所有 `class` 定义的类型都是 `Object` 的子类型（注意 `Object` 本身没有任何成员）：

```cangjie
class A {}
let obj: Object = A()  // OK
```

`Object` 和 `Any` 的区别：
- `Any`：所有类型（包括 `struct`、`enum`、基本类型）的父类型
- `Object`：仅 `class` 类型的父类型

### 19.3 Nothing 类型

`Nothing` 是所有类型的子类型，用于表示永不正常返回的表达式：

```cangjie
func fail(msg: String): Nothing {
    throw Exception(msg)   // throw 的类型就是 Nothing
}

// 可用于 if 表达式的分支对齐
let x: Int64 = if (cond) { 42 } else { fail("error") }
```

### 19.4 类的继承性修饰符完整规则

| 修饰符 | 含义 |
|--------|------|
| 无修饰符（默认） | 类不可被继承 |
| `open` | 类可被继承（本包和外包均可） |
| `abstract` | 抽象类，可被继承，不能直接实例化，`open` 可选 |
| `sealed abstract` | 只能在**本包**内被继承，蕴含 `public` 和 `open` 语义 |

`sealed` 的子类本身可以是 `open`、`sealed` 或不带修饰符，若子类是 `open` 则可在包外被继续继承。

### 19.5 覆盖与重定义的区别

| | 实例函数覆盖 | 静态函数重定义 |
|---|---|---|
| 父类修饰符 | `open` | 无特殊要求 |
| 子类修饰符 | `override`（可选） | `redef`（可选） |
| 派发方式 | **动态派发**（运行时类型决定） | **静态派发**（编译时类型决定） |

```cangjie
open class C {
    public static func foo(): Unit { println("C") }
}
class D <: C {
    public redef static func foo(): Unit { println("D") }
}
C.foo()  // "C"
D.foo()  // "D"
```

### 19.6 接口实现的协变返回类型

接口中成员函数返回类型是 `class` 类型时，实现函数可返回其子类型：

```cangjie
open class Base {}
class Sub <: Base {}

interface I {
    func f(): Base
}

class C <: I {
    public func f(): Sub { Sub() }   // OK，Sub <: Base
}
```

---

## 20. 接口深层特性

### 20.1 接口成员的访问控制

接口成员**默认且强制为 `public`**，不可声明其他访问修饰符。实现类型也必须用 `public` 实现：

```cangjie
interface I {
    func f(): Unit   // 隐式 public
}

class C <: I {
    protected func f() {}   // Error，必须是 public
}
```

### 20.2 静态接口成员与默认实现

接口中的静态成员可以有或没有默认实现：

```cangjie
interface NamedType {
    // 无默认实现：实现类必须提供，不能通过接口名直接调用
    static func typename(): String

    // 有默认实现：实现类可以不覆盖，可通过接口名或类名调用
    static func category(): String { "unknown" }
}
```

在泛型约束中使用静态接口成员时，要求所有静态成员都必须有实现（包括通过继承链）：

```cangjie
func printTypeName<T>() where T <: NamedType {
    println(T.typename())   // 要求 T 的具体类型必须实现 typename
}
```

### 20.3 多接口默认实现冲突

一个类同时实现多个接口，且多个接口有同名默认实现时，默认实现失效，类必须自己提供实现：

```cangjie
interface A { func say() { "A" } }
interface B { func say() { "B" } }

class C <: A & B {
    public func say() { "C" }   // 必须提供，否则编译错误
}
```

### 20.4 Any 类型

`Any` 是内置接口，所有接口默认继承它，所有非接口类型默认实现它：

```cangjie
interface Any {}  // 内置定义

var any: Any = 1
any = 2.0
any = "hello"   // 任意类型都可赋给 Any
```

### 20.5 sealed 接口

```cangjie
package A
sealed interface I {}   // 只能在 A 包内被继承、实现或扩展

// 包外
package B
import A.*
class C <: I {}   // Error，I 是 sealed 接口
```

### 20.6 接口继承规则细节

子接口继承父接口时：
- 父接口有**默认实现**的函数 → 子接口不能只写声明，必须给出新的默认实现（或不写）
- 父接口**无默认实现**的函数 → 子接口可以只写声明，也可以给出默认实现
- `override` / `redef` 修饰符在两种情况下都是可选的

---

## 21. 泛型约束深层细节

### 21.1 约束种类

```cangjie
// 接口约束
func print<T>(a: T) where T <: ToString { println(a) }

// 子类型约束（约束为具体类型）
class Zoo<T> where T <: Animal { }

// 多约束（& 连接）
func foo<T>() where T <: InterfaceA & InterfaceB { }

// 多类型变元约束
func bar<T1, T2>() where T1 <: A, T2 <: B { }
```

### 21.2 覆盖/重定义时的约束规则

子类型覆盖或重定义泛型函数时，类型变元约束只能**更宽松或相同**，不能更严格：

```cangjie
open class Base {
    static func f<T>(): Unit where T <: B {}  // B 是某个类
}

class Sub <: Base {
    redef static func f<T>(): Unit where T <: C {}  // Error，C <: B，约束更严格
    redef static func f<T>(): Unit where T <: A {}  // OK，A 是 B 的父类，约束更宽松
}
```

### 21.3 无约束泛型的限制

没有约束的泛型形参 `T`，只能做值传递，无法调用任何成员：

```cangjie
func id<T>(a: T): T { a }         // OK，只是返回值
func bad<T>(a: T) { a.toString() } // Error，T 未约束 ToString
```

---

## 22. 子类型关系完整规则

### 22.1 基本子类型关系来源

| 来源 | 示例 |
|------|------|
| 类继承 | `class Sub <: Super` → `Sub <: Super` |
| 接口实现 | `class C <: I` → `C <: I` |
| 接口继承 | `interface I2 <: I1` → `I2 <: I1` |
| 扩展实现 | `extend Int64 <: I` → `Int64 <: I` |
| 传递性 | `A <: B`，`B <: C` → `A <: C` |

### 22.2 内置永远成立的子类型关系

```
T <: T                 // 自反性
Nothing <: T           // Nothing 是所有类型的子类型
T <: Any               // 任意类型都是 Any 的子类型
class C {} → C <: Object  // 所有 class 是 Object 的子类型
```

### 22.3 元组类型的子类型关系（协变）

元组的子类型关系对每个位置协变：

```cangjie
open class C1 {}; class C2 <: C1 {}
open class C3 {}; class C4 <: C3 {}

// C2 <: C1，C4 <: C3，因此：
let t: (C1, C3) = (C2(), C4())   // OK
```

### 22.4 函数类型的子类型关系（参数逆变，返回值协变）

`(U1) -> S2 <: (U2) -> S1` 当且仅当 `U2 <: U1`（**参数逆变**）且 `S2 <: S1`（**返回值协变**）：

```cangjie
// U2 <: U1，S2 <: S1
func f(a: U1): S2 { S2() }
func g(a: U2): S1 { S1() }

// f 的类型 (U1)->S2 是 g 的类型 (U2)->S1 的子类型
// 所以需要 (U2)->S1 的地方可以传入 f
func h(lam: (U2) -> S1): S1 { lam(U2()) }
h(f)   // OK
```

**直觉解释**：
- 返回值协变：f 产生更具体的结果，调用方当然可以接受
- 参数逆变：f 接受更宽泛的参数类型，当调用方传来 U2 时，f 也能处理

---

## 23. 扩展深层规则

### 23.1 直接扩展

不新增接口，直接为已有类型添加成员：

```cangjie
extend Int64 {
    public func isEven(): Bool { this % 2 == 0 }
}
42.isEven()   // true
```

### 23.2 接口扩展

为已有类型补充实现某个接口：

```cangjie
interface Printable {
    func print(): Unit
}

extend String <: Printable {
    public func print(): Unit { println(this) }
}
```

### 23.3 扩展的访问规则

- 扩展中**只能访问类型的 `public` 和 `internal` 成员**（不能访问 `private`）
- 扩展内定义的成员默认 `internal` 可见性
- 扩展不能定义构造函数、静态成员变量
- 扩展不能为 `class` 添加实例成员变量
- 同一个类型可以在多处被扩展，各处扩展互不影响

### 23.4 跨包扩展与 sealed 的限制

`sealed` 类型（class 或 interface）只能在定义所在包内被扩展：

```cangjie
package A
public sealed interface I {}

package B
import A.*
extend String <: I {}   // Error，I 是 sealed
```

---

## 24. 作用域与变量遮蔽

### 24.1 作用域规则

- 函数参数作用域：从定义处到函数体结束
- 局部变量作用域：从定义处到当前块结束
- 不能在同一作用域内重定义同名变量（但可以在内层作用域遮蔽外层）

### 24.2 遮蔽规则

内层作用域的变量会遮蔽外层同名变量，遮蔽范围只在内层块内有效：

```cangjie
let x = 1
func foo(): Int64 {
    let x = 2     // 遮蔽外层 x
    return x      // 返回 2
}
// 这里 x 仍然是 1
```

### 24.3 命名规范（编译器关心的细节）

- 类型名、接口名：`PascalCase`
- 变量名、函数名：`camelCase`
- 常量：`UPPER_SNAKE_CASE`（惯例）
- 支持中文标识符（基于 Unicode XID 标准）

---

## 25. enum 深层特性

### 25.1 同名构造器重载

同一 `enum` 中允许定义多个同名构造器，但参数**个数必须不同**（无参视为参数个数为 0）：

```cangjie
enum RGBColor {
    | Red              // 无参
    | Red(UInt8)       // 1 个参数
    // Red(UInt8, UInt8) // 2 个参数也可以
}
```

### 25.2 递归 enum（代数数据类型）

`enum` 构造器的参数可以递归引用自身类型，这是表达 AST 节点的核心机制：

```cangjie
enum Expr {
    | Num(Int64)
    | Add(Expr, Expr)
    | Sub(Expr, Expr)
    | Mul(Expr, Expr)
}

// 求值函数
func eval(e: Expr): Int64 {
    match (e) {
        case Num(n)    => n
        case Add(l, r) => eval(l) + eval(r)
        case Sub(l, r) => eval(l) - eval(r)
        case Mul(l, r) => eval(l) * eval(r)
    }
}
```

> **注意**：`enum` 和 `struct` 互递归，且 `enum` 作为 `Option` 的类型参数时可能有编译错误。

### 25.3 构造器名称解析规则

省略类型名时，构造器名可能与变量名、函数名、类名冲突，冲突时**必须加类型名**：

```cangjie
let Red = 1              // 变量名 Red
func Green(g: UInt8) {}  // 函数名 Green
class Blue {}            // 类名 Blue

enum RGBColor { | Red | Green(UInt8) | Blue(UInt8) }

let r = Red              // 选择变量 Red，不是构造器！
let r2 = RGBColor.Red    // OK：加类型名消除歧义
let b = Blue(100)        // 尝试调用 class Blue 的构造函数，报错
let b2 = RGBColor.Blue(100) // OK
```

### 25.4 enum 可定义成员函数和属性

```cangjie
enum Direction {
    | North | South | East | West

    public func opposite(): Direction {
        match (this) {
            case North => South
            case South => North
            case East  => West
            case West  => East
        }
    }

    public prop isVertical: Bool {
        get() { this == North || this == South }
    }
}
```

构造器名不能与成员函数/属性名重名。

---

## 26. 模式匹配深层细节

### 26.1 模式种类完整列表

| 模式类型 | 语法示例 | 说明 |
|----------|----------|------|
| 通配符模式 | `case _` | 匹配任意值，不绑定 |
| 绑定模式 | `case x` | 匹配并绑定到变量 x |
| 常量模式 | `case 0`、`case "hi"`、`case true` | 匹配具体字面量 |
| 枚举构造器模式 | `case Some(x)`、`case Add(l, r)` | 解构枚举 |
| 元组模式 | `case (a, b)` | 解构元组 |
| 类型模式 | `case x: String` | 类型检查并绑定 |
| 守卫（where） | `case x where x > 0` | 附加条件过滤 |

### 26.2 Refutability（可反驳性）

- **不可反驳模式（Irrefutable）**：总是匹配成功，如绑定模式 `x`、通配符 `_`、无参枚举构造器（当只有一个构造器时）。可用于 `let` 解构。
- **可反驳模式（Refutable）**：可能匹配失败，如常量模式、枚举构造器（多个时）、类型模式。只能用于 `match`、`if-let`、`while-let`。

```cangjie
// 不可反驳：let 解构元组
let (x, y) = (1, 2)   // OK

// 可反驳：必须用 if-let 或 match
if let Some(v) = optVal { ... }   // OK
let Some(v) = optVal              // Error，可反驳模式不能用于 let
```

### 26.3 match 穷举性检查

`match` 表达式要求分支**必须穷举所有可能**（编译期检查）：

```cangjie
enum Color { | Red | Green | Blue }

match (c) {
    case Red   => 0
    case Green => 1
    // Error：未覆盖 Blue
}

// 用 _ 兜底
match (c) {
    case Red => 0
    case _   => -1   // 覆盖其余所有情况
}
```

### 26.4 match 作为表达式

`match` 是表达式，有返回值，所有分支类型必须一致（或为子类型关系）：

```cangjie
let name: String = match (color) {
    case Red   => "red"
    case Green => "green"
    case Blue  => "blue"
}
```

### 26.5 嵌套模式

模式可以任意深度嵌套：

```cangjie
match (expr) {
    case Add(Num(0), r)  => r           // 加 0 优化
    case Add(l, Num(0))  => l
    case Add(Num(a), Num(b)) => Num(a + b)  // 常量折叠
    case _ => expr
}
```

### 26.6 其他可用模式的场合

- `for` 循环的变量绑定
- 函数参数（解构元组参数）
- `let` 语句（不可反驳模式）

```cangjie
// for 中解构元组
for ((k, v) in map) { println("${k}: ${v}") }

// let 解构元组
let (a, b, c) = (1, 2, 3)
```

---

## 27. 属性（prop）深层特性

### 27.1 实例属性与静态属性

```cangjie
class Circle {
    private var _radius: Float64

    // 实例属性（读写）
    public mut prop radius: Float64 {
        get() { _radius }
        set(v) {
            if (v < 0.0) { throw Exception("负半径") }
            _radius = v
        }
    }

    // 实例属性（只读）
    public prop area: Float64 {
        get() { 3.14159 * _radius * _radius }
    }

    // 静态属性
    public static prop defaultRadius: Float64 {
        get() { 1.0 }
    }
}
```

### 27.2 接口中的属性

接口中的属性声明必须指定是否可变（`mut`），实现类型必须保持一致：

```cangjie
interface Shape {
    prop area: Float64 { get() }          // 只读属性
    mut prop name: String { get(); set() } // 读写属性
}
```

### 27.3 属性 vs 成员变量

| | 成员变量（`var`/`let`） | 属性（`prop`） |
|---|---|---|
| 存储 | 直接存储值 | 通过 getter/setter 计算 |
| 接口声明 | 不能在接口中声明 | 可以在接口中声明 |
| 继承/覆盖 | 不可覆盖 | 可以 `override` |
| 计算逻辑 | 无 | getter/setter 中可有任意逻辑 |

### 27.4 属性的 override

子类可以覆盖父类或接口的属性：

```cangjie
open class Base {
    public open prop value: Int64 { get() { 0 } }
}

class Sub <: Base {
    public override prop value: Int64 { get() { 42 } }
}
```

---

## 28. 泛型类型的子类型关系（不变性）

### 28.1 泛型类型默认不变（Invariant）

`Box<Sub>` **不是** `Box<Super>` 的子类型，即使 `Sub <: Super`：

```cangjie
open class Animal {}
class Dog <: Animal {}

class Box<T> { var value: T }

let dogBox: Box<Dog> = Box<Dog>()
let animalBox: Box<Animal> = dogBox   // Error！泛型类型不变
```

**原因**：若允许协变，则可以通过 `animalBox` 写入非 `Dog` 类型的值，破坏类型安全。

### 28.2 通过泛型约束实现"有限协变"

```cangjie
// 通过约束在函数参数上实现协变效果
func processAll<T>(box: Box<T>) where T <: Animal {
    // 只读，安全
}

processAll(Box<Dog>())   // OK，Dog <: Animal 满足约束
```

### 28.3 接口泛型的子类型关系

若类 `C` 实现了 `Interface<T>`，则 `C <: Interface<T>`，但 `Interface<Sub>` 不是 `Interface<Super>` 的子类型（同样不变）。

---

## 29. 关键字完整列表

仓颉的保留关键字，在编译器实现中需要作为 token 种类处理：

### 29.1 声明类关键字
```
abstract    class       enum        extend      func
init        interface   let         macro       mut
operator    package     prop        redef       sealed
static      struct      type        var
```

### 29.2 控制流关键字
```
break       case        catch       continue    do
else        finally     for         if          in
match       return      throw       try         while
```

### 29.3 修饰符关键字
```
foreign     internal    open        override    private
protected   public      unsafe
```

### 29.4 表达式/类型关键字
```
as          false       import      is          Nothing
super       this        This        true        Unit
where
```

### 29.5 原始标识符

用反引号包裹可将关键字用作标识符：
```cangjie
let `class` = "这是个变量名"
let `while` = true
```

---

## 30. 类型转换完整规则

### 30.1 `as` 安全转换（返回 Option）

```cangjie
let a: Animal = Dog()
let d: ?Dog = a as Dog    // 成功返回 Some(dog)，失败返回 None

if let Some(dog) = a as Dog {
    dog.bark()
}
```

### 30.2 `as!` 强制转换（失败抛异常）

```cangjie
let d: Dog = a as! Dog    // 转换失败时抛 ClassCastException
```

### 30.3 `is` 类型检查

```cangjie
if a is Dog {
    println("是 Dog")
}
```

### 30.4 跨扩展类型转换的限制

跨包的扩展实现接口后，`is`/`as` 判断可能失效：

```cangjie
// 包 A
class Foo {}
func get(): Any { Foo() }

// 包 B
interface I {}
extend Foo <: I {}   // 跨包扩展

let v: Any = get()
println(v is I)      // 结果不确定，暂不支持此场景
```

---

## 31. 并发编程深层特性

### 31.1 用户态轻量线程（原生协程）

仓颉的线程是用户态调度的轻量线程，创建成本远低于系统线程：

```cangjie
// 创建线程，返回 Thread 对象
let t: Thread<Int64> = spawn { 42 }

// 等待结果
let result: Int64 = t.get()
```

### 31.2 线程间通信

```cangjie
// Channel：类型安全的线程间通信
let ch = Channel<Int64>(capacity: 10)

spawn { ch.send(42) }
let val = ch.receive()
```

### 31.3 同步原语

```cangjie
// 互斥锁
let mutex = ReentrantMutex()
synchronized (mutex) {
    // 临界区，自动加锁解锁
}

// 原子操作（并发对象）
// 带有 @Concurrent 标注的 class 方法调用是线程安全的
```

### 31.4 并发对象

使用 `@Concurrent` 标注的类，其所有公开方法调用自动加锁，无需手动同步：

```cangjie
@Concurrent
class Counter {
    private var count = 0
    public func increment() { count++ }
    public func get(): Int64 { count }
}
```

---

## 32. 宏系统深层特性

### 32.1 宏的两种形式

```cangjie
// 属性宏：作用于声明
@MyMacro
class Foo {}

// 表达式宏：在表达式位置使用
let x = myMacro!(someExpr)
```

### 32.2 quote 与 Token 操作

宏在编译期操作 Token 流，`quote {}` 构造 Token 序列，`$(expr)` 在 quote 中插值：

```cangjie
macro addLogging(input: Tokens): Tokens {
    let funcName = getFuncName(input)
    return quote {
        $(input)
        println("called: " + $(funcName))
    }
}
```

### 32.3 语法节点 API

宏可以将 Token 解析为结构化的语法节点进行操作：

```cangjie
macro transform(input: Tokens): Tokens {
    let decl = parseDecl(input)   // 解析为 FuncDecl 等节点
    // 检查、修改语法节点
    return decl.toTokens()
}
```

### 32.4 内置编译标记

| 标记 | 作用 |
|------|------|
| `@Deprecated(msg)` | 标记废弃，使用时产生警告 |
| `@Since(version)` | 标记 API 引入版本 |
| `@Concurrent` | 标注并发安全类 |
| `@C` | 标注 C 互操作函数/类型 |
| `@FastNative` | 标注高频 C 调用，减少跨语言开销 |
| `@Intrinsic` | 标注编译器内建实现 |

---

## 33. 编译器实现关键注意事项

> 本节专为基于 Kotlin 编译器架构实现仓颉编译器时的特别备忘

### 33.1 名称解析特殊规则

- 枚举构造器与普通名称的优先级：**变量/函数/类型名优先于枚举构造器**，冲突时必须加类型名限定
- `This` 类型只在实例成员函数返回类型位置有效，其他位置非法
- 命名参数（`p!`）和非命名参数的混合顺序：非命名在前，命名在后，不可交叉

### 33.2 类型检查关键规则

- 泛型类型**不变**，无协变/逆变标注机制（不像 Kotlin 的 `out`/`in`）
- 函数类型**参数逆变、返回值协变**（与 Kotlin 相同）
- 元组类型**每个位置协变**
- 接口实现函数返回类型允许是声明类型的**子类型**（协变返回）

### 33.3 sealed 的作用域

`sealed class` 和 `sealed interface` 的继承/实现/扩展**只能在定义所在的包内**，跨包操作均报错。这与 Kotlin 的 `sealed`（同文件内）不同——仓颉是**同包**粒度。

### 33.4 没有的特性（与 Kotlin 的关键缺失）

| Kotlin 特性 | 仓颉是否有 | 备注 |
|---|---|---|
| `context parameters` | ❌ | 核心差异，编译器架构需裁剪 |
| 协变/逆变标注（`out T`/`in T`） | ❌ | 泛型只有不变 |
| `companion object` | ❌ | 用 `static` 成员替代 |
| `data class` | ❌ | 需手动实现 `==`、`toString` 等 |
| `inline` 函数 | ❌ | 暂无 |
| `tailrec` | ❌ | 暂无尾递归优化标注 |
| `by lazy {}` | ❌ | 手动用 `Option` 实现 |
| `delegation（by）` | ❌ | 无委托语法 |
| `object` 单例 | ❌ | 用 `static` 成员或全局变量替代 |
| `when` 表达式 | ✅ | 对应仓颉的 `match` |
| `is`/`as` | ✅ | 语法相同 |
| `operator fun` | ✅ | 对应 `operator func` |