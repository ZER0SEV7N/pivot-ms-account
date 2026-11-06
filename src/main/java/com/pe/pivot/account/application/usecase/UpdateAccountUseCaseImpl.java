package com.pe.pivot.account.application.usecase;

import com.pe.pivot.account.domain.model.BankAccount;
import com.pe.pivot.account.domain.port.in.FindAccountByIdUseCase;
import com.pe.pivot.account.domain.port.in.UpdateAccountUseCase;
import com.pe.pivot.account.domain.port.out.AccountRepositoryPort;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Application Service responsible only for updating bank account details.
 * It preserves existing values when a field is not supplied in the update.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UpdateAccountUseCaseImpl implements UpdateAccountUseCase {
    private final AccountRepositoryPort accountRepository;
    private final FindAccountByIdUseCase findAccountByIdUseCase;

    /**
     * Updates the details of an existing bank account after validating the input and applying business rules.
     *
     * @param id the unique identifier of the bank account to update
     * @param update the BankAccount object containing updated fields
     * @return Single emitting the updated BankAccount or an error if not found
     */
    @Override
    public Single<BankAccount> updateAccount(String id, BankAccount update) {
        log.debug("Updating bank account with ID: {}", id);
        return findAccountByIdUseCase.findById(id)
                .flatMap(existing -> {
                    updateAccountFields(existing, update);
                    return accountRepository.save(existing);
                })
                .doOnSuccess(updated -> log.info(
                        "Bank account updated successfully with ID: {}", updated.getId()));
    }

    /**
     * Updates the fields of an existing bank account with values from the incoming account,
     * preserving non-null values.
     *
     * @param existing the existing bank account to update
     * @param incoming the incoming bank account containing updated values
     */
    private void updateAccountFields(BankAccount existing, BankAccount incoming) {
        // Update only non-null fields from the incoming account to the existing account
        Optional.ofNullable(incoming.getCurrency()).ifPresent(existing::setCurrency);
        Optional.ofNullable(incoming.getBalance()).ifPresent(existing::setBalance);
        Optional.ofNullable(incoming.getMaintenanceFee()).ifPresent(existing::setMaintenanceFee);
        Optional.ofNullable(incoming.getMaxMonthlyMovements()).ifPresent(existing::setMaxMonthlyMovements);
        Optional.ofNullable(incoming.getAllowedMovementDay()).ifPresent(existing::setAllowedMovementDay);
        Optional.ofNullable(incoming.getHolders()).ifPresent(existing::setHolders);
        Optional.ofNullable(incoming.getAuthorizedSigners()).ifPresent(existing::setAuthorizedSigners);
        Optional.ofNullable(incoming.getStatus()).ifPresent(existing::setStatus);
        existing.setUpdatedAt(Instant.now());
    }
}
