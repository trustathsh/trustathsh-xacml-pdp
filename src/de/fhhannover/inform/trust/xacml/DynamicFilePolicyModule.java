/*
 * =====================================================
 *  _____                _     ____  _____ _   _ _   _ 
 * |_   _|_ __ _   _ ___| |_  / __ \|  ___| | | | | | |
 *   | | | '__| | | / __| __|/ / _` | |_  | |_| | |_| |
 *   | | | |  | |_| \__ \ |_| | (_| |  _| |  _  |  _  |
 *   |_| |_|   \__,_|___/\__|\ \__,_|_|   |_| |_|_| |_|
 *                            \____/                   
 *
 * =====================================================
 * 
 * Copyright (C) 2011, Fachhochschule Hannover
 * (University of Applied Sciences and Arts)
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 *  
 * http://trust.inform.fh-hannover.de/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * 
 */

package de.fhhannover.inform.trust.xacml;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.combine.CombiningAlgFactory;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;
import com.sun.xacml.support.finder.PolicyCollection;
import com.sun.xacml.support.finder.PolicyReader;
import com.sun.xacml.support.finder.StaticPolicyFinderModule;
import com.sun.xacml.support.finder.TopLevelPolicyException;

/**
 * This PolicyFinderModule allows for dynamically updating and reloading of
 * policies.
 * 
 * @author Bastian Hellmann
 */
/**
 * @author bahellma
 *
 */
public class DynamicFilePolicyModule extends PolicyFinderModule {

	private PolicyCollection policies;

	private File schemaFile = null;

	/**
	 * Directory where the policy files reside within.
	 */
	private File directory;

	/**
	 * Flag, if the polcies are currently updated.
	 */
	private boolean currentlyUpdating;

	/**
	 * File extension of the policy files.
	 */
	private String policyFilenameExtension;

	/**
	 * Time after which the polcies are updated on a regular basis.
	 */
	private long refreshTime;

	// the policy identifier for any policy sets we dynamically create
	private static final String POLICY_ID = "urn:com:sun:xacml:support:finder:dynamic-policy-set";
	private static URI policyId = null;

	// the logger we'll use for all messages
	private static final Logger logger = Logger
			.getLogger(StaticPolicyFinderModule.class.getName());

	public static final long DEFAULT_REFRESH_TIME = 60000; // == 1 minute

	static {
		try {
			policyId = new URI(POLICY_ID);
		} catch (Exception e) {
			// this can't actually happen, but just in case...
			if (logger.isLoggable(Level.SEVERE)) {
				logger.log(Level.SEVERE, "couldn't assign default policy id");
			}
		}
	};

	/**
	 * Creates a new instance of the dynamic policy finder module.
	 * 
	 * @param combiningAlg
	 *            the combining algorithm
	 * @param policyDirectory
	 *            Directory where the policy files reside within.
	 * @param policyFilenameExtension
	 *            File extension of the policy files.
	 * @param refreshTime
	 *            Time after which the polcies are updated on a regular basis.
	 *            Has to be greater or equal to 0.
	 * @throws UnknownIdentifierException
	 * @throws URISyntaxException
	 * @throws FileNotFoundException
	 */
	public DynamicFilePolicyModule(String combiningAlg, File policyDirectory,
			String policyFilenameExtension, long refreshTime)
			throws UnknownIdentifierException, URISyntaxException,
			FileNotFoundException {
		PolicyCombiningAlgorithm alg = (PolicyCombiningAlgorithm) (CombiningAlgFactory
				.getInstance().createAlgorithm(new URI(combiningAlg)));

		this.directory = policyDirectory;
		if (!this.directory.isDirectory()) {
			throw new FileNotFoundException("Given path is not a directory.");
		}

		this.policyFilenameExtension = policyFilenameExtension.replace(".", "");
		this.policies = new PolicyCollection(alg, policyId);

		if (refreshTime < 0) {
			throw new IllegalArgumentException(
					"Given refresh time was less than 0.");
		} else {
			this.refreshTime = refreshTime;
		}

		String schemaName = System
				.getProperty(PolicyReader.POLICY_SCHEMA_PROPERTY);
		if (schemaName != null) {
			this.schemaFile = new File(schemaName);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.xacml.finder.PolicyFinderModule#init(com.sun.xacml.finder.
	 * PolicyFinder)
	 */
	@Override
	public void init(PolicyFinder finder) {
		PolicyReader reader = new PolicyReader(finder, logger, this.schemaFile);
		new FileUpdater(this, reader).start();
	}

	/**
	 * Finds a policy based on a request's context. If more than one policy
	 * matches, then this either returns an error or a new policy wrapping the
	 * multiple policies (depending on which constructor was used to construct
	 * this instance).
	 * 
	 * Addition to {@link PolicyFinderModule}: while the policies are updating,
	 * the thread sleeps for 100ms.
	 * 
	 * @param context
	 *            the representation of the request data
	 * 
	 * @return the result of trying to find an applicable policy
	 */
	@Override
	public synchronized PolicyFinderResult findPolicy(EvaluationCtx context) {
		try {
			while (this.currentlyUpdating) {
				Thread.sleep(100);
			}

			AbstractPolicy policy = this.policies.getPolicy(context);

			if (policy == null) {
				return new PolicyFinderResult();
			} else {
				return new PolicyFinderResult(policy);
			}
		} catch (TopLevelPolicyException tlpe) {
			return new PolicyFinderResult(tlpe.getStatus());
		} catch (InterruptedException e) {
			return new PolicyFinderResult();
		}
	}

	/**
	 * Sets the flag, if the policies are checked actually or not.
	 * 
	 * @param status
	 *            boolean, if the policies are actually updated
	 */
	protected synchronized void setCurrentlyUpdating(boolean status) {
		this.currentlyUpdating = status;
	}

	/**
	 * Synchronized method to reset the policies.
	 * 
	 * @param policies
	 *            the new collection of policies
	 */
	protected synchronized void setPolicies(PolicyCollection policies) {
		this.policies = policies;
	}


	/**
	 * @return Time after which the polcies are updated on a regular basis.
	 */
	public long getRefreshTime() {
		return this.refreshTime;
	}
	
	/**
	 * @return File extension of the policy files.
	 */
	public String getPolicyFilenameExtension() {
		return this.policyFilenameExtension;
	}

	/**
	 * @return Directory where the policy files reside within.
	 */
	public File getDirectory() {
		return this.directory;
	}
}
