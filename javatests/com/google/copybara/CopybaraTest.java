/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.copybara.Info.MigrationReference;
import com.google.copybara.authoring.Author;
import com.google.copybara.config.Config;
import com.google.copybara.config.ConfigValidator;
import com.google.copybara.config.Migration;
import com.google.copybara.testing.DummyRevision;
import com.google.copybara.testing.OptionsBuilder;
import com.google.copybara.testing.TestingEventMonitor;
import com.google.copybara.util.console.Message.MessageType;
import com.google.copybara.util.console.testing.TestingConsole;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

@RunWith(JUnit4.class)
public class CopybaraTest {

  private OptionsBuilder optionsBuilder;
  private TestingConsole console;
  private TestingEventMonitor eventMonitor;
  private Migration migration;
  private Config config;

  @Before
  public void setUp() throws Exception {
    optionsBuilder = new OptionsBuilder();
    console = new TestingConsole();
    eventMonitor = new TestingEventMonitor();
    optionsBuilder.setConsole(console);
    optionsBuilder.general.withEventMonitor(eventMonitor);
    migration = mock(Migration.class);
    config = new Config(ImmutableMap.of("workflow", migration), "foo/copy.bara.sky",
        ImmutableMap.of());
  }

  @Test
  public void testInfoUpToDate() throws Exception {
    MigrationReference<DummyRevision> workflow =
        MigrationReference.create("workflow", new DummyRevision("1111"), ImmutableList.of());
    Info<? extends Revision> mockedInfo = Info.create(ImmutableList.of(workflow));
    Mockito.<Info<? extends Revision>>when(migration.getInfo()).thenReturn(mockedInfo);

    Copybara copybara = new Copybara(new ConfigValidator() {}, migration -> {});
    copybara.info(optionsBuilder.build(), config, "workflow");

    assertThat(eventMonitor.infoFinishedEvent).isNotNull();
    assertThat(eventMonitor.infoFinishedEvent.getInfo()).isEqualTo(mockedInfo);
    console
        .assertThat()
        .onceInLog(MessageType.INFO, ".*last_migrated 1111 - last_available None.*");
  }

  @Test
  public void testInfoAvailableToMigrate() throws Exception {
    MigrationReference<DummyRevision> workflow =
        MigrationReference.create(
            "workflow",
            new DummyRevision("1111"),
            ImmutableList.of(newChange("2222"), newChange("3333")));
    Info<? extends Revision> mockedInfo = Info.create(ImmutableList.of(workflow));
    Mockito.<Info<? extends Revision>>when(migration.getInfo()).thenReturn(mockedInfo);

    Copybara copybara = new Copybara(new ConfigValidator() {}, migration -> {});
    copybara.info(optionsBuilder.build(), config, "workflow");

    assertThat(eventMonitor.infoFinishedEvent).isNotNull();
    assertThat(eventMonitor.infoFinishedEvent.getInfo()).isEqualTo(mockedInfo);
    console
        .assertThat()
        .onceInLog(MessageType.INFO, ".*last_migrated 1111 - last_available 3333.*");
  }

  private Change<DummyRevision> newChange(String revision) {
    return new Change<>(
        new DummyRevision(revision),
        new Author("Foo", "Bar"),
        "Lorem Ipsum",
        ZonedDateTime.now(ZoneId.systemDefault()),
        ImmutableMap.of());
  }
}
