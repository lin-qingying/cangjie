## 新增需求

### 需求:Q1 单一内容契约与可插拔输出
快速文档内容必须由一份契约描述，输出由 `DocumentationRenderer<R>` 选择。新增输出格式或新增宿主不得修改契约与内容构建代码。

#### 场景:新增输出格式
- **当** 需要第三种输出（如文档站点 DOM）
- **那么** 只需实现 `DocumentationRenderer<R>`；不得修改 `documentation/api`、`documentation/cdoc`、`documentation/resolve`

#### 场景:两端同源
- **当** 同一符号分别经 IDE 与 LSP 请求
- **那么** 两者必须消费同一个 `DocumentationContent`；任一端不得自行遍历 CDoc 标签决定章节或顺序

#### 场景:扩展范围限定
- **当** 需要接入第二种文档来源（注解文档、外部链接、Java 互操作）
- **那么** 本变更不承诺免改共享层；该场景必须另立变更，`resolve` 层允许被修改

### 需求:Q2 解析入口唯一
元素到内容的解析必须在 `DocumentationService` 内实现一次，返回显式的 `DocumentationResolution`，覆盖：普通引用与调用、宏表达式与宏生成声明的调用点回退、宏候选裁决、原生 enum 条目（ordinal）、绑定模式变量、函数类型参数、修饰符、轻量元素回溯、二级构造器回落、声明符号恢复。

#### 场景:宏生成声明
- **当** 光标位于宏调用、导航 PSI 落在宏生成声明上
- **那么** 必须用请求携带的调用点 PSI 解析被调符号；缺少调用点 PSI 时返回 `DocumentationResolution.NotTarget`；`CaSuccessCallInfo` / `CaErrorCallInfo` 的候选裁决不得出现在宿主中

#### 场景:二级构造器
- **当** 二级构造器自身无 CDoc、其包含类型有 CDoc
- **那么** 必须回落使用包含类型的 CDoc；不得返回空内容

#### 场景:符号恢复
- **当** 目标是库声明或工作区外声明，且请求由一个 use-site 源文件发起
- **那么** 必须经 `toPublicSymbol` / `restoreCallableSymbol` 等价路径，并使用请求携带的 use-site `CjFile` 恢复符号；不得因 `element.symbol` 为空而静默返回 `NotTarget`
- **且** 在携带 use-site 文件后仍无法恢复公开符号时，必须返回 `DocumentationResolution.SymbolRecoveryFailed`

#### 场景:跨文件扩展成员符号恢复
- **当** 跨文件目标是 `CjExtend` 类体中的 callable 声明
- **那么** 必须按扩展声明所在包查询 `CaExtendSymbol`，按 `extendId` 确认扩展身份，再从其 `declaredMemberScope` 按名称和 PSI 等价性恢复 callable
- **且** 不得把没有 `ClassId` 的 `CjExtend` 当作普通 class-like 或顶层 callable 处理

#### 场景:宏包装的扩展成员符号恢复
- **当** `CjExtend` 成员被声明宏包装为 `CjMacroExpression`
- **那么** 恢复过程必须只跨越宏输入与宏表达式透传节点找到扩展类体，并按扩展成员作用域恢复目标 callable
- **且** 不得把宏包装成员误判为顶层 callable

#### 场景:二进制库中的扩展成员定义恢复
- **当** use-site 引用解析到 `.cjo` 反编译 PSI 中的 `CjExtend` 成员
- **那么** compiled PSI 搜索器必须通过 extend provider 和完整扩展结构键定位 CFIR 容器，再恢复 callable
- **且** 不得要求 `CjExtend` 提供 `ClassId`
- **且** callable 的公开 owner 关系必须从组合后的 extend provider 恢复，不能只查询源码扩展索引

#### 场景:请求保留光标上下文
- **当** 宿主调用唯一解析入口
- **那么** 必须显式传入可空光标源 PSI 与可空 use-site `CjFile`；宏生成声明必须带调用点 PSI，否则返回 `DocumentationResolution.NotTarget`；从 use-site 请求解析库符号时必须传入 use-site 文件

#### 场景:跨文件恢复缺少上下文
- **当** 外部声明目标需要跨文件恢复，但请求没有 use-site `CjFile`
- **那么** 必须返回 `DocumentationResolution.MissingUseSiteContext`；不得从目标声明文件推断 use-site，也不得把该情况伪装成 `NotTarget`

