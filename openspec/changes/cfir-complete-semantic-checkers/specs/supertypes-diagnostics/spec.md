## 新增需求

### 需求:SuperTypes 组全部诊断必须被覆盖
系统必须覆盖 `SuperTypes` 组的全部诊断定义：`SUPER_TYPES_SELF_REFERENCE`、`SUPER_TYPES_DUPLICATE`、`INTERFACE_CANNOT_INHERIT_CLASS`、`MULTIPLE_CLASS_SUPER_TYPES`。

#### 场景:超类型自引用
- **当** 声明直接或等价地把自己写进自己的超类型列表
- **那么** 必须报告 `SUPER_TYPES_SELF_REFERENCE`

#### 场景:超类型重复
- **当** 同一超类型在继承列表中重复出现
- **那么** 必须报告 `SUPER_TYPES_DUPLICATE`

#### 场景:接口继承具体类
- **当** interface 试图继承 class
- **那么** 必须报告 `INTERFACE_CANNOT_INHERIT_CLASS`

#### 场景:类拥有多个超类
- **当** class 同时继承多个具体类
- **那么** 必须报告 `MULTIPLE_CLASS_SUPER_TYPES`
