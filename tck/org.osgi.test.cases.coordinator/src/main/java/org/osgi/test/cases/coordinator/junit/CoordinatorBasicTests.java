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
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.CoordinationException;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.service.coordinator.Participant;


/**
 * Basic Coordinator test case.
 */
public class CoordinatorBasicTests extends CoordinatorTest {

	/**
	 * Timeout Fixed exception Extending timeout
	 * @throws Exception
	 */

	@Test
	public void testOverallTimeout() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		{
			Coordination cc = c.begin("timeout-1", 0);
			cc.extendTimeout(100);
			Thread.sleep(500);
			assertThat(cc.isTerminated()).as("must not have timed out because it did not have a timeout to begin with").isFalse();
			assertThat(cc.getFailure()).isNull();
		}

		{
			Coordination cc = c.begin("timeout-2", 100);
			cc.join(0);
			assertThat(cc.isTerminated()).isTrue();
		}
		Thread.interrupted(); // clear flag
		{
			int granularity = 1000;
			Coordination cc = c.begin("timeout-3", 2 * granularity);
			cc.extendTimeout(2 * granularity); // expires before primary
			Thread.sleep(granularity);
			assertThat(cc.isTerminated()).as("must not have timed out yet").isFalse();
			Thread.sleep(2 * granularity); // expires after primary but before
											// extended
			assertThat(cc
					.isTerminated()).as("must not have timed out yet because we extended").isFalse();
			try {
				cc.join(10 * granularity);
			}
			catch (InterruptedException ie) {
				// The spec no longer requires an implementation to
				// interrupt the thread a coordination is associated with
				// if other than the one that called the fail("") method.
				// However, it doesn't prohibit it either, so ignore the
				// exception.
			}
			assertThat(cc.isTerminated()).as("now it must be terminated").isTrue();
			assertThat(cc.getFailure()).isEqualTo(Coordination.TIMEOUT);
		}

	}

	/**
	 * Variables Map must remain available Not shared
	 *
	 */

	class VariableParticipant extends TestParticipant {
		Object	value;

		public void ended(Coordination c) throws Exception {
			super.ended(c);
			value = c.getVariables().get(VariableParticipant.class);
		}

		public void failed(Coordination c) throws Exception {
			super.failed(c);
			value = c.getVariables().get(VariableParticipant.class);
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testVariables() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination cc1 = c.create("variable-1", 0);
		Map<Class< ? >, Object> map1 = cc1.getVariables();
		map1.put(VariableParticipant.class, "hello");
		VariableParticipant vp = new VariableParticipant();
		cc1.addParticipant(vp);
		cc1.end();

		assertThat("hello").isEqualTo(vp.value);
		assertThat(vp.ended.get()).isOne();
		assertThat(vp.failed.get()).isZero();
		assertThat("hello").isEqualTo(cc1.getVariables().get(VariableParticipant.class));

		Coordination cc2 = c.create("variable-2", 0);
		Map<Class< ? >, Object> map2 = cc2.getVariables();
		map2.put(VariableParticipant.class, "goodbye");
		vp = new VariableParticipant();
		cc2.addParticipant(vp);
		cc2.fail(new Exception());

		assertThat("goodbye").isEqualTo(vp.value);
		assertThat(vp.ended.get()).isZero();
		assertThat(vp.failed.get()).isOne();
		assertThat("hello").isEqualTo(cc1.getVariables().get(VariableParticipant.class));
		assertThat("goodbye").isEqualTo(cc2.getVariables().get(VariableParticipant.class));

		assertThat(cc1.getVariables()).isNotSameAs(cc2.getVariables());
	}

	/**
	 * Stack A Coordination can be pushed at most one. Terminated Coordinations
	 * must be removed from the stack begin == create + push
	 * @throws Exception
	 */

	@Test
	public void testStack() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination cc = c.create("stack-1", 0);
		assertThat(cc.push()).isEqualTo(cc);

		try {
			cc.push();
			fail("expected an error because we're already pushed");
		}
		catch (CoordinationException ce) {
			assertThat(ce.getType()).as("Must be already pushed error").isEqualTo(CoordinationException.ALREADY_PUSHED);
		}

		assertThat(c.pop()).isEqualTo(cc);
		assertThat(cc.push()).isEqualTo(cc);

		cc.end(); // terminate it

		assertThat(cc.isTerminated()).isTrue();
		assertThat(c.pop()).as("nothing on stack anymore").isNull();

		Coordination c1 = c.begin("stack-1", 0);
		Coordination c2 = c.begin("stack-2", 0);
		Coordination c3 = c.begin("stack-3", 0);
		Coordination c4 = c.begin("stack-4", 0);

		c2.end();
		assertThat(c3.isTerminated()).isTrue();
		assertThat(c4.isTerminated()).isTrue();

		assertThat(c.pop()).isEqualTo(c1);

		try {
			c2.push();
			fail("must throw an exception");
		}
		catch (CoordinationException e) {
			assertThat(e.getType()).isEqualTo(CoordinationException.ALREADY_ENDED);
		}

		cc = c.begin("pushed", 0);
		assertThat(c.pop()).isEqualTo(cc);
	}

	/**
	 * Termination.
	 *
	 * Participants must see terminated Coordinations in their callback
	 * Termination must be atomic
	 */

	class Checking extends TestParticipant {
		boolean	terminated;
		boolean	result;

		public void ended(Coordination c) throws Exception {
			super.ended(c);
			terminated = c.isTerminated();
			result = c.fail(new Exception());
		}

		public void failed(Coordination c) throws Exception {
			super.failed(c);
			terminated = c.isTerminated();
			result = c.fail(new Exception());
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testTerminatedInCallbacks() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination cc = c.create("termination-1", 0);
		Checking bp = new Checking();
		cc.addParticipant(bp);
		assertThat(bp.terminated).as("initialized to false ").isFalse();
		cc.end();
		assertThat(bp.terminated).as("must  be terminated in callback ").isTrue();
		assertThat(bp.result).as("result of failure is expected to be false because it was already terminated ").isFalse();
		assertThat(bp.ended.get()).as("ended called ").isOne();
		assertThat(bp.failed.get()).as("failed called ").isZero();

		cc = c.create("termination-2", 0);
		bp = new Checking();
		cc.addParticipant(bp);
		assertThat(bp.terminated).as("initialized to false ").isFalse();
		cc.fail(new IllegalStateException());
		assertThat(bp.terminated).as("must  be terminated in callback ").isTrue();
		assertThat(bp.result).as("result of failure is expected to be false because it was already terminated ").isFalse();
		assertThat(bp.ended.get()).as("ended called ").isZero();
		assertThat(bp.failed.get()).as("failed called ").isOne();

	}

	/**
	 * Life cycle
	 *
	 * Test if coordinations are terminated when the service is ungotten
	 *
	 * @throws IOException
	 * @throws BundleException
	 */

	@Test
	public void testUngotten() throws BundleException, IOException {
		BundleContext context = this.context;

		// Ensure no outstanding references
		ServiceReference<Coordinator> ref = context.getServiceReference(Coordinator.class);
		while (context.ungetService(ref));

		// Get another Bundle Context
		Bundle other = installer.installBundle("dummy.jar", false);
		other.start();

		BundleContext otherContext = other.getBundleContext();
		// Ensure no outstanding references
		while (context.ungetService(ref));

		// Get the different Coordinators
		Coordinator cUs = context.getService(ref);
		Coordinator cThem = otherContext.getService(ref);
		assertThat(cUs != cThem).as("Because they're registered with a service factory they must differ").isTrue();

		// Create a bunch of Coordinations
		Coordination ccUs1 = cUs.create("lifecyle-us-1", 0);
		Coordination ccUs2 = cUs.create("lifecyle-us-2", 0);

		Coordination ccThem1 = cThem.create("lifecyle-them-1", 0);
		Coordination ccThem2 = cThem.create("lifecyle-them-2", 0);

		// Check if we have the same view and all are visible
		assertThat(cUs
				.getCoordinations()).as("Must have 4 coordinations now from us").hasSize(4);
		assertThat(cThem
				.getCoordinations()).as("Must have 4 coordinations now from them").hasSize(4);

		// Unget their service
		while (otherContext.ungetService(ref));

		assertThat(cUs.getCoordinations()).as("Must have 2 coordinations now after they are gone").hasSize(2);

		// Check their coordinations are properly terminated
		assertThat(ccThem1.isTerminated()).isTrue();
		// Coordinations that are failed due to a bundle releasing the
		// Coordinator service must have Coordination.RELEASED as the failure
		// cause.
		Exception e = (Exception)ccThem1.getFailure();
		assertThat(e == Coordination.RELEASED).as("The failure cause should have been Coordination.RELEASED").isTrue();

		assertThat(ccThem2.isTerminated()).isTrue();

		// And that should not have affected us
		assertThat(ccUs1.isTerminated()).isFalse();
		assertThat(ccUs2.isTerminated()).isFalse();

		// Unget our service
		while (context.ungetService(ref));

		// now ours should also be gone
		assertThat(ccUs1.isTerminated()).isTrue();
		assertThat(ccUs2.isTerminated()).isTrue();
	}

	/**
	 * Timeout
	 * @throws Exception
	 */
	@Test
	public void testTableTimeout() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination cc = c.begin("table-timeout-active", 100);
		try {
			cc.join(200);
		}
		catch (InterruptedException e) {
			// The spec no longer requires an implementation to
			// interrupt the thread a coordination is associated with
			// if other than the one that called the fail("") method.
			// However, it doesn't prohibit it either, so ignore the
			// exception.
		}
		assertThat(cc.isTerminated()).isTrue();
	}

	/**
	 * Test the fail method according to the table
	 * @throws Exception
	 */

	@Test
	public void testTableFail() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		// ACTIVE, must return true no exception
		{
			Coordination cc = c.create("table-addParticipant-no-exception", 0);
			assertThat(cc.fail(new Exception())).isTrue();
		}

		// END, must return false no exception
		{
			Coordination cc = c.create("table-addParticipant-no-exception", 0);
			cc.end();
			assertThat(cc.fail(new Exception())).isFalse();
		}

		// FALSE, must return false no exception
		{
			Coordination cc = c.create("table-addParticipant-no-exception", 0);
			cc.fail(new Exception());
			assertThat(cc.fail(new Exception())).isFalse();
		}

	}

	/**
	 * Table showing exceptions for the addParticipant method.
	 * @throws Exception
	 */
	@Test
	public void testTableAddParticipant() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		// NONE in ACTIVE state
		Coordination cc = c.create("table-addParticipant-no-exception", 0);
		cc.addParticipant(new TestParticipant());
		cc.end(); // No exception

		// DEADLOCK same thread in ACTIVE state
		{
			Coordination cc1 = c.begin("table-addParticipant-deadlock-1", 0);
			Coordination cc2 = c.begin("table-addParticipant-deadlock-2", 0);

			try {
				TestParticipant tp = new TestParticipant();
				cc1.addParticipant(tp);
				cc2.addParticipant(tp);
			}
			catch (CoordinationException e) {
				assertThat(e
						.getType()).isEqualTo(CoordinationException.DEADLOCK_DETECTED);
			}
		}
		// LOCK_INTERRUPTED
		{

			final TestParticipant tp = new TestParticipant();
			final Coordination cc2 = c.begin("table-addParticipant-deadlock-2",
					0);

			Thread t1 = new Thread() {
				public void run() {
					try {
						Coordination cc1 = c.begin(
								"table-addParticipant-deadlock-1", 0);
						cc1.addParticipant(tp);
						try {
							Thread.sleep(200);
							System.out.println("Will interrupt thread "
									+ cc2.getThread());
							cc2.getThread().interrupt();
							Thread.sleep(100);
							cc1.end();
						}
						catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					catch (Throwable t) {
						t.printStackTrace();
					}
				}
			};
			t1.start();
			Thread.sleep(100);

			try {
				cc2.addParticipant(tp); // will block
			}
			catch (CoordinationException e) {
				assertThat(e
						.getType()).isEqualTo(CoordinationException.LOCK_INTERRUPTED);
			}

			t1.join();
		}

		// ALREADY_ENDED in END state
		cc = c.begin("table-addParticipant-already-ended", 0);
		cc.end();
		try {
			cc.addParticipant(new TestParticipant());
			fail("Must throw ALREADY_ENDED");
		}
		catch (CoordinationException e) {
			assertThat(e.getType()).isEqualTo(CoordinationException.ALREADY_ENDED);
		}

		// FAILED in FAILED state
		cc = c.begin("table-addParticipant-failed", 0);
		Exception localFailure = new Exception();
		try {

			cc.fail(localFailure);
			cc.addParticipant(new TestParticipant());
		}
		catch (CoordinationException e) {
			assertThat(e.getType()).isEqualTo(CoordinationException.FAILED);
			assertThat(e.getCause()).isEqualTo(localFailure);
		}

	}

	/**
	 * Table showing exceptions for the end method.
	 * @throws Exception
	 */

	@Test
	public void testTableEnd() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		// end method

		Coordination cc = c.create("table-end-no-exception", 0);
		cc.end(); // No exception

		cc = c.create("table-end-partially-ended", 0);
		cc.addParticipant(new Participant() {

			public void ended(Coordination local) throws Exception {
				throw new Exception();
			}

			public void failed(Coordination local) throws Exception {
				// ignore
			}

		});
		try {
			cc.end();
		}
		catch (CoordinationException ce) {
			assertThat(ce.getType()).isEqualTo(CoordinationException.PARTIALLY_ENDED);
		}

		cc = c.create("table-end-already-ended", 0);
		cc.end();
		try {
			cc.end();
		}
		catch (CoordinationException ce) {
			assertThat(ce.getType()).isEqualTo(CoordinationException.ALREADY_ENDED);
		}

		cc = c.create("table-end-failed", 0);
		cc.fail(new Exception());
		try {
			cc.end();
		}
		catch (CoordinationException ce) {
			assertThat(ce.getType()).isEqualTo(CoordinationException.FAILED);
		}
	}

	/**
	 * CO0021 – All coordinations can be passed as parameters in method call
	 * @throws Exception
	 */
	@Test
	public void testPassAsArgument() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Vector<BigInteger> queue = new Vector<BigInteger>();
		Coordination cc = c.create("CO0021", 0);
		try {
			for (int i = 0; i < 10; i++)
				foo(cc, 100, queue);
		}
		finally {
			cc.end();
		}
		assertThat(queue).hasSize(10 * 100);
	}

	class ThreadParticipant extends Thread implements Participant {

		public void ended(Coordination c) throws Exception {
			join();
		}

		public void failed(Coordination c) throws Exception {
			interrupt();
		}
	}

	void foo(Coordination c, final int count, final Vector<BigInteger> queue) {
		final Random r = new Random();
		ThreadParticipant t = new ThreadParticipant() {
			public void run() {
				for (int i = 0; i < count; i++) {
					BigInteger big = new BigInteger(2048, r);
					queue.add(big);
				}
			}
		};
		t.start();
		c.addParticipant(t);
	}

	/**
	 * CO0017 – Allow parties that make the coordination fail to provide
	 * information why it failed, the rationale for this requirement is that it
	 * is for trouble shooting
	 * @throws Exception
	 */
	@Test
	public void testFailInfo() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination c1 = c.begin("CO0017", 0);
		try {
			throw new IllegalArgumentException();
		}
		catch (Exception e) {
			c1.fail(e);
		}
		finally {
			try {
				c1.end(); // throws an exception
			}
			catch (CoordinationException e) {
				assertThat(e.getType()).isEqualTo(CoordinationException.FAILED);
				assertThat(e.getCause().getClass()).isSameAs(IllegalArgumentException.class);
			}
		}
	}

	/**
	 * CO0015 – Provide an identifying mechanism for coordinations
	 *
	 * CO0018 – A Coordination must have a unique identification for management
	 * purposes
	 * @throws Exception
	 */
	@Test
	public void testId() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination c1 = c.begin("CO0015", 0);
		Coordination c2 = c.begin("CO0015", 0);
		Coordination c3 = c.begin("CO0015-1", 0);
		Coordination c4 = c.begin("CO0015-2", 0);

		// Check for duplicate names
		assertThat(c1.getName()).isEqualTo("CO0015");
		assertThat(c2.getName()).isEqualTo("CO0015");

		// Some extra
		assertThat(c3.getName()).isEqualTo("CO0015-1");
		assertThat(c4.getName()).isEqualTo("CO0015-2");

		// Monotonically increasing
		assertThat(c1.getId()).isGreaterThanOrEqualTo(0);
		assertThat(c1.getId()).isLessThan(c2.getId());
		assertThat(c2.getId()).isLessThan(c3.getId());
		assertThat(c3.getId()).isLessThan(c4.getId());

		// Findable by ID
		assertThat(c.getCoordination(c1.getId())).isEqualTo(c1);
		assertThat(c.getCoordination(c2.getId())).isEqualTo(c2);
		assertThat(c.getCoordination(c3.getId())).isEqualTo(c3);
		assertThat(c.getCoordination(c4.getId())).isEqualTo(c4);

		// Remove c4, then try to find it
		// and test calling of name/id for a terminated Coordination
		c4.end();
		assertThat(c.getCoordination(c4.getId())).isNull();
		assertThat(c4.getName()).isEqualTo("CO0015-2");
	}

	/**
	 * CO0014 – It must be possible to fail a coordination from outside of the
	 * initiating/active thread.
	 * @throws Exception
	 */

	@Test
	public void testFailCoordinationFromNoninitiatingThread() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		final Semaphore s = new Semaphore(0);
		final AtomicReference<Coordination> coord = new AtomicReference<Coordination>();

		Thread t1 = new Thread() {
			public void run() {
				Coordination c1 = c.begin("co0012-1", 0);
				coord.set(c1);
				s.release(1);
				try {
					c1.join(0);
				}
				catch (InterruptedException e) {
					// The spec no longer requires an implementation to
					// interrupt the thread a coordination is associated with
					// if other than the one that called the fail("") method.
					// However, it doesn't prohibit it either, so ignore the
					// exception.
				}
				finally {
					s.release(1);
				}
			}
		};
		t1.start();
		s.acquire(1);
		Exception e = new Exception();
		coord.get().fail(e);
		s.acquire(1);

		assertThat(coord.get().isTerminated()).isTrue();
		assertThat(coord.get().getFailure()).isSameAs(e);
	}

	void clear(Coordinator c) {
		Thread.interrupted();

		for (Coordination cc : c.getCoordinations())
			cc.fail(new Exception());
		assertThat(c.getCoordinations()).hasSize(0);
	}

	/**
	 * CO0013 – It must be possible to enumerate the active coordinations
	 * @throws Exception
	 */

	@Test
	public void testEnumerateCoordinations() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		clear(c);

		Coordination c1 = c.begin("co0012-1", 0);
		Coordination c2 = c.begin("co0012-2", 0);
		Coordination c3 = c.create("co0012-3", 0);
		Coordination c4 = c.create("co0012-4", 0);

		assertThat(c.peek()).isEqualTo(c2);

		List<Coordination> cl = new ArrayList<Coordination>(Arrays.asList(c1,
				c2, c3, c4));
		assertThat(c.getCoordinations().size()).isEqualTo(4);
		cl.removeAll(c.getCoordinations());
		assertThat(cl.isEmpty()).isTrue();

		Collection< ? extends Coordination> initial = c.getCoordinations();

		c3.end();
		assertThat(initial.size()).isEqualTo(4);
		assertThat(c.getCoordinations().size()).isEqualTo(3);

		cl = new ArrayList<Coordination>(Arrays.asList(c1, c2, c4));
		cl.removeAll(c.getCoordinations());
		assertThat(cl.isEmpty()).isTrue();

		c2.end();
		c1.end();
		c4.end();
		assertThat(c.getCoordinations()).hasSize(0);
		assertThat(initial.size()).isEqualTo(4);

		for (Coordination x : initial) {
			assertThat(x.isTerminated()).isTrue();
		}

		try {
			initial.add(null);
			assertThat(initial.size()).as("Must be mutable").isEqualTo(5);
		}
		catch (Exception e) {
			fail("Must be mutable");
		}
		try {
			initial.remove(null);
			assertThat(initial.size()).as("Must be mutable").isEqualTo(4);

		}
		catch (Exception e) {
			fail("Must be mutable");
		}
	}

	/**
	 * CO0012 – The initiator must be informed if one of the participants throws
	 * an exception in the termination method for an ok outcome.
	 * @throws Exception
	 */

	@Test
	public void testPartiallyEnded() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp1 = new TestParticipant();
		TestParticipant tp2 = new TestParticipant() {
			public void ended(Coordination x) throws Exception {
				super.ended(x);
				throw new RuntimeException();
			}
		};

		Coordination c1 = c.begin("co0012", 0);
		assertThat(c.peek()).isEqualTo(c1);
		try {
			c1.addParticipant(tp1);
			c1.addParticipant(tp2);
		}
		finally {
			try {
				c1.end();
				fail("Expected a PARTIALLY ENDED exception!");
			}
			catch (CoordinationException e) {
				assertThat(e
						.getType()).isEqualTo(CoordinationException.PARTIALLY_ENDED);
				assertThat(tp1.ended.get()).isOne();
				assertThat(tp1.failed.get()).isZero();
				assertThat(tp2.ended.get()).isOne();
				assertThat(tp2.failed.get()).isZero();
			}
			assertThat(c1.isTerminated()).isTrue();
		}
	}

	/**
	 * CO0011 – Coordinations must be allowed to nest where the nested
	 * coordination is independent of the outer coordinations.
	 *
	 * This is the OK case
	 * @throws Exception
	 */
	@Test
	public void testNestingOk() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp1 = new TestParticipant();
		TestParticipant tp2 = new TestParticipant();
		Coordination c1 = c.begin("co0011-1", 0);
		try {
			c1.addParticipant(tp1);
			assertThat(c.peek()).isEqualTo(c1);

			Coordination c2 = c.begin("co0011-2", 0);
			assertThat(c.peek()).isEqualTo(c2);
			assertThat(c1.isTerminated()).isFalse();
			assertThat(c2.isTerminated()).isFalse();
			assertThat(tp1.ended.get()).isZero();
			assertThat(tp1.failed.get()).isZero();

			assertThat(c.pop()).isEqualTo(c2);
			assertThat(c.peek()).isEqualTo(c1);
			c2.push();
			assertThat(c.peek()).isEqualTo(c2);

			try {
				c2.addParticipant(tp2);
				assertThat(c1.isTerminated()).isFalse();
				assertThat(c2.isTerminated()).isFalse();
				assertThat(tp1.ended.get()).isZero();
				assertThat(tp1.failed.get()).isZero();
				assertThat(tp2.ended.get()).isZero();
				assertThat(tp2.failed.get()).isZero();
			}
			finally {
				c2.end();
				assertThat(c1.isTerminated()).isFalse();
				assertThat(c2.isTerminated()).isTrue();
				assertThat(tp1.ended.get()).isZero();
				assertThat(tp1.failed.get()).isZero();
				assertThat(tp2.ended.get()).isOne();
				assertThat(tp2.failed.get()).isZero();
			}

		}
		finally {
			c1.end();
			assertThat(c1.isTerminated()).isTrue();
			assertThat(tp1.ended.get()).isOne();
			assertThat(tp1.failed.get()).isZero();
			assertThat(tp2.ended.get()).isOne();
			assertThat(tp2.failed.get()).isZero();
		}
	}

	/**
	 * CO0011 – Coordinations must be allowed to nest where the nested
	 * coordination is independent of the outer coordinations.
	 *
	 * This is the FAILURE case
	 * @throws Exception
	 */
	@Test
	public void testNestingInnerFails() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp1 = new TestParticipant();
		TestParticipant tp2 = new TestParticipant();
		Coordination c1 = c.begin("co0011-1", 0);
		try {
			c1.addParticipant(tp1);
			assertThat(c.peek()).isEqualTo(c1);

			Coordination c2 = c.begin("co0011-2", 0);
			assertThat(c.peek()).isEqualTo(c2);
			assertThat(c1.isTerminated()).isFalse();
			assertThat(c2.isTerminated()).isFalse();
			assertThat(tp1.ended.get()).isZero();
			assertThat(tp1.failed.get()).isZero();

			assertThat(c.pop()).isEqualTo(c2);
			assertThat(c.peek()).isEqualTo(c1);
			c2.push();
			assertThat(c.peek()).isEqualTo(c2);

			try {
				c2.addParticipant(tp2);
				assertThat(c1.isTerminated()).isFalse();
				assertThat(c2.isTerminated()).isFalse();
				assertThat(tp1.ended.get()).isZero();
				assertThat(tp1.failed.get()).isZero();
				assertThat(tp2.ended.get()).isZero();
				assertThat(tp2.failed.get()).isZero();
				c2.fail(new Exception());
			}
			finally {
				try {
					c2.end();
					fail("expected c2 to throw an exception!");
				}
				catch (CoordinationException e) {
					assertThat(e.getType()).isEqualTo(CoordinationException.FAILED);
					assertThat(c1.isTerminated()).isFalse();
					assertThat(c2.isTerminated()).isTrue();
					assertThat(tp1.ended.get()).isZero();
					assertThat(tp1.failed.get()).isZero();
					assertThat(tp2.ended.get()).isZero();
					assertThat(tp2.failed.get()).isOne();
				}
			}
		}
		finally {
			c1.end();
			assertThat(c1.isTerminated()).isTrue();
			assertThat(tp1.ended.get()).isOne();
			assertThat(tp1.failed.get()).isZero();
			assertThat(tp2.ended.get()).isZero();
			assertThat(tp2.failed.get()).isOne();
		}
	}

	/**
	 * CO0010 – Participants must be able to prevent blocking
	 *
	 * We do this by creating a proxy.
	 */

	class ProxyParticipant implements Participant {
		Participant	proxy;

		public ProxyParticipant(Participant proxy) {
			this.proxy = proxy;
		}

		public void ended(Coordination c) throws Exception {
			proxy.ended(c);
		}

		public void failed(Coordination c) throws Exception {
			proxy.failed(c);
		}
	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testPreventBlocking() throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();

		final TestParticipant tp1 = new TestParticipant();

		Coordination c1 = c.begin("co0010", 0);
		c1.addParticipant(tp1);

		// Check if we block correctly
		final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
		final Coordination c3 = c.create("co0009-3", 0);
		Thread t1 = new Thread() {
			public void run() {
				try {
					c3.addParticipant(new ProxyParticipant(tp1));
				}
				catch (Throwable e) {
					throwable.set(e);
				}
			}
		};
		t1.start();
		t1.join(); // wait for t1 to finish, means not blocked
		assertThat(throwable.get()).isNull();

		c1.end(); // calls stuff on tp1,
		assertThat(tp1.ended.get()).as("after c1 terminated").isOne();
		assertThat(tp1.failed.get()).as("after c1 terminated").isZero();

		c3.end(); // terminates, calling tp1 again on ended()
		assertThat(tp1.ended.get()).as("after c3 terminated").isEqualTo(2);
		assertThat(tp1.failed.get()).as("after c3 terminated").isZero();
	}

	/**
	 *
	 * CO0009 – A participant must automatically be blocked when it attempts to
	 * participate on multiple coordinations in different threads. Participating
	 * in two coordinations on the same thread with the same participant is an
	 * error.
	 * @throws Exception
	 */

	@Test
	public void testNoParticipationOnTwoCoordinationsInTheSameThread()
			throws Exception {
		final Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();

		final TestParticipant tp1 = new TestParticipant();

		Coordination c1 = c.begin("co0009-1", 0);
		Coordination c2 = c.begin("co0009-2", 0);

		c1.addParticipant(tp1);

		// Try to add on the same thread.
		try {
			c2.addParticipant(tp1);
			fail("Excepted deadlock excepton");
		}
		catch (CoordinationException e) {
			assertThat(e.getType()).isEqualTo(CoordinationException.DEADLOCK_DETECTED);
			assertThat(c2.getParticipants()).isEmpty();
			c2.end();
			assertThat(c.peek()).isEqualTo(c1);
		}

		// Check if a timeout cancels the block
		final AtomicReference<CoordinationException> exception = new AtomicReference<CoordinationException>();
		Thread t1 = new Thread() {
			public void run() {
				Coordination c3 = c.begin("co0009-3", 1000);
				try {
					c3.addParticipant(tp1);
				}
				catch (CoordinationException e) {
					exception.set(e);
				}
			}
		};
		t1.start();
		t1.join();
		assertThat(exception.get()).isNotNull();
		assertThat(exception.get().getType()).isEqualTo(CoordinationException.FAILED);
		assertThat(exception.get().getCause()).isEqualTo(Coordination.TIMEOUT);

		// Check if we block correctly
		final AtomicReference<Throwable> throwable = new AtomicReference<Throwable>();
		final Coordination c3 = c.create("co0009-3", 0);
		t1 = new Thread() {
			public void run() {
				try {
					c3.addParticipant(tp1);
				}
				catch (Throwable e) {
					throwable.set(e);
				}
			}
		};
		t1.start();
		Thread.sleep(1000);
		assertThat(t1.isAlive()).isTrue(); // is blocked on addParticipant
		c1.end(); // calls stuff on tp1, unblocks t1
		t1.join(); // wait for t1 to finish, can only happen when unblocks

		// c3 is still not terminated, so we have only one callback yet
		assertThat(tp1.ended.get()).as("after c1 terminated").isOne();
		assertThat(tp1.failed.get()).as("after c1 terminated").isZero();
		assertThat(throwable.get()).isNull();

		c3.end(); // terminates, calling tp1 again on ended()
		assertThat(tp1.ended.get()).as("after c3 terminated").isEqualTo(2);
		assertThat(tp1.failed.get()).as("after c3 terminated").isZero();
	}

	/**
	 * CO0008 – It must be possible to enumerate the participants
	 * @throws Exception
	 */
	@Test
	public void testEnumerateParticipants() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();

		TestParticipant tp1 = new TestParticipant();
		TestParticipant tp2 = new TestParticipant();
		TestParticipant tp3 = new TestParticipant();

		List< ? extends Participant> participants = Arrays
				.asList(tp1, tp2, tp3);
		Coordination cc = c.begin("co0008", 0);
		try {
			for (Participant p : participants) {
				cc.addParticipant(p);
			}
			cc.addParticipant(tp3); // these should not add the fray
			cc.addParticipant(tp2);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("No exception expected here " + e);
		}
		finally {
			assertThat(!cc.isTerminated()).as("must not be terminated").isTrue();
			assertThat(cc.getFailure()).as("no failure").isNull();
			List< ? extends Participant> other = cc.getParticipants();
			assertThat(other).isEqualTo(participants);
			cc.end();
			assertThat(cc.getFailure()).as("no failure").isNull();
			assertThat(cc.isTerminated()).isTrue();
			assertThat(other).isEqualTo(participants);

			// Check if we can no longer add to the list
			try {
				cc.addParticipant(new TestParticipant());
				fail("Expected exception becasue we should no longer be able to add");
			}
			catch (CoordinationException e) {
				assertThat(e.getType()).as("Should not accept any more coordinatins").isEqualTo(CoordinationException.ALREADY_ENDED);
				assertThat(other).as("Should not have sneaked in there").isEqualTo(participants);
			}
		}
	}

	/**
	 */

	/**
	 * CO0003 – Any party participating in the task must be able to make the
	 * coordination always fail when ended.
	 *
	 * CO0005 – Participants must know that the coordination succeeded or failed
	 * at termination
	 */

	Exception	failure	= new Exception("failed");

	class FailingParticipant extends TestParticipant {
		private final Coordinator	c;

		FailingParticipant() throws Exception {
			c = coordinator;
		}

		public void doWork() {
			c.addParticipant(this);
			c.fail(failure);
		}

		public void ended(Coordination cc) throws Exception {
			throw new AssertionError("I did fail this coordination!!");
		}

	}

	/**
	 * @throws Exception
	 */
	@Test
	public void testParticipantFailure() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		FailingParticipant tp1 = new FailingParticipant();

		Coordination cc = c.begin("co0003", 0);
		try {
			tp1.doWork();
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("No exception expected here " + e);
		}
		finally {
			try {
				assertThat(cc.isTerminated()).as("must be terminated").isTrue();
				assertThat(cc.getFailure()).as("failure").isEqualTo(failure);
				assertThat(tp1.ended.get()).as("Ended must be 0").isZero();
				assertThat(tp1.failed.get()).as("Failed must be 1").isOne();
				cc.end();
				fail("End should throw a ConfigurationException FAILED");
			}
			catch (CoordinationException e) {
				assertThat(CoordinationException.FAILED).as("Must be FAILED").isEqualTo(e.getType());
				assertThat(e.getCause()).as("And our original failure").isEqualTo(failure);
			}
		}
	}

	/**
	 * CO0002 – An initiator must be able to initiate a coordination and control
	 * the final outcome
	 *
	 * CO0004 – Participants in the task must be informed when the coordination
	 * is terminated.
	 *
	 * CO0005 – Participants must know that the coordination succeeded or failed
	 * at termination
	 * @throws Exception
	 */
	@Test
	public void testControlOutcome() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp1 = new TestParticipant();

		Exception failure1 = new Exception("failed");
		Coordination cc = c.create("co0002", 0);
		try {
			cc.addParticipant(tp1);
			cc.fail(failure1);
		}
		catch (Exception e) {
			e.printStackTrace();
			fail("No exception expected here " + e);
		}
		finally {
			try {
				assertThat(cc.isTerminated()).as("must be terminated").isTrue();
				assertThat(cc.getFailure()).as("failure").isEqualTo(failure1);
				assertThat(tp1.ended.get()).as("Ended must be 0").isZero();
				assertThat(tp1.failed.get()).as("Failed must be 1").isOne();
				cc.end();
				fail("End should throw a ConfigurationException FAILED");
			}
			catch (CoordinationException e) {
				assertThat("co0002").isEqualTo(e.getName());
				assertThat(e.getId()).isEqualTo(cc.getId());
				assertThat(CoordinationException.FAILED).as("Must be FAILED").isEqualTo(e.getType());
				assertThat(e.getCause()).as("And our original failure").isEqualTo(failure1);
			}
		}

	}

	/**
	 * CO0001 – Provide a solution to allow multiple parties that collaborate to
	 * coordinate the outcome of a task initiated by an initiator.
	 * @throws Exception
	 */

	@Test
	public void testCollaboration() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp1 = new TestParticipant();
		TestParticipant tp2 = new TestParticipant();
		TestParticipant tp3 = new TestParticipant();

		Coordination cc = c.create("co0001", 0);
		try {
			cc.addParticipant(tp1);
			cc.addParticipant(tp2);
			cc.addParticipant(tp3);
		}
		catch (Exception e) {
			fail("No exception expected here " + e);
		}
		finally {
			cc.end();
			assertThat(cc.isTerminated()).as("must be terminated").isTrue();
			assertThat(cc.getFailure()).as("no failure").isNull();
			assertThat(tp1.ended.get()).as("Ended must be 1").isOne();
			assertThat(tp1.failed.get()).as("Failed must be 0").isZero();
			assertThat(tp2.ended.get()).as("Ended must be 1").isOne();
			assertThat(tp2.failed.get()).as("Failed must be 0").isZero();
			assertThat(tp3.ended.get()).as("Ended must be 1").isOne();
			assertThat(tp3.failed.get()).as("Failed must be 0").isZero();
		}

	}

	/**
	 *
	 * CO0006 – A coordination must have a timeout
	 *
	 * CO0007 – The timeout timer must be settable by the initiator
	 *
	 * If a Coordination is not properly terminated (either end() or
	 * fail(Throwable) is not called) it will time out. A timeout will have the
	 * effect of fail(Coordination.TIMEOUT), it will therefore follow the same
	 * process as failing. The Coordination.TIMEOUT field is a singleton
	 * Exception for this purpose. Initiators can find if a timeout occurs by
	 * reacting to the failure and then inspecting the exception from
	 * getFailure().
	 *
	 *
	 * @throws Exception
	 */
	@Test
	public void testTimeout() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp = new TestParticipant();
		Coordination cc = c.create("test1", 100); // timeout after 100 ms

		try {
			cc.addParticipant(tp);
			Thread.sleep(200);
		}
		catch (Exception e) {
			fail("");
		}
		finally {
			try {
				assertThat(cc.isTerminated()).as("must be terminated").isTrue();
				assertThat(cc.getFailure()).as("must have a timeout failure").isEqualTo(Coordination.TIMEOUT);
				assertThat(tp.ended.get()).as("Ended must be 0").isZero();
				assertThat(tp.failed.get()).as("Failed must be 1").isOne();
				cc.end();
			}
			catch (CoordinationException ce) {
				assertThat(ce.getType()).as("except a failed ").isEqualTo(CoordinationException.FAILED);
				assertThat(ce.getCause()).as("except a timeout exception ").isEqualTo(Coordination.TIMEOUT);
			}
		}

	}

	/**
	 * Test the basic normal setup.
	 *
	 * @throws Exception
	 */
	@Test
	public void testBasic() throws Exception {
		Coordinator c = coordinator;
		assertThat(c).as("Expect the service to be there").isNotNull();
		TestParticipant tp = new TestParticipant();
		Coordination cc = c.create("test1", 0);

		try {
			cc.addParticipant(tp);
		}
		catch (Exception e) {
			fail("");
		}
		finally {
			cc.end();
		}

		assertThat(cc.isTerminated()).as("must be terminated").isTrue();
		assertThat(cc.getFailure()).as("must have no failure").isNull();
		assertThat(tp.ended.get()).as("Ended must be 1").isOne();
		assertThat(tp.failed.get()).as("Failed must be 0").isZero();
	}

	/**
	 * (1) Return 0 when no extension has taken place.
	 * (2) Return the current deadline when 0 is passed as an argument.
	 * (3) Return the new deadline when an extension has taken place. The 
	 *     extension must be at least as much as was requested.
	 * (4) A non-zero return value represents the system time in milliseconds 
	 *     when the coordination will time out.
	 * @throws Exception 
	 */
	@Test
	public void testExtendTimeout() throws Exception {
		Coordination c = coordinator.create("c", 0);
		try {
			// (2)
			assertDeadline(0, 0, c);
			// (1)
			assertThat(c.extendTimeout(5000)).as("No extension should have occurred").isZero();
		}
		finally {
			assertEnd(c);
		}
		long start = System.currentTimeMillis();
		c = coordinator.create("c", 3000);
		try {
			// (2), (4)
			assertDeadline(start, 3000, c);
			// (3)
			assertThat(c.extendTimeout(2000) >= 5000).as("Wrong deadline").isTrue();
			// (2), (4)
			assertDeadline(start, 5000, c);
			c.join(8000);
			assertTerminated(c);
			assertTimeoutDuration(start, 5000);
		}
		finally {
			assertEndFailed(c, CoordinationException.FAILED, Coordination.TIMEOUT);
		}
	}
}

class TestParticipant implements Participant {
	final AtomicInteger	ended	= new AtomicInteger(0);
	final AtomicInteger	failed	= new AtomicInteger(0);

	public void failed(Coordination c) throws Exception {
		failed.incrementAndGet();
	}

	public void ended(Coordination c) throws Exception {
		ended.incrementAndGet();
	}

	/**
	 * Create a brain damaged equals to check identity
	 */
	public boolean equals(Object other) {
		return other instanceof TestParticipant;
	}

	/**
	 * Create a brain damaged equals to check identity
	 */
	public int hashCode() {
		return 1;
	}
}
