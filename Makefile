export JAVA_HOME := /usr/local/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home

ENV_FILE ?= .env

.PHONY: run test build docker-build docker-run clean help

## run          Start the app locally with .env loaded
run:
	@bash -c 'set -a && source $(ENV_FILE) && set +a && ./gradlew bootRun --no-daemon'

## test         Run all tests
test:
	JAVA_HOME=$(JAVA_HOME) ./gradlew test --no-daemon

## build        Full build (compile + test + jar)
build:
	JAVA_HOME=$(JAVA_HOME) ./gradlew build --no-daemon

## docker-build Build Docker image
docker-build:
	docker build --platform linux/amd64 -t brand-extractor-service .

## docker-run   Build and run Docker image with .env
docker-run: docker-build
	docker run --env-file $(ENV_FILE) -p 8080:8080 brand-extractor-service

## clean        Clean build outputs
clean:
	JAVA_HOME=$(JAVA_HOME) ./gradlew clean --no-daemon

## help         Show available targets
help:
	@grep -E '^##' Makefile | sed 's/## //'
