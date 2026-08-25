package org.banksolution.service;

import org.banksolution.entity.RiskCheckRequestEntity;
import org.banksolution.exception.RiskCheckRequestNotFoundException;
import org.banksolution.repository.RiskCheckRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.banksolution.fixtures.RiskCheckRequestFixtures.createTransferRiskCheckRequestEntity;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskCheckRequestServiceTest {

    @Mock
    private RiskCheckRequestRepository riskCheckRequestRepository;

    @InjectMocks
    private RiskCheckRequestService riskCheckRequestService;

    @Test
    void shouldDelegateSavingToTheRepository() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();

        riskCheckRequestService.save(entity);

        verify(riskCheckRequestRepository).save(entity);
    }

    @Test
    void shouldReturnTheRequestWhenItExists() {
        RiskCheckRequestEntity entity = createTransferRiskCheckRequestEntity();
        when(riskCheckRequestRepository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThat(riskCheckRequestService.findById(entity.getId())).isEqualTo(entity);
    }

    @Test
    void shouldFailWithTheMissingIdWhenTheRequestDoesNotExist() {
        UUID unknownId = UUID.randomUUID();
        when(riskCheckRequestRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> riskCheckRequestService.findById(unknownId))
                .isInstanceOf(RiskCheckRequestNotFoundException.class)
                .hasMessageContaining(unknownId.toString());
    }
}
