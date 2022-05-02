package com.bootcamp.ms.transaction.service.impl;

import com.bootcamp.ms.transaction.entity.Transaction;
import com.bootcamp.ms.transaction.repository.TransactionRepository;
import com.bootcamp.ms.transaction.service.TransactionService;
import com.bootcamp.ms.transaction.util.Constant;
import com.bootcamp.ms.transaction.util.handler.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    public Flux<Transaction> findByIdOriginTransaction(String idOriginTransaction) {
        return repository.findByIdOriginTransaction(idOriginTransaction);
    }

    
}