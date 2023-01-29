package org.eclipse.tractusx.irs.services.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.tractusx.irs.component.Submodel;
import org.junit.jupiter.api.Test;

class SupplyChainImpactedValidatorTest {

    SupplyChainImpactedValidator supplyChainImpactedValidator = new SupplyChainImpactedValidator();

    @Test
    void shouldThrownExceptionForMoreThanOneSupplyChainImpacted() {
        final Submodel supplyChainImpacted = Submodel.builder().aspectType("urn:bamm:io.catenax.supply_chain_impacted:1.0.0").build();
        List<Submodel> submodels = List.of(supplyChainImpacted, supplyChainImpacted, supplyChainImpacted);

        assertThatThrownBy(() -> supplyChainImpactedValidator.validateNumberOfSubmodels(submodels))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SupplychainImpacted response is in illegal state. The expected number of SupplychainImpacted submodels in job response is 1, actually there are 3");

    }

    @Test
    void shouldNotThrownExceptionForOneSupplyChainImpacted() {
        List<Submodel> submodels = List.of(Submodel.builder().aspectType("urn:bamm:io.catenax.supply_chain_impacted:1.0.0").build());

        assertThatCode(() -> supplyChainImpactedValidator.validateNumberOfSubmodels(submodels))
                .doesNotThrowAnyException();

    }

    @Test
    void shouldNotThrownExceptionForDifferentSubmodels() {
        List<Submodel> submodels = List.of(Submodel.builder().aspectType("urn:bamm:io.catenax.assembly_part_relationship:1.0.0").build());

        assertThatCode(() -> supplyChainImpactedValidator.validateNumberOfSubmodels(submodels))
                  .doesNotThrowAnyException();

    }

}