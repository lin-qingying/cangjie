## 新增需求

### 需求:CFIR 渲染器必须支持可组合架构
CFIR 渲染器必须提供可组合的组件化渲染能力，至少覆盖声明渲染、类型渲染与引用渲染，并且必须允许通过配置组合不同渲染策略，而不是将全部策略硬编码在单一实现中。

#### 场景:通过 profile 选择渲染策略
- **当** 调用方为同一 CFIR 元素选择 golden-compat 与 readability 两种 profile 进行渲染
- **那么** 系统必须生成两份语义一致但格式策略可区分的输出文本

### 需求:CFIR 渲染器必须提供稳定 profile 工厂
系统必须提供统一 `withXxx()` 命名风格的 profile 工厂，至少包含 `withGoldenCompat`、`withDebug`、`withReadability`，并且这些 profile 的语义必须在版本内保持稳定。

#### 场景:使用 withXxx 工厂创建 renderer
- **当** 调用方分别通过 `withGoldenCompat`、`withDebug`、`withReadability` 创建 renderer
- **那么** 系统必须返回可直接渲染 CFIR 元素的实例，并产生各自稳定的策略化输出

### 需求:CFIR 渲染器必须提供兼容默认入口
系统必须保留兼容的默认渲染入口，以确保既有调用方在不修改调用方式时仍可获得可用输出；该入口必须明确映射到一个稳定的兼容 profile。

#### 场景:历史调用方继续使用默认 render 入口
- **当** 现有测试夹具仍调用 `CfirRenderer.render(element)`
- **那么** 系统必须返回与兼容 profile 定义一致的稳定输出，且调用方无需修改即可通过编译

### 需求:默认入口与 golden 兼容 profile 输出必须一致
系统必须保证默认 `render` 入口与 `withGoldenCompat` profile 在同一输入下输出一致，用于保护历史 golden 基线。

#### 场景:比较默认入口与 withGoldenCompat 输出
- **当** 对同一 `CfirElement` 分别调用默认 `render` 与 `withGoldenCompat` renderer
- **那么** 两者输出必须字节级一致

## 修改需求

## 移除需求
