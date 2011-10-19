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

import java.net.URI;
import java.util.TreeSet;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.support.finder.PolicyCollection;

/**
 * Based on {@link PolicyCollection} from the Sun XACML implementation.
 * 
 * In addition to the {@link PolicyCollection} it supports the removal of
 * policies from the collection. This is used by the
 * {@link DynamicFilePolicyModule} to unload policies that does not exist
 * anymore during runtime (if corresponding file is deleted).
 * 
 * @author Bastian Hellmann
 */
public class DynamicPolicyCollection extends PolicyCollection {

	/**
	 * 
	 */
	public DynamicPolicyCollection() {
		super();
	}

	/**
	 * @param combiningAlg
	 * @param parentPolicyId
	 */
	public DynamicPolicyCollection(PolicyCombiningAlgorithm combiningAlg,
			URI parentPolicyId) {
		super(combiningAlg, parentPolicyId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.xacml.support.finder.PolicyCollection#addPolicy(com.sun.xacml
	 * .AbstractPolicy, java.lang.String)
	 */
	public boolean addPolicy(AbstractPolicy policy, String identifier) {
		if (this.policies.containsKey(identifier)) {
			// this identifier is already is use, so see if this version is
			// already in the set
			TreeSet set = (TreeSet) (this.policies.get(identifier));
			set.remove(policy);
			this.policies.put(identifier, set);

			return set.add(policy);
		} else {
			// this identifier isn't already being used, so create a new
			// set in the map for it, and add the policy
			TreeSet set = new TreeSet(this.versionComparator);
			this.policies.put(identifier, set);

			return set.add(policy);
		}
	}

	/**
	 * @param policy
	 * @return
	 */
	public boolean removePolicy(AbstractPolicy policy) {
		return removePolicy(policy, policy.getId().toString());
	}

	/**
	 * @param policy
	 * @param identifier
	 * @return
	 */
	public boolean removePolicy(AbstractPolicy policy, String identifier) {
		if (this.policies.containsKey(identifier)) {
			TreeSet set = (TreeSet) (this.policies.get(identifier));
			return set.remove(policy);
		} else {
			return false;
		}
	}
}
