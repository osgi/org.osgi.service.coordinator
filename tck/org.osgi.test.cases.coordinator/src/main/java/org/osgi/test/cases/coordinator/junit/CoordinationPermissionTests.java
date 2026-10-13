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

import static org.assertj.core.api.Assertions.fail;
import static org.osgi.test.assertj.permission.PermissionAssertions.assertThat;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.PropertyPermission;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.service.coordinator.CoordinationPermission;

/**
 */
public class CoordinationPermissionTests {

	/**
	 * 
	 */
	@Test
	public void testInvalid() {
		invalidCoordinationPermission("(a=b)", "x");
		invalidCoordinationPermission("(a=b)", "   initiate  ,  x   ");
		invalidCoordinationPermission("(a=b)", "");
		invalidCoordinationPermission("(a=b)", "      ");
		invalidCoordinationPermission("(a=b)", null);
		invalidCoordinationPermission("(a=b)", ",");
		invalidCoordinationPermission("(a=b)", ",xxx");
		invalidCoordinationPermission("(a=b)", "xxx,");
		invalidCoordinationPermission("(a=b)", "initiate,");
		invalidCoordinationPermission("(a=b)", "admin,   ");
		invalidCoordinationPermission("(a=b)", "participate,   ");
		invalidCoordinationPermission("(a=b)", "initiateme,");
		invalidCoordinationPermission("(a=b)", "adminme,");
		invalidCoordinationPermission("(a=b)", "participateme,");
		invalidCoordinationPermission("(a=b)", ",initiate");
		invalidCoordinationPermission("(a=b)", ",admin");
		invalidCoordinationPermission("(a=b)", ",participate");
		invalidCoordinationPermission("(a=b)", "   initiateme   ");
		invalidCoordinationPermission("(a=b)", "   adminme     ");
		invalidCoordinationPermission("(a=b)", "   participateme     ");
		invalidCoordinationPermission("(a=b)", "   initiat");
		invalidCoordinationPermission("(a=b)", "   admi");
		invalidCoordinationPermission("(a=b)", "   participat");
		invalidCoordinationPermission("a.b.c", "initiate");
		invalidCoordinationPermission("()", "initiate");
		invalidCoordinationPermission(null, "initiate");

		invalidCoordinationPermission("(coordination.name=a.b.c)", null,
				"initiate");
		invalidCoordinationPermission(null,
				MockBundles.bundle(2, "test.bsn", "test.location"), "admin");
	}

