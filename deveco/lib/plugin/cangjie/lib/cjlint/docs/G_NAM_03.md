G.NAM.03 接口、类、struct、enum 类型和 enum 构造器、类型别名、采用大驼峰命名

【级别】建议

【描述】

1. 类型定义通常是名词或名词短语，其中接口名还可以是形容词或形容词短语，都应采用大驼峰命名。
2. enum 构造器采用大驼峰命名风格。
3. 测试类命名时推荐以被测试类名开头，并以 Test 结尾。例如，HashTest 或 HashIntegrationTest。
4. 建议异常类加 `Exception`/`Error` 后缀。

【正例】

```cangjie
// 符合：类名使用大驼峰
class MarcoPolo {
    // CODE
}
// 符合：enum 类型和 enum 构造器使用大驼峰
enum ThreadState {
    New | Runnable | Blocked | Terminated
}

// 符合：接口名使用大驼峰
interface TaPromotable {
    // CODE
}

// 符合：类型别名使用大驼峰
type Point2D = (Float64, Float64)

// 符合：抽象类名使用大驼峰
abstract class AbstractAppContext {
    // CODE
}
```

【反例】

```cangjie
// 不符合：类名使用小驼峰
class marcoPolos {
    // CODE
}
// 不符合：enum 类型名使用小驼峰
enum timeUnit {
    Year | Month | Day | Hour
}
```

【例外场景】

在 UI 场景下，一些配置需要用 enum 的成员构造来实现，html 里的一些配置习惯用小驼峰，对于领域内有约定的场景，允许例外。