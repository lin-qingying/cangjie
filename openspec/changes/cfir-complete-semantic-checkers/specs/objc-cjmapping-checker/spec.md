## 新增需求

### 需求:ObjCCJMapping 组全部诊断必须被覆盖
系统必须覆盖 `ObjCCJMapping` 组的全部诊断定义：`OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED`、`OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED`。

#### 场景:ObjC CJMapping 继承接口不受支持
- **当** ObjC cangjie mirror 声明继承接口
- **那么** 必须报告 `OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED`

#### 场景:ObjC CJMapping 泛型不受支持
- **当** ObjC cangjie mirror 声明使用不受支持的泛型
- **那么** 必须报告 `OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED`
