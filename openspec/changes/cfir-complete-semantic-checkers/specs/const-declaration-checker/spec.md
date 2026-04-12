## 新增需求

### 需求:const 修饰合法性检查
系���必须验证 const 修饰的使用场景。

#### 场景:期望 const 修饰
- **当** 某些上下文中需要 const 修饰但未提供
- **那么** 必须报告 `EXPECT_CONST` 错误

### 需求:const 函数内 var 限制
系统必须禁止在 const 函数内定义 var 变量。

#### 场景:const 函数内定义 var
- **当** 在标记为 const 的函数体内定义 var 变量
- **那么** 必须报告 `CANNOT_DEFINE_VAR_IN_CONST_FUNCTION` 错误

### 需求:const 构造器前置条件检查
系统必须确保有 const 构造器才能定义 const 成员函数。

#### 场景:无 const 构造器时定义 const 成员函数
- **当** 类没有 const 构造器但定义了 const 成员函数
- **那么** 必须报告 `NO_CONST_INIT` 错误

### 需求:const 构造器与 var 成员冲突检查
系统必须禁止包含 var 成员的类定义 const 构造器。

#### 场景:类有 var 成员且定义 const 构造器
- **当** 类包含 var 成员变量同时定义了 const 构造器
- **那么** 必须报告 `CLASS_CONST_INIT_WITH_VAR` 错误
