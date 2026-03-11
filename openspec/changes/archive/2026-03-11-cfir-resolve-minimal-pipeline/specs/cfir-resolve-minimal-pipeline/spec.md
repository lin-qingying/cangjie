## 新增需求

### 需求:最小 CFIR_RESOLVE 处理器链
系统必须提供 IMPORTS、TYPES、STATUS、CHECKERS 四个阶段的解析处理器，并能按阶段顺序推进解析。

#### 场景:推进到指定阶段
- **当** 调用解析入口将一个包含声明的 `CfirFile` 推进到 `STATUS` 阶段
- **那么** 文件内所有声明必须至少处于 `STATUS` 阶段

### 需求:阶段注册与调度
系统必须允许将阶段处理器注册到 `CfirPhaseResolverRegistry`，并由 `CfirTotalResolveProcessor` 按阶段顺序调度执行。

#### 场景:阶段处理器被调用
- **当** `CfirTotalResolveProcessor` 处理一个包含声明的文件
- **那么** 每个已注册阶段的处理器必须对文件内声明执行一次处理

### 需求:解析过程可在最小依赖下运行
系统必须提供最小可用的符号/提供者实现，使得最小解析链路在缺少完整语义依赖时仍可运行并推进阶段。

#### 场景:最小 provider 支撑解析
- **当** 在仅注册最小 provider 的 session 中执行解析
- **那么** 解析流程必须能够完成到 `CHECKERS` 阶段且不会因缺少 provider 而中断

## 修改需求

## 移除需求
