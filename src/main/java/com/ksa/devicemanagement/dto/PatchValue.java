package com.ksa.devicemanagement.dto;

import java.util.function.Consumer;

public sealed interface PatchValue<T> {

    record Undefined<T>() implements PatchValue<T> {}

    record Present<T>(T value) implements PatchValue<T> {
        public void ifPresent(Consumer<? super T> consumer){
            consumer.accept(value);
        }
    }

    static <T> PatchValue<T> undefined() {
        return new Undefined<>();
    }

    static <T> PatchValue<T> present(T value) {
        return new Present<>(value);
    }

    default void ifPresent(Consumer<? super T> consumer){
    }
}
