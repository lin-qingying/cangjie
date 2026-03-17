## 新增需求

### 需求:多模块会话构建
CfirSessionConstructionUtils 必须支持基于模块的会话构建，并按模块返回 SessionWithSources 列表。

#### 场景:多模块返回会话列表
- **当** 输入包含多个模块的源文件集合
- **那么** 系统必须为每个模块创建对应的会话，并返回与模块一一对应的 SessionWithSources

### 需求:metadata 会话构建路径
系统必须提供 metadata 语义下的会话构建路径，并在该模式下使用共享库会话与库会话构建层次。

#### 场景:metadata 模式下构建
- **当** 会话构建处于 metadata 模式
- **那么** 系统必须构建 shared library session 与 library session，并在其基础上构建 source session

### 需求:注入式会话生产回调
系统必须使用外部注入的 createSharedLibrarySession、createLibrarySession 与 createSourceSession 回调完成会话创建。

#### 场景:回调被调用
- **当** prepareSessions 被调用
- **那么** 系统必须调用注入的回调创建会话，而不是强制使用默认工厂

## 修改需求

## 移除需求
