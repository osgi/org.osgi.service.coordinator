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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Principal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.osgi.framework.Bundle;

/**
 * Bundles for the CoordinationPermission tests, the former
 * {@code PermissionTestCase.newMockBundle} of the TCK test support. Local to
 * this TCK because no other test needs it.
 */
final class MockBundles {

	private MockBundles() {}

	/**
	 * A bundle that answers {@link Bundle#getBundleId()},
	 * {@link Bundle#getSymbolicName()}, {@link Bundle#getLocation()} and
	 * {@link Bundle#getSignerCertificates(int)}, which is what the
	 * bundle-filtered permissions evaluate for an {@code (id=..)},
	 * {@code (name=..)}, {@code (location=..)} or {@code (signer=..)} filter.
	 * Every other method throws {@link UnsupportedOperationException}.
	 *
	 * @param id the bundle id
	 * @param symbolicName the bundle symbolic name, may be {@code null}
	 * @param location the bundle location
	 * @param signerDNs the distinguished names of the signers, one certificate
	 *            chain of one certificate each; none for an unsigned bundle
	 * @return the bundle
	 */
	static Bundle bundle(long id, String symbolicName, String location, String... signerDNs) {
		Objects.requireNonNull(location, "location");
		Map<X509Certificate,List<X509Certificate>> signers = new LinkedHashMap<>();
		if (signerDNs != null) {
			for (String dn : signerDNs) {
				X509Certificate certificate = new MockX509Certificate(
						new MockPrincipal(dn));
				signers.put(certificate, Collections.singletonList(certificate));
			}
		}
		InvocationHandler handler = new MockBundle(id, symbolicName, location,
				signers);
		return (Bundle) Proxy.newProxyInstance(Bundle.class.getClassLoader(),
				new Class< ? >[] {
						Bundle.class
				}, handler);
	}

	private static final class MockBundle implements InvocationHandler {
		private final long										id;
		private final String									symbolicName;
		private final String									location;
		private final Map<X509Certificate,List<X509Certificate>>	signers;

		MockBundle(long id, String symbolicName, String location,
				Map<X509Certificate,List<X509Certificate>> signers) {
			this.id = id;
			this.symbolicName = symbolicName;
			this.location = location;
			this.signers = signers;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) {
			switch (method.getName()) {
				case "getBundleId" :
					return id;
				case "getSymbolicName" :
					return symbolicName;
				case "getLocation" :
					return location;
				case "getSignerCertificates" :
					return new LinkedHashMap<>(signers);
				case "equals" :
					return proxy == args[0];
				case "hashCode" :
					return Long.hashCode(id);
				case "toString" :
					return "MockBundle[id=" + id + ", symbolicName=" + symbolicName
							+ ", location=" + location + ", signers="
							+ signers.keySet() + "]";
				default :
					throw new UnsupportedOperationException("Bundle."
							+ method.getName()
							+ " is not available on a permission mock bundle");
			}
		}
	}

	/**
	 * A certificate that only knows its subject; the permissions read
	 * {@code getSubjectDN().getName()} for the signer filter.
	 */
	private static final class MockX509Certificate extends X509Certificate {
		private final Principal principal;

		MockX509Certificate(Principal principal) {
			this.principal = principal;
		}

		@Override
		public Principal getSubjectDN() {
			return principal;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof MockX509Certificate) {
				return principal.equals(((MockX509Certificate) obj).principal);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return principal.hashCode();
		}

		@Override
		public String toString() {
			return principal.toString();
		}

		@Override
		public void checkValidity()
				throws CertificateExpiredException, CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void checkValidity(Date date)
				throws CertificateExpiredException, CertificateNotYetValidException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getBasicConstraints() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Principal getIssuerDN() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] getIssuerUniqueID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] getKeyUsage() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getNotAfter() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Date getNotBefore() {
			throw new UnsupportedOperationException();
		}

		@Override
		public BigInteger getSerialNumber() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSigAlgName() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getSigAlgOID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getSigAlgParams() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getSignature() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean[] getSubjectUniqueID() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getTBSCertificate() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getVersion() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getEncoded() throws CertificateEncodingException {
			throw new UnsupportedOperationException();
		}

		@Override
		public PublicKey getPublicKey() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void verify(PublicKey key) throws InvalidKeyException,
				NoSuchAlgorithmException, NoSuchProviderException,
				SignatureException, CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void verify(PublicKey key, String sigProvider)
				throws InvalidKeyException, NoSuchAlgorithmException,
				NoSuchProviderException, SignatureException,
				CertificateException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public byte[] getExtensionValue(String oid) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getNonCriticalExtensionOIDs() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasUnsupportedCriticalExtension() {
			throw new UnsupportedOperationException();
		}
	}

	private static final class MockPrincipal implements Principal {
		private final String name;

		MockPrincipal(String name) {
			this.name = Objects.requireNonNull(name, "signer DN");
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj instanceof MockPrincipal) {
				return name.equals(((MockPrincipal) obj).name);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return name.hashCode();
		}

		@Override
		public String toString() {
			return name;
		}
	}
}
