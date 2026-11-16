# gradle-queue-cli/ — 跨进程 Gradle 调用全局串行排队器

当多个终端 / IDE / CI 进程同时在项目根目录跑 gradle 命令时，Gradle Daemon / `.gradle/**.lock` / 输出目录会出现锁等待甚至冲突。本模块提供一个 CLI 排队器：由其在 `.gradle/queue/` 目录下通过跨进程 `FileChannel` 独占锁做全局串行，把并发 gradle 调用排队为一次一个。详见仓库根 [AGENTS.md §4.1](../AGENTS.md)。

## 关键包

| 包 | 职责 |
|---|---|
| `org.cangnova.build.queue` | 全部实现都在此包：`GradleQueueMain`（CLI 入口与参数解析）/ `GlobalProjectLock`（跨进程 `FileChannel` 锁 + 崩溃 owner 恢复）/ `GradleProcessExecutor`（真实 gradlew 启动与退出码透传）/ `QueueJournal` + `QueueEntry`（队列 JSON 快照，纯可观测，失败不影响主流程）/ `ProcessHandleUtil`（PID 存活检查 / 强制销毁） |

## 依赖

- 零一方模块依赖——本模块刻意不使用 `common-configuration` 等项目约定插件，仅依赖 `kotlin-stdlib`，避免首次构建 queue-cli 时触发大量上游模块编译。

## 命令

产品用法（项目根目录，Windows；参数与 `gradlew.bat` 同参，退出码 / stdout / stderr 100% 等价）：

```powershell
.\gradlew-queue.bat :cfir:cfir-cones:assemble                     # 与 gradlew.bat 同参
.\gradlew-queue.bat --show-queue                                  # 查看 WAITING / RUNNING / DONE 条目
.\gradlew-queue.bat --kill-current                                # 强制杀死当前锁持有者（谨慎用）
.\gradlew-queue.bat --timeout-minutes 15 help                     # 15 分钟拿不到锁就放弃
.\gradlew-queue.bat --force-no-daemon clean build                 # 排障用：禁用 Gradle Daemon 复用
```

- 首次运行若 jar 不存在：脚本自动持 `bootstrap.lock` 去构建 `:gradle-queue-cli:shadowJar`，避免首跑并发冲突。
- 默认启用 Gradle Daemon（全局串行已避免多客户端并发抢 daemon 锁，速度显著优于 `--no-daemon`）。

构建本模块自身：

```bash
./gradlew :gradle-queue-cli:assemble
```

## 相关文档

- `../AGENTS.md` — §4.1 权威来源（参数、队列快照、实现文件清单）
- `../README.md` — 仓库总览
