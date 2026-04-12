## 新增需求

### 需求:属性访问器必要性检查
系统必须确保属性声明包含访问器。

#### 场景:属性缺少访问器
- **当** 属性声明没有 getter 或 setter
- **那么** 必须报告 `PROPERTY_MUST_HAVE_ACCESSORS` 错误

### 需求:不可变属性 setter 限制
系统必须禁止不可变属性包含 setter。

#### 场景:不可变属性有 setter
- **当** 不可变属性（let 声明的）包含 setter
- **那么** 必须报告 `IMMUTABLE_PROPERTY_WITH_SETTER` 错误

### 需求:属性继承 mut 一致性检查
系统必须检查属性继承时 mut 修饰符的一致性。

#### 场景:属性应有 mut 修饰符
- **当** 属性覆盖父声明中的 mut 属性，但自身缺少 mut 修饰
- **那么** 必须报告 `PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT` 错误

#### 场景:属性应为不可变
- **当** 属性覆盖父声明中的不可变属性，但自身标记了 mut
- **那么** 必须报告 `PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT` 错误

### 需求:接口属性完整实现检查
系统必须确保实现类同时提供接口属性的 getter 和 setter。

#### 场景:属性未同时实现 getter/setter
- **当** 实现类的属性未同时实现接口属性要求的 getter 和 setter
- **那么** 必须报告 `PROPERTY_MUST_IMPLEMENT_BOTH` 错误