	/**
	 * 
	 */
	@Test
	public void testActions() {
		Permission op = new PropertyPermission("java.home", "read");
		Bundle b1 = MockBundles.bundle(2, "test.bsn", "test.location");

		CoordinationPermission p11 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)",
				"    INITIATE,participate   ");
		CoordinationPermission p12 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)",
				"PARTICIPATE  ,   initiate");
		CoordinationPermission p13 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)", "ADMIN   ");
		CoordinationPermission p14 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)", "    Initiate    ");
		CoordinationPermission p15 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)", "parTICIpate   ");
		CoordinationPermission p16 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)", "parTICIpate,admin");
		CoordinationPermission p17 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)", "iniTIAte, ADmin");
		CoordinationPermission p18 = new CoordinationPermission(
				"(coordination.name=com.foo.coord1)",
				"partICipate, initiATe, aDMIn");

		CoordinationPermission p21 = new CoordinationPermission(
				"com.foo.coord1", b1, "    INITIATE,participate   ");
		CoordinationPermission p23 = new CoordinationPermission(
				"com.foo.coord1", b1, "adMIN   ");
		CoordinationPermission p24 = new CoordinationPermission(
				"com.foo.coord1", b1, "    Initiate    ");
		CoordinationPermission p25 = new CoordinationPermission(
				"com.foo.coord1", b1, "parTICIpate   ");
		CoordinationPermission p26 = new CoordinationPermission(
				"com.foo.coord1", b1, "parTICIpate,admin");
		CoordinationPermission p27 = new CoordinationPermission(
				"com.foo.coord1", b1, "iniTIAte, ADmin");
		CoordinationPermission p28 = new CoordinationPermission(
				"com.foo.coord1", b1, "partICipate, initiATe, aDMIn");

		Assertions.assertThat(p11.getActions()).isEqualTo("initiate,participate");
		Assertions.assertThat(p21.getActions()).isEqualTo("initiate,participate");
		Assertions.assertThat(p12.getActions()).isEqualTo("initiate,participate");
		Assertions.assertThat(p13.getActions()).isEqualTo("admin");
		Assertions.assertThat(p23.getActions()).isEqualTo("admin");
		Assertions.assertThat(p14.getActions()).isEqualTo("initiate");
		Assertions.assertThat(p24.getActions()).isEqualTo("initiate");
		Assertions.assertThat(p15.getActions()).isEqualTo("participate");
		Assertions.assertThat(p25.getActions()).isEqualTo("participate");
		Assertions.assertThat(p16.getActions()).isEqualTo("admin,participate");
		Assertions.assertThat(p26.getActions()).isEqualTo("admin,participate");
		Assertions.assertThat(p17.getActions()).isEqualTo("admin,initiate");
		Assertions.assertThat(p27.getActions()).isEqualTo("admin,initiate");
		Assertions.assertThat(p18.getActions()).isEqualTo("admin,initiate,participate");
		Assertions.assertThat(p28.getActions()).isEqualTo("admin,initiate,participate");

		assertThat(p11).implies(p21);
		assertThat(p11).doesNotImply(p23);
		assertThat(p11).implies(p24);
		assertThat(p11).implies(p25);
		assertThat(p11).doesNotImply(p26);
		assertThat(p11).doesNotImply(p27);
		assertThat(p11).doesNotImply(p28);

		assertThat(p12).implies(p21);
		assertThat(p12).doesNotImply(p23);
		assertThat(p12).implies(p24);
		assertThat(p12).implies(p25);
		assertThat(p12).doesNotImply(p26);
		assertThat(p12).doesNotImply(p27);
		assertThat(p12).doesNotImply(p28);

		assertThat(p13).doesNotImply(p21);
		assertThat(p13).implies(p23);
		assertThat(p13).doesNotImply(p24);
		assertThat(p13).doesNotImply(p25);
		assertThat(p13).doesNotImply(p26);
		assertThat(p13).doesNotImply(p27);
		assertThat(p13).doesNotImply(p28);

		assertThat(p14).doesNotImply(p21);
		assertThat(p14).doesNotImply(p23);
		assertThat(p14).implies(p24);
		assertThat(p14).doesNotImply(p25);
		assertThat(p14).doesNotImply(p26);
		assertThat(p14).doesNotImply(p27);
		assertThat(p14).doesNotImply(p28);

		assertThat(p15).doesNotImply(p21);
		assertThat(p15).doesNotImply(p23);
		assertThat(p15).doesNotImply(p24);
		assertThat(p15).implies(p25);
		assertThat(p15).doesNotImply(p26);
		assertThat(p15).doesNotImply(p27);
		assertThat(p15).doesNotImply(p28);

		assertThat(p16).doesNotImply(p21);
		assertThat(p16).implies(p23);
		assertThat(p16).doesNotImply(p24);
		assertThat(p16).implies(p25);
		assertThat(p16).implies(p26);
		assertThat(p16).doesNotImply(p27);
		assertThat(p16).doesNotImply(p28);

		assertThat(p17).doesNotImply(p21);
		assertThat(p17).implies(p23);
		assertThat(p17).implies(p24);
		assertThat(p17).doesNotImply(p25);
		assertThat(p17).doesNotImply(p26);
		assertThat(p17).implies(p27);

		assertThat(p18).implies(p21);
		assertThat(p18).implies(p23);
		assertThat(p18).implies(p24);
		assertThat(p18).implies(p25);
		assertThat(p18).implies(p26);
		assertThat(p18).implies(p27);
		assertThat(p18).implies(p28);

		assertThat(p11).doesNotImply(p11);
		assertThat(p12).doesNotImply(p12);
		assertThat(p13).doesNotImply(p13);
		assertThat(p14).doesNotImply(p14);
		assertThat(p15).doesNotImply(p15);
		assertThat(p16).doesNotImply(p16);
		assertThat(p17).doesNotImply(p17);
		assertThat(p18).doesNotImply(p18);

		assertThat(p21).doesNotImply(p11);
		assertThat(p23).doesNotImply(p13);
		assertThat(p24).doesNotImply(p14);
		assertThat(p25).doesNotImply(p15);
		assertThat(p26).doesNotImply(p16);
		assertThat(p27).doesNotImply(p17);
		assertThat(p28).doesNotImply(p18);

		assertThat(p11).doesNotImply(op);

		assertThat(p11).isEquivalentTo(p11);
		assertThat(p11).isEquivalentTo(p12);
		assertThat(p11).isNotEquivalentTo(p13);
		assertThat(p11).isNotEquivalentTo(p14);
		assertThat(p11).isNotEquivalentTo(p15);
		assertThat(p11).isNotEquivalentTo(p16);
		assertThat(p11).isNotEquivalentTo(p17);
		assertThat(p11).isNotEquivalentTo(p18);

		assertThat(p12).isEquivalentTo(p11);
		assertThat(p12).isEquivalentTo(p12);
		assertThat(p12).isNotEquivalentTo(p13);
		assertThat(p12).isNotEquivalentTo(p14);
		assertThat(p12).isNotEquivalentTo(p15);
		assertThat(p12).isNotEquivalentTo(p16);
		assertThat(p12).isNotEquivalentTo(p17);
		assertThat(p12).isNotEquivalentTo(p18);

		assertThat(p13).isNotEquivalentTo(p11);
		assertThat(p13).isNotEquivalentTo(p12);
		assertThat(p13).isEquivalentTo(p13);
		assertThat(p13).isNotEquivalentTo(p14);
		assertThat(p13).isNotEquivalentTo(p15);
		assertThat(p13).isNotEquivalentTo(p16);
		assertThat(p13).isNotEquivalentTo(p17);
		assertThat(p13).isNotEquivalentTo(p18);

		assertThat(p14).isNotEquivalentTo(p11);
		assertThat(p14).isNotEquivalentTo(p12);
		assertThat(p14).isNotEquivalentTo(p13);
		assertThat(p14).isEquivalentTo(p14);
		assertThat(p14).isNotEquivalentTo(p15);
		assertThat(p14).isNotEquivalentTo(p16);
		assertThat(p14).isNotEquivalentTo(p17);
		assertThat(p14).isNotEquivalentTo(p18);

		assertThat(p15).isNotEquivalentTo(p11);
		assertThat(p15).isNotEquivalentTo(p12);
		assertThat(p15).isNotEquivalentTo(p13);
		assertThat(p15).isNotEquivalentTo(p14);
		assertThat(p15).isEquivalentTo(p15);
		assertThat(p15).isNotEquivalentTo(p16);
		assertThat(p15).isNotEquivalentTo(p17);
		assertThat(p15).isNotEquivalentTo(p18);

		assertThat(p16).isNotEquivalentTo(p11);
		assertThat(p16).isNotEquivalentTo(p12);
		assertThat(p16).isNotEquivalentTo(p13);
		assertThat(p16).isNotEquivalentTo(p14);
		assertThat(p16).isNotEquivalentTo(p15);
		assertThat(p16).isEquivalentTo(p16);
		assertThat(p16).isNotEquivalentTo(p17);
		assertThat(p16).isNotEquivalentTo(p18);

		assertThat(p17).isNotEquivalentTo(p11);
		assertThat(p17).isNotEquivalentTo(p12);
		assertThat(p17).isNotEquivalentTo(p13);
		assertThat(p17).isNotEquivalentTo(p14);
		assertThat(p17).isNotEquivalentTo(p15);
		assertThat(p17).isNotEquivalentTo(p16);
		assertThat(p17).isEquivalentTo(p17);
		assertThat(p17).isNotEquivalentTo(p18);

		assertThat(p18).isNotEquivalentTo(p11);
		assertThat(p18).isNotEquivalentTo(p12);
		assertThat(p18).isNotEquivalentTo(p13);
		assertThat(p18).isNotEquivalentTo(p14);
		assertThat(p18).isNotEquivalentTo(p15);
		assertThat(p18).isNotEquivalentTo(p16);
		assertThat(p18).isNotEquivalentTo(p17);
		assertThat(p18).isEquivalentTo(p18);

		assertThat(p21).isEquivalentTo(p21);
		assertThat(p21).isNotEquivalentTo(p23);
		assertThat(p21).isNotEquivalentTo(p24);
		assertThat(p21).isNotEquivalentTo(p25);
		assertThat(p21).isNotEquivalentTo(p26);
		assertThat(p21).isNotEquivalentTo(p27);
		assertThat(p21).isNotEquivalentTo(p28);

		assertThat(p23).isNotEquivalentTo(p21);
		assertThat(p23).isEquivalentTo(p23);
		assertThat(p23).isNotEquivalentTo(p24);
		assertThat(p23).isNotEquivalentTo(p25);
		assertThat(p23).isNotEquivalentTo(p26);
		assertThat(p23).isNotEquivalentTo(p27);
		assertThat(p23).isNotEquivalentTo(p28);

		assertThat(p24).isNotEquivalentTo(p21);
		assertThat(p24).isNotEquivalentTo(p23);
		assertThat(p24).isEquivalentTo(p24);
		assertThat(p24).isNotEquivalentTo(p25);
		assertThat(p24).isNotEquivalentTo(p26);
		assertThat(p24).isNotEquivalentTo(p27);
		assertThat(p24).isNotEquivalentTo(p28);

		assertThat(p25).isNotEquivalentTo(p21);
		assertThat(p25).isNotEquivalentTo(p23);
		assertThat(p25).isNotEquivalentTo(p24);
		assertThat(p25).isEquivalentTo(p25);
		assertThat(p25).isNotEquivalentTo(p26);
		assertThat(p25).isNotEquivalentTo(p27);
		assertThat(p25).isNotEquivalentTo(p28);

		assertThat(p26).isNotEquivalentTo(p21);
		assertThat(p26).isNotEquivalentTo(p23);
		assertThat(p26).isNotEquivalentTo(p24);
		assertThat(p26).isNotEquivalentTo(p25);
		assertThat(p26).isEquivalentTo(p26);
		assertThat(p26).isNotEquivalentTo(p27);
		assertThat(p26).isNotEquivalentTo(p28);

		assertThat(p27).isNotEquivalentTo(p21);
		assertThat(p27).isNotEquivalentTo(p23);
		assertThat(p27).isNotEquivalentTo(p24);
		assertThat(p27).isNotEquivalentTo(p25);
		assertThat(p27).isNotEquivalentTo(p26);
		assertThat(p27).isEquivalentTo(p27);
		assertThat(p27).isNotEquivalentTo(p28);

		assertThat(p28).isNotEquivalentTo(p21);
		assertThat(p28).isNotEquivalentTo(p23);
		assertThat(p28).isNotEquivalentTo(p24);
		assertThat(p28).isNotEquivalentTo(p25);
		assertThat(p28).isNotEquivalentTo(p26);
		assertThat(p28).isNotEquivalentTo(p27);
		assertThat(p28).isEquivalentTo(p28);

		PermissionCollection pc = p13.newPermissionCollection();

		assertThat(pc).hasNoElements();

		assertThat(pc).doesNotImply(p23);

		assertThat(pc).accepts(p14);
		assertThat(pc).doesNotImply(p21);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).doesNotImply(p26);
		assertThat(pc).doesNotImply(p27);
		assertThat(pc).doesNotImply(p28);

		assertThat(pc).accepts(p13);
		assertThat(pc).doesNotImply(p21);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).doesNotImply(p26);
		assertThat(pc).implies(p27);
		assertThat(pc).doesNotImply(p28);

		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();
		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).implies(p25);
		assertThat(pc).doesNotImply(p26);
		assertThat(pc).doesNotImply(p27);
		assertThat(pc).doesNotImply(p28);

		assertThat(pc).rejects(op);
		assertThat(pc).isSerializable();

		pc = p13.newPermissionCollection();

		assertThat(pc).accepts(p13);
		assertThat(pc).doesNotImply(p21);
		assertThat(pc).implies(p23);
		assertThat(pc).doesNotImply(p24);
		assertThat(pc).doesNotImply(p25);
		assertThat(pc).doesNotImply(p26);
		assertThat(pc).doesNotImply(p27);
		assertThat(pc).doesNotImply(p28);

		assertThat(pc).isSerializable();

		pc = p11.newPermissionCollection();

		assertThat(pc).accepts(p11);
		assertThat(pc).implies(p21);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).implies(p25);
		assertThat(pc).doesNotImply(p26);
		assertThat(pc).doesNotImply(p27);
		assertThat(pc).doesNotImply(p28);

		pc.setReadOnly();

		assertThat(pc).rejects(p12);

		assertThat(pc).hasElements();

		assertThat(p11).isSerializable();
		assertThat(p12).isSerializable();
		assertThat(p13).isSerializable();
		assertThat(p14).isSerializable();
		assertThat(p15).isSerializable();
		assertThat(p16).isSerializable();
		assertThat(p17).isSerializable();
		assertThat(p18).isSerializable();
		assertThat(p21).isNotSerializable();
		assertThat(p23).isNotSerializable();
		assertThat(p24).isNotSerializable();
		assertThat(p25).isNotSerializable();
		assertThat(p26).isNotSerializable();
		assertThat(p27).isNotSerializable();
		assertThat(p28).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testWildcard() {
		Bundle b1 = MockBundles.bundle(2, "test.bsn", "test.location");
		CoordinationPermission p21 = new CoordinationPermission(
				"(coordination.name=com.foo.*)", "initiate");
		CoordinationPermission p22 = new CoordinationPermission(
				"com.foo.coord2", b1, "initiate");
		CoordinationPermission p23 = new CoordinationPermission("*", "initiate");
		CoordinationPermission p24 = new CoordinationPermission(
				"com.bar.coord2", b1, "initiate");

		assertThat(p21).implies(p22);
		assertThat(p21).doesNotImply(p23);
		assertThat(p21).doesNotImply(p24);
		assertThat(p23).implies(p22);
		assertThat(p23).implies(p24);
		assertThat(p23).implies(p23);

		PermissionCollection pc = p21.newPermissionCollection();

		assertThat(pc).accepts(p21);
		assertThat(pc).implies(p22);
		assertThat(pc).doesNotImply(p23);
		assertThat(pc).doesNotImply(p24);

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);

		assertThat(pc).isSerializable();

		pc = p23.newPermissionCollection();

		assertThat(pc).accepts(p23);
		assertThat(pc).implies(p22);
		assertThat(pc).implies(p23);
		assertThat(pc).implies(p24);
		assertThat(pc).isSerializable();

		assertThat(p21).isSerializable();
		assertThat(p22).isNotSerializable();
		assertThat(p23).isSerializable();
		assertThat(p24).isNotSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testActionImplications() {
		CoordinationPermission participate = new CoordinationPermission("*",
				"participate");
		CoordinationPermission admin = new CoordinationPermission("*", "admin");
		CoordinationPermission initiate = new CoordinationPermission("*",
				"initiate");

		Assertions.assertThat(participate.getActions()).isEqualTo("participate");
		Assertions.assertThat(admin.getActions()).isEqualTo("admin");
		Assertions.assertThat(initiate.getActions()).isEqualTo("initiate");

		PermissionCollection adminpc = admin.newPermissionCollection();
		adminpc.add(admin);
		PermissionCollection initiatepc = initiate.newPermissionCollection();
		initiatepc.add(initiate);
		PermissionCollection participatepc = participate
				.newPermissionCollection();
		participatepc.add(participate);

		assertThat(admin).implies(admin);
		assertThat(adminpc).implies(admin);
		assertThat(initiate).implies(initiate);
		assertThat(initiatepc).implies(initiate);
		assertThat(participate).implies(participate);
		assertThat(participatepc).implies(participate);

		assertThat(admin).doesNotImply(initiate);
		assertThat(adminpc).doesNotImply(initiate);
		assertThat(admin).doesNotImply(participate);
		assertThat(adminpc).doesNotImply(participate);

		assertThat(initiate).doesNotImply(admin);
		assertThat(initiatepc).doesNotImply(admin);
		assertThat(initiate).doesNotImply(participate);
		assertThat(initiatepc).doesNotImply(participate);

		assertThat(participate).doesNotImply(initiate);
		assertThat(participatepc).doesNotImply(initiate);
		assertThat(participate).doesNotImply(admin);
		assertThat(participatepc).doesNotImply(admin);
	}

	/**
	 * 
	 */
	@Test
	public void testFiltersName() {
		Bundle b1 = MockBundles.bundle(2, "test.bsn", "test.location");
		CoordinationPermission p31 = new CoordinationPermission(
				"  (coordination.name  =com.foo.coord3)", "initiate");
		CoordinationPermission p32 = new CoordinationPermission(
				"(coordination.name=com.foo.*)", "initiate");
		CoordinationPermission p33 = new CoordinationPermission(
				"(coordination.name=com.*)", "initiate");
		CoordinationPermission p34 = new CoordinationPermission(
				"(coordination.name=*)", "initiate");
		CoordinationPermission p35 = new CoordinationPermission(
				"com.foo.coord3", b1, "initiate");
		CoordinationPermission p38 = new CoordinationPermission("*", "initiate");

		assertThat(p31).implies(p35);
		assertThat(p32).implies(p35);
		assertThat(p33).implies(p35);
		assertThat(p34).implies(p35);
		assertThat(p38).implies(p35);

		assertThat(p31).doesNotImply(p31);
		assertThat(p31).doesNotImply(p32);
		assertThat(p31).doesNotImply(p33);
		assertThat(p31).doesNotImply(p34);

		assertThat(p31).doesNotImply(p38);
		assertThat(p32).doesNotImply(p38);
		assertThat(p33).doesNotImply(p38);
		assertThat(p34).implies(p38);
		assertThat(p35).doesNotImply(p38);
		assertThat(p38).implies(p38);

		PermissionCollection pc = p31.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p31);

		assertThat(pc).doesNotImply(p31);
		assertThat(pc).doesNotImply(p32);
		assertThat(pc).doesNotImply(p33);
		assertThat(pc).doesNotImply(p34);

		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);

		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p38);

		assertThat(pc).hasElements();
		assertThat(pc).isSerializable();

		pc = p32.newPermissionCollection();
		assertThat(pc).accepts(p32);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p33.newPermissionCollection();
		assertThat(pc).accepts(p33);
		assertThat(pc).implies(p35);
		assertThat(pc).doesNotImply(p38);
		assertThat(pc).isSerializable();

		pc = p34.newPermissionCollection();
		assertThat(pc).accepts(p34);
		assertThat(pc).implies(p35);
		assertThat(pc).implies(p38);

		assertThat(p31).isSerializable();
		assertThat(p32).isSerializable();
		assertThat(p33).isSerializable();
		assertThat(p34).isSerializable();
		assertThat(p35).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testFiltersBundle() {
		CoordinationPermission p41 = new CoordinationPermission("(id=2)",
				"initiate");
		CoordinationPermission p42 = new CoordinationPermission(
				"(location=test.*)", "initiate");
		CoordinationPermission p43 = new CoordinationPermission(
				"(name=test.*)", "initiate");
		CoordinationPermission p44 = new CoordinationPermission(
				"(signer=\\*, o=ACME, c=US)", "initiate");
		CoordinationPermission p45 = new CoordinationPermission(
				"(coordination.name=com.foo.*)", "initiate");

		CoordinationPermission p46 = new CoordinationPermission(
				"com.foo.coord4", MockBundles.bundle(2, "test.bsn", "test.location", "cn=Bugs Bunny, o=ACME, c=US"),
				"initiate");
		CoordinationPermission p47 = new CoordinationPermission(
				"com.bar.coord4", MockBundles.bundle(3, "not.bsn", "not.location", "cn=Bugs Bunny, o=NOT, c=US"), "initiate");

		assertThat(p41).implies(p46);
		assertThat(p42).implies(p46);
		assertThat(p43).implies(p46);
		assertThat(p44).implies(p46);
		assertThat(p45).implies(p46);

		assertThat(p41).doesNotImply(p47);
		assertThat(p42).doesNotImply(p47);
		assertThat(p43).doesNotImply(p47);
		assertThat(p44).doesNotImply(p47);
		assertThat(p45).doesNotImply(p47);

		assertThat(p41).doesNotImply(p41);
		assertThat(p41).doesNotImply(p42);
		assertThat(p41).doesNotImply(p43);
		assertThat(p41).doesNotImply(p44);
		assertThat(p41).doesNotImply(p45);

		PermissionCollection pc = p41.newPermissionCollection();
		assertThat(pc).hasNoElements();

		assertThat(pc).accepts(p44);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);

		assertThat(pc).doesNotImply(p41);
		assertThat(pc).doesNotImply(p42);
		assertThat(pc).doesNotImply(p43);
		assertThat(pc).doesNotImply(p44);
		assertThat(pc).doesNotImply(p45);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p43);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p42);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).isSerializable();

		pc = p41.newPermissionCollection();
		assertThat(pc).accepts(p41);
		assertThat(pc).implies(p46);
		assertThat(pc).doesNotImply(p47);
		assertThat(pc).hasElements();

		// throw IllegalArgumentException If specified permission to be
		// added was constructed with bundles.
		assertThat(pc).rejects(p46);
		assertThat(pc).rejects(p47);

		assertThat(p41).isSerializable();
		assertThat(p42).isSerializable();
		assertThat(p43).isSerializable();
		assertThat(p44).isSerializable();
		assertThat(p45).isSerializable();
		assertThat(p46).isNotSerializable();
		assertThat(p47).isNotSerializable();
		assertThat(pc).isSerializable();
	}

	/**
	 * 
	 */
	@Test
	public void testPermissionCollection() {
		CoordinationPermission p51 = new CoordinationPermission(
				"  (coordination.name  =com.foo.coord5)", "initiate");
		CoordinationPermission p52 = new CoordinationPermission(
				"(coordination.name=com.foo.*)", "initiate");
		CoordinationPermission p53 = new CoordinationPermission(
				"(coordination.name=com.*)", "initiate");
		CoordinationPermission p54 = new CoordinationPermission(
				"(coordination.name=*)", "initiate");
		CoordinationPermission p59 = new CoordinationPermission("*", "initiate");

		CoordinationPermission p5a = new CoordinationPermission(
				"com.foo.coord5", MockBundles.bundle(2, "test.bsn", "test.location", "cn=Bugs Bunny, o=ACME, c=US"),
				"initiate");
		CoordinationPermission p5b = new CoordinationPermission(
				"com.bar.coord5", MockBundles.bundle(3, "not.bsn", "not.location", "cn=Bugs Bunny, o=NOT, c=US"), "initiate");

		PermissionCollection pc;
		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(p51).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p51).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p51.newPermissionCollection();
		assertThat(pc).accepts(p51);
		assertThat(pc).implies(p5a);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p52.newPermissionCollection();
		assertThat(pc).accepts(p52);
		assertThat(p52).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p52).doesNotImply(p5b);
		assertThat(pc).doesNotImply(p5b);
		assertThat(pc).isSerializable();

		pc = p53.newPermissionCollection();
		assertThat(pc).accepts(p53);
		assertThat(p53).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p53).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(pc).accepts(p59);
		assertThat(pc).implies(p5a);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p54.newPermissionCollection();
		assertThat(pc).accepts(p54);
		assertThat(p54).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p54).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();

		pc = p59.newPermissionCollection();
		assertThat(pc).accepts(p59);
		assertThat(p59).implies(p5a);
		assertThat(pc).implies(p5a);
		assertThat(p59).implies(p5b);
		assertThat(pc).implies(p5b);
		assertThat(pc).isSerializable();
	}

	private static void invalidCoordinationPermission(String name,
			String actions) {
		try {
			CoordinationPermission p = new CoordinationPermission(name, actions);
			fail(p + " created with invalid arguments");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		catch (NullPointerException e) {
			// expected
		}
	}

	private static void invalidCoordinationPermission(String name,
			Bundle bundle, String actions) {
		try {
			CoordinationPermission p = new CoordinationPermission(name, bundle,
					actions);
			fail(p + " created with invalid arguments");
		}
		catch (IllegalArgumentException e) {
			// expected
		}
		catch (NullPointerException e) {
			// expected
		}
	}

}
