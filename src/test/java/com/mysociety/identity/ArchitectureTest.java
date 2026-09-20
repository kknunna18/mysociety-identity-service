package com.mysociety.identity;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.mysociety.identity")
class ArchitectureTest {
    @ArchTest
    static final ArchRule apiDoesNotAccessRepositories =
            noClasses().that().resideInAPackage("..api..").should().dependOnClassesThat().resideInAPackage("..repository..");
}
