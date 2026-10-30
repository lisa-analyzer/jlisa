package it.unive.jlisa.utils;

import java.util.Objects;
import java.util.function.Function;

public sealed interface Either<L, R>
		permits
		Either.Left,
		Either.Right {

	record Left<L, R>(
			L value)
			implements
			Either<L, R> {
	}

	record Right<L, R>(
			R value)
			implements
			Either<L, R> {
	}

	static <L, R> Either<L, R> left(
			L value) {
		return new Left<>(Objects.requireNonNull(value));
	}

	static <L, R> Either<L, R> right(
			R value) {
		return new Right<>(Objects.requireNonNull(value));
	}

	default boolean isLeft() {
		return this instanceof Left<?, ?>;
	}

	default boolean isRight() {
		return this instanceof Right<?, ?>;
	}

	default L getLeft() {
		if (this instanceof Left<L, R>(L value)) {
			return value;
		}
		throw new IllegalStateException("Either is Right");
	}

	default R getRight() {
		if (this instanceof Right<L, R>(R value)) {
			return value;
		}
		throw new IllegalStateException("Either is Left");
	}

	@SuppressWarnings("unchecked")
	default <T> T fold(
			Function<? super L, ? extends T> leftFn,
			Function<? super R, ? extends T> rightFn) {

		if (this instanceof Left<?, ?>(Object value)) {
			return leftFn.apply((L) value);
		}

		return rightFn.apply(((Right<L, R>) this).value());
	}

	@SuppressWarnings("unchecked")
	default <T> Either<L, T> map(
			Function<? super R, ? extends T> fn) {

		if (this instanceof Left<?, ?> left) {
			return (Either<L, T>) this;
		}

		return Either.right(fn.apply(((Right<L, R>) this).value()));
	}

	@SuppressWarnings("unchecked")
	default <T> Either<L, T> flatMap(
			Function<? super R, ? extends Either<L, T>> fn) {

		if (this instanceof Left<?, ?> left) {
			return (Either<L, T>) this;
		}

		return Objects.requireNonNull(
				fn.apply(((Right<L, R>) this).value()));
	}
}
