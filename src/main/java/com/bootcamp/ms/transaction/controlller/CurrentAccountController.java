package com.bootcamp.ms.transaction.controlller;

import com.bootcamp.personal.passive.currentaccount.entity.CurrentAccount;
import com.bootcamp.personal.passive.currentaccount.service.CurrentAccountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("personal/passive/saving_account")
@Tag(name = "Personal Passive Product Saving Account Type", description = "Manage Personal Passive Product saving accounts type")
@CrossOrigin(value = {"*"})
@RequiredArgsConstructor
public class CurrentAccountController {

    public final CurrentAccountService service;

    @GetMapping//(value = "/fully")
    public Mono<ResponseEntity<Flux<CurrentAccount>>> getAll() {
        return Mono.just(
                ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(service.getAll())
        );
    }

    @PostMapping
    public Mono<ResponseEntity<CurrentAccount>> create(@RequestBody CurrentAccount currentAccount) {

        return service.save(currentAccount).map(p -> ResponseEntity
                .created(URI.create("/CurrentAccount/".concat(p.getId())))
                .contentType(MediaType.APPLICATION_JSON)
                .body(p)
        );
    }

    @PutMapping
    public Mono<ResponseEntity<CurrentAccount>> update(@RequestBody CurrentAccount currentAccount) {
        return service.update(currentAccount)
                .map(p -> ResponseEntity.created(URI.create("/CurrentAccount/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping
    public Mono<ResponseEntity<CurrentAccount>> delete(@RequestBody String id) {
        return service.delete(id)
                .map(p -> ResponseEntity.created(URI.create("/CurrentAccount/"
                                .concat(p.getId())
                        ))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(p))
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
