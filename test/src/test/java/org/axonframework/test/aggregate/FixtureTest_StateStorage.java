/*
 * Copyright (c) 2010-2018. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.test.aggregate;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.messaging.unitofwork.CurrentUnitOfWork;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.hamcrest.CoreMatchers.any;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Allard Buijze
 */
class FixtureTest_StateStorage {

    private FixtureConfiguration<StateStoredAggregate> fixture;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(StateStoredAggregate.class);
    }

    @AfterEach
    void tearDown() {
        if (CurrentUnitOfWork.isStarted()) {
            fail("A unit of work is still running");
        }
    }

    @Test
    void testCreateStateStoredAggregate() {
        fixture.givenState(() -> new StateStoredAggregate("id", "message"))
               .when(new SetMessageCommand("id", "message2"))
               .expectEvents(new StubDomainEvent())
               .expectState(aggregate -> assertEquals("message2", aggregate.getMessage()));
    }

    @Test
    void testEmittedEventsFromExpectStateAreNotStored() {
        fixture.givenState(() -> new StateStoredAggregate("id", "message"))
               .when(new SetMessageCommand("id", "message2"))
               .expectEvents(new StubDomainEvent())
               .expectState(aggregate -> {
                   apply(new StubDomainEvent());
                   assertEquals("message2", aggregate.getMessage());
               })
               .expectEvents(new StubDomainEvent())
               .expectState(Assertions::assertNotNull);
    }

    @Test
    void testCreateStateStoredAggregate_ErrorInChanges() {
        ResultValidator<StateStoredAggregate> result =
                fixture.givenState(() -> new StateStoredAggregate("id", "message"))
                       .when(new ErrorCommand("id", "message2"))
                       .expectException(any(Exception.class))
                       .expectNoEvents();
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> result.expectState(aggregate -> assertEquals("message2", aggregate.getMessage())));
        assertTrue(e.getMessage().contains("Unit of Work"), "Wrong message: " + e.getMessage());
        assertTrue(e.getMessage().contains("rolled back"), "Wrong message: " + e.getMessage());
    }

    private static class InitializeCommand {

        private final String id;
        private final String message;

        private InitializeCommand(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class SetMessageCommand {

        @TargetAggregateIdentifier
        private final String id;
        private final String message;

        private SetMessageCommand(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    private static class ErrorCommand {

        @TargetAggregateIdentifier
        private final String id;
        private final String message;

        private ErrorCommand(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public String getMessage() {
            return message;
        }
    }

    public static class StateStoredAggregate {

        @AggregateIdentifier
        private String id;

        private String message;

        StateStoredAggregate(String id, String message) {
            this.id = id;
            this.message = message;
        }

        @CommandHandler
        public StateStoredAggregate(InitializeCommand cmd) {
            this.id = cmd.getId();
            apply(new StubDomainEvent());
        }

        @CommandHandler
        public void handle(SetMessageCommand cmd) {
            this.message = cmd.getMessage();
            apply(new StubDomainEvent());
        }

        @CommandHandler
        public void handle(ErrorCommand cmd) {
            this.message = cmd.getMessage();
            apply(new StubDomainEvent());
            throw new RuntimeException("Stub");
        }

        public String getMessage() {
            return message;
        }

    }

    private static class StubDomainEvent {

        StubDomainEvent() {
        }
    }
}
