package com.bootcamp.ms.transaction.service.impl;

import com.bootcamp.ms.transaction.entity.Movement;
import com.bootcamp.ms.transaction.entity.Transaction;
import com.bootcamp.ms.transaction.repository.TransactionRepository;
import com.bootcamp.ms.transaction.service.TransactionService;
import com.bootcamp.ms.transaction.util.Constant;
import com.bootcamp.ms.transaction.util.handler.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    public final TransactionRepository repository;

    @Override
    public Flux<Transaction> getAll() {
        return repository.findAll();
    }

    @Override
    public Mono<Transaction> getById(String id) {
        return repository.findById(id);
    }

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.findById(transaction.getId())
                .map(sa -> {
                    throw new BadRequestException(
                            "ID",
                            "Client have one ore more accounts",
                            sa.getId(),
                            TransactionServiceImpl.class,
                            "save.onErrorResume"
                    );
                })
                .switchIfEmpty(Mono.defer(() -> {
                            transaction.setId(null);
                            transaction.setInsertionDate(new Date());
                            return repository.save(transaction);
                        }
                ))
                .onErrorResume(e -> Mono.error(e)).cast(Transaction.class);
    }

    @Override
    public Mono<Transaction> update(Transaction transaction) {

        return repository.findById(transaction.getId())
                .switchIfEmpty(Mono.error(new Exception("An item with the id " + transaction.getId() + " was not found. >> switchIfEmpty")))
                .flatMap(p -> repository.save(transaction))
                .onErrorResume(e -> Mono.error(new BadRequestException(
                        "ID",
                        "An error occurred while trying to update an item.",
                        e.getMessage(),
                        TransactionServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Mono<Transaction> delete(String id) {
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
                        TransactionServiceImpl.class,
                        "update.onErrorResume"
                )));
    }

    @Override
    public Flux<Transaction> getByIdOriginTransaction(String idOriginTransaction) {
        return repository.findByIdOriginTransaction(idOriginTransaction);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetweenAndIsComission(String idOriginTransaction, Date startDate, Date finishDate, Boolean isComission) {
        return repository.findByIdOriginTransactionAndInsertionDateBetweenAndIsComission(idOriginTransaction, startDate, finishDate, isComission);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndInsertionDateBetween(String idOriginTransaction, Date startDate, Date finishDate) {
        return repository.findByIdOriginTransactionAndInsertionDateBetween(idOriginTransaction, startDate, finishDate);
    }

    @Override
    public Flux<Transaction> getByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(String idOriginTransaction, Short operationType, Date startDate, Date finishDate) {
        return repository.findByIdOriginTransactionAndOperationTypeAndInsertionDateBetween(idOriginTransaction, operationType, startDate, finishDate);
    }

    @Override
    public Mono<Transaction> generateTransactions(Movement movement) {
        Transaction objTransaction = new Transaction(null, movement.getId(), movement.getAmount(),
                movement.getIdDepartureAccount(), new Date(), movement.getIsoCurrencyCode(),
                movement.getOriginMovement(), movement.getDescriptionMovement(), Constant.EXIT, new Date(),
                false,movement.getIsPassive(), "PLHERRERAM", "192.168.1.2", Constant.STATUS_ACTIVE);

        Long transactionsAllowed = 3L;

        return repository.save(objTransaction).flatMap(tr -> {
            tr.setIdOriginTransaction(movement.getIdIncomeAccount());
            tr.setOperationType(Constant.ENTRY);
            Mono<Transaction> transactionMono = repository.save(tr).map(tre -> {
                if(movement.getIsPassive()) {
                    checkAdmissedTransactions(tre, transactionsAllowed);
                    checkAdmissedTransactions(tr, transactionsAllowed);
                }
                return tr;
            });
            return Mono.just(tr);
        });
    }
    @Override
    public Mono<Transaction> checkAdmissedTransactions(Transaction transaction, Long transactionsAllowed) {
        BigDecimal comission = new BigDecimal(1);
        return getByIdOriginTransaction(transaction.getIdOriginTransaction()).count()
                .map(c -> c.compareTo(transactionsAllowed) < 0).flatMap(re -> {
                    if (re) {
                        transaction.setIsComission(true);
                        transaction.setAmount(comission);
                        transaction.setDescriptionMovement(Constant.COMISSION);
                        return repository.save(transaction);
                    } else {
                        return Mono.just(transaction);
                    }
                });
    }
    @Override
    public Mono<BigDecimal> getProductBalance(String idOriginTransaction) {
        return getByIdOriginTransaction(idOriginTransaction)
                .map(tr -> tr.equals(Constant.ENTRY) ? tr.getAmount() : tr.getAmount().negate())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


}