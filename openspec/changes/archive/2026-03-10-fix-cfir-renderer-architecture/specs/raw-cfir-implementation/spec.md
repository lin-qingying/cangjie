## 新增需求

## 修改需求

### 需求:Raw CFIR 渲染能力必须同时服务测试与非测试场景
Raw CFIR 阶段必须提供可复用的渲染能力，用于 golden file 对比、调试定位与开发期可读性输出。系统必须保留 golden file 对比能力，但禁止将渲染器能力限定为仅供 golden file 使用。

#### 场景:执行 golden file 对比
- **当** Raw CFIR 测试执行 `DUMP_CFIR` 并进行期望文件比对
- **那么** 系统必须使用明确的 golden 兼容渲染配置生成稳定输出，以保证历史测试可持续回归

#### 场景:开发者进行调试渲染
- **当** 开发者在非 golden 场景下选择调试或可读性渲染配置输出同一 CFIR 节点
- **那么** 系统必须输出对应配置的文本结果，且不得要求修改 golden file 测试基线

#### 场景:三类 Raw CFIR 测试路径统一使用 golden 兼容输出
- **当** 执行 rawBuilder、sourceElementMapping、lazyBodies 三类 Raw CFIR 测试路径
- **那么** 三者必须统一使用 golden 兼容 profile 产出文本并按各自基线文件进行比较

## 移除需求
