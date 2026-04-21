<copy src="code template/src/stageMode/hvigorfile.ts" dest="${rootOut}/hvigorfile.ts"/>
<#-- common -->
<instantiate src="code template/src/stageMode/module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>
<copy src="code template/src/common/traditionalCode/module-build-profile.json5" dest="${rootOut}/build-profile.json5"/>
<merge src="code template/src/common/traditionalCode/module-build-profile.json5.ftl" dest="${rootOut}/build-profile.json5"/>
<instantiate src="code template/src/common/traditionalCode/oh-package.json5.ftl" dest="${rootOut}/oh-package.json5"/>
<copy src="code template/src/stageMode/Index.d.ets" dest="${rootOut}/Index.d.ets"/>
<#--  res  -->
<copy src="code template/src/common/traditionalCode/res/element/string.json" dest="${rootOut}/src/main/resources/base/element/string.json"/>
<copy src="code template/src/common/traditionalCode/res/element/string.json" dest="${rootOut}/src/main/resources/en_US/element/string.json"/>
<copy src="code template/src/common/traditionalCode/res/element/string.json" dest="${rootOut}/src/main/resources/zh_CN/element/string.json"/>