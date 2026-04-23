<mkdir dest="${rootOut}/libs"/>

<#--  cangjie  -->
<instantiate src="cangjie/code template/code/src/index.cj.ftl"
             dest="${rootOut}/src/main/cangjie/index.cj"/>
<instantiate src="cangjie/code template/code/cjpm.toml.ftl"
             dest="${rootOut}/src/main/cangjie/cjpm.toml"/>
<copy src="cangjie/code template/stageMode/hvigorfile.ts"
      dest = "${rootOut}/hvigorfile.ts"/>

<#--  cangjie resource  -->
<instantiate src="../../common/cj_res/cjpm.toml.ftl" dest="${rootOut}/src/main/cangjie/cj_res/cjpm.toml"/>
<instantiate src="../../common/cj_res/src/AppR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/AppR.cj"/>
<instantiate src="../../common/cj_res/src/SysR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/SysR.cj"/>

<#-- hivgor -->
<copy src="../../common/hvigor/cangjie-build-support-3.1.132.tgz" dest="${projectPath}/hvigor/cangjie-build-support-3.1.132.tgz"/>
<merge src="cangjie/code template/project-hvigor-config.json5.ftl" dest="${projectPath}/hvigor/hvigor-config.json5"/>
<copy src="../../common/hvigorfile.ts" dest="${projectPath}/hvigorfile.ts"/>

<#-- config -->
<merge src="cangjie/code template/stageMode/module-build-profile.json5.ftl" dest="${rootOut}/build-profile.json5"/>
<merge src="cangjie/code template/stageMode/module-oh-package.json5.ftl" dest="${rootOut}/oh-package.json5"/>

<#-- har -->
<copy src="../../common/har/CJHyAPIRegister-v1.0.1.har" dest="${rootOut}/har/CJHyAPIRegister-v1.0.1.har"/>

<#-- loader -->
<copy src="cangjie/code template/loader/libark_interop_loader.d.ts" dest="${rootOut}/src/main/cangjie/loader/libark_interop_loader.d.ts"/>
<copy src="cangjie/code template/loader/oh-package.json5" dest="${rootOut}/src/main/cangjie/loader/oh-package.json5"/>

<open file="${rootOut}/src/main/cangjie/index.cj"/>