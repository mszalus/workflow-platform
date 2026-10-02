package com.wfp.workflow.engine;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.wfp", importOptions = ImportOption.DoNotIncludeTests.class)
class EngineBoundaryTest {

    @ArchTest
    static final ArchRule onlyTheAdapterUsesFlowable = noClasses()
            .that().resideOutsideOfPackage("com.wfp.workflow.engine.flowable..")
            .should().dependOnClassesThat().resideInAPackage("org.flowable..")
            .because("the workflow engine is replaceable; Flowable stays inside its adapter package");
}
