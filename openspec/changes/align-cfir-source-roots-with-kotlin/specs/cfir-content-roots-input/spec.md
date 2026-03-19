## 新增需求

### 需求:统一的内容根输入模型
编译器前端必须以 `CONTENT_ROOTS` 作为源文件输入的唯一配置入口，并从中解析源码与依赖的根路径。

#### 场景:配置包含源码根
- **当** `CompilerConfiguration` 中存在至少一个源码类型的 content root
- **那么** 前端必须从这些 content roots 解析出待处理的源文件列表

#### 场景:配置包含依赖根
- **当** `CompilerConfiguration` 中存在依赖类型的 content root
- **那么** 前端必须将其用于环境构建与符号提供，但不得将其误当作源文件输入

### 需求:兼容旧的源码路径配置
在迁移期内，系统必须支持将旧的 `CLI_SOURCE_FILE_PATHS` 映射为 content roots，并产生弃用提示。

#### 场景:仅配置旧路径键
- **当** `CLI_SOURCE_FILE_PATHS` 被设置且 `CONTENT_ROOTS` 为空
- **那么** 系统必须将旧路径转换为源码 content roots，并继续正常收集源文件

#### 场景:同时配置新旧键
- **当** `CONTENT_ROOTS` 与 `CLI_SOURCE_FILE_PATHS` 同时存在
- **那么** 系统必须以 `CONTENT_ROOTS` 为准，并记录弃用提示

### 需求:测试与CLI共享一致输入
测试基础设施与 CLI 前端必须共享同一套 content roots 配置路径，确保输入来源一致。

#### 场景:测试运行
- **当** 测试框架为模块生成编译配置
- **那么** 配置中必须写入该模块的源码 content roots
