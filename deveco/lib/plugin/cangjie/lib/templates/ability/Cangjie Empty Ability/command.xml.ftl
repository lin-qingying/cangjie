<?xml version="1.0"?>
<command>

    <mkdir dest="${rootOut}/libs"/>

    <#--  resources  -->
    <copy src="../../common/media/background.png" dest="${rootOut}/src/main/resources/base/media/background.png"/>
    <copy src="../../common/media/foreground.png" dest="${rootOut}/src/main/resources/base/media/foreground.png"/>
    <copy src="../../common/media/layered_image.json" dest="${rootOut}/src/main/resources/base/media/layered_image.json"/>
    <merge src="code template/resources/element/color.json.ftl" dest="${rootOut}/src/main/resources/base/element/color.json"/>
    <#include "commands/stringCommand.xml.ftl" />
    <mkdir dest="${rootOut}/src/main/resources/rawfile"/>

    <#--  cangjie  -->
    <instantiate src="code template/cangjie/src/index.cj.ftl"
                 dest="${rootOut}/src/main/cangjie/index.cj"/>
    <instantiate src="code template/cangjie/src/ability_stage.cj.ftl"
                 dest="${rootOut}/src/main/cangjie/ability_stage.cj"/>
    <instantiate src="code template/cangjie/src/main_ability.cj.ftl"
                 dest="${rootOut}/src/main/cangjie/main_ability.cj"/>
    <instantiate src="code template/cangjie/cjpm.toml.ftl"
                 dest="${rootOut}/src/main/cangjie/cjpm.toml"/>
    <copy src="code template/stageMode/hvigorfile.ts"
          dest = "${rootOut}/hvigorfile.ts"/>
    <#if apiType == "stageMode">
        <#include  "commands/stageCommand.xml.ftl"/>
    </#if>

    <#--  cangjie resource  -->
    <instantiate src="../../common/cj_res/cjpm.toml.ftl" dest="${rootOut}/src/main/cangjie/cj_res/cjpm.toml"/>
    <instantiate src="../../common/cj_res/src/AppR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/AppR.cj"/>
    <instantiate src="../../common/cj_res/src/SysR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/SysR.cj"/>

    <#-- hivgor -->
    <copy src="../../common/hvigor/cangjie-build-support-3.1.132.tgz" dest="${projectPath}/hvigor/cangjie-build-support-3.1.132.tgz"/>
    <merge src="code template/project-hvigor-config.json5.ftl" dest="${projectPath}/hvigor/hvigor-config.json5"/>
    <copy src="../../common/hvigorfile.ts" dest="${projectPath}/hvigorfile.ts"/>

    <#include "commands/createAbilityCommand.xml.ftl" />

</command>
