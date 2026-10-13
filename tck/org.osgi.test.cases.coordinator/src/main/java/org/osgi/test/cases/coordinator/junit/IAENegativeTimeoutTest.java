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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.coordinator.Coordination;
import org.osgi.service.coordinator.Coordinator;
import org.osgi.test.common.annotation.InjectBundleContext;

/**
 * An IllegalArgumentException must be thrown when a timeout value is negative.
 */
public class IAENegativeTimeoutTest {

	@InjectBundleContext
	BundleContext		context;
	private Coordinator coordinator;
	private ServiceReference<Coordinator> coordinatorReference;
	
	/**
	 * Beginning a coordination.
	 */
	@Test
	public void testBeginCoordination() {
		IllegalArgumentException iae = null;
		try {
			Coordination c = coordinator.begin("c", -1);
			c.end();
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		assertIllegalArgumentException(iae);
	}
	
	/**
	 * Creating a coordination.
	 */
	@Test
	public void testCreateCoordination() {
		IllegalArgumentException iae = null;
		try {
			Coordination c = coordinator.create("c", -5000);
			c.end();
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		assertIllegalArgumentException(iae);
	}
	
	/**
	 * Extending a timeout.
	 */
	@Test
	public void testCoordinationFail() {
		IllegalArgumentException iae = null;
		Coordination c = coordinator.create("c", 30000);
		try {
			c.extendTimeout(-30000);
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		finally {
			c.end();
		}
		assertIllegalArgumentException(iae);
	}
	
	/**
	 * Joining a coordination.
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void testCoordinationJoin() throws InterruptedException {
		IllegalArgumentException iae = null;
		Coordination c = coordinator.create("c", 2000);
		try {
			c.join(-100);
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		finally {
			c.fail(new Exception());
		}
		assertIllegalArgumentException(iae);
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
	
	private void assertIllegalArgumentException(IllegalArgumentException e) {
		assertThat(e).as("An IllegalArgumentException must be thrown if a negative timeout is provided").isNotNull();
	}
}
