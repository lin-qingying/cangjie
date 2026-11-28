# CJMP SOURCE 统一值与精确类型绑定接续（2026-11-20）

本批继续原P8/H1/H9、相关E6/G1的前端正式产品，唯一指定工作树及历史修改保留。前批[拥有闭包/F](cjmp-source-owned-closure-continuation-20261120.md)的99/99是独立窗口；本批不与它重复加分，不据局部验收关闭原70项/P0–P10或完整S5/S6。

## 实际生产修订

- 原CfirSourceConstantDecoder原地移为CfirConstDeserializer；VarInfo、Pattern、allValues共用同一个context/helper、raw union reader和typed材料化。DD的另一个语义reader/materializer、按tag猜完整类型、Unit补缺和公开ClassId重选classifier路径已删除。
- context.constantValues由同一CompletedGraph发布；v0是合法零基索引，NONE才是未保存值。对象/tuple先登记真实身份，再填字段/元素；共享及自环/互环保留原壳，失败/取消丢本次图，旧完成fields和evaluatedInitializer不被重填。
- VarInfo常量先排入根事务队列，classifier/字段header完成后统一材料化。类自身static const可读取同一完成header；新值引入的真正header依赖继续排空，最终才出版。
- Int16Value按实际Float16/Int16槽分派，binary16保留符号、subnormal、Infinity及NaN有效位。UInt64通过无符号数学值进入BigInteger；Tuple/VArray/Array按实际容器元素类型分派。对象/字段type各自验证真实必需槽，不从静态变量或member声明补0。
- Composite每个入口、缓存命中和在途回边都检查actual value type ≤ expected type。字段声明以actual classifier自身TP symbol→对象typeArgs替换，再与独立MemberValue.type核对；canonical field/type-ref不被写成某个实例类型。合法动态Derived值可经Base声明读取。
- CJO nominal symbol在本context按真实d索引预留，自身TP/Generic身份先登记，bounds/header显式完成。名义Cone首次构造持有fixed symbol，types/header/值共用原产品。type-first或header-first不生成第二symbol，根发布要求所有pending classifier/Generic工作完成。
- 固定Kotlin ConeClassLikeLookupTagWithFixedSymbol/toSymbol职责在原tree/semantics模块本地化；CJO class/interface/struct/enum symbol构造时取得stable fixed tag，SOURCE默认保持原普通tag。源码user-type、String/interpolation/constructor helper通过已选symbol.toLookupTag传播，未增加ClassId/String相等特判或后置tag setter。
- TypeContext/typeAware及tag-bearing alias展开按真实tag取得classifier。self派生保留原constructor tag；binary session单独装配同一typeAware查询服务，未启动BODY或造CFG。wire kind与实际FullId.decl.kind严格核对，不能用Class Cone包装Interface/Struct。
- Bool/Rune整数union分支缺固定编码证据，现明确拒绝；官方SOURCE literal路径仍保存其合法语义。throw/catch标准root从真实core ClassDecl/既有resolved pattern取得，避免checker重造普通Exception marker；原diagnostic/实际表达式类型不改。

固定依据见本批[constant native](../build/cjmp-owned-closure-20261120/constant-native-map.md)、[constant Kotlin](../build/cjmp-owned-closure-20261120/constant-kotlin-map.md)、[fixed classifier native](../build/cjmp-owned-closure-20261120/fixed-classifier-native-map.md)、[fixed classifier Kotlin](../build/cjmp-owned-closure-20261120/fixed-classifier-kotlin-map.md)。Kotlin=3c7a5041f238102285764d370665f56f587f9dc4，native=14276740ffec1262b81b6b5236b1d840af270b37，runtime=fc9f54db5d92079eb1845968abe98e52df92d70d，docs=70281ea0ecb4adb86260ac233cf87fb4dbb23bc9。

固定native writer不写VarInfo.value/allValues，SOURCE ConstPattern不写values。非空v池/binary16/循环的新例是typed schema材料化验证，不冒称官方cjc生产过该值池；原SOURCE18仍是独立实际SOURCE→CJO→fresh library consumer链。

## 失败、审查与实际验证

