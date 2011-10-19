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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.support.finder.PolicyReader;

/**
 * This class is responsible for updating the policies on a regular basis.
 * During initialization, all policy files within the specified directory are
 * loaded, and the time of their last modification is stored. Also, every file
 * is stored as a reference together with the loaded policy. After a specified
 * time all policy files in the directory where checked against the stored
 * modification times, and also all loaded policies if their corresponding files
 * still exists.
 * 
 * If the policy file has changed - the time of last modification differs from
 * the stored one - it is reloaded.
 * 
 * If the files does not have a stored time, it is loaded and a new entry is
 * created.
 * 
 * If the file to a previously loaded policy does not exits anymore, the policy
 * itself is removed from the set of available policies.
 * 
 * During the time of checking and updating the policies, the evaluation on
 * incoming requests is paused.
 * 
 * @author Bastian Hellmann
 */
public class FileUpdater extends Thread {

	private DynamicFilePolicyModule policyModule;

	private Map<File, Long> fileLastModifiedMap;

	private Map<File, AbstractPolicy> filePolicyMap;

	private PolicyReader reader;

	private DynamicPolicyCollection policies;

	private static Logger logger = Logger.getLogger(FileUpdater.class);

	/**
	 * Creates a new FileUpdater thread.
	 * 
	 * @param policyModule
	 *            DynamicFilePolicyModule
	 * @param reader
	 *            PolicyReader-object (needed to read in policies)
	 */
	public FileUpdater(DynamicFilePolicyModule policyModule, PolicyReader reader) {
		this.policyModule = policyModule;
		this.reader = reader;

		this.init();
		this.policyModule.setPolicies(this.policies);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		if (this.policyModule.getRefreshTime() > 0) {
			while (true) {
				try {
					Thread.sleep(this.policyModule.getRefreshTime());
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				logger.debug("Start updating policies");
				this.policyModule.setCurrentlyUpdating(true);
				boolean somethingChanged = this.checkForUpdates();
				if (somethingChanged) {
					this.policyModule.setPolicies(this.policies);
				}
				this.policyModule.setCurrentlyUpdating(false);
				logger.debug("Finished updating policies");
			}
		}
	}

	/**
	 * Initially loads all policies within the specified folder. Sets up the
	 * maps to store the last time of modification on the files and the relation
	 * to the loaded policy.
	 */
	private void init() {
		this.fileLastModifiedMap = new HashMap<File, Long>();
		this.filePolicyMap = new HashMap<File, AbstractPolicy>();
		this.policies = new DynamicPolicyCollection();
		AbstractPolicy policy = null;

		logger.info("Initializing policies");
		for (File file : this.policyModule.getDirectory().listFiles()) {
			if (file.isFile()
					&& file.getName().endsWith(
							this.policyModule.getPolicyFilenameExtension())) {
				try {
					logger.trace("Try to read policy file: " + file.getName());
					policy = this.reader.readPolicy(file);
					// we loaded the policy, so try putting it in the collection
					if (!this.policies.addPolicy(policy)) {
						logger.warn("tried to load the same policy multiple times: "
								+ file.getName());
					} else {
						logger.trace("Put file & last modified stamp to map.");
						this.fileLastModifiedMap.put(file, file.lastModified());
						this.filePolicyMap.put(file, policy);
					}
				} catch (ParsingException e) {
					logger.warn("Error reading policy: " + file.getName(), e);
				}
			}
		}

		logger.trace("Number of policies: " + this.fileLastModifiedMap.size());
	}

	/**
	 * Checks - if policies have changed - if new policies were created - if
	 * previously policys have to be deleted, as their files do not exist
	 * anymore
	 * 
	 * @return true, if something has changed compared to the last call of this
	 *         method
	 */
	private boolean checkForUpdates() {
		logger.info("Checking for changes on files");

		boolean somethingChanged = false;
		AbstractPolicy newPolicy = null;

		/**
		 * Check for new or updated files.
		 */
		for (File file : this.policyModule.getDirectory().listFiles()) {
			if (file.isFile()) {
				Long lastTimeUpdated = this.fileLastModifiedMap.get(file);
				logger.trace("Last modified stamp in map for file '"
						+ file.getName() + "': " + lastTimeUpdated);
				logger.trace("Last modified stamp current for file '"
						+ file.getName() + "': " + file.lastModified());

				/**
				 * file was not loaded previously -> new policy
				 */
				if (lastTimeUpdated == null) {
					try {
						newPolicy = this.reader.readPolicy(file);
						if (this.policies.addPolicy(newPolicy)) {
							logger.debug("New policy was added to database.");
							this.fileLastModifiedMap.put(file,
									file.lastModified());
							this.filePolicyMap.put(file, newPolicy);
							somethingChanged = true;
						} else {
							logger.warn("New policy was NOT added to database.");
						}
					} catch (ParsingException e) {
						e.printStackTrace();
					}
					/**
					 * file in folder is newer than the loaded policy -> update
					 * existing policy
					 */
				} else if (lastTimeUpdated < file.lastModified()) {
					try {
						newPolicy = this.reader.readPolicy(file);
						if (this.policies.addPolicy(newPolicy)) {
							logger.debug("Updated version of policy was added to database.");
							this.fileLastModifiedMap.put(file,
									file.lastModified());
							this.filePolicyMap.put(file, newPolicy);
							somethingChanged = true;
						} else {
							logger.warn("Updated version of policy was NOT added to database; old version remains");
						}
					} catch (ParsingException e) {
						e.printStackTrace();
					}
				}
			}
		}

		/**
		 * Check for deleted files.
		 */
		AbstractPolicy policyToRemove;

		Set<File> keySet = this.filePolicyMap.keySet();
		for (Iterator<File> iterator = keySet.iterator(); iterator.hasNext();) {
			File file = iterator.next();

			if (!file.exists()) {
				logger.debug("File '" + file.getName()
						+ "' doesn't exist anymore and will be removed.");
				policyToRemove = this.filePolicyMap.get(file);
				iterator.remove();
				this.fileLastModifiedMap.remove(file);
				this.policies.removePolicy(policyToRemove);
				somethingChanged = true;
			}
		}

		if (somethingChanged) {
			logger.trace("Number of policies: "
					+ this.fileLastModifiedMap.size());
		} else {
			logger.trace("Nothing changed.");
		}

		return somethingChanged;
	}
}
