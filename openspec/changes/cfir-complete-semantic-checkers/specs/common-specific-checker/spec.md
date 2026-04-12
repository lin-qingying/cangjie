## 新增需求

### 需求:common/specific 声明匹配检查
系统必须验证 specific 声明能找到匹配的 common 声明。

#### 场景:specific 找不到匹配的 common
- **当** specific 声明没有对应的 common 声明
- **那么** 必须报告 `NOT_MATCHED` 错误

### 需求:common/specific 类型一致性检查
系统必须验证 specific 声明类型与 common 一致。

#### 场景:specific 声明类型不同
- **当** specific 声明的类型（class/struct/enum/interface）与 common 不一致
- **那么** 必须报告 `SPECIFIC_HAS_DIFFERENT_KIND` 错误

#### 场景:specific 类型不一致
- **当** specific 声明的值类型与 common 不相等
- **那么** 必须报告 `SPECIFIC_HAS_DIFFERENT_TYPE` 错误

### 需求:common/specific 修饰符一致性检查
系统必须验证 specific 修饰符与 common 匹配。

#### 场景:specific 修饰符不同
- **当** specific 声明的修饰符与 common 不匹配
- **那么** 必须报告 `SPECIFIC_HAS_DIFFERENT_MODIFIER` 错误

### 需求:common open class 构造器检查
系统必须确保 common open class 显式实现构造器。

#### 场景:common open class 无构造器
- **当** common open class 没有显式声明构造器
- **那么** 必须报告 `COMMON_OPEN_CLASS_NO_INIT` 错误

### 需求:多个 specific 实现检查
系统必须禁止 common 声明有多个 specific 实现。

#### 场景:多个 specific 实现
- **当** 同一 common 声明对应多个 specific 实现
- **那么** 必须报告 `MULTIPLE_COMMON_IMPLEMENTATIONS` 错误

### 需求:specific var/let 一致性检查
系统必须禁止 specific var 匹配 common let。

#### 场景:specific var 匹配 common let
- **当** specific 声明为 var 但 common 声明为 let
- **那么** 必须报告 `SPECIFIC_VAR_NOT_MATCH_LET` 错误

### 需求:specific 成员必须有实现体检查
系统必须确保 specific 成员提供函数体。

#### 场景:specific 成员缺少实现体
- **当** specific 成员函数或属性缺少实现体
- **那么** 必须报告 `SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION` 错误

### 需求:common 包不允许 main 检查
系统必须禁止 main 函数出现在 common 包中。

#### 场景:common 包有 main
- **当** main 函���出现在 common 包中
- **那么** 必须报告 `COMMON_PACKAGE_HAS_MAIN` 错误

### 需求:common/specific 抽象成员修饰符检查
系统必须确保 common/specific 抽象类成员有明确修饰符。

#### 场景:抽象类成员缺少显式修饰符
- **当** common/specific 抽象类的成员没有显式的 open/abstract 修饰符
- **那么** 必须报告 `CJMP_ABSTRACT_CLASS_MEMBER_HAS_NO_EXPLICIT_MODIFIER` 错误

#### 场景:abstract 成员有函数体
- **当** 显式标记为 abstract 的成员提供了函数体
- **那么** 必须报告 `EXPLICITLY_ABSTRACT_CAN_NOT_HAVE_BODY` 错误
