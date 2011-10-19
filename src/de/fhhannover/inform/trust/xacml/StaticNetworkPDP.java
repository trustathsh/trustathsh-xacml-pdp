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

/**
 ** License for components that are based on or use parts of the Sun XACML 
 ** implementation. (http://sunxacml.sourceforge.net/)
 **/
/*
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package de.fhhannover.inform.trust.xacml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Node;

import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.combine.PermitOverridesPolicyAlg;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.SelectorModule;
import com.sun.xacml.support.finder.StaticPolicyFinderModule;
import com.sun.xacml.support.finder.StaticRefPolicyFinderModule;
import com.sun.xacml.support.finder.URLPolicyFinderModule;

/**
 * Based on {@link SimplePDP} from the Sun XACML implementation.
 * 
 * @author Bastian Hellmann
 */
public class StaticNetworkPDP implements NetworkPDP {

	// this is the actual PDP object we'll use for evaluation
	private PDP pdp = null;

	/**
	 * Constructor that takes an array of filenames and URLs, each of which
	 * points to an XACML policy, and sets up a <code>PDP</code> with access to
	 * these policies only. These policies may be accessed based on context
	 * matching or by reference (based on their policy identifiers). The
	 * <code>PDP</code> is also setup to support dynamic URL references.
	 * 
	 * @param policies
	 *            an array of filenames and URLs that specify policies
	 * @throws UnknownIdentifierException
	 * @throws URISyntaxException
	 */
	public StaticNetworkPDP(String[] policies) throws URISyntaxException,
			UnknownIdentifierException {
		// Create the two static modules with the given policies so that
		// we have context-based and reference-based access to all the
		// policies provided on the command-line
		List<String> policyList = Arrays.asList(policies);
		StaticPolicyFinderModule staticModule = new StaticPolicyFinderModule(
				PermitOverridesPolicyAlg.algId, policyList);
		StaticRefPolicyFinderModule staticRefModule = new StaticRefPolicyFinderModule(
				policyList);

		// also create a module that lets us get at URL-based policies
		URLPolicyFinderModule urlModule = new URLPolicyFinderModule();

		// next, setup the PolicyFinder that this PDP will use
		PolicyFinder policyFinder = new PolicyFinder();
		Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
		policyModules.add(staticModule);
		policyModules.add(staticRefModule);
		policyModules.add(urlModule);
		policyFinder.setModules(policyModules);

		// now setup attribute finder modules for the current date/time and
		// AttributeSelectors (selectors are optional, but this project does
		// support a basic implementation)
		CurrentEnvModule envAttributeModule = new CurrentEnvModule();
		SelectorModule selectorAttributeModule = new SelectorModule();

		// Setup the AttributeFinder just like we setup the PolicyFinder. Note
		// that unlike with the policy finder, the order matters here. See the
		// the javadocs for more details.
		AttributeFinder attributeFinder = new AttributeFinder();
		List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
		attributeModules.add(envAttributeModule);
		attributeModules.add(selectorAttributeModule);
		attributeFinder.setModules(attributeModules);

		// finally, initialize our pdp
		this.pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.fhhannover.inform.trust.xacml.NetworkPDP#evaluate(org.w3c.dom.Node)
	 */
	@Override
	public ResponseCtx evaluate(Node requestRoot) throws IOException,
			ParsingException {
		// setup the request based on the file
		RequestCtx request = RequestCtx.getInstance(requestRoot);

		// evaluate the request
		return pdp.evaluate(request);
	}
}
