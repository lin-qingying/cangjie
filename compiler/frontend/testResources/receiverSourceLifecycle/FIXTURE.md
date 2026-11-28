# Receiver SOURCE 生命周期回归

Kotlin固定提交：3c7a5041f238102285764d370665f56f587f9dc4。官方仓颉源码固定提交：14276740ffec1262b81b6b5236b1d840af270b37。

真实cjc为1.1.3/cjnative/x86_64-w64-mingw32，executable SHA256 DFEBED3139AFE00EE642F75322AB82D1D97651A5E9F10EBF4F6D86ABAC061257。普通BODY源与独立合法@Frozen选中源均保留；七个官方CJO保存非零被测F.body。official.cjo.base64是原CJO字节的文本保存形式，测试恢复到临时目录并验证SHA256，不修改元数据。

完整原argv、诊断、运行输出、D/E与F/body槽证据及全部失败代次保存在build/cjmp-invisible-peer-20261125/r23b-receiver-tests/generation-2/report.md、source-selection.json、case-differential.json，以及其原始stdout/stderr。非法样例仅做正式诊断检查，不导出错误正文。

新增28项测试检查两parser BODY/本仓真实CJO/官方CJO的同一receiver身份、finalizer限制与super物理Child/selector Parent、局部owner/lexical及普通写/compound顺序。运行成功也不替代完整S1–S7或原70验收。

选中源与官方CJO证明：

- read：CJ 77E13AF741E71CE4FE143EE9A3836F22BB8BBE94ED96B56A5EDE7982B6DECEB1；CJO 8418D9881CB8710A465156AB2B5EAF74E3035656D3D5DC22311F7043EB274CCC；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected\read\receiver_read.cjo package=receiver_read D=10 E=20
- call：CJ 68BD1E710A1383D2C9A6843FC658F78DE6953A28B7568D3D2F4D4C9C0832C502；CJO C36EC7848289E07D99733C3BB9A6B386C5D8AD3CA28ADFDE55768E4B116AA814；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected\call\receiver_call.cjo package=receiver_call D=9 E=23
- write_target：CJ CB577A588E0812930DE9BECD7CDEC1E5ACE3DA8C64A91EC0C96D17CA38F767F3；CJO 301EF068AEAF010B3E079320E3C3ECAA77E6FDB9242A73A837CA91965074F8D7；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected\write_target\receiver_write_target.cjo package=receiver_write_target D=8 E=21
- compound_once：CJ D910D2A00D32CEF9D204FCC70E3AAA2952A45D31F0FF993F2CF48CAFB8F52526；CJO 555C84BE6F7D14606B3F6680881C0D986F6C567FAAC4009793A10A8255C3866F；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected\compound_once\receiver_compound_once.cjo package=receiver_compound_once D=8 E=23
- nested_other：CJ E81BC68821C7F2220DF8AF63A161CE4B88C7E0DCD698A6DA63DA1CFBFFE9D01E；CJO AFBBBDC263DA3F8965D39F8FA0CA6981DA7E598220E6CE8589D65336DA90AD2B；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected-2\nested_other\receiver_nested_other.cjo package=receiver_nested_other D=7 E=21
- finalizer_field：CJ 58D88F17333D4CB7F95F4CBA60DC66A4A80CFFF2A13D60D7D4F9BA3C60E100A0；CJO F1D5A927B728AB274A328F0475F1B051D867A9F680CF0B605D90F8BFE4DDAE75；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected-2\finalizer_field\receiver_finalizer_field.cjo package=receiver_finalizer_field D=7 E=5
- finalizer_nested_field：CJ 1607203C067678574030331FCD8607D3CC13B7FDFD527EB0508C6F1C9C1EA107；CJO 07606466A8C218711D8F79B9CB3CB07FC73EACCBCDB67B2EB5C5E76A13F18A37；CJO C:\Users\lin17\.codex\worktrees\cjmp-framework-implementation\cangjie\build\cjmp-invisible-peer-20261125\r23b-receiver-tests\source-selected-2\finalizer_nested_field\receiver_finalizer_nested_field.cjo package=receiver_finalizer_nested_field D=8 E=9
