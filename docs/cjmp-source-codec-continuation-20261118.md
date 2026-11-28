# CJMP SOURCE codec 接续（2026-11-18）

> 后续[loaded-snapshot接续](cjmp-source-snapshot-continuation-20261118.md)完成表项snapshot/domain/raw index身份、同路径跨代冲突、只读buffer和helper真实顶层归属，实际173/173，原SOURCE14仍14/14。完整冻结类型、统一编码/解码及正式内容消费继续OPEN，以下path-only memo描述是修订前历史。

> 后续S4格式修订2ab24d31在当前新字节下完整通过43/43：CjoPackageWriterTest 25/25、SOURCE18 18/18（原14仍14/14），6841同SHA、5原XML共9209字节全部在真实窗口。[空向量及SOURCE回归](cjmp-source-helper-20261117/source-type-empty-vectors-results.json)。Native只有nominal/func/tuple等角色写参数vector，Generic/primitive/Invalid自身无typeArgs；Generic.upperBounds保留present-empty。该修订不关闭冻结类型、统一身份、decoder/正式消费。

> 最新真实终态：f90e054a联合101/101，原SOURCE14实际14/14、新四官方引用4/4、copy19、S036、canonical8、helper18和原负例2全部通过；6841输入SHA同为1559B80FBA095591C93FA3A4855F326973246890684FD21660FDFC9AE1D67F5B，全部12原XML共21597字节均在真实窗口。完整逐case/压缩XML见[最终ledger](cjmp-source-helper-20261117/source-pattern-reference-joint-101-final-results.json)。这不关闭S3/S4、decoder/正式消费或原条目。以下99/101等保留历史。

目标仍是原70项/P0–P10及有效修订的前端部分，不扩展全编译器，不实施后端。唯一实施树保持不变，全部历史暂存/未暂存修改保留。固定Kotlin为3c7a5041f238102285764d370665f56f587f9dc4，官方v113为14276740ffec1262b81b6b5236b1d840af270b37，均按Git对象读取。

## 当前真实证据

- 三池完成事务编译5406912f退出0，6837输入事前/事后同SHA，仅证明当时serialization编译：[ledger](cjmp-source-helper-20261117/source-unified-tables-compile-results.json)。
- 63217820为SOURCE14的8通过/6失败，原XML实际窗口；采样晚于Return/Nothing和v-freshExc两处修订，SHA已漂移，未称同代复验：[ledger](cjmp-source-helper-20261117/source-canonical-writer-14-results.json)。
- 6ce21178未执行JUnit，resolve新增导入错误；6839同SHA。a00e6a2d生产已编译，新增test字符串插值错误，未执行JUnit；生成器在窗口写入十个tests-gen文件、SHA漂移。两次失败独立归档，未计旧XML。
- 7cad583a实际SOURCE18为12通过/6失败：原SOURCE14为12/14，新69和42引用的四个用例失败。6841输入同SHA，原XML全部在窗口：[ledger](cjmp-source-helper-20261117/source-parent-reference-18-first-results.json)。
- e536b091实际SOURCE18为16通过/2失败：原14为12/14，新四个用例4/4。111个官方引用的Absent/Invalid/实际匹配父类型及完整声明/正文结构通过，原operators失败是局部模式容器被编码为VarDecl：[ledger](cjmp-source-helper-20261117/source-parent-reference-18-publication-results.json)。
- 5f85cfe7联合101为99通过/2原operators失败：copy19、S036、canonical8、helper18、原负例2全部通过，SOURCE18仍16/18。6841同SHA、全部真实窗口XML：[ledger](cjmp-source-helper-20261117/source-pattern-reference-joint-101-first-results.json)。
- 上轮已接入局部declarationPatterns和VarWithPatternInfo；5f85最后失败明确是writer只准许ExtendDecl空identifier。现已依据官方VarWithPatternDecl精确允许其空名称，其他声明仍严格。正在仅复验两个原operators；不能把先前12通过拼接为一次SOURCE14全绿。
- fa93dd04随后只跑两个原operators，实际2/2、6841同SHA、两XML在窗口：[ledger](cjmp-source-helper-20261117/source-pattern-operators-two-results.json)。f90e054a另一次完整联合101/101在相同候选字节下实际重跑全部范围，原SOURCE14的14/14由这一完整窗口证明，未拼接先前12＋2。