#### 场景:直接声明目标没有 use-site
- **当** 请求目标本身是声明，且没有 use-site 文件
- **那么** 可直接读取该声明自身的公开 symbol；不得调用跨文件恢复或从目标文件推断 use-site。若声明自身没有可用公开 symbol，必须返回 `DocumentationResolution.SymbolRecoveryFailed`

#### 场景:符号恢复失败可见
- **当** 外部声明目标带有 use-site 文件，但公开符号恢复失败
- **那么** 必须返回 `DocumentationResolution.SymbolRecoveryFailed`，不得静默降级为 `NotTarget`

#### 场景:NotTarget 与 body 为 null 的区别
- **当** 元素不是文档目标（修饰符、无引用、宏解析失败）
- **那么** `resolve` 必须返回 `DocumentationResolution.NotTarget`
- **当** 元素是目标但只有签名（HINT）
- **那么** 必须返回 `DocumentationResolution.Resolved` 且 `content.body == null`；两种结果不得混用

#### 场景:完整文档目标没有 CDoc
- **当** 元素是完整文档目标，但声明没有 CDoc
- **那么** `resolve` 必须返回 `DocumentationResolution.Resolved`，且 `content.body` 必须是 `DocumentationBody(emptyList())`；渲染器仍可输出定义头与容器

#### 场景:HINT
- **当** 文档请求来自签名提示
- **那么** resolution 必须是 `Resolved`，其 `content.body` 与 `content.body?.enumOrdinal` 均为 null；容器行保留（与现役一致）

### 需求:Q3 模型以类型表达种类
章节必须以 sealed 子类型表达，不得使用"子类型 + kind 枚举"双轨。默认 CDoc 主节的摘要正文单独存入 `DocumentationBody.description`，不得丢弃或伪装成 tag 章节。

#### 场景:非法组合不可表达
- **当** 声明一个 "Returns" 章节
- **那么** 类型必须是 `ReturnsSection`，不存在把返回值塞进 `SamplesSection` 的写法

#### 场景:标签到子类型的映射
- **当** 构建任一 CDoc
- **那么** 标签必须映射到 11 个子类型：`@param` 按名称是否属于类型参数拆为 `ParametersSection` / `TypeParametersSection`，`@property` → `PropertiesSection`，`@throws`/`@exception` 合并为 `ThrowsSection`，`@constructor` / `@return` / `@since` / `@suppress` → 对应 `TextSection`，`@author` / `@see` / `@sample` → `AuthorsSection` / `SeeAlsoSection` / `SamplesSection`
- **且** 不得出现 `RECEIVER`（仓颉 CDoc 无该标签）

#### 场景:默认主节摘要
- **当** CDoc 默认主节包含摘要正文
- **那么** `DocumentationBody.description` 必须保留该正文，标签主节不得重复作为摘要

#### 场景:同形状标签归族
- **当** 新增一个"名称 + 描述"形状的标签
- **那么** 只需新增一个 `NamedEntrySection` 子类与一处 mapper；两个渲染器的 `when` 因家族分支无需修改

#### 场景:顺序与分栏
- **当** 同时存在 `@param` 值参数与类型参数
- **那么** 两组必须分栏且不重叠；章节顺序与现役渲染器一致

### 需求:Q4 契约纯度
`documentation/api` 不得出现 `com.intellij.*`、`org.cangnova.cangjie.psi.*`、`org.cangnova.cangjie.analysis.*`、`PsiElement`、`Project`、`TextAttributes`。

#### 场景:编译期约束
- **当** 契约包引入上述任一类型
- **那么** `DocumentationApiPurityTest` 必须失败并指出文件:行

#### 场景:无会话构建
- **当** 只给定 `CDocCommentDescriptor` 与 `typeParameterNames`
- **那么** 必须能构建章节列表，不需要 `CaSession`

### 需求:Q5 链接与示例由宿主注入
模型不持 PSI；超链接与 `@sample` 源码提取由宿主实现注入，失败必须降级。

#### 场景:LSP 未注入
- **当** LSP 未提供链接支持
- **那么** 文档必须正常渲染，链接为纯文本、示例省略，不得抛异常

#### 场景:IDE 解析失败
- **当** 链接目标无法解析
- **那么** 降级为纯文本，不产生错误节点、不中断整份文档

