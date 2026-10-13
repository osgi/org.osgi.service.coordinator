/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.coordinator.junit;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;

/**
 * Implementations must detect orphaned coordinations (i.e. coordinations that
 * have been dereferenced and are eligible for garbage collection) and fail
 * them with an ORPHANED failure reason.
 */
public class OrphanedCoordinationTest extends CoordinatorTest {
	private Bundle coordinatorBundle;

	/**
	 * Test explicit coordination.
	 *
	 * @throws InterruptedException
	 * @throws BundleException
	 */
	@Test
	public void testOrphanedCoordinationExplicit() throws InterruptedException, BundleException {
		Coordination c = coordinator.create("c", 0);
		long id = c.getId();
		String name = c.getName();
		Bundle bundle = c.getBundle();
		OrphanedParticipant p = new OrphanedParticipant();
		c.addParticipant(p);
		Reference<Coordination> reference = new WeakReference<Coordination>(c, new ReferenceQueue<Coordination>());
		c = null;
		assertReferenceEnqueued(reference);
		coordinatorBundle.stop();
		assertOrphanedCoordination(p, id, name, bundle);
	}

	/**
	 * Test implicit coordination.
	 *
	 * @throws InterruptedException
	 * @throws BundleException
	 */
	@Test
	public void testOrphanedCoordinationImplicit() throws InterruptedException, BundleException {
		assertEmptyStack();
		Coordination c = coordinator.begin("c", 0);
		assertAtTopOfStack(c);
		long id = c.getId();
		String name = c.getName();
		Bundle bundle = c.getBundle();
		OrphanedParticipant p = new OrphanedParticipant();
		c.addParticipant(p);
		Reference<Coordination> reference = new WeakReference<Coordination>(c, new ReferenceQueue<Coordination>());
		c = null;
		assertReferenceEnqueued(reference);
		coordinatorBundle.stop();
		assertOrphanedCoordination(p, id, name, bundle);
		coordinatorBundle.start();
		coordinatorReference = context.getServiceReference(Coordinator.class);
		coordinator = context.getService(coordinatorReference);
		assertEmptyStack();
	}

	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		coordinatorBundle = coordinatorReference.getBundle();
	}

	@AfterEach
	public void tearDown() throws Exception {
		coordinatorBundle.start();
		super.tearDown();
	}

	private static final long DEFAULT_INTERVAL = 500;
	private static final int DEFAULT_ITERATIONS = 10;

	private void assertOrphanedCoordination(OrphanedParticipant p, long id, String name, Bundle b) {
		assertOrphanedParticipant(p);
		assertOrphanedFailure(p.getFailedCoordination());
		assertId(id, p.getFailedCoordination());
		assertName(name, p.getFailedCoordination());
		assertBundle(b, p.getFailedCoordination());
		assertEndFailed(p.getFailedCoordination(), CoordinationException.FAILED);
	}

	private static void assertOrphanedParticipant(OrphanedParticipant p) {
		assertThat(p.getFailedCoordination()).as("Participant did not receive failed notification").isNotNull();
	}

	private static void assertOrphanedFailure(Coordination c) {
		Throwable failure = c.getFailure();
		assertThat(failure == Coordination.ORPHANED || failure == Coordination.RELEASED).as("Failure must be ORPHANED or RELEASED").isTrue();
	}

	private static void assertId(long id, Coordination c) {
		assertThat(c.getId()).as("Wrong id").isEqualTo(id);
	}

	private static void assertName(String name, Coordination c) {
		assertThat(c.getName()).as("Wrong name").isEqualTo(name);
	}

	private static void assertBundle(Bundle b, Coordination c) {
		assertThat(c.getBundle()).as("Wrong bundle").isEqualTo(b);
	}

	private static void assertReferenceEnqueued(Reference<?> reference) throws InterruptedException {
		waitForEnqueue(reference);
		assertThat(reference.isEnqueued()).as("Coordination was not garbage collected").isTrue();
	}

	private static void waitForEnqueue(Reference<?> reference) throws InterruptedException {
		waitForEnqueue(reference, DEFAULT_ITERATIONS, DEFAULT_INTERVAL);
	}

	private static void waitForEnqueue(Reference<?> reference, int iterations, long interval) throws InterruptedException {
		for (int i = 0; i < iterations; i++) {
			System.gc();
			if (reference.isEnqueued())
				break;
			Thread.sleep(interval);
		}
	}

	/**
	 * A participant whose failed method is called due to an orphaned
	 * coordination.
	 */
	public static class OrphanedParticipant implements Participant {
		private final AtomicReference<Coordination> reference = new AtomicReference<Coordination>();

		public void ended(Coordination coordination) throws Exception {
			// noop
		}

		public void failed(Coordination coordination) throws Exception {
			reference.set(coordination);
		}

		/**
		 * Returns the coordination received as the result of a call to this
		 * participant's failed method, if any.
		 *
		 * @return The failed coordination or null.
		 */
		public Coordination getFailedCoordination() {
			return reference.get();
		}
	}
}
