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

import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * A coordination must not block on subsequent calls to terminate while 
 * notifying participants.
 * 
 * Coordinator.fail() followed by Coordination.fail()
 * Coordinator.fail() followed by Coordination.end()
 * Coordination.fail() followed by Coordination.fail()
 * Coordination.fail() followed by Coordination.end()
 * Coordination.end() followed by Coordination.fail()
 * Coordination.end() followed by Coordination.end()
 * 
 * Note that testing whether or not Coordinator.fail(Throwable) will block on 
 * subsequent calls is meaningless. The coordinator will fail the coordination, 
 * if any, at the top of the thread local stack for the calling thread. Calling 
 * the method again from another thread is pointless since it wouldn't be the 
 * same coordination. Calling the method again from the same thread is also 
 * pointless because the previous call has already returned, meaning the 
 * coordination would already be terminated and all participants notified 
 * (i.e. blocking would be impossible).
 */
public class NoBlockParticipantNotifyTest {

	@InjectBundleContext
	BundleContext		context;
	Coordinator coordinator;
	private ServiceReference<Coordinator> coordinatorReference;
	
	/**
	 * Coordinator.fail(Throwable) followed by Coordination.fail(Throwable).
	 * Coordinator.fail(Throwable) followed by Coordination.end().
	 * @throws InterruptedException
	 */
	@Test
	public void testCoordinatorFail() throws InterruptedException {
		final Semaphore lock = new Semaphore(0);
		final AtomicReference<Coordination> coordination = new AtomicReference<Coordination>();
		final AtomicReference<Thread> thread2 = new AtomicReference<Thread>();
		// This thread is responsible for ensuring there is no blockage.
		thread2.set(new BlockTestThread(lock, coordination));
		// This thread is responsible for terminating the coordination at the top
		// of the thread local stack.
		Thread thread1 = new Thread() {
			public void run() {
				// Create and push the coordination onto the thread local stack.
				coordination.set(coordinator.begin("c", 0));
				coordination.get().addParticipant(new Participator(lock));
				thread2.get().setDaemon(true);
				thread2.get().start();
				// Wait until it's okay to proceed with the block test.
				lock.acquireUninterruptibly();
				coordinator.fail(new Exception());
			}
		};
		thread1.setDaemon(true);
		thread1.start();
		thread1.join(5000);
		thread2.get().join(5000);
		assertNotBlocked(thread2.get(), thread1);
	}
	
	/**
	 * Coordination.fail(Throwable) followed by Coordination.fail(Throwable).
	 * Coordination.fail(Throwable) followed by Coordination.end().
	 * @throws InterruptedException
	 */
	@Test
	public void testCoordinationFail() throws InterruptedException {
		final Semaphore lock = new Semaphore(0);
		final AtomicReference<Coordination> coordination = new AtomicReference<Coordination>();
		final AtomicReference<Thread> thread2 = new AtomicReference<Thread>();
		// This thread is responsible for ensuring there is no blockage.
		thread2.set(new BlockTestThread(lock, coordination));
		// This thread is responsible for terminating the coordination.
		Thread thread1 = new Thread() {
			public void run() {
				// Create and push the coordination onto the thread local stack.
				coordination.set(coordinator.begin("c", 0));
				coordination.get().addParticipant(new Participator(lock));
				thread2.get().setDaemon(true);
				thread2.get().start();
				// Wait until it's okay to proceed with the block test.
				lock.acquireUninterruptibly();
				coordination.get().fail(new Exception());
			}
		};
		thread1.setDaemon(true);
		thread1.start();
		thread1.join(5000);
		thread2.get().join(5000);
		assertNotBlocked(thread2.get(), thread1);
	}
	
	/**
	 * Coordination.end() followed by Coordination.fail(Throwable).
	 * Coordination.end() followed by Coordination.end().
	 * @throws InterruptedException
	 */
	@Test
	public void testCoordinationEnd() throws InterruptedException {
		final Semaphore lock = new Semaphore(0);
		final AtomicReference<Coordination> coordination = new AtomicReference<Coordination>();
		final AtomicReference<Thread> thread2 = new AtomicReference<Thread>();
		// This thread is responsible for ensuring there is no blockage.
		thread2.set(new BlockTestThread(lock, coordination));
		// This thread is responsible for terminating the coordination.
		Thread thread1 = new Thread() {
			public void run() {
				// Create and push the coordination onto the thread local stack.
				coordination.set(coordinator.begin("c", 0));
				coordination.get().addParticipant(new Participator(lock));
				thread2.get().setDaemon(true);
				thread2.get().start();
				// Wait until it's okay to proceed with the block test.
				lock.acquireUninterruptibly();
				coordination.get().end();
			}
		};
		thread1.setDaemon(true);
		thread1.start();
		thread1.join(5000);
		thread2.get().join(5000);
		assertNotBlocked(thread2.get(), thread1);
	}
	
	@BeforeEach
	public void setUp() throws Exception {
		coordinatorReference = context.getServiceReference(Coordinator.class);
		coordinator = context.getService(coordinatorReference);
	}
	
	@AfterEach
	public void tearDown() throws Exception {
		context.ungetService(coordinatorReference);
	}
	
	private void assertNotBlocked(Thread t1, Thread t2) {
		assertThat(t2.isAlive() || t1.isAlive()).as("A coordination must not block on subsequent termination calls while notifying participants").isFalse();
	}
	
	private static class BlockTestThread extends Thread {
		private final AtomicReference<Coordination> coordination;
		private final Semaphore lock;
		
		public BlockTestThread(Semaphore lock, AtomicReference<Coordination> coordination) {
			this.lock = lock;
			this.coordination = coordination;
		}
		
		public void run() {
			// Signal ready to proceed with the block test.
			lock.release();
			// Wait until the participator signals the notification process has begun.
			lock.acquireUninterruptibly(2);
			// Terminate the coordination a second time. This must not block.
			coordination.get().fail(new Exception());
			// Terminate the coordination a third time. This must not block.
			try {
				coordination.get().end();
			}
			catch (CoordinationException e) {
				// Okay.
			}
			// Signal that the block test is completed.
			lock.release(3);
		}
	}
	
	/**
	 * A participant that will block during the notification process until
	 * told to proceed.
	 */
	private static class Participator implements Participant {
		private final Semaphore lock;
		
		public Participator(Semaphore lock) {
			this.lock = lock;
		}
		
		public void ended(Coordination c) throws Exception {
			releaseAndAcquire();
		}
		
		public void failed(Coordination c) throws Exception {
			releaseAndAcquire();
		}
		
		private void releaseAndAcquire() {
			// Signal that the participant notification process has begun.
			lock.release(2);
			// Wait until it is okay to continue.
			lock.acquireUninterruptibly(3);
		}
	}
}