#### 场景:不得在共享层构造引用
- **当** 实现链接解析
- **那么** 必须通过注入的 `DocumentationLinkSupport` 使用 Analysis/PSI；不得在共享层用 `label`/`fqName` 构造 `CDocReference`

### 需求:Q6 定义头为显式协作者
定义头渲染必须由 `DefinitionRenderer`（入参 `DocumentationContent`）承担，不得使用与内容无关的无参闭包。`DocumentationContent.definition` 是共享解析服务使用 analysis-api Quick Doc renderer policy 生成的规范文本，采用短类型名、共享 effectiveModality、统一可见性呈现、local 参数与 Unit 返回类型策略。该策略只根据符号和源码语义生成文本，不读取消费方或平台标识。

#### 场景:IDE 定义头
- **当** IDE 渲染定义头
- **那么** renderer 必须消费 `content.definition` 及与其对齐的通用语义区间，对完整文本执行一次仓颉词法扫描并为语义区间覆盖的 token 使用对应主题色；不得将文本按区间切片后重新词法化，渲染后的纯文本必须与 `content.definition` 完全一致

#### 场景:默认实现
- **当** LSP 或未注入的宿主渲染定义头
- **那么** 默认实现必须原样输出 `content.definition`（转义后置于 `cangjie` 围栏）

#### 场景:定义头一致
- **当** 同一符号由 IDE 与 LSP 渲染
- **那么** 两端定义头纯文本必须与 `content.definition` 完全一致；不得保留平台专属命名或 modality 策略

#### 场景:语义区间同次生成
- **当** Analysis renderer 渲染声明与类型名称
- **那么** 必须在同一轮文本输出中采集通用语义类别和 UTF-16 区间；不得在 Quick Doc 中另写符号扫描、签名重渲染或 IDE/LSP 分支

#### 场景:局部 let 关键字与 modifier 分隔
- **当** 快速文档渲染局部绑定 `let flag: UInt`
- **那么** 输出必须包含 `local let flag: UInt`，保留 `local` 与 `let` 的空格，且 `let` 使用仓颉词法高亮 key

#### 场景:可见性修饰符与类型别名语法高亮
- **当** 快速文档渲染 `public type Amount = UInt64`
- **那么** 规范定义头必须使用仓颉源码关键字 `type`，`public` 与 `type` 都必须有词法高亮 key，`Amount` 必须使用类型别名语义颜色

### 需求:Q7 IDE 输出保持
IDE 切换共享实现时，既有 HTML 结构与测试断言不得改变。

#### 场景:正文容器
- **当** 通过 `CDocRenderer.renderCDoc` 渲染
- **那么** 输出必须仍以 `<div class='content'>` 开头（`CDocTemplate.DescriptionBodyTemplate.CangJie`）

#### 场景:既有用例
- **当** 执行 IDE 17 个 Quick Doc 用例与 `CDocRendererTest` 5 个用例
- **那么** 允许不修改任何既有断言而通过

### 需求:Q8 LSP 行为变更可见
统一使用 `CaDeclarationSymbol.findCDoc()` 后，重写成员 hover 显示基类/expect 描述；该变化必须被测试锁定并写入改版说明。

#### 场景:重写成员
- **当** hover 重写成员
- **那么** 输出必须包含基类描述；不得退回 PSI-only 查找

### 需求:Q9 跨端一致性可验收
必须存在单一期望源，证明同一符号在两端产出的章节子类型序列、条目 subject 和定义头文本一致。

#### 场景:期望源唯一
- **当** 两端执行同一用例
- **那么** 必须读取同一份 JSON 的副本，路径写死并在文档中登记同步方式

#### 场景:定义头一致
- **当** IDE 定义头与 LSP 规范定义头对同一符号进行渲染
- **那么** 两端输出必须匹配同一 JSON 中的同一份定义头期望值

#### 场景:副本同步
- **当** 主仓 highlighting/LSP 测试与 `intellij-ide` 测试读取各自资源目录中的期望 JSON
- **那么** 三份文件必须与主仓唯一源文件字节一致；同步检查发现差异时验收失败

### 需求:Q10 三仓库交付边界
主仓、`intellij-ide`、`deveco` 是独立 Git 仓库，交付与验证必须分开记录。

#### 场景:验收报告
- **当** 报告完成情况
- **那么** 必须分别列出主仓测试、IDE 测试、deveco 改动；不得用主仓 `check` 代表全部
