package pl.amitec.mercury;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.GeneralCodingRules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "pl.amitec.mercury", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
    @ArchTest public final ArchRule noCrossIntegratorsDependencies = slices()
            .matching("..integrators.(*)..")
            .should().notDependOnEachOther();

    @ArchTest public final ArchRule noCycles = slices()
            .matching("..(*)..")
            //.namingSlices("$1")
            .should().beFreeOfCycles();

    @ArchTest public final ArchRule noAccessToIntegratorsFromOutside = classes().that().
            resideInAPackage("..integrators.*..")
            .should().onlyBeAccessed().byAnyPackage("..integrators..");

    @ArchTest public final ArchRule noGenericRuntimeExceptionsThrown = GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    @ArchTest public final ArchRule noSystemOut = GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
}
