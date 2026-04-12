## 新增需求

### 需求:继承成员类型一致性检查
系统必须验证子类成员与父类同名成员的类型一致性。

#### 场景:成员类型不一致
- **当** 子类成员（函数/属性）的类型与父类同名成员不一致
- **那么** 必须报告 `INHERIT_MEMBER_KIND_INCONSISTENT` 错误

#### 场景:跨父类型成员类型不一致
- **当** 从多个父类型继承的同名成员声明类型不一致
- **那么** 必须报告 `INHERIT_SUPER_MEMBER_KIND_INCONSISTENT` 错误

#### 场景:跨父类型成员类型无子类型关系
- **当** 从多个父类型继承的同名成员类型不一致且无子类型关系
- **那么** 必须报告 `INHERIT_MEMBER_TYPE_INCONSISTENT` 错误

### 需求:抽象类 static 未实现检查
系统必须检查抽象类中未实现的 static 函数/属性。

#### 场景:抽象类含未实现 static 成员
- **当** 抽象类包含未实现的 static 函数或属性
- **那么** 必须报告 `INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC` 错误

### 需求:open/abstract 成员可见性检查
系统必须确保 open/abstract 成员的可见性为 public 或 protected。

#### 场景:open/abstract 成员可见性不合法
- **当** open 或 abstract 成员的可见性不是 public 或 protected
- **那么** 必须报告 `INVALID_MEMBER_VISIBILITY_IN_CLASS` 错误

### 需求:sealed 继承约束检查
系统必须检查 sealed 类的继承约束。

#### 场景:继承 sealed 类
- **当** 非同文件/同模块的类尝试继承 sealed 类
- **那么** 必须报告 `CANNOT_INHERIT_SEALED` 错误

### 需求:ThreadContext 继承约束检查
系统必须检查 ThreadContext 的继承约束。

#### 场景:非法继承 ThreadContext
- **当** 用户定义的声明尝试继承/实现/扩展 ThreadContext
- **那么** 必须报告 `INHERIT_THREAD_CONTEXT_INVALID` 错误

#### 场景:ThreadContext 子类标记 open
- **当** 继承 ThreadContext 的声明被标记为 open
- **那么** 必须报告 `INHERIT_THREAD_CONTEXT_NOT_OPEN` 错误

### 需求:This 返回类型约束检查
系统必须检查 open 函数返回 This 类型时的 override 约束。

#### 场景:override 未保持 This 返回类型
- **当** 父类 open 函数返回 This 类型，但 override 实现未保持返回 This
- **那么** 必须报告 `INHERIT_NOT_RETURN_THIS` 错误
