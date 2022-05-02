package com.bootcamp.ms.transaction.service.impl;

import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.entity.Transaction;
import com.bootcamp.ms.transaction.repository.MovementRepository;
import com.bootcamp.ms.transaction.service.MovementService;
import com.bootcamp.ms.transaction.service.TransactionService;
import com.bootcamp.ms.transaction.util.Constant;
import com.bootcamp.ms.transaction.util.handler.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    public final MovementRepository repository;

    private TransactionService service;

    private WebClient webClientAccounts;

    @Override
    public Flux<Movement> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Movement> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<Movement> save(Movement movement) {
        return repository.findById(movement.getId())
                .map(sa -> {
                    throw new BadRequestException(
                            "ID",
                            "Client have one ore more accounts",
                            sa.getId(),
                            MovementServiceImpl.class,
                            "save.onErrorResume"
                    );
                })
                .switchIfEmpty(Mono.defer(() -> {
                            movement.setId(null);
                            movement.setInsertionDate(new Date());
                            return repository.save(movement).flatMap(m -> {
                                return generateTransactions(m).map(tr -> {
                                    return m;
                                });
                            });
                        }
                ))
                .onErrorResume(e -> Mono.error(e)).cast(Movement.class);
    }

    @Override
    public Mono<Movement> update(Movement movement) {

        return repository.findById(movement.getId())
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + movement.getId() + " was not found. >> switchIfEmpty")))
                .flatMap(p -> repository.save(movement))
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to update an item.",
                        e.getMessage(),
                        MovementServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Mono<Movement> delete(String id) {
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + id + " was not found. >> switchIfEmpty")))
                .flatMap(p -> {
                    p.setRegistrationStatus(Constant.STATUS_INACTIVE);
                    return repository.save(p);
                })
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to delete an item.",
                        e.getMessage(),
                        MovementServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    private Mono<Transaction> generateTransactions(Movement movement) {
        Transaction objTransaction = new Transaction(null, movement.getId(), movement.getAmount(),
                movement.getIdDepartureAccount(), new Date(), movement.getIsoCurrencyCode(),
                movement.getOriginMovement(), movement.getDescriptionMovement(), Constant.EXIT, new Date(),
                "PLHERRERAM", "192.168.1.2", Constant.STATUS_ACTIVE);

        Long transactionsAllowed = 3L;

        return service.save(objTransaction).flatMap(tr -> {
            tr.setIdOriginTransaction(movement.getIdIncomeAccount());
            tr.setOperationType(Constant.ENTRY);
            Mono<Transaction> transactionMono = service.save(tr).map(tre -> {
                checkAdmissedTransactions(tre, transactionsAllowed);
                checkAdmissedTransactions(tr, transactionsAllowed);
                return tr;
            });
            return Mono.just(tr);
        });
    }

    private Mono<Transaction> checkAdmissedTransactions(Transaction transaction, Long transactionsAllowed) {
        BigDecimal comission = new BigDecimal(1);
        return service.findByIdOriginTransaction(transaction.getIdOriginTransaction()).count()
                .map(c -> c.compareTo(transactionsAllowed) < 0).flatMap(re -> {
                    if (re) {
                        transaction.setAmount(comission);
                        transaction.setDescriptionMovement(Constant.COMISSION);
                        return service.save(transaction);
                    } else {
                        return Mono.just(transaction);
                    }
                });
    }
}