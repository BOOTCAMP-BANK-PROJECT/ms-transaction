package com.bootcamp.ms.transaction.service;

import com.bootcamp.ms.transaction.entity.Transaction;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface TransactionService {

    public Flux<Transaction> getAll();

    public Mono<Transaction> getById(String id);

    public Mono<Transaction> save(Transaction transaction);

    public Mono<Transaction> update(Transaction transaction);

    public Mono<Transaction> delete(String id);

    public Flux<Transaction> findByIdOriginTransaction(String idOriginTransaction);

}