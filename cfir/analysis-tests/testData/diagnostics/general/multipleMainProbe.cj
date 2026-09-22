// RUN_PIPELINE_TILL: FRONTEND
//
// P3 批次 3：包内多个 main 入口（官方 sema_redefinition_entry）。
//
// 官方 CheckEntryFunc（TypeCheckDecl.cpp:118-133）把每个 main 登记到 mainFunctionMap[curFile]，
// 登记表非空且当前 main 不在自己文件的登记集里时报告——包内第一个 main 被保留，其余各报一次。
// cjc 1.0.5 实测：同文件三 main → 第 2、3 个各一条；跨文件两 main → 一条。
// 本 fixture 中第一个 main 不能带标记（被登记者豁免），第二个必须报。
main() {}

<!REDEFINITION_ENTRY!>main<!>() {}
