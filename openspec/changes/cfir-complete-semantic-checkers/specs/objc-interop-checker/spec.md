## 新增需求

### 需求:ObjC 互操作类型兼容性检查
系统必须验证 ObjC 互操作声明中的类型兼容性。

#### 场景:构造器参数类型不兼容 ObjC
- **当** @ObjCMirror/@ObjCImpl 构造器的参数类型不兼容 Objective-C
- **那么** 必须报告 `OBJC_INTEROP_CTOR_PARAM_MUST_BE_OBJC_COMPATIBLE` 错误

#### 场景:方法参数类型不兼容 ObjC
- **当** ObjC 互操作方法的参数类型不兼容 Objective-C
- **那么** 必须报告 `OBJC_INTEROP_METHOD_PARAM_MUST_BE_OBJC_COMPATIBLE` 错误

#### 场景:方法返回类型不兼容 ObjC
- **当** ObjC 互操作方法的返回类型不兼容 Objective-C
- **那么** 必须报告 `OBJC_INTEROP_METHOD_RET_MUST_BE_OBJC_COMPATIBLE` 错误

#### 场景:属性类型不兼容 ObjC
- **当** ObjC 互操作属性的类型不兼容 Objective-C
- **那么** 必须报告 `OBJC_INTEROP_PROP_MUST_BE_OBJC_COMPATIBLE` 错误

### 需求:ObjC mirror 继承约束检查
系统必须验证 @ObjCMirror/@ObjCImpl 的继承约束。

#### 场景:ObjC mirror 不能继承其他超类型
- **当** @ObjCMirror 类型继承了非 mirror 的超类型
- **那么** 必须报告 `OBJC_MIRROR_DECL_CANNOT_INHERIT` 错误

#### 场景:ObjC mirror 子类型缺少注解
- **当** 继承 @ObjCMirror 的子类型未标记 @ObjCMirror 或 @ObjCImpl
- **那么** 必须报告 `OBJC_MIRROR_SUBTYPE_MUST_BE_ANNOTATED` 错误

#### 场景:ObjCImpl 必须继承 mirror
- **当** @ObjCImpl 类型未继承 @ObjCMirror 类型
- **那么** 必须报告 `OBJC_MIRROR_SUBTYPE_MUST_INHERIT_MIRROR` 错误

### 需求:ObjC 互操作 interoplib 导入检查
系统必须确保使用 ObjC 互操作时导入 interoplib.objc。

#### 场景:未导入 interoplib.objc
- **当** 使用 ObjC 互操作功能但未导入 interoplib.objc
- **那么** 必须报告 `OBJC_MIRROR_INTEROPLIB_MUST_BE_IMPORTED` 错误

### 需求:ObjC @ForeignName 检查
系统必须验证多参数 ObjC 方法的 @ForeignName 注解。

#### 场景:多参数方法缺少 @ForeignName
- **当** ObjC 互操作中多参数方法未标记 @ForeignName
- **那么** 必须报告 `OBJC_METHOD_MUST_HAVE_FOREIGN_NAME` 错误

#### 场景:多参数构造器缺少 @ForeignName
- **当** ObjC 互操作中多参数构造器未标记 @ForeignName
- **那么** 必须报告 `OBJC_CTOR_MUST_HAVE_FOREIGN_NAME` 错误

### 需求:ObjCImpl super class 检查
系统必须确保 @ObjCImpl class 有 @ObjCMirror super class。

#### 场景:ObjCImpl 没有 mirror super class
- **当** @ObjCImpl class 没有继承 @ObjCMirror class
- **那么** 必须报告 `OBJC_IMPL_MUST_HAVE_OBJC_MIRROR_SUPER_CLASS` 错误

### 需求:ObjC CJMapping 检查
系统必须验证 ObjC CJMapping 声明的约束。

#### 场景:ObjC CJMapping 继承接口不支持
- **当** ObjC CJMapping 声明尝试继承接口
- **那么** 必须报告 `OBJC_CJMAPPING_INHERITANCE_INTERFACE_NOT_SUPPORTED` 错误

#### 场景:ObjC CJMapping 泛型不支持
- **当** ObjC CJMapping 声明使用了泛型
- **那么** 必须报告 `OBJC_CJMAPPING_GENERIC_NOT_SUPPORTED` 错误
