## 新增需求

### 需求:CJMapping 组全部诊断必须被覆盖
系统必须覆盖 `CJMapping` 组的全部诊断定义：`CJMAPPING_STRUCT_GENERIC_NOT_SUPPORTED`、`CJMAPPING_STRUCT_INHERITANCE_INTERFACE_NOT_SUPPORTED`、`CJMAPPING_DECL_NOT_SUPPORTED`、`CJMAPPING_METHOD_ARG_NOT_SUPPORTED`、`CJMAPPING_METHOD_RET_UNSUPPORTED`、`CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG`。

#### 场景:CJMapping struct 或声明形态不受支持
- **当** cangjie mirror struct 使用了不支持的泛型、接口继承或声明形态
- **那么** 必须分别报告对应的 `CJMapping` 诊断

#### 场景:CJMapping 方法签名不受支持
- **当** cangjie mirror 方法参数类型、返回类型或泛型实例配置不满足要求
- **那么** 必须分别报告 `CJMAPPING_METHOD_ARG_NOT_SUPPORTED`、`CJMAPPING_METHOD_RET_UNSUPPORTED`、`CJ_MAPPING_GENERIC_METHOD_NOT_GET_INSTANCE_CONFIG`