## 实际生产修改

CjoGraphTable为d/e/t共用显式Reserved/Filled及finish合同，三表各自独立一基索引；source与imported声明同一工作队列收敛，Invalid不再被用作未填槽。

CfirReferenceParentType在tree保存真实Absent、Invalid、Instantiated，不借ConeErrorType。normal completion保存generic-call条件、实际receiver/继承/候选映射及lookup context；实际transformFunctionCall/qualified access最终写回接入Applied reference，factory不重建上下文，PCLA随后重映射类型和parent并保存引用别名。前一候选只修改未使用的helper，已纠正实际接线；没有据其编译声称行为完成。

普通Array的合法getter不再只返回元素类型；所有普通Array下标由正常调用解析/完成发布resolvedGetCall，SOURCE只消费真实绑定。Tuple/VArray/RawArray仍保留各自的原生角色。

canonical图新增局部复合声明的declarationPatterns：模式、binding、initializer、d/e槽分别保留身份，providers同代捕获其依赖，encoder投影VarWithPatternDecl/CjoVarWithPatternInfo。简单binding与包级投影仍各按真实角色处理。

新增referenceInstantiation.cj及referenceDerivedInstantiation.cj分别由固定SDK实际编译/解码，原四fixture与正确SOURCE14期望未改。正式harness检查完整D/E闭包及111条引用，支持官方GenericParamDecl/StructDecl、应用后泛型字段类型；没有降低原合法检查。

## 固定考证与审查

新的native/Kotlin父类型、冻结类型映射在build/source-expression-codec-audit-v113/source-matched-parent-{native-authority,kotlin-map}-20261118.md、source-frozen-types-{native-authority,kotlin-map}-20261118.md。两个真实SDKfixture完整产物分别在build/source-reference-instantiation-v113和build/source-reference-derived-instantiation-v113。

source-reference-parent-first-review-20261118.md的结论是REJECT：receiver映射遗漏、serializer live类型消费。receiver分支和真实发布随后已修并通过新四行为用例；不能据此移用旧审查为新ACCEPT。当前限定复审另开只读新agent，未联系停止旧会话。

该新限定复审agent01a1ec34-7519-7071-a992-f8d7a204afad因服务peak-load/最大usage size断流，没有返回新ACCEPT。记录的是服务失败，不冒称候选已独立验收。固定源码与真实行为证据继续有效；完整审查待重试，不阻断其它授权实施。

## 必须继续的原范围

S3完整、S4完整、S5/S6/S7及P8/E6/G1均未关闭。SOURCE14绿也只是必要条件。

下一S4生产工作是退休expressionOrNull及活动类型/provider读取，冻结全部有序类型payload、声明self type/alias/This/RawArray dims、完整Generic语义bounds与独立constraint节点；固定common→specific TP身份投影、loaded snapshot身份/domain/index memo及冲突门、present-empty vectors，统一source/imported编码。现有路径型memo及typeElement返回后仍读live的代码明确OPEN。

present-empty vectors已由2ab24d31完成本次限定格式/回归验证，不能据此关闭整段S4。最新native片段确认SaveGenericsTy只写GenericTyInfo及其上界vector；不是所有SemaTy都拥有typeArgs。之前概括所有空typeArgs的描述按这一真实角色边界执行。下一代码仍是冻结类型输入与fixed投影、同代snapshot/domain/index memo和legacy路径退休，然后正式decoder/materialization与前端/LL消费。

随后交付统一decoder/source materialization及普通/common/depends-on前端与LL的实际正文消费。原copy完成与analysis-ready分开，不用空CFG、phase回退或body=0替代消费者需求。

原前端余额C3/C4/D5/E6/E7/E8/E9/E12/F4/G1/G3/G5、H1/H9/H10/H13附项、H6前端门、P0/P10及全量CFIR/LL/API/IDE和49版本/v10验收全部保留。H6排除后端余额保留历史分母不实施；v10缺少不阻断授权工作。没有发布新整体百分比，没有提交、回退、清理历史修改。