- 93ed67fc：新13项1通过/12失败；9908同8B262E44F6F97FFE9353F05CC2970A554E82381FBCFE43F44B7056C4AD8DEF73，149560字节原XML包含真实循环溢出。见constant-red-results.json。
- 55096d5d：生产/tests编译退出0。387675ca：15/15通过，同41EF35DAB655C7C4E7735A249CA257C2967DB76ABEEE24AC11F314B7662481EB，2978字节原XML。首gate仍拒绝R1–R4，未将15绿替代类型/身份门。
- 584cb0db：完整serialization231及SOURCE18，4个selection harness错误、5个未配置SDK跳过。helper错误因完成CFIR又进RAW准备；Fixture现使用正式completed export capture入口，不回退phase或伪造helper metadata。
- c5525bce：加入R1/R2/R4和Q1warm/Q2 classifier检查，18项14通过/4真实失败。同88B4AAA4323F28105270CE5840AA671BF05B1251FBB5B67D5F862A89655092B9；constant-compatibility-red-results.json完整保存。
- cc761938：main编译完成，tests的Composer导入错误退出1；实际正确包为serialization.provider，已修。3476efc5：新完整265项7失败/5跳过，SOURCE String→String错配真实暴露；类型/源码constructor统一传播后该诊断消失。harness另外补齐LanguageSettings/scope/dummy resolver及同manager正式external callback，不改正确expect。
- 6a89ca7e：编译成功。5ef5138e：serialization236、typeAware12全部执行项通过，SOURCE18仅2个throw错误、5SDK配置跳过。同193371172B98B7EA70A8D9429A0678D3FD4F24BDD012EECAF790DEF924807274；此窗口不冒称联合全过。
- 修throw/default-catch root后，5bd32aa9：实际SOURCE18/18，同9908/24E2A85BE3BEAC5ABD0ED121DDFE758DA25AB353DFCA60492DDE9781F2074AD5，四原XML4602字节。

首/第二gate所有拒绝报告保留；[第三gate](../build/cjmp-owned-closure-20261120/constant-third-gate.md)对R1–R4/B1–B3及必要SOURCE root修订有限静态ACCEPT，不替代全域cache或完整原验收。上述每轮均在build/cjmp-owned-closure-20261120独立保存输入、queue/逐case/gzip原XML，不能合并重复通过数。

## 最终同窗口SDK联合终态

8121a14a于2026-11-19T22:45:23.836Z至22:47:19.626Z实际DONE/exit0，**266/266、无失败/错误/跳过**：完整serialization236（含新20及配置SDK5）、原SOURCE18、typeAware12。9908输入前后同 **24E2A85BE3BEAC5ABD0ED121DDFE758DA25AB353DFCA60492DDE9781F2074AD5**；34份原XML104652字节全部在真实窗口，解压字节/SHA无损核验。见[事前输入](../build/cjmp-owned-closure-20261120/sdk-joint-inputs.json)与[终态/逐case/原XML](../build/cjmp-owned-closure-20261120/sdk-joint-results.json)。

实际SDK只读根为 `C:/Users/lin17/.cangjie/sdks/harmonyos-cangjie-sdk-windows/cangjie/build-tools/modules/linux_ohos_aarch64_cjnative/std`，配置CANGJIE_CJD_SDK_DIR。38个cj.d+46个CJO共84文件，前后同 **47422485EEA657F75E6AF28010A18E192A84CF88894A94D5A00BC59671C941A2**；未复制/修改SDK或混用另一arch快照。

## 接续与完整余额

下一完整产品是canonical统一encoder：source与imported都从正式完成graph/frozen输入编码body/F header/pattern/annotation/位置/值池，退休metadata producer的独立imported wire-copy路径。formal package graph能力及ordinary/common/depends-on重导出、CLI/LL分析按实际consumer/readiness继续验证，不因graph可读授予CFIR BODY/CFG资格。

专用多parent Position/composite失败发布、全域provider/cache/tag/cross-context调度、normal R5消费、P0/P10与原版本/CFIR/API/IDE余额仍按原70项保留。H6排除余额留分母、不实施；v10待办不阻断其它授权工作。原项不增整项分值，目标保持active。
