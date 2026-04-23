<#if (projectType?? && projectType == "Application") || (atomicService?? && !atomicService)>
    <copy src="../../common/media/startIcon.png" dest="${rootOut}/src/main/resources/base/media/startIcon.png"/>
    <merge src="ets/code template/src/stageMode/common/ets/module.json5.ftl" dest="${rootOut}/src/main/module.json5"/>
<#else>
    <copy src="../../common/media/icon.png" dest="${rootOut}/src/main/resources/base/media/startIcon.png"/>
    <merge src="ets/code template/src/stageMode/common/ets/module.json5_automic.ftl" dest="${rootOut}/src/main/module.json5"/>
</#if>

<copy src="ets/code template/res/profile/main_pages.json" dest="${rootOut}/src/main/resources/base/profile/main_pages.json"/>

<#if !superVisualEnable>
<#--  传统开发模式  -->
    <#if uiSyntax?lower_case == "ets">
    <#--  src  -->
        <#if compatibleBaseApi?? && compatibleBaseApi gte 11>
            <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/MainAbility/MainAbility_kit.ets.ftl"
                     dest="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>

            <#if ((projectType?? && projectType == "Application") || (atomicService?? && !atomicService)) &&
            (moduleType?? && moduleType == "entry")>
                <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/BackupExtensionAbility/BackupExtension.ets.ftl"
                        dest="${rootOut}/src/main/ets/${backupAbilityName?lower_case}/${backupAbilityName}.ets"/>
                <merge src="ets/code template/res/profile/backup_config.json.ftl"
                        dest="${rootOut}/src/main/resources/base/profile/backup_config.json"/>
                <merge src="ets/code template/src/stageMode/common/ets/module.json5_backup.ftl"
                        dest="${rootOut}/src/main/module.json5"/>
            </#if>

            <#if action == "ohosCreateProject" && (projectType?? && projectType == "Atomic Service" || (atomicService?? && atomicService))>
                <copy src="ets/code template/src/stageMode/traditionalCode/ets/pages/AuthIndex11.ets" dest="${rootOut}/src/main/ets/pages/Index.ets"/>
            <#else>
                <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/pages/Index11.ets.ftl" dest="${rootOut}/src/main/ets/pages/Index.ets"/>
            </#if>
        <#else>
            <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/MainAbility/MainAbility.ts.ftl"
                     dest="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
            
            <#if ((projectType?? && projectType == "Application") || (atomicService?? && !atomicService)) &&
            (moduleType?? && moduleType == "entry")>
                <instantiate src="ets/code template/src/stageMode/traditionalCode/ets/BackupExtensionAbility/BackupExtension10.ets.ftl"
                        dest="${rootOut}/src/main/ets/${backupAbilityName?lower_case}/${backupAbilityName}.ets"/>

                <merge src="ets/code template/res/profile/backup_config.json.ftl"
                        dest="${rootOut}/src/main/resources/base/profile/backup_config.json"/>
                <merge src="ets/code template/src/stageMode/common/ets/module.json5_backup.ftl"
                        dest="${rootOut}/src/main/module.json5"/>
            </#if>

            <#if action == "ohosCreateProject" && (projectType?? && projectType == "Atomic Service" || (atomicService?? && atomicService))>
                <copy src="ets/code template/src/stageMode/traditionalCode/ets/pages/AuthIndex.ets" dest="${rootOut}/src/main/ets/pages/Index.ets"/>
            <#else>
                <copy src="ets/code template/src/stageMode/traditionalCode/ets/pages/Index.ets" dest="${rootOut}/src/main/ets/pages/Index.ets"/>
            </#if>
        </#if>
        
        <open file="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
        <open file="${rootOut}/src/main/ets/pages/Index.ets"/>
    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">

    </#if>
<#else>
<#--  低代码开发模式  -->
    <#if uiSyntax?lower_case == "ets">
    <#--  src  -->
        <copy src="ets/code template/src/stageMode/superVisual/ets/src/pages" dest="${rootOut}/src/main/ets/pages"/>
        <instantiate src="ets/code template/src/stageMode/superVisual/ets/src/MainAbility/MainAbility.ts.ftl"
                     dest="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
        <#if supportHos!false>
            <copy src="ets/code template/src/stageMode/superVisual/ets/supervisual/index-hos.visual"
                  dest="${rootOut}/src/main/supervisual/pages/Index.visual" />
        <#else>
            <copy src="ets/code template/src/stageMode/superVisual/ets/supervisual/index-ohos.visual"
                  dest="${rootOut}/src/main/supervisual/pages/Index.visual" />
        </#if>
        <open file="${rootOut}/src/main/ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}"/>
        <open file="${rootOut}/src/main/ets/pages/Index.ets"/>
        <open file="${rootOut}/src/main/supervisual/pages/Index.visual"/>
    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">

    </#if>
</#if>

